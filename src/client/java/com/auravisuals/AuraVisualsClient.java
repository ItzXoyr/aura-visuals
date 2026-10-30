package com.auravisuals;

import com.auravisuals.config.ConfigScreen;
import com.auravisuals.config.ModConfig;
import com.auravisuals.particle.ParticleManager;
import com.auravisuals.particle.ParticleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AuraVisualsClient implements ClientModInitializer {

	private static KeyBinding openConfigKey;

	@Override
	public void onInitializeClient() {
		// Load (or create) the config as soon as possible.
		ModConfig.get();

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auravisuals.open_config",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_COMMA,
				"key.category.auravisuals"
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ParticleRenderer::render);
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
