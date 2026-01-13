package dev.wolfieboy09.qtech.api.recipes.data.disk_assembler;

import com.mojang.serialization.MapCodec;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipe;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipeBuilder;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipeConstrains;
import dev.wolfieboy09.qtech.block.disk_assembler.DiskAssemblerBlockEntity;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;

@NothingNullByDefault
public class DiskAssemblerRecipe extends ProcessingRecipe<RecipeWrapper, DiskAssemblerRecipeParams> {

    public DiskAssemblerRecipe(DiskAssemblerRecipeParams params) {
        super(QTRecipeTypes.DISK_ASSEMBLY, params);
    }

    public NonNullList<Ingredient> getExtras() {
        return params.extras;
    }

    @Override
    protected ProcessingRecipeConstrains getRecipeConstrains() {
        return ProcessingRecipeConstrains.builder()
                .maxItemIO(4, 1)
                .build();
    }

    //TODO Be able to extends ProcessingRecipeConstraints
    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        int extraIngredients = getExtras().size();
        if (extraIngredients > 4) {
            errors.add("Recipe has more extra item inputs (" + extraIngredients + ") than supported (4).");
        }

        return errors;
    }

    @Override
    public boolean matches(RecipeWrapper input, Level level) {
        boolean extrasMatch = !getExtras().isEmpty();
        boolean ingredientsMatch = !getItemIngredients().isEmpty();
        for (Ingredient extra : getExtras()) {
            if (!extra.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.EXTRA_SLOT_1))
                    && !extra.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.EXTRA_SLOT_2))
                    && !extra.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.EXTRA_SLOT_3))
                    && !extra.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.EXTRA_SLOT_4))) {
                extrasMatch = false;
            }
        }

        for (SizedIngredient main : getItemIngredients()) {
            if (!main.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.MAIN_SLOT_1))
                    && !main.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.MAIN_SLOT_2))
                    && !main.test(input.getItem(DiskAssemblerBlockEntity.DiskAssemblerSlot.MAIN_SLOT_3))) {
                ingredientsMatch = false;
            }
        }

        return extrasMatch && ingredientsMatch;
    }

    @FunctionalInterface
    public interface Factory<R extends DiskAssemblerRecipe>
            extends ProcessingRecipe.Factory<DiskAssemblerRecipeParams, R> {
        R create(DiskAssemblerRecipeParams params);
    }

    public static class Builder<R extends DiskAssemblerRecipe>
            extends ProcessingRecipeBuilder<DiskAssemblerRecipeParams, R, Builder<R>> {

        public Builder(Factory<R> factory, ResourceLocation recipeId) {
            super(factory, recipeId);
        }

        @Override
        protected DiskAssemblerRecipeParams createParams() {
            return new DiskAssemblerRecipeParams();
        }

        @Override
        public Builder<R> self() {
            return this;
        }

        public Builder<R> withExtras(Ingredient... extras) {
            params.extras = NonNullList.of(Ingredient.EMPTY, extras);
            return self();
        }

        public Builder<R> requireExtra(ItemLike itemLike) {
            params.extras.add(Ingredient.of(itemLike));
            return self();
        }

        public Builder<R> requireExtra(TagKey<Item> itemTag) {
            params.extras.add(Ingredient.of(itemTag));
            return self();
        }
    }

    public static class Serializer<R extends DiskAssemblerRecipe>
            implements RecipeSerializer<R> {

        private final MapCodec<R> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

        public Serializer(ProcessingRecipe.Factory<DiskAssemblerRecipeParams, R> factory) {
            this.codec = ProcessingRecipe.codec(factory, DiskAssemblerRecipeParams.CODEC);
            this.streamCodec = ProcessingRecipe.streamCodec(factory, DiskAssemblerRecipeParams.STREAM_CODEC);
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