package dev.wolfieboy09.qtech.registries;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.recipes.IRecipeTypeInfo;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipe;
import dev.wolfieboy09.qtech.api.recipes.StandardProcessingRecipe;
import dev.wolfieboy09.qtech.api.recipes.data.centrifuge.CentrifugeRecipe;
import dev.wolfieboy09.qtech.api.recipes.data.centrifuge.CentrifugeRecipeParams;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipeParams;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.api.recipes.data.void_crafting.VoidCraftingRecipeParams;
import dev.wolfieboy09.qtech.api.recipes.data.void_crafting.VoidCraftingRecipe;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

@NothingNullByDefault
@SuppressWarnings("unchecked")
public enum QTRecipeTypes implements IRecipeTypeInfo, StringRepresentable {
    DISK_ASSEMBLY(DiskAssemblerRecipe::new),
    VOID_CRAFTING(VoidCraftingRecipe::new),
    CENTRIFUGE(CentrifugeRecipe::new);

    public final ResourceLocation id;
    public final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    @Nullable
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
    private final Supplier<RecipeType<?>> type;

    QTRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier, Supplier<RecipeType<?>> typeSupplier, boolean registerType) {
        String name = ResourceHelper.asResource(name()).toString().toLowerCase();
        id = ResourceHelper.asResource(name);
        this.serializerSupplier = serializerSupplier;
        this.serializerObject = Registers.SERIALIZER.register(name, serializerSupplier);
        if (registerType) {
            typeObject = Registers.TYPE.register(name, typeSupplier);
            type = typeObject;
        } else {
            this.typeObject = null;
            this.type = typeSupplier;
        }
    }

    QTRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String name = ResourceHelper.asResource(name().toLowerCase()).toString();
        this.id = ResourceLocation.parse(name);
        this.serializerSupplier = serializerSupplier;
        this.serializerObject = Registers.SERIALIZER.register(id.getPath(), serializerSupplier);
        this.typeObject = Registers.TYPE.register(id.getPath(), () -> RecipeType.simple(id));
        this.type = typeObject;
    }

    QTRecipeTypes(StandardProcessingRecipe.Factory<?> processingFactory) {
        this(() -> new StandardProcessingRecipe.Serializer<>(processingFactory));
    }

    QTRecipeTypes(DiskAssemblerRecipe.Factory<?> factory) {
        this(() -> new DiskAssemblerRecipe.Serializer<>(factory));
    }

    QTRecipeTypes(VoidCraftingRecipe.Factory<?> factory) {
        this(() -> new VoidCraftingRecipe.Serializer<>(factory));
    }

    QTRecipeTypes(CentrifugeRecipe.Factory<?> factory) {
        this(() -> new CentrifugeRecipe.Serializer<>(factory));
    }

    @ApiStatus.Internal
    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZER.register(modEventBus);
        Registers.TYPE.register(modEventBus);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializerObject.get();
    }

    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) this.type.get();
    }

    public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level world) {
        return world.getRecipeManager().getRecipeFor(getType(), inv, world);
    }

    @Override
    public String getSerializedName() {
        return id.getNamespace() + id.getPath().toLowerCase();
    }

    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, QuantiumizedTech.MOD_ID);
        private static final DeferredRegister<RecipeType<?>> TYPE = DeferredRegister.create(Registries.RECIPE_TYPE, QuantiumizedTech.MOD_ID);
    }
}
