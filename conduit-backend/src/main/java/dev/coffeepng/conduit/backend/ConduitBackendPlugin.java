package dev.coffeepng.conduit.backend;

import dev.coffeepng.conduit.backend.command.ConduitBackendCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

public class ConduitBackendPlugin extends JavaPlugin {

    public static final String CHANNEL = "conduit:cmd";

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        ConduitBackendCommand cmd = new ConduitBackendCommand(this);
        getCommand("ct").setExecutor(cmd);
        getCommand("ct").setTabCompleter(cmd);
        getCommand("cts").setExecutor(cmd);
        getCommand("cts").setTabCompleter(cmd);
        getCommand("ctg").setExecutor(cmd);
        getCommand("ctg").setTabCompleter(cmd);

        getLogger().info("Conduit backend loaded.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
