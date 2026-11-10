#!/bin/bash
# make_delta.sh <surah> <out.zip> [marker]: يضغط ما تغيّر منذ آخر تسليم (محتوى الوحدة، المواصفات، الأدوات، كود التطبيق)
S=$1; OUT=$2; M=${3:-/tmp/claude-0/.last_delivery}
cd "$(dirname "$0")/.." || exit 1
[ -f "$M" ] || { echo "no marker"; exit 1; }
find content/bootstrap/modules/$S content/bootstrap/catalog.json content/specs/$S.json docs/STATUS.md tools app/src -type f -newer "$M" ! -path '*/__pycache__/*' > /tmp/claude-0/delta.lst
rm -f "$OUT"; zip -q "$OUT" -@ < /tmp/claude-0/delta.lst && touch "$M"; unzip -l "$OUT" | tail -n +4
