package com.abdullaharafat.AfkPool;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
// import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.command.CommandSender;
import org.bukkit.metadata.MetadataValue;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class App extends JavaPlugin {
    public WorldGuardPlugin worldGuard;
    public String regionName;
    public long crateInterval;
    public long moneyInterval;

    public void onEnable() {
        this.getLogger().info("AfkPool Version 1.0.6 enabled.");
        this.regionName = this.getConfig().getString("region-name", "afk");
        this.crateInterval = this.getConfig().getLong("crate-interval", 24000L);
        this.moneyInterval = this.getConfig().getLong("money-interval", 600L);
        this.saveDefaultConfig();
        final Essentials ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (ess != null && Bukkit.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            this.worldGuard = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");

            Object task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    checkAfkPlayers();
                }
            }, 0L, 20L);
            this.getLogger().info("Task is running");
            if (task != null) {
                ((BukkitTask) task).cancel();
                this.getLogger().info("Task killed");
            }

        } else {
            this.getLogger().severe("EssentialsX and/or WorldGuard not found or not enabled. Disabling AfkPool.");
            Bukkit.getPluginManager().disablePlugin((Plugin) this);
        }
    }

    public void onDisable() {
        this.getLogger().info("AfkPool Disabled");
    }

    void checkAfkPlayers() {
        final Essentials ess = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (ess.getUser(player).isAfk()) {
                player.sendTitle(ChatColor.WHITE + "AfkPool will be handling your " + ChatColor.BLUE + "AFK Status",
                        (String) null, 10, 70, 20);
                final Location location = player.getLocation();
                final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                final World world = (World) player.getWorld();
                final RegionManager regions = container.get((World) BukkitAdapter.adapt(world));
                this.getLogger().info("Got players world, world is " + player.getWorld());
                if (regions != null) {
                    final ProtectedRegion region = regions.getRegion(this.regionName);

                    if (regions == null || region == null) {
                        this.getLogger().warning("No regions found or region is null");
                        continue;
                    }

                    if (region != null
                            && region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                        if (!region.getMembers().contains(player.getName())) {
                            continue;
                        }
                        final long currentTime = System.currentTimeMillis() / 50L;
                        final long crateTime = player.getMetadata("crate-time").isEmpty() ? 0L
                                : player.getMetadata("crate-time").get(0).asLong();
                        final long moneyTime = player.getMetadata("money-time").isEmpty() ? 0L
                                : player.getMetadata("money-time").get(0).asLong();
                        if (currentTime - crateTime >= this.crateInterval) {
                            Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(),
                                    "crate give " + player.getName() + " afk");
                            player.sendMessage("You have received an AFK crate key!");
                            player.setMetadata("crate-time",
                                    (MetadataValue) new FixedMetadataValue((Plugin) this, (Object) currentTime));
                        }

                        if (regions == null || region == null) {
                            this.getLogger().warning("No regions found or region is null");
                        }

                        if (currentTime - moneyTime >= this.moneyInterval) {
                            final int money = ThreadLocalRandom.current().nextInt(30, 101);
                            Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(),
                                    "eco give " + player.getName() + " " + money);
                            player.sendMessage("You have received " + money + " money!");
                            player.setMetadata("money-time",
                                    (MetadataValue) new FixedMetadataValue((Plugin) this, (Object) currentTime));
                        }
                        if (!this.regionName.equals("afk") || player.hasMetadata("afk-zone-entered")) {
                            continue;
                        }
                        player.sendTitle(ChatColor.WHITE + "You have entered the " + ChatColor.BLUE + "AFK zone",
                                (String) null, 10, 70, 20);
                        player.setMetadata("afk-zone-entered",
                                (MetadataValue) new FixedMetadataValue((Plugin) this, (Object) true));
                    } else {
                        this.getLogger().severe("There is no region");
                        this.onEnable();
                    }
                } else {
                    this.getLogger().severe("There is not any regions");
                    this.onEnable();
                }
            }
        }
    }
}