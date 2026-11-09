"""Generate app/src/main/assets/emoji.txt from Unicode's emoji-test.txt.

Usage:
    python tools/generate_emoji.py path/to/emoji-test.txt

Download the input from https://unicode.org/Public/emoji/latest/emoji-test.txt.
Output: one line per group, "<group name>\t<emoji> <emoji> ...". Only fully-qualified
emoji are kept; skin-tone variants and the "Component" group are left out.
"""

import pathlib
import sys

SKIN_TONES = {chr(c) for c in range(0x1F3FB, 0x1F400)}
OUT = pathlib.Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "emoji.txt"


def parse(lines):
    groups = {}
    order = []
    group = None
    for line in lines:
        line = line.rstrip("\n")
        if line.startswith("# group:"):
            group = line.split(":", 1)[1].strip()
            if group != "Component" and group not in groups:
                groups[group] = []
                order.append(group)
            continue
        if not line or line.startswith("#") or group is None or group == "Component":
            continue
        codes, _, rest = line.partition(";")
        if not rest.strip().startswith("fully-qualified"):
            continue
        emoji = "".join(chr(int(c, 16)) for c in codes.split())
        if any(ch in SKIN_TONES for ch in emoji):
            continue
        groups[group].append(emoji)
    return [(g, groups[g]) for g in order]


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8").splitlines()
    groups = parse(text)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8", newline="\n") as f:
        for name, emoji in groups:
            f.write(f"{name}\t{' '.join(emoji)}\n")
    total = sum(len(e) for _, e in groups)
    print(f"wrote {total} emoji in {len(groups)} groups to {OUT}")


if __name__ == "__main__":
    main()
