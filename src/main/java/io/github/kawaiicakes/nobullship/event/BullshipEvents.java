package io.github.kawaiicakes.nobullship.event;

import io.github.kawaiicakes.nobullship.Config;
import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BullshipEvents {
    // As some entities have hard coded drops, this is a guaranteed way to prevent certain items from being dropped.
    @SubscribeEvent
    public static void onItemCreation(EntityJoinLevelEvent event) {
        if (!Config.DISABLE_DROP.get()) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        ResourceLocation resultLocation = new ResourceLocation(itemEntity.getItem().getItem().toString());
        if (!MultiblockRecipeManager.getInstance().isBlacklistedResult(resultLocation)) return;
        event.setCanceled(true);
    }
}
