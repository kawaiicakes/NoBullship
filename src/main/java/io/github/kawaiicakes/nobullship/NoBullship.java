package io.github.kawaiicakes.nobullship;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kawaiicakes.nobullship.api.*;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipeProvider;
import io.github.kawaiicakes.nobullship.api.schematic.SchematicRecipeProvider;
import io.github.kawaiicakes.nobullship.event.BullshipEvents;
import io.github.kawaiicakes.nobullship.multiblock.SchematicRenderer;
import io.github.kawaiicakes.nobullship.multiblock.block.MultiblockWorkshopBlockEntity;
import io.github.kawaiicakes.nobullship.multiblock.screen.EmptyScreen;
import io.github.kawaiicakes.nobullship.multiblock.screen.MultiblockWorkshopScreen;
import io.github.kawaiicakes.nobullship.multiblock.screen.SaveSelectionScreen;
import io.github.kawaiicakes.nobullship.network.ClientboundUpdateNoBullshipPacket;
import io.github.kawaiicakes.nobullship.network.NoBullshipPackets;
import io.github.kawaiicakes.nobullship.particle.ItemMarker;
import io.github.kawaiicakes.nobullship.schematic.SchematicItem;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.util.Lazy;
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
import org.lwjgl.glfw.GLFW;

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
        MinecraftForge.EVENT_BUS.register(BullshipEvents.class);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onDatagen);

        Registry.register(modEventBus);

        ModLoadingContext.get().registerConfig(COMMON, CONFIG);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        NoBullshipPackets.register();
    }

    @SubscribeEvent
    public void onDatagen(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        BlockTagsProvider blockTagsProvider = new NoBullshipBlockTags(generator, MOD_ID, fileHelper);

        generator.addProvider(
                event.includeServer(),
                new MultiblockRecipeProvider(generator)
        );

        generator.addProvider(
                event.includeServer(),
                new SchematicRecipeProvider(generator)
        );

        generator.addProvider(
                event.includeServer(),
                new NoBullshipRecipeProvider(generator)
        );

        generator.addProvider(
                event.includeServer(),
                new NoBullshipItemTags(generator, blockTagsProvider, MOD_ID, fileHelper)
        );

        generator.addProvider(
                event.includeServer(),
                blockTagsProvider
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
        MultiblockRecipeManager multiblockRecipeManager = new MultiblockRecipeManager(event.getConditionContext());
        event.addListener(new NbtMultiblockResourceListener(multiblockRecipeManager));
        event.addListener(multiblockRecipeManager);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientLevelTick(TickEvent.LevelTickEvent event) {
            if (!event.level.isClientSide || !event.phase.equals(TickEvent.Phase.START)) return;
            SchematicRenderer.BlockIngredient.tickRandomSeedChange();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            while (ClientSetup.OPEN_SELECTION_SCREEN.get().consumeClick()) {
                if (Minecraft.getInstance().player == null) continue;
                ItemStack inHand = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
                Minecraft.getInstance().setScreen(new SaveSelectionScreen(inHand));
            }
        }

        @SubscribeEvent
        public static void onLevelRender(RenderLevelStageEvent event) {
            if (Minecraft.getInstance().player == null) return;
            if (Minecraft.getInstance().level == null) return;
            if (!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)) return;

            ItemStack itemInHand = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!itemInHand.is(MAGIC_WAND_ITEM.get())) return;

            CompoundTag itemTag = itemInHand.getOrCreateTag();
            if (!itemTag.contains("pos1") || !itemTag.contains("pos2")) return;

            int[] pos1Arr = itemTag.getIntArray("pos1");
            int[] pos2Arr = itemTag.getIntArray("pos2");

            BlockPos minPos = new BlockPos(Math.min(pos1Arr[0], pos2Arr[0]) - 1, Math.min(pos1Arr[1], pos2Arr[1]) - 1, Math.min(pos1Arr[2], pos2Arr[2]) - 1);
            BlockPos maxPos = new BlockPos(Math.max(pos1Arr[0], pos2Arr[0]), Math.max(pos1Arr[1], pos2Arr[1]), Math.max(pos1Arr[2], pos2Arr[2]));

            Vec3i enclosingSize = maxPos.subtract(minPos);

            double d0 = 1.0D;
            double d2 = enclosingSize.getX();
            double d3 = enclosingSize.getZ();
            double d4 = d2 < 0.0D ? d0 * 2 : d0;
            double d6 = d3 < 0.0D ? d0 * 2 : d0;
            double d7 = d4 + d2;
            double d8 = d0 + (double) enclosingSize.getY();
            double d9 = d6 + d3;

            PoseStack poseStack = event.getPoseStack();
            LevelRenderer levelRenderer = event.getLevelRenderer();

            Camera camera = event.getCamera();
            Vec3 cameraPos = camera.getPosition();

            VertexConsumer consumer = levelRenderer.renderBuffers.bufferSource().getBuffer(RenderType.lines());

            poseStack.pushPose();
            poseStack.translate((double) minPos.getX() - cameraPos.x(), (double) minPos.getY() - cameraPos.y(), (double) minPos.getZ() - cameraPos.z());
            LevelRenderer.renderLineBox(poseStack, consumer, d4, 1.0D, d6, d7, d8, d9, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
            poseStack.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientSetup {
        public static net.minecraftforge.common.util.Lazy<KeyMapping> OPEN_SELECTION_SCREEN = Lazy.of(() -> new KeyMapping(
                "gui.nobullship.open_selection",
                new IKeyConflictContext() {
                    @Override
                    public boolean isActive() {
                        if (Minecraft.getInstance().player == null) return false;
                        if (Minecraft.getInstance().screen != null) return false;
                        return Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND).is(MAGIC_WAND_ITEM.get());
                    }

                    @Override
                    public boolean conflicts(IKeyConflictContext other) {
                        return this.isActive();
                    }
                },
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.no_bullship"
        ));

        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(WORKSHOP_BLOCK_ENTITY.get(), SchematicRenderer::new);
        }

        @SubscribeEvent
        public static void registerMenuType(FMLClientSetupEvent event) {
            MenuScreens.register(WORKSHOP_MENU.get(), MultiblockWorkshopScreen::new);
            MenuScreens.register(EMPTY_MENU.get(), EmptyScreen::new);
        }

        @SubscribeEvent
        public static void registerParticleFactory(RegisterParticleProvidersEvent event) {
            event.register(ITEM_MARKER_PARTICLE.get(), new ItemMarker.Provider());
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SELECTION_SCREEN.get());
        }
    }
}
