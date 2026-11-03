package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import com.makrozai.eligiusnametag.domain.port.SyncPort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.makrozai.eligiusnametag.domain.port.DatabasePort;

public class NametagService {
    /** Extra blocks beyond view_distance before an already-visible tag is hidden (P1-5). */
    private static final double HYSTERESIS_MARGIN = 2.0;

    /** Cache key part independent of volatile state; one entry per (line, group) per target. */
    private record LineCacheKey(String rawLine, String group) {}

    private record CachedLine(
            net.kyori.adventure.text.Component component,
            long stateHash,
            long animEpoch,
            long cachedAtMs) {}

    private final ConfigPort config;
    private final PlatformPort platform;
    private final NametagRendererPort renderer;
    private final DatabasePort database;
    private final SyncPort syncPort;
    private final Set<UUID> selfViewers = ConcurrentHashMap.newKeySet();
    private long lastTickDuration = 0;

    // Parsed-line cache (P0-2): keyed per target → (raw line, group).
    // Valid while state hash + animation epoch match and within the TTL
    // derived from performance.cache_ticks (1 tick = 50ms). Entries for
    // targets that leave the active set are swept each tick (leak guard).
    private final Map<UUID, Map<LineCacheKey, CachedLine>> parseCache = new ConcurrentHashMap<>();

    // Viewers each target was actually rendered to on the previous tick (P1-5).
    // Drives the hysteresis threshold; swept alongside the parse cache.
    private final Map<UUID, Set<UUID>> shownViewers = new ConcurrentHashMap<>();

    // Cached once at construction — Folia presence never changes at runtime.
    private final boolean isFolia;
    private final org.bukkit.plugin.Plugin plugin;

    public NametagService(ConfigPort config, PlatformPort platform, NametagRendererPort renderer, DatabasePort database, SyncPort syncPort) {
        this.config = config;
        this.platform = platform;
        this.renderer = renderer;
        this.database = database;
        this.syncPort = syncPort;

        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
        this.isFolia = foliaDetected;
        org.bukkit.plugin.PluginManager pm = (org.bukkit.Bukkit.getServer() != null) ? org.bukkit.Bukkit.getPluginManager() : null;
        this.plugin = (pm != null) ? pm.getPlugin("EligiusNametag") : null;

        this.selfViewers.addAll(database.getAllPlayersWithViewSelf());
        
        // Listen to cross-server Redis updates
        if (syncPort != null) {
            syncPort.subscribe((uuid, state) -> {
                if (state) {
                    selfViewers.add(uuid);
                } else {
                    selfViewers.remove(uuid);
                }
            });
        }
    }

    public void updateAllNametags() {
        long startTick = System.currentTimeMillis();
        platform.prepareTick();

        // One viewer pass per tick: world + coordinates for every online player,
        // so the per-target loop below does pure double math instead of
        // Bukkit.getEntity/getLocation + Location allocations per pair (P0-3/P1-1).
        List<PlatformPort.ViewerSnapshot> viewers = platform.snapshotViewers();
        List<UUID> tamedMobs = platform.getTamedMobs();

        // View-distance gate is config-driven and computed once per tick (issue #45).
        int viewDistance = config.getViewDistance();

        sweepCaches(viewers, tamedMobs);
        if (isFolia) {
            for (PlatformPort.ViewerSnapshot viewer : viewers) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(viewer.id());
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(viewer.id(), viewers, true, viewDistance), null, 1L);
                }
            }
            for (UUID targetId : tamedMobs) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(targetId);
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(targetId, viewers, false, viewDistance), null, 1L);
                }
            }
        } else {
            for (PlatformPort.ViewerSnapshot viewer : viewers) {
                updateTarget(viewer.id(), viewers, true, viewDistance);
            }
            for (UUID targetId : tamedMobs) {
                updateTarget(targetId, viewers, false, viewDistance);
            }
        }

        platform.endTick();
        lastTickDuration = System.currentTimeMillis() - startTick;
    }

    public long getLastTickDuration() {
        return lastTickDuration;
    }

    public int getActiveEntityCount() {
        return renderer.getActiveEntityCount();
    }

    public int getSelfViewersCount() {
        return selfViewers.size();
    }

    private void updateTarget(UUID targetId, List<PlatformPort.ViewerSnapshot> viewers, boolean isPlayer, int viewDistance) {
        boolean isHidden = platform.isGloballyHidden(targetId);

        if (!isPlayer) {
            if (!config.isTamedMobsEnabled() || (!config.isTamedMobsShowUnnamed() && !platform.hasCustomName(targetId))) {
                isHidden = true;
            }
        }

        // Resolve the target once per update instead of once per viewer pair.
        PlatformPort.TargetSnapshot target = platform.resolveTarget(targetId);

        // P1-5 hysteresis: show at ≤ view_distance, only hide beyond
        // view_distance + margin, so viewers hovering at the boundary don't
        // flip show/hide (and its packets) every tick.
        double showDistSq = (double) viewDistance * viewDistance;
        double hideDistSq = (viewDistance + HYSTERESIS_MARGIN) * (viewDistance + HYSTERESIS_MARGIN);
        Set<UUID> shownLastTick = shownViewers.getOrDefault(targetId, java.util.Collections.emptySet());

        List<UUID> validViewers = new ArrayList<>();
        List<UUID> invalidViewers = new ArrayList<>();

        for (PlatformPort.ViewerSnapshot viewer : viewers) {
            UUID viewerId = viewer.id();
            if (viewerId.equals(targetId)) {
                if (!isPlayer || !selfViewers.contains(viewerId) || !platform.hasPermission(viewerId, "eligiusnametag.viewself")) {
                    invalidViewers.add(viewerId);
                    continue;
                }
            }

            // Predicate order preserved from the pre-snapshot loop:
            // !isHidden → same world → dist² ≤ threshold → canSee.
            // Mob targets skip canViewerSeeTarget: its only checks (entity alive,
            // same world) are already guaranteed by resolveTarget + world equality.
            // Player targets still need it for vanilla canSee (spectator/invisibility).
            if (target != null
                    && !isHidden
                    && viewer.world().equals(target.world())) {
                double distSq = distanceSquared(viewer, target);
                double threshold = shownLastTick.contains(viewerId) ? hideDistSq : showDistSq;
                if (distSq <= threshold
                        && (!isPlayer || platform.canViewerSeeTarget(viewerId, targetId))) {
                    validViewers.add(viewerId);
                } else {
                    invalidViewers.add(viewerId);
                }
            } else {
                invalidViewers.add(viewerId);
            }
        }

        if (!validViewers.isEmpty()) {
            String group = isPlayer ? platform.getPrimaryGroup(targetId) : platform.getOwnerPrimaryGroup(targetId);
            List<String> template = isPlayer ? config.getPlayerNametagTemplate(group) : config.getTamedMobNametagTemplate(group);
            if (template == null || template.isEmpty()) {
                renderer.hideNametag(targetId, validViewers);
                shownViewers.remove(targetId);
            } else {
                int cacheTicks = config.getCacheTicks();
                long stateHash = cacheTicks > 0 ? platform.getStateHash(targetId) : 0L;
                List<net.kyori.adventure.text.Component> lines = new ArrayList<>(template.size());
                for (String line : template) {
                    lines.add(parseLine(targetId, line, group, cacheTicks, stateHash));
                }
                platform.disableVanillaNametag(targetId);
                float yOffset = (float) config.getYOffset();
                renderer.renderNametag(targetId, lines, validViewers, yOffset);
                shownViewers.put(targetId, new HashSet<>(validViewers));
            }
        } else {
            shownViewers.remove(targetId);
        }

        if (!invalidViewers.isEmpty()) {
            renderer.hideNametag(targetId, invalidViewers);
        }
    }

    private static double distanceSquared(PlatformPort.ViewerSnapshot viewer, PlatformPort.TargetSnapshot target) {
        double dx = viewer.x() - target.x();
        double dy = viewer.y() - target.y();
        double dz = viewer.z() - target.z();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Returns a cached parsed line when the cheap state hash and animation epoch
     * still match and the TTL (performance.cache_ticks × 50ms) has not expired;
     * otherwise runs the full placeholder pipeline and stores the result.
     * PAPI {@code %…%} lines are cached too but their staleness is bounded by
     * the TTL (audit option 3): at the default 10 ticks behaviour is unchanged.
     */
    private net.kyori.adventure.text.Component parseLine(
            UUID targetId, String rawLine, String group, int cacheTicks, long stateHash) {
        if (cacheTicks <= 0) {
            return platform.parsePlaceholders(targetId, rawLine);
        }

        // Only <anim:> lines depend on the frame epoch; other lines would be
        // needlessly invalidated every animation tick.
        long animEpoch = rawLine.contains("<anim:") ? platform.getAnimationEpoch() : 0L;
        long now = System.currentTimeMillis();
        long ttlMs = cacheTicks * 50L;

        Map<LineCacheKey, CachedLine> targetCache =
                parseCache.computeIfAbsent(targetId, k -> new ConcurrentHashMap<>());
        LineCacheKey key = new LineCacheKey(rawLine, group);
        CachedLine hit = targetCache.get(key);
        if (hit != null
                && hit.stateHash() == stateHash
                && hit.animEpoch() == animEpoch
                && now - hit.cachedAtMs() < ttlMs) {
            return hit.component();
        }

        net.kyori.adventure.text.Component parsed = platform.parsePlaceholders(targetId, rawLine);
        targetCache.put(key, new CachedLine(parsed, stateHash, animEpoch, now));
        return parsed;
    }

    /** Drop cache entries for targets that are no longer rendered (leak guard). */
    private void sweepCaches(List<PlatformPort.ViewerSnapshot> viewers, List<UUID> tamedMobs) {
        Set<UUID> active = new HashSet<>(tamedMobs);
        for (PlatformPort.ViewerSnapshot v : viewers) {
            active.add(v.id());
        }
        if (!parseCache.isEmpty()) {
            parseCache.keySet().removeIf(id -> !active.contains(id));
        }
        if (!shownViewers.isEmpty()) {
            shownViewers.keySet().removeIf(id -> !active.contains(id));
        }
    }

    /** Full invalidation — called on /enametag reload so templates re-parse immediately. */
    public void clearCaches() {
        parseCache.clear();
        shownViewers.clear();
    }

    public void cleanup() {
        renderer.destroyAll();
        parseCache.clear();
        shownViewers.clear();
    }

    public boolean toggleSelfView(UUID playerId) {
        if (selfViewers.contains(playerId)) {
            selfViewers.remove(playerId);
            if (isFolia) {
                org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> database.setPlayerViewSelf(playerId, false));
            } else {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.setPlayerViewSelf(playerId, false));
            }
            if (syncPort != null) syncPort.publishSelfViewUpdate(playerId, false);
            return false;
        } else {
            selfViewers.add(playerId);
            if (isFolia) {
                org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> database.setPlayerViewSelf(playerId, true));
            } else {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.setPlayerViewSelf(playerId, true));
            }
            if (syncPort != null) syncPort.publishSelfViewUpdate(playerId, true);
            return true;
        }
    }
}
