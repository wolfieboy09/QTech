package dev.wolfieboy09.qtech.api.recipes.inputs;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.capabilities.gas.IGasHandler;
import dev.wolfieboy09.qtech.api.gas.crafting.SizedGasIngredient;
import dev.wolfieboy09.qtech.api.registry.gas.GasStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A recipe handler to handle recipes that use a {@link IItemHandler}, {@link IFluidHandler} and {@link IGasHandler}
 * @param fluidHandler The {@link IFluidHandler}
 * @param itemHandler The {@link IItemHandler}
 * @param gasHandler The {@link IGasHandler}
 */
@NothingNullByDefault
public record CombinedRecipeInput(
        @Nullable
        IFluidHandler fluidHandler,
        @Nullable
        IItemHandler itemHandler,
        @Nullable
        IGasHandler gasHandler) implements RecipeInput {

    @Override
    public ItemStack getItem(int i) {
        return this.itemHandler == null ? ItemStack.EMPTY : this.itemHandler.getStackInSlot(i);
    }

    public FluidStack getFluid(int i) {
        return this.fluidHandler == null ? FluidStack.EMPTY : this.fluidHandler.getFluidInTank(i);
    }

    public GasStack getGas(int i) {
        return this.gasHandler == null ? GasStack.EMPTY : this.gasHandler.getGasInTank(i);
    }

    @Override
    public int size() {
        if (this.itemHandler == null) return 0;
        return this.itemHandler.getSlots();
    }

    public boolean matchItem(SizedIngredient ingredient) {
        if (this.itemHandler == null) return false;
        boolean match = false;
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (ingredient.test(stack)) {
                match = true;
            }
        }
        return match;
    }

    public boolean matchItem(Ingredient ingredient) {
        if (this.itemHandler == null) return false;
        boolean match = false;
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (ingredient.test(stack)) {
                match = true;
            }
        }
        return match;
    }

    public boolean matchFluid(SizedFluidIngredient fluidIngredient) {
        if (this.fluidHandler == null) return false;
        boolean match = false;
        for (int i = 0; i < this.fluidHandler.getTanks(); i++) {
            var stack = this.fluidHandler.getFluidInTank(i);
            if (fluidIngredient.test(stack)) {
                match = true;
            }
        }
        return match;
    }

    public boolean matchGas(SizedGasIngredient gasIngredient) {
        if (this.gasHandler == null) return false;
        boolean match = false;
        for (int i = 0; i < this.gasHandler.getTanks(); i++) {
            var stack = this.gasHandler.getGasInTank(i);
            if (gasIngredient.test(stack)) {
                match = true;
            }
        }
        return match;
    }
}
