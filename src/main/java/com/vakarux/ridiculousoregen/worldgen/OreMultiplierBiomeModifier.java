package com.vakarux.ridiculousoregen.worldgen;

import com.mojang.serialization.MapCodec;
import com.vakarux.ridiculousoregen.RidiculousOreGen;
import com.vakarux.ridiculousoregen.RidiculousOreGenConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

public record OreMultiplierBiomeModifier() implements BiomeModifier {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RidiculousOreGen.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<OreMultiplierBiomeModifier>> CODEC_HOLDER =
            BIOME_MODIFIER_SERIALIZERS.register("ore_multiplier", () -> MapCodec.unit(new OreMultiplierBiomeModifier()));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // Run in MODIFY phase so every other mod's ADD phase has already completed.
        // Reading from the builder at this point sees vanilla + ALL modded ore features.
        if (phase != Phase.MODIFY) return;

        int extra = RidiculousOreGenConfig.ORE_MULTIPLIER.getAsInt() - 1;
        if (extra <= 0) return;

        addExtraCopies(builder, GenerationStep.Decoration.UNDERGROUND_ORES, extra);
        addExtraCopies(builder, GenerationStep.Decoration.UNDERGROUND_DECORATION, extra);
    }

    private void addExtraCopies(
            ModifiableBiomeInfo.BiomeInfo.Builder builder,
            GenerationStep.Decoration step,
            int extra) {

        // Snapshot before iterating so we don't see our own additions mid-loop
        List<Holder<PlacedFeature>> snapshot = List.copyOf(
                builder.getGenerationSettings().getFeatures(step));

        for (Holder<PlacedFeature> feature : snapshot) {
            // Only multiply ore/vein/blob features — skip geodes, stalactites, etc.
            String id = feature.unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!id.contains("ore") && !id.contains("vein") && !id.contains("blob")) continue;

            for (int i = 0; i < extra; i++) {
                builder.getGenerationSettings().getFeatures(step).add(feature);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC_HOLDER.get();
    }
}
