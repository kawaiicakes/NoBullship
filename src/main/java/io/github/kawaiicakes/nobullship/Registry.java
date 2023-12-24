package io.github.kawaiicakes.nobullship;

import io.github.kawaiicakes.nobullship.block.WildcardBlock;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlock;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopMenu;
import io.github.kawaiicakes.nobullship.schematic.SchematicItem;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static io.github.kawaiicakes.nobullship.NoBullship.MOD_ID;
import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.Serializer.INSTANCE;
import static net.minecraftforge.registries.ForgeRegistries.*;
import static net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS;

public class Registry {
    private static final DeferredRegister<Block> BLOCK_REGISTRY = DeferredRegister.create(BLOCKS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY =
            DeferredRegister.create(BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(ITEMS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_REGISTRY = DeferredRegister.create(MENU_TYPES, MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_REGISTRY = DeferredRegister.create(SOUND_EVENTS, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_REGISTRY
            = DeferredRegister.create(RECIPE_SERIALIZERS, MOD_ID);

    public static final RegistryObject<Block> WILDCARD_BLOCK
            = BLOCK_REGISTRY.register("wildcard", WildcardBlock::new);
    public static final RegistryObject<Block> WORKSHOP_BLOCK
            = BLOCK_REGISTRY.register("workshop", MultiblockWorkshopBlock::new);
    public static final RegistryObject<BlockEntityType<MultiblockWorkshopBlockEntity>> WORKSHOP_BLOCK_ENTITY
            = BLOCK_ENTITY_REGISTRY.register("workshop", () -> BlockEntityType.Builder.of(MultiblockWorkshopBlockEntity::new, WORKSHOP_BLOCK.get()).build(null));
    public static final RegistryObject<BlockItem> WILDCARD_ITEM
            = ITEM_REGISTRY.register(
            "wildcard",
            () -> new BlockItem(WILDCARD_BLOCK.get(), new Item.Properties().tab(NO_BULLSHIP_TAB)));
    public static final RegistryObject<BlockItem> WORKSHOP_ITEM
            = ITEM_REGISTRY.register(
            "workshop",
            () -> new BlockItem(WORKSHOP_BLOCK.get(), new Item.Properties().tab(NO_BULLSHIP_TAB)));
    public static final RegistryObject<MenuType<MultiblockWorkshopMenu>> WORKSHOP_MENU
            = MENU_REGISTRY.register("workshop_menu", () -> IForgeMenuType.create(MultiblockWorkshopMenu::new));
    public static final RegistryObject<SchematicItem> SCHEMATIC
            = ITEM_REGISTRY.register("schematic", SchematicItem::new);
    public static final RegistryObject<SoundEvent> CONSTRUCT_SUCCESS
            = SOUND_REGISTRY.register("construct_success", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_success")));
    public static final RegistryObject<SoundEvent> CONSTRUCT_FAILED
            = SOUND_REGISTRY.register("construct_failed", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_failed")));
    public static final RegistryObject<SoundEvent> CONSTRUCT_EXPENDED
            = SOUND_REGISTRY.register("construct_expended", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_expended")));
    public static final RegistryObject<SchematicRecipe.Serializer> SCHEMATIC_SERIALIZER
            = RECIPE_SERIALIZER_REGISTRY.register("schematic_workbench", () -> INSTANCE);

    public static void register(IEventBus modEventBus) {
        BLOCK_REGISTRY.register(modEventBus);
        BLOCK_ENTITY_REGISTRY.register(modEventBus);
        ITEM_REGISTRY.register(modEventBus);
        MENU_REGISTRY.register(modEventBus);
        RECIPE_SERIALIZER_REGISTRY.register(modEventBus);
        SOUND_REGISTRY.register(modEventBus);
    }
}
