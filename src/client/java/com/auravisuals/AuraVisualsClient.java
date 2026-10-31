package com.auravisuals;

import com.auravisuals.config.ConfigScreen;
import com.auravisuals.config.ModConfig;
import com.auravisuals.particle.ParticleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AuraVisualsClient implements ClientModInitializer {

	private static KeyBinding openConfigKey;

	// Minecraft 1.21.9+ requires a KeyBinding.Category object instead of a bare translation string.
	private static final KeyBinding.Category CATEGORY =
			KeyBinding.Category.create(Identifier.of("auravisuals", "main"));

	@Override
	public void onInitializeClient() {
		// Load (or create) the config as soon as possible.
		ModConfig.get();

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auravisuals.open_config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_COMMA,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		// No custom world-render hook needed: particles are spawned through the vanilla
		// particle system (see ParticleManager), which Mojang keeps stable across the
		// rendering-pipeline rewrites that broke our old hand-rolled renderer.
	}

	private void onClientTick(MinecraftClient client) {
		ParticleManager.getInstance().tick(client);

		while (openConfigKey.wasPressed()) {
			if (client.currentScreen == null) {
				client.setScreen(new ConfigScreen(null));
			}
		}
	}
}
