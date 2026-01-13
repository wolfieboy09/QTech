package dev.wolfieboy09.qtech.api.datagen.builder;

import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.datagen.framework.ProcessingRecipeGen;
import dev.wolfieboy09.qtech.api.recipes.IRecipeTypeInfo;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipeParams;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

@NothingNullByDefault
public abstract class DiskAssemblyRecipeGen extends ProcessingRecipeGen<DiskAssemblerRecipeParams, DiskAssemblerRecipe, DiskAssemblerRecipe.Builder<DiskAssemblerRecipe>> {
    public DiskAssemblyRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String namespace) {
        super(output, registries, namespace);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return QTRecipeTypes.DISK_ASSEMBLY;
    }

    @Override
    protected DiskAssemblerRecipe.Builder<DiskAssemblerRecipe> getBuilder(ResourceLocation id) {
        return new DiskAssemblerRecipe.Builder<>(DiskAssemblerRecipe::new, id);
    }
}
