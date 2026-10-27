package dev.oni.colorsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The items that existed in Minecraft 1.21.11, the last 1.21.x release. Creative search hides
 * everything else: items added in 26.x and items from mods.
 */
public final class Release1211Items {
	private static final String ITEM_LIST = "/colorsearch/items-1.21.11.txt";
	private static final Set<Identifier> IDS = load();

	private Release1211Items() {
	}

	public static boolean contains(ItemStack stack) {
		return IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}

	private static Set<Identifier> load() {
		InputStream input = Release1211Items.class.getResourceAsStream(ITEM_LIST);
		if (input == null) {
			throw new IllegalStateException("Color Search jar is missing " + ITEM_LIST + "; rebuild the mod");
		}
		Set<Identifier> ids = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank() && !line.startsWith("#")) {
					ids.add(Identifier.parse(line.strip()));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + ITEM_LIST + " from the Color Search jar", e);
		}
		return Set.copyOf(ids);
	}
}
