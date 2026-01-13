package dev.wolfieboy09.qtech.api.recipes.data.centrifuge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wolfieboy09.qtech.api.codecs.NonNullListStreamCodec;
import dev.wolfieboy09.qtech.api.recipes.ProcessingRecipeParams;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public class CentrifugeRecipeParams extends ProcessingRecipeParams {
    public static final MapCodec<CentrifugeRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            codec(CentrifugeRecipeParams::new).forGetter(Function.identity()),
            Ingredient.CODEC.listOf(0, 3).optionalFieldOf("parts", List.of()).forGetter(CentrifugeRecipeParams::parts)
    ).apply(instance, (params, parts) -> {
        params.parts = NonNullList.copyOf(parts);
        return params;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipeParams> STREAM_CODEC = streamCodec(CentrifugeRecipeParams::new);

    protected NonNullList<Ingredient> parts;

    public CentrifugeRecipeParams() {
        super();
        this.parts = NonNullList.create();
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        NonNullListStreamCodec.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, parts);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        parts = NonNullListStreamCodec.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer);
    }

    protected final NonNullList<Ingredient> parts() {
        return this.parts;
    }
}
