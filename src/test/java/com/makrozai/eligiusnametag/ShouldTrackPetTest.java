package com.makrozai.eligiusnametag;

import org.bukkit.entity.Tameable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * pets.yml: "false = only mobs with a Name Tag item get holograms".
 * Admission to the pet render cache must be name-based (tamed OR named) —
 * not tame-only. Regression: named-untamed rideables (camels, /summon horses).
 */
public class ShouldTrackPetTest {

    @Test
    public void testTamedIsTracked() {
        Tameable t = Mockito.mock(Tameable.class);
        Mockito.when(t.isTamed()).thenReturn(true);
        Mockito.when(t.getCustomName()).thenReturn(null);

        assertTrue(EligiusNametag.shouldTrackPet(t),
                "tamed mobs must stay in the cache");
    }

    @Test
    public void testNamedUntamedIsTracked() {
        Tameable t = Mockito.mock(Tameable.class);
        Mockito.when(t.isTamed()).thenReturn(false);
        Mockito.when(t.getCustomName()).thenReturn("Rocinante");

        assertTrue(EligiusNametag.shouldTrackPet(t),
                "named-untamed (camel with Name Tag) must be tracked — bug under test");
    }

    @Test
    public void testTamedAndNamedIsTracked() {
        Tameable t = Mockito.mock(Tameable.class);
        Mockito.when(t.isTamed()).thenReturn(true);
        Mockito.when(t.getCustomName()).thenReturn("Bob");

        assertTrue(EligiusNametag.shouldTrackPet(t));
    }

    @Test
    public void testUnnamedUntamedIsNotTracked() {
        Tameable t = Mockito.mock(Tameable.class);
        Mockito.when(t.isTamed()).thenReturn(false);
        Mockito.when(t.getCustomName()).thenReturn(null);

        assertFalse(EligiusNametag.shouldTrackPet(t),
                "wild unnamed mobs must not enter the cache");
    }
}
