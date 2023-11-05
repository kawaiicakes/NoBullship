package io.github.kawaiicakes.nobullship.item;

import com.mojang.datafixers.util.Pair;
import io.github.kawaiicakes.nobullship.datagen.MultiblockRecipeManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.ParametersAreNonnullByDefault;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;
import static net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES;

/**
 * Care is taken to ensure that using the schematic does not alter the blockstate of the clicked block.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SchematicItem extends Item {
    public SchematicItem() {
        super(new Properties().tab(NO_BULLSHIP_TAB));
    }

    @Override
    public String getDescriptionId(ItemStack pStack) {
        CompoundTag nbt = pStack.getTag();
        if (nbt == null) return this.getDescriptionId();

        if (!(nbt.getString("nobullshipRecipe").isEmpty())) return this.getDescriptionId() + ".filled";

        return this.getDescriptionId();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        if (pStack.getTag() == null) return false;
        return !(pStack.getTag().getString("nobullshipRecipe").isEmpty());
    }

    // So long as a claim mod sets $useItem to DENY in RightClickBlock event, this will not bypass claim mods.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext pContext) {
        // TODO: configurable options for cooldown time, whether fake players can use this item...
        // Cooldown is added so spamming this isn't possible. BlockPattern#find is an expensive call.
        if (pContext.getPlayer() == null) return InteractionResult.FAIL;
        if (pContext.getPlayer().getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;

        pContext.getPlayer().getCooldowns().addCooldown(this, 20);

        if (!(pContext.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;
        if (pContext.getHitResult().getType() != HitResult.Type.BLOCK) return InteractionResult.FAIL;

        CompoundTag nbt = stack.getTag();
        if (nbt == null) return InteractionResult.FAIL;
        if (nbt.getString("nobullshipRecipe").isEmpty()) return InteractionResult.FAIL;

        Pair<BlockPattern, ResourceLocation> recipePair
                = MultiblockRecipeManager.getInstance().getRecipePair(new ResourceLocation(nbt.getString("nobullshipRecipe")));
        if (recipePair == null) return InteractionResult.FAIL;

        BlockPattern pattern = recipePair.getFirst();
        ResourceLocation resultLocation = recipePair.getSecond();
        if (pattern == null || resultLocation == null) return InteractionResult.FAIL;

        BlockPos pos = pContext.getClickedPos();

        // FIXME: if the blockstate of one of the blocks changes as this matches, that block will not be removed.
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) return InteractionResult.FAIL;

        for (int i = 0; i < pattern.getWidth(); ++i) {
            for (int j = 0; j < pattern.getHeight(); ++j) {
                BlockInWorld blockinworld = match.getBlock(i, j, 0);
                level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        Entity entity = RegistryObject.create(resultLocation, ENTITY_TYPES).get().create(level);
        if (entity == null) return InteractionResult.FAIL;

        BlockPos blockpos = match.getBlock(1, 2, 0).getPos();
        entity.moveTo((double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.55D, (double)blockpos.getZ() + 0.5D, match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F, 0.0F);
        entity.setYRot(match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F);
        level.addFreshEntity(entity);

        return InteractionResult.FAIL;
    }

    /*
        Overriding the methods below, strictly speaking, is not necessary given that #onItemUseFirst will fire first
        and then pass a fail. If for some reason the PlayerInteractEvent is overridden such that $useItem is set to
        ALLOW, these methods will ensure that still nothing will happen.
     */

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
