package com.makrozai.eligiusnametag;

import com.makrozai.eligiusnametag.adapter.config.YamlConfigAdapter;
import com.makrozai.eligiusnametag.adapter.platform.PaperPlatformAdapter;
import com.makrozai.eligiusnametag.adapter.renderer.BukkitNametagRenderer;
import com.makrozai.eligiusnametag.domain.service.NametagService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.makrozai.eligiusnametag.adapter.database.DatabaseAdapter;
import com.makrozai.eligiusnametag.domain.service.UpdateChecker;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class EligiusNametag extends JavaPlugin implements Listener {
    private YamlConfigAdapter configAdapter;
    private PaperPlatformAdapter platformAdapter;
    private BukkitNametagRenderer rendererAdapter;
    private DatabaseAdapter databaseAdapter;
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
        
        String platform = isFolia ? "Folia" : "Paper";
        String version = getPluginMeta().getVersion();
        
        configAdapter = new YamlConfigAdapter(this);
        String storageType = "mysql".equalsIgnoreCase(configAdapter.getDatabaseType()) ? "MySQL" : "SQLite";

        StartupLogger.printLogo(version, platform, storageType);
        
        if (getServer().getClass().getName().contains("Mock")) {
            StartupLogger.printStep("Running in MockBukkit environment.");
        }
        
        StartupLogger.printStep("Loading configuration...");
        platformAdapter = new PaperPlatformAdapter();
        rendererAdapter = new BukkitNametagRenderer(this, configAdapter.getViewDistance(), configAdapter.getLineSpacing());
        
        StartupLogger.printStep("Loading storage provider... [" + storageType + "]");
        databaseAdapter = new DatabaseAdapter(this, configAdapter);
        if (!databaseAdapter.initialize()) {
            StartupLogger.printError("Failed to initialize database connection!");
            if ("mysql".equalsIgnoreCase(configAdapter.getDatabaseType())) {
                StartupLogger.printError("Please check your MySQL credentials in config.yml.");
            }
        }

        StartupLogger.printStep("Loading internal managers...");
        nametagService = new NametagService(configAdapter, platformAdapter, rendererAdapter, databaseAdapter);

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
        // Carga inicial de mascotas para no perderlas al hacer reload
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Tameable t : w.getEntitiesByClass(org.bukkit.entity.Tameable.class)) {
                if (t.isTamed()) {
                    platformAdapter.addTamedMob(t.getUniqueId());
                }
            }
        }
        
        startTask();
        
        if (configAdapter.isCheckUpdates()) {
            UpdateChecker.fetch(version);
        }
        
        long endTime = System.currentTimeMillis();
        StartupLogger.printSuccess(endTime - startTime);
    }

    @Override
    public void onDisable() {
        if (nametagService != null) {
            nametagService.cleanup();
        }
        if (databaseAdapter != null) {
            databaseAdapter.close();
        }
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        getLogger().info("EligiusNametag disabled.");
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
        rendererAdapter.destroyNametag(event.getPlayer().getUniqueId());
        rendererAdapter.clearViewer(event.getPlayer().getUniqueId());
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
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity e : event.getEntities()) {
            if (e instanceof org.bukkit.entity.Tameable) {
                org.bukkit.entity.Tameable t = (org.bukkit.entity.Tameable) e;
                if (t.isTamed()) {
                    platformAdapter.addTamedMob(t.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (org.bukkit.entity.Entity e : event.getEntities()) {
            if (e instanceof org.bukkit.entity.Tameable) {
                platformAdapter.removeTamedMob(e.getUniqueId());
                rendererAdapter.destroyNametag(e.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Tameable) {
            platformAdapter.removeTamedMob(event.getEntity().getUniqueId());
            rendererAdapter.destroyNametag(event.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        platformAdapter.addTamedMob(event.getEntity().getUniqueId());
    }
}
