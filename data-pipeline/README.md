# EigoSage Data Pipeline

Generates the offline WordNet dictionary database (`wordnet.db`) for the EigoSage Android app.

## Quick Start

```bash
# Create virtual environment (recommended)
python3 -m venv venv
source venv/bin/activate
pip install nltk pandas

# Generate database
python3 generate_wordnet_db.py

# Verify existing database
python3 generate_wordnet_db.py --verify-only
```

## Output

- **File**: `../app/src/main/assets/wordnet.db`
- **Format**: SQLite 3 (Room-compatible with `createFromAsset`)
- **Size**: ~39 MB

## Data Sources

### WordNet 3.1 (via NLTK)
- **License**: WordNet License (permissive, allows commercial use)
- **Content**: 147k+ unique word forms, 207k+ definitions
- **Includes**: Definitions, POS tags, examples, synonyms, antonyms
- **URL**: https://wordnet.princeton.edu/

### Frequency Rankings
- **Default**: Built from NLTK corpora (Brown, Reuters, Gutenberg, Webtext, Inaugural)
- **Coverage**: ~81k words ranked by frequency
- **Optional**: Supply a COCA frequency file for better rankings:
  ```bash
  python3 generate_wordnet_db.py --coca-file /path/to/coca_frequency.txt
  ```

## Database Schema

### `words` table
| Column    | Type    | Description                     |
|-----------|---------|---------------------------------|
| word_id   | INTEGER | Primary key                     |
| word      | TEXT    | Word form (unique, indexed)     |
| lemma     | TEXT    | Base/lemma form                 |
| frequency | INTEGER | Frequency rank (1=most common)  |

### `definitions` table
| Column   | Type    | Description                     |
|----------|---------|---------------------------------|
| def_id   | INTEGER | Primary key (auto-increment)    |
| word_id  | INTEGER | FK to words.word_id             |
| pos      | TEXT    | Part of speech (noun/verb/adj/adverb) |
| meaning  | TEXT    | Definition text                 |
| example  | TEXT    | Usage example(s)                |
| synonyms | TEXT    | Comma-separated synonyms        |
| antonyms | TEXT    | Comma-separated antonyms        |

### `etymology` table (reserved for future use)
| Column  | Type    | Description                     |
|---------|---------|---------------------------------|
| word_id | INTEGER | PK + FK to words.word_id        |
| origin  | TEXT    | Language of origin              |
| root    | TEXT    | Root word                       |
| history | TEXT    | Etymology description           |

## Room Compatibility

The database includes `android_metadata` and `room_master_table` tables required by Room's `createFromAsset()`. The identity hash must match the Room Entity annotations in the Android code.

## Statistics

| Metric                  | Value     |
|-------------------------|-----------|
| Total words             | 147,170   |
| Total definitions       | 206,727   |
| Avg definitions/word    | 1.4       |
| Nouns                   | 146,254   |
| Adjectives              | 29,873    |
| Verbs                   | 25,038    |
| Adverbs                 | 5,562     |
| Words with freq data    | 81,556    |
| Database size           | 38.7 MB   |
| Lookup time             | <1ms      |

## CLI Options

```
usage: generate_wordnet_db.py [-h] [--output PATH] [--coca-file PATH] [--verify-only]

Options:
  --output, -o    Output path (default: ../app/src/main/assets/wordnet.db)
  --coca-file     Path to COCA frequency file (optional)
  --verify-only   Only verify existing database, don't regenerate
```

## Regeneration

To regenerate the database after changes:

```bash
source venv/bin/activate
python3 generate_wordnet_db.py
```

The script is idempotent - it deletes any existing database before generating a new one.
