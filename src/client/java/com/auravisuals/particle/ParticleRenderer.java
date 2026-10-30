package com.auravisuals.particle;

import com.auravisuals.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws every active particle as a camera-facing glowing quad with additive
 * blending, which gives a cheap "shader-like" glow without needing a real
 * shader pack or a custom GLSL pipeline.
 */
public final class ParticleRenderer {

	private ParticleRenderer() {
	}

	public static void render(WorldRenderContext context) {
		ModConfig config = ModConfig.get();
		ParticleManager manager = ParticleManager.getInstance();

		if (manager.getAmbientParticles().isEmpty() && manager.getAuraParticles().isEmpty()) {
			return;
		}

		Vec3d camPos = context.camera().getPos();
		MatrixStack matrices = context.matrixStack();
		if (matrices == null) {
			return;
		}

		// Camera-facing basis vectors, so every quad billboards towards the player.
		Vector3f right = new Vector3f(1, 0, 0).rotate(context.camera().getRotation());
		Vector3f up = new Vector3f(0, 1, 0).rotate(context.camera().getRotation());

		matrices.push();
		matrices.translate(-camPos.x, -camPos.y, -camPos.z);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(
				com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
				com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE); // additive glow
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		if (config.ambientEnabled) {
			int argb = config.ambientColor;
			for (FloatingParticle p : manager.getAmbientParticles()) {
				float alpha = p.lifeAlpha() * 0.85f;
				addQuad(buffer, matrix, right, up, p, argb, alpha);
			}
		}

		if (config.auraEnabled) {
			int argb = config.auraColor;
			for (FloatingParticle p : manager.getAuraParticles()) {
				float alpha = manager.auraAlpha(p) * 0.9f;
				addQuad(buffer, matrix, right, up, p, argb, alpha);
			}
		}

		BufferRenderer.drawWithGlobalProgram(buffer.end());

		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();

		matrices.pop();
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vector3f right, Vector3f up,
			FloatingParticle p, int rgb, float alpha) {
		if (alpha <= 0f) return;

		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;

		float hs = p.size; // half-size
		float x = (float) p.x;
		float y = (float) p.y;
		float z = (float) p.z;

		float x1 = x - right.x * hs - up.x * hs;
		float y1 = y - right.y * hs - up.y * hs;
		float z1 = z - right.z * hs - up.z * hs;

		float x2 = x + right.x * hs - up.x * hs;
		float y2 = y + right.y * hs - up.y * hs;
		float z2 = z + right.z * hs - up.z * hs;

		float x3 = x + right.x * hs + up.x * hs;
		float y3 = y + right.y * hs + up.y * hs;
		float z3 = z + right.z * hs + up.z * hs;

		float x4 = x - right.x * hs + up.x * hs;
		float y4 = y - right.y * hs + up.y * hs;
		float z4 = z - right.z * hs + up.z * hs;

		buffer.vertex(matrix, x1, y1, z1).color(r, g, b, alpha);
		buffer.vertex(matrix, x2, y2, z2).color(r, g, b, alpha);
		buffer.vertex(matrix, x3, y3, z3).color(r, g, b, alpha);
		buffer.vertex(matrix, x4, y4, z4).color(r, g, b, alpha);
	}
}
