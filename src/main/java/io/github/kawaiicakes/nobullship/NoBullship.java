package io.github.kawaiicakes.nobullship;

import io.github.kawaiicakes.nobullship.api.MultiblockRecipeManager;
import io.github.kawaiicakes.nobullship.api.NoBullshipBlockTags;
import io.github.kawaiicakes.nobullship.block.NoBullshipRecipeProvider;
import io.github.kawaiicakes.nobullship.multiblock.SchematicRenderer;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen;
import io.github.kawaiicakes.nobullship.network.ClientboundUpdateNoBullshipPacket;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.particle.ItemMarker;
import io.github.kawaiicakes.nobullship.schematic.SchematicItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.data.DataGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
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
import org.jetbrains.annotations.NotNull;

import static io.github.kawaiicakes.nobullship.Config.CONFIG;
import static io.github.kawaiicakes.nobullship.Registry.*;
import static net.minecraft.world.level.Level.OVERWORLD;
import static net.minecraftforge.fml.config.ModConfig.Type.COMMON;

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

    public NoBullship() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(SchematicItem.class);
        MinecraftForge.EVENT_BUS.register(MultiblockWorkshopBlockEntity.class);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onDatagen);
        modEventBus.addListener(this::registerParticleFactory);

        Registry.register(modEventBus);

        ModLoadingContext.get().registerConfig(COMMON, CONFIG);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        NoBullshipPackets.register();
    }

    @SubscribeEvent
    public void registerParticleFactory(RegisterParticleProvidersEvent event) {
        event.register(ITEM_MARKER_PARTICLE.get(), new ItemMarker.Provider());
    }

    @SubscribeEvent
    public void onDatagen(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();

        generator.addProvider(
                event.includeServer(),
                new NoBullshipRecipeProvider(generator)
        );

        generator.addProvider(
                event.includeServer(),
                new NoBullshipBlockTags(generator, MOD_ID, fileHelper)
        );
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != OVERWORLD) return;
        if (event.phase != TickEvent.Phase.START) return;

        MultiblockRecipeManager.getInstance().decrementGlobalCooldown();
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        NoBullshipPackets.sendToPlayer(new ClientboundUpdateNoBullshipPacket(MultiblockRecipeManager.getInstance()), event.getPlayer());
    }

    @SubscribeEvent
    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(MultiblockRecipeManager.getInstance());
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientLevelTick(TickEvent.LevelTickEvent event) {
            if (!event.level.isClientSide || !event.phase.equals(TickEvent.Phase.START)) return;
            SchematicRenderer.BlockIngredient.tickRandomSeedChange();
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientSetup {
        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(WORKSHOP_BLOCK_ENTITY.get(), SchematicRenderer::new);
        }

        @SubscribeEvent
        public static void registerMenuType(FMLClientSetupEvent event) {
            MenuScreens.register(WORKSHOP_MENU.get(), MultiblockWorkshopScreen::new);
        }
    }
}
