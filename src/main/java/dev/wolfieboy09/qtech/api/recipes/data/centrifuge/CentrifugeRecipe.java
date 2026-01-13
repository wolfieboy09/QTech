package dev.wolfieboy09.qtech.api.recipes.data.centrifuge;

import com.mojang.serialization.MapCodec;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipe;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipeBuilder;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipeConstrains;
import dev.wolfieboy09.qtech.api.recipes.inputs.CombinedRecipeInput;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.List;

@NothingNullByDefault
public class CentrifugeRecipe extends ProcessingRecipe<CombinedRecipeInput, CentrifugeRecipeParams> {
    public CentrifugeRecipe(CentrifugeRecipeParams params) {
        super(QTRecipeTypes.CENTRIFUGE, params);
    }

    public NonNullList<Ingredient> getParts() {
        return params.parts;
    }

    @Override
    protected ProcessingRecipeConstrains getRecipeConstrains() {
        return ProcessingRecipeConstrains.builder()
                .maxItemIO(1, 4)
                .maxFluidIO(1, 4)
                .maxGasIO(1, 4)
                .build();
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        int recordedSize = getParts().size();
        if (recordedSize > 3) {
            errors.add("Recipe has more parts (" + recordedSize + ") than supported (3).");
        }

        recordedSize = this.ingredients.size() + this.fluidIngredients.size() + this.gasIngredients.size();

        if (recordedSize > 2) {
            errors.add("Recipe can only have a maximum of (2) total inputs despite (" + recordedSize + ") ingredients given.");
        }

        return errors;
    }

    @Override
    public boolean matches(CombinedRecipeInput input, Level level) {
        if (getItemIngredients().isEmpty() || getFluidIngredients().isEmpty() || getGasIngredients().isEmpty()) {
            return false;
        }

        return this.getCombinedIngredients().stream().allMatch(eitherIngredient ->
                eitherIngredient.map(
                        input::matchItem,
                        input::matchFluid,
                        input::matchGas
                    )
                );
    }

    @FunctionalInterface
    public interface Factory<R extends CentrifugeRecipe>
            extends ProcessingRecipe.Factory<CentrifugeRecipeParams, R> {
        R create(CentrifugeRecipeParams params);
    }

    public static class Builder<R extends CentrifugeRecipe>
            extends ProcessingRecipeBuilder<CentrifugeRecipeParams, R, Builder<R>> {

        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected CentrifugeRecipeParams createParams() {
            return new CentrifugeRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }

        public Builder<R> withParts(Ingredient... parts) {
            params.parts = NonNullList.of(Ingredient.EMPTY, parts);
            return self();
        }

        public Builder<R> requirePart(ItemLike itemLike) {
            params.parts.add(Ingredient.of(itemLike));
            return self();
        }

        public Builder<R> requirePart(TagKey<Item> itemTag) {
            params.parts.add(Ingredient.of(itemTag));
            return self();
        }
    }

    public static class Serializer<R extends CentrifugeRecipe> implements RecipeSerializer<R> {
        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(ProcessingRecipe.Factory<CentrifugeRecipeParams, R> factory) {
            this.codec = ProcessingRecipe.codec(factory, CentrifugeRecipeParams.CODEC);
            this.streamCodec = ProcessingRecipe.streamCodec(factory, CentrifugeRecipeParams.STREAM_CODEC);
        }

        @Override
        public MapCodec<R> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return streamCodec;
        }
    }
}
