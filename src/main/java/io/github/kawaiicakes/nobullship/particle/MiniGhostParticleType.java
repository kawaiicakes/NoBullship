package io.github.kawaiicakes.nobullship.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public class MiniGhostParticleType extends ParticleType<MiniGhostParticleOption> {
    public MiniGhostParticleType(boolean pOverrideLimiter, ParticleOptions.Deserializer<MiniGhostParticleOption> pDeserializer) {
        super(pOverrideLimiter, pDeserializer);
    }

    @Override
    public Codec<MiniGhostParticleOption> codec() {
        return null;
    }
}
