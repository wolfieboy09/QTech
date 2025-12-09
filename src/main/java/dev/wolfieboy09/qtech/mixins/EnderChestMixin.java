package dev.wolfieboy09.qtech.mixins;

import dev.wolfieboy09.qtech.registries.QTDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

// The very first mixin...
@Mixin(EnderChestBlock.class)
public abstract class EnderChestMixin extends AbstractChestBlock<EnderChestBlockEntity> implements SimpleWaterloggedBlock {
    protected EnderChestMixin(Properties properties, Supplier<BlockEntityType<? extends EnderChestBlockEntity>> blockEntityType) {
        super(properties, blockEntityType);
    }

    // We bouta rob people from their stuff
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void disableEnderChest(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.dimension() == QTDimensions.NULLZONE) {
            player.displayClientMessage(Component.translatable("qtech.ender_chest_fail").withStyle(ChatFormatting.RED), true);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
