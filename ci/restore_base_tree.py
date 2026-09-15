"""Combine the preserved 589-node base with the generated subclass pack."""
import hashlib
import json
import re
import sys
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ci = Path(__file__).resolve().parent
pack = Path(sys.argv[1])
resources = Path(sys.argv[2])
base = ci / 'base_tree_v7_3.zip'
assert hashlib.sha256(base.read_bytes()).hexdigest() == '2ea70b0e2d23beefda5029986581ce77332210cd4fd5fce56ee4c4f2b9cabb7d'
with ZipFile(base) as archive:
    preserved = {n: archive.read(n) for n in archive.namelist()
                 if n.startswith('data/') and not n.endswith('/')}
with ZipFile(pack) as archive:
    content = {n: archive.read(n) for n in archive.namelist() if not n.endswith('/')}
assert not content.keys() & preserved.keys(), 'Base and subclass files overlap'
content.update(preserved)
# The M6 generator omitted the namespace on preceding subclass-node requirements.
# Minecraft otherwise interprets these as minecraft:..., blocking progression.
for name, data in list(content.items()):
    if '/skill_requirements/' in name and name.endswith('.json'):
        req = json.loads(data)
        req['required_skills'] = [s if ':' in s else 'eldenworld:' + s
                                  for s in req.get('required_skills', [])]
        content[name] = json.dumps(req, indent=2).encode()
# Override the obsolete v7.1 tree if an older pack is still present below this one.
content['data/skilltree/skill_trees/main_tree.json'] = json.dumps(
    {'id': 'skilltree:main_tree', 'skillIds': [], 'skillLimitations': {}}).encode()
content['pack.mcmeta'] = json.dumps({'pack': {'pack_format': 15,
    'description': 'EldenWorld M6.1.5: shared 589-node tree + 18 gated subclass trees'}}).encode()

skills = {json.loads(v)['id']: json.loads(v) for n, v in content.items()
          if '/skills/' in n and n.endswith('.json')}
trees = {json.loads(v)['id']: json.loads(v) for n, v in content.items()
         if '/skill_trees/' in n and n.endswith('.json')}
base_ids = set(trees['skilltree:soldier']['skillIds'])
assert len(base_ids) == 589
assert len(skills) == 985
starts = {sid for sid in base_ids if skills[sid].get('isStartingPoint')}
assert starts == {'skilltree:new_skill_2'}
seen = set(starts)
pending = list(starts)
while pending:
    node = skills[pending.pop()]
    for field in ('directConnections', 'longConnections', 'oneWayConnections'):
        for other in node.get(field, []):
            assert other in base_ids, (node['id'], other)
            if other not in seen:
                seen.add(other)
                pending.append(other)
assert seen == base_ids
assert len([t for t in trees.values() if t['skillIds']]) == 19
for tree in trees.values():
    assert set(tree['skillIds']) <= skills.keys()

selector = (ci / 'm613_client/EldenWorldSkillTreeSelectionScreen.java').read_text()
unlocks = dict(re.findall(r'unlock\(map, "([^"]+)", "([^"]+)"\)', selector))
assert len(unlocks) == 18
requirements = [json.loads(v) for n, v in content.items()
                if '/skill_requirements/' in n and n.endswith('.json')]
assert len(requirements) == len({r['skill'] for r in requirements}) == 396
by_skill = {r['skill']: r for r in requirements}
for slug, keystone in unlocks.items():
    assert keystone in base_ids
    assert keystone in by_skill[f'eldenworld:{slug}/root']['required_skills']
    assert len(trees[f'eldenworld:{slug}']['skillIds']) == 22
for req in requirements:
    assert set(req.get('required_skills', [])) <= skills.keys()
# Every original node, coordinate, connection and bonus is preserved byte for byte.
assert all(content[n] == data for n, data in preserved.items())
with ZipFile(pack, 'w', ZIP_DEFLATED) as archive:
    for name, data in sorted(content.items()):
        archive.writestr(name, data)
with ZipFile(pack) as archive:
    assert archive.testzip() is None

# Client assets belong in Core; datapacks do not load client textures/translations.
icon = resources / 'assets/skilltree/textures/icons/skill_tree/soldier.png'
icon.parent.mkdir(parents=True, exist_ok=True)
icon.write_bytes((ci / 'base_tree_icon.png').read_bytes())
for locale, title in [('ru_ru', 'EldenWorld — общее дерево'), ('en_us', 'EldenWorld — Shared Tree')]:
    lang = resources / f'assets/skilltree/lang/{locale}.json'
    lang.parent.mkdir(parents=True, exist_ok=True)
    values = json.loads(lang.read_text()) if lang.exists() else {}
    values['skilltree:soldier'] = title
    lang.write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n')
print('Validated: 589 preserved base nodes, one shared start, 18 gated trees, 396 unique requirements')
