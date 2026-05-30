package dev.coffeepng.conduit;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.coffeepng.conduit.messaging.ConduitMessageListener;
import org.slf4j.Logger;

@Plugin(
    id = "conduit",
    name = "Conduit",
    version = "1.0.0",
    description = "Staff utility to send and retrieve players across proxied servers",
    authors = {"CoffeePNG"}
)
public class ConduitPlugin {

    public static final MinecraftChannelIdentifier CHANNEL =
        MinecraftChannelIdentifier.from("conduit:cmd");

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public ConduitPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Commands live on the backend servers only — proxy just handles the teleport logic
        server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(this, new ConduitMessageListener(server));

        logger.info("Conduit loaded — listening for backend commands.");
    }
}
