package com.jworks.eigosage.domain.nlp

import com.jworks.eigosage.data.local.WordNetDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnglishLemmatizer @Inject constructor(
    private val wordNetDao: WordNetDao
) {
    // Irregular verb forms: inflected -> base
    private val irregularVerbs = mapOf(
        "was" to "be", "were" to "be", "been" to "be", "being" to "be", "am" to "be", "is" to "be", "are" to "be",
        "had" to "have", "has" to "have", "having" to "have",
        "did" to "do", "does" to "do", "doing" to "do", "done" to "do",
        "went" to "go", "goes" to "go", "going" to "go", "gone" to "go",
        "said" to "say", "says" to "say", "saying" to "say",
        "made" to "make", "makes" to "make", "making" to "make",
        "knew" to "know", "knows" to "know", "knowing" to "know", "known" to "know",
        "took" to "take", "takes" to "take", "taking" to "take", "taken" to "take",
        "came" to "come", "comes" to "come", "coming" to "come",
        "saw" to "see", "sees" to "see", "seeing" to "see", "seen" to "see",
        "got" to "get", "gets" to "get", "getting" to "get", "gotten" to "get",
        "gave" to "give", "gives" to "give", "giving" to "give", "given" to "give",
        "found" to "find", "finds" to "find", "finding" to "find",
        "thought" to "think", "thinks" to "think", "thinking" to "think",
        "told" to "tell", "tells" to "tell", "telling" to "tell",
        "became" to "become", "becomes" to "become", "becoming" to "become",
        "left" to "leave", "leaves" to "leave", "leaving" to "leave",
        "felt" to "feel", "feels" to "feel", "feeling" to "feel",
        "put" to "put", "putting" to "put",
        "brought" to "bring", "brings" to "bring", "bringing" to "bring",
        "began" to "begin", "begins" to "begin", "beginning" to "begin", "begun" to "begin",
        "kept" to "keep", "keeps" to "keep", "keeping" to "keep",
        "held" to "hold", "holds" to "hold", "holding" to "hold",
        "wrote" to "write", "writes" to "write", "writing" to "write", "written" to "write",
        "stood" to "stand", "stands" to "stand", "standing" to "stand",
        "lost" to "lose", "loses" to "lose", "losing" to "lose",
        "paid" to "pay", "pays" to "pay", "paying" to "pay",
        "met" to "meet", "meets" to "meet", "meeting" to "meet",
        "ran" to "run", "runs" to "run", "running" to "run",
        "sent" to "send", "sends" to "send", "sending" to "send",
        "built" to "build", "builds" to "build", "building" to "build",
        "fell" to "fall", "falls" to "fall", "falling" to "fall", "fallen" to "fall",
        "cut" to "cut", "cuts" to "cut", "cutting" to "cut",
        "read" to "read", "reads" to "read", "reading" to "read",
        "led" to "lead", "leads" to "lead", "leading" to "lead",
        "understood" to "understand", "understands" to "understand", "understanding" to "understand",
        "spoke" to "speak", "speaks" to "speak", "speaking" to "speak", "spoken" to "speak",
        "broke" to "break", "breaks" to "break", "breaking" to "break", "broken" to "break",
        "set" to "set", "sets" to "set", "setting" to "set",
        "sat" to "sit", "sits" to "sit", "sitting" to "sit",
        "spent" to "spend", "spends" to "spend", "spending" to "spend",
        "grew" to "grow", "grows" to "grow", "growing" to "grow", "grown" to "grow",
        "drew" to "draw", "draws" to "draw", "drawing" to "draw", "drawn" to "draw",
        "bought" to "buy", "buys" to "buy", "buying" to "buy",
        "rose" to "rise", "rises" to "rise", "rising" to "rise", "risen" to "rise",
        "drove" to "drive", "drives" to "drive", "driving" to "drive", "driven" to "drive",
        "wore" to "wear", "wears" to "wear", "wearing" to "wear", "worn" to "wear",
        "chose" to "choose", "chooses" to "choose", "choosing" to "choose", "chosen" to "choose",
        "caught" to "catch", "catches" to "catch", "catching" to "catch",
        "taught" to "teach", "teaches" to "teach", "teaching" to "teach",
        "ate" to "eat", "eats" to "eat", "eating" to "eat", "eaten" to "eat",
        "sang" to "sing", "sings" to "sing", "singing" to "sing", "sung" to "sing",
        "lay" to "lie", "lies" to "lie", "lying" to "lie", "lain" to "lie",
        "slept" to "sleep", "sleeps" to "sleep", "sleeping" to "sleep",
        "woke" to "wake", "wakes" to "wake", "waking" to "wake", "woken" to "wake",
        "flew" to "fly", "flies" to "fly", "flying" to "fly", "flown" to "fly",
        "thrown" to "throw", "threw" to "throw", "throws" to "throw", "throwing" to "throw",
        "bit" to "bite", "bites" to "bite", "biting" to "bite", "bitten" to "bite",
        "hid" to "hide", "hides" to "hide", "hiding" to "hide", "hidden" to "hide",
        "swam" to "swim", "swims" to "swim", "swimming" to "swim", "swum" to "swim",
        "shook" to "shake", "shakes" to "shake", "shaking" to "shake", "shaken" to "shake",
        "blew" to "blow", "blows" to "blow", "blowing" to "blow", "blown" to "blow",
        "forgot" to "forget", "forgets" to "forget", "forgetting" to "forget", "forgotten" to "forget",
        "froze" to "freeze", "freezes" to "freeze", "freezing" to "freeze", "frozen" to "freeze",
        "tore" to "tear", "tears" to "tear", "tearing" to "tear", "torn" to "tear",
        "hung" to "hang", "hangs" to "hang", "hanging" to "hang",
        "dug" to "dig", "digs" to "dig", "digging" to "dig",
        "bound" to "bind", "binds" to "bind", "binding" to "bind",
        "struck" to "strike", "strikes" to "strike", "striking" to "strike", "stricken" to "strike",
        "wound" to "wind", "winds" to "wind", "winding" to "wind",
        "lit" to "light", "lights" to "light", "lighting" to "light",
        "meant" to "mean", "means" to "mean", "meaning" to "mean",
        "shot" to "shoot", "shoots" to "shoot", "shooting" to "shoot",
        "dealt" to "deal", "deals" to "deal", "dealing" to "deal",
        "spun" to "spin", "spins" to "spin", "spinning" to "spin"
    )

    // Irregular noun forms: plural -> singular
    private val irregularNouns = mapOf(
        "men" to "man", "women" to "woman", "children" to "child",
        "teeth" to "tooth", "feet" to "foot", "geese" to "goose",
        "mice" to "mouse", "lice" to "louse", "oxen" to "ox",
        "people" to "person", "dice" to "die", "indices" to "index",
        "matrices" to "matrix", "vertices" to "vertex", "appendices" to "appendix",
        "analyses" to "analysis", "axes" to "axis", "bases" to "basis",
        "crises" to "crisis", "diagnoses" to "diagnosis", "ellipses" to "ellipsis",
        "hypotheses" to "hypothesis", "oases" to "oasis", "parentheses" to "parenthesis",
        "syntheses" to "synthesis", "theses" to "thesis",
        "criteria" to "criterion", "phenomena" to "phenomenon", "data" to "datum",
        "media" to "medium", "bacteria" to "bacterium", "curricula" to "curriculum",
        "formulae" to "formula", "fungi" to "fungus", "alumni" to "alumnus",
        "cacti" to "cactus", "nuclei" to "nucleus", "stimuli" to "stimulus",
        "syllabi" to "syllabus", "radii" to "radius", "foci" to "focus",
        "larvae" to "larva", "antennae" to "antenna", "vertebrae" to "vertebra",
        "wolves" to "wolf", "halves" to "half", "knives" to "knife",
        "lives" to "life", "wives" to "wife", "selves" to "self",
        "shelves" to "shelf", "loaves" to "loaf", "leaves" to "leaf",
        "calves" to "calf", "scarves" to "scarf", "thieves" to "thief",
        "sheaves" to "sheaf", "elves" to "elf",
        "sheep" to "sheep", "deer" to "deer", "fish" to "fish",
        "species" to "species", "series" to "series"
    )

    // Irregular adjective forms: comparative/superlative -> base
    private val irregularAdjectives = mapOf(
        "better" to "good", "best" to "good",
        "worse" to "bad", "worst" to "bad",
        "more" to "much", "most" to "much",
        "less" to "little", "least" to "little",
        "farther" to "far", "farthest" to "far",
        "further" to "far", "furthest" to "far",
        "older" to "old", "oldest" to "old",
        "elder" to "old", "eldest" to "old"
    )

    suspend fun lemmatize(word: String): String {
        val lower = word.lowercase().trim()
        if (lower.isEmpty()) return word

        // 1. Check if already a valid base form in WordNet
        if (wordNetDao.getWord(lower) != null) return lower

        // 2. Check irregular forms
        irregularVerbs[lower]?.let { return it }
        irregularNouns[lower]?.let { return it }
        irregularAdjectives[lower]?.let { return it }

        // 3. Try suffix-stripping rules with WordNet verification
        return trySuffixRules(lower) ?: lower
    }

    private suspend fun trySuffixRules(word: String): String? {
        // Order matters: try more specific rules first

        // -ies -> -y (e.g., "stories" -> "story", "carries" -> "carry")
        if (word.endsWith("ies") && word.length > 4) {
            val candidate = word.dropLast(3) + "y"
            if (verifyWord(candidate)) return candidate
        }

        // -ves -> -f/-fe (e.g., "wolves" -> "wolf", but irregular ones already handled)
        if (word.endsWith("ves") && word.length > 4) {
            val candidateF = word.dropLast(3) + "f"
            if (verifyWord(candidateF)) return candidateF
            val candidateFe = word.dropLast(3) + "fe"
            if (verifyWord(candidateFe)) return candidateFe
        }

        // -ing (e.g., "running" -> "run", "making" -> "make", "walking" -> "walk")
        if (word.endsWith("ing") && word.length > 4) {
            // Try dropping -ing (walking -> walk)
            val base = word.dropLast(3)
            if (verifyWord(base)) return base

            // Try dropping -ing + add -e (making -> make)
            val baseE = base + "e"
            if (verifyWord(baseE)) return baseE

            // Try dropping doubled consonant + -ing (running -> run)
            if (base.length >= 2 && base.last() == base[base.length - 2]) {
                val baseDedup = base.dropLast(1)
                if (verifyWord(baseDedup)) return baseDedup
            }

            // Try -ying -> -ie (dying -> die, lying -> lie)
            if (base.endsWith("y") && base.length >= 2) {
                val baseIe = base.dropLast(1) + "ie"
                if (verifyWord(baseIe)) return baseIe
            }
        }

        // -ed (e.g., "walked" -> "walk", "liked" -> "like", "stopped" -> "stop")
        if (word.endsWith("ed") && word.length > 3) {
            // Try dropping -ed (walked -> walk)
            val base = word.dropLast(2)
            if (verifyWord(base)) return base

            // Try dropping -d only (liked -> like)
            val baseD = word.dropLast(1)
            if (verifyWord(baseD)) return baseD

            // Try dropping doubled consonant + -ed (stopped -> stop)
            if (base.length >= 2 && base.last() == base[base.length - 2]) {
                val baseDedup = base.dropLast(1)
                if (verifyWord(baseDedup)) return baseDedup
            }

            // -ied -> -y (carried -> carry)
            if (word.endsWith("ied")) {
                val baseY = word.dropLast(3) + "y"
                if (verifyWord(baseY)) return baseY
            }
        }

        // -es (e.g., "boxes" -> "box", "watches" -> "watch")
        if (word.endsWith("es") && word.length > 3) {
            // Try dropping -es (boxes -> box)
            val base = word.dropLast(2)
            if (verifyWord(base)) return base

            // Try dropping -s only (plates -> plate)
            val baseS = word.dropLast(1)
            if (verifyWord(baseS)) return baseS
        }

        // -s (e.g., "cats" -> "cat")
        if (word.endsWith("s") && !word.endsWith("ss") && word.length > 2) {
            val base = word.dropLast(1)
            if (verifyWord(base)) return base
        }

        // -er (comparative: "bigger" -> "big", "nicer" -> "nice")
        if (word.endsWith("er") && word.length > 3) {
            val base = word.dropLast(2)
            if (verifyWord(base)) return base
            val baseE = base + "e"
            if (verifyWord(baseE)) return baseE
            // doubled consonant: bigger -> big
            if (base.length >= 2 && base.last() == base[base.length - 2]) {
                val baseDedup = base.dropLast(1)
                if (verifyWord(baseDedup)) return baseDedup
            }
        }

        // -est (superlative: "biggest" -> "big", "nicest" -> "nice")
        if (word.endsWith("est") && word.length > 4) {
            val base = word.dropLast(3)
            if (verifyWord(base)) return base
            val baseE = base + "e"
            if (verifyWord(baseE)) return baseE
            if (base.length >= 2 && base.last() == base[base.length - 2]) {
                val baseDedup = base.dropLast(1)
                if (verifyWord(baseDedup)) return baseDedup
            }
        }

        // -ly (e.g., "quickly" -> "quick", "happily" -> "happy")
        if (word.endsWith("ly") && word.length > 3) {
            val base = word.dropLast(2)
            if (verifyWord(base)) return base
            // -ily -> -y (happily -> happy)
            if (word.endsWith("ily")) {
                val baseY = word.dropLast(3) + "y"
                if (verifyWord(baseY)) return baseY
            }
            // -ally -> -al (basically -> basic... but keep "al" form too)
            if (word.endsWith("ally")) {
                val baseAl = word.dropLast(2)
                if (verifyWord(baseAl)) return baseAl
            }
        }

        return null
    }

    private suspend fun verifyWord(candidate: String): Boolean {
        if (candidate.length < 2) return false
        return wordNetDao.getWord(candidate) != null
    }
}
