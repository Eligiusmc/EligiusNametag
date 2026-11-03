package com.makrozai.eligiusnametag.domain.port;

import java.util.List;
import java.util.UUID;

public interface PlatformPort {
    /**
     * Immutable per-tick view of an online viewer. World is the world name
     * (unique per server, so name equality ≡ world equality) and coordinates
     * are plain doubles so the domain can compute distance without Bukkit.
     */
    record ViewerSnapshot(UUID id, String world, double x, double y, double z) {}

    /** Immutable per-tick view of a render target; null when the entity is gone. */
    record TargetSnapshot(UUID id, String world, double x, double y, double z) {}

    void prepareTick();
    void endTick();
    List<UUID> getOnlinePlayers();
    List<ViewerSnapshot> snapshotViewers();
    TargetSnapshot resolveTarget(UUID id);
    List<UUID> getTamedMobs();
    void addTamedMob(UUID uuid);
    void removeTamedMob(UUID uuid);
    boolean canViewerSeeTarget(UUID viewerId, UUID targetId);
    boolean isSameWorld(UUID a, UUID b);
    double getDistanceSquared(UUID a, UUID b);
    boolean isGloballyHidden(UUID targetId);
    boolean hasCustomName(UUID targetId);
    void disableVanillaNametag(UUID targetId);
    net.kyori.adventure.text.Component parsePlaceholders(UUID targetId, String text);
    String getPrimaryGroup(UUID targetId);
    String getOwnerPrimaryGroup(UUID targetId);
    boolean hasPermission(UUID targetId, String permission);
    /** Cheap hash of volatile target state (health, names, owner, baby) used as parse-cache key part. */
    long getStateHash(UUID targetId);
    /** Advances only when an animation frame changes; used as parse-cache key part for &lt;anim:&gt; lines. */
    long getAnimationEpoch();
}
