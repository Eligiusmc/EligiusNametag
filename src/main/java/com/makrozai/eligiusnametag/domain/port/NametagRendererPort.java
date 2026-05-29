package com.makrozai.eligiusnametag.domain.port;

import java.util.List;
import java.util.UUID;

public interface NametagRendererPort {
    void renderNametag(UUID targetId, List<String> lines, List<UUID> viewers, float yOffset);
    void hideNametag(UUID targetId, List<UUID> viewers);
    void destroyAll();
}
