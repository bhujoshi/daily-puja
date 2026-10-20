#!/usr/bin/env python3
"""Check every native ELF LOAD segment in an APK/AAB, plus APK ZIP alignment.

Usage: python3 scripts/check_android_alignment.py path/to/app.apk [path/to/app.aab]
This validates packaging, not runtime behavior on a 16 KB device.
"""
import struct
import sys
import zipfile
from pathlib import Path


def inspect(path):
    failures = []
    count = 0
    with zipfile.ZipFile(path) as archive, open(path, 'rb') as raw:
        for entry in archive.infolist():
            if not entry.filename.endswith('.so'):
                continue
            count += 1
            data = archive.read(entry)
            if data[:4] != b'\x7fELF':
                failures.append(f'{entry.filename}: not ELF')
                continue
            endian = '<' if data[5] == 1 else '>'
            is64 = data[4] == 2
            offset = struct.unpack_from(endian + ('Q' if is64 else 'I'), data, 32 if is64 else 28)[0]
            size, number = struct.unpack_from(endian + 'HH', data, 54 if is64 else 42)
            loads = 0
            for index in range(number):
                fields = struct.unpack_from(endian + ('IIQQQQQQ' if is64 else 'IIIIIIII'), data, offset + index * size)
                if fields[0] != 1:
                    continue
                loads += 1
                alignment = fields[-1]
                file_offset, address = (fields[2], fields[3]) if is64 else (fields[1], fields[2])
                if alignment < 16384 or (address - file_offset) % 16384:
                    failures.append(f'{entry.filename}: LOAD alignment={alignment}, offset={file_offset}, address={address}')
            if not loads:
                failures.append(f'{entry.filename}: no LOAD segments')
            if path.suffix == '.apk' and entry.compress_type == zipfile.ZIP_STORED:
                raw.seek(entry.header_offset + 26)
                name_size, extra_size = struct.unpack('<HH', raw.read(4))
                start = entry.header_offset + 30 + name_size + extra_size
                if start % 16384:
                    failures.append(f'{entry.filename}: ZIP data offset {start} is not 16 KB aligned')
    print(f'{path}: {count} native libraries, {len(failures)} alignment failures')
    for failure in failures:
        print('  ' + failure)
    return not failures


if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    results = [inspect(Path(arg)) for arg in sys.argv[1:]]
    sys.exit(0 if all(results) else 1)
