package com.github.externaltime.quickcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.TreeMap;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class Aliases {
    private final TreeMap<String, String> aliasMap = new TreeMap<>();
    private final KeyBinding QUICK_COMMAND_KEY = new KeyBinding(
            "quickcommands.alias.key",
            InputUtil.Type.KEYSYM,
            InputUtil.GLFW_KEY_PERIOD,
            KeyBinding.MULTIPLAYER_CATEGORY
    );
    public final Codec<Void> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(map -> {
        this.aliasMap.clear();
        this.aliasMap.putAll(map);
        return null;
    }, unused -> this.aliasMap);

    public Aliases() {
        KeyBindingHelper.registerKeyBinding(QUICK_COMMAND_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (QUICK_COMMAND_KEY.wasPressed())
                client.setScreen(new ChatScreen("/quickcommands run "));
        });
    }

    private void sendMessage(String message) {
        message = message.trim();
        message = StringUtils.normalizeSpace(message);
        message = StringHelper.truncateChat(message);
        message = message.trim();
        var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (message.isEmpty() || networkHandler == null)
            return;
        if (message.startsWith("/"))
            networkHandler.sendCommand(message.substring(1));
        else
            networkHandler.sendChatMessage(message);
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> commands(Runnable save) {
        var aliased = new AliasedArgumentType(aliasMap);
        var add = literal("add").then(argument("alias", StringArgumentType.string()).then(argument("command", new CommandArgumentType()).executes(ctx -> {
            var alias = StringArgumentType.getString(ctx, "alias");
            var command = ctx.getArgument("command", String.class);
            aliasMap.put(alias, command);
            save.run();
            ctx.getSource().sendFeedback(Text.translatable("quickcommands.alias.added", alias, command));
            return 0;
        })));
        var run = literal("run").then(argument("alias", aliased).executes(ctx -> {
            var command = AliasedArgumentType.command(ctx, "alias");
            sendMessage(command);
            return 0;
        }));
        var remove = literal("remove").then(argument("alias", aliased).executes(ctx -> {
            var alias = AliasedArgumentType.alias(ctx, "alias");
            var command = AliasedArgumentType.command(ctx, "alias");
            save.run();
            ctx.getSource().sendFeedback(Text.translatable("quickcommands.alias.removed", alias, command));
            return 0;
        }));
        var list = literal("list").executes(ctx -> {
            if (aliasMap.isEmpty()) {
                ctx.getSource().sendFeedback(Text.translatable("quickcommands.alias.list.empty"));
                return 0;
            }
            ctx.getSource().sendFeedback(Text.translatable("quickcommands.alias.list.header", aliasMap.size()).styled(style -> style.withBold(true)));
            for (var entry : aliasMap.entrySet()) {
                var alias = entry.getKey();
                var command = entry.getValue();
                var text = Text.empty()
                        .append("[")
                        .append(Text.literal("X").styled(style -> style
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.list.remove", alias)))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands remove " + alias))))
                        .append("] ")
                        .append(Text.literal(entry.getKey()).styled(style -> style
                                .withColor(Formatting.WHITE)))
                        .append(" - ")
                        .append(Text.literal(command).styled(style -> style
                                .withItalic(true)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.list.run", command)))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))))
                        .styled(style -> style.withColor(Formatting.GRAY));
                ctx.getSource().sendFeedback(text);
            }
            return 0;
        });
        var clear = literal("clear").executes(ctx -> {
            var len = aliasMap.size();
            aliasMap.clear();
            save.run();
            ctx.getSource().sendFeedback(Text.translatable("quickcommands.alias.list.clear", len));
            return 0;
        });
        var help = literal("help").executes(ctx -> {
            var src = ctx.getSource();
            src.sendFeedback(Text.literal("Quick Commands").styled(style -> style.withBold(true)));
            src.sendFeedback(Text.literal("Available subcommands:"));
            src.sendFeedback(Text.literal("- ")
                    .append(Text.literal("add [alias] [command]").styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.help.hover", "add")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands add "))))
                    .append(" - ")
                    .append(Text.translatable("quickcommands.alias.help.add").styled(style -> style.withItalic(true)))
                    .styled(style -> style.withColor(Formatting.GRAY)));
            src.sendFeedback(Text.literal("- ")
                    .append(Text.literal("run [alias]").styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.help.hover", "run")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands run "))))
                    .append(" - ")
                    .append(Text.translatable("quickcommands.alias.help.run").styled(style -> style.withItalic(true)))
                    .styled(style -> style.withColor(Formatting.GRAY)));
            src.sendFeedback(Text.literal("- ")
                    .append(Text.literal("remove [alias]").styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.help.hover", "remove")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands remove "))))
                    .append(" - ")
                    .append(Text.translatable("quickcommands.alias.help.remove").styled(style -> style.withItalic(true)))
                    .styled(style -> style.withColor(Formatting.GRAY)));
            src.sendFeedback(Text.literal("- ")
                    .append(Text.literal("list").styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.help.hover", "list")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands list"))))
                    .append(" - ")
                    .append(Text.translatable("quickcommands.alias.help.list").styled(style -> style.withItalic(true)))
                    .styled(style -> style.withColor(Formatting.GRAY)));
            src.sendFeedback(Text.literal("- ")
                    .append(Text.literal("clear").styled(style -> style
                            .withColor(Formatting.WHITE)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("quickcommands.alias.help.hover", "clear")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/quickcommands clear"))))
                    .append(" - ")
                    .append(Text.translatable("quickcommands.alias.help.clear").styled(style -> style.withItalic(true)))
                    .styled(style -> style.withColor(Formatting.GRAY)));
            src.sendFeedback(Text.translatable("quickcommands.alias.quickkey"));
            return 0;
        });
        return literal("quickcommands")
                .then(add)
                .then(run)
                .then(remove)
                .then(list)
                .then(clear)
                .then(help);
    }
}
