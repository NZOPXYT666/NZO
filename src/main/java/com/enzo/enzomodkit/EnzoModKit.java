package com.enzo.enzomodkit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EnzoModKit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            
            // SAVE: /nzosave <name>
            dispatcher.register(ClientCommandManager.literal("nzosave")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) return 0;

                    List<String> commands = new ArrayList<>();
                    for (int i = 0; i < client.player.getInventory().size(); i++) {
                        ItemStack stack = client.player.getInventory().getStack(i);
                        if (!stack.isEmpty()) {
                            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                            String components = stack.getComponents().toString();
                            commands.add("item replace entity @s container." + i + " with " + itemId + components);
                        }
                    }

                    try {
                        File file = new File(client.runDirectory, "nzo_kits/" + name + ".txt");
                        file.getParentFile().mkdirs();
                        Files.write(file.toPath(), commands);
                        context.getSource().sendFeedback(Text.literal("§a[NZO] Kit '" + name + "' saved!"));
                    } catch (Exception e) {
                        context.getSource().sendFeedback(Text.literal("§c[NZO] Error saving kit!"));
                    }
                    return 1;
                })));

            // LOAD: /nzoload <name>
            dispatcher.register(ClientCommandManager.literal("nzoload")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    MinecraftClient client = MinecraftClient.getInstance();
                    File file = new File(client.runDirectory, "nzo_kits/" + name + ".txt");

                    if (!file.exists()) {
                        context.getSource().sendFeedback(Text.literal("§c[NZO] Kit not found!"));
                        return 0;
                    }

                    try {
                        List<String> lines = Files.readAllLines(file.toPath());
                        for (String cmd : lines) {
                            client.player.networkHandler.sendChatCommand(cmd);
                        }
                        context.getSource().sendFeedback(Text.literal("§6[NZO] Kit '" + name + "' loaded!"));
                    } catch (Exception e) {
                        context.getSource().sendFeedback(Text.literal("§c[NZO] Error loading kit!"));
                    }
                    return 1;
                })));
        });
    }
}
