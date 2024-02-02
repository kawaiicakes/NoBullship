package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SaveSelectionScreen extends Screen {
    public static final Component NAME_LABEL = Component.translatable("structure_block.structure_name");

    @Nullable
    protected final BlockPos pos1;
    @Nullable
    protected final BlockPos pos2;
    protected EditBox selectionName;

    public SaveSelectionScreen(ItemStack wandItem) {
        super(Component.translatable("gui.nobullship.schematic_saver"));
        CompoundTag wandTag = wandItem.getOrCreateTag();

        int[] pos1Temp = null;
        int[] pos2Temp = null;

        if (wandTag.contains("pos1", Tag.TAG_INT_ARRAY)) {
            int[] pos1Arr = wandTag.getIntArray("pos1");
            pos1Temp = new int[]{pos1Arr[0], pos1Arr[1], pos1Arr[2]};
        }
        if (wandTag.contains("pos2", Tag.TAG_INT_ARRAY)) {
            int[] pos2Arr = wandTag.getIntArray("pos2");
            pos2Temp = new int[]{pos2Arr[0], pos2Arr[1], pos2Arr[2]};
        }

        if (pos1Temp != null && pos2Temp != null) {
            this.pos1 = new BlockPos(Math.min(pos1Temp[0], pos2Temp[0]), Math.min(pos1Temp[1], pos2Temp[1]), Math.min(pos1Temp[1], pos2Temp[1]));
            this.pos2 = new BlockPos(Math.max(pos1Temp[0], pos2Temp[0]), Math.max(pos1Temp[1], pos2Temp[1]), Math.max(pos1Temp[1], pos2Temp[1]));
        } else {
            this.pos1 = null;
            this.pos2 = null;
        }

        this.font = Minecraft.getInstance().font;
    }

    @Override
    protected void init() {
        this.selectionName = this.addWidget(new EditBox(this.font, this.width / 2 - 152, 40, 300, 20, Component.translatable("structure_block.structure_name")) {
            public boolean charTyped(char pCodePoint, int pModifiers) {
                return SaveSelectionScreen.this.isValidCharacterForName(this.getValue(), pCodePoint, this.getCursorPosition()) && super.charTyped(pCodePoint, pModifiers);
            }
        });
        this.selectionName.setMaxLength(128);
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.selectionName.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.selectionName.setValue(s);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 10, 16777215);
        drawString(pPoseStack, this.font, NAME_LABEL, this.width / 2 - 153, 30, 10526880);
        this.selectionName.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void tick() {
        this.selectionName.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
