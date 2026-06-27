package com.makrozai.eligiusnametag.adapter.platform;

import com.makrozai.eligiusnametag.EligiusNametag;
import org.bukkit.Bukkit;

public class FoliaSchedulerAdapter {
    private static io.papermc.paper.threadedregions.scheduler.ScheduledTask currentTask;

    public static int startTask(EligiusNametag plugin, Runnable runnable, long ticks) {
        cancelCurrentTask();
        currentTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), 1L, ticks);
        return currentTask.hashCode();
    }

    public static void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }
}
