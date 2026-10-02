"""Inventory every bundled GLB; optionally compare a saved runtime asset tree.

python3 scripts/audit_models.py --baseline /tmp/baseline/shrine --output report.json
Uses only Python's standard library. Texture bytes are encoded bytes, not GPU memory.
"""
import argparse
import json
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]


def image_size(data):
    if data.startswith(b'\x89PNG\r\n\x1a\n'):
        return struct.unpack_from('>II', data, 16)
    if data.startswith(b'\xff\xd8'):
        offset = 2
        while offset < len(data):
            if data[offset] != 255:
                offset += 1
                continue
            while data[offset] == 255:
                offset += 1
            marker = data[offset]
            offset += 1
            if marker in (0xD8, 0xD9) or 0xD0 <= marker <= 0xD7:
                continue
            length = struct.unpack_from('>H', data, offset)[0]
            if marker in (0xC0, 0xC1, 0xC2):
                height, width = struct.unpack_from('>HH', data, offset + 3)
                return width, height
            offset += length
    raise ValueError('Unsupported embedded image format')


def inspect(path):
    data = path.read_bytes()
    magic, version, length = struct.unpack_from('<4sII', data)
    assert magic == b'glTF' and version == 2 and length == len(data), path
    n, kind = struct.unpack_from('<I4s', data, 12)
    assert kind == b'JSON', path
    model = json.loads(data[20:20+n])
    binary_length, binary_kind = struct.unpack_from('<I4s', data, 20+n)
    assert binary_kind == b'BIN\0', path
    binary = data[28+n:28+n+binary_length]
    assert model['buffers'][0]['byteLength'] <= len(binary), path
    for view in model.get('bufferViews', []):
        assert view.get('byteOffset', 0)+view['byteLength'] <= len(binary), path
    triangles = 0
    for mesh in model['meshes']:
        for primitive in mesh['primitives']:
            assert primitive.get('mode', 4) == 4, path
            accessor = primitive.get('indices', primitive['attributes']['POSITION'])
            triangles += model['accessors'][accessor]['count']//3
    images = []
    for image in model.get('images', []):
        view = model['bufferViews'][image['bufferView']]
        start = view.get('byteOffset', 0)
        width, height = image_size(binary[start:start+view['byteLength']])
        images.append(dict(width=width, height=height, bytes=view['byteLength']))
    return dict(bytes=len(data), triangles=triangles, materials=len(model.get('materials', [])),
                texture_bytes=sum(i['bytes'] for i in images),
                texture_pixels=sum(i['width']*i['height'] for i in images), images=images)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=pathlib.Path)
    parser.add_argument('--output', type=pathlib.Path)
    args = parser.parse_args()
    runtime = ROOT/'android/app/src/main/assets/shrine'
    results = []
    for path in sorted(runtime.rglob('*.glb')):
        relative = path.relative_to(runtime)
        entry = dict(id=str(relative.with_suffix('')), after=inspect(path))
        if args.baseline:
            entry['before'] = inspect(args.baseline/relative)
        results.append(entry)
    totals = {key: {metric: sum(r[key][metric] for r in results)
                   for metric in ('bytes', 'triangles', 'texture_bytes', 'texture_pixels')}
              for key in (('before', 'after') if args.baseline else ('after',))}
    report = dict(profile='phone-balanced', models=results, totals=totals)
    if args.output:
        args.output.write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(totals, indent=2))


if __name__ == '__main__':
    main()
