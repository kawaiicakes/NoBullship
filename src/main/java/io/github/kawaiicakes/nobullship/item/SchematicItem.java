package io.github.kawaiicakes.nobullship.item;

import io.github.kawaiicakes.nobullship.datagen.MultiblockRecipeManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;

import javax.annotation.ParametersAreNonnullByDefault;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;

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

        if (!(nbt.getString("product").isEmpty())) return this.getDescriptionId() + ".filled";

        return this.getDescriptionId();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return false;
    }

    // FIXME: account for claim mods as this might be able to bypass them
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (!(pContext.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;
        BlockPos pos = pContext.getClickedPos();

        CompoundTag nbt = pContext.getItemInHand().getTag();
        //if (nbt == null) return InteractionResult.FAIL;

        BlockPattern pattern
                = MultiblockRecipeManager.getInstance().checkPattern(new ResourceLocation("ballsmungus"));

        if (pattern == null) return InteractionResult.FAIL;

        // FIXME: if the blockstate of one of the blocks changes as this matches, that block will not be removed.
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match != null) {
            for (int i = 0; i < pattern.getWidth(); ++i) {
                for (int j = 0; j < pattern.getHeight(); ++j) {
                    BlockInWorld blockinworld = match.getBlock(i, j, 0);
                    level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            level.getLevel().getServer().getPlayerList().broadcastSystemMessage(Component.literal("AMBATAKUM! \uD83D\uDE2B\uD83D\uDE2B\uD83D\uDCA6\uD83D\uDCA6\uD83D\uDCA6\uD83D\uDCA6"), true);
            level.explode(null, pos.getX(), pos.getY(), pos.getZ(), 3.2F, true, Explosion.BlockInteraction.DESTROY);
        }

        return InteractionResult.FAIL;
    }
}
