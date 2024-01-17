package io.github.kawaiicakes.nobullship.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class ItemMarker extends TextureSheetParticle {
    ItemMarker(ClientLevel pLevel, double pX, double pY, double pZ, ItemStack stack) {
        super(pLevel, pX, pY, pZ);

        BakedModel bakedModel = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(stack);
        this.setSprite(bakedModel.getParticleIcon(ModelData.EMPTY));

        this.gravity = 0.0F;
        this.lifetime = 0;
        this.hasPhysics = false;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public float getQuadSize(float pScaleFactor) {
        return 0.3F;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<ItemParticleOption> {
        public Particle createParticle(ItemParticleOption p_194277_, ClientLevel p_194278_, double p_194279_, double p_194280_, double p_194281_, double p_194282_, double p_194283_, double p_194284_) {
            return new ItemMarker(p_194278_, p_194279_, p_194280_, p_194281_, p_194277_.getItem());
        }
    }
}