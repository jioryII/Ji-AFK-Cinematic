# Compatible Resource-Pack Music

Ji-AFK Cinematic automatically respects resource packs that replace the Vanilla music events it already uses. No additional configuration is required for replacements.

If you only want to play downloaded `.ogg` files from a folder on your computer, follow [Using Your Own Local Music](LOCAL-MUSIC.md) instead.

To add new music to the cinematic track pool, enable a music mode that includes custom tracks and declare the sound events in your resource pack.

## Required structure

```text
assets/<namespace>/sounds.json
assets/<namespace>/sounds/<path>/<track>.ogg
assets/<namespace>/ji_afk_cinematic/music.json
```

The namespace and sound paths may be chosen by the pack author, but every declared event must exist in `sounds.json`.

## Sound event

For long music files, use Ogg Vorbis (`.ogg`) and enable streaming:

```json
{
  "music.cinematic.sunrise": {
    "sounds": [
      {
        "name": "my_pack:music/sunrise",
        "stream": true
      }
    ]
  }
}
```

This example maps the event `my_pack:music.cinematic.sunrise` to:

```text
assets/my_pack/sounds/music/sunrise.ogg
```

## Ji-AFK Cinematic manifest

Create `assets/<namespace>/ji_afk_cinematic/music.json`:

```json
{
  "replace": false,
  "tracks": [
    "my_pack:music.cinematic.sunrise",
    "my_pack:music.cinematic.night"
  ]
}
```

Object entries are also accepted:

```json
{
  "replace": false,
  "tracks": [
    { "sound": "my_pack:music.cinematic.sunrise" }
  ]
}
```

## Manifest rules

- `tracks` contains Minecraft sound-event identifiers in `namespace:path` format.
- Invalid identifiers and unreadable manifests are ignored and reported in the game log.
- Duplicate identifiers are included only once.
- `replace: false` adds the declared events to compatible manifests already processed.
- `replace: true` clears custom events processed before the current manifest, then adds the current list. It does not remove Ji-AFK Cinematic's Vanilla pool.
- When packs provide the same manifest resource, Minecraft's active resource-pack priority determines their processing order.

After installing or editing a pack, reload resources with `F3 + T` or use **Refresh Music** in Ji-AFK Cinematic's settings. Declared tracks are available in **Mixed** and **Custom** modes; **Vanilla** mode ignores them.
