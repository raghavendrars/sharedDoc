package com.kidslearn.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Two-letter words, the three-language word bank, opposites, past tenses and tense sentences. */
final class WordsData {
    private WordsData() {
    }

    /** English 2-letter words: word, example, picture. */
    static final String[] EN_TWO = {
            "am|I am happy.|😊", "an|an apple|🍎", "as|as big as a bus|🚌", "at|at school|🏫", "be|Be kind.|🤗", "by|by the sea|🌊",
            "do|I can do it.|💪", "go|Let's go!|🚦", "he|He is my friend.|👦", "hi|Hi, Mum!|👋", "if|if it rains|🌧️", "in|in the box|📦",
            "is|It is hot.|☀️", "it|Look at it!|👉", "me|Look at me!|🙋", "my|my bag|🎒", "no|No, thank you.|🙅", "of|a cup of tea|☕",
            "oh|Oh no!|😮", "on|on the table|🍽️", "or|tea or milk?|🥛", "ox|The ox is big.|🐂", "so|so happy|😄", "to|Go to bed.|🛏️",
            "up|Look up!|⬆️", "us|Come with us.|👫", "we|We play.|⚽"};

    /** Kannada words of two aksharas: word, meaning, picture. */
    static final String[] KN_TWO = {
            "ಮರ|tree|🌳", "ಜಲ|water|💧", "ಫಲ|fruit|🍎", "ಬಲ|strength|💪", "ದನ|cow|🐄", "ಹಣ|money|💰", "ಮನ|mind|🧠",
            "ಹಸು|cow|🐄", "ಮನೆ|house|🏠", "ಕಾಡು|forest|🌲", "ನೀರು|water|💧", "ಹಾಲು|milk|🥛", "ಹಾವು|snake|🐍", "ಇಲಿ|rat|🐭",
            "ಆನೆ|elephant|🐘", "ಕಿವಿ|ear|👂", "ಮೂಗು|nose|👃", "ಕಾಲು|leg|🦵", "ಎಲೆ|leaf|🍃", "ಹೂವು|flower|🌸", "ಮೀನು|fish|🐟",
            "ನಾಯಿ|dog|🐶", "ಬೆಕ್ಕು|cat|🐱", "ಕೋಳಿ|hen|🐔", "ಕುರಿ|sheep|🐑", "ಕಾಗೆ|crow|🐦", "ಗಿಳಿ|parrot|🦜", "ಮಳೆ|rain|🌧️",
            "ಗಾಳಿ|wind|🌬️", "ತಲೆ|head|🧑", "ಹಲ್ಲು|tooth|🦷", "ಚೆಂಡು|ball|⚽", "ಊಟ|meal|🍛", "ಆಟ|game|🎲", "ಪಾಠ|lesson|📖",
            "ತಾಯಿ|mother|👩", "ಮಗು|baby|👶", "ಕಲ್ಲು|stone|🪨", "ಬಸ್ಸು|bus|🚌", "ದೀಪ|lamp|🪔", "ರೈಲು|train|🚂", "ಮೋಡ|cloud|☁️",
            "ಚೀಲ|bag|🎒", "ಹಾಡು|song|🎵"};

    /** Hindi words of two letters: word, meaning, picture. */
    static final String[] HI_TWO = {
            "घर|house|🏠", "जल|water|💧", "फल|fruit|🍎", "नल|tap|🚰", "बस|bus|🚌", "हल|plough|🌾", "वन|forest|🌲",
            "मन|mind|🧠", "धन|wealth|💰", "तन|body|🧍", "सब|all|👥", "अब|now|⏱️", "दस|ten|🔟", "रस|juice|🧃", "हम|we|👫",
            "चल|walk|🚶", "पल|moment|⏳", "खत|letter|✉️", "सच|truth|✅", "बल|strength|💪", "कप|cup|☕", "टब|tub|🛁",
            "आम|mango|🥭", "नाक|nose|👃", "कान|ear|👂", "गाय|cow|🐄", "चाय|tea|🍵", "मोर|peacock|🦚", "शेर|lion|🦁",
            "सेब|apple|🍎", "बाल|hair|💇", "नदी|river|🏞️", "दूध|milk|🥛", "पानी|water|💧", "ऊँट|camel|🐫", "केला|banana|🍌",
            "हाथ|hand|✋", "पेड़|tree|🌳"};

    static String[] two(String lang) {
        return lang.equals("kn") ? KN_TWO : lang.equals("hi") ? HI_TWO : EN_TWO;
    }

    /** Word bank: "#Category" lines, then English|Kannada|Hindi|picture. */
    private static final String[] BANK = {
            "#Animals",
            "dog|ನಾಯಿ|कुत्ता|🐶", "cat|ಬೆಕ್ಕು|बिल्ली|🐱", "cow|ಹಸು|गाय|🐄", "horse|ಕುದುರೆ|घोड़ा|🐴", "lion|ಸಿಂಹ|शेर|🦁",
            "tiger|ಹುಲಿ|बाघ|🐯", "elephant|ಆನೆ|हाथी|🐘", "monkey|ಕೋತಿ|बंदर|🐒", "rabbit|ಮೊಲ|खरगोश|🐰", "bear|ಕರಡಿ|भालू|🐻",
            "pig|ಹಂದಿ|सूअर|🐷", "sheep|ಕುರಿ|भेड़|🐑", "goat|ಮೇಕೆ|बकरी|🐐", "camel|ಒಂಟೆ|ऊँट|🐫", "deer|ಜಿಂಕೆ|हिरण|🦌",
            "fox|ನರಿ|लोमड़ी|🦊", "rat|ಇಲಿ|चूहा|🐭", "snake|ಹಾವು|साँप|🐍", "frog|ಕಪ್ಪೆ|मेंढक|🐸", "fish|ಮೀನು|मछली|🐟",
            "tortoise|ಆಮೆ|कछुआ|🐢", "squirrel|ಅಳಿಲು|गिलहरी|🐿️", "buffalo|ಎಮ್ಮೆ|भैंस|🐃", "donkey|ಕತ್ತೆ|गधा|",
            "#Birds & insects",
            "bird|ಹಕ್ಕಿ|चिड़िया|🐦", "crow|ಕಾಗೆ|कौआ|🐦", "parrot|ಗಿಳಿ|तोता|🦜", "peacock|ನವಿಲು|मोर|🦚", "hen|ಕೋಳಿ|मुर्गी|🐔",
            "duck|ಬಾತುಕೋಳಿ|बत्तख|🦆", "owl|ಗೂಬೆ|उल्लू|🦉", "ant|ಇರುವೆ|चींटी|🐜", "bee|ಜೇನುನೊಣ|मधुमक्खी|🐝",
            "butterfly|ಚಿಟ್ಟೆ|तितली|🦋", "mosquito|ಸೊಳ್ಳೆ|मच्छर|🦟", "spider|ಜೇಡ|मकड़ी|🕷️", "fly|ನೊಣ|मक्खी|🪰",
            "#Fruits",
            "apple|ಸೇಬು|सेब|🍎", "banana|ಬಾಳೆಹಣ್ಣು|केला|🍌", "mango|ಮಾವಿನಹಣ್ಣು|आम|🥭", "grapes|ದ್ರಾಕ್ಷಿ|अंगूर|🍇",
            "orange|ಕಿತ್ತಳೆ|संतरा|🍊", "watermelon|ಕಲ್ಲಂಗಡಿ|तरबूज़|🍉", "pineapple|ಅನಾನಸ್|अनानास|🍍", "papaya|ಪರಂಗಿ|पपीता|",
            "guava|ಸೀಬೆ|अमरूद|", "coconut|ತೆಂಗಿನಕಾಯಿ|नारियल|🥥", "lemon|ನಿಂಬೆಹಣ್ಣು|नींबू|🍋", "pomegranate|ದಾಳಿಂಬೆ|अनार|",
            "#Vegetables",
            "tomato|ಟೊಮೆಟೊ|टमाटर|🍅", "potato|ಆಲೂಗಡ್ಡೆ|आलू|🥔", "onion|ಈರುಳ್ಳಿ|प्याज़|🧅", "carrot|ಕ್ಯಾರೆಟ್|गाजर|🥕",
            "brinjal|ಬದನೆಕಾಯಿ|बैंगन|🍆", "chilli|ಮೆಣಸಿನಕಾಯಿ|मिर्च|🌶️", "cucumber|ಸೌತೆಕಾಯಿ|खीरा|🥒", "cabbage|ಎಲೆಕೋಸು|पत्तागोभी|🥬",
            "peas|ಬಟಾಣಿ|मटर|", "garlic|ಬೆಳ್ಳುಳ್ಳಿ|लहसुन|🧄", "pumpkin|ಕುಂಬಳಕಾಯಿ|कद्दू|🎃", "corn|ಮುಸುಕಿನ ಜೋಳ|मक्का|🌽",
            "#Food",
            "rice|ಅಕ್ಕಿ|चावल|🍚", "bread|ಬ್ರೆಡ್|ब्रेड|🍞", "milk|ಹಾಲು|दूध|🥛", "water|ನೀರು|पानी|💧", "curd|ಮೊಸರು|दही|",
            "sugar|ಸಕ್ಕರೆ|चीनी|", "salt|ಉಪ್ಪು|नमक|🧂", "tea|ಚಹಾ|चाय|🍵", "egg|ಮೊಟ್ಟೆ|अंडा|🥚", "honey|ಜೇನುತುಪ್ಪ|शहद|🍯",
            "ghee|ತುಪ್ಪ|घी|", "butter|ಬೆಣ್ಣೆ|मक्खन|🧈", "sweets|ಸಿಹಿತಿಂಡಿ|मिठाई|🍬",
            "#Body",
            "head|ತಲೆ|सिर|🧑", "hair|ಕೂದಲು|बाल|💇", "eye|ಕಣ್ಣು|आँख|👁️", "ear|ಕಿವಿ|कान|👂", "nose|ಮೂಗು|नाक|👃",
            "mouth|ಬಾಯಿ|मुँह|👄", "tooth|ಹಲ್ಲು|दाँत|🦷", "tongue|ನಾಲಿಗೆ|जीभ|👅", "hand|ಕೈ|हाथ|✋", "finger|ಬೆರಳು|उँगली|☝️",
            "leg|ಕಾಲು|पैर|🦵", "knee|ಮೊಣಕಾಲು|घुटना|", "stomach|ಹೊಟ್ಟೆ|पेट|", "back|ಬೆನ್ನು|पीठ|", "neck|ಕುತ್ತಿಗೆ|गर्दन|",
            "face|ಮುಖ|चेहरा|🙂",
            "#Family & people",
            "mother|ತಾಯಿ|माँ|👩", "father|ತಂದೆ|पिता|👨", "brother|ಸಹೋದರ|भाई|👦", "sister|ಸಹೋದರಿ|बहन|👧",
            "grandfather|ಅಜ್ಜ|दादा|👴", "grandmother|ಅಜ್ಜಿ|दादी|👵", "son|ಮಗ|बेटा|👦", "daughter|ಮಗಳು|बेटी|👧",
            "baby|ಮಗು|बच्चा|👶", "friend|ಸ್ನೇಹಿತ|दोस्त|🤝", "teacher|ಶಿಕ್ಷಕ|अध्यापक|🧑‍🏫", "boy|ಹುಡುಗ|लड़का|👦",
            "girl|ಹುಡುಗಿ|लड़की|👧", "man|ಗಂಡಸು|आदमी|👨", "woman|ಹೆಂಗಸು|औरत|👩",
            "#Home",
            "house|ಮನೆ|घर|🏠", "door|ಬಾಗಿಲು|दरवाज़ा|🚪", "window|ಕಿಟಕಿ|खिड़की|🪟", "room|ಕೋಣೆ|कमरा|", "chair|ಕುರ್ಚಿ|कुर्सी|🪑",
            "table|ಮೇಜು|मेज़|", "bed|ಹಾಸಿಗೆ|बिस्तर|🛏️", "fan|ಫ್ಯಾನ್|पंखा|", "lamp|ದೀಪ|दीया|🪔", "clock|ಗಡಿಯಾರ|घड़ी|⏰",
            "key|ಬೀಗದ ಕೈ|चाबी|🔑", "plate|ತಟ್ಟೆ|थाली|🍽️", "glass|ಲೋಟ|गिलास|🥛", "spoon|ಚಮಚ|चम्मच|🥄", "pot|ಮಡಕೆ|मटका|🏺",
            "mirror|ಕನ್ನಡಿ|आईना|🪞", "soap|ಸಾಬೂನು|साबुन|🧼", "broom|ಪೊರಕೆ|झाड़ू|🧹", "bucket|ಬಕೆಟ್|बाल्टी|🪣",
            "umbrella|ಛತ್ರಿ|छाता|☂️",
            "#School",
            "school|ಶಾಲೆ|विद्यालय|🏫", "book|ಪುಸ್ತಕ|किताब|📕", "pen|ಪೆನ್ನು|कलम|🖊️", "pencil|ಪೆನ್ಸಿಲ್|पेंसिल|✏️",
            "bag|ಚೀಲ|बस्ता|🎒", "paper|ಕಾಗದ|कागज़|📄", "class|ತರಗತಿ|कक्षा|", "bell|ಗಂಟೆ|घंटी|🔔", "ball|ಚೆಂಡು|गेंद|⚽",
            "game|ಆಟ|खेल|🎲", "lesson|ಪಾಠ|पाठ|📖", "story|ಕಥೆ|कहानी|📚", "song|ಹಾಡು|गाना|🎵",
            "#Colours",
            "red|ಕೆಂಪು|लाल|🔴", "green|ಹಸಿರು|हरा|🟢", "blue|ನೀಲಿ|नीला|🔵", "yellow|ಹಳದಿ|पीला|🟡", "white|ಬಿಳಿ|सफ़ेद|⚪",
            "black|ಕಪ್ಪು|काला|⚫", "orange colour|ಕಿತ್ತಳೆ ಬಣ್ಣ|नारंगी|🟠", "pink|ಗುಲಾಬಿ ಬಣ್ಣ|गुलाबी|", "brown|ಕಂದು|भूरा|🟤",
            "purple|ನೇರಳೆ|बैंगनी|🟣",
            "#Nature",
            "sun|ಸೂರ್ಯ|सूरज|☀️", "moon|ಚಂದ್ರ|चाँद|🌙", "star|ನಕ್ಷತ್ರ|तारा|⭐", "sky|ಆಕಾಶ|आसमान|", "cloud|ಮೋಡ|बादल|☁️",
            "rain|ಮಳೆ|बारिश|🌧️", "wind|ಗಾಳಿ|हवा|🌬️", "tree|ಮರ|पेड़|🌳", "flower|ಹೂವು|फूल|🌸", "leaf|ಎಲೆ|पत्ता|🍃",
            "river|ನದಿ|नदी|🏞️", "sea|ಸಮುದ್ರ|समुद्र|🌊", "mountain|ಬೆಟ್ಟ|पहाड़|⛰️", "stone|ಕಲ್ಲು|पत्थर|🪨", "fire|ಬೆಂಕಿ|आग|🔥",
            "earth|ಭೂಮಿ|धरती|🌍", "day|ದಿನ|दिन|", "night|ರಾತ್ರಿ|रात|🌃", "morning|ಬೆಳಿಗ್ಗೆ|सुबह|🌅", "garden|ತೋಟ|बगीचा|🌷",
            "#Clothes",
            "shirt|ಅಂಗಿ|कमीज़|👕", "cap|ಟೋಪಿ|टोपी|🧢", "shoe|ಶೂ|जूता|👟", "socks|ಕಾಲುಚೀಲ|मोज़े|🧦", "saree|ಸೀರೆ|साड़ी|🥻",
            "clothes|ಬಟ್ಟೆ|कपड़े|👚",
            "#Travel",
            "bus|ಬಸ್ಸು|बस|🚌", "car|ಕಾರು|कार|🚗", "train|ರೈಲು|रेलगाड़ी|🚂", "aeroplane|ವಿಮಾನ|हवाई जहाज़|✈️", "boat|ದೋಣಿ|नाव|⛵",
            "bicycle|ಸೈಕಲ್|साइकिल|🚲", "road|ರಸ್ತೆ|सड़क|🛣️", "ship|ಹಡಗು|जहाज़|🚢",
            "#Doing words",
            "eat|ತಿನ್ನು|खाना|🍽️", "drink|ಕುಡಿ|पीना|🥤", "go|ಹೋಗು|जाना|🚶", "come|ಬಾ|आना|🙋", "sit|ಕುಳಿತುಕೊ|बैठना|🪑",
            "stand|ನಿಲ್ಲು|खड़ा होना|🧍", "run|ಓಡು|दौड़ना|🏃", "walk|ನಡೆ|चलना|🚶", "sleep|ಮಲಗು|सोना|😴", "play|ಆಡು|खेलना|⚽",
            "read|ಓದು|पढ़ना|📖", "write|ಬರೆ|लिखना|✍️", "see|ನೋಡು|देखना|👀", "listen|ಕೇಳು|सुनना|👂", "speak|ಮಾತನಾಡು|बोलना|🗣️",
            "sing|ಹಾಡು|गाना|🎤", "dance|ಕುಣಿ|नाचना|💃", "laugh|ನಗು|हँसना|😂", "cry|ಅಳು|रोना|😢", "jump|ಜಿಗಿ|कूदना|🦘",
            "give|ಕೊಡು|देना|🎁", "open|ತೆರೆ|खोलना|📂", "wash|ತೊಳೆ|धोना|🧼",
            "#Describing words",
            "big|ದೊಡ್ಡ|बड़ा|🐘", "small|ಚಿಕ್ಕ|छोटा|🐜", "hot|ಬಿಸಿ|गरम|🔥", "cold|ತಣ್ಣಗೆ|ठंडा|🧊", "good|ಒಳ್ಳೆಯ|अच्छा|👍",
            "bad|ಕೆಟ್ಟ|बुरा|👎", "new|ಹೊಸ|नया|✨", "old|ಹಳೆಯ|पुराना|", "tall|ಎತ್ತರ|लंबा|🦒", "fast|ವೇಗ|तेज़|🐆",
            "slow|ನಿಧಾನ|धीमा|🐢", "happy|ಸಂತೋಷ|खुश|😊", "sad|ದುಃಖ|दुखी|😢", "clean|ಸ್ವಚ್ಛ|साफ़|🧼", "dirty|ಕೊಳಕು|गंदा|",
            "sweet|ಸಿಹಿ|मीठा|🍬", "sour|ಹುಳಿ|खट्टा|🍋", "heavy|ಭಾರ|भारी|🏋️", "light|ಹಗುರ|हल्का|🪶", "empty|ಖಾಲಿ|खाली|",
            "#Little words",
            "today|ಇಂದು|आज|📅", "yesterday|ನಿನ್ನೆ|बीता कल|", "tomorrow|ನಾಳೆ|आने वाला कल|", "yes|ಹೌದು|हाँ|✅",
            "no|ಇಲ್ಲ|नहीं|❌", "here|ಇಲ್ಲಿ|यहाँ|👇", "there|ಅಲ್ಲಿ|वहाँ|👉", "up|ಮೇಲೆ|ऊपर|⬆️", "down|ಕೆಳಗೆ|नीचे|⬇️"};

    /** One word bank entry. */
    static final class Word {
        final String cat, en, kn, hi, pic;

        Word(String cat, String en, String kn, String hi, String pic) {
            this.cat = cat;
            this.en = en;
            this.kn = kn;
            this.hi = hi;
            this.pic = pic;
        }

        String in(String lang) {
            return lang.equals("kn") ? kn : lang.equals("hi") ? hi : en;
        }
    }

    private static List<Word> cache;

    /** Built-in words plus any list a parent imported (words.csv in the app's files). */
    static synchronized List<Word> bank(Context c) {
        if (cache != null) return cache;
        List<Word> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String cat = "Words";
        for (String line : BANK) {
            if (line.startsWith("#")) {
                cat = line.substring(1);
                continue;
            }
            String[] p = line.split("\\|", -1);
            out.add(new Word(cat, p[0], p[1], p[2], p[3]));
            seen.add(p[0].toLowerCase());
        }
        File f = new File(c.getFilesDir(), "words.csv");
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Word w = parse(line);
                    if (w != null && seen.add(w.en.toLowerCase())) out.add(w);
                }
            } catch (Exception ignored) {
            }
        }
        cache = out;
        return out;
    }

    static synchronized void reset() {
        cache = null;
    }

    /** CSV line: English,Kannada,Hindi[,picture][,category]. Returns null for a header or a bad line. */
    static Word parse(String line) {
        if (line == null) return null;
        String[] p = line.replace("﻿", "").split(",", -1);
        if (p.length < 3) return null;
        String en = p[0].trim(), kn = p[1].trim(), hi = p[2].trim();
        if (en.isEmpty() || kn.isEmpty() || hi.isEmpty() || en.equalsIgnoreCase("english")) return null;
        if (en.length() > 40 || kn.length() > 40 || hi.length() > 40) return null;
        String pic = p.length > 3 ? p[3].trim() : "";
        String cat = p.length > 4 && !p[4].trim().isEmpty() ? p[4].trim() : "My words";
        return new Word(cat, en, kn, hi, pic.length() > 8 ? "" : pic);
    }

    static List<String> categories(Context c) {
        List<String> out = new ArrayList<>();
        for (Word w : bank(c)) if (!out.contains(w.cat)) out.add(w.cat);
        return out;
    }

    // ---- English pairs for match-the-following ----

    static final String[][] OPPOSITES = {{"hot", "cold"}, {"big", "small"}, {"up", "down"}, {"day", "night"}, {"open", "close"},
            {"happy", "sad"}, {"fast", "slow"}, {"tall", "short"}, {"full", "empty"}, {"old", "new"}, {"wet", "dry"}, {"in", "out"},
            {"push", "pull"}, {"light", "dark"}, {"hard", "soft"}, {"top", "bottom"}, {"early", "late"}, {"near", "far"},
            {"clean", "dirty"}, {"loud", "quiet"}};

    static final String[][] PAST = {{"eat", "ate"}, {"go", "went"}, {"run", "ran"}, {"sing", "sang"}, {"write", "wrote"},
            {"drink", "drank"}, {"come", "came"}, {"see", "saw"}, {"sit", "sat"}, {"swim", "swam"}, {"take", "took"},
            {"give", "gave"}, {"sleep", "slept"}, {"make", "made"}, {"buy", "bought"}, {"find", "found"}};

    // ---- tenses: picture, English verb, then present / past / future sentences in English, Kannada, Hindi ----

    static final String[][] TENSES = {
            {"🍽️", "eat", "ತಿನ್ನು", "खाना",
                    "I eat.", "I ate.", "I will eat.",
                    "ನಾನು ತಿನ್ನುತ್ತೇನೆ.", "ನಾನು ತಿಂದೆ.", "ನಾನು ತಿನ್ನುವೆನು.",
                    "मैं खाता हूँ।", "मैंने खाया।", "मैं खाऊँगा।"},
            {"🚶", "go", "ಹೋಗು", "जाना",
                    "I go.", "I went.", "I will go.",
                    "ನಾನು ಹೋಗುತ್ತೇನೆ.", "ನಾನು ಹೋದೆ.", "ನಾನು ಹೋಗುವೆನು.",
                    "मैं जाता हूँ।", "मैं गया।", "मैं जाऊँगा।"},
            {"⚽", "play", "ಆಡು", "खेलना",
                    "I play.", "I played.", "I will play.",
                    "ನಾನು ಆಡುತ್ತೇನೆ.", "ನಾನು ಆಡಿದೆ.", "ನಾನು ಆಡುವೆನು.",
                    "मैं खेलता हूँ।", "मैं खेला।", "मैं खेलूँगा।"},
            {"🎤", "sing", "ಹಾಡು", "गाना",
                    "I sing.", "I sang.", "I will sing.",
                    "ನಾನು ಹಾಡುತ್ತೇನೆ.", "ನಾನು ಹಾಡಿದೆ.", "ನಾನು ಹಾಡುವೆನು.",
                    "मैं गाता हूँ।", "मैंने गाया।", "मैं गाऊँगा।"},
            {"✍️", "write", "ಬರೆ", "लिखना",
                    "I write.", "I wrote.", "I will write.",
                    "ನಾನು ಬರೆಯುತ್ತೇನೆ.", "ನಾನು ಬರೆದೆ.", "ನಾನು ಬರೆಯುವೆನು.",
                    "मैं लिखता हूँ।", "मैंने लिखा।", "मैं लिखूँगा।"},
            {"🥛", "drink", "ಕುಡಿ", "पीना",
                    "I drink.", "I drank.", "I will drink.",
                    "ನಾನು ಕುಡಿಯುತ್ತೇನೆ.", "ನಾನು ಕುಡಿದೆ.", "ನಾನು ಕುಡಿಯುವೆನು.",
                    "मैं पीता हूँ।", "मैंने पिया।", "मैं पिऊँगा।"},
            {"😴", "sleep", "ಮಲಗು", "सोना",
                    "I sleep.", "I slept.", "I will sleep.",
                    "ನಾನು ಮಲಗುತ್ತೇನೆ.", "ನಾನು ಮಲಗಿದೆ.", "ನಾನು ಮಲಗುವೆನು.",
                    "मैं सोता हूँ।", "मैं सोया।", "मैं सोऊँगा।"},
            {"🙋", "come", "ಬಾ", "आना",
                    "I come.", "I came.", "I will come.",
                    "ನಾನು ಬರುತ್ತೇನೆ.", "ನಾನು ಬಂದೆ.", "ನಾನು ಬರುವೆನು.",
                    "मैं आता हूँ।", "मैं आया।", "मैं आऊँगा।"},
            {"🏃", "run", "ಓಡು", "दौड़ना",
                    "I run.", "I ran.", "I will run.",
                    "ನಾನು ಓಡುತ್ತೇನೆ.", "ನಾನು ಓಡಿದೆ.", "ನಾನು ಓಡುವೆನು.",
                    "मैं दौड़ता हूँ।", "मैं दौड़ा।", "मैं दौड़ूँगा।"},
            {"👀", "see", "ನೋಡು", "देखना",
                    "I see.", "I saw.", "I will see.",
                    "ನಾನು ನೋಡುತ್ತೇನೆ.", "ನಾನು ನೋಡಿದೆ.", "ನಾನು ನೋಡುವೆನು.",
                    "मैं देखता हूँ।", "मैंने देखा।", "मैं देखूँगा।"}};

    static final String[] TENSE_NAMES = {"Present", "Past", "Future"};

    /** tense: 0 present, 1 past, 2 future. */
    static String tense(String[] verb, String lang, int tense) {
        int base = lang.equals("kn") ? 7 : lang.equals("hi") ? 10 : 4;
        return verb[base + tense];
    }

    static String verbName(String[] verb, String lang) {
        return lang.equals("kn") ? verb[2] : lang.equals("hi") ? verb[3] : verb[1];
    }

    /** "Yesterday / Today / Tomorrow" in each language. */
    static String when(String lang, int tense) {
        String[][] w = {{"Today", "Yesterday", "Tomorrow"}, {"ಇಂದು", "ನಿನ್ನೆ", "ನಾಳೆ"}, {"आज", "बीता कल", "आने वाला कल"}};
        return w[lang.equals("kn") ? 1 : lang.equals("hi") ? 2 : 0][tense];
    }
}
