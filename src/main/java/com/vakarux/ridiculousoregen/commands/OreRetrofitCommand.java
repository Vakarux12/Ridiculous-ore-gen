package com.vakarux.ridiculousoregen.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vakarux.ridiculousoregen.RidiculousOreGen;
import com.vakarux.ridiculousoregen.RidiculousOreGenConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.*;

public class OreRetrofitCommand {

    /** Stone-like blocks that can be replaced with ore during vein fill. */
    private static final Set<Block> REPLACEABLE = Set.of(
            Blocks.STONE, Blocks.DEEPSLATE, Blocks.TUFF,
            Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.CALCITE, Blocks.SMOOTH_BASALT, Blocks.RAW_IRON_BLOCK
    );

    private static final int MAX_VEIN_SCAN = 128;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ridiculousores")
                .requires(src -> src.hasPermission(2))

                // /ridiculousores fill <x> <y> <z>
                // Finds the ore vein at that block, counts it, places (count*(multi-1)) more blocks nearby.
                .then(Commands.literal("fill")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> fillVein(ctx,
                                BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))

                // /ridiculousores populate <radius>
                // Re-runs ore worldgen features (multiplier-1) extra times on each loaded chunk in radius.
                .then(Commands.literal("populate")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .executes(ctx -> populateArea(ctx,
                                IntegerArgumentType.getInteger(ctx, "radius")))))
        );
    }

    // ── fill ────────────────────────────────────────────────────────────────

    private static int fillVein(CommandContext<CommandSourceStack> ctx, BlockPos pos)
            throws CommandSyntaxException {

        ServerLevel level = ctx.getSource().getLevel();
        BlockState target = level.getBlockState(pos);

        if (!isOre(target)) {
            ctx.getSource().sendFailure(Component.literal(
                    "No ore block at " + pos.toShortString() + " (found: " + target.getBlock().getDescriptionId() + ")"));
            return 0;
        }

        // Flood-fill to find connected vein
        Set<BlockPos> vein = floodFillVein(level, pos, target.getBlock(), MAX_VEIN_SCAN);
        int current = vein.size();
        int multiplier = RidiculousOreGenConfig.ORE_MULTIPLIER.getAsInt();
        int toAdd = current * (multiplier - 1);

        if (toAdd <= 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("Multiplier is 1 — nothing to add."), false);
            return 0;
        }

        BlockPos center = centroid(vein);
        int placed = placeExtraOre(level, target, vein, center, toAdd);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Vein size: " + current + " → target: " + (current + toAdd) + " | placed: " + placed), true);
        return placed;
    }

    private static Set<BlockPos> floodFillVein(ServerLevel level, BlockPos start, Block block, int max) {
        Set<BlockPos> visited = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && visited.size() < max) {
            BlockPos cur = queue.poll();
            if (visited.contains(cur)) continue;
            if (!level.getBlockState(cur).is(block)) continue;
            visited.add(cur);
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos nb = cur.relative(dir);
                if (!visited.contains(nb)) queue.add(nb);
            }
        }
        return visited;
    }

    private static BlockPos centroid(Set<BlockPos> blocks) {
        long x = 0, y = 0, z = 0;
        for (BlockPos p : blocks) { x += p.getX(); y += p.getY(); z += p.getZ(); }
        int n = blocks.size();
        return new BlockPos((int)(x / n), (int)(y / n), (int)(z / n));
    }

    private static int placeExtraOre(ServerLevel level, BlockState ore,
                                     Set<BlockPos> existing, BlockPos center, int needed) {
        Random rng = new Random(center.asLong());
        Set<BlockPos> occupied = new HashSet<>(existing);
        int placed = 0;
        int radius = 2;

        while (placed < needed && radius <= 12) {
            int attempts = needed * 20;
            while (attempts-- > 0 && placed < needed) {
                int dx = rng.nextInt(radius * 2 + 1) - radius;
                int dy = rng.nextInt(radius * 2 + 1) - radius;
                int dz = rng.nextInt(radius * 2 + 1) - radius;
                BlockPos candidate = center.offset(dx, dy, dz);
                if (occupied.contains(candidate)) continue;
                if (!level.isLoaded(candidate)) continue;
                BlockState existing_state = level.getBlockState(candidate);
                if (REPLACEABLE.contains(existing_state.getBlock())) {
                    level.setBlock(candidate, ore, Block.UPDATE_ALL);
                    occupied.add(candidate);
                    placed++;
                }
            }
            radius++;
        }
        return placed;
    }

    // ── populate ─────────────────────────────────────────────────────────────

    private static int populateArea(CommandContext<CommandSourceStack> ctx, int radiusChunks)
            throws CommandSyntaxException {

        int maxRadius = RidiculousOreGenConfig.RETROFIT_MAX_RADIUS.getAsInt();
        if (radiusChunks > maxRadius) {
            ctx.getSource().sendFailure(Component.literal(
                    "Radius " + radiusChunks + " exceeds config limit of " + maxRadius));
            return 0;
        }

        int extra = RidiculousOreGenConfig.ORE_MULTIPLIER.getAsInt() - 1;
        if (extra <= 0) {
            ctx.getSource().sendFailure(Component.literal("Multiplier is 1 — nothing to do."));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        BlockPos playerPos = BlockPos.containing(ctx.getSource().getPosition());
        int cx0 = SectionPos.blockToSectionCoord(playerPos.getX());
        int cz0 = SectionPos.blockToSectionCoord(playerPos.getZ());

        int processed = 0;
        for (int cx = cx0 - radiusChunks; cx <= cx0 + radiusChunks; cx++) {
            for (int cz = cz0 - radiusChunks; cz <= cz0 + radiusChunks; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                runExtraOresInChunk(level, cx, cz, extra);
                processed++;
            }
        }

        final int count = processed;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Populated " + count + " chunks with " + extra + " extra ore pass(es)."), true);
        RidiculousOreGen.LOGGER.info("ridiculousores populate: {} chunks, {} extra passes", count, extra);
        return count;
    }

    /**
     * Re-runs ore-tagged PlacedFeatures from the biome at the chunk centre
     * {@code extra} additional times, effectively retrofitting the chunk.
     */
    private static void runExtraOresInChunk(ServerLevel level, int chunkX, int chunkZ, int extra) {
        // Seed matches vanilla per-chunk feature seed pattern
        long seed = level.getSeed()
                ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L);
        RandomSource rng = RandomSource.create(seed);

        // Use the middle block as the feature origin (most placed features use X/Z from their own placement)
        BlockPos origin = new ChunkPos(chunkX, chunkZ).getMiddleBlockPosition(64);

        Holder<Biome> biome = level.getBiome(origin);
        List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();

        runStep(level, steps, GenerationStep.Decoration.UNDERGROUND_ORES, origin, rng, extra);
        runStep(level, steps, GenerationStep.Decoration.UNDERGROUND_DECORATION, origin, rng, extra);
    }

    private static void runStep(
            ServerLevel level,
            List<HolderSet<PlacedFeature>> steps,
            GenerationStep.Decoration step,
            BlockPos origin,
            RandomSource rng,
            int extra) {

        int idx = step.ordinal();
        if (idx >= steps.size()) return;

        for (Holder<PlacedFeature> feature : steps.get(idx)) {
            String id = feature.unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (!id.contains("ore") && !id.contains("vein") && !id.contains("blob")) continue;

            for (int i = 0; i < extra; i++) {
                try {
                    feature.value().place(level, level.getChunkSource().getGenerator(), rng, origin);
                } catch (Exception e) {
                    RidiculousOreGen.LOGGER.warn("Failed to place feature {} in chunk: {}", id, e.getMessage());
                }
            }
        }
    }

    private static boolean isOre(BlockState state) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock()).getPath();
        return id.contains("ore") || id.contains("ancient_debris");
    }
}
