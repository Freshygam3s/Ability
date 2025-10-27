package org.freshx;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Ability extends JavaPlugin implements Listener {

    // Cooldowns: Map<player UUID, Map<ability number, last used time>>
    private final HashMap<UUID, HashMap<Integer, Long>> cooldowns = new HashMap<>();

    // Per-ability cooldowns (milliseconds)
    private final long ABILITY1_COOLDOWN = 5 * 1000;
    private final long ABILITY2_COOLDOWN = 8 * 1000;
    private final long ABILITY3_COOLDOWN = 6 * 1000;
    private final long ABILITY4_COOLDOWN = 10 * 1000;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Multi-Ability Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Multi-Ability Plugin Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!command.getName().equalsIgnoreCase("ability")) return false;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /ability <1-4>");
            return true;
        }

        int abilityNum;
        try {
            abilityNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid ability number!");
            return true;
        }

        if (abilityNum < 1 || abilityNum > 4) {
            player.sendMessage(ChatColor.RED + "Ability number must be between 1 and 4!");
            return true;
        }

        // Check and apply cooldown
        if (!canUseAbility(player, abilityNum)) {
            long timeLeft = getTimeLeft(player, abilityNum);
            player.sendMessage(ChatColor.RED + "⏳ Wait " + (timeLeft / 1000.0) + "s before using that ability again!");
            return true;
        }
        setCooldown(player, abilityNum);

        // Execute ability
        switch (abilityNum) {
            case 1 -> abilityLaunch(player);
            case 2 -> abilityFireBurst(player);
            case 3 -> abilityDash(player);
            case 4 -> abilityThrowRocks(player);
        }
        return true;
    }

    // -------------------- ABILITY 1: Launch Up --------------------
    private void abilityLaunch(Player player) {
        player.setVelocity(new Vector(0, 2.5, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.3f);
        player.sendMessage(ChatColor.AQUA + "💫 Air Burst!");

        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks > 20 || player.isOnGround()) cancel();
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.3, 0.2, 0.3, 0.01);
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                ticks++;
            }
        }.runTaskTimer(this, 0L, 2L);
    }

    // -------------------- ABILITY 2: Fire Burst --------------------
    private void abilityFireBurst(Player player) {
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        player.sendMessage(ChatColor.RED + "🔥 Fire Burst!");

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 1, z), 5, 0.1, 0.1, 0.1, 0.01);
        }

        for (Entity e : player.getNearbyEntities(3, 2, 3)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(4.0, player);
                le.setFireTicks(60);
            }
        }
    }

    // -------------------- ABILITY 3: Dash Forward --------------------
    private void abilityDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize().multiply(2.5);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        player.sendMessage(ChatColor.BLUE + "⚡ Dash!");

        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks > 10) cancel();
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    // -------------------- ABILITY 4: Throw Rocks --------------------
    private void abilityThrowRocks(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.9f);
        player.sendMessage(ChatColor.GRAY + "🪨 Rock Throw!");

        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                public void run() {
                    Snowball rock = player.launchProjectile(Snowball.class);
                    rock.setVelocity(player.getLocation().getDirection().multiply(1.5));
                    rock.setCustomName("Rock");
                    rock.setCustomNameVisible(false);
                    rock.setItem(new ItemStack(Material.COBBLESTONE));
                    player.getWorld().spawnParticle(
                            Particle.BLOCK,
                            rock.getLocation(),
                            15, 0.2, 0.2, 0.2, 0.1,
                            Bukkit.createBlockData(Material.COBBLESTONE)
                    );
                }
            }.runTaskLater(this, i * 10L);
        }
    }

    // -------------------- EVENT: Rock Hit Detection --------------------
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) return;
        if (!"Rock".equals(projectile.getCustomName())) return;

        projectile.getWorld().playSound(projectile.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 1f);
        projectile.getWorld().spawnParticle(
                Particle.BLOCK,
                projectile.getLocation(),
                20, 0.3, 0.3, 0.3, 0.2,
                Bukkit.createBlockData(Material.COBBLESTONE)
        );

        if (event.getHitEntity() instanceof LivingEntity target) {
            double damage = 3.0;
            Vector knockback = projectile.getVelocity().normalize().multiply(0.6);
            target.damage(damage, (Entity) projectile.getShooter());
            target.setVelocity(target.getVelocity().add(knockback));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 1f);
        }

        projectile.remove(); // Remove the snowball after impact
    }

    // -------------------- Cooldown Logic --------------------
    private boolean canUseAbility(Player player, int ability) {
        UUID id = player.getUniqueId();
        if (!cooldowns.containsKey(id)) return true;
        HashMap<Integer, Long> playerMap = cooldowns.get(id);
        if (!playerMap.containsKey(ability)) return true;

        long lastUse = playerMap.get(ability);
        long now = System.currentTimeMillis();
        long cooldownTime = getCooldownTime(ability);
        return now - lastUse >= cooldownTime;
    }

    private void setCooldown(Player player, int ability) {
        UUID id = player.getUniqueId();
        cooldowns.computeIfAbsent(id, k -> new HashMap<>()).put(ability, System.currentTimeMillis());
    }

    private long getTimeLeft(Player player, int ability) {
        UUID id = player.getUniqueId();
        if (!cooldowns.containsKey(id)) return 0;
        long lastUse = cooldowns.get(id).getOrDefault(ability, 0L);
        long now = System.currentTimeMillis();
        long cooldownTime = getCooldownTime(ability);
        long left = cooldownTime - (now - lastUse);
        return Math.max(left, 0);
    }

    private long getCooldownTime(int ability) {
        return switch (ability) {
            case 1 -> ABILITY1_COOLDOWN;
            case 2 -> ABILITY2_COOLDOWN;
            case 3 -> ABILITY3_COOLDOWN;
            case 4 -> ABILITY4_COOLDOWN;
            default -> 5000;
        };
    }
}
