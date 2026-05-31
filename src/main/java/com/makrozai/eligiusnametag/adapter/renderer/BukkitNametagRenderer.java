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
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitNametagRenderer implements NametagRendererPort {
    private final Plugin plugin;
    private final double viewDistance;
    private final double lineSpacing;
    
    private final Map<UUID, List<TextDisplay>> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> lineSpawnedViewers = new ConcurrentHashMap<>();
    private final Map<UUID, Component> lineJsonCache = new ConcurrentHashMap<>();

    public BukkitNametagRenderer(Plugin plugin, double viewDistance, double lineSpacing) {
        this.plugin = plugin;
        this.viewDistance = viewDistance;
        this.lineSpacing = lineSpacing;
    }

    @Override
    public void renderNametag(UUID targetId, List<Component> lines, List<UUID> viewers, float yOffset) {
        Entity target = Bukkit.getEntity(targetId);
        if (target == null) return;

        List<TextDisplay> displays = activeEntities.computeIfAbsent(targetId, k -> new ArrayList<>());

        // 1. Spawn missing lines
        while (displays.size() < lines.size()) {
            Location spawnLoc = target.getLocation();
            TextDisplay display = target.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                entity.setPersistent(false);
                entity.setVisibleByDefault(false);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setViewRange((float) viewDistance);
                entity.setShadowRadius(0f);
                entity.setShadowStrength(0f);
                // Ensure text is visible through blocks just like virtual ones usually are if configured?
                // Depending on requirements.
            });
            displays.add(display);
        }

        // 2. Destroy extra lines
        while (displays.size() > lines.size()) {
            TextDisplay removed = displays.remove(displays.size() - 1);
            removed.remove();
            lineSpawnedViewers.remove(removed.getUniqueId());
            lineJsonCache.remove(removed.getUniqueId());
        }

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
                display.text(lineComp);
                lineJsonCache.put(displayId, lineComp);
            }

            // Update viewer visibility
            Set<UUID> currentViewers = lineSpawnedViewers.computeIfAbsent(displayId, k -> new HashSet<>());
            
            // Show to new valid viewers
            for (UUID viewerId : viewers) {
                if (!currentViewers.contains(viewerId)) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        viewer.showEntity(plugin, display);
                        currentViewers.add(viewerId);
                    }
                }
            }

            // Hide from players who are no longer valid viewers
            Iterator<UUID> it = currentViewers.iterator();
            while (it.hasNext()) {
                UUID currentViewerId = it.next();
                if (!viewers.contains(currentViewerId)) {
                    Player currentViewer = Bukkit.getPlayer(currentViewerId);
                    if (currentViewer != null && currentViewer.isOnline()) {
                        currentViewer.hideEntity(plugin, display);
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
            if (currentViewers != null) {
                for (UUID viewerId : viewers) {
                    if (currentViewers.contains(viewerId)) {
                        Player viewer = Bukkit.getPlayer(viewerId);
                        if (viewer != null && viewer.isOnline()) {
                            viewer.hideEntity(plugin, display);
                        }
                        currentViewers.remove(viewerId);
                    }
                }
            }
        }
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
        lineJsonCache.clear();
    }

    @Override
    public void clearViewer(UUID viewerId) {
        for (Set<UUID> viewers : lineSpawnedViewers.values()) {
            viewers.remove(viewerId);
        }
    }
}
