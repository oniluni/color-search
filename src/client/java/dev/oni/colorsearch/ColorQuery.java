package dev.oni.colorsearch;

import java.util.Set;
import net.minecraft.world.item.DyeColor;

/**
 * What a color word searches for: pixels in any of the colors of {@code pixelMask}
 * (see {@link PixelClassifier#maskOf}), plus blocks whose id names one of {@code namedDyes}.
 * Shades search by pixels only; no block id names a shade.
 */
public record ColorQuery(int pixelMask, Set<DyeColor> namedDyes) {
	public static ColorQuery of(Set<DyeColor> dyes) {
		int mask = 0;
		for (DyeColor dye : dyes) {
			mask |= PixelClassifier.maskOf(dye);
		}
		return new ColorQuery(mask, dyes);
	}

	public static ColorQuery of(Shade shade) {
		return new ColorQuery(PixelClassifier.maskOf(shade), Set.of());
	}
}
