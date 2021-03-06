package com.knoban.anvilrain;

import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.commandsII.annotations.AtlasParam;
import com.knoban.atlas.data.local.DataHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

// Recommended settings:
// /anvil toggle
// /anvil flip
// /anvil set 10 0.0001 1 0
// /anvil set 15 0.004 4 604800
public class AnvilRain extends JavaPlugin {

    private static AnvilRain instance;

    private DataHandler.YML config;
    private AnvilRainManager manager;

    @Override
    public void onEnable() {
        long tStart = System.currentTimeMillis();
        instance = this;
        super.onEnable();

        // Register configuration file.
        config = new DataHandler.YML(this, "/config.yml");

        // Register ACv2 commands
        ACAPI.getApi().registerCommandsFromClass(this, AnvilRain.class, this);

        // Register the AnvilRainManager that literally performs the game.
        manager = new AnvilRainManager(this);

        long tEnd = System.currentTimeMillis();
        getLogger().info("Successfully Enabled! (" + (tEnd - tStart) + " ms)");
    }

    @Override
    public void onDisable() {
        long tStart = System.currentTimeMillis();
        super.onDisable();

        manager.remove();
        manager = null;

        long tEnd = System.currentTimeMillis();
        getLogger().info("Successfully Disabled! (" + (tEnd - tStart) + " ms)");
    }

    @AtlasCommand(paths = "anvil")
    public void cmdAnvilBase(CommandSender sender) {
        sender.sendMessage("§2Anvil Rain §a- §fBy kNoAPP");
        sender.sendMessage("§7---------------------------");
        sender.sendMessage("   §e/anvil toggle §f- Toggle anvil rain");
        sender.sendMessage("   §e/anvil toggle <player> §f- Toggle a player");
        sender.sendMessage("   §e/anvil flip §f- Flip player toggle between whitelist/blacklist.");
        sender.sendMessage("   §e/anvil set <tradius> <tdensity> <tpower> <secs> §f- Progression.");
    }

    @AtlasCommand(paths = "anvil toggle", permission = "op")
    public void cmdAnvilToggle(CommandSender sender) {
        boolean enabled = !manager.isEnabled();
        manager.setEnabled(enabled);

        if(enabled) {
            sender.sendMessage("§aAnvil Rain is now enabled.");
        } else {
            sender.sendMessage("§cAnvil Rain is now disabled.");
        }
    }

    @AtlasCommand(paths = "anvil toggle", permission = "op")
    public void cmdAnvilToggle(CommandSender sender, Player target) {
        boolean enabled = !manager.getTargets().contains(target.getUniqueId());

        if(enabled) {
            manager.getTargets().add(target.getUniqueId());
            sender.sendMessage("§2" + target.getName() + " §ahas been added!");
        } else {
            manager.getTargets().remove(target.getUniqueId());
            sender.sendMessage("§4" + target.getName() + " §chas been removed!");
        }
    }

    @AtlasCommand(paths = "anvil flip", permission = "op")
    public void cmdAnvilFlipToggle(CommandSender sender) {
        boolean enabled = !manager.isFlippingTargets();
        manager.setFlipTargets(enabled);

        if(enabled) {
            sender.sendMessage("§aAnvil Rain is now using a blacklist. Add/remove players with:");
            sender.sendMessage("   §d/anvil toggle <player>");
        } else {
            sender.sendMessage("§aAnvil Rain is now using a whitelist. Add/remove players with:");
            sender.sendMessage("   §d/anvil toggle <player>");
        }
    }

    @AtlasCommand(paths = "anvil set", permission = "op")
    public void cmdAnvilToggle(CommandSender sender,
                               @AtlasParam(filter = "range:0to100", suggestions = {"# - radius"}) int tRadius,
                               @AtlasParam(filter = "range:0.0to1.0", suggestions = {"# - density"}) float tDensity,
                               @AtlasParam(filter = "range:0to255", suggestions = {"# - power"}) int tPower,
                               @AtlasParam(filter = "min:0", suggestions = {"# - seconds"})long secs) {
        manager.setTargetRadius(tRadius);
        manager.setTargetDensity(tDensity);
        manager.setTargetPower(tPower);
        manager.setTimer(secs*1000);

        sender.sendMessage("§aYour changes have been applied!");
    }
}
