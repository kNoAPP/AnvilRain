package com.knoban.anvilrain;

import org.bukkit.plugin.java.JavaPlugin;

public class AnvilRain extends JavaPlugin {

    private static AnvilRain instance;

    @Override
    public void onEnable() {
        long tStart = System.currentTimeMillis();
        instance = this;
        super.onEnable();
        config = new DataHandler.YML(this, "/config.yml");

        long tEnd = System.currentTimeMillis();
        getLogger().info("Successfully Enabled! (" + (tEnd - tStart) + " ms)");
    }

    @Override
    public void onDisable() {
        long tStart = System.currentTimeMillis();
        super.onDisable();
        long tEnd = System.currentTimeMillis();
        getLogger().info("Successfully Disabled! (" + (tEnd - tStart) + " ms)");
    }

    public static AnvilRain getInstance() {
        return instance;
    }
}
