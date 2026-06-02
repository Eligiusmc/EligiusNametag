package com.makrozai.eligiusnametag.adapter.network;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import com.makrozai.eligiusnametag.domain.port.SyncPort;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BiConsumer;

public class RedisAdapter implements SyncPort {

    private static final String CHANNEL_NAME = "eligius:sync:nametag";
    
    private final JavaPlugin plugin;
    private final ConfigPort config;
    
    private JedisPool jedisPool;
    private JedisPubSub pubSubListener;
    private boolean isFolia;

    public RedisAdapter(JavaPlugin plugin, ConfigPort config) {
        this.plugin = plugin;
        this.config = config;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            this.isFolia = true;
        } catch (ClassNotFoundException e) {
            this.isFolia = false;
        }
    }

    @Override
    public boolean initialize() {
        if (!config.isRedisEnabled()) {
            return false;
        }

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            
            String password = config.getRedisPassword();
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000, password);
            } else {
                jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000);
            }
            
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                if (!"PONG".equals(jedis.ping())) {
                    plugin.getLogger().warning("Redis ping failed.");
                    return false;
                }
            }
            
            plugin.getLogger().info("Successfully connected to Redis!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Redis: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (pubSubListener != null) {
            try {
                pubSubListener.unsubscribe();
            } catch (Exception ignored) {}
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    @Override
    public void publishSelfViewUpdate(UUID uuid, boolean viewSelf) {
        if (jedisPool == null || jedisPool.isClosed()) return;
        
        runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String message = uuid.toString() + ":" + viewSelf;
                jedis.publish(CHANNEL_NAME, message);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to publish Redis message: " + e.getMessage());
            }
        });
    }

    @Override
    public void subscribe(BiConsumer<UUID, Boolean> onUpdateReceived) {
        if (jedisPool == null || jedisPool.isClosed()) return;

        pubSubListener = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (CHANNEL_NAME.equals(channel)) {
                    try {
                        String[] parts = message.split(":");
                        if (parts.length == 2) {
                            UUID uuid = UUID.fromString(parts[0]);
                            boolean viewSelf = Boolean.parseBoolean(parts[1]);
                            onUpdateReceived.accept(uuid, viewSelf);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Received malformed Redis message: " + message);
                    }
                }
            }
        };

        runAsync(() -> {
            boolean active = true;
            while (active && !jedisPool.isClosed()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(pubSubListener, CHANNEL_NAME);
                } catch (Exception e) {
                    if (jedisPool.isClosed()) {
                        active = false;
                    } else {
                        plugin.getLogger().warning("Redis subscription lost. Reconnecting in 3 seconds...");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignored) {}
                    }
                }
            }
        });
    }
    
    private void runAsync(Runnable runnable) {
        if (isFolia) {
            org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}
