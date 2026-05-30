package com.makrozai.eligiusnametag.adapter.renderer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolLibNametagRenderer implements NametagRendererPort {
    private final ProtocolManager protocolManager;
    private final Map<UUID, List<Integer>> activeEntities = new ConcurrentHashMap<>();
    private final Map<Integer, Set<UUID>> lineSpawnedViewers = new ConcurrentHashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE / 2);
    private final double lineSpacing;
    private final float viewDistance;

    public ProtocolLibNametagRenderer(double lineSpacing, int viewDistanceBlocks) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.lineSpacing = lineSpacing;
        this.viewDistance = (1.0f / 80.0f) * viewDistanceBlocks;
    }

    @Override
    public void renderNametag(UUID targetId, List<String> lines, List<UUID> viewers, float yOffset) {
        Entity target = Bukkit.getEntity(targetId);
        if (target == null) return;
        
        List<Integer> entityIds = activeEntities.computeIfAbsent(targetId, k -> new ArrayList<>());
        
        while (entityIds.size() < lines.size()) {
            entityIds.add(entityIdCounter.decrementAndGet());
        }

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) continue;

            int parentId = target.getEntityId();

            for (int i = 0; i < lines.size(); i++) {
                int lineEntityId = entityIds.get(i);
                String rawLine = lines.get(i);
                
                // Convert legacy ampersand colors to MiniMessage tags
                rawLine = rawLine.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
                        .replace("&4", "<dark_red>").replace("&5", "<dark_purple>").replace("&6", "<gold>").replace("&7", "<gray>")
                        .replace("&8", "<dark_gray>").replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                        .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
                        .replace("&k", "<obfuscated>").replace("&l", "<bold>").replace("&m", "<strikethrough>").replace("&n", "<underlined>")
                        .replace("&o", "<italic>").replace("&r", "<reset>");

                Component comp = MiniMessage.miniMessage().deserialize(rawLine);
                String jsonComp = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(comp);

                try {
                    Set<UUID> spawnedFor = lineSpawnedViewers.computeIfAbsent(lineEntityId, k -> ConcurrentHashMap.newKeySet());
                    boolean isNewSpawn = spawnedFor.add(viewerId);
                    
                    if (isNewSpawn) {
                        // 1. Spawn Packet
                        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                        spawnPacket.getIntegers().write(0, lineEntityId);
                        spawnPacket.getUUIDs().write(0, UUID.randomUUID());
                        spawnPacket.getEntityTypeModifier().write(0, org.bukkit.entity.EntityType.TEXT_DISPLAY);
                        
                        // Set location to the parent so it spawns in a loaded chunk immediately
                        spawnPacket.getDoubles().write(0, target.getLocation().getX());
                        spawnPacket.getDoubles().write(1, target.getLocation().getY());
                        spawnPacket.getDoubles().write(2, target.getLocation().getZ());

                        protocolManager.sendServerPacket(viewer, spawnPacket);

                        // 2. Metadata Packet
                        PacketContainer metaPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                        metaPacket.getIntegers().write(0, lineEntityId);

                        List<WrappedDataValue> dataValues = new ArrayList<>();
                        // Text Display Flags: billboard (0x03 - center)
                        dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x03));
                        // View Distance
                        dataValues.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Float.class), viewDistance));
                        
                        // Translation Offset
                        float calculatedYOffset = yOffset + ((lines.size() - 1 - i) * (float) lineSpacing);
                        org.joml.Vector3f offset = new org.joml.Vector3f(0f, calculatedYOffset, 0f);
                        dataValues.add(new WrappedDataValue(11, WrappedDataWatcher.Registry.get(org.joml.Vector3f.class), offset));
                        
                        // Text Component
                        Object compHandle = WrappedChatComponent.fromJson(jsonComp).getHandle();
                        dataValues.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false), compHandle));

                        metaPacket.getDataValueCollectionModifier().write(0, dataValues);
                        protocolManager.sendServerPacket(viewer, metaPacket);
                    }

                    // 3. Mount Packet - continually sent to ensure it attaches when the pet finally loads
                    PacketContainer mountPacket = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
                    mountPacket.getIntegers().write(0, parentId);
                    
                    if (parentId == target.getEntityId()) {
                        // Mounting to the main entity, preserve its existing passengers!
                        List<Integer> passengers = new ArrayList<>();
                        for (Entity p : target.getPassengers()) {
                            passengers.add(p.getEntityId());
                        }
                        passengers.add(lineEntityId);
                        int[] passengerArray = passengers.stream().mapToInt(Integer::intValue).toArray();
                        mountPacket.getIntegerArrays().write(0, passengerArray);
                    } else {
                        // Mounting to the previous text display
                        mountPacket.getIntegerArrays().write(0, new int[]{lineEntityId});
                    }
                    protocolManager.sendServerPacket(viewer, mountPacket);
                    
                    parentId = lineEntityId;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void hideNametag(UUID targetId, List<UUID> viewers) {
        List<Integer> entityIds = activeEntities.get(targetId);
        if (entityIds == null || entityIds.isEmpty()) return;

        List<Integer> ids = new ArrayList<>(entityIds);
        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroyPacket.getIntLists().write(0, ids);

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                protocolManager.sendServerPacket(viewer, destroyPacket);
                for (int id : ids) {
                    Set<UUID> spawnedFor = lineSpawnedViewers.get(id);
                    if (spawnedFor != null) spawnedFor.remove(viewerId);
                }
            }
        }
    }

    @Override
    public void destroyAll() {
        for (Map.Entry<UUID, List<Integer>> entry : activeEntities.entrySet()) {
            List<Integer> ids = new ArrayList<>(entry.getValue());
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, ids);
            protocolManager.broadcastServerPacket(destroyPacket);
        }
        activeEntities.clear();
        lineSpawnedViewers.clear();
    }

    /**
     * Removes a viewer from all spawn tracking maps.
     * Must be called when a player disconnects so that
     * fresh spawn packets are sent when they reconnect.
     */
    public void clearViewer(UUID viewerId) {
        for (Set<UUID> viewers : lineSpawnedViewers.values()) {
            viewers.remove(viewerId);
        }
    }
}
