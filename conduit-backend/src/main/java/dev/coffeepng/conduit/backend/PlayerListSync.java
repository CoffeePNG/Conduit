package dev.coffeepng.conduit.backend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Powers /ctg and /ct get with no argument: asks the proxy for the
 * network-wide online player list and renders it as a clickable chat menu.
 *
 * Outgoing: subcommand 3 + requester UUID, carried by the requesting player.
 * Incoming: subcommand 3 + CSV of "name:server" pairs, minus the requester,
 *           delivered back down that same player's connection.
 */
public class PlayerListSync implements PluginMessageListener {

    private static final byte REQUEST_PLAYERS = 3;

    private final ConduitBackendPlugin plugin;

    public PlayerListSync(ConduitBackendPlugin plugin) {
        this.plugin = plugin;
    }

    /** Asks the proxy for the current online player list, to be shown to the requester. */
    public void requestPlayerList(Player requester) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(REQUEST_PLAYERS);
            out.writeUTF(requester.getUniqueId().toString());
            requester.sendPluginMessage(plugin, ConduitBackendPlugin.CHANNEL, buf.toByteArray());
        } catch (Exception e) {
            requester.sendMessage("§cConduit: failed to contact proxy. Is the proxy plugin installed?");
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(ConduitBackendPlugin.CHANNEL)) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            byte tag = in.readByte();
            if (tag != REQUEST_PLAYERS) return;

            sendPlayerList(player, in.readUTF());
        } catch (Exception e) {
            // Malformed reply — ignore
        }
    }

    private void sendPlayerList(Player viewer, String csv) {
        TreeMap<String, List<String>> byServer = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int total = 0;
        for (String pair : csv.split(",")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;
            byServer.computeIfAbsent(parts[1], s -> new ArrayList<>()).add(parts[0]);
            total++;
        }

        if (total == 0) {
            viewer.sendMessage(Component.text("No other players online.", NamedTextColor.GRAY));
            return;
        }

        viewer.sendMessage(Component.text()
            .append(Component.text("Conduit ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("— click a name to pull them (" + total + " online)", NamedTextColor.GRAY))
            .build());

        byServer.forEach((server, names) -> {
            names.sort(String.CASE_INSENSITIVE_ORDER);

            var line = Component.text()
                .append(Component.text(" " + server + " ", NamedTextColor.DARK_AQUA, TextDecoration.BOLD));

            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                line.append(Component.text(name, NamedTextColor.WHITE)
                    .clickEvent(ClickEvent.runCommand("/ctg " + name))
                    .hoverEvent(HoverEvent.showText(
                        Component.text("Click to pull " + name + " from " + server, NamedTextColor.YELLOW))));
                if (i < names.size() - 1) {
                    line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
                }
            }

            viewer.sendMessage(line.build());
        });
    }
}
