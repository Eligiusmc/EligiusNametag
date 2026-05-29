package com.makrozai.eligiusnametag;

import com.makrozai.eligiusnametag.adapter.config.YamlConfigAdapter;
import com.makrozai.eligiusnametag.adapter.platform.PaperPlatformAdapter;
import com.makrozai.eligiusnametag.adapter.renderer.ProtocolLibNametagRenderer;
import com.makrozai.eligiusnametag.domain.service.NametagService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.makrozai.eligiusnametag.adapter.database.DatabaseAdapter;

public class EligiusNametag extends JavaPlugin implements Listener {
    private YamlConfigAdapter configAdapter;
    private PaperPlatformAdapter platformAdapter;
    private ProtocolLibNametagRenderer rendererAdapter;
    private DatabaseAdapter databaseAdapter;
    private NametagService nametagService;
    private int taskId = -1;

    @Override
    public void onEnable() {
        if (getServer().getClass().getName().contains("Mock")) {
            getLogger().info("Running in MockBukkit environment.");
        }
        
        configAdapter = new YamlConfigAdapter(this);
        platformAdapter = new PaperPlatformAdapter();
        rendererAdapter = new ProtocolLibNametagRenderer(configAdapter.getLineSpacing(), configAdapter.getViewDistance());
        
        databaseAdapter = new DatabaseAdapter(this);
        databaseAdapter.initialize();

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
        startTask();
        getLogger().info("EligiusNametag enabled successfully.");
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
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            nametagService.updateAllNametags();
        }, ticks, ticks);
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
        rendererAdapter.clearViewer(event.getPlayer().getUniqueId());
    }
}
