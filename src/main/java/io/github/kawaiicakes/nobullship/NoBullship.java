package io.github.kawaiicakes.nobullship;

import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipeProvider;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlock;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopMenu;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen;
import io.github.kawaiicakes.nobullship.network.ClientboundUpdateNoBullshipPacket;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.schematic.SchematicItem;
import io.github.kawaiicakes.nobullship.schematic.SchematicRecipe;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.nobullship.Config.CONFIG;
import static io.github.kawaiicakes.nobullship.schematic.SchematicRecipe.Serializer.INSTANCE;
import static net.minecraft.world.level.Level.OVERWORLD;
import static net.minecraftforge.fml.config.ModConfig.Type.COMMON;
import static net.minecraftforge.registries.ForgeRegistries.*;

@Mod(NoBullship.MOD_ID)
public class NoBullship
{
    public static final String MOD_ID = "nobullship";

    public static final CreativeModeTab NO_BULLSHIP_TAB = new CreativeModeTab("nobullship_tab") {
        @Override
        public @NotNull ItemStack makeIcon() {
            return SCHEMATIC.get().getDefaultInstance();
        }
    };

    private static final DeferredRegister<Block> BLOCK_REGISTRY = DeferredRegister.create(BLOCKS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY =
            DeferredRegister.create(BLOCK_ENTITY_TYPES, MOD_ID);
    private static final DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(ITEMS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENU_REGISTRY = DeferredRegister.create(MENU_TYPES, MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUND_REGISTRY = DeferredRegister.create(SOUND_EVENTS, MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER_REGISTRY
            = DeferredRegister.create(RECIPE_SERIALIZERS, MOD_ID);

    public static final RegistryObject<Block> WORKSHOP_BLOCK
            = BLOCK_REGISTRY.register("workshop", () -> new MultiblockWorkshopBlock(BlockBehaviour.Properties.of(Material.STONE)));
    public static final RegistryObject<BlockEntityType<MultiblockWorkshopBlockEntity>> WORKSHOP_BLOCK_ENTITY
            = BLOCK_ENTITY_REGISTRY.register("workshop", () -> BlockEntityType.Builder.of(MultiblockWorkshopBlockEntity::new, WORKSHOP_BLOCK.get()).build(null));
    public static final RegistryObject<BlockItem> WORKSHOP_ITEM
            = ITEM_REGISTRY.register(
                    "workshop",
            () -> new BlockItem(WORKSHOP_BLOCK.get(), new Item.Properties().tab(NO_BULLSHIP_TAB)));
    public static final RegistryObject<MenuType<MultiblockWorkshopMenu>> WORKSHOP_MENU
            = MENU_REGISTRY.register("workshop_menu", () -> IForgeMenuType.create(MultiblockWorkshopMenu::new));

    public static final RegistryObject<SchematicItem> SCHEMATIC = ITEM_REGISTRY.register("schematic", SchematicItem::new);
    public static final RegistryObject<SoundEvent> CONSTRUCT_SUCCESS
            = SOUND_REGISTRY.register("construct_success", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_success")));
    public static final RegistryObject<SoundEvent> CONSTRUCT_FAILED
            = SOUND_REGISTRY.register("construct_failed", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_failed")));
    public static final RegistryObject<SoundEvent> CONSTRUCT_EXPENDED
            = SOUND_REGISTRY.register("construct_expended", () -> new SoundEvent(new ResourceLocation(MOD_ID, "construct_expended")));

    public static final RegistryObject<SchematicRecipe.Serializer> SCHEMATIC_SERIALIZER = RECIPE_SERIALIZER_REGISTRY.register("schematic_workbench", () -> INSTANCE);

    public NoBullship()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SchematicItem.class);

        modEventBus.addListener(this::gatherData);

        BLOCK_REGISTRY.register(modEventBus);
        BLOCK_ENTITY_REGISTRY.register(modEventBus);
        ITEM_REGISTRY.register(modEventBus);
        MENU_REGISTRY.register(modEventBus);
        SOUND_REGISTRY.register(modEventBus);
        RECIPE_SERIALIZER_REGISTRY.register(modEventBus);

        ModLoadingContext.get().registerConfig(COMMON, CONFIG);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        NoBullshipPackets.register();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != OVERWORLD) return;

        MultiblockRecipeManager.getInstance().decrementGlobalCooldown();
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        NoBullshipPackets.sendToPlayer(new ClientboundUpdateNoBullshipPacket(MultiblockRecipeManager.getInstance()), event.getPlayer());
    }

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeServer(),
                new MultiblockRecipeProvider(event.getGenerator())
        );

        /*
        commented out because these aren't strictly needed atm and are causing problems
        Map<ResourceLocation, NonNullList<ItemStack>> blocksForDrops = MultiblockRecipeManager.getInstance().getBlockItemsForRecipes();

        event.getGenerator().addProvider(
                event.includeServer(),
                new SchematicRecipeProvider(event.getGenerator(), blocksForDrops)
        );

        event.getGenerator().addProvider(
                event.includeServer(),
                new MultiblockDropsLootProvider(event.getGenerator(), blocksForDrops)
        );
        */
    }

    @SubscribeEvent
    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(MultiblockRecipeManager.getInstance());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientSetup {
        @SubscribeEvent
        public static void registerMenuType(FMLClientSetupEvent event) {
            MenuScreens.register(WORKSHOP_MENU.get(), MultiblockWorkshopScreen::new);
        }
    }
}
