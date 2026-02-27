#!/usr/bin/env python3
"""
enhance_wordnet.py - Add IPA pronunciation and CEFR levels to EigoLens wordnet.db

Enhances the existing wordnet.db by adding:
  - phonetic (IPA) from CMU Pronouncing Dictionary via ARPAbet -> IPA conversion
  - cefr_level from Brown Corpus frequency ranks

Uses the same proven ARPAbet->IPA mapping as EigoQuest.

Usage:
    cd data-pipeline
    python3 enhance_wordnet.py
    python3 enhance_wordnet.py --db ../app/src/main/assets/wordnet.db

Requirements:
    pip install nltk
    # NLTK data: cmudict, brown
"""

import argparse
import logging
import sqlite3
import sys
import time
from collections import Counter
from typing import Dict, List, Tuple

import nltk
from nltk.corpus import brown, cmudict

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("enhance")

DEFAULT_DB = "../app/src/main/assets/wordnet.db"

# CEFR level boundaries (by frequency rank, 1-indexed)
CEFR_BOUNDARIES = [
    (1000, "A1"),
    (3000, "A2"),
    (5000, "B1"),
    (7000, "B2"),
    (9000, "C1"),
    (99999, "C2"),
]

# ARPAbet to IPA conversion (from EigoQuest)
ARPABET_TO_IPA = {
    "AA": "\u0251\u02D0", "AE": "\u00E6", "AH": "\u028C", "AO": "\u0254\u02D0",
    "AW": "a\u028A", "AY": "a\u026A", "B": "b", "CH": "t\u0283",
    "D": "d", "DH": "\u00F0", "EH": "\u025B", "ER": "\u025C\u02D0r",
    "EY": "e\u026A", "F": "f", "G": "\u0261", "HH": "h",
    "IH": "\u026A", "IY": "i\u02D0", "JH": "d\u0292", "K": "k",
    "L": "l", "M": "m", "N": "n", "NG": "\u014B",
    "OW": "o\u028A", "OY": "\u0254\u026A", "P": "p", "R": "r",
    "S": "s", "SH": "\u0283", "T": "t", "TH": "\u03B8",
    "UH": "\u028A", "UW": "u\u02D0", "V": "v", "W": "w",
    "Y": "j", "Z": "z", "ZH": "\u0292",
}


def arpabet_to_ipa(phones: List[str]) -> str:
    """Convert a CMU ARPAbet pronunciation to IPA."""
    ipa_parts: List[str] = []
    for phone in phones:
        base = phone.rstrip("012")
        stress = phone[-1] if phone[-1] in "012" else None

        ipa_char = ARPABET_TO_IPA.get(base, base.lower())

        if stress == "1":
            ipa_parts.append("\u02C8" + ipa_char)  # primary stress
        elif stress == "2":
            ipa_parts.append("\u02CC" + ipa_char)  # secondary stress
        else:
            if base == "AH" and stress == "0":
                ipa_parts.append("\u0259")  # schwa
            else:
                ipa_parts.append(ipa_char)

    return "/" + "".join(ipa_parts) + "/"


def rank_to_cefr(rank: int) -> str:
    """Convert a frequency rank to CEFR level."""
    for boundary, level in CEFR_BOUNDARIES:
        if rank <= boundary:
            return level
    return "C2"


def compute_brown_ranks() -> Dict[str, int]:
    """Compute word frequency ranks from the Brown corpus."""
    log.info("Computing Brown corpus frequency ranks...")
    freq = Counter()
    for word in brown.words():
        w = word.lower()
        if w.isalpha() and len(w) >= 2:
            freq[w] += 1

    # Assign ranks (1 = most frequent)
    ranks = {}
    for rank, (word, _count) in enumerate(freq.most_common(), start=1):
        ranks[word] = rank

    log.info(f"  Ranked {len(ranks)} words from Brown corpus")
    return ranks


def build_cmu_ipa() -> Dict[str, str]:
    """Build word -> IPA mapping from CMU Pronouncing Dictionary."""
    log.info("Building CMU -> IPA mapping...")
    cmu = cmudict.dict()
    ipa_map = {}
    for word, pronunciations in cmu.items():
        # Use first pronunciation variant
        ipa_map[word.lower()] = arpabet_to_ipa(pronunciations[0])

    log.info(f"  {len(ipa_map)} words with IPA")
    return ipa_map


def enhance_db(db_path: str) -> None:
    """Add phonetic and cefr_level columns to wordnet.db."""
    log.info(f"Enhancing database: {db_path}")

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Check current schema
    cur.execute("PRAGMA table_info(words)")
    columns = [row[1] for row in cur.fetchall()]

    if "phonetic" not in columns:
        log.info("Adding 'phonetic' column...")
        cur.execute("ALTER TABLE words ADD COLUMN phonetic TEXT")
    else:
        log.info("'phonetic' column already exists")

    if "cefr_level" not in columns:
        log.info("Adding 'cefr_level' column...")
        cur.execute("ALTER TABLE words ADD COLUMN cefr_level TEXT")
    else:
        log.info("'cefr_level' column already exists")

    conn.commit()

    # Load data sources
    ipa_map = build_cmu_ipa()
    brown_ranks = compute_brown_ranks()

    # Fetch all words
    cur.execute("SELECT word_id, word, lemma, frequency FROM words")
    rows = cur.fetchall()
    log.info(f"Processing {len(rows)} words...")

    ipa_hits = 0
    cefr_assigned = 0
    batch = []

    for word_id, word, lemma, frequency in rows:
        w = word.lower()

        # IPA lookup: try exact word, then lemma
        ipa = ipa_map.get(w) or ipa_map.get(lemma.lower())

        # CEFR: use Brown corpus rank; fall back to existing frequency column
        rank = brown_ranks.get(w) or brown_ranks.get(lemma.lower())
        if rank is None and frequency and frequency < 999999:
            rank = frequency  # use existing frequency as approximate rank
        cefr = rank_to_cefr(rank) if rank else "C2"

        if ipa:
            ipa_hits += 1
        cefr_assigned += 1

        batch.append((ipa, cefr, word_id))

    # Batch update
    log.info("Writing updates...")
    cur.executemany(
        "UPDATE words SET phonetic = ?, cefr_level = ? WHERE word_id = ?",
        batch
    )
    conn.commit()

    # Stats
    total = len(rows)
    log.info(f"Done! IPA: {ipa_hits}/{total} ({100*ipa_hits/total:.1f}%), "
             f"CEFR: {cefr_assigned}/{total} ({100*cefr_assigned/total:.1f}%)")

    # Verify
    cur.execute("SELECT cefr_level, COUNT(*) FROM words GROUP BY cefr_level ORDER BY cefr_level")
    for level, count in cur.fetchall():
        log.info(f"  {level or 'NULL'}: {count} words")

    cur.execute("SELECT COUNT(*) FROM words WHERE phonetic IS NOT NULL")
    ipa_count = cur.fetchone()[0]
    log.info(f"  Words with IPA: {ipa_count}")

    # Check DB size
    cur.execute("PRAGMA page_count")
    pages = cur.fetchone()[0]
    cur.execute("PRAGMA page_size")
    page_size = cur.fetchone()[0]
    size_mb = pages * page_size / (1024 * 1024)
    log.info(f"  Database size: {size_mb:.1f} MB")

    conn.close()


def main():
    parser = argparse.ArgumentParser(description="Enhance wordnet.db with IPA and CEFR")
    parser.add_argument("--db", default=DEFAULT_DB, help="Path to wordnet.db")
    args = parser.parse_args()

    # Ensure NLTK data is available
    for corpus in ["cmudict", "brown"]:
        try:
            nltk.data.find(f"corpora/{corpus}")
        except LookupError:
            log.info(f"Downloading NLTK corpus: {corpus}")
            nltk.download(corpus)

    start = time.time()
    enhance_db(args.db)
    elapsed = time.time() - start
    log.info(f"Total time: {elapsed:.1f}s")


if __name__ == "__main__":
    main()
