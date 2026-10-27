package dev.oni.colorsearch;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;

/**
 * Decides which dye colors and shades a texture pixel counts as. Each dye color is a region of
 * OKLCh (hue angle, chroma, lightness); regions overlap on purpose so in-between pixels count
 * for every color they plausibly belong to: brick is both red and brown, ice both blue and
 * light blue. {@link Shade}s narrow their parent color further. Bounds were tuned against the
 * vanilla block textures.
 */
public final class PixelClassifier {
	private static final int OPAQUE_ALPHA = 128;
	private static final float ANY = ColorRegion.ANY;
	private static final List<DyeRegion> DYE_REGIONS = List.of(
		dye(DyeColor.RED, 345, 42, 0.035f, ANY, 0.15f, 1),
		dye(DyeColor.ORANGE, 40, 72, 0.09f, ANY, 0.45f, 1),
		dye(DyeColor.BROWN, 20, 100, 0.025f, 0.13f, 0.2f, 0.7f),
		dye(DyeColor.YELLOW, 72, 112, 0.05f, ANY, 0.6f, 1),
		dye(DyeColor.LIME, 112, 150, 0.1f, ANY, 0.55f, 1),
		dye(DyeColor.GREEN, 112, 172, 0.04f, ANY, 0.15f, 1),
		dye(DyeColor.CYAN, 172, 215, 0.04f, ANY, 0.15f, 1),
		dye(DyeColor.LIGHT_BLUE, 210, 265, 0.05f, ANY, 0.6f, 1),
		// Pale cyan-ish blues (diamond, beacon) read as light blue; darker teals (warped) stay cyan only.
		dye(DyeColor.LIGHT_BLUE, 185, 210, 0.05f, ANY, 0.75f, 1),
		dye(DyeColor.BLUE, 245, 295, 0.05f, ANY, 0.15f, 1),
		dye(DyeColor.PURPLE, 285, 330, 0.05f, ANY, 0.15f, 1),
		dye(DyeColor.MAGENTA, 315, 350, 0.08f, ANY, 0.3f, 1),
		dye(DyeColor.PINK, 330, 32, 0.04f, ANY, 0.65f, 1),
		dye(DyeColor.WHITE, 0, 360, 0, 0.045f, 0.82f, 1),
		dye(DyeColor.LIGHT_GRAY, 0, 360, 0, 0.035f, 0.55f, 0.88f),
		dye(DyeColor.GRAY, 0, 360, 0, 0.035f, 0.3f, 0.7f),
		dye(DyeColor.BLACK, 0, 360, 0, 0.07f, 0, 0.3f)
	);
	private static final double[] SRGB_TO_LINEAR = new double[256];

	static {
		if (DyeColor.values().length + Shade.values().length > Integer.SIZE) {
			throw new IllegalStateException("Dye colors and shades no longer fit the int color mask");
		}
		for (int channel = 0; channel < SRGB_TO_LINEAR.length; channel++) {
			double c = channel / 255.0;
			SRGB_TO_LINEAR[channel] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
		}
	}

	private PixelClassifier() {
	}

	/** Bit for {@code color} in the color masks returned by {@link #histogram}. */
	public static int maskOf(DyeColor color) {
		return 1 << color.ordinal();
	}

	/** Bit for {@code shade} in the color masks returned by {@link #histogram}; shades follow the dye bits. */
	public static int maskOf(Shade shade) {
		return 1 << (DyeColor.values().length + shade.ordinal());
	}

	/**
	 * Fraction of opaque pixels per color mask: the key is the set of colors and shades a pixel counts as
	 * (see {@link #maskOf}), so overlapping colors can be combined without double counting.
	 * Every pixel is multiplied by {@code tint} first (use {@code -1} for no tint).
	 * Empty when the pixels contain nothing opaque.
	 */
	public static Optional<Int2FloatMap> histogram(int[] argbPixels, int tint) {
		Int2FloatOpenHashMap counts = new Int2FloatOpenHashMap();
		int opaque = 0;
		for (int pixel : argbPixels) {
			if (ARGB.alpha(pixel) < OPAQUE_ALPHA) {
				continue;
			}
			opaque++;
			int mask = colorMask(ARGB.multiply(pixel, tint));
			if (mask != 0) {
				counts.addTo(mask, 1);
			}
		}
		if (opaque == 0) {
			return Optional.empty();
		}
		for (Int2FloatMap.Entry entry : counts.int2FloatEntrySet()) {
			entry.setValue(entry.getFloatValue() / opaque);
		}
		return Optional.of(counts);
	}

	private static int colorMask(int argb) {
		// sRGB → OKLab → OKLCh, per https://bottosson.github.io/posts/oklab/
		double r = SRGB_TO_LINEAR[ARGB.red(argb)];
		double g = SRGB_TO_LINEAR[ARGB.green(argb)];
		double b = SRGB_TO_LINEAR[ARGB.blue(argb)];
		double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
		double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
		double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
		double lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
		double labA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
		double labB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
		double chroma = Math.hypot(labA, labB);
		double hue = Math.toDegrees(Math.atan2(labB, labA));
		if (hue < 0) {
			hue += 360;
		}

		int mask = 0;
		for (DyeRegion dyeRegion : DYE_REGIONS) {
			if (dyeRegion.region().contains(hue, chroma, lightness)) {
				mask |= maskOf(dyeRegion.color());
			}
		}
		for (Shade shade : Shade.values()) {
			if ((mask & maskOf(shade.parent())) != 0 && shade.contains(hue, chroma, lightness)) {
				mask |= maskOf(shade);
			}
		}
		return mask;
	}

	private static DyeRegion dye(
		DyeColor color, float hueFrom, float hueTo,
		float minChroma, float maxChroma, float minLightness, float maxLightness
	) {
		return new DyeRegion(color, new ColorRegion(hueFrom, hueTo, minChroma, maxChroma, minLightness, maxLightness));
	}

	private record DyeRegion(DyeColor color, ColorRegion region) {
	}
}
