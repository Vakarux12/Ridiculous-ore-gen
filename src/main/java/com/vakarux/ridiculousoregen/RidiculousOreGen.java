package com.vakarux.ridiculousoregen;

import com.mojang.logging.LogUtils;
import com.vakarux.ridiculousoregen.commands.OreRetrofitCommand;
import com.vakarux.ridiculousoregen.worldgen.OreMultiplierBiomeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(RidiculousOreGen.MODID)
public class RidiculousOreGen {

    public static final String MODID = "ridiculousoregen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RidiculousOreGen(IEventBus modEventBus, ModContainer modContainer) {
        OreMultiplierBiomeModifier.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, RidiculousOreGenConfig.SPEC);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                OreRetrofitCommand.register(event.getDispatcher()));
    }
}
