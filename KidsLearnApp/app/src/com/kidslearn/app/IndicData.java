package com.kidslearn.app;

import java.util.ArrayList;
import java.util.List;

/** Kannada and Hindi extras: gunitakshara / barakhadi, compound letters, vocabulary themes and everyday sentences. */
final class IndicData {
    private IndicData() {
    }

    // ---- vowel signs (matras) added to a consonant ----

    static final String[] KN_SIGNS = {"", "ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೃ", "ೆ", "ೇ", "ೈ", "ೊ", "ೋ", "ೌ", "ಂ", "ಃ"};
    static final String[] KN_SOUNDS = {"a", "aa", "i", "ee", "u", "oo", "ru", "e", "ae", "ai", "o", "oa", "au", "am", "aha"};
    static final String[] HI_SIGNS = {"", "ा", "ि", "ी", "ु", "ू", "ृ", "े", "ै", "ो", "ौ", "ं", "ः"};
    static final String[] HI_SOUNDS = {"a", "aa", "i", "ee", "u", "oo", "ri", "e", "ai", "o", "au", "an", "ah"};

    static String[] signs(String lang) {
        return lang.equals("kn") ? KN_SIGNS : HI_SIGNS;
    }

    static String[] sounds(String lang) {
        return lang.equals("kn") ? KN_SOUNDS : HI_SOUNDS;
    }

    static String[][] consonants(String lang) {
        return lang.equals("kn") ? Data.KN_C : Data.HI_C;
    }

    /** "ka" → "k", "ksha" → "ksh". */
    static String base(String sound) {
        return sound.endsWith("a") ? sound.substring(0, sound.length() - 1) : sound;
    }

    /** {syllable, sound} for one consonant with every vowel sign, e.g. ಕ ಕಾ ಕಿ … */
    static String[][] row(String lang, int consonant) {
        String[] c = consonants(lang)[consonant];
        String[] sg = signs(lang), sd = sounds(lang);
        String[][] out = new String[sg.length][];
        for (int i = 0; i < sg.length; i++) out[i] = new String[]{c[0] + sg[i], base(c[1]) + sd[i]};
        return out;
    }

    // ---- compound letters: compound, example word, word in English letters, meaning, picture ----

    static final String[][] KN_COMPOUND = {
            {"ಕ್ಕ", "ಅಕ್ಕ", "akka", "elder sister", "👧"}, {"ಮ್ಮ", "ಅಮ್ಮ", "amma", "mother", "👩"},
            {"ಣ್ಣ", "ಅಣ್ಣ", "aNNa", "elder brother", "👦"}, {"ಲ್ಲ", "ಹಲ್ಲು", "hallu", "tooth", "🦷"},
            {"ತ್ತ", "ಹತ್ತು", "hattu", "ten", "🔟"}, {"ಪ್ಪ", "ಕಪ್ಪೆ", "kappe", "frog", "🐸"},
            {"ಸ್ತ", "ಪುಸ್ತಕ", "pustaka", "book", "📕"}, {"ಪ್ರ", "ಪ್ರಾಣಿ", "praaNi", "animal", "🐾"},
            {"ಗ್ರ", "ಗ್ರಾಮ", "graama", "village", "🏘️"}, {"ಕ್ಷ", "ಪಕ್ಷಿ", "pakshi", "bird", "🐦"},
            {"ಡ್ಡ", "ದೊಡ್ಡ", "dodda", "big", "🐘"}, {"ಟ್ಟ", "ತಟ್ಟೆ", "tatte", "plate", "🍽️"},
            {"ನ್ನ", "ಅನ್ನ", "anna", "cooked rice", "🍚"}, {"ಜ್ಞ", "ಜ್ಞಾನ", "jnaana", "knowledge", "🎓"}};

    static final String[][] HI_COMPOUND = {
            {"च्च", "बच्चा", "bachcha", "child", "👶"}, {"ल्ल", "बिल्ली", "billi", "cat", "🐱"},
            {"ट्ट", "मिट्टी", "mitti", "soil", "🪴"}, {"स्त", "बस्ता", "basta", "school bag", "🎒"},
            {"त्र", "पत्र", "patra", "letter", "✉️"}, {"क्ष", "कक्षा", "kaksha", "class", "🏫"},
            {"ज्ञ", "ज्ञान", "gyaan", "knowledge", "🎓"}, {"प्र", "प्रकाश", "prakaash", "light", "💡"},
            {"क्क", "पक्का", "pakka", "ripe, firm", "🥭"}, {"म्म", "अम्मा", "amma", "mother", "👩"},
            {"न्न", "गन्ना", "ganna", "sugarcane", "🎋"}, {"श्र", "श्रम", "shram", "hard work", "💪"}};

    /** "ಕ್ಕ" → "ಕ್ + ಕ": the half letter and the full letter. */
    static String breakdown(String compound) {
        char virama = compound.indexOf('್') >= 0 ? '್' : '्';
        int v = compound.indexOf(virama);
        return v < 0 ? compound : compound.substring(0, v + 1) + "  +  " + compound.substring(v + 1);
    }

    // ---- vocabulary themes: word, English letters, meaning, picture ----

    static final String[] THEMES = {"👪 Family", "🧍 Body", "🎨 Colours", "🍛 Food"};

    static final String[][][] KN_VOCAB = {
            {{"ಅಮ್ಮ", "amma", "mother", "👩"}, {"ಅಪ್ಪ", "appa", "father", "👨"}, {"ಅಜ್ಜ", "ajja", "grandfather", "👴"},
                    {"ಅಜ್ಜಿ", "ajji", "grandmother", "👵"}, {"ಅಣ್ಣ", "aNNa", "elder brother", "👦"}, {"ಅಕ್ಕ", "akka", "elder sister", "👧"},
                    {"ತಮ್ಮ", "tamma", "younger brother", "🧒"}, {"ತಂಗಿ", "tangi", "younger sister", "👧"}},
            {{"ತಲೆ", "tale", "head", "🧑"}, {"ಕಣ್ಣು", "kaNNu", "eye", "👁️"}, {"ಕಿವಿ", "kivi", "ear", "👂"}, {"ಮೂಗು", "moogu", "nose", "👃"},
                    {"ಬಾಯಿ", "baayi", "mouth", "👄"}, {"ಕೈ", "kai", "hand", "✋"}, {"ಕಾಲು", "kaalu", "leg", "🦵"}, {"ಹಲ್ಲು", "hallu", "tooth", "🦷"}},
            {{"ಕೆಂಪು", "kempu", "red", "🔴"}, {"ಹಸಿರು", "hasiru", "green", "🟢"}, {"ನೀಲಿ", "neeli", "blue", "🔵"},
                    {"ಹಳದಿ", "haLadi", "yellow", "🟡"}, {"ಬಿಳಿ", "biLi", "white", "⚪"}, {"ಕಪ್ಪು", "kappu", "black", "⚫"},
                    {"ಕಿತ್ತಳೆ", "kittaLe", "orange", "🟠"}, {"ನೇರಳೆ", "neraLe", "purple", "🟣"}},
            {{"ಅನ್ನ", "anna", "cooked rice", "🍚"}, {"ಹಾಲು", "haalu", "milk", "🥛"}, {"ನೀರು", "neeru", "water", "💧"},
                    {"ಹಣ್ಣು", "haNNu", "fruit", "🍎"}, {"ಮೊಸರು", "mosaru", "curd", "🥣"}, {"ರೊಟ್ಟಿ", "rotti", "flatbread", "🫓"},
                    {"ದೋಸೆ", "dose", "dosa", "🥞"}, {"ಸಕ್ಕರೆ", "sakkare", "sugar", "🍬"}}};

    static final String[][][] HI_VOCAB = {
            {{"माँ", "maa", "mother", "👩"}, {"पिता", "pita", "father", "👨"}, {"दादा", "daada", "grandfather", "👴"},
                    {"दादी", "daadi", "grandmother", "👵"}, {"भाई", "bhai", "brother", "👦"}, {"बहन", "behen", "sister", "👧"},
                    {"चाचा", "chaacha", "uncle", "🧔"}, {"बच्चा", "bachcha", "child", "👶"}},
            {{"सिर", "sir", "head", "🧑"}, {"आँख", "aankh", "eye", "👁️"}, {"कान", "kaan", "ear", "👂"}, {"नाक", "naak", "nose", "👃"},
                    {"मुँह", "munh", "mouth", "👄"}, {"हाथ", "haath", "hand", "✋"}, {"पैर", "pair", "foot", "🦶"}, {"दाँत", "daant", "tooth", "🦷"}},
            {{"लाल", "laal", "red", "🔴"}, {"हरा", "hara", "green", "🟢"}, {"नीला", "neela", "blue", "🔵"}, {"पीला", "peela", "yellow", "🟡"},
                    {"सफ़ेद", "safed", "white", "⚪"}, {"काला", "kaala", "black", "⚫"}, {"नारंगी", "naarangi", "orange", "🟠"},
                    {"बैंगनी", "baingani", "purple", "🟣"}},
            {{"चावल", "chaawal", "rice", "🍚"}, {"दूध", "doodh", "milk", "🥛"}, {"पानी", "paani", "water", "💧"}, {"फल", "phal", "fruit", "🍎"},
                    {"दही", "dahi", "curd", "🥣"}, {"रोटी", "roti", "flatbread", "🫓"}, {"दाल", "daal", "lentils", "🍲"},
                    {"चीनी", "cheeni", "sugar", "🍬"}}};

    static String[][][] vocab(String lang) {
        return lang.equals("kn") ? KN_VOCAB : HI_VOCAB;
    }

    // ---- everyday sentences: sentence, English letters, meaning, picture ----

    static final String[][] KN_SENTENCES = {
            {"ನಮಸ್ಕಾರ.", "Namaskaara.", "Hello.", "🙏"},
            {"ನಿನ್ನ ಹೆಸರು ಏನು?", "Ninna hesaru enu?", "What is your name?", "🙋"},
            {"ನನ್ನ ಹೆಸರು ರಾಮ.", "Nanna hesaru Raama.", "My name is Rama.", "🧒"},
            {"ನೀನು ಹೇಗಿದ್ದೀಯ?", "Neenu hegiddeeya?", "How are you?", "🙂"},
            {"ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ.", "Naanu chennaagiddene.", "I am fine.", "😊"},
            {"ನನಗೆ ನೀರು ಬೇಕು.", "Nanage neeru beku.", "I want water.", "💧"},
            {"ನಾನು ಶಾಲೆಗೆ ಹೋಗುತ್ತೇನೆ.", "Naanu shaalege hoguttene.", "I go to school.", "🏫"},
            {"ಊಟ ಆಯಿತಾ?", "Oota aayitaa?", "Have you eaten?", "🍛"},
            {"ಬಾ, ಆಟ ಆಡೋಣ.", "Baa, aata aadona.", "Come, let's play.", "⚽"},
            {"ಧನ್ಯವಾದಗಳು.", "Dhanyavaadagalu.", "Thank you.", "💐"}};

    static final String[][] HI_SENTENCES = {
            {"नमस्ते।", "Namaste.", "Hello.", "🙏"},
            {"आपका नाम क्या है?", "Aapka naam kya hai?", "What is your name?", "🙋"},
            {"मेरा नाम राम है।", "Mera naam Ram hai.", "My name is Ram.", "🧒"},
            {"आप कैसे हैं?", "Aap kaise hain?", "How are you?", "🙂"},
            {"मैं ठीक हूँ।", "Main theek hoon.", "I am fine.", "😊"},
            {"मुझे पानी चाहिए।", "Mujhe paani chaahiye.", "I want water.", "💧"},
            {"मैं स्कूल जाता हूँ।", "Main school jaata hoon.", "I go to school.", "🏫"},
            {"क्या आपने खाना खाया?", "Kya aapne khaana khaaya?", "Have you eaten?", "🍛"},
            {"आओ, खेलते हैं।", "Aao, khelte hain.", "Come, let's play.", "⚽"},
            {"धन्यवाद।", "Dhanyavaad.", "Thank you.", "💐"}};

    // ---- cards ----

    private static Item card(String glyph, String note, String emoji, String title, String sub, String say, int i, String lang) {
        Item it = new Item(glyph, emoji, title, sub, say, Data.BRIGHT[i % Data.BRIGHT.length]);
        it.note = note;
        it.lang = lang;
        it.hear = say;
        return it;
    }

    static List<Item> compoundCards(String lang) {
        List<Item> out = new ArrayList<>();
        String[][] src = lang.equals("kn") ? KN_COMPOUND : HI_COMPOUND;
        for (int i = 0; i < src.length; i++) {
            String[] r = src[i];
            out.add(card(r[0], breakdown(r[0]), r[4], r[1], r[2] + "  ·  " + r[3], r[1], i, lang));
        }
        return out;
    }

    static List<Item> vocabCards(String lang, int theme) {
        List<Item> out = new ArrayList<>();
        String[][] src = vocab(lang)[theme];
        for (int i = 0; i < src.length; i++)
            out.add(card(src[i][0], "", src[i][3], src[i][1], src[i][2], src[i][0], i, lang));
        return out;
    }

    static List<Item> sentenceCards(String lang) {
        List<Item> out = new ArrayList<>();
        String[][] src = lang.equals("kn") ? KN_SENTENCES : HI_SENTENCES;
        for (int i = 0; i < src.length; i++) {
            Item it = card(src[i][0], "", src[i][3], src[i][1], src[i][2], src[i][0], i, lang);
            it.hear = null; // whole sentences are hard for voice typing; listening and reading only
            out.add(it);
        }
        return out;
    }
}
