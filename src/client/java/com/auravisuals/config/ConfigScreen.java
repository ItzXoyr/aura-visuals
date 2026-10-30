package com.auravisuals.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Plain vanilla-widget config screen - no extra GUI library dependency needed.
 * Open with the keybinding registered in AuraVisualsClient (default: comma ",").
 */
public class ConfigScreen extends Screen {

	private final Screen parent;

	private ButtonWidget ambientToggle;
	private ButtonWidget auraToggle;
	private TextFieldWidget ambientColorField;
	private TextFieldWidget auraColorField;

	// Working copy, only written back to ModConfig on Save.
	private boolean ambientEnabled;
	private boolean auraEnabled;
	private int ambientColor;
	private int auraColor;
	private float density;
	private float speed;

	public ConfigScreen(Screen parent) {
		super(Text.translatable("auravisuals.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig config = ModConfig.get();
		ambientEnabled = config.ambientEnabled;
		auraEnabled = config.auraEnabled;
		ambientColor = config.ambientColor;
		auraColor = config.auraColor;
		density = config.density;
		speed = config.speed;

		int centerX = this.width / 2;
		int top = this.height / 2 - 90;
		int rowHeight = 24;
		int fieldWidth = 200;

		// Ambient toggle
		ambientToggle = ButtonWidget.builder(toggleLabel("auravisuals.config.ambient.toggle", ambientEnabled), btn -> {
			ambientEnabled = !ambientEnabled;
			btn.setMessage(toggleLabel("auravisuals.config.ambient.toggle", ambientEnabled));
		}).dimensions(centerX - fieldWidth / 2, top, fieldWidth, 20).build();
		this.addDrawableChild(ambientToggle);

		// Ambient color field
		ambientColorField = new TextFieldWidget(this.textRenderer, centerX - fieldWidth / 2, top + rowHeight, fieldWidth, 20,
				Text.translatable("auravisuals.config.ambient.color"));
		ambientColorField.setMaxLength(6);
		ambientColorField.setText(toHex(ambientColor));
		this.addDrawableChild(ambientColorField);

		// Aura toggle
		auraToggle = ButtonWidget.builder(toggleLabel("auravisuals.config.aura.toggle", auraEnabled), btn -> {
			auraEnabled = !auraEnabled;
			btn.setMessage(toggleLabel("auravisuals.config.aura.toggle", auraEnabled));
		}).dimensions(centerX - fieldWidth / 2, top + rowHeight * 2 + 6, fieldWidth, 20).build();
		this.addDrawableChild(auraToggle);

		// Aura color field
		auraColorField = new TextFieldWidget(this.textRenderer, centerX - fieldWidth / 2, top + rowHeight * 3 + 6, fieldWidth, 20,
				Text.translatable("auravisuals.config.aura.color"));
		auraColorField.setMaxLength(6);
		auraColorField.setText(toHex(auraColor));
		this.addDrawableChild(auraColorField);

		// Density slider
		this.addDrawableChild(new LabeledSlider(centerX - fieldWidth / 2, top + rowHeight * 4 + 14, fieldWidth, 20,
				"auravisuals.config.density", density, 0.1, 3.0, value -> density = (float) value));

		// Speed slider
		this.addDrawableChild(new LabeledSlider(centerX - fieldWidth / 2, top + rowHeight * 5 + 14, fieldWidth, 20,
				"auravisuals.config.speed", speed, 0.1, 3.0, value -> speed = (float) value));

		// Save / Cancel
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("auravisuals.config.save"), btn -> save())
				.dimensions(centerX - fieldWidth / 2, top + rowHeight * 6 + 24, fieldWidth / 2 - 4, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.translatable("auravisuals.config.cancel"), btn -> close())
				.dimensions(centerX + 4, top + rowHeight * 6 + 24, fieldWidth / 2 - 4, 20).build());
	}

	private Text toggleLabel(String key, boolean enabled) {
		String state = enabled ? "auravisuals.config.on" : "auravisuals.config.off";
		return Text.translatable(key).append(": ").append(Text.translatable(state));
	}

	private static String toHex(int rgb) {
		return String.format("%06X", rgb & 0xFFFFFF);
	}

	private static int parseHexOrDefault(String text, int fallback) {
		try {
			String cleaned = text.replace("#", "").trim();
			if (cleaned.isEmpty()) return fallback;
			return Integer.parseInt(cleaned, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private void save() {
		ModConfig config = ModConfig.get();
		config.ambientEnabled = ambientEnabled;
		config.auraEnabled = auraEnabled;
		config.ambientColor = parseHexOrDefault(ambientColorField.getText(), config.ambientColor);
		config.auraColor = parseHexOrDefault(auraColorField.getText(), config.auraColor);
		config.density = density;
		config.speed = speed;
        config.save();
		close();
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 110, 0xFFFFFF);

		// Live color swatches next to the hex fields, so you see the color before saving.
		int liveAmbient = parseHexOrDefault(ambientColorField.getText(), ambientColor);
		int liveAura = parseHexOrDefault(auraColorField.getText(), auraColor);
		context.fill(ambientColorField.getX() + ambientColorField.getWidth() + 6, ambientColorField.getY(),
				ambientColorField.getX() + ambientColorField.getWidth() + 22, ambientColorField.getY() + 20,
				0xFF000000 | liveAmbient);
		context.fill(auraColorField.getX() + auraColorField.getWidth() + 6, auraColorField.getY(),
				auraColorField.getX() + auraColorField.getWidth() + 22, auraColorField.getY() + 20,
				0xFF000000 | liveAura);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Small helper SliderWidget bound to a double value via a setter callback. */
	private static class LabeledSlider extends SliderWidget {
		private final String labelKey;
		private final double min;
		private final double max;
		private final java.util.function.DoubleConsumer onChange;

		LabeledSlider(int x, int y, int width, int height, String labelKey, double current, double min, double max,
				java.util.function.DoubleConsumer onChange) {
			super(x, y, width, height, Text.empty(), (current - min) / (max - min));
			this.labelKey = labelKey;
			this.min = min;
			this.max = max;
			this.onChange = onChange;
			updateMessage();
		}

		private double currentValue() {
			return min + (max - min) * this.value;
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Text.translatable(labelKey).append(": " + String.format("%.2f", currentValue())));
		}

		@Override
		protected void applyValue() {
			onChange.accept(currentValue());
		}
	}
}
