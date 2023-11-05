package io.github.kawaiicakes.nobullship;

import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlock;
import io.github.kawaiicakes.nobullship.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.datagen.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.datagen.MultiblockRecipeProvider;
import io.github.kawaiicakes.nobullship.item.SchematicItem;
import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopMenu;
import io.github.kawaiicakes.nobullship.screen.MultiblockWorkshopScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

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

    public NoBullship()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::gatherData);

        BLOCK_REGISTRY.register(modEventBus);
        BLOCK_ENTITY_REGISTRY.register(modEventBus);
        ITEM_REGISTRY.register(modEventBus);
        MENU_REGISTRY.register(modEventBus);
    }

    // TODO: THIS SHOULD ONLY BE FOR DEBUG
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (!(event.getItemStack().is(ITEMS.getValue(new ResourceLocation("stick"))))) return;

        BlockPattern creeperPattern
                = MultiblockRecipeManager.getInstance().checkPattern(new ResourceLocation("ballsmungus"));

        if (creeperPattern == null) return;


        // FIXME: if the blockstate of one of the blocks changes as this matches, that block will not be removed.
        BlockPattern.BlockPatternMatch match = creeperPattern.find(level, event.getPos());
        if (match != null) {
            for (int i = 0; i < creeperPattern.getWidth(); ++i) {
                for (int j = 0; j < creeperPattern.getHeight(); ++j) {
                    BlockInWorld blockinworld = match.getBlock(i, j, 0);
                    level.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            level.getLevel().getServer().getPlayerList().broadcastSystemMessage(Component.literal("AMBATAKUM! \uD83D\uDE2B\uD83D\uDE2B\uD83D\uDCA6\uD83D\uDCA6\uD83D\uDCA6\uD83D\uDCA6"), true);
            level.explode(null, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), 3.2F, true, Explosion.BlockInteraction.DESTROY);
        }
    }

    @SubscribeEvent
    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeServer(),
                new MultiblockRecipeProvider(event.getGenerator())
        );
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
