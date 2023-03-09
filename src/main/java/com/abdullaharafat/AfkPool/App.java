package com.abdullaharafat.AfkPool;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
// import com.sk89q.worldguard.bukkit.BukkitRegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import net.md_5.bungee.api.ChatColor;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.sk89q.worldguard.WorldGuard;


public class App extends JavaPlugin {

    private WorldGuardPlugin worldGuard; // A reference to WorldGuard plugin
    private String regionName; // The name of the AFK zone region
    private long crateInterval; // The interval for giving crate keys (in ticks)
    private long moneyInterval; // The interval for giving money (in ticks)

    @Override
    public void onEnable() {
        getLogger().info("AfkPool Version 1.0.4 enabled.");
        // Load the config values from config.yml or use default values
        regionName = getConfig().getString("region-name", "afk-zone");
        crateInterval = getConfig().getLong("crate-interval", 24000); // 20 minutes
        moneyInterval = getConfig().getLong("money-interval", 600); // 30 seconds

        // Save the config if it does not exist
        saveDefaultConfig();

        // Start a repeating task that checks for AFK players every second
        Bukkit.getScheduler().runTaskTimer(this, this::checkAfkPlayers, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkPool Disabled");
    }

    private void checkAfkPlayers() {
    Essentials ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("EssentialsX");
    // Get an instance of WorldGuard plugin
    worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
    if (ess != null)
    if(worldGuard != null)
        // Get all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if the player is AFK using EssentialsX method
            if(ess.getUser(player).isAfk()) {
                // Get the player's location
                Location location = player.getLocation();
                // Get the region container from WorldGuard
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                // Get the region manager for the player's world
                RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
                // Get the region by name
                ProtectedRegion region = regions.getRegion(regionName);
                // Check if the region exists and contains the player's location
                if (region != null && region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                    // Check if the player is a member of the region
                    if (region.getMembers().contains(player.getName())) {
                        // Give rewards to the player based on intervals
                        long currentTime = System.currentTimeMillis() / 50;
                        long crateTime = player.getMetadata("crate-time").isEmpty() ? 0L : player.getMetadata("crate-time").get(0).asLong();
                        long moneyTime = player.getMetadata("money-time").isEmpty() ? 0L : player.getMetadata("money-time").get(0).asLong();
        
                        // Check if the crate interval has passed since the last reward
                        if (currentTime - crateTime >= crateInterval) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate give " + player.getName() + " afk");
                            player.sendMessage("You have received an AFK crate key!");
                            player.setMetadata("crate-time", new FixedMetadataValue(this, currentTime));
                        }
        
                        // Check if the money interval has passed since the last reward
                        if (currentTime - moneyTime >= moneyInterval) {
                            int money = ThreadLocalRandom.current().nextInt(30, 101); // Generate a random amount between 30 and 100 (inclusive)
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + money);
                            player.sendMessage("You have received " + money + " money!");
                            player.setMetadata("money-time", new FixedMetadataValue(this, currentTime));
                        }
        
                        // Show a title to the player when they enter the 'afk' region
                        if (regionName.equals("afk") && !player.hasMetadata("afk-zone-entered")) {
                            player.sendTitle(ChatColor.WHITE + "You have entered the " + ChatColor.BLUE + "AFK zone", null, 10, 70, 20);
                            player.setMetadata("afk-zone-entered", new FixedMetadataValue(this, true));
                        }
                    }
                }
            }
        }
    }
 }