package io.github.kawaiicakes.nobullship.multiblock.block;

import com.mojang.logging.LogUtils;
import io.github.kawaiicakes.nobullship.multiblock.screen.ProxyMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ProxyContainer<T extends BlockEntity> extends BaseContainerBlockEntity {
    protected static final Logger LOGGER = LogUtils.getLogger();
    protected final T originalContainer;
    protected final AbstractContainerMenu menu;

    public ProxyContainer(T originalContainer, BlockPos pPos, BlockState pBlockState, int containerId) {
        super(originalContainer.getType(), pPos, pBlockState);
        this.originalContainer = originalContainer;

        if (this.originalContainer instanceof MenuProvider menuProvider) {
            final Level originalLevel = Objects.requireNonNull(this.originalContainer).getLevel();
            assert Minecraft.getInstance().level != null;
            Objects.requireNonNull(this.originalContainer).setLevel(Minecraft.getInstance().level);
            if (Minecraft.getInstance().player == null) {
                assert originalLevel != null;
                Objects.requireNonNull(this.originalContainer).setLevel(originalLevel);
                throw new IllegalArgumentException();
            }
            Inventory pInventory = Minecraft.getInstance().player.getInventory();
            AbstractContainerMenu menu;
            try {
                menu = menuProvider.createMenu(containerId, pInventory, pInventory.player);
                if (menu == null) menu = new ProxyMenu(containerId, pInventory, new FriendlyByteBuf(Unpooled.buffer()));
            } catch (RuntimeException e) {
                LOGGER.error("Error attempting to deduce menu type!", e);
                assert originalLevel != null;
                Objects.requireNonNull(this.originalContainer).setLevel(originalLevel);
                menu = new ProxyMenu(containerId, pInventory, new FriendlyByteBuf(Unpooled.buffer()));
            }
            this.menu = menu;
        } else {
            assert Minecraft.getInstance().player != null;
            Inventory pInventory = Minecraft.getInstance().player.getInventory();
            final Level originalLevel = Objects.requireNonNull(this.originalContainer).getLevel();
            assert originalLevel != null;
            Objects.requireNonNull(this.originalContainer).setLevel(originalLevel);
            this.menu = new ProxyMenu(containerId, pInventory, new FriendlyByteBuf(Unpooled.buffer()));
        }
    }

    @Override
    protected Component getDefaultName() {
        return this.originalContainer instanceof Nameable nameable ?
                nameable.getDisplayName() :
                Component.translatable("gui.nobullship.empty_block_entity");
    }

    public MenuType<?> getMenuType() {
        return this.menu.getType();
    }

    @Override
    protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
        return this.menu;
    }

    @Override
    public int getContainerSize() {
        return this.originalContainer instanceof BaseContainerBlockEntity base ?
                base.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        if (this.originalContainer instanceof Container container) return container.isEmpty();
        return true;
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {}

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    @Override
    public void clearContent() {}
}
