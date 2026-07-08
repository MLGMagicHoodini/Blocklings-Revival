#!/usr/bin/env python3
"""Parallel locale generator for Blocklings lang files from en_us.json."""

from __future__ import annotations

import json
import re
import shutil
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

from deep_translator import GoogleTranslator

ROOT = Path(__file__).resolve().parents[1]
COMMON_LANG = ROOT / "common" / "src" / "main" / "resources" / "assets" / "blocklings" / "lang"
NEOFORGE_LANG = ROOT / "neoforge" / "src" / "main" / "resources" / "assets" / "blocklings" / "lang"

LOCALES: dict[str, str] = {
    "de_de": "de",
    "es_es": "es",
    "es_mx": "es",
    "pt_br": "pt",
    "pt_pt": "pt",
    "it_it": "it",
    "nl_nl": "nl",
    "pl_pl": "pl",
    "ru_ru": "ru",
    "uk_ua": "uk",
    "zh_cn": "zh-CN",
    "zh_tw": "zh-TW",
    "ja_jp": "ja",
    "ko_kr": "ko",
    "tr_tr": "tr",
    "cs_cz": "cs",
    "hu_hu": "hu",
    "sv_se": "sv",
    "da_dk": "da",
    "fi_fi": "fi",
    "no_no": "no",
    "th_th": "th",
    "vi_vn": "vi",
    "id_id": "id",
    "ro_ro": "ro",
    "el_gr": "el",
}

PLACEHOLDER_RE = re.compile(r"(%%|%\d+\$[sd]|%[sd]|%.1f|%ss|§[0-9a-fk-or])")
BATCH = 25
WORKERS = 4


def protect(text: str) -> tuple[str, list[str]]:
    parts: list[str] = []

    def repl(m: re.Match[str]) -> str:
        parts.append(m.group(0))
        return f"⟦{len(parts) - 1}⟧"

    return PLACEHOLDER_RE.sub(repl, text), parts


def restore(text: str, parts: list[str]) -> str:
    out = text
    for i, part in enumerate(parts):
        for token in (f"⟦{i}⟧", f"[{i}]", f"[[{i}]]", f"<{i}>", f"{{{i}}}", f"({i})"):
            if token in out:
                out = out.replace(token, part)
                break
    return out


def translate_one(translator: GoogleTranslator, value: str) -> str:
    if not value or value.strip() in {"???", "Blockling", "Blocklings"}:
        return value
    protected, parts = protect(value)
    for attempt in range(5):
        try:
            raw = translator.translate(protected)
            return restore(raw or value, parts)
        except Exception:  # noqa: BLE001
            time.sleep(1.2 * (attempt + 1))
    return value


def write_json(path: Path, data: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def translate_locale(locale: str, gt_code: str, keys: list[str], values: list[str]) -> tuple[str, int]:
    translator = GoogleTranslator(source="en", target=gt_code)
    out_vals: list[str] = []
    total = len(values)
    for i, value in enumerate(values, start=1):
        out_vals.append(translate_one(translator, value))
        if i % 50 == 0 or i == total:
            print(f"[{locale}] {i}/{total}", flush=True)
            time.sleep(0.25)
        else:
            time.sleep(0.05)

    out = {k: v for k, v in zip(keys, out_vals)}
    write_json(COMMON_LANG / f"{locale}.json", out)
    write_json(NEOFORGE_LANG / f"{locale}.json", out)
    print(f"[{locale}] DONE {len(out)} keys", flush=True)
    return locale, len(out)


def main() -> None:
    with (COMMON_LANG / "en_us.json").open(encoding="utf-8") as f:
        en: dict[str, str] = json.load(f)

    keys = list(en.keys())
    values = [en[k] for k in keys]
    print(f"Source keys: {len(en)} — locales: {len(LOCALES)} — workers: {WORKERS}", flush=True)

    NEOFORGE_LANG.mkdir(parents=True, exist_ok=True)
    for keep in ("en_us.json", "fr_fr.json"):
        src = COMMON_LANG / keep
        if src.exists():
            shutil.copy2(src, NEOFORGE_LANG / keep)

    # Skip locales already complete with same key count (resume support)
    pending: list[tuple[str, str]] = []
    for locale, code in LOCALES.items():
        path = COMMON_LANG / f"{locale}.json"
        if path.exists():
            try:
                with path.open(encoding="utf-8") as f:
                    existing = json.load(f)
                if set(existing.keys()) == set(en.keys()) and all(
                    isinstance(v, str) and v for v in existing.values()
                ):
                    # Heuristic: if still mostly English, regenerate
                    sample_keys = [
                        "blocklings.taming.hint",
                        "blocklings.tab.stats",
                        "blocklings.skill.general.heal.name",
                    ]
                    still_en = sum(1 for k in sample_keys if existing.get(k) == en.get(k))
                    if still_en < 2:
                        write_json(NEOFORGE_LANG / f"{locale}.json", existing)
                        print(f"[{locale}] skip (already translated)", flush=True)
                        continue
            except Exception:  # noqa: BLE001
                pass
        pending.append((locale, code))

    print(f"Pending: {len(pending)}", flush=True)
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futures = [
            pool.submit(translate_locale, locale, code, keys, values)
            for locale, code in pending
        ]
        for fut in as_completed(futures):
            locale, count = fut.result()
            print(f"Completed {locale} ({count})", flush=True)

    # Final verification
    ok = True
    for locale in LOCALES:
        path = COMMON_LANG / f"{locale}.json"
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
        if set(data.keys()) != set(en.keys()):
            print(f"MISMATCH {locale}", flush=True)
            ok = False
        shutil.copy2(path, NEOFORGE_LANG / f"{locale}.json")
    print("ALL OK" if ok else "FAILED VERIFICATION", flush=True)


if __name__ == "__main__":
    main()
