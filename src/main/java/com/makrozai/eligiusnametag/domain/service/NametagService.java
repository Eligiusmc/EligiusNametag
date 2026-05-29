package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final Set<UUID> selfViewers = ConcurrentHashMap.newKeySet();

    public NametagService(ConfigPort config, PlatformPort platform, NametagRendererPort renderer, DatabasePort database) {
        this.config = config;
        this.platform = platform;
        this.renderer = renderer;
        this.database = database;
        this.selfViewers.addAll(database.getAllPlayersWithViewSelf());
    }

    public void updateAllNametags() {
        List<UUID> onlinePlayers = platform.getOnlinePlayers();
        List<UUID> tamedMobs = config.isTamedMobsEnabled() ? platform.getTamedMobs() : new ArrayList<>();

        for (UUID targetId : onlinePlayers) {
            updateTarget(targetId, onlinePlayers, true);
        }

        for (UUID targetId : tamedMobs) {
            updateTarget(targetId, onlinePlayers, false);
        }
    }

    private void updateTarget(UUID targetId, List<UUID> potentialViewers, boolean isPlayer) {
        boolean isHidden = platform.isGloballyHidden(targetId);
        
        if (!isPlayer && !config.isTamedMobsShowUnnamed() && !platform.hasCustomName(targetId)) {
            isHidden = true;
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
                List<String> lines = new ArrayList<>();
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
        if (selfViewers.contains(playerId)) {
            selfViewers.remove(playerId);
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                org.bukkit.Bukkit.getPluginManager().getPlugin("EligiusNametag"), 
                () -> database.setPlayerViewSelf(playerId, false)
            );
            return false;
        } else {
            selfViewers.add(playerId);
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                org.bukkit.Bukkit.getPluginManager().getPlugin("EligiusNametag"), 
                () -> database.setPlayerViewSelf(playerId, true)
            );
            return true;
        }
    }
}
