package com.github.externaltime.quickcommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.NoSuchElementException;

public class Persistence {
    private final static Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("quickcommands.json");
    private final static Path CONFIG_HELPER_PATH = CONFIG_PATH.resolveSibling("quickcommands.json.tmp");
    private final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private final Logger logger = LoggerFactory.getLogger(Persistence.class);
    private final Codec<Void> codec;

    public Persistence(Codec<Void> codec) {
        this.codec = codec;
    }

    public void save() {
        try {
            var asJson = codec.encodeStart(JsonOps.INSTANCE, null)
                    .result()
                    .orElseThrow();
            var asString = gson.toJson(asJson);
            Files.writeString(CONFIG_HELPER_PATH, asString, StandardCharsets.UTF_8);
            Files.move(CONFIG_HELPER_PATH, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | NoSuchElementException e) {
            logger.error("Failed to save Quick Commands config.", e);
        }
    }

    public void load() {
        try {
            var asString = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            var jsonElement = gson.fromJson(asString, new TypeToken<JsonElement>() {});
            codec.decode(JsonOps.INSTANCE, jsonElement)
                    .error()
                    .ifPresent(partial -> logger.error(partial.message()));
        } catch (NoSuchFileException ignored) {
            logger.info("Config file for Quick Commands not found.");
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load Quick Commands config.", e);
        }
    }
}
