package com.github.externaltime.quickcommands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AliasedArgumentType implements ArgumentType<Map.Entry<String, String>> {
    private final DynamicCommandExceptionType NO_SUCH_ALIAS = new DynamicCommandExceptionType(alias -> Text.translatable("quickcommands.alias.unknown", alias));
    private final Map<String, String> aliases;

    public AliasedArgumentType(Map<String, String> aliases) {
        this.aliases = aliases;
    }

    public static <S> String alias(CommandContext<S> ctx, String argumentId) {
        return (String) ctx.getArgument(argumentId, Map.Entry.class).getKey();
    }

    public static <S> String command(CommandContext<S> ctx, String argumentId) {
        return (String) ctx.getArgument(argumentId, Map.Entry.class).getValue();
    }

    @Override
    public Map.Entry<String, String> parse(StringReader reader) throws CommandSyntaxException {
        var alias = reader.getRemaining();
        var res = aliases.get(alias);
        if (res == null)
            throw NO_SUCH_ALIAS.create(alias);
        reader.setCursor(reader.getTotalLength());
        return new AbstractMap.SimpleImmutableEntry<>(alias, res);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(aliases.keySet(), builder);
    }
}
