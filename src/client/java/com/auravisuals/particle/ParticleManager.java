package com.auravisuals.particle;

import com.auravisuals.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;

import java.util.Random;

/**
 * Spawns Minecraft's own built-in "dust" particle (the same effect used for redstone dust /
 * sniffer eggs / brushable blocks, which takes an arbitrary RGB color) instead of rendering
 * custom geometry ourselves. Letting vanilla own the actual rendering means we don't depend on
 * Minecraft's internal rendering pipeline internals, which change significantly between versions.
 *
 *  - AMBIENT: a steady trickle of motes spawned in a radius around the player, drifting slowly.
 *  - AURA: particles continuously re-spawned along an orbit path around the player, which (since
 *    each one only needs to live a couple of ticks) reads visually as a ring/aura orbiting them.
 */
public class ParticleManager {

	private static final ParticleManager INSTANCE = new ParticleManager();

	public static ParticleManager getInstance() {
		return INSTANCE;
	}

	private final Random random = new Random();

	// Persistent per-slot orbit state for the aura particles.
	private double[] auraAngle = new double[0];
	private double[] auraRadius = new double[0];
	private double[] auraHeight = new double[0];
	private double[] auraSpeed = new double[0];

	private static final double BASE_AMBIENT_PER_TICK = 1.2;
	private static final int BASE_AURA_COUNT = 16;
	private static final double AMBIENT_RADIUS = 4.0;

	private double ambientCarry = 0.0;

	private ParticleManager() {
	}

	public void tick(MinecraftClient client) {
		ModConfig config = ModConfig.get();
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;

		if (player == null || world == null) {
			return;
		}

		if (config.ambientEnabled) {
			tickAmbient(world, player, config);
		}

		if (config.auraEnabled) {
			tickAura(world, player, config);
		}
	}

	private void tickAmbient(ClientWorld world, ClientPlayerEntity player, ModConfig config) {
		DustParticleEffect effect = dust(config.ambientColor, 1.1f);

		// Fractional spawn-rate accumulator so low densities don't just round down to zero.
		ambientCarry += BASE_AMBIENT_PER_TICK * config.density;
		int toSpawn = (int) ambientCarry;
		ambientCarry -= toSpawn;

		for (int i = 0; i < toSpawn; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = random.nextDouble() * AMBIENT_RADIUS;
			double x = player.getX() + Math.cos(angle) * dist;
			double y = player.getY() + random.nextDouble() * 2.5;
			double z = player.getZ() + Math.sin(angle) * dist;

			double vx = (random.nextDouble() - 0.5) * 0.01 * config.speed;
			double vy = 0.004 * config.speed;
			double vz = (random.nextDouble() - 0.5) * 0.01 * config.speed;

			world.addParticle(effect, true, false, x, y, z, vx, vy, vz);
		}
	}

	private void tickAura(ClientWorld world, ClientPlayerEntity player, ModConfig config) {
		int count = Math.max(0, Math.round(BASE_AURA_COUNT * config.density));
		ensureAuraSlots(count);

		DustParticleEffect effect = dust(config.auraColor, 0.9f);

		for (int i = 0; i < count; i++) {
			auraAngle[i] += auraSpeed[i] * config.speed;
			double x = player.getX() + Math.cos(auraAngle[i]) * auraRadius[i];
			double y = player.getY() + 0.2 + auraHeight[i];
			double z = player.getZ() + Math.sin(auraAngle[i]) * auraRadius[i];

			// Zero velocity: we re-place it next tick ourselves, we don't want vanilla physics
			// (gravity/drag) moving it away from the orbit path in the meantime.
			world.addParticle(effect, true, false, x, y, z, 0.0, 0.0, 0.0);
		}
	}

	private void ensureAuraSlots(int count) {
		if (auraAngle.length == count) {
			return;
		}

		double[] newAngle = new double[count];
		double[] newRadius = new double[count];
		double[] newHeight = new double[count];
		double[] newSpeed = new double[count];

		int reuse = Math.min(count, auraAngle.length);
		System.arraycopy(auraAngle, 0, newAngle, 0, reuse);
		System.arraycopy(auraRadius, 0, newRadius, 0, reuse);
		System.arraycopy(auraHeight, 0, newHeight, 0, reuse);
		System.arraycopy(auraSpeed, 0, newSpeed, 0, reuse);

		for (int i = reuse; i < count; i++) {
			newAngle[i] = random.nextDouble() * Math.PI * 2;
			newRadius[i] = 0.9 + random.nextDouble() * 0.7;
			newHeight[i] = random.nextDouble() * 2.0;
			newSpeed[i] = (0.05 + random.nextDouble() * 0.05) * (random.nextBoolean() ? 1 : -1);
		}

		auraAngle = newAngle;
		auraRadius = newRadius;
		auraHeight = newHeight;
		auraSpeed = newSpeed;
	}

	private DustParticleEffect dust(int rgb, float scale) {
		// 1.21.11's DustParticleEffect takes a packed 0xRRGGBB color directly, not a Vector3f.
		return new DustParticleEffect(rgb, scale);
	}
}
