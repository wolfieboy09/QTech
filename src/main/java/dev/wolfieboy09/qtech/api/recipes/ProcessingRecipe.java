package dev.wolfieboy09.qtech.api.recipes;

import com.google.common.base.Joiner;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.gas.crafting.SizedGasIngredient;
import dev.wolfieboy09.qtech.api.recipes.result.FluidStackChanceResult;
import dev.wolfieboy09.qtech.api.recipes.result.GasStackChanceResult;
import dev.wolfieboy09.qtech.api.recipes.result.ItemStackChanceResult;
import dev.wolfieboy09.qtech.api.registry.gas.GasStack;
import dev.wolfieboy09.qtech.api.util.TriEither;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NothingNullByDefault
public abstract class ProcessingRecipe<I extends RecipeInput, P extends ProcessingRecipeParams> implements Recipe<I> {
    protected P params;
    protected NonNullList<SizedIngredient> ingredients;
    protected NonNullList<SizedFluidIngredient> fluidIngredients;
    protected NonNullList<SizedGasIngredient> gasIngredients;

    protected NonNullList<ItemStackChanceResult> itemResults;
    protected NonNullList<FluidStackChanceResult> fluidResults;
    protected NonNullList<GasStackChanceResult> gasResults;

    protected int energyCost;
    protected int processingDuration;
    protected CleanroomCondition requiredCleanRoom;

    private final RecipeType<?> type;
    private final RecipeSerializer<?> serializer;
    private final IRecipeTypeInfo typeInfo;

    public ProcessingRecipe(IRecipeTypeInfo typeInfo, P params) {
        this.params = params;
        this.ingredients = params.ingredients;
        this.fluidIngredients = params.fluidIngredients;
        this.gasIngredients = params.gasIngredients;

        this.itemResults = params.itemResults;
        this.fluidResults = params.fluidResults;
        this.gasResults = params.gasResults;

        this.energyCost = params.energyCost;
        this.processingDuration = params.processingDuration;
        this.requiredCleanRoom = params.requiredCleanRoom;
        this.type = typeInfo.getType();
        this.serializer = typeInfo.getSerializer();
        this.typeInfo = typeInfo;
    }

    protected abstract ProcessingRecipeConstrains getRecipeConstrains();

    protected RecipeIgnorance getRecipeIgnorance() {
        return RecipeIgnorance.createBlank();
    }

    public P getParams() {
        return this.params;
    }

    /**
     * Use {@link ProcessingRecipe#getItemIngredients()} to get a {@link NonNullList} of {@link SizedIngredient} instead
     */
    @Override
    @Deprecated
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> stuff = NonNullList.create();
        this.ingredients.forEach(e -> stuff.add(e.ingredient()));
        return stuff;
    }

    public NonNullList<SizedIngredient> getItemIngredients() {
        return this.ingredients;
    }

    public NonNullList<SizedFluidIngredient> getFluidIngredients() {
        return fluidIngredients;
    }

    public NonNullList<SizedGasIngredient> getGasIngredients() {
        return gasIngredients;
    }

    public int getEnergyCost() {
        return energyCost;
    }

    public int getProcessingDuration() {
        return processingDuration;
    }

    public CleanroomCondition getRequiredCleanRoom() {
        return requiredCleanRoom;
    }

    @Override
    public ItemStack assemble(I input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    public List<ItemStackChanceResult> getRollableResults() {
        return itemResults;
    }

    public List<FluidStackChanceResult> getRollableFluidResults() {
        return fluidResults;
    }

    public List<GasStackChanceResult> getRollableGasResults() {
        return gasResults;
    }

    public List<ItemStack> rollResults() {
        return getRollableResults().stream()
                .map(ItemStackChanceResult::getIfRolled)
                .filter(Objects::nonNull)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<FluidStack> rollFluidResults() {
        return getRollableFluidResults().stream()
                .map(FluidStackChanceResult::getIfRolled)
                .filter(Objects::nonNull)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<GasStack> rollGasResults() {
        return getRollableGasResults().stream()
                .map(GasStackChanceResult::getIfRolled)
                .filter(Objects::nonNull)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return getRollableResults().isEmpty() ? ItemStack.EMPTY : getRollableResults().getFirst().getResult();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String getGroup() {
        return "processing";
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    public IRecipeTypeInfo getTypeInfo() {
        return typeInfo;
    }

    @Override
    public RecipeType<?> getType() {
        return this.type;
    }

    /**
     * @return A {@link List} of {@link TriEither} containing one of three ingredient types: {@link SizedIngredient}, {@link SizedFluidIngredient}, or {@link SizedGasIngredient}
     */
    public List<TriEither<SizedIngredient, SizedFluidIngredient, SizedGasIngredient>> getCombinedIngredients() {
        List<TriEither<SizedIngredient, SizedFluidIngredient, SizedGasIngredient>> combined = new ArrayList<>();
        this.ingredients.forEach((i) -> combined.add(TriEither.left(i)));
        this.fluidIngredients.forEach((i) -> combined.add(TriEither.middle(i)));
        this.gasIngredients.forEach((i) -> combined.add(TriEither.right(i)));
        return combined;
    }

    @OverridingMethodsMustInvokeSuper
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        ProcessingRecipeConstrains constrains = getRecipeConstrains();

        int ingredientCount = ingredients.size();
        int outputCount = itemResults.size();

        if (ingredientCount > constrains.maxItemInputs()) {
            errors.add("Recipe has more item inputs (" + ingredientCount + ") than supported ("
                    + constrains.maxItemOutputs() + ").");
        }

        if (outputCount > constrains.maxItemOutputs()) {
                errors.add("Recipe has more item outputs (" + outputCount + ") than supported ("
                        + constrains.maxItemOutputs() + ").");
        }

        ingredientCount = fluidIngredients.size();
        outputCount = fluidResults.size();

        if (ingredientCount > constrains.maxFluidInputs())
            errors.add("Recipe has more fluid inputs (" + ingredientCount + ") than supported ("
                    + constrains.maxFluidInputs() + ").");

        if (outputCount > constrains.maxFluidOutputs())
            errors.add("Recipe has more fluid outputs (" + outputCount + ") than supported ("
                    + constrains.maxFluidOutputs() + ").");

        ingredientCount = gasIngredients.size();
        outputCount = gasResults.size();

        if (ingredientCount > constrains.maxFluidInputs())
            errors.add("Recipe has more fluid inputs (" + ingredientCount + ") than supported ("
                    + constrains.maxGasInputs() + ").");

        if (outputCount > constrains.maxFluidOutputs())
            errors.add("Recipe has more fluid outputs (" + outputCount + ") than supported ("
                    + constrains.maxGasOutputs() + ").");

        return errors;
    }


    public static <P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>> MapCodec<R> codec(
            Factory<P, R> factory, MapCodec<P> paramsCodec
    ) {
        return paramsCodec.xmap(factory::create, recipe -> recipe.getParams())
                .validate(recipe -> {
                    var errors = recipe.validate();
                    if (errors.isEmpty())
                        return DataResult.success(recipe);
                    errors.add(recipe.getClass().getSimpleName() + " failed validation:");
                    return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
                });
    }

    public static <P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>> StreamCodec<RegistryFriendlyByteBuf, R> streamCodec(
            Factory<P, R> factory, StreamCodec<RegistryFriendlyByteBuf, P> streamCodec
    ) {
        return streamCodec.map(factory::create, ProcessingRecipe::getParams);
    }

    @FunctionalInterface
    public interface Factory<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>> {
        R create(P params);
    }
}
