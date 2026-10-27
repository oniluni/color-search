package dev.oni.colorsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.DyeColor;

/**
 * Maps a search query to the colors it names. Main color words find the color with all its
 * shades; broad ones cover neighbouring dyes too ("blue" also means light blue, "зелёный" also
 * means lime). Shade words find only that shade ("бордовый" is dark red, not all red).
 */
public final class ColorFamily {
	// Russian adjective endings for every gender/number, so "синяя" and "синие" match "син".
	private static final List<String> RUSSIAN_ENDINGS = List.of("ий", "ый", "ой", "ая", "яя", "ое", "ее", "ые", "ие");
	private static final Map<String, ColorQuery> QUERIES_BY_WORD = buildQueriesByWord();

	private ColorFamily() {
	}

	/** The colors the whole query names, or empty if it is not a color word. */
	public static Optional<ColorQuery> parse(String query) {
		return Optional.ofNullable(QUERIES_BY_WORD.get(normalize(query)));
	}

	private static String normalize(String query) {
		return query.toLowerCase(Locale.ROOT)
			.replace('ё', 'е')
			.replaceAll("[_\\-\\s]+", " ")
			.trim();
	}

	private static Map<String, ColorQuery> buildQueriesByWord() {
		Map<String, ColorQuery> words = new HashMap<>();
		for (DyeColor color : DyeColor.values()) {
			words.put(normalize(color.getName()), ColorQuery.of(Set.of(color)));
		}
		words.put("light grey", ColorQuery.of(Set.of(DyeColor.LIGHT_GRAY)));

		// Broad English words override the exact dye entries above.
		ColorQuery greens = ColorQuery.of(Set.of(DyeColor.GREEN, DyeColor.LIME));
		ColorQuery grays = ColorQuery.of(Set.of(DyeColor.GRAY, DyeColor.LIGHT_GRAY));
		words.put("blue", ColorQuery.of(Set.of(DyeColor.BLUE, DyeColor.LIGHT_BLUE)));
		words.put("green", greens);
		words.put("gray", grays);
		words.put("grey", grays);
		putEnglish(words, ColorQuery.of(Set.of(DyeColor.PURPLE)), "violet");
		putEnglish(words, ColorQuery.of(Set.of(DyeColor.CYAN)), "turquoise", "aqua");

		putRussian(words, ColorQuery.of(Set.of(DyeColor.WHITE)), "бел");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.ORANGE)), "оранжев");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.MAGENTA)), "пурпурн");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.LIGHT_BLUE)), "голуб", "светло син");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.YELLOW)), "желт");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.LIME)), "лаймов");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.PINK)), "розов");
		putRussian(words, grays, "сер");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.LIGHT_GRAY)), "светло сер");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.CYAN)), "бирюзов");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.PURPLE)), "фиолетов", "лилов");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.BLUE)), "син");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.BROWN)), "коричнев");
		putRussian(words, greens, "зелен");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.RED)), "красн");
		putRussian(words, ColorQuery.of(Set.of(DyeColor.BLACK)), "черн");

		putShade(words, Shade.MAROON, List.of("maroon", "burgundy", "dark red"), List.of("бордов", "темно красн"));
		putShade(words, Shade.SCARLET, List.of("scarlet"), List.of("ал"));
		putShade(words, Shade.RASPBERRY, List.of("crimson", "raspberry"), List.of("малинов"));
		putShade(words, Shade.GOLD, List.of("gold", "golden"), List.of("золот"));
		putShade(words, Shade.BEIGE, List.of("beige", "tan"), List.of("бежев"));
		putShade(words, Shade.CHARTREUSE, List.of("chartreuse"), List.of("салатов"));
		putShade(words, Shade.LIGHT_GREEN, List.of("light green"), List.of("светло зелен"));
		putShade(words, Shade.DARK_GREEN, List.of("dark green"), List.of("темно зелен"));
		putShade(words, Shade.EMERALD, List.of("emerald"), List.of("изумрудн"));
		putShade(words, Shade.TEAL, List.of("teal"), List.of("темно бирюзов"));
		putShade(words, Shade.SKY_BLUE, List.of("sky blue"), List.of("небесн"));
		putShade(words, Shade.NAVY, List.of("navy", "dark blue"), List.of("темно син"));
		putShade(words, Shade.LILAC, List.of("lilac", "lavender"), List.of("сиренев"));
		putShade(words, Shade.DARK_GRAY, List.of("dark gray", "dark grey"), List.of("темно сер"));
		return Map.copyOf(words);
	}

	private static void putShade(Map<String, ColorQuery> words, Shade shade, List<String> english, List<String> russianStems) {
		ColorQuery query = ColorQuery.of(shade);
		putEnglish(words, query, english.toArray(String[]::new));
		putRussian(words, query, russianStems.toArray(String[]::new));
	}

	private static void putEnglish(Map<String, ColorQuery> words, ColorQuery query, String... names) {
		for (String name : names) {
			words.put(name, query);
		}
	}

	private static void putRussian(Map<String, ColorQuery> words, ColorQuery query, String... stems) {
		for (String stem : stems) {
			for (String ending : RUSSIAN_ENDINGS) {
				words.put(stem + ending, query);
			}
		}
	}
}
