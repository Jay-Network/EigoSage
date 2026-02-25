import Foundation

final class EnglishLemmatizer {
    private let db: WordNetDatabase

    init(db: WordNetDatabase) {
        self.db = db
    }

    // MARK: - Irregular Forms

    private let irregularVerbs: [String: String] = [
        "was": "be", "were": "be", "been": "be", "being": "be", "am": "be", "is": "be", "are": "be",
        "had": "have", "has": "have", "having": "have",
        "did": "do", "does": "do", "doing": "do", "done": "do",
        "went": "go", "goes": "go", "going": "go", "gone": "go",
        "said": "say", "says": "say", "saying": "say",
        "made": "make", "makes": "make", "making": "make",
        "knew": "know", "knows": "know", "knowing": "know", "known": "know",
        "took": "take", "takes": "take", "taking": "take", "taken": "take",
        "came": "come", "comes": "come", "coming": "come",
        "saw": "see", "sees": "see", "seeing": "see", "seen": "see",
        "got": "get", "gets": "get", "getting": "get", "gotten": "get",
        "gave": "give", "gives": "give", "giving": "give", "given": "give",
        "found": "find", "finds": "find", "finding": "find",
        "thought": "think", "thinks": "think", "thinking": "think",
        "told": "tell", "tells": "tell", "telling": "tell",
        "became": "become", "becomes": "become", "becoming": "become",
        "left": "leave", "leaves": "leave", "leaving": "leave",
        "felt": "feel", "feels": "feel", "feeling": "feel",
        "put": "put", "putting": "put",
        "brought": "bring", "brings": "bring", "bringing": "bring",
        "began": "begin", "begins": "begin", "beginning": "begin", "begun": "begin",
        "kept": "keep", "keeps": "keep", "keeping": "keep",
        "held": "hold", "holds": "hold", "holding": "hold",
        "wrote": "write", "writes": "write", "writing": "write", "written": "write",
        "stood": "stand", "stands": "stand", "standing": "stand",
        "lost": "lose", "loses": "lose", "losing": "lose",
        "paid": "pay", "pays": "pay", "paying": "pay",
        "met": "meet", "meets": "meet", "meeting": "meet",
        "ran": "run", "runs": "run", "running": "run",
        "sent": "send", "sends": "send", "sending": "send",
        "built": "build", "builds": "build", "building": "build",
        "fell": "fall", "falls": "fall", "falling": "fall", "fallen": "fall",
        "cut": "cut", "cuts": "cut", "cutting": "cut",
        "read": "read", "reads": "read", "reading": "read",
        "led": "lead", "leads": "lead", "leading": "lead",
        "understood": "understand", "understands": "understand", "understanding": "understand",
        "spoke": "speak", "speaks": "speak", "speaking": "speak", "spoken": "speak",
        "broke": "break", "breaks": "break", "breaking": "break", "broken": "break",
        "set": "set", "sets": "set", "setting": "set",
        "sat": "sit", "sits": "sit", "sitting": "sit",
        "spent": "spend", "spends": "spend", "spending": "spend",
        "grew": "grow", "grows": "grow", "growing": "grow", "grown": "grow",
        "drew": "draw", "draws": "draw", "drawing": "draw", "drawn": "draw",
        "bought": "buy", "buys": "buy", "buying": "buy",
        "rose": "rise", "rises": "rise", "rising": "rise", "risen": "rise",
        "drove": "drive", "drives": "drive", "driving": "drive", "driven": "drive",
        "wore": "wear", "wears": "wear", "wearing": "wear", "worn": "wear",
        "chose": "choose", "chooses": "choose", "choosing": "choose", "chosen": "choose",
        "caught": "catch", "catches": "catch", "catching": "catch",
        "taught": "teach", "teaches": "teach", "teaching": "teach",
        "ate": "eat", "eats": "eat", "eating": "eat", "eaten": "eat",
        "sang": "sing", "sings": "sing", "singing": "sing", "sung": "sing",
        "lay": "lie", "lies": "lie", "lying": "lie", "lain": "lie",
        "slept": "sleep", "sleeps": "sleep", "sleeping": "sleep",
        "woke": "wake", "wakes": "wake", "waking": "wake", "woken": "wake",
        "flew": "fly", "flies": "fly", "flying": "fly", "flown": "fly",
        "thrown": "throw", "threw": "throw", "throws": "throw", "throwing": "throw",
        "bit": "bite", "bites": "bite", "biting": "bite", "bitten": "bite",
        "hid": "hide", "hides": "hide", "hiding": "hide", "hidden": "hide",
        "swam": "swim", "swims": "swim", "swimming": "swim", "swum": "swim",
        "shook": "shake", "shakes": "shake", "shaking": "shake", "shaken": "shake",
        "blew": "blow", "blows": "blow", "blowing": "blow", "blown": "blow",
        "forgot": "forget", "forgets": "forget", "forgetting": "forget", "forgotten": "forget",
        "froze": "freeze", "freezes": "freeze", "freezing": "freeze", "frozen": "freeze",
        "tore": "tear", "tears": "tear", "tearing": "tear", "torn": "tear",
        "hung": "hang", "hangs": "hang", "hanging": "hang",
        "dug": "dig", "digs": "dig", "digging": "dig",
        "bound": "bind", "binds": "bind", "binding": "bind",
        "struck": "strike", "strikes": "strike", "striking": "strike", "stricken": "strike",
        "wound": "wind", "winds": "wind", "winding": "wind",
        "lit": "light", "lights": "light", "lighting": "light",
        "meant": "mean", "means": "mean", "meaning": "mean",
        "shot": "shoot", "shoots": "shoot", "shooting": "shoot",
        "dealt": "deal", "deals": "deal", "dealing": "deal",
        "spun": "spin", "spins": "spin", "spinning": "spin",
    ]

    private let irregularNouns: [String: String] = [
        "men": "man", "women": "woman", "children": "child",
        "teeth": "tooth", "feet": "foot", "geese": "goose",
        "mice": "mouse", "lice": "louse", "oxen": "ox",
        "people": "person", "dice": "die", "indices": "index",
        "matrices": "matrix", "vertices": "vertex", "appendices": "appendix",
        "analyses": "analysis", "axes": "axis", "bases": "basis",
        "crises": "crisis", "diagnoses": "diagnosis", "ellipses": "ellipsis",
        "hypotheses": "hypothesis", "oases": "oasis", "parentheses": "parenthesis",
        "syntheses": "synthesis", "theses": "thesis",
        "criteria": "criterion", "phenomena": "phenomenon", "data": "datum",
        "media": "medium", "bacteria": "bacterium", "curricula": "curriculum",
        "formulae": "formula", "fungi": "fungus", "alumni": "alumnus",
        "cacti": "cactus", "nuclei": "nucleus", "stimuli": "stimulus",
        "syllabi": "syllabus", "radii": "radius", "foci": "focus",
        "larvae": "larva", "antennae": "antenna", "vertebrae": "vertebra",
        "wolves": "wolf", "halves": "half", "knives": "knife",
        "lives": "life", "wives": "wife", "selves": "self",
        "shelves": "shelf", "loaves": "loaf", "leaves": "leaf",
        "calves": "calf", "scarves": "scarf", "thieves": "thief",
        "sheaves": "sheaf", "elves": "elf",
        "sheep": "sheep", "deer": "deer", "fish": "fish",
        "species": "species", "series": "series",
    ]

    private let irregularAdjectives: [String: String] = [
        "better": "good", "best": "good",
        "worse": "bad", "worst": "bad",
        "more": "much", "most": "much",
        "less": "little", "least": "little",
        "farther": "far", "farthest": "far",
        "further": "far", "furthest": "far",
        "older": "old", "oldest": "old",
        "elder": "old", "eldest": "old",
    ]

    // MARK: - Lemmatize

    func lemmatize(_ word: String) -> String {
        let lower = word.lowercased().trimmingCharacters(in: .whitespaces)
        guard !lower.isEmpty else { return word }

        // 1. Check if already a valid base form
        if db.getWord(lower) != nil { return lower }

        // 2. Check irregular forms
        if let base = irregularVerbs[lower] { return base }
        if let base = irregularNouns[lower] { return base }
        if let base = irregularAdjectives[lower] { return base }

        // 3. Try suffix-stripping rules with WordNet verification
        return trySuffixRules(lower) ?? lower
    }

    // MARK: - Suffix Rules

    private func trySuffixRules(_ word: String) -> String? {
        // -ies -> -y (stories -> story)
        if word.hasSuffix("ies"), word.count > 4 {
            let candidate = String(word.dropLast(3)) + "y"
            if verifyWord(candidate) { return candidate }
        }

        // -ves -> -f/-fe (wolves -> wolf)
        if word.hasSuffix("ves"), word.count > 4 {
            let candidateF = String(word.dropLast(3)) + "f"
            if verifyWord(candidateF) { return candidateF }
            let candidateFe = String(word.dropLast(3)) + "fe"
            if verifyWord(candidateFe) { return candidateFe }
        }

        // -ing (running -> run, making -> make, walking -> walk)
        if word.hasSuffix("ing"), word.count > 4 {
            let base = String(word.dropLast(3))
            if verifyWord(base) { return base }

            let baseE = base + "e"
            if verifyWord(baseE) { return baseE }

            // Doubled consonant (running -> run)
            if base.count >= 2, base.last == base[base.index(before: base.index(before: base.endIndex))] {
                let baseDedup = String(base.dropLast(1))
                if verifyWord(baseDedup) { return baseDedup }
            }

            // -ying -> -ie (dying -> die)
            if base.hasSuffix("y"), base.count >= 2 {
                let baseIe = String(base.dropLast(1)) + "ie"
                if verifyWord(baseIe) { return baseIe }
            }
        }

        // -ed (walked -> walk, liked -> like, stopped -> stop)
        if word.hasSuffix("ed"), word.count > 3 {
            let base = String(word.dropLast(2))
            if verifyWord(base) { return base }

            let baseD = String(word.dropLast(1))
            if verifyWord(baseD) { return baseD }

            // Doubled consonant (stopped -> stop)
            if base.count >= 2, base.last == base[base.index(before: base.index(before: base.endIndex))] {
                let baseDedup = String(base.dropLast(1))
                if verifyWord(baseDedup) { return baseDedup }
            }

            // -ied -> -y (carried -> carry)
            if word.hasSuffix("ied") {
                let baseY = String(word.dropLast(3)) + "y"
                if verifyWord(baseY) { return baseY }
            }
        }

        // -es (boxes -> box, watches -> watch)
        if word.hasSuffix("es"), word.count > 3 {
            let base = String(word.dropLast(2))
            if verifyWord(base) { return base }

            let baseS = String(word.dropLast(1))
            if verifyWord(baseS) { return baseS }
        }

        // -s (cats -> cat)
        if word.hasSuffix("s"), !word.hasSuffix("ss"), word.count > 2 {
            let base = String(word.dropLast(1))
            if verifyWord(base) { return base }
        }

        // -er (bigger -> big, nicer -> nice)
        if word.hasSuffix("er"), word.count > 3 {
            let base = String(word.dropLast(2))
            if verifyWord(base) { return base }
            let baseE = base + "e"
            if verifyWord(baseE) { return baseE }
            if base.count >= 2, base.last == base[base.index(before: base.index(before: base.endIndex))] {
                let baseDedup = String(base.dropLast(1))
                if verifyWord(baseDedup) { return baseDedup }
            }
        }

        // -est (biggest -> big, nicest -> nice)
        if word.hasSuffix("est"), word.count > 4 {
            let base = String(word.dropLast(3))
            if verifyWord(base) { return base }
            let baseE = base + "e"
            if verifyWord(baseE) { return baseE }
            if base.count >= 2, base.last == base[base.index(before: base.index(before: base.endIndex))] {
                let baseDedup = String(base.dropLast(1))
                if verifyWord(baseDedup) { return baseDedup }
            }
        }

        // -ly (quickly -> quick, happily -> happy)
        if word.hasSuffix("ly"), word.count > 3 {
            let base = String(word.dropLast(2))
            if verifyWord(base) { return base }
            if word.hasSuffix("ily") {
                let baseY = String(word.dropLast(3)) + "y"
                if verifyWord(baseY) { return baseY }
            }
            if word.hasSuffix("ally") {
                let baseAl = String(word.dropLast(2))
                if verifyWord(baseAl) { return baseAl }
            }
        }

        return nil
    }

    private func verifyWord(_ candidate: String) -> Bool {
        guard candidate.count >= 2 else { return false }
        return db.getWord(candidate) != nil
    }
}
