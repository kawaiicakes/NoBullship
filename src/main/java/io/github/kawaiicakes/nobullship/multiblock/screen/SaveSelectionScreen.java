package io.github.kawaiicakes.nobullship.multiblock.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockPatternBuilder;
import io.github.kawaiicakes.nobullship.api.multiblock.MultiblockRecipe;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static io.github.kawaiicakes.nobullship.multiblock.screen.RequisiteScreen.CLOSE_MSG;

public class SaveSelectionScreen extends Screen {
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final Component NAME_LABEL = Component.translatable("structure_block.structure_name");
    public static final Component NEED_POS = Component.translatable("gui.nobullship.empty_pos");
    public static final Component SAVE_ERROR = Component.translatable("gui.nobullship.save_error");
    public static final Component BAD_NAME = Component.translatable("gui.nobullship.bad_name");
    public static final Component JSON_HOVER = Component.translatable("gui.nobullship.save_json");
    public static final Component JSON_MSG = Component.translatable("gui.nobullship.json");
    public static final Component NBT_HOVER = Component.translatable("gui.nobullship.save_nbt");
    public static final Component NBT_MSG = Component.translatable("gui.nobullship.nbt");
    public static final Component SAVE_SUCCESS = Component.translatable("gui.nobullship.save_success");
    public static final Path SCHEMATICS_DIR = FMLPaths.GAMEDIR.relative().resolve("bullship_schematics");

    @Nullable
    protected final BlockPos pos1;
    @Nullable
    protected final BlockPos pos2;
    protected EditBox selectionName;
    protected Button saveJson;
    protected Button saveNbt;
    protected Button closeButton;
    protected boolean shouldDisplayResultMsg = false;
    protected Component resultMsg = NEED_POS;

    public SaveSelectionScreen(ItemStack wandItem) {
        super(Component.translatable("gui.nobullship.schematic_saver"));
        CompoundTag wandTag = wandItem.getOrCreateTag();

        int[] pos1Temp = null;
        int[] pos2Temp = null;

        if (wandTag.contains("pos1", Tag.TAG_INT_ARRAY)) {
            int[] pos1Arr = wandTag.getIntArray("pos1");
            pos1Temp = new int[]{pos1Arr[0], pos1Arr[1], pos1Arr[2]};
        }
        if (wandTag.contains("pos2", Tag.TAG_INT_ARRAY)) {
            int[] pos2Arr = wandTag.getIntArray("pos2");
            pos2Temp = new int[]{pos2Arr[0], pos2Arr[1], pos2Arr[2]};
        }

        if (pos1Temp != null && pos2Temp != null) {
            this.pos1 = new BlockPos(Math.min(pos1Temp[0], pos2Temp[0]), Math.min(pos1Temp[1], pos2Temp[1]), Math.min(pos1Temp[2], pos2Temp[2]));
            this.pos2 = new BlockPos(Math.max(pos1Temp[0], pos2Temp[0]), Math.max(pos1Temp[1], pos2Temp[1]), Math.max(pos1Temp[2], pos2Temp[2]));
        } else {
            this.pos1 = null;
            this.pos2 = null;
        }

        this.font = Minecraft.getInstance().font;
    }

    @Override
    protected void init() {
        assert Minecraft.getInstance().player != null;

        this.selectionName = this.addWidget(new EditBox(this.font, this.width / 2 - 152, 40, 300, 20, Component.translatable("structure_block.structure_name")) {
            public boolean charTyped(char pCodePoint, int pModifiers) {
                return SaveSelectionScreen.this.isValidCharacterForName(this.getValue(), pCodePoint, this.getCursorPosition()) && super.charTyped(pCodePoint, pModifiers);
            }
        });
        this.selectionName.setMaxLength(128);

        int buttonWidth = 300;
        this.saveJson = this.addRenderableWidget(new Button(
                (this.width - buttonWidth) / 2,
                this.height / 2,
                buttonWidth,
                20,
                JSON_MSG,
                (button) -> {
                    this.shouldDisplayResultMsg = true;
                    this.resultMsg = NEED_POS;
                    if (this.saveAsJson()) {
                        Minecraft.getInstance().setScreen(null);
                        Minecraft.getInstance().player.sendSystemMessage(SAVE_SUCCESS);
                    }
                },
                ((pButton, pPoseStack, pMouseX, pMouseY) -> this.renderTooltip(pPoseStack, JSON_HOVER, pMouseX, pMouseY))
        ));
        this.saveNbt = this.addRenderableWidget(new Button(
                (this.width - buttonWidth) / 2,
                (this.height + 48) / 2,
                buttonWidth,
                20,
                NBT_MSG,
                (button) -> {
                    this.shouldDisplayResultMsg = true;
                    this.resultMsg = NEED_POS;
                    if (this.saveAsNbt()) {
                        Minecraft.getInstance().setScreen(null);
                        Minecraft.getInstance().player.sendSystemMessage(SAVE_SUCCESS);
                    }
                },
                ((pButton, pPoseStack, pMouseX, pMouseY) -> this.renderTooltip(pPoseStack, NBT_HOVER, pMouseX, pMouseY))
        ));
        this.closeButton = this.addRenderableWidget(new Button(
                (this.width - 150) / 2,
                (this.height + 144) / 2,
                150,
                20,
                CLOSE_MSG,
                (button) -> Minecraft.getInstance().setScreen(null)
        ));
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.selectionName.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.selectionName.setValue(s);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 10, 16777215);
        drawString(pPoseStack, this.font, NAME_LABEL, this.width / 2 - 153, 30, 10526880);
        if (this.shouldDisplayResultMsg)
            drawCenteredString(pPoseStack, this.font, this.resultMsg, this.width / 2, (this.height / 2) - 24, 16736352);
        this.selectionName.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void tick() {
        this.selectionName.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean saveAsNbt() {
        if (this.pos1 == null || this.pos2 == null) {
            this.resultMsg = NEED_POS;
            return false;
        }
        if (Minecraft.getInstance().level == null) {
            this.resultMsg = NEED_POS;
            return false;
        }

        ResourceLocation schematicNameAndResult;

        try {
            schematicNameAndResult = new ResourceLocation(this.selectionName.getValue());
            if (schematicNameAndResult.getPath().contains("//"))
                throw new ResourceLocationException("Invalid resource path: " + schematicNameAndResult);
        } catch (RuntimeException e) {
            LOGGER.error("Invalid name for schematic!", e);
            this.resultMsg = BAD_NAME;
            return false;
        }

        Path nbtFileParentPath = SCHEMATICS_DIR.resolve(schematicNameAndResult.getNamespace());

        String nbtFileName = schematicNameAndResult.getPath() + ".nbt";

        Path nbtFile;

        try {
            if (schematicNameAndResult.getPath().endsWith(".nbt") || schematicNameAndResult.getPath().isEmpty())
                throw new InvalidPathException(nbtFileName, "empty resource name");
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid name for schematic!", e);
            this.resultMsg = BAD_NAME;
            return false;
        }

        try {
            nbtFile = FileUtil.createPathToResource(nbtFileParentPath, schematicNameAndResult.getPath(), ".nbt");

            if (!nbtFile.startsWith(nbtFileParentPath) || !FileUtil.isPathNormalized(nbtFile) || !FileUtil.isPathPortable(nbtFile))
                throw new ResourceLocationException("Invalid resource path: " + nbtFile);
        } catch (InvalidPathException invalidpathexception) {
            LOGGER.error("Invalid resource path: " + schematicNameAndResult.getPath(),invalidpathexception);
            this.resultMsg = BAD_NAME;
            return false;
        }

        // more thorough check here probably isn't necessary since the path points to the gamedir;
        // how tf would someone manage to get in this far to run this code if the game's files haven't been generated yet?
        try {
            Files.createDirectories(Files.exists(nbtFileParentPath) ? nbtFileParentPath.toRealPath() : nbtFileParentPath);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create parent directory: {}", nbtFileParentPath);
            this.resultMsg = SAVE_ERROR;
            return false;
        }

        List<StructureTemplate.StructureBlockInfo> nbtBlocks = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> normalBlocks = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> specialBlocks = Lists.newArrayList();

        for (BlockPos enclosed : BlockPos.betweenClosed(this.pos1, this.pos2)) {
            BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(enclosed);
            BlockPos relativeEnclosed = enclosed.subtract(this.pos1);
            BlockState enclosedState = Minecraft.getInstance().level.getBlockState(enclosed);
            StructureTemplate.StructureBlockInfo blockInfo;

            if (blockEntity != null) {
                blockInfo = new StructureTemplate.StructureBlockInfo(relativeEnclosed, enclosedState, blockEntity.saveWithId());
            } else {
                blockInfo = new StructureTemplate.StructureBlockInfo(relativeEnclosed, enclosedState, null);
            }

            // check is necessary; $nbt is actually nullable
            //noinspection ConstantValue
            if (blockInfo.nbt != null) {
                nbtBlocks.add(blockInfo);
            } else if (!blockInfo.state.getBlock().hasDynamicShape() && blockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                normalBlocks.add(blockInfo);
            } else {
                specialBlocks.add(blockInfo);
            }
        }

        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                        (info) -> info.pos.getY())
                .thenComparingInt((info) -> info.pos.getX())
                .thenComparingInt((info) -> info.pos.getZ());

        //noinspection RedundantOperationOnEmptyContainer
        normalBlocks.sort(comparator);
        //noinspection RedundantOperationOnEmptyContainer
        specialBlocks.sort(comparator);
        nbtBlocks.sort(comparator);

        List<StructureTemplate.StructureBlockInfo> infoList = Lists.newArrayList();
        infoList.addAll(normalBlocks);
        infoList.addAll(specialBlocks);
        infoList.addAll(nbtBlocks);

        StructureTemplate.Palette palette = new StructureTemplate.Palette(infoList);
        StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();

        CompoundTag serializedSchematic = new CompoundTag();

        ListTag blockListTag = new ListTag();
        List<StructureTemplate.StructureBlockInfo> blockList = palette.blocks();

        for (StructureTemplate.StructureBlockInfo blockInfo : blockList) {
            CompoundTag blockData = new CompoundTag();
            blockData.put("pos", this.newIntegerList(blockInfo.pos.getX(), blockInfo.pos.getY(), blockInfo.pos.getZ()));
            int k = simplePalette.idFor(blockInfo.state);
            blockData.putInt("state", k);
            //noinspection ConstantValue
            if (blockInfo.nbt != null) {
                blockData.put("nbt", blockInfo.nbt);
            }

            blockListTag.add(blockData);
        }

        serializedSchematic.put("blocks", blockListTag);

        ListTag paletteListTag = new ListTag();

        for (BlockState blockstate : simplePalette) {
            paletteListTag.add(NbtUtils.writeBlockState(blockstate));
        }

        serializedSchematic.put("palette", paletteListTag);

        final Vec3i selectionSize = this.selectionSize();

        // these are down here to try and give at least *some* functionality with vanilla if for some reason in the future
        // someone tries loading a recipe into the actual world.
        serializedSchematic.put("entities", new ListTag());
        serializedSchematic.put("size", this.newIntegerList(selectionSize.getX(), selectionSize.getY(), selectionSize.getZ()));
        serializedSchematic.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

        try {
            OutputStream outputstream = new FileOutputStream(nbtFile.toFile());

            try {
                NbtIo.writeCompressed(serializedSchematic, outputstream);
            } catch (Throwable e) {
                try {
                    outputstream.close();
                } catch (Throwable throwable) {
                    e.addSuppressed(throwable);
                }

                throw e;
            }

            outputstream.close();
            return true;
        } catch (Throwable e2) {
            LOGGER.error("Error while saving!", e2);
            this.resultMsg = SAVE_ERROR;
            return false;
        }
    }

    public boolean saveAsJson() {
        if (this.pos1 == null || this.pos2 == null) {
            this.resultMsg = NEED_POS;
            return false;
        }
        if (Minecraft.getInstance().level == null) {
            this.resultMsg = NEED_POS;
            return false;
        }

        ResourceLocation schematicNameAndResult;

        try {
            schematicNameAndResult = new ResourceLocation(this.selectionName.getValue());
            if (schematicNameAndResult.getPath().contains("//"))
                throw new ResourceLocationException("Invalid resource path: " + schematicNameAndResult);
        } catch (RuntimeException e) {
            LOGGER.error("Invalid name for schematic!", e);
            this.resultMsg = BAD_NAME;
            return false;
        }

        Path jsonFileParentPath = SCHEMATICS_DIR.resolve(schematicNameAndResult.getNamespace());

        String jsonFileName = schematicNameAndResult.getPath() + ".json";

        Path jsonFilePath;

        try {
            if (schematicNameAndResult.getPath().endsWith(".json") || schematicNameAndResult.getPath().isEmpty())
                throw new InvalidPathException(jsonFileName, "empty resource name");
        } catch (InvalidPathException e) {
            LOGGER.error("Invalid name for schematic!", e);
            this.resultMsg = BAD_NAME;
            return false;
        }

        try {
            jsonFilePath = FileUtil.createPathToResource(jsonFileParentPath, schematicNameAndResult.getPath(), ".json");

            if (!jsonFilePath.startsWith(jsonFileParentPath) || !FileUtil.isPathNormalized(jsonFilePath) || !FileUtil.isPathPortable(jsonFilePath))
                throw new ResourceLocationException("Invalid resource path: " + jsonFilePath);
        } catch (InvalidPathException invalidpathexception) {
            LOGGER.error("Invalid resource path: " + schematicNameAndResult.getPath(),invalidpathexception);
            this.resultMsg = BAD_NAME;
            return false;
        }

        try {
            Files.createDirectories(Files.exists(jsonFileParentPath) ? jsonFileParentPath.toRealPath() : jsonFileParentPath);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create parent directory: {}", jsonFileParentPath);
            this.resultMsg = SAVE_ERROR;
            return false;
        }

        List<StructureTemplate.StructureBlockInfo> nbtBlocks = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> normalBlocks = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> specialBlocks = Lists.newArrayList();

        for (BlockPos enclosed : BlockPos.betweenClosed(this.pos1, this.pos2)) {
            BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(enclosed);
            BlockPos relativeEnclosed = enclosed.subtract(this.pos1);
            BlockState enclosedState = Minecraft.getInstance().level.getBlockState(enclosed);
            StructureTemplate.StructureBlockInfo blockInfo;

            if (blockEntity != null) {
                blockInfo = new StructureTemplate.StructureBlockInfo(relativeEnclosed, enclosedState, blockEntity.saveWithId());
            } else {
                blockInfo = new StructureTemplate.StructureBlockInfo(relativeEnclosed, enclosedState, null);
            }

            //noinspection ConstantValue
            if (blockInfo.nbt != null) {
                nbtBlocks.add(blockInfo);
            } else if (!blockInfo.state.getBlock().hasDynamicShape() && blockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                normalBlocks.add(blockInfo);
            } else {
                specialBlocks.add(blockInfo);
            }
        }

        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                        (info) -> info.pos.getY())
                .thenComparingInt((info) -> info.pos.getX())
                .thenComparingInt((info) -> info.pos.getZ());

        //noinspection RedundantOperationOnEmptyContainer
        normalBlocks.sort(comparator);
        //noinspection RedundantOperationOnEmptyContainer
        specialBlocks.sort(comparator);
        nbtBlocks.sort(comparator);

        List<StructureTemplate.StructureBlockInfo> infoList = Lists.newArrayList();
        infoList.addAll(normalBlocks);
        infoList.addAll(specialBlocks);
        infoList.addAll(nbtBlocks);

        StructureTemplate.Palette palette = new StructureTemplate.Palette(infoList);
        StructureTemplate.SimplePalette simplePalette = new StructureTemplate.SimplePalette();

        CompoundTag serializedSchematic = new CompoundTag();

        ListTag blockListTag = new ListTag();
        List<StructureTemplate.StructureBlockInfo> blockList = palette.blocks();

        for (StructureTemplate.StructureBlockInfo blockInfo : blockList) {
            CompoundTag blockData = new CompoundTag();
            blockData.put("pos", this.newIntegerList(blockInfo.pos.getX(), blockInfo.pos.getY(), blockInfo.pos.getZ()));
            int k = simplePalette.idFor(blockInfo.state);
            blockData.putInt("state", k);
            //noinspection ConstantValue
            if (blockInfo.nbt != null) {
                blockData.put("nbt", blockInfo.nbt);
            }

            blockListTag.add(blockData);
        }

        serializedSchematic.put("blocks", blockListTag);

        ListTag paletteListTag = new ListTag();

        for (BlockState blockstate : simplePalette) {
            paletteListTag.add(NbtUtils.writeBlockState(blockstate));
        }

        serializedSchematic.put("palette", paletteListTag);

        final Vec3i selectionSize = this.selectionSize();
        serializedSchematic.put("size", this.newIntegerList(selectionSize.getX(), selectionSize.getY(), selectionSize.getZ()));

        MultiblockPatternBuilder builder = MultiblockRecipe.fromRawNbt(serializedSchematic, schematicNameAndResult.toString());

        try {
            OutputStream outputstream = new FileOutputStream(jsonFilePath.toFile());

            try {
                // FIXME
                // NbtIo.writeCompressed(builder, outputstream);
            } catch (Throwable e) {
                try {
                    outputstream.close();
                } catch (Throwable throwable) {
                    e.addSuppressed(throwable);
                }

                throw e;
            }

            outputstream.close();
            return true;
        } catch (Throwable e2) {
            LOGGER.error("Error while saving!", e2);
            this.resultMsg = SAVE_ERROR;
            return false;
        }
    }

    public Vec3i selectionSize() {
        if (this.pos1 == null || this.pos2 == null) throw new RuntimeException();

        return new Vec3i(
                this.pos2.getX() - this.pos1.getX() + 1,
                this.pos2.getY() - this.pos1.getY() + 1,
                this.pos2.getZ() - this.pos1.getZ() + 1
        );
    }

    protected ListTag newIntegerList(int... pValues) {
        ListTag toReturn = new ListTag();

        for (int i : pValues) {
            toReturn.add(IntTag.valueOf(i));
        }

        return toReturn;
    }
}
