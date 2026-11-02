package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NametagServiceTest {

    private ConfigPort configPort;
    private PlatformPort platformPort;
    private NametagRendererPort rendererPort;
    private DatabasePort databasePort;
    private NametagService nametagService;

    @BeforeEach
    public void setup() {
        configPort = Mockito.mock(ConfigPort.class);
        platformPort = Mockito.mock(PlatformPort.class);
        rendererPort = Mockito.mock(NametagRendererPort.class);
        databasePort = Mockito.mock(DatabasePort.class);

        when(databasePort.getAllPlayersWithViewSelf()).thenReturn(Collections.emptySet());
        // Defaults that make a "visible nearby viewer" scenario pass without per-test stubs.
        when(configPort.getViewDistance()).thenReturn(64);
        when(configPort.isTamedMobsEnabled()).thenReturn(true);
        when(configPort.isTamedMobsShowUnnamed()).thenReturn(true);
        when(platformPort.getDistanceSquared(any(), any())).thenReturn(0.0);
        when(platformPort.isSameWorld(any(), any())).thenReturn(true);
        when(platformPort.canViewerSeeTarget(any(), any())).thenReturn(true);
        when(platformPort.isGloballyHidden(any())).thenReturn(false);
        when(platformPort.getPrimaryGroup(any())).thenReturn("default");
        when(configPort.getPlayerNametagTemplate(anyString())).thenReturn(Arrays.asList("Test"));
        when(configPort.getYOffset()).thenReturn(0.35);
        when(platformPort.parsePlaceholders(any(), anyString()))
                .thenReturn(net.kyori.adventure.text.Component.text("Test"));

        nametagService = new NametagService(configPort, platformPort, rendererPort, databasePort, null);
    }

    @Test
    public void testUpdateAllNametags_HiddenTarget() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(player1, player2));
        when(platformPort.isGloballyHidden(player1)).thenReturn(true);
        when(platformPort.isGloballyHidden(player2)).thenReturn(false);

        nametagService.updateAllNametags();

        // Player 1 is hidden, so renderer should hide it
        verify(rendererPort, atLeastOnce()).hideNametag(eq(player1), anyList());

        // Player 2 is visible, should be rendered
        verify(rendererPort, atLeastOnce()).renderNametag(eq(player2), anyList(), anyList(), anyFloat());
    }

    @Test
    public void testViewDistance_HidesTargetBeyondConfiguredRange() {
        // Issue #45: view_distance must gate visibility server-side.
        UUID target = UUID.randomUUID();
        UUID nearViewer = UUID.randomUUID();
        UUID farViewer = UUID.randomUUID();

        when(configPort.getViewDistance()).thenReturn(64);
        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(target, nearViewer, farViewer));
        // Symmetric stubs: distance(a,b) must match both call orders.
        when(platformPort.getDistanceSquared(target, nearViewer)).thenReturn(10.0 * 10.0);
        when(platformPort.getDistanceSquared(nearViewer, target)).thenReturn(10.0 * 10.0);
        when(platformPort.getDistanceSquared(target, farViewer)).thenReturn(100.0 * 100.0);
        when(platformPort.getDistanceSquared(farViewer, target)).thenReturn(100.0 * 100.0);
        when(platformPort.getDistanceSquared(nearViewer, farViewer)).thenReturn(100.0 * 100.0);
        when(platformPort.getDistanceSquared(farViewer, nearViewer)).thenReturn(100.0 * 100.0);

        nametagService.updateAllNametags();

        // Near viewer must be in a render list for the target.
        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(nearViewer)), anyFloat());
        // Far viewer must never appear in any render list for the target.
        verify(rendererPort, never()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(farViewer)), anyFloat());
        // Far viewer is hidden from the target (may be batched with the self-viewer hide).
        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(farViewer)));
    }

    @Test
    public void testViewDistance_RendersWhenWithinRange() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(configPort.getViewDistance()).thenReturn(64);
        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(target, viewer));
        when(platformPort.getDistanceSquared(target, viewer)).thenReturn(32.0 * 32.0);
        when(platformPort.getDistanceSquared(viewer, target)).thenReturn(32.0 * 32.0);

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
        // The in-range viewer must not be hidden from the target.
        verify(rendererPort, never()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
    }

    @Test
    public void testViewDistance_BoundaryIsInclusive() {
        // Exactly at view_distance must still be visible (<= comparison).
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(configPort.getViewDistance()).thenReturn(64);
        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(target, viewer));
        when(platformPort.getDistanceSquared(target, viewer)).thenReturn(64.0 * 64.0);
        when(platformPort.getDistanceSquared(viewer, target)).thenReturn(64.0 * 64.0);

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
    }

    @Test
    public void testDistanceUnavailable_HidesTarget() {
        // Missing entity / other world → adapter returns Double.MAX_VALUE → must hide.
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(target, viewer));
        when(platformPort.getDistanceSquared(any(), any())).thenReturn(Double.MAX_VALUE);

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
        verify(rendererPort, never()).renderNametag(eq(target), anyList(), anyList(), anyFloat());
    }

    @Test
    public void testDifferentWorld_HidesTarget() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(platformPort.getOnlinePlayers()).thenReturn(Arrays.asList(target, viewer));
        when(platformPort.isSameWorld(target, viewer)).thenReturn(false);
        when(platformPort.isSameWorld(viewer, target)).thenReturn(false);
        when(platformPort.getDistanceSquared(target, viewer)).thenReturn(0.0);
        when(platformPort.getDistanceSquared(viewer, target)).thenReturn(0.0);

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
        verify(rendererPort, never()).renderNametag(eq(target), anyList(), anyList(), anyFloat());
    }
}
