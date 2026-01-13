package dev.wolfieboy09.qtech.integration.jei.category.recipes;

import dev.wolfieboy09.qtech.api.recipes.data.disk_assembler.DiskAssemblerRecipe;
import dev.wolfieboy09.qtech.integration.jei.category.QTRecipeCategory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DiskAssemblerCategory extends QTRecipeCategory<DiskAssemblerRecipe> {

    public DiskAssemblerCategory(Info<DiskAssemblerRecipe> info) {
        super(info);
    }

    @Override
    protected void setRecipe(IRecipeLayoutBuilder builder, DiskAssemblerRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.getItemIngredients().size(); i++) {
            if (i == 0) {
                builder.addInputSlot(13, 24).addIngredients(recipe.getItemIngredients().get(i).ingredient());
            } else if (i == 1) {
                builder.addInputSlot(13, 42).addIngredients(recipe.getItemIngredients().get(i).ingredient());
            } else {
                builder.addInputSlot(31, 33).addIngredients(recipe.getItemIngredients().get(i).ingredient());
            }
        }

        for (int i = 0; i < recipe.getExtras().size(); i++) {
            Ingredient extra = recipe.getExtras().get(i);
            int x = (112 + (i / 2) * 18);
            int y = (24 + (i % 2) * 18);
            IRecipeSlotBuilder slot = builder.addInputSlot(x, y);

            if (!extra.isEmpty()) {
                slot.addIngredients(extra);
            }
        }

        builder.addOutputSlot(76, 33).addIngredient(
                VanillaTypes.ITEM_STACK,
                recipe.getRollableResults().getFirst().getResult()
        ).setBackground(getRenderedSlot(recipe.getRollableResults().getFirst().getChance()), -1, -1)
                .addRichTooltipCallback(addChanceTooltip(recipe.getRollableResults().getFirst()));
    }

    @Override
    protected void draw(DiskAssemblerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {

    }
}
