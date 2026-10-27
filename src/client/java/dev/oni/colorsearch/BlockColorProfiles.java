package dev.oni.colorsearch;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * How much of each dye color a block's model textures contain and how light those pixels are,
 * computed lazily from the loaded resource packs and cached until the block models are reloaded.
 * Render thread only.
 */
public final class BlockColorProfiles {
	/** Minimum share of pixels matching the queried colors for a block to count as that color. */
	public static final float MIN_SHARE = 0.25f;

	private static final Logger LOG = LoggerFactory.getLogger("Color Search");
	private static final int NO_TINT = -1;
	// Large resource-pack textures are sampled on a grid this wide; 16×16 vanilla ones are read fully.
	private static final int MAX_SAMPLES_PER_AXIS = 32;
	private static final long MODEL_SEED = 42L;
	private static final Direction[] FACES_AND_UNCULLED = {
		Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null
	};

	private static final Map<Item, ColorProfile> PROFILES_BY_ITEM = new HashMap<>();
	private static final Map<SpriteTint, Optional<ColorProfile>> PROFILES_BY_SPRITE = new HashMap<>();
	private static BlockStateModelSet profiledModels;

	private BlockColorProfiles() {
	}

	/**
	 * Blocks among {@code candidates} named after one of the query's dyes (blue_terracotta, red_bed)
	 * or with at least {@link #MIN_SHARE} of their texture pixels in the query's colors, from the
	 * lightest to the darkest shade of those colors.
	 */
	public static List<ItemStack> matching(Collection<ItemStack> candidates, ColorQuery query) {
		Minecraft minecraft = Minecraft.getInstance();
		resetIfModelsReloaded(minecraft);

		long startNanos = System.nanoTime();
		int cachedBefore = PROFILES_BY_ITEM.size();
		List<Match> matches = new ArrayList<>();
		for (ItemStack stack : candidates) {
			if (!(stack.getItem() instanceof BlockItem blockItem) || !isBlockOfSameName(blockItem)) {
				continue;
			}
			Optional<DyeColor> namedDye = namedDye(blockItem, query.namedDyes());
			ColorProfile profile = PROFILES_BY_ITEM.computeIfAbsent(blockItem, item -> profile(minecraft, blockItem));
			if (namedDye.isPresent() || profile.share(query.pixelMask()) >= MIN_SHARE) {
				matches.add(new Match(stack, sortLightness(profile, query.pixelMask(), namedDye)));
			}
		}
		int profiled = PROFILES_BY_ITEM.size() - cachedBefore;
		if (profiled > 0) {
			LOG.info("[Color Search] Profiled {} blocks in {} ms", profiled, (System.nanoTime() - startNanos) / 1_000_000);
		}
		matches.sort(Comparator.comparingDouble(Match::lightness).reversed());
		return matches.stream().map(Match::stack).toList();
	}

	/**
	 * How light the block's queried color is: the mean lightness of its pixels in those colors.
	 * Dye-named blocks may have none (blue terracotta's texture is purple-grey), so they fall back
	 * to the whole texture, and beds and banners, which have no texture to read, to their dye.
	 */
	private static double sortLightness(ColorProfile profile, int colorMask, Optional<DyeColor> namedDye) {
		OptionalDouble colorLightness = profile.lightness(colorMask);
		if (colorLightness.isPresent()) {
			return colorLightness.getAsDouble();
		}
		OptionalDouble overallLightness = profile.overallLightness();
		if (overallLightness.isPresent()) {
			return overallLightness.getAsDouble();
		}
		// Only dye-named blocks get here: any other match has pixels in the queried colors.
		return PixelClassifier.lightness(namedDye.orElseThrow().getTextureDiffuseColor());
	}

	// Seeds, carrots, redstone dust and the like place a differently named block whose model
	// (a crop, a wire) looks nothing like the item; they are items, not blocks, for this search.
	private static boolean isBlockOfSameName(BlockItem item) {
		return BuiltInRegistries.ITEM.getKey(item).equals(BuiltInRegistries.BLOCK.getKey(item.getBlock()));
	}

	// Dyed blocks keep their dye in the id even when the texture disagrees: blue terracotta is
	// purple-grey, beds and banners have no block model to read.
	private static Optional<DyeColor> namedDye(BlockItem item, Collection<DyeColor> colors) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		for (DyeColor color : colors) {
			if (path.startsWith(color.getName() + "_")) {
				return Optional.of(color);
			}
		}
		return Optional.empty();
	}

	private static void resetIfModelsReloaded(Minecraft minecraft) {
		BlockStateModelSet models = minecraft.getModelManager().getBlockStateModelSet();
		if (models != profiledModels) {
			PROFILES_BY_ITEM.clear();
			PROFILES_BY_SPRITE.clear();
			profiledModels = models;
		}
	}

	/** Average of the per-quad color profiles over every textured quad of the block's default state. */
	private static ColorProfile profile(Minecraft minecraft, BlockItem item) {
		BlockState state = item.getBlock().defaultBlockState();
		List<BlockStateModelPart> parts = new ArrayList<>();
		profiledModels.get(state).collectParts(RandomSource.create(MODEL_SEED), parts);

		ColorProfile total = new ColorProfile();
		int texturedQuads = 0;
		for (BlockStateModelPart part : parts) {
			for (Direction face : FACES_AND_UNCULLED) {
				for (BakedQuad quad : part.getQuads(face)) {
					BakedQuad.MaterialInfo material = quad.materialInfo();
					int tint = material.isTinted() ? tintColor(minecraft, state, material.tintIndex()) : NO_TINT;
					Optional<ColorProfile> quadProfile = spriteProfile(minecraft, material.sprite(), tint);
					if (quadProfile.isPresent()) {
						total.addAll(quadProfile.get());
						texturedQuads++;
					}
				}
			}
		}
		// Chests, signs, shulker boxes and the like render through block entities and have no quads;
		// their particle texture stands in. Not for beds and banners: their particle is plain planks,
		// and their dye name already places them.
		if (texturedQuads == 0 && namedDye(item, DyeColor.VALUES).isEmpty()) {
			Optional<ColorProfile> particle = spriteProfile(minecraft, profiledModels.getParticleMaterial(state).sprite(), NO_TINT);
			if (particle.isPresent()) {
				total.addAll(particle.get());
				texturedQuads = 1;
			}
		}
		if (texturedQuads > 1) {
			total.scale(1f / texturedQuads);
		}
		return total;
	}

	private static int tintColor(Minecraft minecraft, BlockState state, int tintIndex) {
		BlockTintSource source = minecraft.getBlockColors().getTintSource(state, tintIndex);
		return source == null ? NO_TINT : source.color(state);
	}

	private static Optional<ColorProfile> spriteProfile(Minecraft minecraft, TextureAtlasSprite sprite, int tint) {
		SpriteContents contents = sprite.contents();
		return PROFILES_BY_SPRITE.computeIfAbsent(
			new SpriteTint(contents.name(), tint),
			key -> readFirstFrame(minecraft, contents).flatMap(pixels -> PixelClassifier.histogram(pixels, tint))
		);
	}

	/** ARGB pixels of the sprite's first animation frame, read from its PNG in the resource packs. */
	private static Optional<int[]> readFirstFrame(Minecraft minecraft, SpriteContents contents) {
		Identifier file = contents.name().withPath(path -> "textures/" + path + ".png");
		Optional<Resource> resource = minecraft.getResourceManager().getResource(file);
		if (resource.isEmpty()) {
			// Generated sprites such as the missing-texture checkerboard have no file.
			LOG.debug("[Color Search] No texture file {} for sprite {}", file, contents.name());
			return Optional.empty();
		}
		try (InputStream input = resource.get().open(); NativeImage image = NativeImage.read(input)) {
			int width = Math.min(contents.width(), image.getWidth());
			int height = Math.min(contents.height(), image.getHeight());
			int stepX = Math.max(1, width / MAX_SAMPLES_PER_AXIS);
			int stepY = Math.max(1, height / MAX_SAMPLES_PER_AXIS);
			int[] pixels = new int[Math.ceilDiv(width, stepX) * Math.ceilDiv(height, stepY)];
			int i = 0;
			for (int y = 0; y < height; y += stepY) {
				for (int x = 0; x < width; x += stepX) {
					pixels[i++] = image.getPixel(x, y);
				}
			}
			return Optional.of(pixels);
		} catch (IOException e) {
			LOG.warn("[Color Search] Failed to read texture {} for sprite {}; block colors from it are skipped", file, contents.name(), e);
			return Optional.empty();
		}
	}

	private record SpriteTint(Identifier sprite, int tint) {
	}

	private record Match(ItemStack stack, double lightness) {
	}
}
