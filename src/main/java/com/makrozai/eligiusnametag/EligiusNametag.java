package com.makrozai.eligiusnametag;

import com.makrozai.eligiusnametag.adapter.config.YamlConfigAdapter;
import com.makrozai.eligiusnametag.adapter.database.DatabaseAdapter;
import com.makrozai.eligiusnametag.adapter.network.RedisAdapter;
import com.makrozai.eligiusnametag.adapter.platform.PaperPlatformAdapter;
import com.makrozai.eligiusnametag.adapter.renderer.BukkitNametagRenderer;
import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import com.makrozai.eligiusnametag.domain.port.NametagRendererPort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import com.makrozai.eligiusnametag.domain.port.SyncPort;
import com.makrozai.eligiusnametag.domain.service.NametagService;
import com.makrozai.eligiusnametag.domain.service.UpdateChecker;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class EligiusNametag extends JavaPlugin implements Listener {
    private YamlConfigAdapter configAdapter;
    private PlatformPort platform;
    private DatabasePort database;
    private SyncPort syncPort;
    private NametagRendererPort renderer;
    private NametagService nametagService;
    private int taskId = -1;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
        
        String platformStr = isFolia ? "Folia" : "Paper";
        String version = getPluginMeta().getVersion();
        
        configAdapter = new YamlConfigAdapter(this);
        String storageType = "mysql".equalsIgnoreCase(configAdapter.getDatabaseType()) ? "MySQL" : "SQLite";

        StartupLogger.printLogo(version, platformStr, storageType);
        
        if (getServer().getClass().getName().contains("Mock")) {
            StartupLogger.printStep("Running in MockBukkit environment.");
        }
        
        StartupLogger.printStep("Loading configuration...");
        this.platform = new PaperPlatformAdapter();
        
        StartupLogger.printStep("Loading storage provider... [" + storageType + "]");
        this.database = new DatabaseAdapter(this, configAdapter);
        if (!database.initialize()) {
            StartupLogger.printError("Failed to initialize database connection!");
            if ("mysql".equalsIgnoreCase(configAdapter.getDatabaseType())) {
                StartupLogger.printError("Please check your MySQL credentials in config.yml.");
            }
        }

        // Network Initialization
        StartupLogger.printStep("Initializing network synchronization...");
        syncPort = new RedisAdapter(this, configAdapter);
        if (configAdapter.isRedisEnabled()) {
            boolean redisConnected = syncPort.initialize();
            if (redisConnected) {
                getLogger().info("Redis Pub/Sub successfully initialized.");
            } else {
                getLogger().warning("Redis is enabled but failed to connect. Running in standalone mode.");
            }
        }
        
        // Service Initialization
        StartupLogger.printStep("Initializing services...");
        renderer = new BukkitNametagRenderer(this, configAdapter.getViewDistance(), configAdapter.getLineSpacing());
        nametagService = new NametagService(configAdapter, platform, renderer, database, syncPort);

        // Register Command
        if (!getServer().getClass().getName().contains("Mock")) {
            LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
            manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                final Commands commands = event.registrar();
                EligiusNametagCommand.createCommand(this, nametagService, commands);
            });
        }

        getServer().getPluginManager().registerEvents(this, this);
        
        StartupLogger.printStep("Performing initial data load...");
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Tameable t : w.getEntitiesByClass(org.bukkit.entity.Tameable.class)) {
                if (t.isTamed()) {
                    platform.addTamedMob(t.getUniqueId());
                }
            }
        }
        
        startTask();
        
        if (configAdapter.isCheckUpdates()) {
            UpdateChecker.fetch(version);
        }
        
        StartupLogger.printStep("Initializing metrics...");
        Metrics metrics = new Metrics(this, 31756);
        metrics.addCustomChart(new SimplePie("database_type", () -> storageType));
        metrics.addCustomChart(new SimplePie("platform", () -> platformStr));
        
        long endTime = System.currentTimeMillis();
        StartupLogger.printSuccess(endTime - startTime);
    }

    @Override
    public void onDisable() {
        if (nametagService != null) {
            nametagService.cleanup();
        }
        if (database != null) {
            database.close();
        }
        if (syncPort != null) {
            syncPort.close();
        }
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        getLogger().info("Plugin successfully disabled");
    }

    private void startTask() {
        long ticks = Math.round(configAdapter.getInterval() * 20L);
        if (ticks < 1) ticks = 1;
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                nametagService.updateAllNametags();
            }, 1L, ticks);
            taskId = foliaTask.hashCode();
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                nametagService.updateAllNametags();
            }, ticks, ticks);
        }
    }
    
    public YamlConfigAdapter getConfigAdapter() {
        return configAdapter;
    }
    
    public void reloadPlugin() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        configAdapter.reload();
        startTask();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        renderer.destroyNametag(event.getPlayer().getUniqueId());
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("eligiusnametag.admin") && UpdateChecker.isUpdateAvailable()) {
            String header = configAdapter.getMessage("update_available_header");
            String versions = configAdapter.getMessage("update_available_versions");
            String download = configAdapter.getMessage("update_available_download");
            
            if (header != null && !header.isEmpty()) {
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(header));
                
                if (versions != null && !versions.isEmpty()) {
                    versions = versions.replace("{current}", getPluginMeta().getVersion())
                                       .replace("{new}", UpdateChecker.getLatestVersion());
                    event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(versions));
                }
                
                if (download != null && !download.isEmpty()) {
                    download = download.replace("{url}", UpdateChecker.getDownloadUrl());
                    event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(download));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity e : event.getEntities()) {
            if (e instanceof org.bukkit.entity.Tameable) {
                org.bukkit.entity.Tameable t = (org.bukkit.entity.Tameable) e;
                if (t.isTamed()) {
                    platform.addTamedMob(t.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (org.bukkit.entity.Entity e : event.getEntities()) {
            if (e instanceof org.bukkit.entity.Tameable) {
                platform.removeTamedMob(e.getUniqueId());
                renderer.destroyNametag(e.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Tameable) {
            platform.removeTamedMob(event.getEntity().getUniqueId());
            renderer.destroyNametag(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        platform.addTamedMob(event.getEntity().getUniqueId());
    }
}
