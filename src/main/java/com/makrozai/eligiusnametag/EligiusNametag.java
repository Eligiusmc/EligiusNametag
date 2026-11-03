package com.makrozai.eligiusnametag;

import com.makrozai.eligiusnametag.adapter.config.YamlConfigAdapter;
import com.makrozai.eligiusnametag.adapter.database.DatabaseAdapter;
import com.makrozai.eligiusnametag.adapter.network.RedisAdapter;
import com.makrozai.eligiusnametag.adapter.platform.PaperPlatformAdapter;
import com.makrozai.eligiusnametag.adapter.renderer.BukkitNametagRenderer;
import com.makrozai.eligiusnametag.domain.port.DatabasePort;
import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import com.makrozai.eligiusnametag.domain.port.SyncPort;
import com.makrozai.eligiusnametag.domain.service.AnimationManager;
import com.makrozai.eligiusnametag.domain.service.NametagService;
import com.makrozai.eligiusnametag.domain.service.UpdateChecker;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public class EligiusNametag extends JavaPlugin implements Listener {
    private YamlConfigAdapter configAdapter;
    private PlatformPort platform;
    private DatabasePort database;
    private SyncPort syncPort;
    private BukkitNametagRenderer renderer;
    private NametagService nametagService;
    private AnimationManager animationManager;
    private int taskId = -1;

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
        
        String platformStr = isFolia ? "Folia" : "Paper";
        String version = getDescription().getVersion();
        
        configAdapter = new YamlConfigAdapter(this);
        String storageType = "mysql".equalsIgnoreCase(configAdapter.getDatabaseType()) ? "MySQL" : "SQLite";

        StartupLogger.printLogo(version, platformStr, storageType);
        
        if (getServer().getClass().getName().contains("Mock")) {
            StartupLogger.printStep("Running in MockBukkit environment.");
        }
        
        StartupLogger.printStep("Loading configuration...");
        animationManager = new AnimationManager(this);
        animationManager.loadAnimations();
        this.platform = new PaperPlatformAdapter(animationManager);
        
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
        renderer = new BukkitNametagRenderer(this, configAdapter.getLineSpacing());
        nametagService = new NametagService(configAdapter, platform, renderer, database, syncPort);

        // Register Command
        if (!getServer().getClass().getName().contains("Mock")) {
            org.bukkit.command.PluginCommand cmd = getCommand("eligiusnametag");
            if (cmd != null) {
                EligiusNametagCommand commandExecutor = new EligiusNametagCommand(this, nametagService);
                cmd.setExecutor(commandExecutor);
                cmd.setTabCompleter(commandExecutor);
                cmd.setAliases(configAdapter.getCommandAliases());
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
        
        StartupLogger.printStep("Performing initial data load...");
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Tameable t : w.getEntitiesByClass(org.bukkit.entity.Tameable.class)) {
                if (shouldTrackPet(t)) {
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
        cancelActiveTask();
        getLogger().info("Plugin successfully disabled");
    }

    private void cancelActiveTask() {
        if (taskId == -1) return;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            com.makrozai.eligiusnametag.adapter.platform.FoliaSchedulerAdapter.cancelCurrentTask();
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskId = -1;
    }

    private void startTask() {
        long ticks = Math.round(configAdapter.getInterval() * 20L);
        if (ticks < 1) ticks = 1;
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            taskId = com.makrozai.eligiusnametag.adapter.platform.FoliaSchedulerAdapter.startTask(this, () -> {
                animationManager.tick();
                nametagService.updateAllNametags();
            }, ticks);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                animationManager.tick();
                nametagService.updateAllNametags();
            }, ticks, ticks);
        }
    }
    
    public YamlConfigAdapter getConfigAdapter() {
        return configAdapter;
    }
    
    public void reloadPlugin() {
        cancelActiveTask();
        configAdapter.reload();
        if (animationManager != null) {
            animationManager.loadAnimations();
        }
        if (renderer != null) {
            renderer.reloadConfig(configAdapter.getLineSpacing());
        }
        startTask();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        renderer.destroyNametag(event.getPlayer().getUniqueId());
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission("eligiusnametag.admin") && UpdateChecker.isUpdateAvailable()) {
            String header = configAdapter.getMessage("update_available_header");
            String versions = configAdapter.getMessage("update_available_versions");
            String download = configAdapter.getMessage("update_available_download");
            
            if (header != null && !header.isEmpty()) {
                event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(header));
                
                if (versions != null && !versions.isEmpty()) {
                    versions = versions.replace("{current}", getDescription().getVersion())
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
        renderer.destroyNametag(event.getPlayer().getUniqueId());
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        renderer.destroyNametag(event.getPlayer().getUniqueId());
        renderer.clearViewer(event.getPlayer().getUniqueId());
    }

    /**
     * Fixes #46 / #47: a nametag TextDisplay is mounted as a passenger of the player,
     * turning the player into a "vehicle". CraftBukkit refuses to teleport a vehicle
     * (cross-world returns false; pre-1.21.10 same-world is also blocked), which is why
     * Multiverse-Core and other teleport plugins silently failed.
     *
     * We eject the displays right before the teleport resolves (MONITOR runs last, still
     * synchronously before CraftBukkit performs the move) so the player has no passenger.
     * The renderer re-mounts on the next tick; a 1-tick follow-up remount inside
     * ejectPassengers() minimises any floating ghost at the origin.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        renderer.ejectPassengers(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Tameable) {
            renderer.ejectPassengers(event.getEntity().getUniqueId());
        }
    }

    /**
     * Most rideable mobs accept only ONE passenger. Our TextDisplay occupies that slot,
     * so the vanilla mount silently fails (right-click does nothing). Free the slot
     * before vanilla resolves the interaction; do not cancel the event.
     *
     * Eject only for rideable types â€” right-clicking a wolf/cat with food must not
     * yank its nametag for a tick.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        org.bukkit.entity.Entity clicked = event.getRightClicked();
        if (isMountableRide(clicked)) {
            renderer.ejectPassengers(clicked.getUniqueId());
        }
        // Spigot fallback: PlayerNameEntityEvent is Paper-only. The Name Tag is
        // applied during vanilla interaction, so re-check one tick later and admit
        // any newly-named Tameable into the render cache.
        if (clicked instanceof org.bukkit.entity.Tameable) {
            final java.util.UUID clickedId = clicked.getUniqueId();
            java.util.function.Consumer<org.bukkit.entity.Entity> admit = e -> {
                if (e instanceof org.bukkit.entity.Tameable t && shouldTrackPet(t)) {
                    platform.addTamedMob(t.getUniqueId());
                }
            };
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                // Folia: Bukkit scheduler is unsupported — use the entity's own scheduler.
                clicked.getScheduler().run(this, task -> {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(clickedId);
                    if (e != null) admit.accept(e);
                }, () -> {});
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                getServer().getScheduler().runTask(this, () -> {
                    org.bukkit.entity.Entity ent = Bukkit.getEntity(clickedId);
                    if (ent != null) admit.accept(ent);
                });
            }
        }
    }

    /**
     * Player actually mounted: hide the nametag for the duration of the ride
     * (decision: hidden-while-ridden). The render loop re-spawns on the next tick
     * after dismount once no player passenger remains.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player) {
            renderer.destroyNametag(event.getMount().getUniqueId());
        }
    }

    /** Rideable types where a single-passenger slot conflict blocks mounting. */
    static boolean isMountableRide(org.bukkit.entity.Entity entity) {
        return entity instanceof org.bukkit.entity.AbstractHorse
                || entity instanceof org.bukkit.entity.Llama
                || entity instanceof org.bukkit.entity.Pig
                || entity instanceof org.bukkit.entity.Strider;
    }

    /**
     * Pets.yml documents "false = only mobs with a Name Tag item get holograms":
     * the contract is name-based (tamed OR named), not tame-only. Untamed rideables
     * like camels that only need a saddle+label must enter the render cache too.
     */
    static boolean shouldTrackPet(org.bukkit.entity.Tameable t) {
        return t.isTamed() || t.getCustomName() != null;
    }

    /**
     * Paper: fires when a Name Tag is applied. Spigot has no such event â€” the
     * interact fallback below covers renaming on those platforms.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerNameEntity(io.papermc.paper.event.player.PlayerNameEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Tameable t) {
            platform.addTamedMob(t.getUniqueId());
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity e : event.getEntities()) {
            if (e instanceof org.bukkit.entity.Tameable) {
                org.bukkit.entity.Tameable t = (org.bukkit.entity.Tameable) e;
                if (shouldTrackPet(t)) {
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
