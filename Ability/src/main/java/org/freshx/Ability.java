package org.freshx;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Ability extends JavaPlugin {

    // Store cooldowns per player
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    private final long COOLDOWN_TIME = 5 * 1000; // 5 seconds (in milliseconds)

    @Override
    public void onEnable() {
        getLogger().info("Ability Plugin with particles and cooldown enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Ability Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("ability")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /ability <number>");
                return true;
            }

            if (args[0].equals("1")) {
                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();

                // Check cooldown
                if (cooldowns.containsKey(playerId)) {
                    long lastUse = cooldowns.get(playerId);
                    long timeLeft = (lastUse + COOLDOWN_TIME) - currentTime;
                    if (timeLeft > 0) {
                        player.sendMessage(ChatColor.RED + "⏳ You must wait " + (timeLeft / 1000.0) + " seconds!");
                        return true;
                    }
                }

                // Update cooldown
                cooldowns.put(playerId, currentTime);

                // Launch player upwards
                player.setVelocity(new Vector(0, 2.5, 0));

                // Sound and message
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);
                player.sendMessage(ChatColor.AQUA + "💫 Whoosh! You soar through the sky!");

                // Particle effects trail
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks > 20 || player.isOnGround()) {
                            cancel();
                            return;
                        }

                        player.getWorld().spawnParticle(
                                Particle.CLOUD,
                                player.getLocation(),
                                10, // amount
                                0.3, 0.2, 0.3, // offset
                                0.01 // speed
                        );

                        player.getWorld().spawnParticle(
                                Particle.END_ROD,
                                player.getLocation(),
                                4,
                                0.1, 0.1, 0.1,
                                0.02
                        );

                        ticks++;
                    }
                }.runTaskTimer(this, 0L, 2L); // runs every 2 ticks (~0.1 sec)

                return true;
            }

            player.sendMessage(ChatColor.RED + "Unknown ability: " + args[0]);
            return true;
        }

        return false;
    }
}

