# Dblp Parsing Demo

Ce dossier contient un projet Java pour analyser un fichier DBLP XML et détecter des composantes fortement connexes.

## Pré-requis

- Java JDK installé (version 17 ou supérieure recommandée)
- Pour le script Bash : un shell compatible (`bash`)

## Fichiers d’entrée par défaut

Lorsque aucun argument n’est fourni, le script utilise automatiquement :

- `dblp-2026-01-01.xml.gz`
- `dblp.dtd`

!!! Ces fichiers doivent se trouver dans le même répertoire que le script. !!!

## Lancer le projet

### Windows

Ouvre un terminal dans `d:\Code\ALGO2_2\base` puis exécute :

```bat
pip install matplotlib
```
Pour installer la librairie matplotlib.

```bat
compile_and_run.bat
```
Pour executer le script bat.

Le script compile les fichiers Java dans `out/` puis lance `DblpParsingDemo`.

### Linux / macOS / Git Bash

Ouvre un terminal dans `d:\Code\ALGO2_2\base` puis exécute :

```bash
python3 -m venv venv && ./venv/bin/pip install matplotlib
```
Pour créer un environement virtuel et installer la librairie matplotlib.

```bash
chmod +x compile_and_run.sh
```
Pour pouvoir executer le script bash.

```bash
./compile_and_run.sh
```
Pour executer le script bash.

Le script compile les fichiers Java dans `out/` puis lance `DblpParsingDemo`.

## Arguments facultatifs

Tu peux également fournir explicitement le fichier XML, le DTD, la tâche et une limite :

```bat
compile_and_run.bat dblp-2026-01-01.xml.gz dblp.dtd --task=2 --limit=500000
```

```bash
./compile_and_run.sh dblp-2026-01-01.xml.gz dblp.dtd --task=2 --limit=500000
```

Pour exécuter la tâche 1 :

```bat
compile_and_run.bat dblp-2026-01-01.xml.gz dblp.dtd --task=1
```

```bash
./compile_and_run.sh dblp-2026-01-01.xml.gz dblp.dtd --task=1
```

Tu peux aussi passer les options avec un espace si ton shell sépare `--task` et sa valeur :

```bat
compile_and_run.bat --task 1 --limit 100000
```

```bash
./compile_and_run.sh --task 1 --limit 100000
```

## Résultats générés

Les fichiers de sortie sont stockés dans le dossier `base/data_out`.

- Tâche 1 :
  - `communautes_tache1_data.csv`
  - `communautes_tache1_histogram.txt`
  - `communautes_tache1_histogram.png`

- Tâche 2 :
  - `communautes_tache2_data.csv`
  - `communautes_tache2_histogram.txt`
  - `communautes_tache2_histogram.png`

## Exécution manuelle

Si tu veux compiler manuellement :

```bash
javac -d out *.java
java -Xmx2g -cp out DblpParsingDemo dblp-2026-01-01.xml.gz dblp.dtd --task=2
```

Tu peux aussi exécuter la tâche 1 directement :

```bash
java -Xmx2g -cp out DblpParsingDemo dblp-2026-01-01.xml.gz dblp.dtd --task 1 --limit 100000
```

## Résultat

Les classes compilées sont placées dans le dossier `out/`.