package com.knoban.anvilrain;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AnvilRainManager implements Listener {

    private static final Random RANDOM = new Random();

    private final AnvilRain plugin;

    private boolean enabled;
    private int startRadius, currentRadius, targetRadius;
    private float startDensity, currentDensity, targetDensity;
    private long startTime, endTime;
    private Collection<UUID> targets;

    private Collection<FallingBlock> fallingAnvils;

    private BukkitTask task;

    public AnvilRainManager(final AnvilRain plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        enabled = false;
        startRadius = currentRadius = targetRadius = 0;
        startDensity = currentDensity = targetDensity = 0;

        startTime = endTime = System.currentTimeMillis();

        targets = new HashSet<>();
        fallingAnvils = new HashSet<>();
    }

    public void remove() {
        HandlerList.unregisterAll(this);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if(enabled) {
            if(task == null) {
                task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::anvilLoop, 1L, 1L);
            }
        } else if(task != null) {
            task.cancel();
            task = null;
        }
    }

    private void anvilLoop() {
        float prctTimeElapsed = isComplete() ? 1.0f : getPercentElapsedTime();
        currentRadius = startRadius + (int)((float)(targetRadius - startRadius) * prctTimeElapsed);
        currentDensity = startDensity + ((targetDensity - startDensity) * prctTimeElapsed);

        for(Player p : Bukkit.getOnlinePlayers()) {
            if(targets.contains(p.getUniqueId()) && p.getGameMode() == GameMode.SURVIVAL) {
                int bx = p.getLocation().getBlockX();
                int bz = p.getLocation().getBlockZ();
                for(int x=-currentRadius; x<currentRadius; x++) {
                    for(int z=-currentRadius; z<currentRadius; z++) {
                        if(RANDOM.nextFloat() < currentDensity) {
                            Location spawnLoc = new Location(p.getWorld(), bx + x, 255, bz + z);
                            Block block = p.getWorld().getBlockAt(spawnLoc);
                            block.setType(Material.AIR);

                            FallingBlock anvil = p.getWorld().spawnFallingBlock(spawnLoc, Material.ANVIL.createBlockData());
                            fallingAnvils.add(anvil);
                        }
                    }
                }
            }
        }
    }

    private static final EnumSet<Material> STRONG_BLOCKS = EnumSet.of(
            Material.OBSIDIAN, Material.BEDROCK
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent e) {
        Entity entity = e.getEntity();
        if(fallingAnvils.contains(entity)) {
            Block block = e.getBlock();
            Block beneath = block.getRelative(BlockFace.DOWN);
            if(!STRONG_BLOCKS.contains(beneath.getType())) {
                beneath.breakNaturally(new ItemStack(Material.AIR));
                e.setCancelled(true);
            } else
                fallingAnvils.remove(entity);
        }
    }

    public boolean isComplete() {
        return System.currentTimeMillis() >= endTime;
    }

    public float getPercentElapsedTime() {
        return (float)(System.currentTimeMillis() - startTime) / (float)(endTime - startTime);
    }

    public long getEndTime() {
        return endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setTimer(long millis) {
        if(millis <= 0)
            millis = 1;

        startTime = System.currentTimeMillis();
        endTime = startTime + millis;
    }

    public int getTargetRadius() {
        return targetRadius;
    }

    public void setTargetRadius(int targetRadius) {
        this.startRadius = currentRadius;
        this.targetRadius = targetRadius;
    }

    public float getTargetDensity() {
        return targetDensity;
    }

    public void setTargetDensity(float targetDensity) {
        this.startDensity = currentDensity;
        this.targetDensity = targetDensity;
    }

    public int getCurrentRadius() {
        return currentRadius;
    }

    public float getCurrentDensity() {
        return currentDensity;
    }

    public Collection<UUID> getTargets() {
        return targets;
    }
}
