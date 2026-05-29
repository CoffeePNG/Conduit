package dev.coffeepng.conduit.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** /cg <player> — shorthand for /ct get */
public class GetCommand implements SimpleCommand {

    private final ConduitCommand delegate;
    private final ProxyServer server;

    public GetCommand(ProxyServer server, ConduitCommand delegate) {
        this.server = server;
        this.delegate = delegate;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] original = invocation.arguments();
        String[] forwarded = new String[original.length + 1];
        forwarded[0] = "get";
        System.arraycopy(original, 0, forwarded, 1, original.length);
        delegate.execute(new ForwardedInvocation(invocation, forwarded));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] original = invocation.arguments();
        String[] forwarded = new String[original.length + 1];
        forwarded[0] = "get";
        System.arraycopy(original, 0, forwarded, 1, original.length);
        return delegate.suggestAsync(new ForwardedInvocation(invocation, forwarded));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("conduit.get");
    }
}
