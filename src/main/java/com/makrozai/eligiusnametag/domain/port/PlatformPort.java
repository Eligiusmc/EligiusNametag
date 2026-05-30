package com.makrozai.eligiusnametag.domain.port;

import java.util.List;
import java.util.UUID;

public interface PlatformPort {
    void prepareTick();
    void endTick();
    List<UUID> getOnlinePlayers();
    List<UUID> getTamedMobs();
    boolean canViewerSeeTarget(UUID viewerId, UUID targetId);
    boolean isSameWorld(UUID a, UUID b);
    boolean isGloballyHidden(UUID targetId);
    boolean hasCustomName(UUID targetId);
    void disableVanillaNametag(UUID targetId);
    net.kyori.adventure.text.Component parsePlaceholders(UUID targetId, String text);
    String getPrimaryGroup(UUID targetId);
    String getOwnerPrimaryGroup(UUID targetId);
    boolean hasPermission(UUID targetId, String permission);
}
