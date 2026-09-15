# EldenWorld M6 — progression architecture

## Global rules
- Character/PST progression target: 1–150.
- World Level = arithmetic mean of PST levels of every online player on the server, regardless of distance or dimension.
- Specialization UI must expose only trees unlocked by the player's main-tree keystones.
- Display names never include the `EldenWorld` namespace/prefix.
- No description-only mastery: a Mastery is not final until its effect exists in Core.
- Every icon must resolve to a texture that exists in the installed pack; vanilla-safe icons are used as fallback.

## Cross-system rule
Magic is not Mage-exclusive. Each archetype gets magic that supports its identity rather than replacing it.

### Rogue
- Shadowstep: daggers/dual wield + Ender/Shadow mobility; spell -> weapon and weapon -> spell loops.
- Ghost: illusion/occult magic + stealth; evasive casting and ambush windows.
- Opportunist: dueling/trick weapons + light Blood/Arcane utility; rewards exploiting debuffs and openings.

### Warrior
- Juggernaut: heavy weapons + Fire/Earth/Blood battle magic.
- Unyielding: shield/tank weapons + Earth/Holy warding and anti-magic.
- Second Wind: melee sustain + Holy/Fire rally magic; party-facing Vanguard path can interact with villages/MCA relations.

### Ranger
- Keen Instinct: bows/crossbows + Nature magic; tracking, marks and hunting.
- Windrunner: ranged/light weapons + Wind magic; movement and alternating shot/spell rotations.
- Pathfinder: Nature/Wind utility + pets + exploration. Beastmaster supports owned pets; Dragon Rider is reserved for real dragon integration.

### Mage
Archmage no longer has one branch per school. It has three broad disciplines:
- Elemental: Fire, Ice, Lightning, Wind, Earth/Geomancy.
- Occult: Blood, Ender, Forbidden/Cataclysm, Twilight.
- Arcane: Evocation, pure mana, Ars Nouveau spellweaving.
Manaflow focuses on resource/casting engines. Arcane Ward focuses on wards, counters and spell defense.

### Builder
- Master Builder: construction/engineering + Earth/Geomancy and Ars utility.
- Enduring Tools: smithing/tool mastery + enchantment/earth reinforcement.
- Prospector: mining + geomancy + dimension-specific resources.

### Adventurer
- Wayfarer: dimensions + Ender/Nature utility; bonuses earned through actual exploration.
- Treasure Hunter: Relics/Artifacts + archaeology + dimension loot.
- Survivor: adaptation to biomes/dimensions, monster hunting and defensive utility magic.

## Example M6 mastery loops
- Gale Dancer: bow hit empowers Wind spell; Wind spell empowers next ranged hit.
- Nightblade: dagger hit builds Shadow charge; Ender/Shadow cast consumes it for mobility/ambush payoff.
- Beast Lord: Nature support effects applied by the player can also benefit the owned pet; pet combat feeds a player buff.
- Vanguard: defending allied villagers/settlements builds Rally; Rally powers party-defense effects. MCA-specific behavior must only ship after API/event verification.
- Planeswalker: visiting distinct dimensions unlocks permanent exploration milestones; Ender utility scales with milestones.
- Grand Weaver: Ars Nouveau casting patterns feed Arcane stacks rather than being a generic spell-power branch.
