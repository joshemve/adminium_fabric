package com.adminium.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Stores the list of spawn-pod locations (one per dimension).
 */
public class PodManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File CONFIG_FILE = new File("config/adminium_pods.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of dimension → immutable list of positions
    private static final Map<RegistryKey<World>, List<BlockPos>> pods = new HashMap<>();

    // players to exclude from /pods start teleport (runtime only)
    private static final Set<java.util.UUID> excludedPlayers = new java.util.HashSet<>();
    private static final String JSON_KEY_EXCLUDED = "excluded";

    private static MinecraftServer server;

    public static void setServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    /* ------------------------------ Networking ----------------------------- */
    public static void syncToPlayer(net.minecraft.server.level.ServerPlayerEntity player) {
        com.adminium.mod.network.ModNetworking.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.adminium.mod.network.S2CSyncPodsPacket(copyPods())
        );
    }

    public static void syncToAllPlayers(MinecraftServer srv) {
        if (srv == null) return;
        com.adminium.mod.network.ModNetworking.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.ALL.noArg(),
            new com.adminium.mod.network.S2CSyncPodsPacket(copyPods())
        );
    }

    // Helper to deep-copy pod map
    private static Map<RegistryKey<World>, List<BlockPos>> copyPods() {
        Map<RegistryKey<World>, List<BlockPos>> map = new HashMap<>();
        pods.forEach((dim, list) -> map.put(dim, new ArrayList<>(list)));
        return map;
    }

    // Client-side: replace pod data with server-sent map
    public static void clientReplacePods(Map<RegistryKey<World>, List<BlockPos>> newData) {
        pods.clear();
        pods.putAll(newData);
    }

    /* ------------------------------- Pod CRUD ------------------------------- */
    public static void addPod(RegistryKey<World> dimension, BlockPos pos) {
        pods.computeIfAbsent(dimension, k -> new ArrayList<>()).add(pos.immutable());
        save();
        if (server != null) syncToAllPlayers(server);
    }

    public static boolean removePod(RegistryKey<World> dimension, BlockPos pos) {
        List<BlockPos> list = pods.get(dimension);
        if (list == null) return false;
        boolean removed = list.remove(pos);
        if (removed) {
            if (list.isEmpty()) pods.remove(dimension);
            save();
            if (server != null) syncToAllPlayers(server);
        }
        return removed;
    }

    public static void clear() {
        pods.clear();
        save();
        if (server != null) syncToAllPlayers(server);
    }

    public static List<BlockPos> getPods(RegistryKey<World> dimension) {
        return pods.getOrDefault(dimension, Collections.emptyList());
    }

    public static int getTotalPodCount() {
        return pods.values().stream().mapToInt(List::size).sum();
    }

    /* --------------------------- PlayerEntity exclusions -------------------------- */
    public static void addExcluded(java.util.UUID uuid) {
        excludedPlayers.add(uuid);
        save();
    }

    public static void removeExcluded(java.util.UUID uuid) {
        excludedPlayers.remove(uuid);
        save();
    }

    public static boolean isExcluded(java.util.UUID uuid) {
        return excludedPlayers.contains(uuid);
    }

    public static java.util.Set<java.util.UUID> getExcludedPlayers() {
        return java.util.Collections.unmodifiableSet(excludedPlayers);
    }

    /* ----------------------------- Teleportation ---------------------------- */
    /**
     * Returns a shuffled list assigning at most one pod to each player. Players beyond
     * available pods will be left out of the resulting map.
     */
    public static <T> Map<T, BlockPos> assignPodsRandomly(Collection<T> players, RegistryKey<World> dimension) {
        List<BlockPos> available = new ArrayList<>(getPods(dimension));
        Collections.shuffle(available);

        List<T> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        int count = Math.min(available.size(), shuffledPlayers.size());
        Map<T, BlockPos> out = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            out.put(shuffledPlayers.get(i), available.get(i));
        }
        return out;
    }

    /* ------------------------------ Persistence ----------------------------- */
    public static void load() {
        pods.clear();
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            // load exclusions first
            excludedPlayers.clear();
            if (root.has(JSON_KEY_EXCLUDED)) {
                JsonArray arrExc = root.getAsJsonArray(JSON_KEY_EXCLUDED);
                for (JsonElement el : arrExc) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(el.getAsString());
                        excludedPlayers.add(id);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            for (String dimKey : root.keySet()) {
                if (dimKey.equals(JSON_KEY_EXCLUDED)) continue; // skip special key
                RegistryKey<World> dimension = RegistryKey.create(net.minecraft.core.registries.Registries.DIMENSION, new Identifier(dimKey));
                JsonArray arr = root.getAsJsonArray(dimKey);
                List<BlockPos> list = new ArrayList<>();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    int x = obj.get("x").getAsInt();
                    int y = obj.get("y").getAsInt();
                    int z = obj.get("z").getAsInt();
                    list.add(new BlockPos(x, y, z));
                }
                pods.put(dimension, list);
            }
            LOGGER.info("Loaded {} pod locations across {} dimension(s)", getTotalPodCount(), pods.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load pod config", e);
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            pods.forEach((dimension, list) -> {
                JsonArray arr = new JsonArray();
                for (BlockPos pos : list) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("x", pos.getX());
                    obj.addProperty("y", pos.getY());
                    obj.addProperty("z", pos.getZ());
                    arr.add(obj);
                }
                root.add(dimension.location().toString(), arr);
            });

            // Save excluded players
            JsonArray excArr = new JsonArray();
            for (java.util.UUID id : excludedPlayers) {
                excArr.add(id.toString());
            }
            root.add(JSON_KEY_EXCLUDED, excArr);

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save pod config", e);
        }
    }
} 