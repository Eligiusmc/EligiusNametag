package com.makrozai.eligiusnametag.domain.port;

import java.util.List;


public interface ConfigPort {
    List<String> getPlayerNametagTemplate(String group);
    List<String> getTamedMobNametagTemplate(String group);
    boolean isTamedMobsEnabled();
    boolean isTamedMobsShowUnnamed();
    int getViewDistance();
    double getLineSpacing();
    double getYOffset();
    double getInterval();
    java.util.List<String> getCommandAliases();
    String getMessage(String key);
    void reload();
    
    boolean hasLanguage(String lang);
    void setLanguage(String lang);
    void setPetsEnabled(boolean enabled);
    boolean isCheckUpdates();
    
    String getDatabaseType();
    String getDatabaseHost();
    int getDatabasePort();
    String getDatabaseName();
    String getDatabaseUsername();
    String getDatabasePassword();
    
    boolean isRedisEnabled();
    String getRedisHost();
    int getRedisPort();
    String getRedisPassword();
}
