# Color Search

A client-side Fabric mod for Minecraft 26.3 that lets the creative search tab find blocks by color.

Type a color word, in English or Russian, and the tab lists every block with at least 25% of its
texture in that color, from the lightest to the darkest shade of it, followed by the remaining
name matches (dyes, bundles, …).

- **Main colors** are the 16 dye colors: `red` / `красный`, `light blue` / `голубой`, `lime` /
  `лаймовый`, … Russian words work in any gender and number (`синий`, `синяя`, `синие`).
- **Broad words** cover neighbouring dyes: `blue` = blue + light blue, `green` / `зелёный` =
  green + lime, `gray` / `серый` = gray + light gray.
- **Shades** narrow a main color: `maroon` / `бордовый`, `scarlet` / `алый`, `crimson` /
  `малиновый`, `gold` / `золотой`, `beige` / `бежевый`, `chartreuse` / `салатовый`,
  `light green` / `светло-зелёный`, `dark green` / `тёмно-зелёный`, `emerald` / `изумрудный`,
  `teal` / `тёмно-бирюзовый`, `sky blue` / `небесный`, `navy` / `тёмно-синий`,
  `lilac` / `сиреневый`, `dark gray` / `тёмно-серый`. A main color always finds all of its
  shades; a shade finds only itself.
- Blocks named after a dye (`blue_terracotta`, `red_bed`) always match that dye, even when the
  texture looks different.

Colors are computed from the loaded resource packs, so texture packs change the results.

> **The creative search tab lists only items that existed in Minecraft 1.21.11.** Items added in
> 26.x and items from other mods are hidden from the search tab (other tabs are unaffected).

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Java 25

## Building

```sh
./gradlew build                # jar in build/libs/
./gradlew runClientGameTest    # in-game test: types every color word and checks the results
```

## How it works

Every opaque texture pixel of the block's model is converted to OKLCh and classified into
overlapping hue/chroma/lightness regions, one per dye color (`PixelClassifier`), with shades as
sub-regions of their parent color (`Shade`). Grass and leaves are tinted with their default color
first. The item list in `src/client/resources/colorsearch/items-1.21.11.txt` comes from the
vanilla 1.21.11 data generator report (`registries.json`).

## License

MIT
