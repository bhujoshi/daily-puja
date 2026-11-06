#!/usr/bin/env python3
"""
Validates all Google Play Store & Fastlane metadata, changelogs, icons, feature graphics,
and phone/tablet screenshots for Pavitra Mandir (com.pavitramandir.app).
"""

import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FASTLANE_META = ROOT / "android" / "fastlane" / "metadata" / "android"
PLAY_STORE_DIR = ROOT / "play_store"

LOCALES = ["en-US", "en-IN", "hi-IN"]
EXPECTED_SCREENSHOTS = [
    "01_sacred_gateway.png",
    "02_swarna_sanctum_puja.png",
    "03_radha_krishna_marble.png",
    "04_aarti_shankh_naad.png",
    "05_mandir_sajaye_customizer.png",
    "06_bhajan_aarti_player.png",
    "07_punya_coin_passbook.png",
    "08_morning_shodhan.png",
]


def read_png_info(path: Path):
    """Returns (width, height, bit_depth, color_type) from a PNG file header."""
    data = path.read_bytes()
    if len(data) < 29 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: Not a valid PNG file")
    if data[12:16] != b"IHDR":
        raise ValueError(f"{path}: Missing IHDR chunk")
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, bit_depth, color_type


def check_text_limit(path: Path, max_chars: int, label: str, errors: list):
    if not path.is_file():
        errors.append(f"Missing {label}: {path}")
        return
    text = path.read_text(encoding="utf-8").strip()
    length = len(text)
    if length == 0:
        errors.append(f"Empty {label}: {path}")
    elif length > max_chars:
        errors.append(f"{label} exceeds {max_chars} chars ({length} chars): {path}")
    else:
        print(f"  [OK] {label}: {length}/{max_chars} chars ({path.relative_to(ROOT)})")


def check_png(path: Path, expected_w: int, expected_h: int, label: str, errors: list):
    if not path.is_file():
        errors.append(f"Missing {label}: {path}")
        return
    try:
        w, h, bit_depth, color_type = read_png_info(path)
        if (w, h) != (expected_w, expected_h):
            errors.append(
                f"{label} wrong dimensions {w}x{h} (expected {expected_w}x{expected_h}): {path}"
            )
        elif path.stat().st_size >= 8_000_000:
            errors.append(
                f"{label} exceeds Google Play 8,000,000 byte limit ({path.stat().st_size} bytes): {path}"
            )
        else:
            print(
                f"  [OK] {label}: {w}x{h} PNG (color_type={color_type}, {path.stat().st_size // 1024} KB) ({path.relative_to(ROOT)})"
            )
    except Exception as exc:
        errors.append(str(exc))


def main():
    errors = []
    print("Verifying Pavitra Mandir Google Play Store & Fastlane assets...\n")

    # 1. Check Fastlane configuration files
    for cfg in [
        ROOT / "android" / "Gemfile",
        ROOT / "android" / "fastlane" / "Appfile",
        ROOT / "android" / "fastlane" / "Fastfile",
        ROOT / "docs" / "PLAY_STORE_SUBMISSION_GUIDE.md",
        ROOT / "play_store" / "PLAY_STORE_SUBMISSION_GUIDE.md",
    ]:
        if not cfg.is_file():
            errors.append(f"Missing required file: {cfg}")
        else:
            print(f"  [OK] Found {cfg.relative_to(ROOT)}")

    # 2. Check top-level play_store/ assets
    check_png(PLAY_STORE_DIR / "icon_512x512.png", 512, 512, "play_store icon", errors)
    check_png(
        PLAY_STORE_DIR / "feature_graphic_1024x500.png",
        1024,
        500,
        "play_store featureGraphic",
        errors,
    )
    for name in EXPECTED_SCREENSHOTS:
        check_png(
            PLAY_STORE_DIR / "screenshots" / name,
            1080,
            2280,
            f"play_store phone/{name}",
            errors,
        )
        check_png(
            PLAY_STORE_DIR / "screenshots_7inch_tablet" / name,
            1200,
            1920,
            f"play_store 7inch/{name}",
            errors,
        )
        check_png(
            PLAY_STORE_DIR / "screenshots_10inch_tablet" / name,
            1600,
            2560,
            f"play_store 10inch/{name}",
            errors,
        )

    # 3. Check each Fastlane locale
    for locale in LOCALES:
        print(f"\nChecking Fastlane locale [{locale}]...")
        loc_dir = FASTLANE_META / locale
        check_text_limit(loc_dir / "title.txt", 30, f"[{locale}] title.txt", errors)
        check_text_limit(
            loc_dir / "short_description.txt",
            80,
            f"[{locale}] short_description.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "full_description.txt",
            4000,
            f"[{locale}] full_description.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "changelogs" / "18.txt",
            500,
            f"[{locale}] changelogs/18.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "changelogs" / "19.txt",
            500,
            f"[{locale}] changelogs/19.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "changelogs" / "20.txt",
            500,
            f"[{locale}] changelogs/20.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "changelogs" / "21.txt",
            500,
            f"[{locale}] changelogs/21.txt",
            errors,
        )
        check_text_limit(
            loc_dir / "changelogs" / "default.txt",
            500,
            f"[{locale}] changelogs/default.txt",
            errors,
        )

        img_dir = loc_dir / "images"
        check_png(img_dir / "icon.png", 512, 512, f"[{locale}] icon.png", errors)
        check_png(
            img_dir / "featureGraphic.png",
            1024,
            500,
            f"[{locale}] featureGraphic.png",
            errors,
        )

        for name in EXPECTED_SCREENSHOTS:
            check_png(
                img_dir / "phoneScreenshots" / name,
                1080,
                2280,
                f"[{locale}] phoneScreenshots/{name}",
                errors,
            )
            check_png(
                img_dir / "sevenInchScreenshots" / name,
                1200,
                1920,
                f"[{locale}] sevenInchScreenshots/{name}",
                errors,
            )
            check_png(
                img_dir / "tenInchScreenshots" / name,
                1600,
                2560,
                f"[{locale}] tenInchScreenshots/{name}",
                errors,
            )

    if errors:
        print("\nVALIDATION FAILED:")
        for err in errors:
            print(f"  [ERROR] {err}")
        sys.exit(1)

    print(
        "\nALL PLAY STORE & FASTLANE ASSETS VERIFIED SUCCESSFULLY (0 errors across 3 locales & 26 PNGs per locale)."
    )


if __name__ == "__main__":
    main()
