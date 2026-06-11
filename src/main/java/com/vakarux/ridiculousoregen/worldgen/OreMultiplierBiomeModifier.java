package com.vakarux.ridiculousoregen.worldgen;

import com.mojang.serialization.MapCodec;
import com.vakarux.ridiculousoregen.RidiculousOreGen;
import com.vakarux.ridiculousoregen.RidiculousOreGenConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
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

        for (Holder<PlacedFeature> feature : snapshot) {
            features.add(feature);
            String id = feature.unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!id.contains("ore") && !id.contains("vein") && !id.contains("blob")) continue;

            // Create structurally distinct PlacedFeature instances for each extra copy.
            // We cannot reuse the same Holder — FeatureSorter deduplicates by PlacedFeature
            // value (Object2IntOpenHashMap keyed on PlacedFeature.equals()), so two identical
            // entries produce a self-loop in its successor graph, detected as a feature order
            // cycle and crashing the server.
            //
            // Fix: append i copies of CountPlacement.of(1) (a true no-op at chain end — each
            // incoming position passes through unchanged) to produce i+1 structurally distinct
            // PlacedFeature values. Each runs the ore with the same effective count as the
            // original.
            PlacedFeature pf = feature.value();
            for (int i = 1; i <= extra; i++) {
                List<PlacementModifier> newPlacement = new ArrayList<>(pf.placement());
                for (int j = 0; j < i; j++) {
                    newPlacement.add(CountPlacement.of(1));
                }
                features.add(Holder.direct(new PlacedFeature(pf.feature(), newPlacement)));
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC_HOLDER.get();
    }
}
