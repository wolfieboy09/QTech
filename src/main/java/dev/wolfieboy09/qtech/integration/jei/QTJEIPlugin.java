package dev.wolfieboy09.qtech.integration.jei;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.annotation.NothingNullByDefault;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.api.recipes.data.void_crafting.VoidCraftingRecipe;
import dev.wolfieboy09.qtech.api.registry.QTRegistries;
import dev.wolfieboy09.qtech.api.registry.gas.Gas;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import dev.wolfieboy09.qtech.integration.jei.category.EmptyBackground;
import dev.wolfieboy09.qtech.integration.jei.category.QTRecipeCategory;
import dev.wolfieboy09.qtech.integration.jei.category.recipes.DiskAssemblerCategory;
import dev.wolfieboy09.qtech.integration.jei.category.recipes.VoidCraftingCategory;
import dev.wolfieboy09.qtech.integration.jei.modifiers.GasIngredientHelper;
import dev.wolfieboy09.qtech.integration.jei.modifiers.GasIngredientRenderer;
import dev.wolfieboy09.qtech.registries.QTBlocks;
import dev.wolfieboy09.qtech.registries.QTGasses;
import dev.wolfieboy09.qtech.registries.QTGuiTextures;
import dev.wolfieboy09.qtech.registries.QTRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@JeiPlugin
@NothingNullByDefault
public class QTJEIPlugin implements IModPlugin {
    public static final IIngredientType<Gas> GAS_TYPE = () -> Gas.class;
    private final List<QTRecipeCategory<?>> allCategories = new ArrayList<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceHelper.asResource("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        loadCategories();
        registration.addRecipeCategories(this.allCategories.toArray(IRecipeCategory[]::new));

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        this.allCategories.forEach(c -> c.registerRecipes(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        this.allCategories.forEach(c -> c.registerCatalysts(registration));
    }

    @Override
    public void registerModInfo(IModInfoRegistration modAliasRegistration) {
        modAliasRegistration.addModAliases(QuantiumizedTech.MOD_ID);
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(GAS_TYPE, QTRegistries.GAS.stream().filter(g -> g != QTGasses.EMPTY.get()).toList(), new GasIngredientHelper(), new GasIngredientRenderer(16, 16), Gas.CODEC);
    }

    private void loadCategories() {
        this.allCategories.clear();
        QTRecipeCategory<?>
                disk_assembly = builder(DiskAssemblerRecipe.class)
                .addTypedRecipes(QTRecipeTypes.DISK_ASSEMBLY)
                .background(asDrawable(QTGuiTextures.JEI_DISK_ASSEMBLER))
                .catalystStack(() -> new ItemStack(QTBlocks.DISK_ASSEMBLER.get()))
                .build(ResourceHelper.asResource("disk_assembly"), DiskAssemblerCategory::new);

        QTRecipeCategory<?>
                void_crafting = builder(VoidCraftingRecipe.class)
                .addTypedRecipes(QTRecipeTypes.VOID_CRAFTING)
                .background(new EmptyBackground(150, 100))
                .catalystStack(() -> new ItemStack(Items.ECHO_SHARD))
                .build(ResourceHelper.asResource("void_crafting"), VoidCraftingCategory::new);
    }


    public static void consumeAllRecipes(Consumer<? super RecipeHolder<?>> consumer) {
        Minecraft.getInstance()
                .getConnection()
                .getRecipeManager()
                .getRecipes()
                .forEach(consumer);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Recipe<?>> void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer, RecipeType<?> type) {
        List<? extends RecipeHolder<?>> map = Minecraft.getInstance()
                .getConnection()
                .getRecipeManager().getAllRecipesFor((RecipeType) type);
        if (!map.isEmpty())
            map.forEach(consumer);
    }

    protected static IDrawable asDrawable(QTGuiTextures texture) {
        return new IDrawable() {
            @Override
            public int getWidth() {
                return texture.getWidth();
            }

            @Override
            public int getHeight() {
                return texture.getHeight();
            }

            @Override
            public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
                texture.render(guiGraphics, xOffset, yOffset);
            }
        };
    }

    private class CategoryBuilder<T extends Recipe<?>> extends QTRecipeCategory.Builder<T> {
        public CategoryBuilder(Class<? extends T> recipeClass) {
            super(recipeClass);
        }

        @Override
        public QTRecipeCategory<T> build(ResourceLocation id, QTRecipeCategory.Factory<T> factory) {
            QTRecipeCategory<T> category = super.build(id, factory);
            allCategories.add(category);
            return category;
        }
    }

    private <T extends Recipe<? extends RecipeInput>> CategoryBuilder<T> builder(Class<T> recipeClass) {
        return new CategoryBuilder<>(recipeClass);
    }

}
