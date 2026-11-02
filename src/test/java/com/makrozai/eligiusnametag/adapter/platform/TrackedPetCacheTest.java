package com.makrozai.eligiusnametag.adapter.platform;

import org.bukkit.entity.Horse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * getTamedMobs() used to require isTamed() and EVICT anything else — even a
 * Tameable already admitted to the cache. Named-untamed pets (camels) were
 * dropped on the next tick, so their nametag never rendered.
 */
public class TrackedPetCacheTest {

    private ServerMock server;
    private PaperPlatformAdapter adapter;
    private WorldMock world;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        adapter = new PaperPlatformAdapter(null);
        world = new WorldMock();
        server.addWorld(world);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testNamedUntamedPetIsNotEvicted() {
        Horse horse = world.spawn(world.getSpawnLocation(), Horse.class);
        horse.setCustomName("Camel");
        // isTamed stays false (default)

        adapter.addTamedMob(horse.getUniqueId());
        List<UUID> result = adapter.getTamedMobs();

        assertTrue(result.contains(horse.getUniqueId()),
                "named-untamed pet must remain in getTamedMobs()");
    }

    @Test
    public void testUnnamedUntamedIsEvicted() {
        Horse horse = world.spawn(world.getSpawnLocation(), Horse.class);

        adapter.addTamedMob(horse.getUniqueId());
        List<UUID> result = adapter.getTamedMobs();

        assertFalse(result.contains(horse.getUniqueId()),
                "unnamed untamed entity must be evicted");
    }

    @Test
    public void testTamedIsKept() {
        Horse horse = world.spawn(world.getSpawnLocation(), Horse.class);
        horse.setTamed(true);

        adapter.addTamedMob(horse.getUniqueId());
        List<UUID> result = adapter.getTamedMobs();

        assertTrue(result.contains(horse.getUniqueId()),
                "tamed pet must be kept");
    }
}
