package dev.wolfieboy09.qtech.api.datagen.builder;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.datagen.framework.ProcessingRecipeGen;
import dev.wolfieboy09.qtech.api.recipes.IRecipeTypeInfo;
import dev.wolfieboy09.qtech.api.recipes.data.void_crafting.VoidCraftingRecipeParams;
import dev.wolfieboy09.qtech.api.recipes.data.void_crafting.VoidCraftingRecipe;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.concurrent.CompletableFuture;

@NothingNullByDefault
public abstract class VoidCraftingRecipeGen extends ProcessingRecipeGen<VoidCraftingRecipeParams, VoidCraftingRecipe, VoidCraftingRecipe.Builder<VoidCraftingRecipe>> {
    public VoidCraftingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String namespace) {
        super(output, registries, namespace);
    }

    protected ResourceLocation theEnd() {
        return ResourceLocation.withDefaultNamespace("the_end");
    }

    protected ResourceLocation theNether() {
        return ResourceLocation.withDefaultNamespace("the_nether");
    }

    protected ResourceLocation overworld() {
        return ResourceLocation.withDefaultNamespace("overworld");
    }

    protected ResourceLocation fromLevelKey(ResourceKey<Level> levelResourceKey) {
        return levelResourceKey.location();
    }

    protected ResourceLocation fromDimensionType(ResourceKey<DimensionType> dimensionTypeResourceKey) {
        return dimensionTypeResourceKey.location();
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return QTRecipeTypes.VOID_CRAFTING;
    }

    @Override
    protected VoidCraftingRecipe.Builder<VoidCraftingRecipe> getBuilder(ResourceLocation id) {
        return new VoidCraftingRecipe.Builder<>(VoidCraftingRecipe::new, id);
    }
}
