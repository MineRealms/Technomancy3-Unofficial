#!/usr/bin/env python3
"""Check the distributable jar, including loader metadata and target resource paths."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile


def check(jar: Path, node: str) -> list[str]:
    neo = node == "1.21.1-neoforge"
    errors: list[str] = []
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        entries = set(names)
        if len(entries) != len(names):
            errors.append("duplicate archive entries")
        meta = "META-INF/neoforge.mods.toml" if neo else "META-INF/mods.toml"
        other = "META-INF/mods.toml" if neo else "META-INF/neoforge.mods.toml"
        if meta not in entries:
            errors.append(f"missing {meta}")
        else:
            text = archive.read(meta).decode("utf-8")
            if "${" in text:
                errors.append("unexpanded metadata variables")
            loader = "neoforge" if neo else "forge"
            if f'modId="{loader}"' not in text:
                errors.append(f"missing {loader} dependency")
        if other in entries:
            errors.append(f"wrong-loader metadata {other}")
        for name in entries:
            if name.startswith("theflogat/technomancy/gametest/"):
                errors.append(f"shipped GameTest class: {name}")
            if "/structure/gametest/" in name or "/structures/gametest/" in name:
                errors.append(f"shipped GameTest template: {name}")
            if name.endswith(".class"):
                major = struct.unpack(">H", archive.read(name)[6:8])[0]
                if major != (65 if neo else 61):
                    errors.append(f"unexpected Java class version {major}: {name}")
            if neo and name.startswith("data/"):
                parts = name.split("/")
                if len(parts) > 2 and parts[2] in {"recipes", "loot_tables", "structures"}:
                    errors.append(f"legacy data directory: {name}")
        for name in ["META-INF/LICENSE", "META-INF/NOTICE", "pack.mcmeta"]:
            if name not in entries:
                errors.append(f"missing {name}")
        if "pack.mcmeta" in entries:
            pack = archive.read("pack.mcmeta").decode("utf-8")
            if "${" in pack:
                errors.append("unexpanded pack metadata")
            if json.loads(pack)["pack"]["pack_format"] != (34 if neo else 15):
                errors.append("wrong pack format")
        if not neo:
            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
            if "MixinConfigs: technom.mixins.json" not in manifest:
                errors.append("missing Forge MixinConfigs manifest registration")
        elif "technom.mixins.json" in entries:
            errors.append("legacy Forge JEI compatibility mixin in NeoForge artifact")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--node", required=True, choices=["1.20.1-forge", "1.21.1-neoforge"])
    args = parser.parse_args()
    errors = check(args.jar, args.node)
    for error in errors:
        print("ERROR:", error)
    if errors:
        return 1
    print(f"PASS {args.node}: {args.jar}")
    print("SHA-256", hashlib.sha256(args.jar.read_bytes()).hexdigest())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
