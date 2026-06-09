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

import java.util.ArrayList;
import java.util.List;

public record OreMultiplierBiomeModifier() implements BiomeModifier {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RidiculousOreGen.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<OreMultiplierBiomeModifier>> CODEC_HOLDER =
            BIOME_MODIFIER_SERIALIZERS.register("ore_multiplier", () -> MapCodec.unit(new OreMultiplierBiomeModifier()));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
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

        List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(step);
        List<Holder<PlacedFeature>> snapshot = List.copyOf(features);
        features.clear();

        // Insert copies immediately after each original to avoid feature order cycles.
        // Appending all copies at the end would interleave ores with non-ore features
        // (e.g. in biomes like alexscaves:candy_cavity), creating ordering contradictions
        // across biomes that crash the FeatureSorter.
        for (Holder<PlacedFeature> feature : snapshot) {
            features.add(feature);
            String id = feature.unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!id.contains("ore") && !id.contains("vein") && !id.contains("blob")) continue;
            for (int i = 0; i < extra; i++) {
                features.add(feature);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC_HOLDER.get();
    }
}
