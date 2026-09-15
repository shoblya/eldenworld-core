from pathlib import Path
import json, uuid, math, sys
project=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
root=project/'src/main/resources'; skills=root/'data/eldenworld/skills'; trees=root/'data/eldenworld/skill_trees'; reqs=root/'data/eldenworld_core/skill_requirements'
for p in (skills,trees,reqs): p.mkdir(parents=True,exist_ok=True)
D=[
('shadowstep','Shadowstep','skilltree:miner_mastery','rogue',['Riftwalker','Nightblade','Mirage']),('ghost','Ghost','skilltree:miner_subclass_2_mastery','rogue',['Phantom','Assassin','Specter']),('opportunist','Opportunist','skilltree:miner_subclass_1_mastery','rogue',['Duelist','Predator','Trickster']),('juggernaut','Juggernaut','skilltree:blacksmith_mastery','warrior',['Berserker','Bloodguard','Colossus']),('unyielding','Unyielding','skilltree:blacksmith_subclass_1_mastery','warrior',['Bulwark','Thorned Guard','Spellguard']),('second_wind','Second Wind','skilltree:blacksmith_subclass_2_mastery','warrior',['Revenant','Vanguard','Ironheart']),('keen_instinct','Keen Instinct','skilltree:hunter_mastery','ranger',['Marksman','Hunter','Sentinel']),('windrunner','Windrunner','skilltree:hunter_subclass_1_mastery','ranger',['Skirmisher','Stormshot','Gale Dancer']),('pathfinder','Pathfinder','skilltree:hunter_subclass_2_mastery','ranger',['Scout','Beastmaster','Dragon Rider','Trailblazer']),('archmage','Archmage','skilltree:alchemist_mastery','mage',['Fire','Ice','Lightning','Wind','Earth','Blood','Holy','Ender','Evocation','Nature','Spellweaver','Dragon Magic','Forbidden Magic','Twilight Magic']),('manaflow','Manaflow','skilltree:alchemist_subclass_2_mastery','mage',['Channeler','Reservoir','Overcaster']),('arcane_ward','Arcane Ward','skilltree:alchemist_subclass_1_mastery','mage',['Aegis','Spellbreaker','Runewarden']),('master_builder','Master Builder','skilltree:cook_mastery','builder',['Architect','Engineer','Fortifier']),('enduring_tools','Enduring Tools','skilltree:cook_subclass_1_mastery','builder',['Smith','Temperer']),('prospector','Prospector','skilltree:cook_subclass_2_mastery','builder',['Deep Delver','Gem Hunter','Excavator']),('wayfarer','Wayfarer','skilltree:enchanter_mastery','adventurer',['Pilgrim','Dimension Walker','Cartographer']),('treasure_hunter','Treasure Hunter','skilltree:enchanter_subclass_1_mastery','adventurer',['Relic Seeker','Fortune Hunter','Archaeologist']),('survivor','Survivor','skilltree:enchanter_subclass_2_mastery','adventurer',['Last Stand','Wastelander','Monster Slayer'])]
slug=lambda s:s.lower().replace(' ','_').replace("'",'')
icons={'rogue':'minecraft:textures/item/iron_sword.png','warrior':'minecraft:textures/item/shield.png','ranger':'minecraft:textures/item/bow.png','mage':'minecraft:textures/item/enchanted_book.png','builder':'minecraft:textures/item/iron_pickaxe.png','adventurer':'minecraft:textures/item/compass_16.png'}
attrs={'rogue':('minecraft:generic.movement_speed',.01,1),'warrior':('minecraft:generic.max_health',.015,1),'ranger':('minecraft:generic.movement_speed',.008,1),'mage':('irons_spellbooks:spell_power',.018,1),'builder':('minecraft:generic.luck',.20,0),'adventurer':('minecraft:generic.movement_speed',.006,1)}
school={x:x for x in ['fire','ice','lightning','blood','holy','ender','evocation','nature']}
def bonus(a,n,op,name): return {'type':'skilltree:attribute','attribute':a,'id':str(uuid.uuid5(uuid.NAMESPACE_URL,'eldenworld:'+name)),'name':'EldenWorld M5','amount':n,'operation':op,'player_multiplier':{'type':'skilltree:none'},'player_condition':{'type':'skilltree:none'}}
def node(idp,title,x,y,start,con,g,t,b=None):
 a,n,op=attrs[g]; bs=[] if t==0 else [bonus(a,n*(1.5 if t==5 else 1),op,idp+':base')]
 if g=='mage' and b and slug(b) in school: bs=[bonus('irons_spellbooks:'+school[slug(b)]+'_spell_power',.018 if t<5 else .035,1,idp+':school')]
 if t==5:
  if g in ('rogue','warrior','ranger'): bs.append(bonus('minecraft:generic.attack_damage',.035,1,idp+':master'))
  elif g=='mage': bs.append(bonus('irons_spellbooks:max_mana',15,0,idp+':mana'))
  elif g=='builder': bs.append(bonus('minecraft:generic.armor',1,0,idp+':armor'))
  else: bs.append(bonus('minecraft:generic.luck',.5,0,idp+':luck'))
 desc=['Специализация EldenWorld M5.'] if t==0 else [f'{b}: ступень {t}/5.','Использует общий пул очков Passive Skill Tree.']
 if t==5: desc+=['MASTERY: финальная нода ветки.','Открывает механику Core, если она предусмотрена для этой специализации.']
 d={'id':'eldenworld:'+idp,'bonuses':bs,'directConnections':['eldenworld:'+c for c in con],'longConnections':[],'oneWayConnections':[],'tags':[],'backgroundTexture':'skilltree:textures/icons/background/'+('keystone.png' if t in (0,5) else 'lesser.png'),'iconTexture':icons[g],'borderTexture':'skilltree:textures/tooltip/'+('keystone.png' if t in (0,5) else 'lesser.png'),'title':title,'titleColor':'','positionX':round(x,3),'positionY':round(y,3),'buttonSize':32 if t in (0,5) else 24,'isStartingPoint':start,'requirements':[],'description':[{'text':z} for z in desc]}
 (skills/(idp.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))
def req(idp,lvl,parent): (reqs/(idp.replace('/','__')+'.json')).write_text(json.dumps({'skill':'eldenworld:'+idp,'min_pst_level':lvl,'required_skills':[parent]},indent=2))
for key,disp,parent,g,branches in D:
 ids=[]; rootid=f'{key}/root'; ids.append('eldenworld:'+rootid); first=[f'{key}/{slug(b)}/1' for b in branches]; node(rootid,disp+' — Specializations',0,0,True,first,g,0); req(rootid,40,parent); n=len(branches)
 for bi,b in enumerate(branches):
  ang=2*math.pi*bi/n-math.pi/2; ux,uy=math.cos(ang),math.sin(ang)
  for t in range(1,6):
   idp=f'{key}/{slug(b)}/'+('mastery' if t==5 else str(t)); ids.append('eldenworld:'+idp); prev=rootid if t==1 else f'{key}/{slug(b)}/{t-1}'; nxt=[] if t==5 else [f'{key}/{slug(b)}/'+('mastery' if t==4 else str(t+1))]; con=([prev] if prev else [])+nxt; node(idp,(b+' Mastery') if t==5 else f'{b} {t}',95*t*ux,95*t*uy,False,con,g,t,b); req(idp,40+4*t,parent)
 (trees/f'{key}.json').write_text(json.dumps({'id':'eldenworld:'+key,'skills':ids,'backgroundTexture':'skilltree:textures/screen/background.png'},indent=2))
print('generated',len(list(skills.glob('*.json'))),'M5 nodes and',len(list(trees.glob('*.json'))),'trees')
