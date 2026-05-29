package com.makrozai.eligiusnametag.domain.port;

import java.util.UUID;
import java.util.Set;

public interface DatabasePort {
    void initialize();
    void close();
    boolean getPlayerViewSelf(UUID uuid);
    void setPlayerViewSelf(UUID uuid, boolean viewSelf);
    Set<UUID> getAllPlayersWithViewSelf();
}
