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
import java.util.Comparator;
import java.util.List;

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

    // Cycled through deterministically per server name so the same server always
    // shows the same hover color, without needing to track assignments anywhere.
    private static final NamedTextColor[] SERVER_COLORS = {
        NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.YELLOW,
        NamedTextColor.LIGHT_PURPLE, NamedTextColor.GOLD, NamedTextColor.BLUE,
        NamedTextColor.DARK_AQUA, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE
    };

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
        List<PlayerEntry> players = new ArrayList<>();
        for (String pair : csv.split(",")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) continue;
            players.add(new PlayerEntry(parts[0], parts[1]));
        }

        if (players.isEmpty()) {
            viewer.sendMessage(Component.text("No other players online.", NamedTextColor.GRAY));
            return;
        }

        players.sort(Comparator.comparing(PlayerEntry::name, String.CASE_INSENSITIVE_ORDER));

        viewer.sendMessage(Component.text()
            .append(Component.text("Conduit ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("(Hover to see server) ", NamedTextColor.GRAY))
            .append(Component.text("— " + players.size() + " online", NamedTextColor.DARK_GRAY))
            .build());

        var line = Component.text().append(Component.text(" "));
        for (int i = 0; i < players.size(); i++) {
            PlayerEntry p = players.get(i);
            NamedTextColor serverColor = colorFor(p.server());
            line.append(Component.text(p.name(), NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/ctg " + p.name()))
                .hoverEvent(HoverEvent.showText(Component.text(p.server(), serverColor, TextDecoration.BOLD))));
            if (i < players.size() - 1) {
                line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            }
        }

        viewer.sendMessage(line.build());
    }

    private static NamedTextColor colorFor(String server) {
        int index = Math.floorMod(server.hashCode(), SERVER_COLORS.length);
        return SERVER_COLORS[index];
    }

    private record PlayerEntry(String name, String server) {}
}
