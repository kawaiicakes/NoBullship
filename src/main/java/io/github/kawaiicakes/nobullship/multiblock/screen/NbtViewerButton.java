package io.github.kawaiicakes.nobullship.multiblock.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class NbtViewerButton extends ImageButton {
    public static final ResourceLocation NBT_VIEWER_ICON
            = new ResourceLocation(MOD_ID, "textures/gui/nbt_viewer_icon.png");

    public NbtViewerButton(BlockPos pos) {
        // TODO: proper pos
        super(
                0, 0,
                16, 16,
                0, 0, 16,
                NBT_VIEWER_ICON, 16, 48,
                (button) -> Minecraft.getInstance().setScreen(new NbtViewerScreen(pos))
        );
        this.active = false;
        this.visible = false;
    }

    public void setVisibility(boolean isActive) {
        this.active = isActive;
        this.visible = isActive;
    }
}
