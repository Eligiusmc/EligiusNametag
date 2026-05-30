package com.makrozai.eligiusnametag.adapter.config;

import com.makrozai.eligiusnametag.domain.port.ConfigPort;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;


public class YamlConfigAdapter implements ConfigPort {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public YamlConfigAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        if (config.getInt("config_version", 0) < 6) {
            plugin.getLogger().warning("Config version is old. Migrating to v6...");
            File oldConfig = new File(plugin.getDataFolder(), "config.old.yml");
            configFile.renameTo(oldConfig);
            plugin.saveResource("config.yml", true);
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().warning("Old config saved as config.old.yml");
        }

        // Load defaults
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }
    }

    @Override
    public java.util.List<String> getPlayerNametagTemplate(String group) {
        if (group != null && config.contains("players.groups." + group)) {
            return getTemplateAsList("players.groups." + group);
        }
        return getTemplateAsList("players.default_format");
    }
    
    @Override
    public java.util.List<String> getTamedMobNametagTemplate(String group) {
        if (group != null && config.contains("pets.groups." + group)) {
            return getTemplateAsList("pets.groups." + group);
        }
        return getTemplateAsList("pets.default_format");
    }

    /**
     * Reads a config path that can be either a String (legacy \n format)
     * or a List of Strings (new YAML list format). Returns a unified List<String>.
     */
    private java.util.List<String> getTemplateAsList(String path) {
        if (!config.contains(path)) {
            return java.util.Collections.emptyList();
        }
        if (config.isList(path)) {
            return config.getStringList(path);
        }
        // Legacy support: single string with \n separators
        String value = config.getString(path, "");
        if (value.isEmpty()) return java.util.Collections.emptyList();
        return java.util.Arrays.asList(value.split("\\\\n|\\n"));
    }

    @Override
    public boolean isTamedMobsEnabled() {
        return config.getBoolean("pets.enabled", true);
    }

    @Override
    public boolean isTamedMobsShowUnnamed() {
        return config.getBoolean("pets.show_unnamed", false);
    }

    @Override
    public int getViewDistance() {
        return config.getInt("view_distance", 64);
    }

    @Override
    public double getLineSpacing() {
        return config.getDouble("line_spacing", 0.275);
    }

    @Override
    public double getYOffset() {
        return config.getDouble("y_offset", 0.35);
    }

    @Override
    public double getInterval() {
        return config.getDouble("interval", 0.5);
    }

    @Override
    public java.util.List<String> getCommandAliases() {
        if (config.contains("command_aliases")) {
            return config.getStringList("command_aliases");
        }
        return java.util.Collections.singletonList("eligiusnametag");
    }

    @Override
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }
}
