package com.makrozai.eligiusnametag;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EligiusNametagE2ETest {

    private ServerMock server;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testE2EEnvironment() {
        PlayerMock player = server.addPlayer("makrozai");
        assertNotNull(player);
        assertTrue(player.isOnline());
        // Since ProtocolLib requires a real server agent to hook into Netty,
        // true packet inspection E2E in MockBukkit requires a custom ProtocolManager mock.
        // This test ensures the E2E MockBukkit server starts successfully.
    }
}
