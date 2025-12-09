package dev.wolfieboy09.qtech.datagen;

import dev.wolfieboy09.qtech.QuantiumizedTech;
import dev.wolfieboy09.qtech.api.recipes.CleanroomCondition;
import dev.wolfieboy09.qtech.api.util.NamingUtil;
import dev.wolfieboy09.qtech.registries.*;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QTLangProvider extends LanguageProvider {
    public QTLangProvider(PackOutput output) {
        super(output, QuantiumizedTech.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        final Map<String, String> toGenerate = getManuallyWrittenTranslations();

        QTItems.ITEMS.getEntries().forEach(
                item -> addItem(item, NamingUtil.toHumanReadable(item.getRegisteredName().split(":")[1]))
        );
        QTBlocks.BLOCKS.getEntries().forEach(
                block -> addBlock(block, NamingUtil.toHumanReadable(block.getRegisteredName().split(":")[1]))
        );
        QTCreativeTab.REGISTER.getEntries().forEach(
                tab -> add(tab.get().getDisplayName().getString(), QTCreativeTab.nameForLangGen.get(tab.get().getDisplayName().getString()))
        );
        QTGasses.GASSES.getEntries().forEach(
                tab -> add(tab.get().getName().getString(), NamingUtil.toHumanReadable(tab.get().getResourceLocation().getPath()))
        );
        Arrays.asList(QTRecipeTypes.values()).forEach(
                recipe -> add(recipe.getId().getNamespace() + ".recipe." + recipe.getId().getPath(), NamingUtil.toHumanReadable(recipe.getId().getPath()))
        );
        toGenerate.forEach(this::add);
    }

    private static @NotNull Map<String, String> getManuallyWrittenTranslations() {
        final Map<String, String> toGenerate = new HashMap<>();
        toGenerate.put("qtech.oxygen_deprivation", "%s could not breath on land");
        toGenerate.put("effect.qtech.oxygen_deprivation", "Oxygen Deprivation");
        toGenerate.put("tooltip.qtech.max_energy_upgrade", "Adds more energy capacity");
        toGenerate.put("tooltip.qtech.speed_upgrade", "Makes the process faster");

        toGenerate.put("qtech.screen.fluid", "%s: %s/%s");
        toGenerate.put("qtech.tooltip.shift_info", "Press §6SHIFT§r for more info");
        toGenerate.put("qtech.gas_canister.shifted_info", "Supports containing gasses inside the container");
        toGenerate.put("qtech.gas_canister.contains_gas", "Storing §e%s§r");

        toGenerate.put("qtech.configuration.renderFluidOnSmeltery", "Render fluid on Smeltery");

        toGenerate.put("multiblock.qtech.formed", "Multiblock Formed");
        toGenerate.put("multiblock.qtech.unformed", "Multiblock Not Formed");
        toGenerate.put("config.jade.plugin_qtech.multiblock_controller", "Multiblock Controller");
        toGenerate.put("config.jade.plugin_qtech.cleanroom_controller", "Cleanroom Controller");

        toGenerate.put("cleanroom.qtech.contaminated", "Contaminated");
        toGenerate.put("cleanroom.qtech.clean", "Clean");
        toGenerate.put("cleanroom.qtech.clean_status", "Status: %s");

        toGenerate.put("cleanroom.qtech.condition", "Condition: %s");
        toGenerate.put("qtech.recipe.processing.chance", "%1$s%% Chance");
        toGenerate.put("qtech.ender_chest_fail", "This ender chest does not seem to work here...");

        for (CleanroomCondition condition : CleanroomCondition.values()) {
            toGenerate.put(condition.getTranslationKey(), NamingUtil.toHumanReadable(condition.getSerializedName()));
        }

        return toGenerate;
    }
}
