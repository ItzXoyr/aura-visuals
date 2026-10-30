package com.auravisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-backed config. No external config library is required:
 * it's just Gson (bundled with Minecraft) writing to config/auravisuals.json.
 */
public class ModConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("auravisuals.json");

	private static ModConfig instance;

	public boolean ambientEnabled = true;
	public boolean auraEnabled = true;

	// Stored as 0xRRGGBB
	public int ambientColor = 0x55E8FF; // ciano
	public int auraColor = 0xFF5CF0;    // magenta

	public float density = 1.0f; // 0.1 - 3.0
	public float speed = 1.0f;   // 0.1 - 3.0

	public static ModConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			System.err.println("[AuraVisuals] Impossibile salvare la config: " + e.getMessage());
		}
	}

	private static ModConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException e) {
				System.err.println("[AuraVisuals] Impossibile leggere la config, uso i valori di default: " + e.getMessage());
			}
		}
		ModConfig fresh = new ModConfig();
		fresh.save();
		return fresh;
	}
}
