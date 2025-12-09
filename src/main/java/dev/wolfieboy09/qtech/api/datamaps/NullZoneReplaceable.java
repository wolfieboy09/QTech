package dev.wolfieboy09.qtech.api.datamaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record NullZoneReplaceable(Holder<Item> replaceWith) {
    public static final Codec<NullZoneReplaceable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("replaceable").forGetter(NullZoneReplaceable::replaceWith)
    ).apply(instance, NullZoneReplaceable::new));
}
