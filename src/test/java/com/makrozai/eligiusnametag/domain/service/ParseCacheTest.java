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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-2: the parsed-line cache must skip platform.parsePlaceholders on
 * identical inputs, clear on reload, and bust on animation epoch changes.
 */
public class ParseCacheTest {

    private static final String WORLD = "world";

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
        when(configPort.getViewDistance()).thenReturn(64);
        when(configPort.isTamedMobsEnabled()).thenReturn(true);
        when(configPort.isTamedMobsShowUnnamed()).thenReturn(true);
        when(configPort.getCacheTicks()).thenReturn(10);
        when(configPort.getYOffset()).thenReturn(0.35);

        target = UUID.randomUUID();
        viewer = UUID.randomUUID();
        when(platformPort.snapshotViewers()).thenReturn(Arrays.asList(
                new PlatformPort.ViewerSnapshot(target, WORLD, 0, 0, 0),
                new PlatformPort.ViewerSnapshot(viewer, WORLD, 10, 0, 0)));
        when(platformPort.resolveTarget(any())).thenAnswer(inv ->
                new PlatformPort.TargetSnapshot(inv.getArgument(0), WORLD, 0, 0, 0));
        when(platformPort.getTamedMobs()).thenReturn(Collections.emptyList());
        when(platformPort.isGloballyHidden(any())).thenReturn(false);
        when(platformPort.canViewerSeeTarget(any(), any())).thenReturn(true);
        when(platformPort.getPrimaryGroup(any())).thenReturn("default");
        when(platformPort.getStateHash(any())).thenReturn(42L);
        when(platformPort.getAnimationEpoch()).thenReturn(0L);
        when(configPort.getPlayerNametagTemplate(anyString())).thenReturn(Arrays.asList("Test"));
        when(platformPort.parsePlaceholders(any(), anyString()))
                .thenReturn(Component.text("Test"));

        nametagService = new NametagService(configPort, platformPort, rendererPort, databasePort, null);
    }

    @Test
    public void testSecondTickDoesNotReparse() {
        nametagService.updateAllNametags();
        nametagService.updateAllNametags();

        // Both ticks rendered the target, but the second must hit the cache.
        verify(platformPort, times(1)).parsePlaceholders(eq(target), eq("Test"));
    }

    @Test
    public void testClearCachesForcesReparse() {
        nametagService.updateAllNametags();
        nametagService.clearCaches();
        nametagService.updateAllNametags();

        verify(platformPort, times(2)).parsePlaceholders(eq(target), eq("Test"));
    }

    @Test
    public void testStateHashChangeBustsCache() {
        nametagService.updateAllNametags();
        when(platformPort.getStateHash(any())).thenReturn(43L);
        nametagService.updateAllNametags();

        verify(platformPort, times(2)).parsePlaceholders(eq(target), eq("Test"));
    }

    @Test
    public void testAnimationEpochBustsAnimLinesOnly() {
        when(configPort.getPlayerNametagTemplate(anyString()))
                .thenReturn(Arrays.asList("<anim:rainbow>Hi</anim:rainbow>", "Plain"));
        when(platformPort.getAnimationEpoch()).thenReturn(0L, 1L);

        nametagService.updateAllNametags();
        nametagService.updateAllNametags();

        // Anim line re-parsed because the frame epoch advanced…
        verify(platformPort, times(2))
                .parsePlaceholders(eq(target), eq("<anim:rainbow>Hi</anim:rainbow>"));
        // …while the non-anim line stayed cached.
        verify(platformPort, times(1)).parsePlaceholders(eq(target), eq("Plain"));
    }

    @Test
    public void testDisabledCacheAlwaysReparses() {
        when(configPort.getCacheTicks()).thenReturn(0);

        nametagService.updateAllNametags();
        nametagService.updateAllNametags();

        verify(platformPort, times(2)).parsePlaceholders(eq(target), eq("Test"));
    }

    @Test
    public void testHiddenTargetNeverParses() {
        when(platformPort.isGloballyHidden(target)).thenReturn(true);

        nametagService.updateAllNametags();

        verify(platformPort, never()).parsePlaceholders(eq(target), anyString());
    }
}
