# Performance Audit — EligiusNametag

> **Date:** 2026-11-02  
> **Method:** Full hot-path code review (`code-review-and-quality` skill, Performance axis). Read-only audit; no code was changed.  
> **Scope:** `NametagService` tick loop → `updateTarget` → `parsePlaceholders` → `BukkitNametagRenderer`, plus platform/config adapters.  
> **Goal of this doc:** serve as the single source of truth so work can resume on any batch **without re-auditing**. Includes copy-paste prompts for AI agents.

---

## 1. How the plugin consumes CPU (hot path)

Every `interval` seconds (default `0.5` = every 10 ticks), `EligiusNametag.startTask()` runs:

```
animationManager.tick()
nametagService.updateAllNametags()
  ├─ Class.forName("…RegionizedServer")                 ← P0-4, every tick
  ├─ platform.getTamedMobs()                            ← Bukkit.getEntity × M, every tick
  └─ for each target T ∈ (onlinePlayers ∪ tamedMobs):
       for each viewer V ∈ onlinePlayers:               ← O((N+M) × N)
         platform.isSameWorld(T,V)          → 2× Bukkit.getEntity
         platform.getDistanceSquared(T,V)   → 2× Bukkit.getEntity + 2× getLocation() (alloc)
         platform.canViewerSeeTarget(V,T)   → 2× Bukkit.getPlayer [+ getEntity]
       if T visible for ≥1 viewer:
         platform.getPrimaryGroup(T)        ← Vault/LuckPerms, every tick (P0-5)
         for each template line:
           platform.parsePlaceholders(T, line)           ← P0-1/P0-2, every line, every tick
             String.replace × ~10
             PlaceholderAPI.setPlaceholders (often expensive)
             Pattern.compile × 2                          ← P0-1, recompiled every call
             MiniMessage.deserialize
             animation regex
         renderer.renderNametag(...)
           new HashSet<>(viewers) per line                ← P1-3
           show/hideEntity only on set difference         ← already OK
```

**Reference load (mental model):** 100 players + 50 pets ⇒ ~15,000 viewer-pairs per tick and ~450 full parse pipelines per second at default interval.

**Key existing optimizations (do NOT regress these):**

- `showEntity`/`hideEntity` only fire on set difference (no packet spam when stable).
- Text updates only when `Component` differs (`lineJsonCache`).
- `disableVanillaNametag` guarded by `isCustomNameVisible()`.
- DB + Redis in `toggleSelfView` are async.
- `UpdateChecker` is async with 5s timeout.
- Uses `distanceSquared` (not `distance`).
- No DB N+1 per viewer.

---

## 2. Finding catalog

Severity key: 🔴 P0 = urgent (real TPS impact, medium+ servers) · 🟠 P1 = high (measurable, cheap) · 🟡 P2 = medium debt · ✅ OK = already good.

### 🔴 P0 — Urgent

#### P0-1 — `Pattern.compile` inside the hot path
- **Where:** `src/main/java/com/makrozai/eligiusnametag/adapter/platform/PaperPlatformAdapter.java:226` and `:249`
- **What:** Both regexes are compiled **on every `parsePlaceholders` call**:
  - `":([a-zA-Z0-9_\\-]+):"` (img placeholder scan)
  - `"<anim:([a-zA-Z0-9_\\-]+)>(.*?)</anim:\\1>"` (animation scan)
- **Cost:** one compile per line per entity per tick; thousands/sec on busy servers.
- **Fix:**
  ```java
  private static final Pattern IMG_PATTERN = Pattern.compile(":([a-zA-Z0-9_\\-]+):");
  private static final Pattern ANIM_PATTERN = Pattern.compile("<anim:([a-zA-Z0-9_\\-]+)>(.*?)</anim:\\1>");
  ```
  Replace both `Pattern.compile(...)` call sites with the constants.
- **Risk:** none (patterns are stateless).
- **Tests:** existing suite must stay green; no new behavior.

#### P0-2 — Full placeholder pipeline re-runs every tick even when nothing changed
- **Where:** `NametagService.java:136-138` (`platform.parsePlaceholders(targetId, line)` per line) → `PaperPlatformAdapter.parsePlaceholders` (lines ~166–278)
- **What:** Every visible target × every template line × every tick runs: ~10 `String.replace`, `PlaceholderAPI.setPlaceholders`, `MiniMessage.deserialize`, animation regex, legacy-color chain.
- **Cost:** Dominant cost when PlaceholderAPI is installed (many expansions hit disk/DB/network).
- **Fix (design):** parsed-line cache.
  - Key must include everything that can change the output:
    - template line string (raw)
    - resolved group name
    - animation frame index for each `<anim:…>` used (or a global animation epoch from `AnimationManager.tick()`)
    - volatile player/mob state: name, display name, rounded health, max health, age/baby, owner name, type label
  - Practical simplification: **cache key = raw template line + group + animation epoch + a small "state hash"** (health as int, UUID, custom name hash). Invalidate whole cache on `reloadPlugin()`.
  - Store: `Map<CacheKey, Component>` (or per-target `Map<line, Component>`).
  - PAPI output still needs invalidation: if a `%placeholder%` is dynamic beyond our state hash, we cannot know. Mitigation options (choose one, document choice):
    1. TTL cache (e.g. re-parse at most every N ticks per line; default N=2 or interval-based) — simple, bounded staleness.
    2. Re-parse always when template contains `%…%` (PAPI) but cache pure-MiniMessage lines — safer.
    3. Config flag `cache_placeholders: true|false` default true + TTL.
  - **Recommended:** option 3 (TTL + flag), TTL = 10 ticks (0.5s) so behavior at default interval is unchanged, but interval can be lowered without linear parse cost.
- **Risk:** stale text (health/placeholder lag by TTL). Must invalidate on reload.
- **Tests:**
  - unit: same inputs → second call does not invoke `parsePlaceholders` again (mock `PlatformPort`, count invocations).
  - unit: reload clears cache.
  - unit: animation epoch bump changes output for `<anim:…>` lines.

#### P0-3 — O((N+M)×N) pair loop with repeated entity resolution + Location allocations
- **Where:**
  - Loop: `NametagService.java:111-127`
  - `PaperPlatformAdapter.isSameWorld` (118–123), `getDistanceSquared` (126–132), `canViewerSeeTarget` (101–115)
- **What:** For each (target, viewer) pair the code calls `Bukkit.getEntity`/`getPlayer` 4–6 times and allocates 2 `Location` copies via `getLocation()` just to compute squared distance.
- **Cost:** At 150×100: ~15k map lookups + ~30k `Location` allocations per tick.
- **Fix:**
  1. Once per tick in `updateAllNametags`, build a **viewer snapshot**: `List<ViewerSnap>` where `ViewerSnap { UUID id; Player player; String world; double x,y,z; }` from `Bukkit.getOnlinePlayers()` (one pass).
  2. In `updateTarget`, resolve **target once**: entity, world, x/y/z (and player-vs-mob flags).
  3. Distance: `dx*dx+dy*dy+dz*dz` — no `getLocation()`.
  4. Keep domain purity: either extend `PlatformPort` with a snapshot API (`List<ViewerView> snapshotViewers()`, `TargetView resolve(UUID)`) or compute snapshots in the adapter and pass them down. Prefer **adding methods to `PlatformPort`** over leaking Bukkit types into the domain (architecture axis).
  5. Short-circuit: if `!isHidden` fails, skip all viewer work for that target (already partially true — keep early-continue for hidden targets).
- **Risk:** medium (touches core loop). Keep behavior identical: same visibility predicates.
- **Tests:**
  - Existing `NametagServiceTest` must stay green (6 tests).
  - New: out-of-range/near-range/cross-world still correct with snapshot path.
  - Optional micro-bench: count `getEntity` calls via mock before/after (expect O(N+M) not O((N+M)×N)).

#### P0-4 — `Class.forName` Folia detection every tick
- **Where:**
  - `NametagService.java:58` (inside `updateAllNametags`)
  - `NametagService.java:157` (inside `toggleSelfView` — less hot but same fix)
- **What:** Reflection probe for `io.papermc.paper.threadedregions.RegionizedServer` on every tick / every toggle.
- **Fix:**
  ```java
  private static final boolean FOLIA = (() -> {
      try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; }
      catch (ClassNotFoundException | NoClassDefFoundError e) { return false; }
  })();
  ```
  Same pattern may be centralized in one utility (e.g. `PlatformDetector.FOLIA`) reused by `EligiusNametag.onEnable/startTask`, `NametagService`, `RedisAdapter`.
- **Risk:** none (classpath cannot change at runtime).
- **Tests:** suite green; behavior on Paper vs Folia unchanged.

#### P0-5 — Vault `getPrimaryGroup` every tick per visible target
- **Where:** `NametagService.java:130` → `PaperPlatformAdapter.getPrimaryGroup(UUID)` (282–286) and `getOwnerPrimaryGroup` (294–306)
- **What:** Vault/LuckPerms primary-group lookup on the main thread for every target every tick. LuckPerms can be non-trivial (cached usually, but API boundary cost + possible cache misses on group change).
- **Fix:**
  - `Map<UUID, CachedGroup { String group; long expiresAt; }>` in the adapter (or service).
  - TTL ~5000 ms; invalidate on `PlayerJoinEvent` (optional), and expose `invalidateGroup(UUID)` if a future group-change hook appears.
  - For offline owners (`getOwnerPrimaryGroup`): same cache keyed by owner UUID; `getOfflinePlayer` is itself a lookup — cache it too.
- **Risk:** group changes visible up to TTL late (5s) — acceptable for nametag formatting.
- **Tests:** second call within TTL does not hit Vault (mock `Permission` provider, verify `getPrimaryGroup` invoked once).

---

### 🟠 P1 — High (measurable, cheap)

#### P1-1 — `getLocation()` only for distance math
- **Where:** `PaperPlatformAdapter.java:131`
- **Fix:** use `entity.getX()/getY()/getZ()` (or snapshot coords from P0-3). Eliminates 2 `Location` allocations per pair.
- Overlaps P0-3 — implement together.

#### P1-2 — Repeated `Bukkit.getPluginManager().getPlugin("PlaceholderAPI")` inside one parse
- **Where:** `PaperPlatformAdapter.java:182`, `:220`, `:225`
- **Fix:** resolve `boolean papiPresent` once per `parsePlaceholders` (local var) **or** cache in a field refreshed on reload/onEnable.
- **Risk:** none.

#### P1-3 — `new HashSet<>(viewers)` per line per tick in renderer
- **Where:** `BukkitNametagRenderer.java:166` (inside the per-line loop of `renderNametag`)
- **Fix:** hoist `Set<UUID> viewerSet = new HashSet<>(viewers);` **once** before the `for (int i = 0; i < lines.size(); i++)` loop (line ~124).
- **Risk:** none (same set reused immutably for the call).

#### P1-4 — Folia branch resolves plugin + entity per target per tick
- **Where:** `NametagService.java:60-73`
- **Fix:** after P0-4, also cache `Plugin plugin = Bukkit.getPluginManager().getPlugin("EligiusNametag")` once per `updateAllNametags` call (not per target).
- **Risk:** none.

#### P1-5 — Distance boundary flicker (show/hide packet ping-pong)
- **Where:** `NametagService.java:119-126` gate `distanceSquared <= maxDistanceSquared`
- **What:** viewers hovering at exactly `view_distance` can flip valid/invalid across ticks → repeated `showEntity`/`hideEntity` packets.
- **Fix:** hysteresis — track per (target,viewer) whether currently shown; hide when `d2 > (vd+margin)²`, show when `d2 <= vd²` (margin ≈ 2 blocks). Requires small state in renderer or service (it already has `lineSpawnedViewers` — can read “is shown” from renderer if exposed, or keep a service-side shown-set).
- **Risk:** low; tags may appear up to `margin` blocks beyond config while leaving — document in config comment.
- **Tests:** unit: viewer at vd+1 after being shown stays shown until vd+2.

---

### 🟡 P2 — Medium (deuda, no urgente)

| ID | Where | Issue | Direction |
|----|-------|-------|-----------|
| **P2-1** | `PaperPlatformAdapter.java:267-276` | Legacy `&`→MiniMessage chain allocates ~40 strings when PAPI returns legacy colors | Keep the `contains(&)` gate; optional: precompile a single `Pattern` for `&[0-9a-fk-or]` and do one-pass replace |
| **P2-2** | `PaperPlatformAdapter.getTamedMobs` (85–98) | Revalidates every cached UUID every tick (`getEntity`, `isDead`, `isValid`) | Acceptable (event-driven add/remove already exists). Only optimize if pet count is huge (1000+) |
| **P2-3** | `BukkitNametagRenderer.clearViewer` (291–298) | On quit, iterates every display’s viewer sets | Invert index `viewerId → Set<displayId>` only if profiling shows pain |
| **P2-4** | Renderer design | 1 real `TextDisplay` entity per line ⇒ entity count = `(N+M)×lines` | Packet-based holograms would remove entities — major redesign, out of scope |
| **P2-5** | `config.yml` `interval` | Default 0.5s ⇒ full scan 2×/s | Document as perf knob in config comment + docs (higher interval = less CPU, tag updates slower) |
| **P2-6** | `EligiusNametag.reloadPlugin` (176–183) | `Bukkit.getScheduler().cancelTask` on Folia is wrong (known; fixed on branch `feature/fix-critical-bugs` as `FoliaSchedulerAdapter.cancelCurrentTask`) | Merge that fix when touching Folia scheduling; **also clear P0-2 parse cache on reload** |
| **P2-7** | `AnimationManager.tick` | Cheap (modulo per animation) — OK | No action |

---

### ✅ Confirmed OK (do not regress)

- Diff-based show/hide (`lineSpawnedViewers` / `lineHiddenViewers`)
- Component-equality text updates (`lineJsonCache`)
- Guarded vanilla nametag suppression
- Async DB/Redis in `toggleSelfView`
- Async update checker
- `distanceSquared` usage
- Concurrent maps for Folia thread-safety
- `getTamedMobs` eviction of dead/unnamed (correctness fix from v1.6.1)

---

## 3. Batches (execution plan)

### Batch A — Micro-fixes (safe, ~30–60 min, no behavior change)

| Item | Files |
|------|-------|
| P0-1 static patterns | `PaperPlatformAdapter.java` |
| P0-4 static FOLIA flag | `NametagService.java` (+ optional shared detector) |
| P1-1 coordinate math (if done without full snapshot — else defer to B) | `PaperPlatformAdapter.java` |
| P1-2 PAPI presence local/field | `PaperPlatformAdapter.java` |
| P1-3 hoist `viewerSet` | `BukkitNametagRenderer.java:166` → before line-loop |
| P1-4 cache plugin ref per tick | `NametagService.java` |

**Exit criteria:** `./gradlew test` green (23 tests), `./gradlew build` green, no public API change.

### Batch B — Per-tick entity snapshot (P0-3 + P1-1)

1. Design `PlatformPort` additions (domain-safe types):
   ```java
   // sketch — final names up to implementer
   interface ViewerSnapshot { UUID id(); String world(); double x(); double y(); double z(); boolean canSee(UUID target); }
   interface TargetSnapshot { UUID id(); String world(); double x(); double y(); double z(); }
   List<ViewerSnapshot> snapshotViewers();
   TargetSnapshot resolveTarget(UUID id); // null if gone
   ```
2. Rebuild `updateAllNametags`/`updateTarget` to use snapshots; drop per-pair `isSameWorld`/`getDistanceSquared`/`canViewerSeeTarget` calls **or** keep old methods for compatibility but stop using them in the hot loop (then deprecate).
3. Preserve exact predicate order/logic:
   - self-viewer branch first (permission + selfViewers set)
   - `!isHidden && sameWorld && dist² ≤ max² && canSee`
4. Folia path: snapshots must be taken on the appropriate thread (global region read of online players is OK on Folia? — **verify**: if unsafe, take snapshot inside each entity’s scheduler or use `Bukkit.getOnlinePlayers()` which Paper documents as safe-ish; flag for implementer to check Paper 26.3 Folia threading rules before merging).

**Exit criteria:** all existing visibility tests green + new snapshot tests; manual QA on test server (view distance 5 still works).

### Batch C — Parsed-line cache (P0-2) + group cache (P0-5)

1. **Group cache** first (simpler): TTL 5s map in `PaperPlatformAdapter`.
2. **Parse cache** in `NametagService` (domain layer owns “what text to show”):
   - Key: `(targetId, lineIndex, rawLine, group, animationEpoch, stateHash)`
   - `stateHash`: e.g. `Objects.hash(name, displayName, healthInt, maxHealthInt, customName, ownerName, isBaby)` — cheap fields only; **do not** include full PAPI output.
   - TTL: N ticks (config `cache_ticks`, default 10) **or** re-parse if raw line contains `%` and TTL expired.
   - Clear on `reloadPlugin()` (add `nametagService.clearCaches()`).
   - Clear per-target on `destroyNametag`/quit/death (avoid leaks: use `ConcurrentHashMap` keyed by target, remove entry in `cleanup`/`clearTargetCache(UUID)` hooked to existing destroy paths).
3. Config: `performance.cache_ticks: 10` in `config.yml` (document in comment).

**Exit criteria:** unit tests for hit/miss/invalidations; QA: health updates still appear ≤1 interval later; rainbow animation still animates.

### Batch D — Hysteresis (P1-5)

1. Use renderer’s existing “currently shown” sets (expose `boolean isShownTo(UUID target, UUID viewer)` on `NametagRendererPort` if needed).
2. Show threshold: `d2 <= vd²`; hide threshold: `d2 > (vd+2)²`.

**Exit criteria:** unit test for border stability.

### Batch E (optional, later)

- P2-1 one-pass legacy color convert.
- P2-5 docs for `interval`.
- P2-6 merge Folia cancel-task fix from `feature/fix-critical-bugs` (separate PR).

---

## 4. Suggested order & PR slicing

| PR | Content | Risk |
|----|---------|------|
| **PR-1** | Batch A | Very low |
| **PR-2** | Batch B | Medium (core loop) |
| **PR-3** | Batch C (groups + parse cache + config) | Medium (staleness semantics) |
| **PR-4** | Batch D | Low |

One batch per PR (skill: separate refactors; keep diffs reviewable <300 lines where possible).

---

## 5. Verification plan (every batch)

```bash
# local
./gradlew test
./gradlew build
```

**Manual QA on test server (`run_test`, Paper 26.3):**
1. `view_distance: 5` — show/hide at boundary still works (and after Batch D, no flicker at 5–6 blocks).
2. Named camel / tamed horse tags still render (regression from v1.6.1).
3. Mount/dismount still hides/restores tag.
4. `/enametag reload` — templates update immediately (cache cleared).
5. Health in tag updates after damage (≤ interval + TTL).
6. Rainbow `<anim:rainbow>` still cycles.

**Perf check (before/after each batch):**
- `/enametag debug` already prints `Tracking Loop Latency (ms)` — record on test server with same population.
- Optional: temporarily log `parsePlaceholders` invocation count per tick (remove before merge) to prove Batch C reduction.

---

## 6. AI agent prompts (copy-paste)

Use these when resuming work. Adjust paths if repo layout changes.

### Prompt — Batch A

```text
Repo: EligiusNametag (Java 21, Paper plugin, Gradle). Read docs/PERFORMANCE_AUDIT.md first.

Implement ONLY Batch A (micro-fixes). Do not touch other batches.

1. PaperPlatformAdapter: replace both Pattern.compile(...) in parsePlaceholders with
   private static final Pattern IMG_PATTERN / ANIM_PATTERN (same regex strings).
2. PaperPlatformAdapter: resolve PlaceholderAPI presence once per parsePlaceholders
   call (local boolean) — three call sites currently call getPlugin("PlaceholderAPI").
3. NametagService: replace per-tick Class.forName Folia detection with a private
   static final boolean FOLIA initialized in a static block; reuse in updateAllNametags
   and toggleSelfView.
4. NametagService: hoist Bukkit.getPluginManager().getPlugin("EligiusNametag") to a
   local variable once per updateAllNametags in the Folia branch.
5. BukkitNametagRenderer.renderNametag: move `Set<UUID> viewerSet = new HashSet<>(viewers);`
   out of the per-line for-loop (compute once per method call).

Constraints:
- No behavior changes. No new config keys. No API changes.
- Follow existing code style (no new comments unless non-obvious).
- Run: ./gradlew test && ./gradlew build — must be green (23 tests).
- Do NOT commit or push until the user confirms.
Return: summary of edits + test results.
```

### Prompt — Batch B

```text
Repo: EligiusNametag. Read docs/PERFORMANCE_AUDIT.md sections 1, P0-3, P1-1, Batch B.

Implement the per-tick viewer/target snapshot to remove O((N+M)×N) entity lookups
and Location allocations from the visibility loop.

1. Add domain-safe snapshot types + PlatformPort methods (see sketch in the doc).
   Implement in PaperPlatformAdapter using Bukkit, but do not leak Bukkit types
   into domain.service.
2. Rewrite NametagService.updateAllNametags / updateTarget to use one viewer
   snapshot per tick and one target resolution per updateTarget call.
3. Distance via double math on snapshot coords (no getLocation()).
4. Keep exact visibility semantics (self-viewer, hidden, same world, distance,
   canSee) — existing NametagServiceTest must pass unmodified.
5. Check Folia: if snapshotting Bukkit.getOnlinePlayers() from the global task is
   unsafe on Paper 26.3 Folia, document and take snapshot via the safest available
   API; do not guess — verify against Paper javadoc in your research.

Constraints: ./gradlew test && ./gradlew build green. No commits/push until user confirms.
Return: API added, predicate mapping table (old→new), test results, Folia note.
```

### Prompt — Batch C

```text
Repo: EligiusNametag. Read docs/PERFORMANCE_AUDIT.md P0-2, P0-5, Batch C.

Implement:
A) Group cache in PaperPlatformAdapter: Map<UUID, CachedGroup> with 5s TTL for
   getPrimaryGroup(UUID) and getOwnerPrimaryGroup(UUID). Invalidate on player join
   if an easy hook exists; otherwise TTL only.
B) Parsed-line cache in NametagService (domain owns text):
   - key: targetId + raw line + group + animation epoch + cheap state hash
     (health int, max health int, name, custom name, owner, baby flag)
   - TTL from new config key performance.cache_ticks (default 10); if raw line
     contains '%', treat as PAPI-sensitive and refresh at TTL
   - clear on reload (expose nametagService.clearCaches(), call from reloadPlugin)
   - clear per target on destroy/quit paths (avoid leak)
C) config.yml: add performance.cache_ticks with a short comment.

Tests (required):
- second identical render does not call platform.parsePlaceholders again (Mockito count)
- reload clears cache
- animation epoch change busts cache for <anim:> lines
- group TTL: Vault called once within TTL

./gradlew test && ./gradlew build green. No commits/push until user confirms.
Return: invalidation matrix + test list + any PAPI staleness caveats.
```

### Prompt — Batch D

```text
Repo: EligiusNametag. Read docs/PERFORMANCE_AUDIT.md P1-5, Batch D.

Add show/hide hysteresis: show when d2 <= vd^2, hide only when d2 > (vd+2)^2.
Use renderer's lineSpawnedViewers (expose isShownTo on NametagRendererPort if needed).
Add unit test: viewer shown at vd stays shown at vd+1, hides at vd+2+.
./gradlew test && ./gradlew build green. No commits/push until user confirms.
```

### Prompt — re-audit (only if code drifts)

```text
Read docs/PERFORMANCE_AUDIT.md. Re-verify each P0/P1 finding against current code
(read the cited files/lines). Update the doc: mark FIXED (PR link) or still-open,
refresh line numbers if moved. Do not re-derive findings that are already documented.
```

---

## 7. Open questions for the implementer

1. **Folia snapshot safety (Batch B):** confirm reading `Bukkit.getOnlinePlayers()` + entity coords from the global-region task on Paper 26.3 Folia. If unsafe, snapshot must move into region schedulers — flag before coding.
2. **PAPI staleness (Batch C):** confirm TTL default (10 ticks) acceptable for health placeholders; or choose the “always re-parse lines containing `%`” variant.
3. **Hysteresis margin (Batch D):** 2 blocks OK with default `view_distance` docs?
4. **Config surface:** is a new `performance.*` section acceptable, or keep knobs undocumented constants for now?

---

## 8. Traceability

| Finding | Status (audit date 2026-11-02) |
|---------|-------------------------------|
| P0-1 (static patterns) | **FIXED** — pre-existing on develop, verified in re-audit (branch `feature/perf-optimization`) |
| P0-2 (parsed-line cache) | **FIXED in `feature/perf-optimization`** (Batch C commit `87c745f`) — `NametagService.parseLine` + `performance.cache_ticks` |
| P0-3 (per-tick snapshots) | **FIXED in `feature/perf-optimization`** (Batch B commit `ae760ed`) — `PlatformPort.snapshotViewers`/`resolveTarget` |
| P0-4 (cached Folia detection) | **FIXED** — merged from `feature/fix-critical-bugs` (`024281e`) into `feature/perf-optimization` |
| P0-5 (group TTL cache) | **FIXED in `feature/perf-optimization`** (Batch C commit `87c745f`) — 5s TTL in `PaperPlatformAdapter` |
| P1-1 (no `getLocation()` in distance) | **FIXED in `feature/perf-optimization`** (Batch B) — pure double math on snapshots |
| P1-2 (single PAPI presence check) | **FIXED** — pre-existing `hasPapi` field on develop, verified |
| P1-3 (hoist `viewerSet`) | **FIXED in `feature/perf-optimization`** (Batch A commit `79e01f1`) |
| P1-4 (plugin ref per tick) | **FIXED** — merged from `feature/fix-critical-bugs` (plugin cached at construction) |
| P1-5 (hysteresis) | **FIXED in `feature/perf-optimization`** (Batch D commit `cd10b2a`) — 2-block margin |
| P2-1 … P2-5, P2-7 | **Open** (deferred Batch E / non-urgent) |
| P2-6 (Folia cancel-task on reload) | **FIXED** — merged from `feature/fix-critical-bugs` (`f79bdff` → `cancelActiveTask`) |
| GitHub perf issues | None open (#46 fixed on this branch lineage, #22/#23 are features) |

**Branch:** all batches A–D executed on a single branch `feature/perf-optimization` (per user instruction), based on `develop` + `origin/master` + merged `origin/feature/fix-critical-bugs`.

**Verification (2026-11-02):** `./gradlew clean build` green — 35 tests, 0 failures (was 23 before; +12 new: ParseCacheTest 6, GroupCacheTest 2, HysteresisTest 4).

When a batch merges, update this table: `FIXED in <PR> (date)`.

---

*End of audit. Resume work from §3 batches using §6 prompts — no need to re-walk the codebase.*
