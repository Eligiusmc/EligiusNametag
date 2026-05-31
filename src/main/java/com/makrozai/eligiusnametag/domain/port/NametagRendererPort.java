package com.makrozai.eligiusnametag.domain.port;

import java.util.List;
import java.util.UUID;

public interface NametagRendererPort {
    void renderNametag(UUID targetId, List<net.kyori.adventure.text.Component> lines, List<UUID> viewers, float yOffset);
    void hideNametag(UUID targetId, List<UUID> viewers);
    void clearViewer(UUID viewerId);
    void destroyAll();
}
