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

    private static final String WORLD = "world";

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
        when(platformPort.snapshotViewers()).thenReturn(Collections.emptyList());
        when(platformPort.resolveTarget(any())).thenReturn(null);
        when(platformPort.isGloballyHidden(any())).thenReturn(false);
        when(platformPort.getPrimaryGroup(any())).thenReturn("default");
        when(configPort.getPlayerNametagTemplate(anyString())).thenReturn(Arrays.asList("Test"));
        when(configPort.getYOffset()).thenReturn(0.35);
        when(platformPort.canViewerSeeTarget(any(), any())).thenReturn(true);
        when(platformPort.parsePlaceholders(any(), anyString()))
                .thenReturn(net.kyori.adventure.text.Component.text("Test"));

        nametagService = new NametagService(configPort, platformPort, rendererPort, databasePort, null);
    }

    private static PlatformPort.ViewerSnapshot viewer(UUID id, String world, double x, double y, double z) {
        return new PlatformPort.ViewerSnapshot(id, world, x, y, z);
    }

    private void stubTargets(PlatformPort.TargetSnapshot... targets) {
        for (PlatformPort.TargetSnapshot t : targets) {
            when(platformPort.resolveTarget(t.id())).thenReturn(t);
        }
    }

    @Test
    public void testUpdateAllNametags_HiddenTarget() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PlatformPort.ViewerSnapshot v1 = viewer(player1, WORLD, 0, 0, 0);
        PlatformPort.ViewerSnapshot v2 = viewer(player2, WORLD, 0, 0, 0);
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(v1, v2));
        stubTargets(
                new PlatformPort.TargetSnapshot(player1, WORLD, 0, 0, 0),
                new PlatformPort.TargetSnapshot(player2, WORLD, 0, 0, 0));
        when(platformPort.isGloballyHidden(player1)).thenReturn(true);

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
        PlatformPort.ViewerSnapshot vTarget = viewer(target, WORLD, 0, 0, 0);
        PlatformPort.ViewerSnapshot vNear = viewer(nearViewer, WORLD, 10, 0, 0);
        PlatformPort.ViewerSnapshot vFar = viewer(farViewer, WORLD, 100, 0, 0);
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(vTarget, vNear, vFar));
        stubTargets(
                new PlatformPort.TargetSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.TargetSnapshot(nearViewer, WORLD, 10, 0, 0),
                new PlatformPort.TargetSnapshot(farViewer, WORLD, 100, 0, 0));

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
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                viewer(target, WORLD, 0, 0, 0),
                viewer(viewer, WORLD, 32, 0, 0)));
        stubTargets(
                new PlatformPort.TargetSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.TargetSnapshot(viewer, WORLD, 32, 0, 0));

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
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                viewer(target, WORLD, 0, 0, 0),
                viewer(viewer, WORLD, 64, 0, 0)));
        stubTargets(
                new PlatformPort.TargetSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.TargetSnapshot(viewer, WORLD, 64, 0, 0));

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
    }

    @Test
    public void testDistanceUnavailable_HidesTarget() {
        // Missing entity → resolveTarget returns null → must hide, never render.
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                viewer(target, WORLD, 0, 0, 0),
                viewer(viewer, WORLD, 0, 0, 0)));
        when(platformPort.resolveTarget(any())).thenReturn(null);

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
        verify(rendererPort, never()).renderNametag(eq(target), anyList(), anyList(), anyFloat());
    }

    @Test
    public void testDifferentWorld_HidesTarget() {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                viewer(target, WORLD, 0, 0, 0),
                viewer(viewer, "world_nether", 0, 0, 0)));
        stubTargets(
                new PlatformPort.TargetSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.TargetSnapshot(viewer, "world_nether", 0, 0, 0));

        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
        verify(rendererPort, never()).renderNametag(eq(target), anyList(), anyList(), anyFloat());
    }
}
