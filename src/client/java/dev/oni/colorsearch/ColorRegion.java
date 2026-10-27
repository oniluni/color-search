package dev.oni.colorsearch;

/**
 * A box in OKLCh space. The hue range is [hueFrom, hueTo) in degrees and wraps past 360
 * when hueFrom > hueTo.
 */
record ColorRegion(
	float hueFrom, float hueTo,
	float minChroma, float maxChroma,
	float minLightness, float maxLightness
) {
	static final float ANY = Float.MAX_VALUE;

	/** Any hue and chroma, lightness in [minLightness, maxLightness). */
	static ColorRegion lightness(float minLightness, float maxLightness) {
		return new ColorRegion(0, 360, 0, ANY, minLightness, maxLightness);
	}

	boolean contains(double hue, double chroma, double lightness) {
		boolean inHue = hueFrom < hueTo ? hue >= hueFrom && hue < hueTo : hue >= hueFrom || hue < hueTo;
		return inHue
			&& chroma >= minChroma && chroma < maxChroma
			&& lightness >= minLightness && lightness < maxLightness;
	}
}
