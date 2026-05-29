package dev.coffeepng.conduit.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** /cs <player> <server> — shorthand for /ct send */
public class SendCommand implements SimpleCommand {

    private final ConduitCommand delegate;
    private final ProxyServer server;

    public SendCommand(ProxyServer server, ConduitCommand delegate) {
        this.server = server;
        this.delegate = delegate;
    }

    @Override
    public void execute(Invocation invocation) {
        // Prepend "send" and re-dispatch through the main command
        String[] original = invocation.arguments();
        String[] forwarded = new String[original.length + 1];
        forwarded[0] = "send";
        System.arraycopy(original, 0, forwarded, 1, original.length);
        delegate.execute(new ForwardedInvocation(invocation, forwarded));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] original = invocation.arguments();
        String[] forwarded = new String[original.length + 1];
        forwarded[0] = "send";
        System.arraycopy(original, 0, forwarded, 1, original.length);
        return delegate.suggestAsync(new ForwardedInvocation(invocation, forwarded));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("conduit.send");
    }
}
