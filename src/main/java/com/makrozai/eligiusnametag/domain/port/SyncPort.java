package com.makrozai.eligiusnametag.domain.port;

import java.util.UUID;
import java.util.function.BiConsumer;

public interface SyncPort {
    /**
     * Initializes the synchronization adapter (e.g., connects to Redis).
     * @return true if successfully connected and ready.
     */
    boolean initialize();
    
    /**
     * Closes the connection gracefully.
     */
    void close();

    /**
     * Publishes a self-view update to the network.
     * @param uuid The player UUID whose setting changed.
     * @param viewSelf The new boolean state.
     */
    void publishSelfViewUpdate(UUID uuid, boolean viewSelf);

    /**
     * Subscribes to incoming updates from other servers.
     * @param onUpdateReceived Callback to execute when a message arrives (uuid, state).
     */
    void subscribe(BiConsumer<UUID, Boolean> onUpdateReceived);
}
