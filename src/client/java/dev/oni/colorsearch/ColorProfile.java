package dev.oni.colorsearch;

import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.OptionalDouble;
import java.util.function.IntPredicate;

/**
 * Color statistics of a texture or a block. For every color mask (see
 * {@link PixelClassifier#maskOf}) it keeps the share of opaque pixels with exactly that mask and
 * their summed OKLab lightness, both as fractions of all opaque pixels. Mask 0 holds pixels of no
 * named color. Filled while it is built, then only read.
 */
final class ColorProfile {
	private final Int2FloatOpenHashMap shares = new Int2FloatOpenHashMap();
	private final Int2FloatOpenHashMap lightnessSums = new Int2FloatOpenHashMap();

	void add(int mask, float share, float lightnessSum) {
		shares.addTo(mask, share);
		lightnessSums.addTo(mask, lightnessSum);
	}

	void addAll(ColorProfile other) {
		for (Int2FloatMap.Entry entry : other.shares.int2FloatEntrySet()) {
			add(entry.getIntKey(), entry.getFloatValue(), other.lightnessSums.get(entry.getIntKey()));
		}
	}

	void scale(float factor) {
		for (Int2FloatMap.Entry entry : shares.int2FloatEntrySet()) {
			entry.setValue(entry.getFloatValue() * factor);
		}
		for (Int2FloatMap.Entry entry : lightnessSums.int2FloatEntrySet()) {
			entry.setValue(entry.getFloatValue() * factor);
		}
	}

	/** Share of opaque pixels in any of the colors of {@code colorMask}. */
	float share(int colorMask) {
		float share = 0;
		for (Int2FloatMap.Entry entry : shares.int2FloatEntrySet()) {
			if ((entry.getIntKey() & colorMask) != 0) {
				share += entry.getFloatValue();
			}
		}
		return share;
	}

	/** Mean lightness of the pixels in any of the colors of {@code colorMask}; empty if there are none. */
	OptionalDouble lightness(int colorMask) {
		return meanLightness(mask -> (mask & colorMask) != 0);
	}

	/** Mean lightness of all opaque pixels; empty if there are none. */
	OptionalDouble overallLightness() {
		return meanLightness(mask -> true);
	}

	private OptionalDouble meanLightness(IntPredicate includesMask) {
		float share = 0;
		float lightnessSum = 0;
		for (Int2FloatMap.Entry entry : shares.int2FloatEntrySet()) {
			if (includesMask.test(entry.getIntKey())) {
				share += entry.getFloatValue();
				lightnessSum += lightnessSums.get(entry.getIntKey());
			}
		}
		return share > 0 ? OptionalDouble.of(lightnessSum / share) : OptionalDouble.empty();
	}
}
