package dev.wolfieboy09.qtech.block.multiblock.centrifuge;

import dev.wolfieboy09.qtech.api.capabilities.gas.GasTank;
import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandler;
import dev.wolfieboy09.qtech.api.fluids.ExtendedFluidTank;
import dev.wolfieboy09.qtech.api.items.ExtendedItemStackHandler;
import dev.wolfieboy09.qtech.api.multiblock.blocks.controller.BaseMultiblockControllerEntity;
import dev.wolfieboy09.qtech.api.recipes.data.centrifuge.CentrifugeRecipe;
import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.api.registry.gas.GasStack;
import dev.wolfieboy09.qtech.registries.QTBlockEntities;
import dev.wolfieboy09.qtech.registries.QTMultiblockTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CentrifugeBlockEntityController extends BaseMultiblockControllerEntity {
    private final Component TITLE = Component.translatable("block.qtech.centrifuge_controller");
    private int progress = 0;
    private int crafting_ticks = 0;
    private int energy_required = 0;
    private CentrifugeRecipe recipe = null;
    private boolean isValidRecipe = false;

    private static final int INPUT_FLUID_TANKS_COUNT = 1;
    private static final int INPUT_GAS_TANKS_COUNT = 1;
    public static final int TANK_CAPACITY = 10000;

    // Tanks need to contain [regId, amount]
    // Plus 1 for progress amount
    private final ContainerData containerData = new SimpleContainerData((INPUT_FLUID_TANKS_COUNT * 2) + (INPUT_GAS_TANKS_COUNT * 2) + 1);
    private final ExtendedItemStackHandler inventory = new ExtendedItemStackHandler(5, this::setChanged);
    private final ExtendedFluidTank inputFluidTank = new ExtendedFluidTank(TANK_CAPACITY, this::setChanged);
    private final GasTank inputGasTank = new GasTank(TANK_CAPACITY, this::setChanged);

    public CentrifugeBlockEntityController(BlockPos pos, BlockState blockState) {
        super(QTBlockEntities.CENTRIFUGE_CONTROLLER.get(), QTMultiblockTypes.CENTRIFUGE.get(), pos, blockState);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateContainerData();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void updateContainerData() {
        FluidStack fluid = this.inputFluidTank.getFluid();
        int regId = BuiltInRegistries.FLUID.getId(fluid.getFluid());
        int amount = fluid.getAmount();
        this.containerData.set(0, regId);
        this.containerData.set(1, amount);

        GasStack gas = this.inputGasTank.getGas();
        regId = QTRegistries.GAS.getId(gas.getGas());
        amount = gas.getAmount();
        this.containerData.set(2, regId);
        this.containerData.set(3, amount);

        this.containerData.set(4, this.getProgress());
    }

    private int getProgress() {
        return this.recipe == null ? 0 : (int) (this.crafting_ticks / (float) this.recipe.getProcessingDuration() * 100);
    }

    public void tick() {

    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag toMerge = new CompoundTag();
        toMerge.putInt("CraftingTicks", this.crafting_ticks);
        return super.saveWithoutMetadata(registries).merge(toMerge);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.crafting_ticks = tag.getInt("CraftingTicks");
    }

    public IFluidHandler getInputFluidTank() {
        return this.inputFluidTank;
    }

    public IGasHandler getInputGasTank() {
        return this.inputGasTank;
    }
}
