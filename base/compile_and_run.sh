#!/usr/bin/env bash
# Compile all Java sources into out and run the main class with provided arguments.
# Usage: ./compile_and_run.sh [<dblp.xml|dblp.xml.gz> <dblp.dtd>] --task=1|2 [--limit=1000000]
# Script generated with IA.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
mkdir -p out
javac -d out *.java
echo "Compilation réussie."
if [ "$#" -eq 0 ]; then
  XML="dblp-2026-01-01.xml.gz"
  DTD="dblp.dtd"
  echo "Aucun argument fourni, utilisation de $XML $DTD"
  set -- "$XML" "$DTD"
elif [ "$#" -eq 1 ] && { [ "${1#--task=}" != "$1" ]; }; then
  XML="dblp-2026-01-01.xml.gz"
  DTD="dblp.dtd"
  echo "Aucun XML/DTD fourni, utilisation de $XML $DTD"
  set -- "$XML" "$DTD" "$1"
fi
java -Xmx4g -cp out DblpParsingDemo "$@"
