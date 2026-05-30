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
    private FileConfiguration playersConfig;
    private FileConfiguration petsConfig;
    private FileConfiguration langConfig;

    private File configFile;
    private File playersFile;
    private File petsFile;
    private File langFile;

    public YamlConfigAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public void reload() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
            playersFile = new File(plugin.getDataFolder(), "players.yml");
            petsFile = new File(plugin.getDataFolder(), "pets.yml");
        }
        
        // Save defaults if not exist
        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        if (!playersFile.exists()) plugin.saveResource("players.yml", false);
        if (!petsFile.exists()) plugin.saveResource("pets.yml", false);
        
        // Save language defaults
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        File enLang = new File(langDir, "en.yml");
        if (!enLang.exists()) plugin.saveResource("lang/en.yml", false);
        File esLang = new File(langDir, "es.yml");
        if (!esLang.exists()) plugin.saveResource("lang/es.yml", false);
        File ptLang = new File(langDir, "pt.yml");
        if (!ptLang.exists()) plugin.saveResource("lang/pt.yml", false);
        File frLang = new File(langDir, "fr.yml");
        if (!frLang.exists()) plugin.saveResource("lang/fr.yml", false);
        File deLang = new File(langDir, "de.yml");
        if (!deLang.exists()) plugin.saveResource("lang/de.yml", false);
        File ruLang = new File(langDir, "ru.yml");
        if (!ruLang.exists()) plugin.saveResource("lang/ru.yml", false);

        config = YamlConfiguration.loadConfiguration(configFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        petsConfig = YamlConfiguration.loadConfiguration(petsFile);

        String lang = config.getString("language", "en");
        langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file " + lang + ".yml not found. Falling back to en.yml");
            langFile = new File(langDir, "en.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream)));
        }
    }

    @Override
    public java.util.List<String> getPlayerNametagTemplate(String group) {
        if (group != null && playersConfig.contains("groups." + group)) {
            return getTemplateAsList(playersConfig, "groups." + group);
        }
        return getTemplateAsList(playersConfig, "default_format");
    }
    
    @Override
    public java.util.List<String> getTamedMobNametagTemplate(String group) {
        if (group != null && petsConfig.contains("groups." + group)) {
            return getTemplateAsList(petsConfig, "groups." + group);
        }
        return getTemplateAsList(petsConfig, "default_format");
    }

    /**
     * Reads a config path that can be either a String (legacy \n format)
     * or a List of Strings (new YAML list format). Returns a unified List<String>.
     */
    private java.util.List<String> getTemplateAsList(FileConfiguration cfg, String path) {
        if (!cfg.contains(path)) {
            return java.util.Collections.emptyList();
        }
        if (cfg.isList(path)) {
            return cfg.getStringList(path);
        }
        // Legacy support: single string with \n separators
        String value = cfg.getString(path, "");
        if (value.isEmpty()) return java.util.Collections.emptyList();
        return java.util.Arrays.asList(value.split("\\\\n|\\n"));
    }

    @Override
    public boolean isTamedMobsEnabled() {
        return petsConfig.getBoolean("enabled", true);
    }

    @Override
    public boolean isTamedMobsShowUnnamed() {
        return petsConfig.getBoolean("show_unnamed", false);
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
        String message = langConfig.getString(key, "");
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        if (key.startsWith("help_")) {
            return message;
        }
        
        String prefix = config.getString("prefix", "");
        return prefix + message;
    }

    @Override
    public boolean hasLanguage(String lang) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        File checkFile = new File(langDir, lang + ".yml");
        return checkFile.exists();
    }

    @Override
    public void setLanguage(String lang) {
        config.set("language", lang);
        try {
            config.save(configFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save config.yml!");
        }
    }

    @Override
    public void setPetsEnabled(boolean enabled) {
        petsConfig.set("enabled", enabled);
        try {
            petsConfig.save(petsFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save pets.yml!");
        }
    }

    @Override
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    @Override
    public String getDatabaseHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    @Override
    public int getDatabasePort() {
        return config.getInt("database.mysql.port", 3306);
    }

    @Override
    public String getDatabaseName() {
        return config.getString("database.mysql.database", "eligiusnametag");
    }

    @Override
    public String getDatabaseUsername() {
        return config.getString("database.mysql.username", "root");
    }

    @Override
    public String getDatabasePassword() {
        return config.getString("database.mysql.password", "");
    }
}
