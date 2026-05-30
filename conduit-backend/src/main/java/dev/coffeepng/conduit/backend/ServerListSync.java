package dev.coffeepng.conduit.backend;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps the backend's known server list in sync with the proxy.
 *
 * Outgoing: asks the proxy for its registered servers (subcommand 2) — on player
 *           join and on a periodic refresh, carried by any online player.
 * Incoming: receives the proxy's reply (tag 2 + comma-separated names) and caches it
 *           on the plugin for tab-completion.
 */
public class ServerListSync implements Listener, PluginMessageListener {

    private static final byte REQUEST_SERVERS = 2;

    private final ConduitBackendPlugin plugin;

    public ServerListSync(ConduitBackendPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        requestServerList(event.getPlayer());
    }

    /** Sends a server-list request to the proxy, carried by the given player. */
    public void requestServerList(Player carrier) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(REQUEST_SERVERS);
            carrier.sendPluginMessage(plugin, ConduitBackendPlugin.CHANNEL, buf.toByteArray());
        } catch (Exception e) {
            // Proxy unreachable — keep using the cached/config list
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(ConduitBackendPlugin.CHANNEL)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            byte tag = in.readByte();
            if (tag != REQUEST_SERVERS) return;

            String csv = in.readUTF();
            List<String> servers = csv.isEmpty()
                ? List.of()
                : Arrays.stream(csv.split(",")).filter(s -> !s.isEmpty()).toList();

            if (!servers.isEmpty()) {
                plugin.setProxyServers(servers);
            }
        } catch (Exception e) {
            // Malformed reply — ignore
        }
    }
}
