package io.github.kawaiicakes.nobullship.api;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface RawBIWPredicateBuilder<T extends RawBIWPredicateBuilder<T>> {
    T requireProperties(Property<?> property, Set<Comparable<?>> value);

    T requireProperties(String property, Set<String> values);

    T requireProperty(Property<?> property, Comparable<?> value);

    T requireProperty(String property, String value);

    T requireStrictNbt(CompoundTag tag);

    T requireNbt(CompoundTag tag);

    CompoundTag getNaiveNbt();

    BlockInWorldPredicate build();

    @Nullable JsonObject toJson() throws RuntimeException;

    @Nullable CompoundTag toNbt();

    Set<BlockState> getValidBlockstates();

    ItemStack getItemized();
    boolean requiresNbt();
}
