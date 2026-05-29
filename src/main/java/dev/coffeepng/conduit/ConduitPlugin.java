package dev.coffeepng.conduit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.coffeepng.conduit.command.ConduitCommand;
import dev.coffeepng.conduit.command.GetCommand;
import dev.coffeepng.conduit.command.SendCommand;
import org.slf4j.Logger;

@Plugin(
    id = "conduit",
    name = "Conduit",
    version = "1.0.0",
    description = "Staff utility to send and retrieve players across proxied servers",
    authors = {"CoffeePNG"}
)
public class ConduitPlugin {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public ConduitPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        ConduitCommand conduit = new ConduitCommand(server);

        server.getCommandManager().register(
            server.getCommandManager()
                .metaBuilder("ct")
                .aliases("conduit")
                .plugin(this)
                .build(),
            conduit
        );

        server.getCommandManager().register(
            server.getCommandManager()
                .metaBuilder("cts")
                .plugin(this)
                .build(),
            new SendCommand(server, conduit)
        );

        server.getCommandManager().register(
            server.getCommandManager()
                .metaBuilder("ctg")
                .plugin(this)
                .build(),
            new GetCommand(server, conduit)
        );

        logger.info("Conduit loaded — /ct, /cts, /ctg ready.");
    }
}
