package io.github.kawaiicakes.nobullship.multiblock.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class NbtViewerButton extends ImageButton {
    public static final ResourceLocation NBT_VIEWER_ICON
            = new ResourceLocation(MOD_ID, "textures/gui/nbt_viewer_icon.png");

    public NbtViewerButton(BlockPos pos, OnTooltip onTooltip) {
        super(
                0, 0,
                20, 16,
                0, 0, 16,
                NBT_VIEWER_ICON, 20, 48,
                ((button) -> Minecraft.getInstance().setScreen(new NbtViewerScreen(pos))),
                onTooltip,
                Component.empty()
        );
        this.active = false;
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }
}
