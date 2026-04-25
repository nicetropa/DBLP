#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Generé à l'aide de l'IA, vérifié et corrigé par l'humain.
"""
Script pour générer un histogramme des tailles de communautés d'auteurs DBLP.
Produit deux subplots :
  - Gauche : distribution complète en échelle log-log (toutes les communautés)
  - Droite  : zoom log-log sur les communautés intermédiaires
              (singletons et composante géante exclus)
"""

import sys
import os

if sys.platform == 'win32':
    os.system('chcp 65001 >nul 2>&1')
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')

import csv
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from pathlib import Path


def read_csv(csv_path):
    sizes, frequencies = [], []
    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                sizes.append(int(row['Taille de communauté']))
                frequencies.append(int(row['Fréquence']))
    except FileNotFoundError:
        print(f"[ERREUR] Fichier introuvable : {csv_path}"); sys.exit(1)
    except Exception as e:
        print(f"[ERREUR] Lecture CSV : {e}"); sys.exit(1)
    if not sizes:
        print("[ERREUR] Aucune donnée dans le CSV"); sys.exit(1)
    return sizes, frequencies


def compute_stats(sizes, frequencies):
    total = sum(frequencies)
    avg = sum(s * f for s, f in zip(sizes, frequencies)) / total
    return {
        'total': total,
        'min': min(sizes),
        'max': max(sizes),
        'avg': avg,
        'nb_singleton': next((f for s, f in zip(sizes, frequencies) if s == 1), 0),
        'nb_non_trivial': sum(f for s, f in zip(sizes, frequencies) if s >= 2),
    }


def detect_giant(sizes, frequencies):
    """Détecte la composante géante : taille >= 10x la 2e plus grande."""
    sorted_sizes = sorted(sizes, reverse=True)
    if len(sorted_sizes) >= 2 and sorted_sizes[0] >= 10 * sorted_sizes[1]:
        return sorted_sizes[0]
    return None


def fmt_axis(ax):
    ax.xaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f'{int(x):,}'))
    ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f'{int(x):,}'))


def plot_full(ax, sizes, frequencies, stats, giant_size):
    """Gauche : toutes les communautés, log-log."""
    ax.scatter(sizes, frequencies, color='steelblue', edgecolors='navy',
               alpha=0.75, s=40, zorder=3)
    ax.set_xscale('log'); ax.set_yscale('log')
    ax.set_xlabel('Taille de communauté', fontsize=11, fontweight='bold')
    ax.set_ylabel('Fréquence', fontsize=11, fontweight='bold')
    ax.set_title('Distribution complète (log-log)', fontsize=12, fontweight='bold')
    ax.grid(True, which='both', alpha=0.3, linestyle='--')
    fmt_axis(ax)

    if stats['nb_singleton'] > 0:
        ax.scatter([1], [stats['nb_singleton']], color='red', s=70, zorder=5)
        ax.annotate(f"  {stats['nb_singleton']:,} singletons",
                    xy=(1, stats['nb_singleton']), fontsize=9, color='red', va='center')

    if giant_size is not None:
        gf = frequencies[sizes.index(giant_size)]
        ax.scatter([giant_size], [gf], color='darkgreen', s=70, zorder=5)
        ax.annotate(f"composante géante\n({giant_size:,} auteurs)  ",
                    xy=(giant_size, gf), fontsize=9, color='darkgreen',
                    va='center', ha='right')


def plot_zoom(ax, sizes, frequencies, giant_size):
    """Droite : zoom log-log sur communautés intermédiaires."""
    exclude = {1}
    if giant_size is not None:
        exclude.add(giant_size)

    filtered = [(s, f) for s, f in zip(sizes, frequencies) if s not in exclude]
    if not filtered:
        ax.text(0.5, 0.5, 'Aucune communauté intermédiaire',
                ha='center', va='center', transform=ax.transAxes, fontsize=12)
        return

    s2, f2 = zip(*filtered)
    ax.scatter(s2, f2, color='darkorange', edgecolors='saddlebrown', alpha=0.8, s=50, zorder=3)
    ax.set_xscale('log'); ax.set_yscale('log')
    ax.set_xlabel('Taille de communauté', fontsize=11, fontweight='bold')
    ax.set_ylabel('Fréquence', fontsize=11, fontweight='bold')
    suffix = ' et composante géante exclus)' if giant_size else ' exclus)'
    ax.set_title(f'Zoom : communautés intermédiaires (log-log)\n(singletons{suffix}',
                 fontsize=12, fontweight='bold')
    ax.grid(True, which='both', alpha=0.3, linestyle='--')
    fmt_axis(ax)

    # Annoter les tailles si peu de points
    if len(s2) <= 40:
        for s, f in zip(s2, f2):
            ax.annotate(f' {s:,}', xy=(s, f), fontsize=7, alpha=0.85, va='center')


def generate_histogram(csv_path, output_path):
    sizes, frequencies = read_csv(csv_path)
    stats = compute_stats(sizes, frequencies)
    giant_size = detect_giant(sizes, frequencies)

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 7))

    stem = Path(csv_path).stem
    if 'tache1' in stem or 'task1' in stem:
        label = 'Tâche 1 — Graphe non orienté (Union-Find)'
    elif 'tache2' in stem or 'task2' in stem:
        label = 'Tâche 2 — Graphe orienté (seuil ≥ 6, Tarjan)'
    else:
        label = ''

    fig.suptitle(f'Distribution des tailles de communautés — DBLP\n{label}',
                 fontsize=13, fontweight='bold')

    plot_full(ax1, sizes, frequencies, stats, giant_size)
    plot_zoom(ax2, sizes, frequencies, giant_size)

    stats_text = (
        f"Total communautés : {stats['total']:,}  |  "
        f"Singletons : {stats['nb_singleton']:,}  |  "
        f"Non triviales (≥2) : {stats['nb_non_trivial']:,}  |  "
        f"Taille max : {stats['max']:,}  |  "
        f"Taille moyenne : {stats['avg']:.2f}"
    )
    fig.text(0.5, -0.02, stats_text, ha='center', fontsize=10,
             bbox=dict(boxstyle='round', facecolor='lightyellow', alpha=0.8))

    plt.tight_layout()
    try:
        plt.savefig(output_path, dpi=300, bbox_inches='tight')
        print(f"[OK] Histogramme généré : {output_path}")
    except Exception as e:
        print(f"[ERREUR] Sauvegarde : {e}"); sys.exit(1)
    plt.close()


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python generate_histogram.py <chemin_csv> [<chemin_png>]")
        sys.exit(1)
    csv_path = Path(sys.argv[1])
    output_path = (Path(sys.argv[2]) if len(sys.argv) >= 3
                   else csv_path.parent / (csv_path.stem + '_histogram.png'))
    generate_histogram(str(csv_path), str(output_path))