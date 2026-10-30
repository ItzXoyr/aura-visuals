package com.auravisuals.particle;

import com.auravisuals.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulates both particle "visual styles":
 *  - AMBIENT: particles drifting slowly in the air around the player, like dust/magic motes.
 *  - AURA: particles orbiting tightly around the player, like a shader aura.
 * Rendering is handled separately by ParticleRenderer.
 */
public class ParticleManager {

	private static final ParticleManager INSTANCE = new ParticleManager();

	public static ParticleManager getInstance() {
		return INSTANCE;
	}

	private final Random random = new Random();
	private final List<FloatingParticle> ambientParticles = new ArrayList<>();
	private final List<FloatingParticle> auraParticles = new ArrayList<>();

	private static final int BASE_AMBIENT_TARGET = 60;
	private static final int BASE_AURA_TARGET = 24;
	private static final double AMBIENT_RADIUS = 6.0;

	private ParticleManager() {
	}

	public List<FloatingParticle> getAmbientParticles() {
		return ambientParticles;
	}

	public List<FloatingParticle> getAuraParticles() {
		return auraParticles;
	}

	public void tick(MinecraftClient client) {
		ModConfig config = ModConfig.get();
		ClientPlayerEntity player = client.player;

		if (player == null) {
			ambientParticles.clear();
			auraParticles.clear();
			return;
		}

		if (config.ambientEnabled) {
			tickAmbient(player, config);
		} else {
			ambientParticles.clear();
		}

		if (config.auraEnabled) {
			tickAura(player, config);
		} else {
			auraParticles.clear();
		}
	}

	private void tickAmbient(ClientPlayerEntity player, ModConfig config) {
		int target = Math.max(0, Math.round(BASE_AMBIENT_TARGET * config.density));

		// Spawn new particles up to target.
		while (ambientParticles.size() < target) {
			FloatingParticle p = new FloatingParticle(FloatingParticle.Kind.AMBIENT);

			double angle = random.nextDouble() * Math.PI * 2;
			double dist = random.nextDouble() * AMBIENT_RADIUS;
			p.x = player.getX() + Math.cos(angle) * dist;
			p.y = player.getY() + random.nextDouble() * 4.0;
			p.z = player.getZ() + Math.sin(angle) * dist;

			double driftSpeed = (0.004 + random.nextDouble() * 0.006) * config.speed;
			double driftAngle = random.nextDouble() * Math.PI * 2;
			p.velX = Math.cos(driftAngle) * driftSpeed;
			p.velZ = Math.sin(driftAngle) * driftSpeed;
			p.velY = (0.002 + random.nextDouble() * 0.004) * config.speed;

			p.size = 0.045f + random.nextFloat() * 0.05f;
			p.maxAge = 100 + random.nextInt(140);
			p.age = 0;

			ambientParticles.add(p);
		}

		for (int i = ambientParticles.size() - 1; i >= 0; i--) {
			FloatingParticle p = ambientParticles.get(i);
			p.x += p.velX;
			p.y += p.velY;
			p.z += p.velZ;
			p.age++;

			// Gently wobble horizontally so it doesn't look like it's rising in a straight line.
			p.velX += (random.nextDouble() - 0.5) * 0.0006 * config.speed;
			p.velZ += (random.nextDouble() - 0.5) * 0.0006 * config.speed;

			double dx = p.x - player.getX();
			double dz = p.z - player.getZ();
			if (p.isDead() || (dx * dx + dz * dz) > (AMBIENT_RADIUS * 2.0) * (AMBIENT_RADIUS * 2.0)) {
				ambientParticles.remove(i);
			}
		}
	}

	private void tickAura(ClientPlayerEntity player, ModConfig config) {
		int target = Math.max(0, Math.round(BASE_AURA_TARGET * config.density));

		while (auraParticles.size() < target) {
			FloatingParticle p = new FloatingParticle(FloatingParticle.Kind.AURA);
			p.orbitAngle = random.nextDouble() * Math.PI * 2;
			p.orbitRadius = 0.9 + random.nextDouble() * 0.7;
			p.orbitHeight = random.nextDouble() * 2.0;
			p.orbitSpeed = (0.03 + random.nextDouble() * 0.03) * config.speed * (random.nextBoolean() ? 1 : -1);
			p.size = 0.035f + random.nextFloat() * 0.035f;
			p.maxAge = 9999; // aura particles live as long as they're needed; we recycle instead of killing.
			p.age = 1;

			updateAuraPosition(player, p);
			auraParticles.add(p);
		}

		while (auraParticles.size() > target) {
			auraParticles.remove(auraParticles.size() - 1);
		}

		for (FloatingParticle p : auraParticles) {
			p.orbitAngle += p.orbitSpeed;
			if (p.age < 20) p.age++; // just for the fade-in on (re)spawn
			updateAuraPosition(player, p);
		}
	}

	private void updateAuraPosition(ClientPlayerEntity player, FloatingParticle p) {
		p.x = player.getX() + Math.cos(p.orbitAngle) * p.orbitRadius;
		p.z = player.getZ() + Math.sin(p.orbitAngle) * p.orbitRadius;
		p.y = player.getY() + 0.2 + p.orbitHeight;
	}

	/** Alpha for aura particles doesn't use maxAge the same way as ambient; small fade-in only. */
	public float auraAlpha(FloatingParticle p) {
		return Math.min(1f, p.age / 20f);
	}
}
