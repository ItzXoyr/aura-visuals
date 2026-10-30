package com.auravisuals.particle;

/**
 * A single lightweight "shader-like" particle simulated and rendered
 * entirely on the client, without touching the vanilla particle system.
 */
public class FloatingParticle {

	public enum Kind {
		AMBIENT,
		AURA
	}

	public double x, y, z;
	public double velX, velY, velZ;
	public float size;
	public int age;
	public int maxAge;
	public final Kind kind;

	// Used only by AURA particles to orbit the player.
	public double orbitAngle;
	public double orbitRadius;
	public double orbitHeight;
	public double orbitSpeed;

	public FloatingParticle(Kind kind) {
		this.kind = kind;
	}

	/** 0 at birth/death, 1 at full brightness - used to fade in/out smoothly. */
	public float lifeAlpha() {
		if (maxAge <= 0) return 1f;
		float t = (float) age / (float) maxAge;
		float fadeIn = Math.min(1f, age / 10f);
		float fadeOut = Math.min(1f, (maxAge - age) / 10f);
		return Math.max(0f, Math.min(fadeIn, fadeOut));
	}

	public boolean isDead() {
		return age >= maxAge;
	}
}
