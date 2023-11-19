package io.github.kawaiicakes.nobullship.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class MiniGhostParticleOption implements ParticleOptions {
    public static final ParticleOptions.Deserializer<MiniGhostParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public MiniGhostParticleOption fromCommand(ParticleType<MiniGhostParticleOption> pParticleType, StringReader pReader) throws CommandSyntaxException {
            return null;
        }

        @Override
        public MiniGhostParticleOption fromNetwork(ParticleType<MiniGhostParticleOption> pParticleType, FriendlyByteBuf pBuffer) {
            return null;
        }
    };

    @Override
    public ParticleType<?> getType() {
        return null;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf pBuffer) {

    }

    @Override
    public String writeToString() {
        return null;
    }
}
