package io.github.kawaiicakes.nobullship.multiblock;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static io.github.kawaiicakes.nobullship.NoBullship.NO_BULLSHIP_TAB;
import static io.github.kawaiicakes.nobullship.Registry.LIGMA_SOUND;

public class MagicWandItem extends Item {
    public static final Component INSTRUCTIONS1 = Component.translatable("tooltip.nobullship.magic_wand1").withStyle(ChatFormatting.AQUA);
    public static final Component INSTRUCTIONS2 = Component.translatable("tooltip.nobullship.magic_wand2").withStyle(ChatFormatting.AQUA);
    public static final Component INSTRUCTIONS3 = Component.translatable("tooltip.nobullship.magic_wand3").withStyle(ChatFormatting.AQUA);
    public static final Component TOOLTIP = Component.translatable("tooltip.nobullship.magic_wand4").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD);

    protected final Multimap<Attribute, AttributeModifier> modifiers;

    public MagicWandItem() {
        super(new Properties().tab(NO_BULLSHIP_TAB).rarity(Rarity.EPIC));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", Integer.MAX_VALUE, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", 3.0, AttributeModifier.Operation.ADDITION));
        this.modifiers = builder.build();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return !pStack.getOrCreateTag().isEmpty();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();

        if (context.isSecondaryUseActive()) {
            stack.setTag(new CompoundTag());
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.clear_pos"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getOrCreateTag().getIntArray("pos1").length < 3) {
            stack.addTagElement("pos1", new IntArrayTag(new int[]{clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()}));
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.pos1", clickedPos.toShortString()), true);
            }
        } else {
            stack.addTagElement("pos2", new IntArrayTag(new int[]{clickedPos.getX(), clickedPos.getY(), clickedPos.getZ()}));
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("chat.nobullship.pos2", clickedPos.toShortString()), true);
            }
        }

        return super.onItemUseFirst(stack, context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(INSTRUCTIONS1);
        pTooltipComponents.add(INSTRUCTIONS2);
        pTooltipComponents.add(INSTRUCTIONS3);
        pTooltipComponents.add(Component.empty());
        pTooltipComponents.add(TOOLTIP);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pSlot) {
        return pSlot == EquipmentSlot.MAINHAND ? this.modifiers : super.getDefaultAttributeModifiers(pSlot);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity pTarget) {
        DamageSource ligma = (new EntityDamageSource("ligma", player)).bypassArmor().bypassInvul().bypassMagic().bypassEnchantments().setMagic();
        Vec3 targetPos = pTarget.position();

        if (!(pTarget.level instanceof ServerLevel level)) {
            pTarget.hurt(ligma, Float.MAX_VALUE);
            this.playLigmaSound(pTarget.level, player, targetPos);
            return false;
        }
        CompoundTag lightningTag = new CompoundTag();
        lightningTag.putString("id", "minecraft:lightning_bolt");
        Entity lightning = EntityType.loadEntityRecursive(lightningTag, level, (entity) -> {
            entity.moveTo(targetPos.x, targetPos.y, targetPos.z, entity.getYRot(), entity.getXRot());
            return entity;
        });

        if (lightning == null) {
            pTarget.hurt(ligma, Float.MAX_VALUE);
            this.playLigmaSound(level, player, targetPos);
            return false;
        }

        level.tryAddFreshEntityWithPassengers(lightning);
        pTarget.hurt(ligma, Float.MAX_VALUE);
        this.playLigmaSound(level, player, targetPos);
        return false;
    }

    public void playLigmaSound(Level level, Player attacker, Vec3 targetPos) {
        level.playSound(attacker, targetPos.x, targetPos.y, targetPos.z, LIGMA_SOUND.get(), SoundSource.MASTER, 1.0F, 1.0F);
    }
}
