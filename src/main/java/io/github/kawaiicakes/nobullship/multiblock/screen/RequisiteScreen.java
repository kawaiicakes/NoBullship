package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.network.ServerboundWorkshopOpenPacket;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class RequisiteScreen extends Screen {
    protected final BlockPos blockEntityPos;
    protected final ImmutableList<ItemStack> requisites;
    protected final int maxPages;
    protected int page;

    protected RequisiteScreen(BlockPos blockEntityPos) {
        super(Component.empty());
        this.blockEntityPos = blockEntityPos;
        if (Minecraft.getInstance().level == null) throw new IllegalArgumentException("No client level yet a screen attempted to be instantiated!");

        BlockEntity be = Minecraft.getInstance().level.getExistingBlockEntity(this.blockEntityPos);
        if (!(be instanceof MultiblockWorkshopBlockEntity workshop)) throw new IllegalArgumentException("No workshop at passed coords!");

        List<SchematicRecipe> recipeList
                = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(SchematicRecipe.Type.INSTANCE);
        if (recipeList.isEmpty()) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        List<SchematicRecipe> shapedMatches = recipeList.stream()
                .filter(recipe -> recipe.shapedMatches(workshop))
                .toList();

        Optional<SchematicRecipe> matchingRecipe = shapedMatches.stream().findFirst();
        if (shapedMatches.size() > 1)
            matchingRecipe = shapedMatches.stream().filter(recipe -> recipe.shapelessMatches(workshop)).findFirst();
        if (matchingRecipe.isEmpty()) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        MultiblockRecipe resultRecipe = MultiblockRecipeManager.getInstance().getRecipe(matchingRecipe.get().getResultId()).orElse(null);
        if (resultRecipe == null) {
            this.requisites = ImmutableList.of();
            this.maxPages = 0;
            return;
        }

        ImmutableList.Builder<ItemStack> requisiteBuilder = ImmutableList.builder();
        if (matchingRecipe.get().getRequisites() != null) requisiteBuilder.addAll(matchingRecipe.get().getRequisites());
        // written like this because for some reason IntelliJ states that resultRecipe.requisites() could be null
        // even after the null check... I'm paranoid, so I'll just acquiesce to its desires despite me being fairly certain that it's fine
        ImmutableList<ItemStack> multiblockReqs = resultRecipe.requisites();
        if (multiblockReqs != null) requisiteBuilder.addAll(multiblockReqs);
        
        this.requisites = requisiteBuilder.build();

        // TODO: make this more precise
        this.maxPages = (this.requisites.size() / 36) + 1;
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        for (ItemStack stack : this.requisites) {
            if (stack == null || stack.isEmpty()) continue;
            // TODO: fix rendering items lol (including making/using pages)
            // Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, pMouseX, pMouseY, 0);
            // this.renderTooltip(pPoseStack, stack, pMouseX, pMouseY);
        }
    }

    @Override
    public void removed() {
        super.removed();
        NoBullshipPackets.sendToServer(new ServerboundWorkshopOpenPacket(blockEntityPos));
    }
}
