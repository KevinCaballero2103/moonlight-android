#!/usr/bin/env python3
"""Check localized resources and that the tested Genshin input data stays intact."""
from pathlib import Path
import collections
import json
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
HAN = re.compile(r'[\u3400-\u9fff]')
errors = []

def resources(folder):
    result = {}
    for path in sorted((RES / folder).glob('*.xml')):
        for node in ET.parse(path).getroot():
            if node.tag not in ('string', 'string-array', 'plurals'):
                continue
            key = (node.tag, node.get('name'))
            if key in result:
                errors.append(f'Duplicate {folder} resource: {key}')
            result[key] = node
    return result

base = resources('values')
spanish = resources('values-es')

def text(node):
    return ''.join(node.itertext())

def format_arguments(value):
    value = value.replace('%%', '')
    return collections.Counter(re.findall(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?([sSdDfoxXeEgGbBcCaAhH])', value))

for key, node in base.items():
    if node.get('translatable') == 'false':
        continue
    translated = spanish.get(key)
    if key[0] == 'string' and translated is None:
        errors.append(f'Missing Spanish string: {key[1]}')
    if translated is not None:
        if HAN.search(text(translated)):
            errors.append(f'Chinese text in Spanish resource: {key[1]}')
        if key[0] == 'string' and node.get('formatted') != 'false':
            if format_arguments(text(node)) != format_arguments(text(translated)):
                errors.append(f'Changed format arguments: {key[1]}')

for path in RES.rglob('*.xml'):
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        errors.append(f'{path.relative_to(ROOT)}: {e}')
        continue
    if path.parent.name.startswith(('layout', 'xml')):
        for node in tree.iter():
            for key, value in node.attrib.items():
                if HAN.search(value):
                    errors.append(f'Hardcoded visible Chinese: {path.relative_to(ROOT)} {key}')

original = json.loads((ROOT / 'presets/genshin/axi_OSC_Keyboard_original.txt').read_text())
preset = json.loads((ROOT / 'app/src/main/assets/config/genshin_touch_es.json').read_text())
export = json.loads((ROOT / 'presets/genshin/axi_OSC_Keyboard_es.txt').read_text())
if len(original) != 11 or len(preset) != len(original) or preset != export:
    errors.append('Genshin preset size/export differs from the supplied configuration')
for a, b in zip(original, preset):
    inputs = lambda d: {k:v for k,v in d.items() if k not in ('name','desc')}
    if inputs(a) != inputs(b):
        errors.append(f'Changed Genshin input/layout: {a.get("id")}')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    raise SystemExit(1)
print(f'Spanish checks passed: {sum(k[0]=="string" for k in spanish)} strings; 11 original Genshin controls preserved')
