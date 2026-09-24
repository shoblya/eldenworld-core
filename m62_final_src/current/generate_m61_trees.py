from pathlib import Path
import json, uuid, math, sys, shutil
project=Path(sys.argv[1]) if len(sys.argv)>1 else Path('.')
spec_path=Path(sys.argv[2]) if len(sys.argv)>2 else Path('ci/m6_specializations.json')
rows=json.loads(spec_path.read_text())
root=project/'src/main/resources'; skills=root/'data/eldenworld/skills'; trees=root/'data/eldenworld/skill_trees'; reqs=root/'data/eldenworld_core/skill_requirements'
for p in (skills,trees,reqs):
    if p.exists(): shutil.rmtree(p)
    p.mkdir(parents=True,exist_ok=True)
slug=lambda s:s.lower().replace(' ','_').replace("'",'').replace('/','_')
meta={
'shadowstep':('Shadowstep','skilltree:miner_mastery','rogue'),'ghost':('Ghost','skilltree:miner_subclass_2_mastery','rogue'),'opportunist':('Opportunist','skilltree:miner_subclass_1_mastery','rogue'),
'juggernaut':('Juggernaut','skilltree:blacksmith_mastery','warrior'),'unyielding':('Unyielding','skilltree:blacksmith_subclass_1_mastery','warrior'),'second_wind':('Second Wind','skilltree:blacksmith_subclass_2_mastery','warrior'),
'keen_instinct':('Keen Instinct','skilltree:hunter_mastery','ranger'),'windrunner':('Windrunner','skilltree:hunter_subclass_1_mastery','ranger'),'pathfinder':('Pathfinder','skilltree:hunter_subclass_2_mastery','ranger'),
'archmage':('Archmage','skilltree:alchemist_mastery','mage'),'manaflow':('Manaflow','skilltree:alchemist_subclass_2_mastery','mage'),'arcane_ward':('Arcane Ward','skilltree:alchemist_subclass_1_mastery','mage'),
'master_builder':('Builder','skilltree:cook_mastery','builder'),'enduring_tools':('Miner','skilltree:cook_subclass_1_mastery','builder'),'prospector':('Cook','skilltree:cook_subclass_2_mastery','builder'),
'wayfarer':('Wayfarer','skilltree:enchanter_mastery','adventurer'),'treasure_hunter':('Treasure Hunter','skilltree:enchanter_subclass_1_mastery','adventurer'),'survivor':('Survivor','skilltree:enchanter_subclass_2_mastery','adventurer')}
icons={'rogue':'minecraft:textures/item/iron_sword.png','warrior':'minecraft:textures/item/shield.png','ranger':'minecraft:textures/item/bow.png','mage':'minecraft:textures/item/enchanted_book.png','builder':'minecraft:textures/item/iron_pickaxe.png','adventurer':'minecraft:textures/item/compass_00.png'}
branch_icons={'nightblade':'minecraft:textures/item/iron_sword.png','riftwalker':'minecraft:textures/item/ender_pearl.png','mirage':'minecraft:textures/item/echo_shard.png','assassin':'minecraft:textures/item/stone_sword.png','phantom':'minecraft:textures/item/phantom_membrane.png','specter':'minecraft:textures/item/soul_lantern.png','duelist':'minecraft:textures/item/golden_sword.png','predator':'minecraft:textures/item/iron_axe.png','trickster':'minecraft:textures/item/snowball.png','berserker':'minecraft:textures/item/diamond_axe.png','bloodguard':'minecraft:textures/item/shield.png','colossus':'minecraft:textures/item/netherite_sword.png','bulwark':'minecraft:textures/item/shield.png','thorned_guard':'minecraft:textures/item/sweet_berries.png','spellguard':'minecraft:textures/item/totem_of_undying.png','revenant':'minecraft:textures/item/totem_of_undying.png','vanguard':'minecraft:textures/item/gold_ingot.png','ironheart':'minecraft:textures/item/iron_ingot.png','marksman':'minecraft:textures/item/bow.png','hunter':'minecraft:textures/item/crossbow_standby.png','sentinel':'minecraft:textures/item/spectral_arrow.png','gale_dancer':'minecraft:textures/item/feather.png','stormshot':'minecraft:textures/item/trident.png','skirmisher':'minecraft:textures/item/arrow.png','beastmaster':'minecraft:textures/item/bone.png','dragon_rider':'minecraft:textures/item/saddle.png','trailblazer':'minecraft:textures/item/compass_00.png','elementalist':'minecraft:textures/item/blaze_powder.png','occultist':'minecraft:textures/item/ender_eye.png','arcanist':'minecraft:textures/item/enchanted_book.png','channeler':'minecraft:textures/item/amethyst_shard.png','reservoir':'minecraft:textures/item/lapis_lazuli.png','overcaster':'minecraft:textures/item/fire_charge.png','aegis':'minecraft:textures/item/shield.png','spellbreaker':'minecraft:textures/item/milk_bucket.png','runewarden':'minecraft:textures/item/enchanted_book.png','architect':'minecraft:textures/item/brick.png','engineer':'minecraft:textures/item/redstone.png','fortifier':'minecraft:textures/item/iron_ingot.png','smith':'minecraft:textures/item/iron_ingot.png','temperer':'minecraft:textures/item/netherite_ingot.png','runesmith':'minecraft:textures/item/enchanted_book.png','deep_delver':'minecraft:textures/item/iron_pickaxe.png','gem_hunter':'minecraft:textures/item/diamond.png','excavator':'minecraft:textures/item/diamond_pickaxe.png','dimension_walker':'minecraft:textures/item/ender_eye.png','pilgrim':'minecraft:textures/item/leather_boots.png','cartographer':'minecraft:textures/item/map.png','relic_seeker':'minecraft:textures/item/totem_of_undying.png','fortune_hunter':'minecraft:textures/item/emerald.png','archaeologist':'minecraft:textures/item/brush.png','last_stand':'minecraft:textures/item/golden_apple.png','wastelander':'minecraft:textures/item/leather_chestplate.png','monster_slayer':'minecraft:textures/item/diamond_sword.png'}
branch_icons.update({
'stoneguard':'minecraft:textures/item/amethyst_shard.png',
'paladin':'minecraft:textures/item/golden_apple.png',
'technomancer':'minecraft:textures/item/comparator.png',
'prospector':'minecraft:textures/item/diamond.png',
'geomancer':'minecraft:textures/item/amethyst_shard.png',
'chef':'minecraft:textures/item/cooked_beef.png',
'brewer':'minecraft:textures/item/potion.png',
'feastmaster':'minecraft:textures/item/golden_carrot.png',
'broker_envoy':'minecraft:textures/item/emerald.png'
})
# Unified stat-node system: every branch has 4 small stats and one large stat, selected from a thematic profile.
profiles={
'offense':[('Точный удар','minecraft:generic.attack_damage',.03,1,'+3% урона атаки'),('Быстрые руки','minecraft:generic.attack_speed',.03,1,'+3% скорости атаки'),('Острый расчёт','attributeslib:crit_chance',.03,0,'+3% шанса критического удара'),('Лёгкий шаг','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Смертельная точность','attributeslib:crit_damage',.10,0,'+10% критического урона')],
'evasion':[('Лёгкий шаг','minecraft:generic.movement_speed',.025,1,'+2.5% скорости передвижения'),('Уклонение','attributeslib:dodge_chance',.025,0,'+2.5% шанса уклонения'),('Быстрые руки','minecraft:generic.attack_speed',.025,1,'+2.5% скорости атаки'),('Запас прочности','minecraft:generic.max_health',.02,1,'+2% максимального здоровья'),('Неуловимость','attributeslib:dodge_chance',.08,0,'+8% шанса уклонения')],
'warrior_offense':[('Сильный удар','minecraft:generic.attack_damage',.035,1,'+3.5% урона атаки'),('Закалка','minecraft:generic.max_health',.03,1,'+3% максимального здоровья'),('Тяжёлая броня','minecraft:generic.armor',1.0,0,'+1 брони'),('Темп боя','minecraft:generic.attack_speed',.025,1,'+2.5% скорости атаки'),('Мощь воина','minecraft:generic.attack_damage',.10,1,'+10% урона атаки')],
'tank':[('Закалка','minecraft:generic.max_health',.035,1,'+3.5% максимального здоровья'),('Тяжёлая броня','minecraft:generic.armor',1.0,0,'+1 брони'),('Устойчивость','minecraft:generic.knockback_resistance',.05,0,'+5% сопротивления отбрасыванию'),('Второе дыхание','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Крепость','minecraft:generic.armor',3.0,0,'+3 брони')],
'ranger':[('Сильный выстрел','attributeslib:arrow_damage',.04,1,'+4% урона стрел'),('Быстрая тетива','attributeslib:draw_speed',.04,1,'+4% скорости натяжения'),('Лёгкий шаг','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Точный выстрел','attributeslib:crit_chance',.025,0,'+2.5% шанса критического удара'),('Мастер выстрела','attributeslib:arrow_damage',.12,1,'+12% урона стрел')],
'mage':[('Сила заклинаний','irons_spellbooks:spell_power',.03,1,'+3% силы заклинаний'),('Запас маны','irons_spellbooks:max_mana',10,0,'+10 максимальной маны'),('Поток маны','irons_spellbooks:mana_regen',.05,1,'+5% восстановления маны'),('Сила заклинаний II','irons_spellbooks:spell_power',.025,1,'+2.5% силы заклинаний'),('Великий резерв','irons_spellbooks:max_mana',30,0,'+30 максимальной маны')],
'builder':[('Опыт шахтёра','attributeslib:mining_speed',.05,1,'+5% скорости добычи'),('Удачная находка','minecraft:generic.luck',.25,0,'+0.25 удачи'),('Длинная рука','forge:block_reach',.25,0,'+0.25 блока дальности взаимодействия'),('Рабочий темп','minecraft:generic.attack_speed',.025,1,'+2.5% скорости атаки'),('Мастер инструмента','attributeslib:mining_speed',.15,1,'+15% скорости добычи')],
'adventurer':[('Лёгкий путь','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Удача путника','minecraft:generic.luck',.25,0,'+0.25 удачи'),('Выносливость','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Дорожная броня','minecraft:generic.armor',.75,0,'+0.75 брони'),('Любимец судьбы','minecraft:generic.luck',1.0,0,'+1 удачи')]}

# More specific profiles reuse only real attributes verified in the installed 1.20.1 JARs.
profiles.update({
'light_weapon':[('Быстрые руки','minecraft:generic.attack_speed',.04,1,'+4% скорости атаки'),('Лёгкий шаг','minecraft:generic.movement_speed',.025,1,'+2.5% скорости передвижения'),('Критический шанс','attributeslib:crit_chance',.03,0,'+3% шанса критического удара'),('Острота','minecraft:generic.attack_damage',.025,1,'+2.5% урона атаки'),('Мастер лёгкого оружия','attributeslib:crit_damage',.12,0,'+12% критического урона')],
'duelist':[('Точность','attributeslib:crit_chance',.03,0,'+3% шанса критического удара'),('Темп','minecraft:generic.attack_speed',.035,1,'+3.5% скорости атаки'),('Шаг дуэлянта','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Пробитие','attributeslib:armor_pierce',1.0,0,'+1 пробивания брони'),('Фехтовальщик','attributeslib:armor_pierce',3.0,0,'+3 пробивания брони')],
'heavy_weapon':[('Тяжёлый удар','minecraft:generic.attack_damage',.05,1,'+5% урона атаки'),('Стойкость','minecraft:generic.knockback_resistance',.06,0,'+6% сопротивления отбрасыванию'),('Пробитие','attributeslib:armor_pierce',1.0,0,'+1 пробивания брони'),('Закалка','minecraft:generic.max_health',.03,1,'+3% максимального здоровья'),('Сокрушитель','attributeslib:armor_shred',.12,0,'+12% разрушения брони')],
'pet':[('Связь','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Путь зверя','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Природная магия','irons_spellbooks:nature_spell_power',.04,1,'+4% силы Nature-заклинаний'),('Выживание','minecraft:generic.armor',.75,0,'+0.75 брони'),('Повелитель зверей','irons_spellbooks:summon_damage',.15,1,'+15% урона призванных существ')],
'mount':[('Всадник','minecraft:generic.movement_speed',.02,1,'+2% скорости передвижения'),('Закалка','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Огненная связь','irons_spellbooks:fire_spell_power',.04,1,'+4% силы Fire-заклинаний'),('Воздушная связь','irons_spellbooks:casting_movespeed',.04,1,'+4% скорости во время каста'),('Драконий всадник','irons_spellbooks:fire_spell_power',.12,1,'+12% силы Fire-заклинаний')],
'elementalist':[('Огонь','irons_spellbooks:fire_spell_power',.03,1,'+3% Fire Spell Power'),('Лёд','irons_spellbooks:ice_spell_power',.03,1,'+3% Ice Spell Power'),('Молния','irons_spellbooks:lightning_spell_power',.03,1,'+3% Lightning Spell Power'),('Природа','irons_spellbooks:nature_spell_power',.03,1,'+3% Nature Spell Power'),('Стихийная мощь','irons_spellbooks:spell_power',.10,1,'+10% общей силы заклинаний')],
'occult':[('Кровь','irons_spellbooks:blood_spell_power',.04,1,'+4% Blood Spell Power'),('Эндер','irons_spellbooks:ender_spell_power',.04,1,'+4% Ender Spell Power'),('Элдрич','irons_spellbooks:eldritch_spell_power',.04,1,'+4% Eldritch Spell Power'),('Запас маны','irons_spellbooks:max_mana',12,0,'+12 максимальной маны'),('Запретная мощь','irons_spellbooks:spell_power',.11,1,'+11% силы заклинаний')],
'mana':[('Запас маны','irons_spellbooks:max_mana',12,0,'+12 максимальной маны'),('Поток маны','irons_spellbooks:mana_regen',.05,1,'+5% восстановления маны'),('Скорость каста','irons_spellbooks:cast_time_reduction',.04,1,'+4% сокращения времени каста'),('Движение в касте','irons_spellbooks:casting_movespeed',.04,1,'+4% скорости во время каста'),('Великий поток','irons_spellbooks:max_mana',35,0,'+35 максимальной маны')],
'spellguard':[('Сопротивление магии','irons_spellbooks:spell_resist',.04,1,'+4% сопротивления магии'),('Запас здоровья','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Сопротивление школе','irons_spellbooks:spell_resist',.035,1,'+3.5% сопротивления магии'),('Броня','minecraft:generic.armor',.75,0,'+0.75 брони'),('Антимаг','irons_spellbooks:spell_resist',.12,1,'+12% сопротивления магии')],
'survival':[('Выносливость','minecraft:generic.max_health',.03,1,'+3% максимального здоровья'),('Броня','minecraft:generic.armor',1.0,0,'+1 брони'),('Получаемое лечение','attributeslib:healing_received',.04,1,'+4% получаемого лечения'),('Устойчивость','minecraft:generic.knockback_resistance',.05,0,'+5% сопротивления отбрасыванию'),('Живучесть','minecraft:generic.max_health',.10,1,'+10% максимального здоровья')],
'relic':[('Удача','minecraft:generic.luck',.3,0,'+0.3 удачи'),('Выносливость','minecraft:generic.max_health',.025,1,'+2.5% максимального здоровья'),('Критический шанс','attributeslib:crit_chance',.025,0,'+2.5% шанса критического удара'),('Сила заклинаний','irons_spellbooks:spell_power',.025,1,'+2.5% силы заклинаний'),('Охотник за реликвиями','minecraft:generic.luck',1.25,0,'+1.25 удачи')]
})

def bonus(attr,amount,op,key):
 return {'type':'skilltree:attribute','attribute':attr,'id':str(uuid.uuid5(uuid.NAMESPACE_URL,'eldenworld:m62:'+key)),'name':'EldenWorld M6.2 stat','amount':amount,'operation':op,'player_multiplier':{'type':'skilltree:none'},'player_condition':{'type':'skilltree:none'}}
def write_node(idp,title,x,y,start,connections,group,kind,branch=None,desc='',stat=None,extra_bonuses=None):
 ic=icons[group] if branch is None else branch_icons.get(slug(branch),icons[group])
 bonuses=[]
 if stat:
  n,a,v,o,text=stat; bonuses=[bonus(a,v,o,idp)]
  desc=f'{text}. Постоянный бонус этой ветки.'
 if extra_bonuses:
  for j,(a,v,o,text) in enumerate(extra_bonuses):
   if abs(v)>1e-9: bonuses.append(bonus(a,v,o,idp+':downside:'+str(j)))
 d={'id':'eldenworld:'+idp,'bonuses':bonuses,'directConnections':['eldenworld:'+c for c in connections],'longConnections':[],'oneWayConnections':[],'tags':[],'backgroundTexture':'skilltree:textures/icons/background/'+('keystone.png' if kind in ('start','specialization','big','mastery') else 'lesser.png'),'iconTexture':ic,'borderTexture':'skilltree:textures/tooltip/'+('keystone.png' if kind in ('start','specialization','big','mastery') else 'lesser.png'),'title':title,'titleColor':'','positionX':round(x,3),'positionY':round(y,3),'buttonSize':32 if kind in ('start','specialization','big','mastery') else 24,'isStartingPoint':start,'requirements':[],'description':[{'text':desc}]}
 (skills/(idp.replace('/','__')+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2))
def req(idp,lvl,parent,previous=None):
 needed=[parent]+(([previous] if previous else []))
 (reqs/(idp.replace('/','__')+'.json')).write_text(json.dumps({'skill':'eldenworld:'+idp,'min_pst_level':lvl,'required_skills':needed},indent=2))
by_tree={k:[] for k in meta}
for r in rows: by_tree[r['tree']].append(r)
levels=[0,0,0,0,0,0,0]
for key,(disp,parent,group) in meta.items():
 branches=by_tree[key]; assert len(branches)==3
 rootid=f'{key}/root'; ids=['eldenworld:'+rootid]
 first=[f"{key}/{slug(r['branch'])}/stat_1" for r in branches]
 write_node(rootid,disp,0,0,True,first,group,'start',desc=f'{disp}: стартовая точка. Выберите одну из трёх специализаций. Внутренние ноды ограничены порядком ветки и стоимостью skill points, без отдельного level-gate.')
 req(rootid,40,parent)
 for bi,r in enumerate(branches):
  b=r['branch']; profile=profiles[r['profile']]; ang=2*math.pi*bi/3-math.pi/2; ux,uy=math.cos(ang),math.sin(ang)
  seq=[('stat_1',profile[0][0],'stat',profile[0]),('stat_2',profile[1][0],'stat',profile[1]),('specialization',b,'specialization',None),('stat_3',profile[2][0],'stat',profile[2]),('stat_4',profile[3][0],'stat',profile[3]),('big_stat',profile[4][0],'big',profile[4]),('mastery',b+' Mastery','mastery',None)]
  prev=rootid
  for i,(suffix,title,kind,stat) in enumerate(seq):
   idp=f'{key}/{slug(b)}/{suffix}'; ids.append('eldenworld:'+idp)
   nxt=[] if i==6 else [f'{key}/{slug(b)}/{seq[i+1][0]}']
   desc=r['specialization'] if kind=='specialization' else (r['mastery'] if kind=='mastery' else '')
   extra=r.get('downside') if kind=='mastery' and isinstance(r.get('downside'),list) else None
   write_node(idp,title,82*(i+1)*ux,82*(i+1)*uy,False,[prev]+nxt,group,kind,b,desc,stat,extra)
   req(idp,levels[i],parent,prev); prev=idp
 (trees/f'{key}.json').write_text(json.dumps({'id':'eldenworld:'+key,'skills':ids,'backgroundTexture':'skilltree:textures/screen/background.png'},indent=2))
count=len(list(skills.glob('*.json')))
assert count==396,count
assert len(list(trees.glob('*.json')))==18
alltext='\n'.join(p.read_text() for p in skills.glob('*.json'))
for bad in ('SPECIALIZATION:','MASTERY: более','STAT:','BIG STAT:','Реальный эффект реализуется'):
 assert bad not in alltext,bad
for legacy in ('Thorned Guard','Vanguard','Runewarden','Archaeologist','Smith Mastery','Temperer Mastery','Runesmith Mastery','Gem Hunter','Excavator Mastery'):
 assert legacy not in alltext,legacy
# Internal specialization-tree nodes deliberately have no separate character-level gate.
for p in reqs.glob('*.json'):
 data=json.loads(p.read_text())
 if data['skill'].endswith('/root'):
  assert data['min_pst_level']==40
 else:
  assert data['min_pst_level']==0
print('generated',count,'nodes, 18 trees, 54 M6.2 specialization/mastery descriptions; topology preserved; inner level gates disabled')
