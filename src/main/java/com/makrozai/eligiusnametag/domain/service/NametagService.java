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

    public NametagService(ConfigPort config, PlatformPort platform, NametagRendererPort renderer, DatabasePort database, SyncPort syncPort) {
        this.config = config;
        this.platform = platform;
        this.renderer = renderer;
        this.database = database;
        this.syncPort = syncPort;
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
        
        List<UUID> onlinePlayers = platform.getOnlinePlayers();
        List<UUID> tamedMobs = platform.getTamedMobs();

        boolean isFolia = false;
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); isFolia = true; } catch (ClassNotFoundException e) {}

        if (isFolia) {
            org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EligiusNametag");
            for (UUID targetId : onlinePlayers) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(targetId);
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(targetId, onlinePlayers, true), null, 1L);
                }
            }
            for (UUID targetId : tamedMobs) {
                org.bukkit.entity.Entity target = org.bukkit.Bukkit.getEntity(targetId);
                if (target != null) {
                    target.getScheduler().execute(plugin, () -> updateTarget(targetId, onlinePlayers, false), null, 1L);
                }
            }
        } else {
            for (UUID targetId : onlinePlayers) {
                updateTarget(targetId, onlinePlayers, true);
            }
            for (UUID targetId : tamedMobs) {
                updateTarget(targetId, onlinePlayers, false);
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

    private void updateTarget(UUID targetId, List<UUID> potentialViewers, boolean isPlayer) {
        boolean isHidden = platform.isGloballyHidden(targetId);
        
        if (!isPlayer) {
            if (!config.isTamedMobsEnabled() || (!config.isTamedMobsShowUnnamed() && !platform.hasCustomName(targetId))) {
                isHidden = true;
            }
        }

        List<UUID> validViewers = new ArrayList<>();
        List<UUID> invalidViewers = new ArrayList<>();

        for (UUID viewerId : potentialViewers) {
            if (viewerId.equals(targetId)) {
                if (!isPlayer || !selfViewers.contains(viewerId) || !platform.hasPermission(viewerId, "eligiusnametag.viewself")) {
                    invalidViewers.add(viewerId);
                    continue;
                }
            }
            
            if (!isHidden && platform.isSameWorld(targetId, viewerId) && platform.canViewerSeeTarget(viewerId, targetId)) {
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

    public void cleanup() {
        renderer.destroyAll();
    }

    public boolean toggleSelfView(UUID playerId) {
        org.bukkit.plugin.Plugin plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("EligiusNametag");
        boolean isFolia = false;
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); isFolia = true; } catch (ClassNotFoundException e) {}

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
