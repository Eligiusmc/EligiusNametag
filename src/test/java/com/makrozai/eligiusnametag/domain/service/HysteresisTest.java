package com.makrozai.eligiusnametag.domain.service;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import net.kyori.adventure.text.Component;
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

/**
 * P1-5: show/hide hysteresis. A viewer shown at view_distance must stay
 * shown up to view_distance + margin before hiding, so boundary hoverers
 * don't ping-pong show/hide packets every tick.
 */
public class HysteresisTest {

    private static final String WORLD = "world";
    private static final int VIEW_DISTANCE = 64;
    private static final double MARGIN = 2.0;

    private ConfigPort configPort;
    private PlatformPort platformPort;
    private NametagRendererPort rendererPort;
    private NametagService nametagService;
    private UUID target;
    private UUID viewer;

    @BeforeEach
    public void setup() {
        configPort = Mockito.mock(ConfigPort.class);
        platformPort = Mockito.mock(PlatformPort.class);
        rendererPort = Mockito.mock(NametagRendererPort.class);
        DatabasePort databasePort = Mockito.mock(DatabasePort.class);

        when(databasePort.getAllPlayersWithViewSelf()).thenReturn(Collections.emptySet());
        when(configPort.getViewDistance()).thenReturn(VIEW_DISTANCE);
        when(configPort.isTamedMobsEnabled()).thenReturn(true);
        when(configPort.isTamedMobsShowUnnamed()).thenReturn(true);
        when(configPort.getYOffset()).thenReturn(0.35);
        when(configPort.getPlayerNametagTemplate(anyString())).thenReturn(Arrays.asList("Test"));
        when(platformPort.getTamedMobs()).thenReturn(Collections.emptyList());
        when(platformPort.isGloballyHidden(any())).thenReturn(false);
        when(platformPort.canViewerSeeTarget(any(), any())).thenReturn(true);
        when(platformPort.getPrimaryGroup(any())).thenReturn("default");
        when(platformPort.parsePlaceholders(any(), anyString())).thenReturn(Component.text("Test"));
        when(platformPort.resolveTarget(any())).thenAnswer(inv ->
                new PlatformPort.TargetSnapshot(inv.getArgument(0), WORLD, 0, 0, 0));

        target = UUID.randomUUID();
        viewer = UUID.randomUUID();
        placeViewerAt(0);

        nametagService = new NametagService(configPort, platformPort, rendererPort, databasePort, null);
    }

    private void placeViewerAt(double x) {
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                new PlatformPort.ViewerSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.ViewerSnapshot(viewer, WORLD, x, 0, 0)));
    }

    @Test
    public void testShownViewerStaysVisibleBeyondViewDistance() {
        // Tick 1: viewer exactly at view_distance → shown.
        placeViewerAt(VIEW_DISTANCE);
        nametagService.updateAllNametags();
        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());

        // Tick 2: viewer at vd+1 → still shown (within vd+margin).
        placeViewerAt(VIEW_DISTANCE + 1);
        nametagService.updateAllNametags();
        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
        verify(rendererPort, never()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
    }

    @Test
    public void testShownViewerHidesBeyondMargin() {
        placeViewerAt(VIEW_DISTANCE);
        nametagService.updateAllNametags();
        Mockito.clearInvocations(rendererPort);

        // Past the margin (vd+2 is the inclusive hide boundary → vd+3 hides).
        placeViewerAt(VIEW_DISTANCE + MARGIN + 1);
        nametagService.updateAllNametags();

        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
        verify(rendererPort, never()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
    }

    @Test
    public void testNewViewerNeedsStrictViewDistance() {
        // Never shown before → strict show threshold (vd) applies, not the margin.
        placeViewerAt(VIEW_DISTANCE + 1);
        nametagService.updateAllNametags();

        verify(rendererPort, never()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
        verify(rendererPort, atLeastOnce()).hideNametag(
                eq(target), argThat(v -> v != null && v.contains(viewer)));
    }

    @Test
    public void testHiddenViewerMustReturnWithinViewDistance() {
        // Shown, then pushed far → hidden.
        placeViewerAt(VIEW_DISTANCE);
        nametagService.updateAllNametags();
        placeViewerAt(VIEW_DISTANCE + MARGIN + 1);
        nametagService.updateAllNametags();
        Mockito.clearInvocations(rendererPort);

        // Coming back to vd+1 is NOT enough: once hidden, the strict
        // show threshold applies again until the viewer is back within vd.
        placeViewerAt(VIEW_DISTANCE + 1);
        nametagService.updateAllNametags();
        verify(rendererPort, never()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());

        // Back within view_distance → shown again.
        placeViewerAt(VIEW_DISTANCE);
        nametagService.updateAllNametags();
        verify(rendererPort, atLeastOnce()).renderNametag(
                eq(target), anyList(), argThat(v -> v != null && v.contains(viewer)), anyFloat());
    }
}
