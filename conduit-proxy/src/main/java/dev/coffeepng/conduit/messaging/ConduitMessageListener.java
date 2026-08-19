package dev.coffeepng.conduit.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.coffeepng.conduit.ConduitPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;

/**
 * Receives plugin messages from backend servers on the conduit:cmd channel.
 *
 * Message format (DataOutputStream):
 *   byte   subcommand  (0 = send, 1 = get, 2 = server-list request, 3 = player-list request)
 *   UTF    senderUuid
 *   UTF    targetPlayerName
 *   UTF    destinationServer  (send: explicit server; get: sender's current server written by backend)
 */
public class ConduitMessageListener {

    private final ProxyServer server;

    public ConduitMessageListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(ConduitPlugin.CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            byte subcommand = in.readByte();

            // subcommand 2 = backend requesting the live server list. Reply down the
            // same connection and stop — no further fields are present in a request.
            if (subcommand == 2) {
                replyWithServerList((ServerConnection) event.getSource());
                return;
            }

            // subcommand 3 = backend requesting the network-wide online player list
            // for /ctg with no argument. Only field present is the requester's UUID,
            // used to exclude them from their own list.
            if (subcommand == 3) {
                replyWithPlayerList((ServerConnection) event.getSource(), in.readUTF());
                return;
            }

            String senderUuid      = in.readUTF();
            String targetName      = in.readUTF();
            String destinationName = in.readUTF();

            Optional<Player> senderOpt = server.getPlayer(java.util.UUID.fromString(senderUuid));
            Optional<Player> targetOpt = server.getPlayer(targetName);

            // For "get" (subcommand 1) the backend sends an empty destination —
            // resolve it here from the sender's current server on the proxy.
            if (subcommand == 1 && destinationName.isEmpty()) {
                if (senderOpt.isPresent()) {
                    destinationName = senderOpt.get().getCurrentServer()
                        .map(c -> c.getServerInfo().getName())
                        .orElse("");
                }
            }

            Optional<RegisteredServer> destOpt = server.getServer(destinationName);

            if (senderOpt.isEmpty()) return;
            Player sender = senderOpt.get();

            if (targetOpt.isEmpty()) {
                sender.sendMessage(Component.text("Player '" + targetName + "' is not online.", NamedTextColor.RED));
                return;
            }
            if (destOpt.isEmpty()) {
                sender.sendMessage(Component.text("Server '" + destinationName + "' does not exist.", NamedTextColor.RED));
                return;
            }

            Player target = targetOpt.get();
            RegisteredServer dest = destOpt.get();
            String destServerName = dest.getServerInfo().getName();
            String sourceServerName = target.getCurrentServer()
                .map(c -> c.getServerInfo().getName())
                .orElse("");

            String action = subcommand == 0 ? "Sent" : "Retrieved";
            String fromInfo = sourceServerName.isEmpty() ? "" : " from " + sourceServerName;
            String targetMsg = subcommand == 0
                ? "You were sent to " + destServerName + " by a staff member."
                : "You were retrieved to " + destServerName + " by a staff member.";

            target.createConnectionRequest(dest).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    sender.sendMessage(Component.text(action + " " + target.getUsername() + fromInfo + " to " + destServerName + ".", NamedTextColor.GREEN));
                    target.sendMessage(Component.text(targetMsg, NamedTextColor.YELLOW));
                } else {
                    String reason = result.getReasonComponent()
                        .map(c -> " (" + PlainTextComponentSerializer.plainText().serialize(c) + ")")
                        .orElse("");
                    sender.sendMessage(Component.text(
                        "Failed to move " + target.getUsername() + " to " + destServerName + "." + reason,
                        NamedTextColor.RED));
                }
            });

        } catch (Exception e) {
            // Malformed message — ignore
        }
    }

    /** Sends the proxy's full registered-server list back to the requesting backend. */
    private void replyWithServerList(ServerConnection connection) {
        String csv = server.getAllServers().stream()
            .map(s -> s.getServerInfo().getName())
            .collect(java.util.stream.Collectors.joining(","));

        try {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(buf);
            out.writeByte(2); // response tag, mirrors the request
            out.writeUTF(csv);
            connection.sendPluginMessage(ConduitPlugin.CHANNEL, buf.toByteArray());
        } catch (Exception e) {
            // Failed to reply — backend will fall back to its config list
        }
    }

    /**
     * Sends every online player back to the requesting backend, minus the requester
     * themself and anyone already on the requester's current server — pulling either
     * would be a no-op.
     */
    private void replyWithPlayerList(ServerConnection connection, String requesterUuid) {
        Optional<Player> requesterLookup;
        try {
            requesterLookup = server.getPlayer(java.util.UUID.fromString(requesterUuid));
        } catch (IllegalArgumentException e) {
            requesterLookup = Optional.empty();
        }
        final Optional<Player> requester = requesterLookup;

        String requesterServer = requester.flatMap(Player::getCurrentServer)
            .map(c -> c.getServerInfo().getName())
            .orElse(null);

        String csv = server.getAllPlayers().stream()
            .filter(p -> requester.isEmpty() || !p.getUniqueId().equals(requester.get().getUniqueId()))
            .map(p -> new String[] { p.getUsername(), p.getCurrentServer()
                .map(c -> c.getServerInfo().getName())
                .orElse("?") })
            .filter(pair -> requesterServer == null || !pair[1].equals(requesterServer))
            .map(pair -> pair[0] + ":" + pair[1])
            .collect(java.util.stream.Collectors.joining(","));

        try {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(buf);
            out.writeByte(3); // response tag, mirrors the request
            out.writeUTF(csv);
            connection.sendPluginMessage(ConduitPlugin.CHANNEL, buf.toByteArray());
        } catch (Exception e) {
            // Failed to reply — backend will just show nothing
        }
    }
}
