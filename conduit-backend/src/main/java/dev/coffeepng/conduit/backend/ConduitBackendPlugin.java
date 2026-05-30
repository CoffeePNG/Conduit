package dev.coffeepng.conduit.backend;

import dev.coffeepng.conduit.backend.command.ConduitBackendCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConduitBackendPlugin extends JavaPlugin {

    public static final String CHANNEL = "conduit:cmd";

    // How often (ticks) to re-sync the server list from the proxy. 6000 ticks = 5 minutes.
    private static final long REFRESH_TICKS = 6000L;

    private volatile List<String> proxyServers = List.of();

    @Override
    public void onEnable() {
        // Server list is sourced entirely from the proxy — requested on join and refreshed periodically.
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        ServerListSync sync = new ServerListSync(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, sync);
        getServer().getPluginManager().registerEvents(sync, this);

        // Periodically re-request the list so it tracks proxy changes without a restart.
        getServer().getScheduler().runTaskTimer(this, () -> {
            Player carrier = getServer().getOnlinePlayers().stream().findFirst().orElse(null);
            if (carrier != null) sync.requestServerList(carrier);
        }, REFRESH_TICKS, REFRESH_TICKS);

        ConduitBackendCommand cmd = new ConduitBackendCommand(this);
        getCommand("ct").setExecutor(cmd);
        getCommand("ct").setTabCompleter(cmd);
        getCommand("cts").setExecutor(cmd);
        getCommand("cts").setTabCompleter(cmd);
        getCommand("ctg").setExecutor(cmd);
        getCommand("ctg").setTabCompleter(cmd);

        getLogger().info("Conduit backend loaded. Awaiting server list from proxy.");
    }

    public List<String> getProxyServers() {
        return proxyServers;
    }

    /** Updates the cached server list with the live list reported by the proxy. */
    public void setProxyServers(List<String> servers) {
        this.proxyServers = List.copyOf(servers);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }
}
