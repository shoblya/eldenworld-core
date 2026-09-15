# EldenWorld Core M3

Server-only Forge 1.20.1 companion mod for the EldenWorld Passive Skill Tree.

## Required server mods
- Forge 47.4.10+
- Passive Skill Tree 0.7.6e+
- Iron's Spells 'n Spellbooks 1.20.1-3.16.3+

The Core jar is intended to exist only on the dedicated server. `displayTest="IGNORE_SERVER_VERSION"` remains enabled; clients keep their normal modpack but do not need the EldenWorld Core jar.

## 18 keystone passives

### Rogue
- Shadowstep — at <=15% HP: safe backward blink, 4s invisibility, Speed II for 5s; 60s cooldown.
- Ghost — successful damage has 15% chance to grant 4s invisibility; 8s cooldown. The next hit consumes Core-created invisibility; PST supplies the invisibility crit bonuses.
- Opportunist — while in combat gains 1 stack every 3s, max 5; each stack gives +2% dodge and +2% crit chance. Resets after 8s out of combat.

### Warrior
- Juggernaut — damage dealt heals 2.5/4.5/7/10% depending on current HP; max 4 HP per hit.
- Unyielding — reflects 15% of incoming attacker damage, capped at 6; thorns reflection cannot recurse.
- Second Wind — lethal damage is canceled, restoring 35% max HP and giving Resistance II + Regeneration II for 5s; 5m cooldown.

### Ranger
- Keen Instinct — after 10s out of combat, the first incoming attack is canceled; 20s cooldown after activation.
- Windrunner — while sprinting in combat gains 1 stack every 2s, max 3; each gives +4% movement speed and +4% spell power. At 3 stacks also gets Jump Boost I.
- Pathfinder — permanent Jump Boost III while learned; fall damage is canceled.

### Mage
- Archmage — Iron's spell mana costs are increased by 30%; PST keeps the large mana/spell-power bonuses.
- Manaflow — magic damage dealt gives up to 5 stacks of +8% mana regen each (+40% max); stacks decay after the damage window expires.
- Arcane Ward — magic damage creates an 8s adaptive ward: +25% resistance to a detected school, or +15% generic spell resistance when the school cannot be detected.

### Builder
- Master Builder — +2 block reach. Consecutive block placement builds Rhythm up to 6 stacks, granting up to Haste III; at 4+ stacks also Speed I.
- Enduring Tools — breaking an ore repairs the held damageable tool by 2 durability.
- Prospector — ore streaks within 6s build up to 5 stacks, +5% mining speed each. Every ore also has a 15% chance to add one extra item from its normal drops.

### Adventurer
- Wayfarer — after 10s out of combat, Travel Mode grants Speed I + Jump Boost I.
- Treasure Hunter — first visit to each biome grants Luck II for 90s and +10 XP.
- Survivor — at <=30% HP: Resistance I + Regeneration II +10% dodge for 8s; 90s cooldown.

## PST descriptions
Use the paired datapack `EldenWorld_Passive_Tree_v7_3_M3_KEYSTONES.zip`. It changes only the 18 keystone descriptions from v7.2 and leaves node ids, bonuses, coordinates, connections, and all other nodes unchanged.

## Build
The repository includes a GitHub Actions workflow using Java 17 and Gradle 8.8. The expected output is `build/libs/EldenWorld-Core-0.3.0-m3.jar` after ForgeGradle/reobf succeeds.
