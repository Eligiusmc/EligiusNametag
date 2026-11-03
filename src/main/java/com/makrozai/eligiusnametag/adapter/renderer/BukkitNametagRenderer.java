package com.makrozai.eligiusnametag.adapter.renderer;

import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitNametagRenderer implements NametagRendererPort {
    private final Plugin plugin;
    private double lineSpacing;
    private final boolean hasEntityScheduler;

    private final Map<UUID, List<TextDisplay>> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> lineSpawnedViewers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> lineHiddenViewers = new ConcurrentHashMap<>();
    private final Map<UUID, Component> lineJsonCache = new ConcurrentHashMap<>();

    public BukkitNametagRenderer(Plugin plugin, double lineSpacing) {
        this.plugin = plugin;
        this.lineSpacing = lineSpacing;
        boolean schedulerAvailable = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            schedulerAvailable = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
        this.hasEntityScheduler = schedulerAvailable;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void renderNametag(UUID targetId, List<Component> lines, List<UUID> viewers, float yOffset) {
        Entity target = Bukkit.getEntity(targetId);
        if (target == null) return;

        // Safety net: while a player rides this entity the seat belongs to the rider.
        // Spawning/remounting a display here would kick the player or fail silently.
        // Destroy so the next render after dismount starts clean (hidden-while-ridden).
        for (Entity passenger : target.getPassengers()) {
            if (passenger instanceof Player) {
                destroyNametag(targetId);
                return;
            }
        }

        List<TextDisplay> displays = activeEntities.computeIfAbsent(targetId, k -> new ArrayList<>());

        // 0. Remove any dead, invalid or cross-world displays
        // (Fixes memory leaks on chunk unload and ghost tags left behind on world change,
        //  since remounting a passenger across worlds silently fails and orphans the display)
        displays.removeIf(display -> {
            boolean wrongWorld = display.getWorld() != target.getWorld();
            if (!display.isValid() || display.isDead() || wrongWorld) {
                if (wrongWorld && display.isValid() && !display.isDead()) {
                    display.remove();
                }
                lineSpawnedViewers.remove(display.getUniqueId());
                lineHiddenViewers.remove(display.getUniqueId());
                lineJsonCache.remove(display.getUniqueId());
                return true;
            }
            return false;
        });

        // 1. Spawn missing lines
        while (displays.size() < lines.size()) {
            final int lineIndex = displays.size();
            final float spawnYOffset = yOffset + ((lines.size() - 1 - lineIndex) * (float) lineSpacing);
            final Component initialText = lines.get(lineIndex);
            Location spawnLoc = target.getLocation().add(0, target.getBoundingBox().getHeight(), 0);
            TextDisplay display = target.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                entity.setPersistent(false);
                entity.setVisibleByDefault(true);
                entity.setBillboard(Display.Billboard.CENTER);
                // view_range is a render-distance MULTIPLIER (1.0 = default), not blocks.
                // Real distance gating is enforced server-side via show/hideEntity by NametagService.
                entity.setViewRange(1.0f);
                entity.setShadowRadius(0f);
                entity.setShadowStrength(0f);
                entity.setTeleportDuration(0);
                entity.setInterpolationDuration(0);
                // Apply the line offset INSIDE the spawn consumer so the initial entity metadata
                // already contains the transformation. Setting it after spawn makes Bedrock clients
                // (GeyserMC) render all lines stacked at the same position until a relog. (Issue #45)
                entity.setTransformation(new Transformation(
                    new Vector3f(0f, spawnYOffset, 0f),
                    new Quaternionf(),
                    new Vector3f(1f, 1f, 1f),
                    new Quaternionf()
                ));
                entity.setTextOpacity((byte) 0);
                try {
                    entity.text(initialText);
                } catch (NoSuchMethodError e) {
                    entity.setText(serializeLegacy(initialText));
                }
            });
            displays.add(display);
            lineJsonCache.put(display.getUniqueId(), initialText);

            // Delayed visibility (spawn-only) to prevent the fly-in animation.
            // Entity scheduler works on Paper/Folia; Spigot falls back to the global scheduler.
            final TextDisplay spawned = display;
            if (hasEntityScheduler) {
                spawned.getScheduler().runDelayed(plugin, task -> {
                    if (spawned.isValid() && !spawned.isDead()) {
                        spawned.setTextOpacity((byte) -1);
                    }
                }, null, 2L);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (spawned.isValid() && !spawned.isDead()) {
                        spawned.setTextOpacity((byte) -1);
                    }
                }, 2L);
            }
        }

        // 2. Destroy extra lines
        while (displays.size() > lines.size()) {
            TextDisplay removed = displays.remove(displays.size() - 1);
            removed.remove();
            lineSpawnedViewers.remove(removed.getUniqueId());
            lineHiddenViewers.remove(removed.getUniqueId());
            lineJsonCache.remove(removed.getUniqueId());
        }

        // O(1) contains() in the per-line hot loop below (avoids O(n²) on large viewer lists).
        Set<UUID> viewerSet = new HashSet<>(viewers);

        // 3. Update displays
        for (int i = 0; i < lines.size(); i++) {
            TextDisplay display = displays.get(i);
            Component lineComp = lines.get(i);
            UUID displayId = display.getUniqueId();

            // Mount directly to target if detached
            if (display.getVehicle() == null || !display.getVehicle().getUniqueId().equals(target.getUniqueId())) {
                target.addPassenger(display);
            }

            // Set translation (yOffset)
            float calculatedYOffset = yOffset + ((lines.size() - 1 - i) * (float) lineSpacing);
            Vector3f offset = new Vector3f(0f, calculatedYOffset, 0f);
            
            Transformation currentTransform = display.getTransformation();
            if (!currentTransform.getTranslation().equals(offset)) {
                Transformation newTransform = new Transformation(
                    offset, 
                    currentTransform.getLeftRotation(), 
                    currentTransform.getScale(), 
                    currentTransform.getRightRotation()
                );
                display.setTransformation(newTransform);
            }

            // Update text if changed
            Component lastComp = lineJsonCache.get(displayId);
            if (!lineComp.equals(lastComp)) {
                try {
                    display.text(lineComp);
                } catch (NoSuchMethodError e) {
                    display.setText(serializeLegacy(lineComp));
                }
                lineJsonCache.put(displayId, lineComp);
            }

            // Update viewer visibility
            // ConcurrentHashMap.newKeySet() keeps Folia region threads from hitting ConcurrentModificationException.
            Set<UUID> currentViewers = lineSpawnedViewers.computeIfAbsent(displayId, k -> ConcurrentHashMap.newKeySet());
            Set<UUID> hiddenViewers = lineHiddenViewers.computeIfAbsent(displayId, k -> ConcurrentHashMap.newKeySet());

            // Show to new valid viewers
            for (UUID viewerId : viewerSet) {
                if (!currentViewers.contains(viewerId)) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        viewer.showEntity(plugin, display);
                        currentViewers.add(viewerId);
                        hiddenViewers.remove(viewerId);
                    }
                }
            }

            // Hide from players who are no longer valid viewers
            Iterator<UUID> it = currentViewers.iterator();
            while (it.hasNext()) {
                UUID currentViewerId = it.next();
                if (!viewerSet.contains(currentViewerId)) {
                    Player currentViewer = Bukkit.getPlayer(currentViewerId);
                    if (currentViewer != null && currentViewer.isOnline()) {
                        currentViewer.hideEntity(plugin, display);
                        hiddenViewers.add(currentViewerId);
                    }
                    it.remove();
                }
            }
        }
    }

    @Override
    public void hideNametag(UUID targetId, List<UUID> viewers) {
        List<TextDisplay> displays = activeEntities.get(targetId);
        if (displays == null || displays.isEmpty()) return;

        for (TextDisplay display : displays) {
            Set<UUID> currentViewers = lineSpawnedViewers.get(display.getUniqueId());
            Set<UUID> hiddenViewers = lineHiddenViewers.computeIfAbsent(display.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            
            for (UUID viewerId : viewers) {
                if (!hiddenViewers.contains(viewerId)) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        viewer.hideEntity(plugin, display);
                        hiddenViewers.add(viewerId);
                        if (currentViewers != null) {
                            currentViewers.remove(viewerId);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void destroyNametag(UUID targetId) {
        List<TextDisplay> displays = activeEntities.remove(targetId);
        if (displays != null) {
            for (TextDisplay display : displays) {
                if (display.isValid() && !display.isDead()) {
                    display.remove();
                }
                lineSpawnedViewers.remove(display.getUniqueId());
                lineHiddenViewers.remove(display.getUniqueId());
                lineJsonCache.remove(display.getUniqueId());
            }
        }
    }

    /**
     * Unmounts this target's TextDisplays without destroying them.
     * CraftBukkit refuses to teleport an entity that has passengers (the player becomes
     * a "vehicle"), which blocked Multiverse and other TP plugins (#46 / #47).
     * Called right before the teleport resolves; the next render pass re-mounts, and a
     * 1-tick follow-up remount minimises the floating ghost at the origin.
     */
    @Override
    public void ejectPassengers(UUID targetId) {
        List<TextDisplay> displays = activeEntities.get(targetId);
        if (displays == null || displays.isEmpty()) return;

        Entity target = Bukkit.getEntity(targetId);
        if (target == null) return;

        for (TextDisplay display : displays) {
            if (display.isValid() && !display.isDead()
                    && display.getVehicle() != null
                    && display.getVehicle().getUniqueId().equals(targetId)) {
                target.removePassenger(display);
            }
        }

        // Teleport has not moved the player yet at event time; remount on the next tick.
        target.getScheduler().run(plugin, task -> {
            Entity t = Bukkit.getEntity(targetId);
            if (t == null) return;
            // Never steal the rider's seat: if a player mounted meanwhile, leave it alone
            // (EntityMountEvent/render destroy the tag; respawn happens after dismount).
            for (Entity p : t.getPassengers()) {
                if (p instanceof Player) return;
            }
            List<TextDisplay> current = activeEntities.get(targetId);
            if (current == null) return;
            for (TextDisplay display : current) {
                if (display.isValid() && !display.isDead() && display.getVehicle() == null) {
                    t.addPassenger(display);
                }
            }
        }, () -> { /* entity retired (quit/unload) — render loop handles cleanup */ });
    }

    @Override
    public void destroyAll() {
        for (List<TextDisplay> displays : activeEntities.values()) {
            for (TextDisplay display : displays) {
                display.remove();
            }
        }
        activeEntities.clear();
        lineSpawnedViewers.clear();
        lineHiddenViewers.clear();
        lineJsonCache.clear();
    }

    @Override
    public void clearViewer(UUID viewerId) {
        for (Set<UUID> viewers : lineSpawnedViewers.values()) {
            viewers.remove(viewerId);
        }
        for (Set<UUID> viewers : lineHiddenViewers.values()) {
            viewers.remove(viewerId);
        }
    }

    @Override
    public int getActiveEntityCount() {
        int count = 0;
        for (List<TextDisplay> displays : activeEntities.values()) {
            count += displays.size();
        }
        return count;
    }

    public void reloadConfig(double lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    private static String serializeLegacy(Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build()
                .serialize(component);
    }
}
