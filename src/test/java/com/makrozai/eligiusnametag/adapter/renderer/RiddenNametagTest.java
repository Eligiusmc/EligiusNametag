package com.makrozai.eligiusnametag.adapter.renderer;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hidden-while-ridden: a player sitting on the mount owns the only passenger seat,
 * so the renderer must destroy the tag instead of kicking the rider.
 */
public class RiddenNametagTest {

    private ServerMock server;
    private WorldMock world;
    private BukkitNametagRenderer renderer;
    private PlayerMock viewer;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        world = new WorldMock();
        server.addWorld(world);
        renderer = new BukkitNametagRenderer(MockBukkit.createMockPlugin(), 0.275);
        viewer = server.addPlayer("viewer");
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testRendersWhenNoPlayerIsRiding() {
        Entity mount = world.spawn(viewer.getLocation(), org.bukkit.entity.ArmorStand.class);
        UUID mountId = mount.getUniqueId();

        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);

        assertTrue(renderer.getActiveEntityCount() > 0,
                "tag should render when nobody is riding the mount");
    }

    @Test
    public void testDestroysTagWhenPlayerIsRiding() {
        Entity mount = world.spawn(viewer.getLocation(), org.bukkit.entity.ArmorStand.class);
        UUID mountId = mount.getUniqueId();

        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);
        assertTrue(renderer.getActiveEntityCount() > 0);

        // Player takes the single passenger seat (simulates successful mount).
        mount.addPassenger(viewer);
        assertTrue(mount.getPassengers().stream().anyMatch(p -> p instanceof Player),
                "precondition: player is riding");

        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);

        assertEquals(0, renderer.getActiveEntityCount(),
                "tag must be destroyed while a player rides (no seat-stealing remount)");
    }

    @Test
    public void testEjectDoesNotRemountOverPlayer() {
        Entity mount = world.spawn(viewer.getLocation(), org.bukkit.entity.ArmorStand.class);
        UUID mountId = mount.getUniqueId();

        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);
        assertTrue(renderer.getActiveEntityCount() > 0);

        // Teleport-style eject, then a player claims the seat before the deferred remount.
        renderer.ejectPassengers(mountId);
        mount.addPassenger(viewer);

        // Run deferred entity-scheduler work if MockBukkit queues it.
        server.getScheduler().performTicks(2);

        boolean displayOnMount = mount.getPassengers().stream().anyMatch(p -> p instanceof TextDisplay);
        assertTrue(!displayOnMount,
                "deferred remount must not push a TextDisplay onto a player-ridden mount");
    }

    @Test
    public void testRespawnsAfterPlayerDismounts() {
        Entity mount = world.spawn(viewer.getLocation(), org.bukkit.entity.ArmorStand.class);
        UUID mountId = mount.getUniqueId();

        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);
        mount.addPassenger(viewer);
        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);
        assertEquals(0, renderer.getActiveEntityCount());

        // Player leaves the mount → next render pass restores the tag.
        mount.removePassenger(viewer);
        renderer.renderNametag(mountId, List.of(Component.text("Tag")), List.of(viewer.getUniqueId()), 0.35f);

        assertTrue(renderer.getActiveEntityCount() > 0,
                "tag should respawn after the player dismounts");
    }
}
