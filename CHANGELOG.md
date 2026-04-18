# Changelog

## [0.5.3] - 2026-04-18
### Fixed
- Implemented boss detection in `BossReplacedRenderer` using Armor Stand custom names with the `[BF:` prefix and the `bossframework:boss_id` persistent tag.
- Updated the Fabric 1.21.1 mod-side GeckoLib and resource pack integration so `mod/gradlew build` succeeds again.
- Aligned the documented and declared Paper/Fabric/Minecraft versions to 1.21.1.
- Removed dead client mixins by deleting `ResourcePackManagerMixin` and the empty `CustomPayloadMixin`, and clearing the mixin list.
- Sent `bossframework:despawn` to players tracking active bosses before plugin reload or disable to prevent ghost bosses on the client.
