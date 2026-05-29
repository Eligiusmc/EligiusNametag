package com.makrozai.eligiusnametag.domain.port;

import java.util.List;
import java.util.UUID;

public interface PlatformPort {
    List<UUID> getOnlinePlayers();
    List<UUID> getTamedMobs();
    boolean canViewerSeeTarget(UUID viewerId, UUID targetId);
    boolean isSameWorld(UUID a, UUID b);
    boolean isGloballyHidden(UUID targetId);
    boolean hasCustomName(UUID targetId);
    void disableVanillaNametag(UUID targetId);
    String parsePlaceholders(UUID targetId, String text);
    String getPrimaryGroup(UUID targetId);
    String getOwnerPrimaryGroup(UUID targetId);
    boolean hasPermission(UUID targetId, String permission);
}
