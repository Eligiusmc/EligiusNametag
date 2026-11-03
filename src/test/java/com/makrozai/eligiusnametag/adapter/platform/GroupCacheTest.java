package com.makrozai.eligiusnametag.adapter.platform;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-5: Vault primary-group lookups must be TTL-cached so the hot loop
 * does not hit LuckPerms/Vault once per target per tick.
 */
public class GroupCacheTest {

    private ServerMock server;
    private PaperPlatformAdapter adapter;
    private Permission vault;
    private PlayerMock player;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        adapter = new PaperPlatformAdapter(null);
        vault = Mockito.mock(Permission.class);
        when(vault.hasGroupSupport()).thenReturn(true);
        when(vault.getPrimaryGroup(any(Player.class))).thenReturn("admin");
        adapter.vaultPerms = vault;
        player = server.addPlayer("steve");
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testGroupResolvedOnceWithinTtl() {
        Player p = player;

        String first = adapter.getPrimaryGroup(p.getUniqueId());
        String second = adapter.getPrimaryGroup(p.getUniqueId());

        org.junit.jupiter.api.Assertions.assertEquals("admin", first);
        org.junit.jupiter.api.Assertions.assertEquals("admin", second);
        verify(vault, times(1)).getPrimaryGroup(any(Player.class));
    }

    @Test
    public void testNullGroupIsAlsoCached() {
        // Player not online → resolution returns null, but must still be cached.
        java.util.UUID absent = java.util.UUID.randomUUID();

        String first = adapter.getPrimaryGroup(absent);
        String second = adapter.getPrimaryGroup(absent);

        org.junit.jupiter.api.Assertions.assertNull(first);
        org.junit.jupiter.api.Assertions.assertNull(second);
        // Never reached Vault: no online player to resolve.
        verify(vault, times(0)).getPrimaryGroup(any(Player.class));
    }
}
