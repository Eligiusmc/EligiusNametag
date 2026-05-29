package com.makrozai.eligiusnametag.adapter.platform;

import com.makrozai.eligiusnametag.domain.port.PlatformPort;
import com.makrozai.eligiusnametag.adapter.config.YamlConfigAdapter;
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

public class PaperPlatformAdapter implements PlatformPort {
    private Permission vaultPerms;

    public PaperPlatformAdapter() {
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
    public List<UUID> getTamedMobs() {
        List<UUID> list = new ArrayList<>();
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Tameable t : w.getEntitiesByClass(Tameable.class)) {
                if (t.isTamed()) {
                    list.add(t.getUniqueId());
                }
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
        return true; // For tamed mobs, assume visible if in same world.
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

    @Override
    public String parsePlaceholders(UUID targetId, String text) {
        org.bukkit.entity.Entity target = Bukkit.getEntity(targetId);
        String parsed = text;
        org.bukkit.OfflinePlayer op = null;
        
        if (target instanceof Player) {
            Player p = (Player) target;
            op = p;
            parsed = parsed.replace("<PLAYER>", p.getName());
            parsed = parsed.replace("<DISPLAYNAME>", p.displayName() != null ? p.displayName().toString() : p.getName());
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
            parsed = parsed.replace("<DISPLAYNAME>", mobName);
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
        return parsed;
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
