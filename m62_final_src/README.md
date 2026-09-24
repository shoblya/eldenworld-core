# EldenWorld M6.2 final source workspace

This directory is generated once from the packed RC source so M6.2 FINAL can be edited and reviewed as normal text.

- current/ = unpacked source bundle used by the RC workflow.
- overrides/ = decompressed Java overrides from m62_tools.
- The Passive Skill Tree topology is not to be redesigned for M6.2 FINAL.

## M6.2.2 restored progression build

- Correct all 378 internal `required_skills` references to use `eldenworld:`.
- Preserve 18 subclass roots, 54 specializations, 54 masteries and the existing
  subclass slot thresholds (40 / 60 / 80). No additional character-level gate
  is added to internal skills.
- Generate a distinct 16×16 class/attribute/branch icon for each of 396 nodes.
  These textures ship inside the Core JAR, which the client must load for icons.
- World Tier stores the highest character level ever observed in a world,
  capped at 150. It updates every second, never drops when players leave,
  and reads the original M6.2 persistent world record after a server restart.
- Preserve a mob's current health fraction on tier changes and chunk reloads.
- Include configurable entity tags for mob classification; known registry IDs
  can be added to the Mob Scaling datapack without recompiling Core.
- Treasure Hunter retains the 30% base trade discount from the M6.2
  specialization specification; the root tooltip now states its actual value.

Build validation checks all 396 links and corresponding PNGs. In-game combat,
mod compatibility and client rendering require a separate playtest.
