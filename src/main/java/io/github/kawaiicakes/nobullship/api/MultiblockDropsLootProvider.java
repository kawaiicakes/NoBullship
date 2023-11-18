package io.github.kawaiicakes.nobullship.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.EntityLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.kawaiicakes.nobullship.Config.DROP_RAW_PERCENT;

public class MultiblockDropsLootProvider extends LootTableProvider {
    protected final Map<ResourceLocation, NonNullList<ItemStack>> blocksForDrop;

    public MultiblockDropsLootProvider(DataGenerator pGenerator, Map<ResourceLocation, NonNullList<ItemStack>> blocksForDrop) {
        super(pGenerator);
        this.blocksForDrop = blocksForDrop;
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return ImmutableList.of(Pair.of(MultiblockEntityDrops::new, LootContextParamSets.ENTITY));
    }

    public class MultiblockEntityDrops extends EntityLoot {
        protected final ConstantValue rollsValue;
        protected final Map<ResourceLocation, LootTable.Builder> map = new HashMap<>();

        public MultiblockEntityDrops() {
            this.rollsValue = ConstantValue.exactly(DROP_RAW_PERCENT.get().floatValue());
        }

        @Override
        protected void addTables() {
            for (Map.Entry<ResourceLocation, NonNullList<ItemStack>> entry : MultiblockDropsLootProvider.this.blocksForDrop.entrySet()) {
                // this.add(entry.getKey(), LootTable.lootTable().withPool(LootPool.lootPool().setRolls(rollsValue).add(LootItem.lootTableItem())));
            }
        }

        @Override
        public void accept(BiConsumer<ResourceLocation, LootTable.Builder> consumer) {
            this.addTables();
            Set<ResourceLocation> set = Sets.newHashSet();

            for (Map.Entry<ResourceLocation, NonNullList<ItemStack>> entry : MultiblockDropsLootProvider.this.blocksForDrop.entrySet()) {
                ResourceLocation key = entry.getKey();

                if (key != BuiltInLootTables.EMPTY && set.add(key)) {
                    LootTable.Builder loottable$builder = this.map.remove(key);
                    if (loottable$builder == null) {
                        throw new IllegalStateException(String.format(Locale.ROOT, "Missing loot table for '%s'", key));
                    }

                    consumer.accept(key, loottable$builder);
                }
            }

            this.map.forEach(consumer);
        }
    }
}
