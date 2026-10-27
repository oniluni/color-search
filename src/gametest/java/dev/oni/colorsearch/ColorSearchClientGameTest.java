package dev.oni.colorsearch;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Types color words into the creative search tab and checks which blocks show up.
 * Screenshots land in build/run/clientGameTest/screenshots for manual review.
 */
public final class ColorSearchClientGameTest implements FabricClientGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("Color Search test");
	private static final int MAX_QUERY_LENGTH = 20;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(20);
			TestInput input = context.getInput();

			input.pressKey(options -> options.keyInventory);
			context.waitForScreen(CreativeModeInventoryScreen.class);
			// The chat key switches the creative screen to the search tab without typing itself.
			input.pressKey(options -> options.keyChat);
			context.waitTicks(2);

			// Russian words don't hit vanilla name search in the default en_us test client, so these
			// results come from the color index alone.
			List<String> failures = new ArrayList<>();
			Map<String, Set<String>> results = new HashMap<>();
			results.put("красный", check(failures, context, input, "красный",
				List.of("red_wool", "redstone_block", "red_terracotta", "mangrove_planks", "bricks", "nether_bricks",
					"red_nether_bricks", "crimson_planks", "granite"),
				// Cinnabar and wool stairs are 26.x additions, hidden by the 1.21.11 filter.
				List.of("stone", "oak_planks", "lapis_block", "cinnabar", "red_cushion", "red_wool_stairs")));
			check(failures, context, input, "бордовый",
				List.of("red_nether_bricks", "nether_bricks", "crimson_planks"), List.of("stone", "pink_wool", "red_terracotta"));
			check(failures, context, input, "оранжевый",
				List.of("orange_wool", "orange_terracotta", "acacia_planks", "pumpkin", "copper_block", "red_sand"),
				List.of("stone", "lapis_block"));
			results.put("жёлтый", check(failures, context, input, "жёлтый",
				List.of("yellow_wool", "gold_block", "sand", "sponge", "hay_block", "bamboo_planks"),
				List.of("stone", "lapis_block", "white_banner", "red_bed")));
			results.put("коричневый", check(failures, context, input, "коричневый",
				List.of("brown_wool", "brown_terracotta", "dirt", "spruce_planks", "dark_oak_planks", "mud_bricks"),
				List.of("stone", "lapis_block", "white_banner")));
			results.put("лаймовый", check(failures, context, input, "лаймовый", List.of("lime_wool", "slime_block", "emerald_block"), List.of("stone")));
			results.put("зелёный", check(failures, context, input, "Зелёный",
				List.of("grass_block", "oak_leaves", "lime_wool", "green_wool", "moss_block", "emerald_block", "cactus"),
				List.of("stone", "red_wool")));
			results.put("бирюзовый", check(failures, context, input, "бирюзовый",
				List.of("cyan_wool", "cyan_terracotta", "warped_planks", "diamond_block", "prismarine_bricks"), List.of("stone")));
			results.put("голубой", check(failures, context, input, "голубой",
				List.of("light_blue_wool", "ice", "packed_ice", "blue_ice", "diamond_block"),
				List.of("stone", "blue_wool", "warped_planks")));
			results.put("синий", check(failures, context, input, "синий",
				List.of("blue_wool", "blue_concrete", "blue_terracotta", "lapis_block", "blue_ice"),
				List.of("stone", "light_blue_concrete")));
			results.put("фиолетовый", check(failures, context, input, "фиолетовый",
				List.of("purple_wool", "amethyst_block", "purpur_block"), List.of("stone")));
			check(failures, context, input, "пурпурный", List.of("magenta_wool", "purpur_block"), List.of("stone"));
			check(failures, context, input, "розовый", List.of("pink_wool", "cherry_planks", "cherry_leaves"), List.of("stone"));
			check(failures, context, input, "белый",
				List.of("white_wool", "snow_block", "quartz_block", "calcite", "iron_block"), List.of("dirt"));
			check(failures, context, input, "светло-серый",
				List.of("light_gray_wool", "stone", "andesite"), List.of("dirt"));
			results.put("серый", check(failures, context, input, "серый",
				List.of("gray_wool", "stone", "cobblestone", "andesite", "deepslate", "tuff"), List.of("dirt", "red_wool")));
			check(failures, context, input, "чёрный",
				List.of("black_wool", "obsidian", "coal_block", "blackstone"), List.of("snow_block"));
			// English queries add color matches after vanilla's name matches.
			results.put("blue", check(failures, context, input, "blue",
				List.of("lapis_block", "blue_wool", "light_blue_concrete", "ice", "blue_dye"), List.of("stone")));
			check(failures, context, input, "red", List.of("red_wool", "bricks", "mangrove_planks"), List.of("stone"));
			check(failures, context, input, "blu", List.of(), List.of("lapis_block", "grass_block"));

			// Color blocks run from lightest to darkest; plain name matches (dyes) come after them.
			checkOrder(failures, "серый", results.get("серый"), "light_gray_wool", "gray_wool");
			checkOrder(failures, "серый", results.get("серый"), "stone", "deepslate");
			checkOrder(failures, "красный", results.get("красный"), "pink_wool", "red_wool");
			checkOrder(failures, "красный", results.get("красный"), "red_wool", "nether_bricks");
			checkOrder(failures, "зелёный", results.get("зелёный"), "lime_wool", "green_wool");
			checkOrder(failures, "коричневый", results.get("коричневый"), "oak_planks", "dark_oak_planks");
			checkOrder(failures, "blue", results.get("blue"), "light_blue_wool", "blue_wool");
			checkOrder(failures, "blue", results.get("blue"), "ice", "lapis_block");
			checkOrder(failures, "blue", results.get("blue"), "lapis_block", "blue_dye");

			// A shade finds a non-empty subset of what its main color finds, and less than all of it.
			Map<String, String> parentByShade = new LinkedHashMap<>();
			parentByShade.put("бордовый", "красный");
			parentByShade.put("алый", "красный");
			parentByShade.put("малиновый", "красный");
			parentByShade.put("золотой", "жёлтый");
			parentByShade.put("бежевый", "жёлтый");
			parentByShade.put("салатовый", "лаймовый");
			parentByShade.put("светло-зелёный", "зелёный");
			parentByShade.put("тёмно-зелёный", "зелёный");
			parentByShade.put("изумрудный", "зелёный");
			parentByShade.put("тёмно-бирюзовый", "бирюзовый");
			parentByShade.put("небесный", "голубой");
			parentByShade.put("тёмно-синий", "синий");
			parentByShade.put("сиреневые", "фиолетовый");
			parentByShade.put("тёмно-серый", "серый");
			for (Map.Entry<String, String> entry : parentByShade.entrySet()) {
				String shade = entry.getKey();
				Set<String> parent = results.get(entry.getValue());
				Set<String> shown = search(context, input, shade);
				Set<String> outsideParent = new LinkedHashSet<>(shown);
				outsideParent.removeAll(parent);
				if (shown.isEmpty()) {
					failures.add("Query '" + shade + "' shows nothing");
				}
				if (!outsideParent.isEmpty()) {
					failures.add("Query '" + shade + "' shows " + outsideParent + " that '" + entry.getValue() + "' does not");
				}
				if (shown.size() >= parent.size()) {
					failures.add("Query '" + shade + "' is not narrower than '" + entry.getValue() + "'");
				}
			}

			// Empty query: the whole search tab, limited to items that existed in 1.21.11.
			check(failures, context, input, "",
				List.of("stone", "pale_oak_planks", "leaf_litter", "copper_chest", "iron_chain", "diamond_sword"),
				List.of("cinnabar", "sulfur", "poplar_planks", "red_cushion", "red_wool_stairs", "sulfur_cube_spawn_egg"));
			if (!failures.isEmpty()) {
				throw new AssertionError(String.join("\n", failures));
			}
		}
	}

	private static Set<String> search(ClientGameTestContext context, TestInput input, String query) {
		for (int i = 0; i < MAX_QUERY_LENGTH; i++) {
			input.pressKey(InputConstants.KEY_BACKSPACE);
		}
		input.typeChars(query);
		context.waitTicks(2);
		context.takeScreenshot("colorsearch-" + query);
		List<String> shown = context.computeOnClient(mc -> {
			List<String> ids = new ArrayList<>();
			// waitForScreen above guarantees the creative screen is still open.
			CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) mc.gui.screen();
			for (ItemStack stack : screen.getMenu().items) {
				ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
			}
			return ids;
		});
		LOG.info("Query '{}' shows {} items: {}", query, shown.size(), shown);
		return new LinkedHashSet<>(shown);
	}

	private static void checkOrder(List<String> failures, String query, Set<String> shown, String earlier, String later) {
		List<String> order = List.copyOf(shown);
		int earlierIndex = order.indexOf(earlier);
		int laterIndex = order.indexOf(later);
		if (earlierIndex < 0 || laterIndex < 0 || earlierIndex > laterIndex) {
			failures.add("Query '" + query + "' should list " + earlier + " (at " + earlierIndex + ") before "
				+ later + " (at " + laterIndex + ")");
		}
	}

	private static Set<String> check(
		List<String> failures, ClientGameTestContext context, TestInput input,
		String query, List<String> expected, List<String> unexpected
	) {
		Set<String> shown = search(context, input, query);
		for (String id : expected) {
			if (!shown.contains(id)) {
				failures.add("Query '" + query + "' should show " + id);
			}
		}
		for (String id : unexpected) {
			if (shown.contains(id)) {
				failures.add("Query '" + query + "' should not show " + id);
			}
		}
		return shown;
	}
}
