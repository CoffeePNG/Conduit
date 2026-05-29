package dev.coffeepng.conduit.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * /ct send <player> <server>  — push a player to any connected server
 * /ct get  <player>           — pull a player to your current server
 */
public class ConduitCommand implements SimpleCommand {

    private static final String PERM_SEND = "conduit.send";
    private static final String PERM_GET  = "conduit.get";

    private final ProxyServer server;

    public ConduitCommand(ProxyServer server) {
        this.server = server;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "send" -> handleSend(source, args);
            case "get"  -> handleGet(source, args);
            default     -> sendUsage(source);
        }
    }

    // -------------------------------------------------------------------------
    // /ct send <player> <server>
    // -------------------------------------------------------------------------

    private void handleSend(CommandSource source, String[] args) {
        if (!source.hasPermission(PERM_SEND)) {
            source.sendMessage(noPermission());
            return;
        }

        if (args.length < 3) {
            source.sendMessage(text("Usage: /ct send <player> <server>", NamedTextColor.YELLOW));
            return;
        }

        String playerName = args[1];
        String serverName = args[2];

        Optional<Player> targetOpt = server.getPlayer(playerName);
        if (targetOpt.isEmpty()) {
            source.sendMessage(text("Player '" + playerName + "' is not online.", NamedTextColor.RED));
            return;
        }

        Optional<RegisteredServer> destOpt = server.getServer(serverName);
        if (destOpt.isEmpty()) {
            source.sendMessage(text("Server '" + serverName + "' does not exist.", NamedTextColor.RED));
            return;
        }

        Player target = targetOpt.get();
        RegisteredServer dest = destOpt.get();

        String currentServer = currentServerName(target);
        if (currentServer.equalsIgnoreCase(serverName)) {
            source.sendMessage(text(target.getUsername() + " is already on " + serverName + ".", NamedTextColor.YELLOW));
            return;
        }

        connect(source, target, dest,
            "Sent " + target.getUsername() + " to " + dest.getServerInfo().getName() + ".",
            "You were sent to " + dest.getServerInfo().getName() + " by a staff member."
        );
    }

    // -------------------------------------------------------------------------
    // /ct get <player>
    // -------------------------------------------------------------------------

    private void handleGet(CommandSource source, String[] args) {
        if (!source.hasPermission(PERM_GET)) {
            source.sendMessage(noPermission());
            return;
        }

        if (!(source instanceof Player staff)) {
            source.sendMessage(text("Only in-game staff can use /ct get.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(text("Usage: /ct get <player>", NamedTextColor.YELLOW));
            return;
        }

        String playerName = args[1];

        Optional<Player> targetOpt = server.getPlayer(playerName);
        if (targetOpt.isEmpty()) {
            source.sendMessage(text("Player '" + playerName + "' is not online.", NamedTextColor.RED));
            return;
        }

        Player target = targetOpt.get();

        Optional<RegisteredServer> destOpt = staff.getCurrentServer()
            .map(conn -> conn.getServer());
        if (destOpt.isEmpty()) {
            source.sendMessage(text("Could not determine your current server.", NamedTextColor.RED));
            return;
        }

        RegisteredServer dest = destOpt.get();
        String destName = dest.getServerInfo().getName();

        if (currentServerName(target).equalsIgnoreCase(destName)) {
            source.sendMessage(text(target.getUsername() + " is already on " + destName + ".", NamedTextColor.YELLOW));
            return;
        }

        connect(source, target, dest,
            "Retrieved " + target.getUsername() + " to " + destName + ".",
            "You were retrieved to " + destName + " by a staff member."
        );
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private void connect(CommandSource source, Player target, RegisteredServer dest,
                         String successMsg, String targetMsg) {
        target.createConnectionRequest(dest).connect().thenAccept(result -> {
            if (result.isSuccessful()) {
                source.sendMessage(text(successMsg, NamedTextColor.GREEN));
                target.sendMessage(text(targetMsg, NamedTextColor.YELLOW));
            } else {
                String reason = result.getReasonComponent()
                    .map(c -> " (" + PlainTextComponentSerializer.plainText().serialize(c) + ")")
                    .orElse("");
                source.sendMessage(text(
                    "Failed to connect " + target.getUsername() + " to " + dest.getServerInfo().getName() + "." + reason,
                    NamedTextColor.RED));
            }
        });
    }

    private String currentServerName(Player player) {
        return player.getCurrentServer()
            .map(conn -> conn.getServerInfo().getName())
            .orElse("");
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(text("Conduit — Staff Teleport Utility", NamedTextColor.GOLD));
        source.sendMessage(text("/ct send <player> <server>  — send a player to a server", NamedTextColor.YELLOW));
        source.sendMessage(text("/ct get  <player>           — pull a player to your server", NamedTextColor.YELLOW));
    }

    private Component text(String msg, NamedTextColor color) {
        return Component.text(msg, color);
    }

    private Component noPermission() {
        return text("You don't have permission to use this command.", NamedTextColor.RED);
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();

        // First arg: subcommand
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            return CompletableFuture.completedFuture(
                List.of("send", "get").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList()
            );
        }

        String sub = args[0].toLowerCase();

        // Second arg: player name
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return CompletableFuture.completedFuture(
                server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted()
                    .toList()
            );
        }

        // Third arg (send only): server name
        if (args.length == 3 && sub.equals("send")) {
            String partial = args[2].toLowerCase();
            return CompletableFuture.completedFuture(
                server.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(n -> n.toLowerCase().startsWith(partial))
                    .sorted()
                    .toList()
            );
        }

        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource src = invocation.source();
        return src.hasPermission(PERM_SEND) || src.hasPermission(PERM_GET);
    }
}
