package com.github.externaltime.quickcommands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

public class CommandArgumentType implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return StringArgumentType.greedyString().parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        var reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        if (networkHandler == null || !reader.canRead() || reader.peek() != '/')
            return Suggestions.empty();
        reader.skip();
        var dispatcher = networkHandler.getCommandDispatcher();
        var parsed = dispatcher.parse(reader, networkHandler.getCommandSource());
        return dispatcher.getCompletionSuggestions(parsed);
    }
}
