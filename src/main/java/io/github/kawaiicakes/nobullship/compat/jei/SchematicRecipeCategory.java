package io.github.kawaiicakes.nobullship.compat.jei;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static io.github.kawaiicakes.nobullship.NoBullship.*;
import static io.github.kawaiicakes.nobullship.Registry.SCHEMATIC;
import static io.github.kawaiicakes.nobullship.Registry.WORKSHOP_ITEM;
import static io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen.TEXTURE;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.Type.ID;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.getSummedContents;
import static mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
import static mezz.jei.api.recipe.RecipeIngredientRole.*;

public class SchematicRecipeCategory implements IRecipeCategory<SchematicRecipe>, IRecipeCategoryExtension {
    public static final ResourceLocation UID = new ResourceLocation(MOD_ID, ID);
    public static final Component TITLE = Component.translatable("block.nobullship.workshop");

    protected final IDrawable background;
    protected final IDrawable icon;
    protected int rot;

    @Nullable
    protected SchematicRecipe currentRecipe;
    protected ImmutableList<ItemStack> requisites;

    public SchematicRecipeCategory(IGuiHelper helper) {
        this.background = helper
                .drawableBuilder(TEXTURE, 7, 15, 163, 77)
                .setTextureSize(305, 245)
                .addPadding(0, 49, 0, 0)
                .build();
        this.icon = helper.createDrawableIngredient(ITEM_STACK, new ItemStack(WORKSHOP_ITEM.get()));
    }

    @Override
    public RecipeType<SchematicRecipe> getRecipeType() {
        return NoBullshipJEI.SCHEMATIC_TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SchematicRecipe recipe, IFocusGroup focuses) {
        this.currentRecipe = recipe;
        ImmutableList<Ingredient> shapedIngredients = recipe.getShapedIngredients();
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                if (j + i * 3 >= shapedIngredients.size()) break;
                builder.addSlot(INPUT, 55 + j * 18, 2 + i * 18).addIngredients(shapedIngredients.get(j + i * 3));
            }
        }

        List<ItemStack> shapelessIngredients = getSummedContents(recipe.getShapelessIngredients());

        for (int i = 0; i < 9; ++i) {
            if (i >= shapelessIngredients.size()) break;
            builder.addSlot(INPUT, 1 + i * 18, 60).addItemStack(shapelessIngredients.get(i));
        }

        builder.addSlot(INPUT, 131, 36).addItemStack(SCHEMATIC.get().getDefaultInstance());
        builder.addSlot(OUTPUT, 131, 11).addItemStack(recipe.getResultItem());

        MultiblockRecipe resultRecipe = MultiblockRecipeManager.getInstance().getRecipe(this.currentRecipe.getResultId()).orElse(null);
        if (resultRecipe == null) {
            this.requisites = null;
            return;
        }

        ImmutableList.Builder<ItemStack> requisiteBuilder = ImmutableList.builder();
        if (this.currentRecipe.getRequisites() != null) requisiteBuilder.addAll(this.currentRecipe.getRequisites());
        if (resultRecipe.requisites() != null) //noinspection DataFlowIssue
            requisiteBuilder.addAll(resultRecipe.requisites());

        this.requisites = requisiteBuilder.build();

        int i = 0;
        for (ItemStack item : this.requisites) {
            int slotX = 1 + ((i++ % 9) * 18);
            int slotY = 80 + ((i / 9) * 18);
            builder.addSlot(RENDER_ONLY, slotX, slotY).addItemStack(item);
        }
    }

    @Override
    public void draw(SchematicRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack stack, double mouseX, double mouseY) {
        IRecipeCategory.super.draw(recipe, recipeSlotsView, stack, mouseX, mouseY);
        this.drawInfo(getBackground().getWidth(), getBackground().getHeight(), stack, mouseX, mouseY);
    }

    @Override
    public void drawInfo(int recipeWidth, int recipeHeight, PoseStack stack, double mouseX, double mouseY) {
        if (this.currentRecipe == null) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = MultiblockRecipeManager.getInstance().getEntityForRecipe(this.currentRecipe.getResultId(), level);
        if (entity == null) return;
        Optional<MultiblockRecipe> multiblockRecipe = MultiblockRecipeManager.getInstance().getRecipe(this.currentRecipe.getResultId());
        if (multiblockRecipe.isEmpty()) return;
        AABB entityHitbox = entity.getBoundingBox();
        double longestSide = Math.max(entityHitbox.getXsize(), Math.max(entityHitbox.getYsize(), entityHitbox.getZsize()));
        int scale = (int) (22 / (longestSide / 1.8));

        renderEntity(26, 50, stack, scale, 130 - mouseX, entity);

        String stringForDisplay = multiblockRecipe.get().resultingEntityName() != null ? multiblockRecipe.get().resultingEntityName() : entity.getDisplayName().getString();

        stack.pushPose();
        stack.translate(81, -15, 0);
        GuiComponent.drawCenteredString(stack, Minecraft.getInstance().font, stringForDisplay, 0, 0, 8453920);
        stack.popPose();
    }

    /**
     * This method was taken from
     * <a href="https://github.com/Mrbysco/JustEnoughProfessions/blob/multi/1.20/common/src/main/java/com/mrbysco/justenoughprofessions/RenderHelper.java">
     *     the Just Enough Professions GitHub repo.</a>
     * Credit must be given to <a href="https://github.com/Mrbysco">Mrbysco</a>.
     * As of the time of writing, JEP is MIT licensed.
     */
    public void renderEntity(int x, int y, PoseStack stack, double scale, double yaw, Entity entity) {
        if (this.rot++ > 3599) this.rot = 0;
        stack.pushPose();
        stack.translate((float) x, (float) y, 50f);
        stack.scale((float) scale, (float) scale, (float) scale);
        stack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
        // Rotate entity
        stack.mulPose(Vector3f.YP.rotationDegrees(-((float) this.rot) / 100F * 10.0F));

        entity.setYRot(((float) -this.rot) / 100F * 20.0F);

        if (entity instanceof LivingEntity living) {
            living.yBodyRot = ((float) -this.rot) / 100F * 20.0F;
            living.yHeadRot = entity.getYRot();
            living.yHeadRotO = entity.getYRot();
        }

        stack.translate(0.0F, entity.getMyRidingOffset(), 0.0F);
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.overrideCameraOrientation(new Quaternion(0.0F, 0.0F, 0.0F, 1.0F));
        entityRenderDispatcher.setRenderShadow(false);
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() ->
                entityRenderDispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, stack, bufferSource, 15728880));
        bufferSource.endBatch();
        entityRenderDispatcher.setRenderShadow(true);
        stack.popPose();
    }
}
