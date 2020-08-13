package com.knoban.anvilrain;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.atlas.utils.Tools;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AnvilRainManager implements Listener {

    private static final Random RANDOM = new Random();

    private final AnvilRain plugin;

    private boolean enabled, flipTargets;
    private int startRadius, currentRadius, targetRadius, startPower, currentPower, targetPower;
    private float startDensity, currentDensity, targetDensity;
    private long startTime, endTime;
    private Collection<UUID> targets;

    private BukkitTask task;

    public AnvilRainManager(@NotNull final AnvilRain plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        enabled = false;
        flipTargets = false;
        startRadius = currentRadius = targetRadius = 0;
        startDensity = currentDensity = targetDensity = 0;
        startPower = currentPower = targetPower = 0;

        startTime = endTime = System.currentTimeMillis();

        targets = new HashSet<>();
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

    public boolean isFlippingTargets() {
        return flipTargets;
    }

    public void setFlipTargets(boolean flipTargets) {
        this.flipTargets = flipTargets;
    }

    private static final String METADATA = "falling-anvil";
    private void anvilLoop() {
        float prctTimeElapsed = isComplete() ? 1.0f : getPercentElapsedTime();
        currentRadius = startRadius + (int)((float)(targetRadius - startRadius) * prctTimeElapsed);
        currentDensity = startDensity + ((targetDensity - startDensity) * prctTimeElapsed);
        currentPower = startPower + (int)((float)(targetPower - startPower) * prctTimeElapsed);

        for(Player p : Bukkit.getOnlinePlayers()) {
            if(targets.contains(p.getUniqueId()) == !flipTargets && p.getGameMode() != GameMode.SPECTATOR) {
                p.sendActionBar("§4Difficulty §f- [ " + Tools.generateWaitBar(prctTimeElapsed, 50,
                        ChatColor.YELLOW, '|',
                        ChatColor.GRAY, '|') + " §f] - §6" + String.format("%.3f", prctTimeElapsed * 100) + "%");
                int bx = p.getLocation().getBlockX();
                int by = p.getLocation().getBlockY() + 40;
                int bz = p.getLocation().getBlockZ();
                for(int x=-currentRadius; x<currentRadius; x++) {
                    for(int z=-currentRadius; z<currentRadius; z++) {
                        if(RANDOM.nextFloat() < currentDensity) {
                            Location spawnLoc = new Location(p.getWorld(),
                                    bx + x + 0.5,
                                    Math.max(p.getWorld().getHighestBlockYAt(bx + x, bz + z) + 2, by),
                                    bz + z + 0.5);
                            Block block = p.getWorld().getBlockAt(spawnLoc);
                            block.setType(Material.AIR);

                            FallingBlock anvil = p.getWorld().spawnFallingBlock(spawnLoc, Material.ANVIL.createBlockData());
                            anvil.setMetadata(METADATA, new FixedMetadataValue(plugin, true));
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilLand(EntityChangeBlockEvent e) {
        Entity entity = e.getEntity();
        if(entity.hasMetadata(METADATA)) {
            Block block = e.getBlock();
            blockBreakLoop(block, 0);

            for(Entity nearby : entity.getNearbyEntities(0.2, 0.2, 0.2)) {
                if(nearby instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) nearby;
                    le.damage(10, entity);
                    continue;
                }
                if(nearby.hasMetadata(METADATA))
                    continue;
                nearby.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnvilLand(EntityRemoveFromWorldEvent e) {
        Entity entity = e.getEntity();
        if(entity.hasMetadata(METADATA)) {
            Block block = entity.getLocation().getBlock();
            blockBreakLoop(block, 0);

            for(Entity nearby : entity.getNearbyEntities(0.2, 0.2, 0.2)) {
                if(nearby instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) nearby;
                    le.damage(10, entity);
                    continue;
                }
                if(nearby.hasMetadata(METADATA))
                    continue;
                nearby.remove();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if(!p.hasPlayedBefore()) {
            int x = RANDOM.nextInt(2000) - 1000;
            int z = RANDOM.nextInt(2000) - 1000;
            int y = p.getWorld().getHighestBlockYAt(x, z);

            p.sendMessage("§7You have been teleported to a random location.");
            p.teleport(new Location(p.getWorld(), x, y + 2, z));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int x = RANDOM.nextInt(2000) - 1000;
            int z = RANDOM.nextInt(2000) - 1000;
            int y = p.getWorld().getHighestBlockYAt(x, z);

            p.sendMessage("§7You have been teleported to a random location.");
            p.teleport(new Location(p.getWorld(), x, y + 2, z));
        }, 2L);
    }

    private static final EnumSet<Material> STRONG_BLOCKS = EnumSet.of(
            Material.OBSIDIAN, Material.BEDROCK, Material.END_PORTAL_FRAME
    );
    private void blockBreakLoop(@NotNull Block block, int i) {
        if(STRONG_BLOCKS.contains(block.getType()) || block.getY() <= 0) {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3F, 1F);
            return;
        }

        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
        block.setType(Material.AIR);

        if(++i < currentPower) {
            final int nextI = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> blockBreakLoop(block.getRelative(BlockFace.DOWN), nextI), 1L);
        }
    }

    public boolean isComplete() {
        return System.currentTimeMillis() >= endTime;
    }

    public float getPercentElapsedTime() {
        return (float)(System.currentTimeMillis() - startTime) / (float)(endTime - startTime);
    }

    public void setTimer(long millis) {
        if(millis <= 0)
            millis = 1;

        startTime = System.currentTimeMillis();
        endTime = startTime + millis;
    }

    public void setTargetRadius(int targetRadius) {
        this.startRadius = currentRadius;
        this.targetRadius = targetRadius;
    }

    public void setTargetDensity(float targetDensity) {
        this.startDensity = currentDensity;
        this.targetDensity = targetDensity;
    }

    public void setTargetPower(int targetPower) {
        this.startPower = currentPower;
        this.targetPower = targetPower;
    }

    public Collection<UUID> getTargets() {
        return targets;
    }
}
