package com.kidslearn.app;

import java.util.ArrayList;
import java.util.List;

/** One flash card. glyph / emoji / sub may be absent. */
final class Item {
    final String glyph, emoji, title, sub, say;
    final int color;
    String note = "";   // small line under the big glyph
    String lang = "en"; // voice used to speak it: en, kn or hi
    String hear;        // word the child can say into the microphone, or null

    Item(String glyph, String emoji, String title, String sub, String say, int color) {
        this.glyph = glyph;
        this.emoji = emoji;
        this.title = title;
        this.sub = sub;
        this.say = say;
        this.color = color;
    }
}

/** Learning content. Pictures are emoji, so the app carries no image files. */
final class Data {
    static final int LETTERS = 0, NUMBERS = 1, KN_VOWELS = 2, KN_CONS = 3, KN_WORDS = 4, KN_NUMS = 5,
            HI_VOWELS = 6, HI_CONS = 7, HI_WORDS = 8, HI_NUMS = 9, PH_LETTERS = 10, PH_BLENDS = 11;

    static final int[] BRIGHT = {0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFEC407A, 0xFF00ACC1};

    static final String[][] ABC = {
            {"A", "Apple", "🍎"}, {"B", "Ball", "⚽"}, {"C", "Cat", "🐱"}, {"D", "Dog", "🐶"}, {"E", "Elephant", "🐘"},
            {"F", "Fish", "🐟"}, {"G", "Grapes", "🍇"}, {"H", "Hat", "🎩"}, {"I", "Ice cream", "🍦"}, {"J", "Jeans", "👖"},
            {"K", "Key", "🔑"}, {"L", "Lion", "🦁"}, {"M", "Monkey", "🐵"}, {"N", "Nose", "👃"}, {"O", "Orange", "🍊"},
            {"P", "Pencil", "✏️"}, {"Q", "Queen", "👸"}, {"R", "Rabbit", "🐰"}, {"S", "Sun", "☀️"}, {"T", "Tree", "🌳"},
            {"U", "Umbrella", "☂️"}, {"V", "Violin", "🎻"}, {"W", "Watch", "⌚"}, {"X", "Xylophone", "🎶"}, {"Y", "Yacht", "⛵"},
            {"Z", "Zebra", "🦓"}};

    static final String[] NUM = {"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty"};

    // ---- Kannada and Hindi: letter, how it sounds, example word, word in English letters, meaning, picture ----

    static final String[][] KN_V = {
            {"ಅ", "a", "ಅಮ್ಮ", "amma", "mother", "👩"}, {"ಆ", "aa", "ಆನೆ", "aane", "elephant", "🐘"},
            {"ಇ", "i", "ಇಲಿ", "ili", "rat", "🐭"}, {"ಈ", "ee", "ಈಜು", "eeju", "swim", "🏊"},
            {"ಉ", "u", "ಉಂಗುರ", "ungura", "ring", "💍"}, {"ಊ", "oo", "ಊಟ", "oota", "meal", "🍛"},
            {"ಋ", "ru", "ಋಷಿ", "rushi", "sage", "🧘"}, {"ಎ", "e", "ಎಲೆ", "ele", "leaf", "🍃"},
            {"ಏ", "ae", "ಏಡಿ", "aedi", "crab", "🦀"}, {"ಐ", "ai", "ಐದು", "aidu", "five", "🖐️"},
            {"ಒ", "o", "ಒಂಟೆ", "onte", "camel", "🐫"}, {"ಓ", "oa", "ಓದು", "oadu", "read", "📖"},
            {"ಔ", "au", "ಔಷಧ", "aushadha", "medicine", "💊"}, {"ಅಂ", "am", "ಅಂಗಡಿ", "angadi", "shop", "🏪"},
            {"ಅಃ", "aha", "", "", "", ""}};

    static final String[][] KN_C = {
            {"ಕ", "ka", "ಕಪ್ಪೆ", "kappe", "frog", "🐸"}, {"ಖ", "kha", "ಖರ್ಜೂರ", "kharjura", "dates", "🌴"},
            {"ಗ", "ga", "ಗಡಿಯಾರ", "gadiyara", "clock", "⏰"}, {"ಘ", "gha", "ಘಂಟೆ", "ghante", "bell", "🔔"},
            {"ಙ", "nga", "", "", "", ""}, {"ಚ", "cha", "ಚಂದ್ರ", "chandra", "moon", "🌙"},
            {"ಛ", "chha", "ಛತ್ರಿ", "chhatri", "umbrella", "☂️"}, {"ಜ", "ja", "ಜಿಂಕೆ", "jinke", "deer", "🦌"},
            {"ಝ", "jha", "ಝರಿ", "jhari", "stream", "🏞️"}, {"ಞ", "nya", "", "", "", ""},
            {"ಟ", "Ta", "ಟೊಮೆಟೊ", "tometo", "tomato", "🍅"}, {"ಠ", "Tha", "", "", "", ""},
            {"ಡ", "Da", "ಡಬ್ಬ", "dabba", "box", "📦"}, {"ಢ", "Dha", "ಢಕ್ಕೆ", "dhakke", "drum", "🥁"},
            {"ಣ", "Na", "", "", "", ""}, {"ತ", "ta", "ತಟ್ಟೆ", "tatte", "plate", "🍽️"},
            {"ಥ", "tha", "", "", "", ""}, {"ದ", "da", "ದನ", "dana", "cow", "🐄"},
            {"ಧ", "dha", "ಧ್ವಜ", "dhvaja", "flag", "🚩"}, {"ನ", "na", "ನಾಯಿ", "naayi", "dog", "🐶"},
            {"ಪ", "pa", "ಪುಸ್ತಕ", "pustaka", "book", "📕"}, {"ಫ", "pha", "ಫಲ", "phala", "fruit", "🍎"},
            {"ಬ", "ba", "ಬಾಳೆಹಣ್ಣು", "baalehannu", "banana", "🍌"}, {"ಭ", "bha", "ಭೂಮಿ", "bhoomi", "earth", "🌍"},
            {"ಮ", "ma", "ಮರ", "mara", "tree", "🌳"}, {"ಯ", "ya", "ಯಂತ್ರ", "yantra", "machine", "⚙️"},
            {"ರ", "ra", "ರೈಲು", "railu", "train", "🚂"}, {"ಲ", "la", "ಲಾರಿ", "laari", "lorry", "🚚"},
            {"ವ", "va", "ವಿಮಾನ", "vimaana", "aeroplane", "✈️"}, {"ಶ", "sha", "ಶಾಲೆ", "shaale", "school", "🏫"},
            {"ಷ", "Sha", "ಷಟ್ಕೋನ", "shatkona", "hexagon", ""}, {"ಸ", "sa", "ಸೂರ್ಯ", "surya", "sun", "☀️"},
            {"ಹ", "ha", "ಹಾವು", "haavu", "snake", "🐍"}, {"ಳ", "La", "", "", "", ""}};

    static final String[][] HI_V = {
            {"अ", "a", "अनानास", "anaanaas", "pineapple", "🍍"}, {"आ", "aa", "आग", "aag", "fire", "🔥"},
            {"इ", "i", "इंद्रधनुष", "indradhanush", "rainbow", "🌈"}, {"ई", "ee", "ईंट", "eent", "brick", "🧱"},
            {"उ", "u", "उल्लू", "ullu", "owl", "🦉"}, {"ऊ", "oo", "ऊँट", "oont", "camel", "🐫"},
            {"ऋ", "ri", "ऋषि", "rishi", "sage", "🧘"}, {"ए", "e", "एक", "ek", "one", "☝️"},
            {"ऐ", "ai", "ऐनक", "ainak", "spectacles", "👓"}, {"ओ", "o", "ओस", "os", "dew", "💧"},
            {"औ", "au", "औरत", "aurat", "woman", "👩"}, {"अं", "an", "अंगूर", "angoor", "grapes", "🍇"},
            {"अः", "aha", "", "", "", ""}};

    static final String[][] HI_C = {
            {"क", "ka", "कबूतर", "kabootar", "pigeon", "🕊️"}, {"ख", "kha", "खरगोश", "khargosh", "rabbit", "🐰"},
            {"ग", "ga", "गाय", "gaay", "cow", "🐄"}, {"घ", "gha", "घर", "ghar", "house", "🏠"},
            {"ङ", "nga", "", "", "", ""}, {"च", "cha", "चम्मच", "chammach", "spoon", "🥄"},
            {"छ", "chha", "छाता", "chhaata", "umbrella", "☂️"}, {"ज", "ja", "जहाज", "jahaaz", "ship", "🚢"},
            {"झ", "jha", "झंडा", "jhanda", "flag", "🚩"}, {"ञ", "nya", "", "", "", ""},
            {"ट", "Ta", "टमाटर", "tamaatar", "tomato", "🍅"}, {"ठ", "Tha", "ठेला", "thela", "cart", "🛒"},
            {"ड", "Da", "डमरू", "damroo", "small drum", "🥁"}, {"ढ", "Dha", "ढोल", "dhol", "drum", "🪘"},
            {"ण", "Na", "", "", "", ""}, {"त", "ta", "तितली", "titli", "butterfly", "🦋"},
            {"थ", "tha", "थाली", "thaali", "plate", "🍽️"}, {"द", "da", "दरवाज़ा", "darwaaza", "door", "🚪"},
            {"ध", "dha", "धनुष", "dhanush", "bow", "🏹"}, {"न", "na", "नाव", "naav", "boat", "⛵"},
            {"प", "pa", "पत्ता", "patta", "leaf", "🍃"}, {"फ", "pha", "फल", "phal", "fruit", "🍎"},
            {"ब", "ba", "बकरी", "bakri", "goat", "🐐"}, {"भ", "bha", "भालू", "bhaalu", "bear", "🐻"},
            {"म", "ma", "मछली", "machhli", "fish", "🐟"}, {"य", "ya", "योग", "yog", "yoga", "🧘"},
            {"र", "ra", "रस", "ras", "juice", "🍹"}, {"ल", "la", "लड़का", "ladka", "boy", "👦"},
            {"व", "va", "वन", "van", "forest", "🌲"}, {"श", "sha", "शेर", "sher", "lion", "🦁"},
            {"ष", "Sha", "षट्कोण", "shatkon", "hexagon", ""}, {"स", "sa", "सूरज", "sooraj", "sun", "☀️"},
            {"ह", "ha", "हाथी", "haathi", "elephant", "🐘"}, {"क्ष", "ksha", "क्षत्रिय", "kshatriya", "warrior", "⚔️"},
            {"त्र", "tra", "त्रिशूल", "trishool", "trident", "🔱"}, {"ज्ञ", "gya", "ज्ञानी", "gyaani", "wise person", "🎓"}};

    static final String[] KN_NUM = {"ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು", "ಹತ್ತು",
            "ಹನ್ನೊಂದು", "ಹನ್ನೆರಡು", "ಹದಿಮೂರು", "ಹದಿನಾಲ್ಕು", "ಹದಿನೈದು", "ಹದಿನಾರು", "ಹದಿನೇಳು", "ಹದಿನೆಂಟು", "ಹತ್ತೊಂಬತ್ತು", "ಇಪ್ಪತ್ತು"};
    static final String[] KN_NUM_TR = {"ondu", "eradu", "mooru", "naalku", "aidu", "aaru", "eelu", "entu", "ombattu", "hattu",
            "hannondu", "hanneradu", "hadimooru", "hadinaalku", "hadinaidu", "hadinaaru", "hadineelu", "hadinentu", "hattombattu", "ippattu"};
    static final String[] HI_NUM = {"एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
            "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस"};
    static final String[] HI_NUM_TR = {"ek", "do", "teen", "chaar", "paanch", "chhah", "saat", "aath", "nau", "das",
            "gyaarah", "baarah", "terah", "chaudah", "pandrah", "solah", "satrah", "athaarah", "unnees", "bees"};

    // ---- Phonics ----

    /** Letter, how it sounds (written so the voice can read it), example word, picture. */
    static final String[][] LETTER_SOUND = {
            {"a", "ah", "apple", "🍎"}, {"b", "buh", "ball", "⚽"}, {"c", "kuh", "cat", "🐱"}, {"d", "duh", "dog", "🐶"},
            {"e", "eh", "egg", "🥚"}, {"f", "fuh", "fish", "🐟"}, {"g", "guh", "goat", "🐐"}, {"h", "huh", "hat", "🎩"},
            {"i", "ih", "ink", "🖋️"}, {"j", "juh", "jam", "🍓"}, {"k", "kuh", "key", "🔑"}, {"l", "luh", "lion", "🦁"},
            {"m", "muh", "moon", "🌙"}, {"n", "nuh", "nose", "👃"}, {"o", "aw", "orange", "🍊"}, {"p", "puh", "pig", "🐷"},
            {"q", "kwuh", "queen", "👸"}, {"r", "ruh", "rabbit", "🐰"}, {"s", "sss", "sun", "☀️"}, {"t", "tuh", "tree", "🌳"},
            {"u", "uh", "umbrella", "☂️"}, {"v", "vuh", "van", "🚐"}, {"w", "wuh", "watch", "⌚"}, {"x", "ks", "box", "📦"},
            {"y", "yuh", "yacht", "⛵"}, {"z", "zuh", "zebra", "🦓"}};

    /** Letter pair, spoken sound, example word, picture, note. */
    static final String[][] BLEND = {
            {"sh", "shh", "ship", "🚢", ""}, {"ch", "chuh", "cheese", "🧀", ""}, {"th", "thuh", "thumb", "👍", ""},
            {"ph", "fuh", "phone", "📱", "ph sounds like f"}, {"wh", "wuh", "whale", "🐳", ""}, {"ck", "kuh", "duck", "🦆", "ck sounds like k"},
            {"qu", "kwuh", "queen", "👸", ""}, {"oo", "ooh", "moon", "🌙", ""}, {"ee", "eee", "bee", "🐝", ""},
            {"bl", "bluh", "blue", "🔵", ""}, {"br", "bruh", "bread", "🍞", ""}, {"cl", "kluh", "clock", "⏰", ""},
            {"cr", "kruh", "crab", "🦀", ""}, {"dr", "druh", "drum", "🥁", ""}, {"fl", "fluh", "flower", "🌸", ""},
            {"fr", "fruh", "frog", "🐸", ""}, {"gr", "gruh", "grapes", "🍇", ""}, {"pl", "pluh", "plane", "✈️", ""},
            {"sn", "snuh", "snake", "🐍", ""}, {"sp", "spuh", "spoon", "🥄", ""}, {"st", "stuh", "star", "⭐", ""},
            {"sw", "swuh", "swim", "🏊", ""}, {"tr", "truh", "tree", "🌳", ""},
            {"ps", "sss", "psychology", "🧠", "p is silent — ps sounds like s"}, {"kn", "nuh", "knife", "🔪", "k is silent — kn sounds like n"},
            {"wr", "ruh", "write", "✍️", "w is silent — wr sounds like r"}, {"mb", "muh", "lamb", "🐑", "b is silent — mb sounds like m"}};

    private Data() {
    }

    static String repeat(String s, int n, int perLine) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0 && perLine > 0 && i % perLine == 0) b.append('\n');
            b.append(s);
        }
        return b.toString();
    }

    static String[][] join(String[][] x, String[][] y) {
        String[][] out = new String[x.length + y.length][];
        System.arraycopy(x, 0, out, 0, x.length);
        System.arraycopy(y, 0, out, x.length, y.length);
        return out;
    }

    /** Rows that have an example word. */
    static List<String[]> words(String lang) {
        List<String[]> out = new ArrayList<>();
        for (String[] r : lang.equals("kn") ? join(KN_V, KN_C) : join(HI_V, HI_C)) if (!r[2].isEmpty()) out.add(r);
        return out;
    }

    /** 12 → "೧೨" (Kannada) or "१२" (Devanagari). */
    static String digits(int n, String lang) {
        char zero = lang.equals("kn") ? '೦' : '०';
        StringBuilder b = new StringBuilder();
        for (char ch : String.valueOf(n).toCharArray()) b.append((char) (zero + (ch - '0')));
        return b.toString();
    }

    private static String orNull(String s) {
        return s.isEmpty() ? null : s;
    }

    private static void letters(List<Item> out, String[][] src, String lang) {
        for (int i = 0; i < src.length; i++) {
            String[] r = src[i];
            boolean word = !r[2].isEmpty();
            Item it = new Item(r[0], orNull(r[5]), r[2], word ? r[3] + "  ·  " + r[4] : "", word ? r[0] + ". " + r[2] : r[0], BRIGHT[i % BRIGHT.length]);
            it.note = r[1];
            it.lang = lang;
            it.hear = orNull(r[2]);
            out.add(it);
        }
    }

    static List<Item> items(int cat) {
        List<Item> out = new ArrayList<>();
        switch (cat) {
            case LETTERS:
                for (int i = 0; i < ABC.length; i++) {
                    String[] r = ABC[i];
                    Item it = new Item(r[0] + " " + r[0].toLowerCase(), r[2], r[0] + " for " + r[1], "", r[0] + ", for " + r[1], BRIGHT[i % BRIGHT.length]);
                    it.hear = r[1];
                    out.add(it);
                }
                break;
            case NUMBERS:
                for (int i = 0; i < NUM.length; i++)
                    out.add(new Item(String.valueOf(i + 1), repeat("⭐", i + 1, 5), NUM[i], "", NUM[i], BRIGHT[i % BRIGHT.length]));
                break;
            case KN_VOWELS:
                letters(out, KN_V, "kn");
                break;
            case KN_CONS:
                letters(out, KN_C, "kn");
                break;
            case HI_VOWELS:
                letters(out, HI_V, "hi");
                break;
            case HI_CONS:
                letters(out, HI_C, "hi");
                break;
            case KN_WORDS:
            case HI_WORDS: {
                String lang = cat == KN_WORDS ? "kn" : "hi";
                List<String[]> ws = words(lang);
                for (int i = 0; i < ws.size(); i++) {
                    String[] r = ws.get(i);
                    Item it = new Item(r[2], orNull(r[5]), r[3], r[4], r[2], BRIGHT[i % BRIGHT.length]);
                    it.lang = lang;
                    it.hear = r[2];
                    out.add(it);
                }
                break;
            }
            case KN_NUMS:
            case HI_NUMS: {
                String lang = cat == KN_NUMS ? "kn" : "hi";
                String[] w = lang.equals("kn") ? KN_NUM : HI_NUM, tr = lang.equals("kn") ? KN_NUM_TR : HI_NUM_TR;
                for (int i = 0; i < w.length; i++) {
                    Item it = new Item(digits(i + 1, lang), null, w[i], (i + 1) + "  ·  " + tr[i], w[i], BRIGHT[i % BRIGHT.length]);
                    it.lang = lang;
                    it.hear = w[i];
                    out.add(it);
                }
                break;
            }
            case PH_LETTERS:
                for (int i = 0; i < LETTER_SOUND.length; i++) {
                    String[] r = LETTER_SOUND[i];
                    Item it = new Item(r[0], r[3], r[2], "", r[0].toUpperCase() + " says, " + r[1] + ". " + r[2] + ".", BRIGHT[i % BRIGHT.length]);
                    it.note = "says “" + r[1] + "”";
                    it.hear = r[2];
                    out.add(it);
                }
                break;
            default:
                for (int i = 0; i < BLEND.length; i++) {
                    String[] r = BLEND[i];
                    String spelled = r[0].substring(0, 1).toUpperCase() + ", " + r[0].substring(1).toUpperCase();
                    Item it = new Item(r[0], r[3], r[2], r[4], spelled + " says, " + r[1] + ". As in " + r[2] + ".", BRIGHT[i % BRIGHT.length]);
                    it.note = "says “" + r[1] + "”";
                    it.hear = r[2];
                    out.add(it);
                }
                break;
        }
        return out;
    }
}
