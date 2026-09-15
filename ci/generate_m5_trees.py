from pathlib import Path
import json, uuid, math, sys, shutil
project=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
root=project/'src/main/resources'; skills=root/'data/eldenworld/skills'; trees=root/'data/eldenworld/skill_trees'; reqs=root/'data/eldenworld_core/skill_requirements'
for p in (skills,trees,reqs):
    if p.exists(): shutil.rmtree(p)
    p.mkdir(parents=True,exist_ok=True)

# M6 fixed layout for EVERY specialization branch:
# START -> STAT I -> STAT II -> SPECIALIZATION -> STAT III -> STAT IV -> BIG STAT -> MASTERY
# Specialization/Mastery mechanics are implemented by Core; datapack nodes only expose the agreed progression/stat skeleton.
D=[
('shadowstep','Shadowstep','skilltree:miner_mastery','rogue',[('Nightblade','Shadow/Ender + daggers'),('Riftwalker','Ender mobility + blades'),('Mirage','Ars Nouveau decoy/illusion')]),
('ghost','Ghost','skilltree:miner_subclass_2_mastery','rogue',[('Assassin','Blood/Shadow + daggers'),('Phantom','Ender/Ars evasion'),('Specter','Occult control + scythes')]),
('opportunist','Opportunist','skilltree:miner_subclass_1_mastery','rogue',[('Duelist','Wind + one-handed weapons'),('Predator','Nature/Blood hunting'),('Trickster','Ars utility')]),
('juggernaut','Juggernaut','skilltree:blacksmith_mastery','warrior',[('Berserker','Fire/Blood + axes'),('Bloodguard','Blood/Earth + heavy weapons'),('Colossus','Earth + great weapons')]),
('unyielding','Unyielding','skilltree:blacksmith_subclass_1_mastery','warrior',[('Bulwark','Earth/Holy + shield'),('Thorned Guard','Nature/Earth retaliation'),('Spellguard','Iron spells anti-magic')]),
('second_wind','Second Wind','skilltree:blacksmith_subclass_2_mastery','warrior',[('Revenant','Blood/Occult survival'),('Vanguard','Holy + MCA allies'),('Ironheart','Fire/Earth weapon tempering')]),
('keen_instinct','Keen Instinct','skilltree:hunter_mastery','ranger',[('Marksman','Wind + bows/crossbows'),('Hunter','Nature hunting'),('Sentinel','Nature/Holy ranged defense')]),
('windrunner','Windrunner','skilltree:hunter_subclass_1_mastery','ranger',[('Gale Dancer','Wind + arrows'),('Stormshot','Lightning + bows'),('Skirmisher','Wind mobility + ranged weapons')]),
('pathfinder','Pathfinder','skilltree:hunter_subclass_2_mastery','ranger',[('Beastmaster','Nature + pets'),('Dragon Rider','Fire/Wind + mounts'),('Trailblazer','Nature + dimensions')]),
('archmage','Archmage','skilltree:alchemist_mastery','mage',[('Elementalist','Fire/Ice/Lightning/Wind/Earth'),('Occultist','Blood/Ender/Twilight/Forbidden'),('Arcanist','Ars Nouveau + Arcane')]),
('manaflow','Manaflow','skilltree:alchemist_subclass_2_mastery','mage',[('Channeler','Efficient casting'),('Reservoir','Mana reserve'),('Overcaster','High-risk spell burst')]),
('arcane_ward','Arcane Ward','skilltree:alchemist_subclass_1_mastery','mage',[('Aegis','Elemental warding'),('Spellbreaker','Iron spells anti-magic'),('Runewarden','Ars defensive utility')]),
('master_builder','Master Builder','skilltree:cook_mastery','builder',[('Architect','Ars construction utility'),('Engineer','Ars/Redstone utility'),('Fortifier','Earth + defensive building')]),
('enduring_tools','Enduring Tools','skilltree:cook_subclass_1_mastery','builder',[('Smith','Apothic gear + forging'),('Temperer','Durability mastery'),('Runesmith','Ars equipment utility')]),
('prospector','Prospector','skilltree:cook_subclass_2_mastery','builder',[('Deep Delver','Earth + underground survival'),('Gem Hunter','Rare materials + relics'),('Excavator','Geomancy + mining')]),
('wayfarer','Wayfarer','skilltree:enchanter_mastery','adventurer',[('Dimension Walker','Ender + Waystones/dimensions'),('Pilgrim','Nature/Holy exploration'),('Cartographer','World discovery + Waystones')]),
('treasure_hunter','Treasure Hunter','skilltree:enchanter_subclass_1_mastery','adventurer',[('Relic Seeker','Artifacts/Relics'),('Fortune Hunter','Luck + loot'),('Archaeologist','Structures + exploration')]),
('survivor','Survivor','skilltree:enchanter_subclass_2_mastery','adventurer',[('Last Stand','Holy/Blood survival'),('Wastelander','Environmental adaptation'),('Monster Slayer','Boss/elite hunting')])]
slug=lambda s:s.lower().replace(' ','_').replace("'",'')
icons={'rogue':'minecraft:textures/item/iron_sword.png','warrior':'minecraft:textures/item/shield.png','ranger':'minecraft:textures/item/bow.png','mage':'minecraft:textures/item/enchanted_book.png','builder':'minecraft:textures/item/iron_pickaxe.png','adventurer':'minecraft:textures/item/compass_00.png'}
branch_icons={'nightblade':'minecraft:textures/item/iron_sword.png','riftwalker':'minecraft:textures/item/ender_pearl.png','mirage':'minecraft:textures/item/echo_shard.png','assassin':'minecraft:textures/item/stone_sword.png','phantom':'minecraft:textures/item/phantom_membrane.png','specter':'minecraft:textures/item/soul_lantern.png','duelist':'minecraft:textures/item/golden_sword.png','predator':'minecraft:textures/item/iron_axe.png','trickster':'minecraft:textures/item/snowball.png','berserker':'minecraft:textures/item/diamond_axe.png','bloodguard':'minecraft:textures/item/shield.png','colossus':'minecraft:textures/item/netherite_sword.png','bulwark':'minecraft:textures/item/shield.png','thorned_guard':'minecraft:textures/item/sweet_berries.png','spellguard':'minecraft:textures/item/totem_of_undying.png','revenant':'minecraft:textures/item/totem_of_undying.png','vanguard':'minecraft:textures/item/gold_ingot.png','ironheart':'minecraft:textures/item/iron_ingot.png','marksman':'minecraft:textures/item/bow.png','hunter':'minecraft:textures/item/crossbow_standby.png','sentinel':'minecraft:textures/item/spectral_arrow.png','gale_dancer':'minecraft:textures/item/feather.png','stormshot':'minecraft:textures/item/trident.png','skirmisher':'minecraft:textures/item/arrow.png','beastmaster':'minecraft:textures/item/bone.png','dragon_rider':'minecraft:textures/item/saddle.png','trailblazer':'minecraft:textures/item/compass_00.png','elementalist':'minecraft:textures/item/blaze_powder.png','occultist':'minecraft:textures/item/ender_eye.png','arcanist':'minecraft:textures/item/enchanted_book.png','channeler':'minecraft:textures/item/amethyst_shard.png','reservoir':'minecraft:textures/item/lapis_lazuli.png','overcaster':'minecraft:textures/item/fire_charge.png','aegis':'minecraft:textures/item/shield.png','spellbreaker':'minecraft:textures/item/milk_bucket.png','runewarden':'minecraft:textures/item/enchanted_book.png','architect':'minecraft:textures/item/brick.png','engineer':'minecraft:textures/item/redstone.png','fortifier':'minecraft:textures/item/iron_ingot.png','smith':'minecraft:textures/item/iron_ingot.png','temperer':'minecraft:textures/item/netherite_ingot.png','runesmith':'minecraft:textures/item/enchanted_book.png','deep_delver':'minecraft:textures/item/iron_pickaxe.png','gem_hunter':'minecraft:textures/item/diamond.png','excavator':'minecraft:textures/item/diamond_pickaxe.png','dimension_walker':'minecraft:textures/item/ender_eye.png','pilgrim':'minecraft:textures/item/leather_boots.png','cartographer':'minecraft:textures/item/map.png','relic_seeker':'minecraft:textures/item/totem_of_undying.png','fortune_hunter':'minecraft:textures/item/emerald.png','archaeologist':'minecraft:textures/item/brush.png','last_stand':'minecraft:textures/item/golden_apple.png','wastelander':'minecraft:textures/item/leather_chestplate.png','monster_slayer':'minecraft:textures/item/diamond_sword.png'}
attrs={'rogue':('minecraft:generic.movement_speed',.008,1),'warrior':('minecraft:generic.max_health',.012,1),'ranger':('minecraft:generic.movement_speed',.007,1),'mage':('irons_spellbooks:spell_power',.015,1),'builder':('minecraft:generic.luck',.15,0),'adventurer':('minecraft:generic.movement_speed',.005,1)}

def bonus(a,n,op,name): return {'type':'skilltree:attribute','attribute':a,'id':str(uuid.uuid5(uuid.NAMESPACE_URL,'eldenworld:m6:'+name)),'name':'Specialization stat','amount':n,'operation':op,'player_multiplier':{'type':'skilltree:none'},'player_condition':{'type':'skilltree:none'}}
def stat_bonuses(group,branch,kind,index):
    a,n,op=attrs[group]
    if kind in ('start','specialization'): return []
    mult=1.0 + .18*index
    if kind=='big': mult=3.0
    if kind=='mastery': mult=1.7
    out=[bonus(a,n*mult,op,f'{group}:{branch}:{kind}:{index}')]
    if kind=='big' and group in ('rogue','warrior','ranger'): out.append(bonus('minecraft:generic.attack_damage',.035,1,f'{branch}:big:damage'))
    if kind=='big' and group=='mage': out.append(bonus('irons_spellbooks:max_mana',15,0,f'{branch}:big:mana'))
    return out

def node(idp,title,x,y,start,connections,g,kind,index=0,b=None,theme=''):
    if kind=='start': desc=[f'{title}. Выберите специализацию.','Схема ветки: 2 stat -> specialization -> 2 stat -> big stat -> mastery.']
    elif kind=='specialization': desc=[theme,'SPECIALIZATION: простая классовая способность. Реальный эффект реализуется EldenWorld Core/интеграцией с указанным модом.']
    elif kind=='mastery': desc=[theme,'MASTERY: более сильная простая способность + реальный дебаф/ограничение. Без дополнительных мини-систем.']
    elif kind=='big': desc=[theme,'BIG STAT: усиленная статовая нода перед Mastery.']
    else: desc=[theme,'STAT: небольшая статовая нода специализации.']
    ic=icons[g] if not b else branch_icons.get(slug(b),icons[g])
    d={'id':'eldenworld:'+idp,'bonuses':stat_bonuses(g,b or '',kind,index),'directConnections':['eldenworld:'+c for c in connections],'longConnections':[],'oneWayConnections':[],'tags':[],'backgroundTexture':'skilltree:textures/icons/background/'+('keystone.png' if kind in ('start','specialization','big','mastery') else 'lesser.png'),'iconTexture':ic,'borderTexture':'skilltree:textures/tooltip/'+('keystone.png' if kind in ('start','specialization','big','mastery') else 'lesser.png'),'title':title,'titleColor':'','positionX':round(x,3),'positionY':round(y,3),'buttonSize':32 if kind in ('start','specialization','big','mastery') else 24,'isStartingPoint':start,'requirements':[],'description':[{'text':z} for z in desc]}
    (skills/(idp.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))

def req(idp,lvl,parent,previous=None):
    needed=[parent]+(([previous] if previous else []))
    (reqs/(idp.replace('/','__')+'.json')).write_text(json.dumps({'skill':'eldenworld:'+idp,'min_pst_level':lvl,'required_skills':needed},indent=2))

# Per branch, root is the START and seven following nodes produce the exact requested eight-node path.
# Thresholds keep the 1-150 M6 progression while putting specialization early and Mastery at 150.
steps=[
('stat_1','STAT I','stat',50),
('stat_2','STAT II','stat',60),
('specialization','Specialization','specialization',70),
('stat_3','STAT III','stat',85),
('stat_4','STAT IV','stat',105),
('big_stat','BIG STAT','big',125),
('mastery','Mastery','mastery',150),
]
for key,disp,parent,g,branches in D:
    ids=[]; rootid=f'{key}/root'; ids.append('eldenworld:'+rootid)
    first=[f'{key}/{slug(b[0])}/stat_1' for b in branches]
    node(rootid,disp,0,0,True,first,g,'start'); req(rootid,40,parent)
    n=len(branches)
    for bi,(b,theme) in enumerate(branches):
        ang=2*math.pi*bi/n-math.pi/2; ux,uy=math.cos(ang),math.sin(ang)
        prev=rootid
        for i,(suffix,label,kind,lvl) in enumerate(steps,1):
            idp=f'{key}/{slug(b)}/{suffix}'; ids.append('eldenworld:'+idp)
            nxt=[] if i==len(steps) else [f'{key}/{slug(b)}/{steps[i][0]}']
            title=b if kind=='specialization' else (b+' Mastery' if kind=='mastery' else f'{b} {label}')
            node(idp,title,82*i*ux,82*i*uy,False,[prev]+nxt,g,kind,i,b,theme)
            req(idp,lvl,parent,prev)
            prev=idp
    (trees/f'{key}.json').write_text(json.dumps({'id':'eldenworld:'+key,'skills':ids,'backgroundTexture':'skilltree:textures/screen/background.png'},indent=2))

count=len(list(skills.glob('*.json')))
# 18 roots + 54 branches * 7 nodes = 396 nodes. No orphan completion placeholder.
assert count==396, count
assert len(list(trees.glob('*.json')))==18
print('generated',count,'M6 nodes and 18 trees with fixed 8-node branch layout')
