package dev.coffeepng.conduit.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

/** Wraps an Invocation with a replaced argument array so /cs and /cg can delegate to ConduitCommand. */
public class ForwardedInvocation implements SimpleCommand.Invocation {

    private final SimpleCommand.Invocation original;
    private final String[] arguments;

    public ForwardedInvocation(SimpleCommand.Invocation original, String[] arguments) {
        this.original = original;
        this.arguments = arguments;
    }

    @Override
    public CommandSource source() {
        return original.source();
    }

    @Override
    public String alias() {
        return original.alias();
    }

    @Override
    public String[] arguments() {
        return arguments;
    }
}
