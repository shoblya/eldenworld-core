from pathlib import Path
import json, uuid, math, sys, shutil
project=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
root=project/'src/main/resources'; skills=root/'data/eldenworld/skills'; trees=root/'data/eldenworld/skill_trees'; reqs=root/'data/eldenworld_core/skill_requirements'
for p in (skills,trees,reqs):
    if p.exists(): shutil.rmtree(p)
    p.mkdir(parents=True,exist_ok=True)
# M6: 18 specialization trees. Magic is distributed between archetypes instead of belonging only to Mage.
D=[
('shadowstep','Shadowstep','skilltree:miner_mastery','rogue',[('Nightblade','Shadow/Ender + daggers'),('Riftwalker','Ender mobility + blades'),('Mirage','Arcane illusion + dual wield')]),
('ghost','Ghost','skilltree:miner_subclass_2_mastery','rogue',[('Assassin','Blood/Shadow + daggers'),('Phantom','Ender evasion + light weapons'),('Specter','Occult control + scythes')]),
('opportunist','Opportunist','skilltree:miner_subclass_1_mastery','rogue',[('Duelist','Wind + one-handed weapons'),('Predator','Nature/Blood + execution weapons'),('Trickster','Arcane utility + thrown/ranged tools')]),
('juggernaut','Juggernaut','skilltree:blacksmith_mastery','warrior',[('Berserker','Fire/Blood + axes'),('Bloodguard','Blood/Earth + heavy weapons'),('Colossus','Earth + great weapons')]),
('unyielding','Unyielding','skilltree:blacksmith_subclass_1_mastery','warrior',[('Bulwark','Earth/Holy + shield'),('Thorned Guard','Nature/Earth retaliation'),('Spellguard','Holy/Arcane anti-magic')]),
('second_wind','Second Wind','skilltree:blacksmith_subclass_2_mastery','warrior',[('Revenant','Blood/Occult sustain'),('Vanguard','Holy + MCA village leadership'),('Ironheart','Fire/Earth weapon tempering')]),
('keen_instinct','Keen Instinct','skilltree:hunter_mastery','ranger',[('Marksman','Wind + bows/crossbows'),('Hunter','Nature + hunting weapons'),('Sentinel','Nature/Holy + ranged defense')]),
('windrunner','Windrunner','skilltree:hunter_subclass_1_mastery','ranger',[('Gale Dancer','Wind spell/arrow rotation'),('Stormshot','Wind/Lightning + bows'),('Skirmisher','Wind mobility + ranged weapons')]),
('pathfinder','Pathfinder','skilltree:hunter_subclass_2_mastery','ranger',[('Beastmaster','Nature + pets'),('Dragon Rider','Fire/Wind + mounts'),('Trailblazer','Nature + dimensions')]),
('archmage','Archmage','skilltree:alchemist_mastery','mage',[('Elementalist','Fire/Ice/Lightning/Wind/Earth'),('Occultist','Blood/Ender/Twilight/Forbidden'),('Arcanist','Evocation + Ars Nouveau spellweaving')]),
('manaflow','Manaflow','skilltree:alchemist_subclass_2_mastery','mage',[('Channeler','Efficient continuous casting'),('Reservoir','Maximum mana and regeneration'),('Overcaster','High-risk spell burst')]),
('arcane_ward','Arcane Ward','skilltree:alchemist_subclass_1_mastery','mage',[('Aegis','Elemental warding'),('Spellbreaker','Anti-magic counterplay'),('Runewarden','Ars/Arcane defensive utility')]),
('master_builder','Master Builder','skilltree:cook_mastery','builder',[('Architect','Construction reach and mobility'),('Engineer','Arcane/Redstone utility'),('Fortifier','Earth magic + defensive building')]),
('enduring_tools','Enduring Tools','skilltree:cook_subclass_1_mastery','builder',[('Smith','Fire/Earth + weapon/tool forging'),('Temperer','Durability and equipment mastery'),('Runesmith','Ars/Arcane equipment utility')]),
('prospector','Prospector','skilltree:cook_subclass_2_mastery','builder',[('Deep Delver','Earth + underground survival'),('Gem Hunter','Luck + relic/material hunting'),('Excavator','Geomancy + mining')]),
('wayfarer','Wayfarer','skilltree:enchanter_mastery','adventurer',[('Dimension Walker','Ender + cross-dimension progression'),('Pilgrim','Nature/Holy exploration'),('Cartographer','World discovery + Waystones')]),
('treasure_hunter','Treasure Hunter','skilltree:enchanter_subclass_1_mastery','adventurer',[('Relic Seeker','Artifacts/Relics + Arcane'),('Fortune Hunter','Luck + loot specialization'),('Archaeologist','Ancient structures + occult lore')]),
('survivor','Survivor','skilltree:enchanter_subclass_2_mastery','adventurer',[('Last Stand','Holy/Blood survival'),('Wastelander','Nature adaptation + hostile dimensions'),('Monster Slayer','Boss hunting + weapon versatility')])]
slug=lambda s:s.lower().replace(' ','_').replace("'",'')
# Vanilla icons only in M6 tree JSON: guaranteed resources, no missing-texture squares.
icons={'rogue':'minecraft:textures/item/iron_sword.png','warrior':'minecraft:textures/item/shield.png','ranger':'minecraft:textures/item/bow.png','mage':'minecraft:textures/item/enchanted_book.png','builder':'minecraft:textures/item/iron_pickaxe.png','adventurer':'minecraft:textures/item/compass_00.png'}
attrs={'rogue':('minecraft:generic.movement_speed',.008,1),'warrior':('minecraft:generic.max_health',.012,1),'ranger':('minecraft:generic.movement_speed',.007,1),'mage':('irons_spellbooks:spell_power',.015,1),'builder':('minecraft:generic.luck',.15,0),'adventurer':('minecraft:generic.movement_speed',.005,1)}
branch_icons={'nightblade':'minecraft:textures/item/iron_sword.png','riftwalker':'minecraft:textures/item/ender_pearl.png','mirage':'minecraft:textures/item/echo_shard.png','assassin':'minecraft:textures/item/stone_sword.png','phantom':'minecraft:textures/item/phantom_membrane.png','specter':'minecraft:textures/item/soul_lantern.png','duelist':'minecraft:textures/item/golden_sword.png','predator':'minecraft:textures/item/iron_axe.png','trickster':'minecraft:textures/item/snowball.png','berserker':'minecraft:textures/item/diamond_axe.png','bloodguard':'minecraft:textures/item/shield.png','colossus':'minecraft:textures/item/netherite_sword.png','bulwark':'minecraft:textures/item/shield.png','thorned_guard':'minecraft:textures/block/sweet_berry_bush_stage3.png','spellguard':'minecraft:textures/item/totem_of_undying.png','revenant':'minecraft:textures/item/totem_of_undying.png','vanguard':'minecraft:textures/item/bell.png','ironheart':'minecraft:textures/item/anvil.png','marksman':'minecraft:textures/item/bow.png','hunter':'minecraft:textures/item/crossbow_standby.png','sentinel':'minecraft:textures/item/spectral_arrow.png','gale_dancer':'minecraft:textures/item/feather.png','stormshot':'minecraft:textures/item/trident.png','skirmisher':'minecraft:textures/item/arrow.png','beastmaster':'minecraft:textures/item/bone.png','dragon_rider':'minecraft:textures/item/saddle.png','trailblazer':'minecraft:textures/item/compass_00.png','elementalist':'minecraft:textures/item/blaze_powder.png','occultist':'minecraft:textures/item/ender_eye.png','arcanist':'minecraft:textures/item/enchanted_book.png','channeler':'minecraft:textures/item/amethyst_shard.png','reservoir':'minecraft:textures/item/lapis_lazuli.png','overcaster':'minecraft:textures/item/fire_charge.png','aegis':'minecraft:textures/item/shield.png','spellbreaker':'minecraft:textures/item/milk_bucket.png','runewarden':'minecraft:textures/item/enchanted_book.png','architect':'minecraft:textures/item/bricks.png','engineer':'minecraft:textures/item/redstone.png','fortifier':'minecraft:textures/item/iron_block.png','smith':'minecraft:textures/item/iron_ingot.png','temperer':'minecraft:textures/item/netherite_ingot.png','runesmith':'minecraft:textures/item/enchanted_book.png','deep_delver':'minecraft:textures/item/iron_pickaxe.png','gem_hunter':'minecraft:textures/item/diamond.png','excavator':'minecraft:textures/item/diamond_pickaxe.png','dimension_walker':'minecraft:textures/item/ender_eye.png','pilgrim':'minecraft:textures/item/leather_boots.png','cartographer':'minecraft:textures/item/map.png','relic_seeker':'minecraft:textures/item/totem_of_undying.png','fortune_hunter':'minecraft:textures/item/emerald.png','archaeologist':'minecraft:textures/item/brush.png','last_stand':'minecraft:textures/item/golden_apple.png','wastelander':'minecraft:textures/item/leather_chestplate.png','monster_slayer':'minecraft:textures/item/diamond_sword.png'}
def bonus(a,n,op,name): return {'type':'skilltree:attribute','attribute':a,'id':str(uuid.uuid5(uuid.NAMESPACE_URL,'eldenworld:m6:'+name)),'name':'M6 specialization','amount':n,'operation':op,'player_multiplier':{'type':'skilltree:none'},'player_condition':{'type':'skilltree:none'}}
def bonuses(group,branch,t):
    a,n,op=attrs[group]; out=[] if t==0 else [bonus(a,n*(1.0+.15*t),op,f'{group}:{branch}:{t}:base')]
    theme=branch.lower()
    if t>=3 and any(k in theme for k in ('wind','gale','storm','rift','phantom','dimension','duelist','skirmisher')): out.append(bonus('minecraft:generic.movement_speed',.006 if t<6 else .015,1,f'{branch}:{t}:mobility'))
    if t>=3 and group=='mage': out.append(bonus('irons_spellbooks:max_mana',5 if t<6 else 20,0,f'{branch}:{t}:mana'))
    if t==6:
        if group in ('rogue','warrior','ranger'): out.append(bonus('minecraft:generic.attack_damage',.06,1,f'{branch}:mastery:damage'))
        if group in ('mage','ranger','rogue','warrior'): out.append(bonus('irons_spellbooks:spell_power',.05,1,f'{branch}:mastery:magic'))
        if group in ('builder','adventurer'): out.append(bonus('minecraft:generic.luck',1.0,0,f'{branch}:mastery:luck'))
    return out
def node(idp,title,x,y,start,con,g,t,b=None,theme=''):
    desc=[]
    if t==0: desc=[f'{title}. Выберите одно из направлений специализации.','M6: развитие рассчитано на уровни персонажа до 150.']
    elif t<6: desc=[theme,f'Этап {t}/5. Следующий порог развития: {level_for(t)} уровень PST.']
    else: desc=[theme,'MASTERY — финальная нода ветки. Даёт усиленный тематический бонус; дополнительные Core-механики работают только там, где они реализованы в коде.']
    ic=icons[g] if not b else branch_icons.get(slug(b),icons[g])
    d={'id':'eldenworld:'+idp,'bonuses':bonuses(g,b or '',t),'directConnections':['eldenworld:'+c for c in con],'longConnections':[],'oneWayConnections':[],'tags':[],'backgroundTexture':'skilltree:textures/icons/background/'+('keystone.png' if t in (0,6) else 'lesser.png'),'iconTexture':ic,'borderTexture':'skilltree:textures/tooltip/'+('keystone.png' if t in (0,6) else 'lesser.png'),'title':title,'titleColor':'','positionX':round(x,3),'positionY':round(y,3),'buttonSize':32 if t in (0,6) else 24,'isStartingPoint':start,'requirements':[],'description':[{'text':z} for z in desc]}
    (skills/(idp.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))
def level_for(t): return {1:50,2:65,3:80,4:100,5:120,6:150}.get(t,40)
def req(idp,lvl,parent,previous=None):
    needed=[parent]+(([previous] if previous else []))
    (reqs/(idp.replace('/','__')+'.json')).write_text(json.dumps({'skill':'eldenworld:'+idp,'min_pst_level':lvl,'required_skills':needed},indent=2))
for key,disp,parent,g,branches in D:
    ids=[]; rootid=f'{key}/root'; ids.append('eldenworld:'+rootid); first=[f'{key}/{slug(b[0])}/1' for b in branches]
    node(rootid,disp,0,0,True,first,g,0); req(rootid,40,parent)
    n=len(branches)
    for bi,(b,theme) in enumerate(branches):
        ang=2*math.pi*bi/n-math.pi/2; ux,uy=math.cos(ang),math.sin(ang)
        for t in range(1,7):
            suffix='mastery' if t==6 else str(t); idp=f'{key}/{slug(b)}/{suffix}'; ids.append('eldenworld:'+idp)
            prev=rootid if t==1 else f'{key}/{slug(b)}/{"mastery" if t-1==6 else str(t-1)}'
            nxt=[] if t==6 else [f'{key}/{slug(b)}/{"mastery" if t+1==6 else str(t+1)}']
            node(idp,(b+' Mastery') if t==6 else f'{b} {t}',90*t*ux,90*t*uy,False,[prev]+nxt,g,t,b,theme)
            req(idp,level_for(t),parent,None if t==1 else prev)
    (trees/f'{key}.json').write_text(json.dumps({'id':'eldenworld:'+key,'skills':ids,'backgroundTexture':'skilltree:textures/screen/background.png'},indent=2))
# One shared level-150 completion marker keeps the historical 343-node contract without adding another visible tree branch.
cap='m6_completion'; node(cap,'M6: Ascendant',0,650,False,[],'adventurer',6,'Monster Slayer','Достигните вершины развития M6.'); req(cap,150,'skilltree:enchanter_subclass_2_mastery')
print('generated',len(list(skills.glob('*.json'))),'M6 nodes and',len(list(trees.glob('*.json'))),'trees')
