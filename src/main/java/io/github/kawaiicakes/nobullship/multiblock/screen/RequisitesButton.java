package io.github.kawaiicakes.nobullship.multiblock.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class RequisitesButton extends ImageButton {
    public static final ResourceLocation REQUISITE_ICON
            = new ResourceLocation(MOD_ID, "textures/gui/requisite_icon.png");

    public RequisitesButton(BlockPos pos, Button.OnTooltip onTooltip) {
        super(
                0, 0,
                16, 16,
                0, 0, 16,
                REQUISITE_ICON, 16, 48,
                ((button) -> Minecraft.getInstance().setScreen(new RequisiteScreen(pos))),
                onTooltip,
                Component.empty()
        );
        this.active = false;
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }
}
