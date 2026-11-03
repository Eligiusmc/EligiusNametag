package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import com.makrozai.eligiusnametag.domain.port.SyncPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.makrozai.eligiusnametag.domain.port.DatabasePort;

public class NametagService {
    private final ConfigPort config;
    private final PlatformPort platform;
    private final NametagRendererPort renderer;
    private final DatabasePort database;
    private final SyncPort syncPort;
    private final Set<UUID> selfViewers = ConcurrentHashMap.newKeySet();
    private long lastTickDuration = 0;

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
        double maxDistanceSquared = (double) viewDistance * viewDistance;

        if (isFolia) {
            for (PlatformPort.ViewerSnapshot viewer : viewers) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(viewer.id());
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(viewer.id(), viewers, true, maxDistanceSquared), null, 1L);
                }
            }
            for (UUID targetId : tamedMobs) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(targetId);
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(targetId, viewers, false, maxDistanceSquared), null, 1L);
                }
            }
        } else {
            for (PlatformPort.ViewerSnapshot viewer : viewers) {
                updateTarget(viewer.id(), viewers, true, maxDistanceSquared);
            }
            for (UUID targetId : tamedMobs) {
                updateTarget(targetId, viewers, false, maxDistanceSquared);
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

    private void updateTarget(UUID targetId, List<PlatformPort.ViewerSnapshot> viewers, boolean isPlayer, double maxDistanceSquared) {
        boolean isHidden = platform.isGloballyHidden(targetId);

        if (!isPlayer) {
            if (!config.isTamedMobsEnabled() || (!config.isTamedMobsShowUnnamed() && !platform.hasCustomName(targetId))) {
                isHidden = true;
            }
        }

        // Resolve the target once per update instead of once per viewer pair.
        PlatformPort.TargetSnapshot target = platform.resolveTarget(targetId);

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
            // !isHidden → same world → dist² ≤ max² → canSee.
            // Mob targets skip canViewerSeeTarget: its only checks (entity alive,
            // same world) are already guaranteed by resolveTarget + world equality.
            // Player targets still need it for vanilla canSee (spectator/invisibility).
            if (target != null
                    && !isHidden
                    && viewer.world().equals(target.world())
                    && distanceSquared(viewer, target) <= maxDistanceSquared
                    && (!isPlayer || platform.canViewerSeeTarget(viewerId, targetId))) {
                validViewers.add(viewerId);
            } else {
                invalidViewers.add(viewerId);
            }
        }

        if (!validViewers.isEmpty()) {
            String group = isPlayer ? platform.getPrimaryGroup(targetId) : platform.getOwnerPrimaryGroup(targetId);
            List<String> template = isPlayer ? config.getPlayerNametagTemplate(group) : config.getTamedMobNametagTemplate(group);
            if (template == null || template.isEmpty()) {
                renderer.hideNametag(targetId, validViewers);
            } else {
                List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
                for (String line : template) {
                    lines.add(platform.parsePlaceholders(targetId, line));
                }
                platform.disableVanillaNametag(targetId);
                float yOffset = (float) config.getYOffset();
                renderer.renderNametag(targetId, lines, validViewers, yOffset);
            }
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

    public void cleanup() {
        renderer.destroyAll();
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
