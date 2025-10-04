package com.adminium.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.util.Formatting;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import java.util.Arrays;

public class AnnounceCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Simple announce command: /announce "title" "description"
        dispatcher.register(Commands.literal("announce")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("title", StringArgumentType.string())
                .then(Commands.argument("description", StringArgumentType.string())
                    .executes(context -> {
                        String title = StringArgumentType.getString(context, "title");
                        String description = StringArgumentType.getString(context, "description");
                        
                        sendAnnouncement(context.getSource(), title, description, Formatting.WHITE, Formatting.WHITE);
                        return 1;
                    })))
            
            // Colored announce command: /announce <titleColor> "title" <descColor> "description"
            .then(Commands.argument("titleColor", StringArgumentType.word())
                .suggests((context, builder) -> {
                    Arrays.stream(Formatting.values())
                        .filter(Formatting::isColor)
                        .forEach(color -> builder.suggest(color.getName()));
                    return builder.buildFuture();
                })
                .then(Commands.argument("title", StringArgumentType.string())
                    .then(Commands.argument("descColor", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Arrays.stream(Formatting.values())
                                .filter(Formatting::isColor)
                                .forEach(color -> builder.suggest(color.getName()));
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("description", StringArgumentType.string())
                            .executes(context -> {
                                String titleColorName = StringArgumentType.getString(context, "titleColor");
                                String title = StringArgumentType.getString(context, "title");
                                String descColorName = StringArgumentType.getString(context, "descColor");
                                String description = StringArgumentType.getString(context, "description");
                                
                                Formatting titleColor = Formatting.getByName(titleColorName);
                                Formatting descColor = Formatting.getByName(descColorName);
                                
                                if (titleColor == null || !titleColor.isColor()) {
                                    context.getSource().sendFailure(Text.literal("Invalid title color: " + titleColorName));
                                    return 0;
                                }
                                
                                if (descColor == null || !descColor.isColor()) {
                                    context.getSource().sendFailure(Text.literal("Invalid description color: " + descColorName));
                                    return 0;
                                }
                                
                                sendAnnouncement(context.getSource(), title, description, titleColor, descColor);
                                return 1;
                            })))))
        );
    }
    
    private static void sendAnnouncement(ServerCommandSource source, String title, String description, 
                                        Formatting titleColor, Formatting descColor) {
        MutableText titleComponent = Text.literal(title).withStyle(titleColor);
        MutableText subtitleComponent = Text.literal(description).withStyle(descColor);
        
        // Send to all players
        for (ServerPlayerEntity player : source.getServer().getPlayerList().getPlayers()) {
            // Set timing (fade in, stay, fade out) in ticks
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            
            // Send title and subtitle
            player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
        }
        
        // Also send to chat for logging
        Text chatMessage = Text.literal("[ANNOUNCEMENT] ")
            .withStyle(Formatting.GOLD, Formatting.BOLD)
            .append(titleComponent)
            .append(Text.literal(" - ").withStyle(Formatting.GRAY))
            .append(subtitleComponent);
        
        source.getServer().getPlayerList().broadcastSystemMessage(chatMessage, false);
        
        // Feedback to command sender
        source.sendSuccess(() -> Text.literal("Announcement sent to all players!"), true);
    }
} 