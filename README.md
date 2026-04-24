# Dblp Parsing Demo

Ce dossier contient un projet Java pour analyser un fichier DBLP XML et détecter des composantes fortement connexes.

## Pré-requis

- Java JDK installé (version 17 ou supérieure recommandée)
- Pour le script Bash : un shell compatible (`bash`)

## Fichiers d’entrée par défaut

Lorsque aucun argument n’est fourni, le script utilise automatiquement :

- `dblp-2026-01-01.xml.gz`
- `dblp.dtd`

Ces fichiers doivent se trouver dans le même répertoire que le script.

## Lancer le projet

### Windows

Ouvre un terminal dans `d:\Code\ALGO2_2\base` puis exécute :

```bat
compile_and_run.bat
```

Le script compile les fichiers Java dans `out/` puis lance `DblpParsingDemo`.

### Linux / macOS / Git Bash

Ouvre un terminal dans `d:\Code\ALGO2_2\base` puis exécute :

```bash
./compile_and_run.sh
```

Le script compile les fichiers Java dans `out/` puis lance `DblpParsingDemo`.

## Arguments facultatifs

Tu peux aussi fournir explicitement le fichier XML, le DTD et une limite :

```bat
compile_and_run.bat dblp-2026-01-01.xml.gz dblp.dtd --limit=500000
```

```bash
./compile_and_run.sh dblp-2026-01-01.xml.gz dblp.dtd --limit=500000
```

## Exécution manuelle

Si tu veux compiler manuellement :

```bash
javac -d out *.java
java -Xmx2g -cp out DblpParsingDemo dblp-2026-01-01.xml.gz dblp.dtd
```

## Résultat

Les classes compilées sont placées dans le dossier `out/`.