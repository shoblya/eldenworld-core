from pathlib import Path
import json, uuid, math, sys
project=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
root=project/'src/main/resources'
skills=root/'data/eldenworld/skills'; trees=root/'data/eldenworld/skill_trees'; reqs=root/'data/eldenworld_core/skill_requirements'
for p in (skills,trees,reqs): p.mkdir(parents=True,exist_ok=True)
D=[
('shadowstep','Shadowstep','skilltree:miner_mastery','rogue',['Riftwalker','Nightblade','Mirage']),
('ghost','Ghost','skilltree:miner_subclass_2_mastery','rogue',['Phantom','Assassin','Specter']),
('opportunist','Opportunist','skilltree:miner_subclass_1_mastery','rogue',['Duelist','Predator','Trickster']),
('juggernaut','Juggernaut','skilltree:blacksmith_mastery','warrior',['Berserker','Bloodguard','Colossus']),
('unyielding','Unyielding','skilltree:blacksmith_subclass_1_mastery','warrior',['Bulwark','Thorned Guard','Spellguard']),
('second_wind','Second Wind','skilltree:blacksmith_subclass_2_mastery','warrior',['Revenant','Vanguard','Ironheart']),
('keen_instinct','Keen Instinct','skilltree:hunter_mastery','ranger',['Marksman','Hunter','Sentinel']),
('windrunner','Windrunner','skilltree:hunter_subclass_1_mastery','ranger',['Skirmisher','Stormshot','Gale Dancer']),
('pathfinder','Pathfinder','skilltree:hunter_subclass_2_mastery','ranger',['Scout','Beastmaster','Dragon Rider','Trailblazer']),
('archmage','Archmage','skilltree:alchemist_mastery','mage',['Fire','Ice','Lightning','Wind','Earth','Blood','Holy','Ender','Evocation','Nature','Spellweaver','Dragon Magic','Forbidden Magic','Twilight Magic']),
('manaflow','Manaflow','skilltree:alchemist_subclass_2_mastery','mage',['Channeler','Reservoir','Overcaster']),
('arcane_ward','Arcane Ward','skilltree:alchemist_subclass_1_mastery','mage',['Aegis','Spellbreaker','Runewarden']),
('master_builder','Master Builder','skilltree:cook_mastery','builder',['Architect','Engineer','Fortifier']),
('enduring_tools','Enduring Tools','skilltree:cook_subclass_1_mastery','builder',['Smith','Temperer']),
('prospector','Prospector','skilltree:cook_subclass_2_mastery','builder',['Deep Delver','Gem Hunter','Excavator']),
('wayfarer','Wayfarer','skilltree:enchanter_mastery','adventurer',['Pilgrim','Dimension Walker','Cartographer']),
('treasure_hunter','Treasure Hunter','skilltree:enchanter_subclass_1_mastery','adventurer',['Relic Seeker','Fortune Hunter','Archaeologist']),
('survivor','Survivor','skilltree:enchanter_subclass_2_mastery','adventurer',['Last Stand','Wastelander','Monster Slayer'])]
slug=lambda s:s.lower().replace(' ','_').replace("'",'')
icons={'rogue':'minecraft:textures/item/iron_sword.png','warrior':'minecraft:textures/item/shield.png','ranger':'minecraft:textures/item/bow.png','mage':'minecraft:textures/item/enchanted_book.png','builder':'minecraft:textures/item/iron_pickaxe.png','adventurer':'minecraft:textures/item/compass_00.png'}
attrs={'rogue':('minecraft:generic.movement_speed',0.01,1),'warrior':('minecraft:generic.max_health',0.015,1),'ranger':('minecraft:generic.movement_speed',0.008,1),'mage':('irons_spellbooks:spell_power',0.018,1),'builder':('minecraft:generic.luck',0.20,0),'adventurer':('minecraft:generic.movement_speed',0.006,1)}
school={'fire':'fire','ice':'ice','lightning':'lightning','blood':'blood','holy':'holy','ender':'ender','evocation':'evocation','nature':'nature'}
def bonus(attr,amount,op,name):
 return {'type':'skilltree:attribute','attribute':attr,'id':str(uuid.uuid5(uuid.NAMESPACE_URL,'eldenworld:'+name)),'name':'EldenWorld M5','amount':amount,'operation':op,'player_multiplier':{'type':'skilltree:none'},'player_condition':{'type':'skilltree:none'}}
def write_node(idpath,title,x,y,start,connections,group,tier,branch=None):
 attr,amt,op=attrs[group]; bonuses=[] if tier==0 else [bonus(attr,amt*(1.5 if tier==5 else 1),op,idpath+':base')]
 if group=='mage' and branch and slug(branch) in school:
  a=f"irons_spellbooks:{school[slug(branch)]}_spell_power"; bonuses=[bonus(a,0.018 if tier<5 else 0.035,1,idpath+':school')]
 if tier==5:
  if group in ('rogue','warrior','ranger'): bonuses.append(bonus('minecraft:generic.attack_damage',0.035,1,idpath+':master'))
  elif group=='mage': bonuses.append(bonus('irons_spellbooks:max_mana',15,0,idpath+':mana'))
  elif group=='builder': bonuses.append(bonus('minecraft:generic.armor',1.0,0,idpath+':armor'))
  else: bonuses.append(bonus('minecraft:generic.luck',0.5,0,idpath+':luck'))
 desc=['Специализация EldenWorld M5.'] if tier==0 else [f'{branch}: ступень {tier}/5.','Использует общий пул очков Passive Skill Tree.']
 if tier==5: desc += ['MASTERY: финальная нода ветки.','Небольшой стат-бонус + механика Core, если предусмотрена для этой специализации.']
 d={'id':'eldenworld:'+idpath,'bonuses':bonuses,'directConnections':['eldenworld:'+c for c in connections],'longConnections':[],'oneWayConnections':[],'tags':[],'backgroundTexture':'skilltree:textures/icons/background/'+('keystone.png' if tier in (0,5) else 'lesser.png'),'iconTexture':icons[group],'borderTexture':'skilltree:textures/tooltip/'+('keystone.png' if tier in (0,5) else 'lesser.png'),'title':title,'titleColor':'','positionX':round(x,3),'positionY':round(y,3),'buttonSize':32 if tier in (0,5) else 24,'isStartingPoint':start,'requirements':[],'description':[{'text':t} for t in desc]}
 (skills/(idpath.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))
def req(idpath,level,parent):
 d={'skill':'eldenworld:'+idpath,'min_pst_level':level,'required_skills':[parent]}; (reqs/(idpath.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))
for key,disp,parent,group,branches in D:
 ids=[]; rootid=f'{key}/root'; ids.append('eldenworld:'+rootid); firsts=[f'{key}/{slug(b)}/1' for b in branches]
 write_node(rootid,disp+' — Specializations',0,0,True,firsts,group,0); req(rootid,40,parent); n=len(branches)
 for bi,b in enumerate(branches):
  angle=2*math.pi*bi/n-math.pi/2; ux,uy=math.cos(angle),math.sin(angle)
  for tier in range(1,6):
   idp=f'{key}/{slug(b)}/'+('mastery' if tier==5 else str(tier)); ids.append('eldenworld:'+idp)
   prev=rootid if tier==1 else f'{key}/{slug(b)}/'+str(tier-1)
   nxt=None if tier==5 else f'{key}/{slug(b)}/'+('mastery' if tier+1==5 else str(tier+1))
   con=[prev]+([nxt] if nxt else []); r=95*tier
   write_node(idp,(b+' Mastery') if tier==5 else f'{b} {tier}',ux*r,uy*r,False,con,group,tier,b); req(idp,40+tier*4,parent)
 (trees/(key+'.json')).write_text(json.dumps({'skillIds':ids,'title':disp,'iconTexture':icons[group]},ensure_ascii=False,indent=2))
print('M5 generated',len(D),'trees',len(list(skills.glob('*.json'))),'nodes')
