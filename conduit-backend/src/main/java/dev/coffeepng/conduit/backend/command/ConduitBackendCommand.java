package dev.coffeepng.conduit.backend.command;

import dev.coffeepng.conduit.backend.ConduitBackendPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /ct, /cts, /ctg on the backend server.
 * Serialises the request and forwards it to the proxy over conduit:cmd.
 *
 * /ct send <player> <server>  →  subcommand 0
 * /ct get  <player>           →  subcommand 1
 * /cts     <player> <server>  →  subcommand 0 (shorthand)
 * /ctg     <player>           →  subcommand 1 (shorthand)
 */
public class ConduitBackendCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_SEND = "conduit.send";
    private static final String PERM_GET  = "conduit.get";

    private final ConduitBackendPlugin plugin;

    public ConduitBackendCommand(ConduitBackendPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly in-game players can use this command.");
            return true;
        }

        String cmd = label.toLowerCase();

        switch (cmd) {
            case "cts" -> handleSend(player, args);
            case "ctg" -> handleGet(player, args);
            default -> { // /ct
                if (args.length == 0) { sendUsage(player); return true; }
                switch (args[0].toLowerCase()) {
                    case "send" -> handleSend(player, shift(args));
                    case "get"  -> handleGet(player, shift(args));
                    default     -> sendUsage(player);
                }
            }
        }
        return true;
    }

    private void handleSend(Player sender, String[] args) {
        if (!sender.hasPermission(PERM_SEND)) { noPermission(sender); return; }
        if (args.length < 2) { sender.sendMessage("§eUsage: /ct send <player> <server>"); return; }
        forwardToProxy(sender, (byte) 0, args[0], args[1]);
    }

    private void handleGet(Player sender, String[] args) {
        if (!sender.hasPermission(PERM_GET)) { noPermission(sender); return; }
        if (args.length < 1) {
            plugin.getPlayerListSync().requestPlayerList(sender);
            return;
        }

        // The proxy knows the player's current server already via their connection,
        // so we pass the sender's UUID and let the proxy resolve it there.
        // We send an empty destination; the proxy uses the sender's current server for "get".
        forwardToProxy(sender, (byte) 1, args[0], "");
    }

    private void forwardToProxy(Player sender, byte subcommand, String targetName, String destination) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(subcommand);
            out.writeUTF(sender.getUniqueId().toString());
            out.writeUTF(targetName);
            out.writeUTF(destination);
            sender.sendPluginMessage(plugin, ConduitBackendPlugin.CHANNEL, buf.toByteArray());
        } catch (Exception e) {
            sender.sendMessage("§cConduit: failed to contact proxy. Is the proxy plugin installed?");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String cmd = label.toLowerCase();

        // /cts <player> <server>
        if (cmd.equals("cts")) {
            if (args.length == 1) return matchPlayers(args[0]);
            if (args.length == 2) return matchServers(args[1]);
            return List.of();
        }

        // /ctg <player>
        if (cmd.equals("ctg")) {
            if (args.length == 1) return matchPlayers(args[0]);
            return List.of();
        }

        // /ct <subcommand> ...
        if (args.length == 1) return List.of("send", "get").stream()
            .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        if (args.length == 2) return matchPlayers(args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("send")) return matchServers(args[2]);
        return List.of();
    }

    private List<String> matchPlayers(String partial) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }

    private List<String> matchServers(String partial) {
        return plugin.getProxyServers().stream()
            .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }

    private String[] shift(String[] args) {
        if (args.length <= 1) return new String[0];
        String[] shifted = new String[args.length - 1];
        System.arraycopy(args, 1, shifted, 0, shifted.length);
        return shifted;
    }

    private void sendUsage(Player p) {
        p.sendMessage("§6Conduit §7—§r Staff Teleport");
        p.sendMessage("§e/ct send <player> <server>");
        p.sendMessage("§e/ct get <player>");
    }

    private void noPermission(Player p) {
        p.sendMessage("§cYou don't have permission to use this command.");
    }
}
