package io.github.kawaiicakes.nobullship.network;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.Config;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ClientboundUpdateNoBullshipPacket {
    public static Logger LOGGER = LogUtils.getLogger();
    public Map<ResourceLocation, MultiblockRecipe> recipes;
    public List<ResourceLocation> whiteList;
    public List<ResourceLocation> blackList;
    public int globalCooldownTime;
    public int maxGlobalCooldownTime;

    public ClientboundUpdateNoBullshipPacket(MultiblockRecipeManager manager) {
        this.recipes = manager.getRecipes();
        this.globalCooldownTime = manager.getGlobalCooldownTime();
        this.maxGlobalCooldownTime = manager.getMaxGlobalCooldownTime();

        this.whiteList = Config.DROP_WHITELIST.get().stream().map(ResourceLocation::new).toList();
        this.blackList = Config.DROP_BLACKLIST.get().stream().map(ResourceLocation::new).toList();
    }

    public ClientboundUpdateNoBullshipPacket(FriendlyByteBuf buffer) {
        this.globalCooldownTime = buffer.readInt();
        this.maxGlobalCooldownTime = buffer.readInt();
        CompoundTag recipeTag = buffer.readAnySizeNbt();

        if (recipeTag == null) throw new IllegalArgumentException("Network does not contain recipes!");

        Map<ResourceLocation, MultiblockRecipe> newRecipes = new HashMap<>(recipeTag.size());
        for (String key : recipeTag.getAllKeys()) {
            Tag valueAt = recipeTag.get(key);
            if (!(valueAt instanceof CompoundTag nbt)) {
                LOGGER.error("Recipe '{}' could not be sent to the client!", key);
                continue;
            }

            MultiblockRecipe fromNbt;
            try {
                fromNbt = MultiblockRecipe.fromNbt(nbt);
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error parsing recipe {} from NBT!", key);
                LOGGER.error("Error: ", e);
                continue;
            }

            if (fromNbt == null) {
                LOGGER.error("Error parsing recipe {} from NBT!", key);
                continue;
            }

            newRecipes.put(new ResourceLocation(key), fromNbt);
        }
        this.recipes = newRecipes;
        this.whiteList = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
        this.blackList = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        CompoundTag serialized = new CompoundTag();
        for (Map.Entry<ResourceLocation, MultiblockRecipe> entry : this.recipes.entrySet()) {
            CompoundTag valueTag = entry.getValue().toNbt();
            if (valueTag == null) {
                LOGGER.error("Error parsing recipe {} due to malformed syntax!", entry.getKey());
                continue;
            }
            serialized.put(entry.getKey().toString(), valueTag);
        }

        buffer.writeInt(this.globalCooldownTime);
        buffer.writeInt(this.maxGlobalCooldownTime);
        buffer.writeNbt(serialized);
        buffer.writeCollection(this.whiteList, FriendlyByteBuf::writeResourceLocation);
        buffer.writeCollection(this.blackList, FriendlyByteBuf::writeResourceLocation);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        if (Minecraft.getInstance().player == null) return;
        MultiblockRecipeManager.getInstance().replaceRecipes(this);
        LOGGER.info("Successfully received {} No Bullship! recipes from the server!", this.recipes.size());
    }
}
