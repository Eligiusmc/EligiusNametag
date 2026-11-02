package com.makrozai.eligiusnametag;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Pig;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.ZombieHorse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: right-click must stay usable on rideable mobs whose nametag
 * TextDisplay would otherwise occupy the single passenger seat.
 */
public class MountableRideTest {

    @Test
    public void testAbstractHorseFamilyIsMountable() {
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Horse.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Donkey.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Mule.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(SkeletonHorse.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(ZombieHorse.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(AbstractHorse.class)));
    }

    @Test
    public void testOtherRideablesAreMountable() {
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Llama.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Pig.class)));
        assertTrue(EligiusNametag.isMountableRide(Mockito.mock(Strider.class)));
    }

    @Test
    public void testNonRideableTameablesAreNotMountable() {
        // Wolves/cats are Tameable and get nametags, but right-click is feed/tame —
        // ejecting there would flicker the tag for no benefit.
        assertFalse(EligiusNametag.isMountableRide(Mockito.mock(Wolf.class)));
    }

    @Test
    public void testPlainEntityIsNotMountable() {
        assertFalse(EligiusNametag.isMountableRide(Mockito.mock(Entity.class)));
    }

    @Test
    public void testNullIsNotMountable() {
        assertFalse(EligiusNametag.isMountableRide(null));
    }
}
