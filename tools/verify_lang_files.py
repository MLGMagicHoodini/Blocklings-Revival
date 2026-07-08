import json
import shutil
from pathlib import Path

common = Path(r"c:\Mes mods crée\mm\Blocklings-Revival\common\src\main\resources\assets\blocklings\lang")
neo = Path(r"c:\Mes mods crée\mm\Blocklings-Revival\neoforge\src\main\resources\assets\blocklings\lang")
neo.mkdir(parents=True, exist_ok=True)

en = json.loads((common / "en_us.json").read_text(encoding="utf-8"))
en_keys = set(en)

files = sorted(common.glob("*.json"))
print(f"common files: {len(files)}")
ok = True
for p in files:
    data = json.loads(p.read_text(encoding="utf-8"))
    missing = en_keys - set(data)
    extra = set(data) - en_keys
    status = "OK" if not missing and not extra else f"missing={len(missing)} extra={len(extra)}"
    if missing or extra:
        ok = False
    shutil.copy2(p, neo / p.name)
    print(f"  {p.name}: {len(data)} keys [{status}]")

print(f"neoforge files: {len(list(neo.glob('*.json')))}")
print("VERDICT:", "ALL OK" if ok else "FAILED")
