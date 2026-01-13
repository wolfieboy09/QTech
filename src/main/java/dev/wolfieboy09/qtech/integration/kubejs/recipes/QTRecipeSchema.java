package dev.wolfieboy09.qtech.integration.kubejs.recipes;

import com.mojang.datafixers.util.Either;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.*;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.TickDuration;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.wolfieboy09.qtech.api.gas.crafting.SizedGasIngredient;
import dev.wolfieboy09.qtech.api.recipes.CleanroomCondition;
import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.api.recipes.result.FluidStackChanceResult;
import dev.wolfieboy09.qtech.api.recipes.result.GasStackChanceResult;
import dev.wolfieboy09.qtech.api.recipes.result.ItemStackChanceResult;
import dev.wolfieboy09.qtech.api.util.ResourceHelper;
import dev.wolfieboy09.qtech.api.util.TriEither;
import dev.wolfieboy09.qtech.integration.kubejs.recipes.components.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class QTRecipeSchema {
    private static final RecipeKey<List<TriEither<Ingredient, SizedFluidIngredient, SizedGasIngredient>>> INGREDIENTS = TriEitherComponent.of(
            IngredientComponent.INGREDIENT.instance(),
            SizedFluidIngredientComponent.FLAT.instance(),
            SizedGasIngredientComponent.FLAT.instance()).asList().inputKey("ingredients");

    private static final RecipeKey<Integer> ENERGY = NumberComponent.INT.otherKey("energy").optional(200);

    private static final RecipeKey<TickDuration> TICKS = TimeComponent.TICKS.otherKey("processing_time").functionNames(List.of("processingTime", "duration")).optional(TickDuration.of(100));

    private static final RecipeKey<List<TriEither<ItemStackChanceResult, FluidStackChanceResult, GasStackChanceResult>>> RESULTS = TriEitherComponent.of(
            ItemStackChanceComponent.TYPE.instance(),
            FluidStackChanceComponent.TYPE.instance(),
            GasStackChanceComponent.TYPE.instance()).asList().outputKey("results");

    private static final RecipeKey<CleanroomCondition> CLEANROOM_CONDITION = EnumComponent.of(location("cleanroom"), CleanroomCondition.class, CleanroomCondition.CODEC).otherKey("cleanroom_condition").optional(CleanroomCondition.NONE).functionNames(List.of("cleanroom", "cleanroomCondition"));

    private static final RecipeKey<List<Ingredient>> EXTRA_INGREDIENTS = IngredientComponent.INGREDIENT.instance().asList().key("extras", ComponentRole.INPUT).defaultOptional();

    private static final RecipeKey<List<Either<ItemStack, FluidStack>>> WASTE = ItemStackComponent.ITEM_STACK.instance().or(FluidStackComponent.FLUID_STACK.instance()).asList().key("waste", ComponentRole.OUTPUT);

    private static final RecipeKey<Integer> TEMPERATURE = NumberComponent.INT.key("temperature", ComponentRole.OTHER).optional(200);

    private static RecipeSchema create(String id, Class<? extends Recipe<?>> recipeClass, QTRecipeFactory factory, RecipeKey<?>... keys) {
        return new RecipeSchema(keys).factory(new KubeRecipeFactory(ResourceHelper.asResource(id), TypeInfo.of(recipeClass), () -> factory)).uniqueIds(List.of(INGREDIENTS, RESULTS, CLEANROOM_CONDITION));
    }

    @Contract("_ -> new")
    private static @NotNull ResourceLocation location(String path) {
        return ResourceHelper.asResource(path);
    }

    public static final RecipeSchema DISK_ASSEMBLY = create("disk_assembly", DiskAssemblerRecipe.class,
            new QTRecipeFactory()
                    .ingredients(INGREDIENTS, 3, 0, 0)
                    .extraIngredients(EXTRA_INGREDIENTS, 4)
                    .results(RESULTS, 1, 0, 0)
                    .energy(ENERGY)
                    .cleanroom(CLEANROOM_CONDITION)
                    .duration(TICKS),
            RESULTS, INGREDIENTS, EXTRA_INGREDIENTS, CLEANROOM_CONDITION, ENERGY, TICKS)
            .constructor(RESULTS, INGREDIENTS);
}
