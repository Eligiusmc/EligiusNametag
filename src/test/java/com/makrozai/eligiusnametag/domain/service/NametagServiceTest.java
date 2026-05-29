package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NametagServiceTest {

    private ConfigPort configPort;
    private PlatformPort platformPort;
    private NametagRendererPort rendererPort;
    private NametagService nametagService;

    @BeforeEach
    public void setup() {
        configPort = Mockito.mock(ConfigPort.class);
        platformPort = Mockito.mock(PlatformPort.class);
        rendererPort = Mockito.mock(NametagRendererPort.class);
        nametagService = new NametagService(configPort, platformPort, rendererPort);
    }

    @Test
    public void testUpdateAllNametags_HiddenTarget() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(player1, player2));
        when(platformPort.isGloballyHidden(player1)).thenReturn(true);
        when(platformPort.isGloballyHidden(player2)).thenReturn(false);
        when(platformPort.canViewerSeeTarget(any(), any())).thenReturn(true);
        when(platformPort.isSameWorld(any(), any())).thenReturn(true);
        
        when(configPort.getPlayerNametagTemplate(player2)).thenReturn("Test");
        when(configPort.isSelfViewEnabled()).thenReturn(true);
        when(platformPort.parsePlaceholders(any(), any())).thenReturn("Test");

        nametagService.updateAllNametags();

        // Player 1 is hidden, so renderer should hide it
        verify(rendererPort, atLeastOnce()).hideNametag(eq(player1), anyList());
        
        // Player 2 is visible, should be rendered
        verify(rendererPort, atLeastOnce()).renderNametag(eq(player2), anyList(), anyList());
    }
}
