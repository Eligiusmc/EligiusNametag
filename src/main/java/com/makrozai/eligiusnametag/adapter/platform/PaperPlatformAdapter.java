package com.makrozai.eligiusnametag.adapter.platform;

import com.makrozai.eligiusnametag.domain.port.PlatformPort;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.makrozai.eligiusnametag.domain.service.AnimationManager;

public class PaperPlatformAdapter implements PlatformPort {
    private Permission vaultPerms;
    private final AnimationManager animationManager;
    private final java.util.Set<UUID> cachedTamedMobs = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    // Fallback universal para 1.21.1 (GENERIC_MAX_HEALTH) y 1.21.2+ (MAX_HEALTH)
    private static org.bukkit.attribute.Attribute CACHED_MAX_HEALTH_ATTRIBUTE;
    static {
        try {
            // Intentar 1.21.2+
            CACHED_MAX_HEALTH_ATTRIBUTE = (org.bukkit.attribute.Attribute) org.bukkit.attribute.Attribute.class.getField("MAX_HEALTH").get(null);
        } catch (Exception e) {
            try {
                // Fallback a 1.21.1 o inferior
                CACHED_MAX_HEALTH_ATTRIBUTE = (org.bukkit.attribute.Attribute) org.bukkit.attribute.Attribute.class.getField("GENERIC_MAX_HEALTH").get(null);
            } catch (Exception ex) {
                CACHED_MAX_HEALTH_ATTRIBUTE = null;
            }
        }
    }

    public PaperPlatformAdapter(AnimationManager animationManager) {
        this.animationManager = animationManager;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                vaultPerms = rsp.getProvider();
            }
        }
    }

    public String getPrimaryGroup(Player player) {
        if (vaultPerms != null && vaultPerms.hasGroupSupport()) {
            try {
                return vaultPerms.getPrimaryGroup(player);
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public List<UUID> getOnlinePlayers() {
        List<UUID> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            list.add(p.getUniqueId());
        }
        return list;
    }

    @Override
    public void prepareTick() { }

    @Override
    public void endTick() { }

    public void addTamedMob(UUID uuid) {
        cachedTamedMobs.add(uuid);
    }

    public void removeTamedMob(UUID uuid) {
        cachedTamedMobs.remove(uuid);
    }

    @Override
    public List<UUID> getTamedMobs() {
        List<UUID> list = new ArrayList<>();
        for (UUID uuid : cachedTamedMobs) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(uuid);
            if (e instanceof Tameable && ((Tameable) e).isTamed() && !e.isDead() && e.isValid()) {
                list.add(uuid);
            } else {
                cachedTamedMobs.remove(uuid);
            }
        }
        return list;
    }

    @Override
    public boolean canViewerSeeTarget(UUID viewerId, UUID targetId) {
        Player viewer = Bukkit.getPlayer(viewerId);
        Player target = Bukkit.getPlayer(targetId);
        if (viewer == null) return false;
        if (target != null) {
            if (viewer.equals(target)) {
                return viewer.hasPermission("eligiusnametag.viewself");
            }
            return viewer.canSee(target);
        }
        org.bukkit.entity.Entity entity = Bukkit.getEntity(targetId);
        if (entity != null) {
            // Utilizar una comprobación de distancia segura en lugar de getTrackedBy()
            // 64 bloques de distancia es el límite estándar de tracking.
            if (entity.getWorld().equals(viewer.getWorld())) {
                return entity.getLocation().distanceSquared(viewer.getLocation()) <= (64.0 * 64.0);
            }
        }
        return false;
    }

    @Override
    public boolean isSameWorld(UUID a, UUID b) {
        org.bukkit.entity.Entity entityA = Bukkit.getEntity(a);
        org.bukkit.entity.Entity entityB = Bukkit.getEntity(b);
        if (entityA == null || entityB == null) return false;
        return entityA.getWorld().equals(entityB.getWorld());
    }

    @Override
    public boolean isGloballyHidden(UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            return target.isSneaking() || target.hasPotionEffect(PotionEffectType.INVISIBILITY) || target.isInvisible();
        }
        return false;
    }

    @Override
    public boolean hasPermission(UUID targetId, String permission) {
        Player player = Bukkit.getPlayer(targetId);
        return player != null && player.hasPermission(permission);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasCustomName(UUID targetId) {
        org.bukkit.entity.Entity entity = Bukkit.getEntity(targetId);
        return entity != null && entity.getCustomName() != null;
    }

    @Override
    public void disableVanillaNametag(UUID targetId) {
        org.bukkit.entity.Entity entity = Bukkit.getEntity(targetId);
        if (entity != null && entity.isCustomNameVisible()) {
            entity.setCustomNameVisible(false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public net.kyori.adventure.text.Component parsePlaceholders(UUID targetId, String text) {
        org.bukkit.entity.Entity target = Bukkit.getEntity(targetId);
        String parsed = text;
        org.bukkit.OfflinePlayer op = null;
        
        if (target instanceof Player) {
            Player p = (Player) target;
            op = p;
            parsed = parsed.replace("<PLAYER>", p.getName());
            parsed = parsed.replace("<DISPLAYNAME>", p.getDisplayName() != null ? p.getDisplayName() : p.getName());
            
            org.bukkit.attribute.AttributeInstance healthAttr = p.getAttribute(CACHED_MAX_HEALTH_ATTRIBUTE);
            double maxHealth = healthAttr != null ? healthAttr.getValue() : 20.0;
            parsed = parsed.replace("<HEALTH>", String.valueOf(Math.round(p.getHealth())));
            parsed = parsed.replace("<MAX_HEALTH>", String.valueOf(Math.round(maxHealth)));
            
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                parsed = PlaceholderAPI.setPlaceholders(p, parsed);
            }
        } else if (target instanceof Tameable) {
            Tameable t = (Tameable) target;
            String mobName = t.getCustomName() != null ? t.getCustomName() : t.getType().name();
            String ownerName = t.getOwner() != null ? t.getOwner().getName() : "Owner";
            if (t.getOwner() != null && t.getOwner().getUniqueId() != null) {
                op = Bukkit.getOfflinePlayer(t.getOwner().getUniqueId());
                if (ownerName == null) {
                     ownerName = op.getName();
                }
            }
            if (ownerName == null) ownerName = "Owner";
            parsed = parsed.replace("<PLAYER>", ownerName);
            parsed = parsed.replace("<OWNER>", ownerName);
            parsed = parsed.replace("<DISPLAYNAME>", mobName);
            parsed = parsed.replace("<CUSTOM_NAME>", t.getCustomName() != null ? t.getCustomName() : "");
            
            String rawType = t.getType().name().toLowerCase().replace("_", " ");
            StringBuilder capType = new StringBuilder();
            for (String word : rawType.split(" ")) {
                if (!word.isEmpty()) {
                    capType.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                }
            }
            parsed = parsed.replace("<TYPE>", capType.toString().trim());
            
            if (target instanceof org.bukkit.entity.Ageable) {
                boolean isBaby = !((org.bukkit.entity.Ageable) target).isAdult();
                parsed = parsed.replace("<AGE>", isBaby ? "Baby" : "Adult");
            }
            
            org.bukkit.attribute.AttributeInstance healthAttr = t.getAttribute(CACHED_MAX_HEALTH_ATTRIBUTE);
            double maxHealth = healthAttr != null ? healthAttr.getValue() : 20.0;
            parsed = parsed.replace("<HEALTH>", String.valueOf(Math.round(t.getHealth())));
            parsed = parsed.replace("<MAX_HEALTH>", String.valueOf(Math.round(maxHealth)));
            
            if (op != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                parsed = PlaceholderAPI.setPlaceholders(op, parsed);
            }
        }
        
        if (op != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(":([a-zA-Z0-9_\\-]+):");
            java.util.regex.Matcher matcher = pattern.matcher(parsed);
            StringBuffer sb = new StringBuffer();
            boolean found = false;

            while (matcher.find()) {
                String placeholder = "%img_" + matcher.group(1) + "%";
                String resolved = PlaceholderAPI.setPlaceholders(op, placeholder);
                
                if (resolved.equals(placeholder)) {
                    matcher.appendReplacement(sb, matcher.group(0));
                } else {
                    matcher.appendReplacement(sb, resolved);
                }
                found = true;
            }
            if (found) {
                matcher.appendTail(sb);
                parsed = sb.toString();
            }
        }
        
        if (animationManager != null) {
            java.util.regex.Pattern animPattern = java.util.regex.Pattern.compile("<anim:([a-zA-Z0-9_\\-]+)>(.*?)</anim:\\1>");
            java.util.regex.Matcher animMatcher = animPattern.matcher(parsed);
            StringBuffer animSb = new StringBuffer();
            boolean animFound = false;
            while (animMatcher.find()) {
                String animName = animMatcher.group(1);
                String animText = animMatcher.group(2);
                String resolvedAnim = animationManager.applyAnimation(animText, animName);
                animMatcher.appendReplacement(animSb, resolvedAnim);
                animFound = true;
            }
            if (animFound) {
                animMatcher.appendTail(animSb);
                parsed = animSb.toString();
            }
        }

        // Convert standard legacy colors to MiniMessage format just in case PlaceholderAPI returns them
        if (parsed.contains("&") || parsed.contains("§")) {
            parsed = parsed.replace("§", "&");
            parsed = parsed.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                           .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                           .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                           .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                           .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                           .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                           .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");
        }
        
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(parsed);
    }

    @Override
    public String getPrimaryGroup(UUID targetId) {
        Player player = Bukkit.getPlayer(targetId);
        if (player != null && vaultPerms != null && vaultPerms.hasGroupSupport()) {
            try {
                return vaultPerms.getPrimaryGroup(player);
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public String getOwnerPrimaryGroup(UUID targetId) {
        org.bukkit.entity.Entity entity = Bukkit.getEntity(targetId);
        if (entity instanceof Tameable) {
            Tameable t = (Tameable) entity;
            if (t.getOwner() != null && t.getOwner().getUniqueId() != null) {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(t.getOwner().getUniqueId());
                if (op.isOnline()) {
                    return getPrimaryGroup(op.getUniqueId());
                } else if (vaultPerms != null && vaultPerms.hasGroupSupport()) {
                    // Try getting primary group for offline player (some Vault implementations support it, otherwise fallback null)
                    try {
                        return vaultPerms.getPrimaryGroup(null, op);
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }
}
