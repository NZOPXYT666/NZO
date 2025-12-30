package com.enzo.enzomodkit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class EnzoModKit implements ClientModInitializer {
    private File kitDir;
    private final Queue<String> commandQueue = new LinkedList<>();
    private int tickDelay = 0;

    @Override
    public void onInitializeClient() {
        kitDir = new File(MinecraftClient.getInstance().runDirectory, "config/enzomodkit/kits");
        if (!kitDir.exists()) kitDir.mkdirs();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!commandQueue.isEmpty()) {
                if (tickDelay <= 0) {
                    String cmd = commandQueue.poll();
                    if (client.getNetworkHandler() != null && cmd != null) {
                        client.getNetworkHandler().sendChatCommand(cmd);
                    }
                    tickDelay = 2; 
                } else {
                    tickDelay--;
                }
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("nzo")
                .then(ClientCommandManager.literal("save")
                    .then(ClientCommandManager.argument("kitname", StringArgumentType.string())
                        .executes(context -> {
                            saveKit(StringArgumentType.getString(context, "kitname"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("load")
                    .then(ClientCommandManager.argument("kitname", StringArgumentType.string())
                        .executes(context -> {
                            loadKit(StringArgumentType.getString(context, "kitname"));
                            return 1;
                        }))));
        });
    }

    private void saveKit(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PlayerInventory inv = client.player.getInventory();
        List<String> lines = new ArrayList<>();
        lines.add("# EnzoModKit v1");

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            String slotName = (i < 9) ? "hotbar." + i : "inventory." + (i - 9);
            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            String components = stack.getComponents().isEmpty() ? "" : stack.getComponents().toString();

            String cmd = String.format("item replace entity @s %s with %s%s %d", 
                          slotName, itemId, components, stack.getCount());

            if (cmd.length() > 256) {
                client.player.sendMessage(Text.literal("§6[!] Slot " + i + " too long, skipped."), false);
                continue;
            }
            lines.add(cmd);
        }

        try {
            Files.write(new File(kitDir, name + ".txt").toPath(), lines);
            client.player.sendMessage(Text.literal("§aKit '" + name + "' saved!"), false);
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§cSave failed."), false);
        }
    }

    private void loadKit(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        File file = new File(kitDir, name + ".txt");
        if (!file.exists()) return;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            commandQueue.clear();
            for (String line : lines) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                commandQueue.add(line);
            }
        } catch (Exception e) {
            if (client.player != null) client.player.sendMessage(Text.literal("§cLoad failed."), false);
        }
    }
}
