package com.makrozai.eligiusnametag.adapter.platform;

import com.makrozai.eligiusnametag.EligiusNametag;
import org.bukkit.Bukkit;

public class FoliaSchedulerAdapter {
    public static int startTask(EligiusNametag plugin, Runnable runnable, long ticks) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            runnable.run();
        }, 1L, ticks);
        return foliaTask.hashCode();
    }
}
