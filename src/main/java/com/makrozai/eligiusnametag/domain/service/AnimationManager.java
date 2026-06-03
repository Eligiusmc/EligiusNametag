package com.makrozai.eligiusnametag.domain.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {
    private final JavaPlugin plugin;
    private final Map<String, Animation> animations = new ConcurrentHashMap<>();
    private int globalTick = 0;

    public AnimationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAnimations() {
        animations.clear();
        globalTick = 0;
        
        File animFolder = new File(plugin.getDataFolder(), "animations");
        if (!animFolder.exists()) {
            animFolder.mkdirs();
            createDefaultAnimation(animFolder);
        }

        File[] files = animFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            
            int interval = config.getInt("interval", 2);
            List<String> frames = config.getStringList("frames");
            
            if (frames.isEmpty()) {
                plugin.getLogger().warning("Animation " + name + " has no frames!");
                continue;
            }
            
            animations.put(name, new Animation(interval, frames));
        }
        
        plugin.getLogger().info("Loaded " + animations.size() + " animations.");
    }

    private void createDefaultAnimation(File animFolder) {
        File defaultFile = new File(animFolder, "rainbow.yml");
        if (!defaultFile.exists()) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.set("interval", 2);
                List<String> frames = new ArrayList<>();
                frames.add("<red>{text}</red>");
                frames.add("<gold>{text}</gold>");
                frames.add("<yellow>{text}</yellow>");
                frames.add("<green>{text}</green>");
                frames.add("<blue>{text}</blue>");
                frames.add("<dark_purple>{text}</dark_purple>");
                frames.add("<light_purple>{text}</light_purple>");
                config.set("frames", frames);
                config.save(defaultFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create default animation!");
            }
        }
    }

    public void tick() {
        globalTick++;
        for (Animation anim : animations.values()) {
            if (globalTick % anim.interval == 0) {
                anim.currentFrameIndex = (anim.currentFrameIndex + 1) % anim.frames.size();
            }
        }
    }

    public String applyAnimation(String text, String animationName) {
        Animation anim = animations.get(animationName);
        if (anim == null) return text;
        
        String frame = anim.frames.get(anim.currentFrameIndex);
        return frame.replace("{text}", text);
    }
    
    public Map<String, Animation> getAnimations() {
        return animations;
    }

    public static class Animation {
        public final int interval;
        public final List<String> frames;
        public int currentFrameIndex = 0;

        public Animation(int interval, List<String> frames) {
            this.interval = Math.max(1, interval);
            this.frames = frames;
        }
    }
}
