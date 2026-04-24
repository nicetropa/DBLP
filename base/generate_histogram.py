#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Generé à l'aide de l'IA, vérifié et corrigé par l'humain.
"""
Script pour générer un histogramme des tailles de communautés d'auteurs.
Utilise matplotlib pour produire une image PNG avec un vrai histogramme.
"""

import sys
import os

# Force UTF-8 encoding pour Windows
if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')

import csv
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path

def generate_histogram(csv_path, output_path):
    """
    Génère un histogramme à partir d'un fichier CSV.

    Args:
        csv_path: Chemin vers le fichier communautés_data.csv
        output_path: Chemin où sauvegarder l'image PNG
    """

    sizes = []
    frequencies = []

    # Lire le CSV
    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                sizes.append(int(row['Taille de communauté']))
                frequencies.append(int(row['Fréquence']))
    except FileNotFoundError:
        print(f"[ERREUR] Le fichier {csv_path} n'a pas été trouvé")
        sys.exit(1)
    except Exception as e:
        print(f"[ERREUR] Lors de la lecture du CSV: {e}")
        sys.exit(1)

    if not sizes:
        print("[ERREUR] Aucune donnée dans le CSV")
        sys.exit(1)

    # Créer la figure
    fig, ax = plt.subplots(figsize=(14, 8))

    # Décider entre bar plot (si peu de données) ou scatter + interpolation (si beaucoup)
    if len(sizes) <= 100:
        # Bar plot pour peu de valeurs
        bars = ax.bar(sizes, frequencies, color='steelblue', edgecolor='navy', alpha=0.7, width=0.8)
        ax.set_xlabel('Taille de communauté', fontsize=12, fontweight='bold')
    else:
        # Scatter plot + log scale pour beaucoup de valeurs
        ax.scatter(sizes, frequencies, alpha=0.6, s=50, color='steelblue', edgecolors='navy')
        ax.set_xlabel('Taille de communauté (échelle log)', fontsize=12, fontweight='bold')
        ax.set_xscale('log')

    ax.set_ylabel('Fréquence (nombre de communautés)', fontsize=12, fontweight='bold')
    ax.set_title('Distribution des Tailles de Communautés d\'Auteurs DBLP',
                 fontsize=14, fontweight='bold', pad=20)

    # Ajouter une grille
    ax.grid(True, alpha=0.3, linestyle='--')

    # Statistiques
    total_communities = sum(frequencies)
    max_size = max(sizes)
    min_size = min(sizes)
    avg_size = sum(s * f for s, f in zip(sizes, frequencies)) / total_communities

    # Ajouter les statistiques dans le titre ou en texte
    stats_text = f'Total: {total_communities:,} | Min: {min_size} | Max: {max_size:,} | Moy: {avg_size:.1f}'
    ax.text(0.5, -0.12, stats_text, transform=ax.transAxes,
            ha='center', fontsize=11, bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))

    # Ajuster la disposition
    plt.tight_layout()

    # Sauvegarder
    try:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"[OK] Histogramme genere: {output_path}")
    except Exception as e:
        print(f"[ERREUR] Lors de la sauvegarde: {e}")
        sys.exit(1)

    plt.close()

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python generate_histogram.py <chemin_csv>")
        print("Exemple: python generate_histogram.py communautes_data.csv")
        sys.exit(1)

    csv_path = Path(sys.argv[1])
    output_path = csv_path.parent / 'communautes_histogram.png'

    generate_histogram(str(csv_path), str(output_path))
