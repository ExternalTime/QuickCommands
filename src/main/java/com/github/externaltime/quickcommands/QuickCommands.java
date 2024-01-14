package com.github.externaltime.quickcommands;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;


public class QuickCommands implements ClientModInitializer {
    private final Aliases aliases = new Aliases();

    private final Codec<Void> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            aliases.CODEC.fieldOf("aliases").forGetter(unused -> null)
    ).apply(instance, unused -> null));

    @Override
    public void onInitializeClient() {
        var persistence = new Persistence(CODEC);
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) ->
                dispatcher.register(aliases.commands(persistence::save))));
        persistence.load();
    }
}
