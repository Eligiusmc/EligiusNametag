package com.makrozai.eligiusnametag.domain.port;

import net.kyori.adventure.text.Component;
import java.util.List;
import java.util.UUID;

public interface NametagRendererPort {
    void renderNametag(UUID targetId, List<Component> lines, List<UUID> viewers, float yOffset);
    void hideNametag(UUID targetId, List<UUID> viewers);
    void destroyNametag(UUID targetId);
    void clearViewer(UUID viewerId);
    void destroyAll();
    int getActiveEntityCount();
}
