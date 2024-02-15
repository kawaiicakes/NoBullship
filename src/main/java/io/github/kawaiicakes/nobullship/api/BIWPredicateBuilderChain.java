package io.github.kawaiicakes.nobullship.api;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class allowing "chaining" of multiple {@link BlockInWorldPredicateBuilder}s. This class, as well as the motivation
 * behind making it, was MEGA rushed due to my desire to release v1.3.0 urgently.
 */
public class BIWPredicateBuilderChain implements RawBIWPredicateBuilder<BIWPredicateBuilderChain> {
    private static final Logger LOGGER = LogUtils.getLogger();

    protected final List<BlockInWorldPredicateBuilder> biwPredicateBuilderList = new ArrayList<>();
    protected int cursor = 0;

    protected BIWPredicateBuilderChain(BlockInWorldPredicateBuilder chainStart) {
        this.biwPredicateBuilderList.add(chainStart);
    }

    public static BIWPredicateBuilderChain of(BlockInWorldPredicateBuilder chainStart) {
        return new BIWPredicateBuilderChain(chainStart);
    }

    public BIWPredicateBuilderChain and(BlockInWorldPredicateBuilder addition) {
        this.biwPredicateBuilderList.add(addition);
        this.cursor++;
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireProperties(Property<?> property, Set<Comparable<?>> value) {
        this.biwPredicateBuilderList.get(this.cursor).requireProperties(property, value);
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireProperties(String property, Set<String> values) {
        this.biwPredicateBuilderList.get(this.cursor).requireProperties(property, values);
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireProperty(Property<?> property, Comparable<?> value) {
        this.biwPredicateBuilderList.get(this.cursor).requireProperty(property, value);
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireProperty(String property, String value) {
        this.biwPredicateBuilderList.get(this.cursor).requireProperty(property, value);
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireStrictNbt(CompoundTag tag) {
        this.biwPredicateBuilderList.get(this.cursor).requireStrictNbt(tag);
        return this;
    }

    @Override
    public BIWPredicateBuilderChain requireNbt(CompoundTag tag) {
        this.biwPredicateBuilderList.get(this.cursor).requireNbt(tag);
        return this;
    }

    @Override
    public CompoundTag getNaiveNbt() {
        CompoundTag toReturn = new CompoundTag();
        for (BlockInWorldPredicateBuilder builder : this.biwPredicateBuilderList) {
            if (builder.getBlockEntityNbtData() != null) toReturn.merge(builder.getBlockEntityNbtData());
            if (builder.getBlockEntityNbtDataStrict() != null) toReturn.merge(builder.getBlockEntityNbtDataStrict());
        }
        return toReturn;
    }

    @Override
    public BlockInWorldPredicate build() {
        BlockInWorldPredicate toReturn = null;
        for (BlockInWorldPredicateBuilder builder : this.biwPredicateBuilderList) {
            if (toReturn == null) {
                toReturn = builder.build();
                continue;
            }

            toReturn = toReturn.or(builder.build());
        }
        return toReturn;
    }

    @Override
    public @Nullable JsonObject toJson() throws RuntimeException {
        return null;
    }

    @Override
    public @Nullable CompoundTag toNbt() {
        return null;
    }

    @Override
    public Set<BlockState> getValidBlockstates() {
        return null;
    }

    @Override
    public ItemStack getItemized() {
        return null;
    }

    @Override
    public boolean requiresNbt() {
        for (BlockInWorldPredicateBuilder builder : this.biwPredicateBuilderList) {
            if (builder.requiresNbt()) return true;
        }
        return false;
    }
}
