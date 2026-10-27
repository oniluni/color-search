package dev.oni.colorsearch;

import net.minecraft.world.item.DyeColor;

/**
 * A named shade of one dye color. A pixel counts as a shade only when it already counts as
 * the parent color, so searching the parent always finds every block its shades find.
 */
public enum Shade {
	MAROON(DyeColor.RED, ColorRegion.lightness(0, 0.42f)),
	// Bright red, but darker than pink starts (lightness 0.65).
	SCARLET(DyeColor.RED, new ColorRegion(0, 360, 0.14f, ColorRegion.ANY, 0.42f, 0.65f)),
	RASPBERRY(DyeColor.RED, new ColorRegion(345, 12, 0.06f, ColorRegion.ANY, 0, 1)),
	GOLD(DyeColor.YELLOW, new ColorRegion(0, 360, 0.12f, ColorRegion.ANY, 0, 1)),
	BEIGE(DyeColor.YELLOW, new ColorRegion(0, 360, 0, 0.09f, 0.7f, 1)),
	CHARTREUSE(DyeColor.LIME, ColorRegion.lightness(0.7f, 1)),
	LIGHT_GREEN(DyeColor.GREEN, ColorRegion.lightness(0.7f, 1)),
	DARK_GREEN(DyeColor.GREEN, ColorRegion.lightness(0, 0.45f)),
	// Saturated bluish green; plain plant stems stay below this chroma.
	EMERALD(DyeColor.GREEN, new ColorRegion(140, 172, 0.14f, ColorRegion.ANY, 0, 1)),
	TEAL(DyeColor.CYAN, ColorRegion.lightness(0, 0.55f)),
	SKY_BLUE(DyeColor.LIGHT_BLUE, ColorRegion.lightness(0.75f, 1)),
	NAVY(DyeColor.BLUE, ColorRegion.lightness(0, 0.4f)),
	LILAC(DyeColor.PURPLE, ColorRegion.lightness(0.6f, 1)),
	DARK_GRAY(DyeColor.GRAY, ColorRegion.lightness(0, 0.45f));

	private final DyeColor parent;
	private final ColorRegion region;

	Shade(DyeColor parent, ColorRegion region) {
		this.parent = parent;
		this.region = region;
	}

	public DyeColor parent() {
		return parent;
	}

	boolean contains(double hue, double chroma, double lightness) {
		return region.contains(hue, chroma, lightness);
	}
}
