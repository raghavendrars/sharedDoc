package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class HomeScreen {
    static final int COLOR = 0xFF7E57C2;
    private static boolean greeted;

    private static final class Tile {
        final String icon, label;
        final int color, minGrade;
        final Runnable go;

        Tile(String icon, String label, int color, Runnable go) {
            this(icon, label, color, go, 0);
        }

        Tile(String icon, String label, int color, Runnable go, int minGrade) {
            this.icon = icon;
            this.label = label;
            this.color = color;
            this.go = go;
            this.minGrade = minGrade;
        }
    }

    private HomeScreen() {
    }

    static Runnable cards(final MainActivity a, final int cat, final String title, final int color) {
        return () -> a.show(CardScreen.build(a, cat, title, color), false, color, MainActivity.AUDIO);
    }

    static Runnable menu(final MainActivity a, final String title, final int color, final String[] labels, final Runnable[] go) {
        return menu(a, title, color, labels, go, null);
    }

    /** A sub-menu: big buttons under a title. Items above the child's class are left out. */
    static Runnable menu(final MainActivity a, final String title, final int color, final String[] labels, final Runnable[] go, final int[] minGrade) {
        return () -> {
            LinearLayout page = new LinearLayout(a);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setBackgroundColor(color);
            page.addView(Ui.topBar(a, title));
            LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            int p = Ui.dp(a, 24);
            box.setPadding(p, Ui.dp(a, 6), p, p);
            int shown = 0, hidden = 0;
            for (int i = 0; i < labels.length; i++) {
                if (minGrade != null && a.grade() < minGrade[i]) {
                    hidden++;
                    continue;
                }
                TextView b = Ui.button(a, labels[i], Color.WHITE, Ui.INK, () -> {
                });
                Grade.guided(a, b, labels[i], go[i]);
                LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
                lp.topMargin = Ui.dp(a, 14);
                box.addView(b, lp);
                Ui.popIn(b, 50L * shown++);
            }
            if (hidden > 0) {
                TextView more = Ui.text(a, "🔒 " + hidden + " more for older classes", 15, 0xCCFFFFFF, false);
                more.setPadding(0, Ui.dp(a, 16), 0, 0);
                box.addView(more);
            }
            ScrollView scroll = new ScrollView(a);
            scroll.addView(box);
            page.addView(scroll, Ui.lp(Ui.MATCH, 0, 1));
            a.show(page, false, color);
            if (a.voiceGuide()) page.post(() -> a.speaker.say(Grade.speakable(title) + ". Tap a button to hear it."));
        };
    }

    private static Runnable quizTime(MainActivity a) {
        int n = QuizScreen.QUIZ_TIME;
        String[] labels = new String[n];
        Runnable[] go = new Runnable[n];
        // Maths, English, EVS, GK, Animals, Plants, Phonics, Kannada, Hindi
        int[] min = {0, 1, 1, 2, 0, 1, 0, 0, 0};
        for (int i = 0; i < n; i++) {
            labels[i] = QuizScreen.ICONS[i] + "  " + QuizScreen.TITLES[i];
            go[i] = QuizScreen.open(a, i);
        }
        return menu(a, a.prefs.getBoolean("online", false) ? "Quiz Time 🌐" : "Quiz Time", QuizScreen.QUIZ_COLOR, labels, go, min);
    }

    private static Runnable maths(MainActivity a) {
        int c = QuizScreen.COLORS[QuizScreen.MATHS];
        return menu(a, "Maths", c,
                new String[]{"🔢  Times tables", "➕  Addition", "➖  Subtraction", "✖️  Multiplication", "➗  Division",
                        "🔗  Match the following", "🎲  Mixed maths quiz", "🔟  Numbers 1–20"},
                new Runnable[]{TablesScreen.open(a), QuizScreen.open(a, QuizScreen.ADD), QuizScreen.open(a, QuizScreen.SUB),
                        QuizScreen.open(a, QuizScreen.MUL), QuizScreen.open(a, QuizScreen.DIV),
                        PairGame.open(a, "Match the sums", "en", "en", PairGame.maths(a.grade())),
                        QuizScreen.open(a, QuizScreen.MATHS), cards(a, Data.NUMBERS, "Numbers", 0xFF1E88E5)},
                new int[]{0, 0, 0, 2, 3, 0, 0, 0});
    }

    private static Runnable matchAll(MainActivity a) {
        return menu(a, "Match the following", PairGame.COLOR,
                new String[]{"➕  Sums and answers", "🖼️  Word and picture", "↔️  Opposites", "🕰️  Verb and past tense",
                        "ಕ  Kannada → English", "क  Hindi → English", "ಕ ↔ क  Kannada ↔ Hindi",
                        "🔢  Numbers in English", "೧  Numbers in Kannada", "१  Numbers in Hindi"},
                new Runnable[]{PairGame.open(a, "Match the sums", "en", "en", PairGame.maths(a.grade())),
                        PairGame.open(a, "Word and picture", "en", "en", PairGame.bank(a, "en", "pic")),
                        PairGame.open(a, "Opposites", "en", "en", PairGame.fixed(WordsData.OPPOSITES)),
                        PairGame.open(a, "Past tense", "en", "en", PairGame.fixed(WordsData.PAST)),
                        PairGame.open(a, "Kannada → English", "kn", "en", PairGame.bank(a, "kn", "en")),
                        PairGame.open(a, "Hindi → English", "hi", "en", PairGame.bank(a, "hi", "en")),
                        PairGame.open(a, "Kannada ↔ Hindi", "kn", "hi", PairGame.bank(a, "kn", "hi")),
                        PairGame.open(a, "Numbers", "en", "en", PairGame.numbers("en")),
                        PairGame.open(a, "Kannada numbers", "kn", "kn", PairGame.numbers("kn")),
                        PairGame.open(a, "Hindi numbers", "hi", "hi", PairGame.numbers("hi"))},
                new int[]{0, 0, 1, 2, 1, 1, 2, 0, 0, 0});
    }

    private static Runnable language(MainActivity a, boolean kannada) {
        final int c = kannada ? QuizScreen.COLORS[QuizScreen.KANNADA] : QuizScreen.COLORS[QuizScreen.HINDI];
        final String lang = kannada ? "kn" : "hi";
        String[] themes = IndicData.THEMES;
        Runnable[] themeGo = new Runnable[themes.length + 1];
        String[] themeLabels = new String[themes.length + 1];
        for (int i = 0; i < themes.length; i++) {
            themeLabels[i] = themes[i];
            themeGo[i] = CardScreen.open(a, IndicData.vocabCards(lang, i), themes[i].substring(themes[i].indexOf(' ') + 1), c);
        }
        themeLabels[themes.length] = "🏆 Vocabulary quiz";
        themeGo[themes.length] = QuizScreen.open(a, kannada ? QuizScreen.KN_VOCAB : QuizScreen.HI_VOCAB);
        Runnable vocab = menu(a, kannada ? "ಪದ ಸಂಪತ್ತು  Vocabulary" : "शब्द भंडार  Vocabulary", c, themeLabels, themeGo);
        Runnable write = menu(a, kannada ? "Write Kannada" : "Write Hindi", c,
                new String[]{"✍️  Letters", "✍️  Numbers " + Data.digits(123, lang), "✍️  " + (kannada ? "Gunitakshara (choose a letter)" : "Barakhadi (choose a letter)")},
                new Runnable[]{WriteScreen.open(a, kannada ? 3 : 4), WriteScreen.open(a, kannada ? 5 : 6), GuniScreen.open(a, lang, c)},
                new int[]{0, 0, 1});
        Runnable games = menu(a, kannada ? "Kannada games" : "Hindi games", c,
                new String[]{"🧩  Match letter & picture", "🧩  Match word & picture", "🔗  Match the following",
                        "❓  Letter quiz", kannada ? "ಕಿ  Gunitakshara quiz" : "कि  Barakhadi quiz", "🏆  Vocabulary quiz",
                        "🕰️  Tense quiz"},
                new Runnable[]{MatchScreen.open(a, lang, MatchScreen.LETTERS, c), MatchScreen.open(a, lang, MatchScreen.WORDS, c),
                        PairGame.open(a, kannada ? "Kannada → English" : "Hindi → English", lang, "en", PairGame.bank(a, lang, "en")),
                        QuizScreen.open(a, kannada ? QuizScreen.KANNADA : QuizScreen.HINDI),
                        QuizScreen.open(a, kannada ? QuizScreen.KN_GUNI : QuizScreen.HI_GUNI),
                        QuizScreen.open(a, kannada ? QuizScreen.KN_VOCAB : QuizScreen.HI_VOCAB),
                        QuizScreen.open(a, kannada ? QuizScreen.TENSE_KN : QuizScreen.TENSE_HI)},
                new int[]{0, 0, 1, 0, 1, 0, 2});
        int[] min = {0, 0, 1, 2, 0, 0, 0, 1, 0, 2, 0, 0};
        if (kannada) {
            return menu(a, "ಕನ್ನಡ  Kannada", c,
                    new String[]{"ಸ್ವರಗಳು  ·  Vowels", "ವ್ಯಂಜನಗಳು  ·  Consonants", "ಗುಣಿತಾಕ್ಷರ  ·  ಕ ಕಾ ಕಿ…", "ಒತ್ತಕ್ಷರ  ·  Compound letters",
                            "ಎರಡು ಅಕ್ಷರದ ಪದಗಳು  ·  2-letter words", "ಪದಗಳು  ·  Words", "ಪದ ಸಂಪತ್ತು  ·  Vocabulary",
                            "ದಿನದ ಮಾತು  ·  Everyday sentences", "ಸಂಖ್ಯೆಗಳು  ·  Numbers", "ಕಾಲಗಳು  ·  Tenses",
                            "✍️  Write Kannada", "🎮  Games & quizzes"},
                    new Runnable[]{cards(a, Data.KN_VOWELS, "Kannada vowels", c), cards(a, Data.KN_CONS, "Kannada consonants", c),
                            GuniScreen.open(a, lang, c), CardScreen.open(a, IndicData.compoundCards(lang), "Compound letters", c),
                            WordScreens.two(a, lang), cards(a, Data.KN_WORDS, "Kannada words", c), vocab,
                            CardScreen.open(a, IndicData.sentenceCards(lang), "Everyday sentences", c),
                            cards(a, Data.KN_NUMS, "Kannada numbers", c), WordScreens.tenses(a, lang), write, games}, min);
        }
        return menu(a, "हिंदी  Hindi", c,
                new String[]{"स्वर  ·  Vowels", "व्यंजन  ·  Consonants", "बारहखड़ी  ·  क का कि…", "संयुक्त अक्षर  ·  Compound letters",
                        "दो अक्षर वाले शब्द  ·  2-letter words", "शब्द  ·  Words", "शब्द भंडार  ·  Vocabulary",
                        "रोज़ की बातें  ·  Everyday sentences", "संख्याएँ  ·  Numbers", "काल  ·  Tenses",
                        "✍️  Write Hindi (chalkboard)", "🎮  Games & quizzes"},
                new Runnable[]{cards(a, Data.HI_VOWELS, "Hindi vowels", c), cards(a, Data.HI_CONS, "Hindi consonants", c),
                        GuniScreen.open(a, lang, c), CardScreen.open(a, IndicData.compoundCards(lang), "Compound letters", c),
                        WordScreens.two(a, lang), cards(a, Data.HI_WORDS, "Hindi words", c), vocab,
                        CardScreen.open(a, IndicData.sentenceCards(lang), "Everyday sentences", c),
                        cards(a, Data.HI_NUMS, "Hindi numbers", c), WordScreens.tenses(a, lang), write, games}, min);
    }

    static View build(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFFFFF8E1);

        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setBackgroundColor(COLOR);
        int p = Ui.dp(a, 14);
        head.setPadding(p, p, p, p);
        TextView title = Ui.text(a, "ThinkPhonics", 24, Color.WHITE, true);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        head.addView(title, Ui.lp(0, Ui.WRAP, 1));
        head.addView(Ui.starPill(a));
        final TextView[] snd = new TextView[1];
        snd[0] = Ui.circleButton(a, a.speaker.on ? "🔊" : "🔇", 18, () -> {
            a.speaker.on = !a.speaker.on;
            a.prefs.edit().putBoolean("sound", a.speaker.on).apply();
            snd[0].setText(a.speaker.on ? "🔊" : "🔇");
            if (a.speaker.on) a.speaker.say("Sound on");
            else a.speaker.stop();
        });
        snd[0].setContentDescription("Sound on or off");
        TextView parents = Ui.circleButton(a, "👪", 18, () -> ParentScreen.open(a));
        parents.setContentDescription("Parents area");
        for (TextView b : new TextView[]{snd[0], parents}) {
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(a, 42), Ui.dp(a, 42));
            sp.leftMargin = Ui.dp(a, 8);
            head.addView(b, sp);
        }
        page.addView(head);

        long used = a.usedToday();
        int lim = a.limitMinutes();
        int streak = a.stats.streak(MainActivity.today());
        String today = Grade.NAMES[a.grade()] + "   ·   ⏱ " + Stats.mins(used)
                + (lim > 0 ? " of " + (lim + a.stats.bonus(MainActivity.today())) + " min" : "")
                + (streak > 0 ? "   ·   🔥 " + streak + "-day streak" : "");
        TextView strip = Ui.text(a, today, 15, Ui.INK, true);
        strip.setPadding(0, Ui.dp(a, 8), 0, 0);
        page.addView(strip);
        if (a.voiceGuide()) {
            TextView hint = Ui.text(a, "🔊 Tap once to hear · tap again to open", 14, 0xFF6A1B9A, true);
            page.addView(hint);
        }

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(a, 8);
        list.setPadding(pad, 0, pad, pad);

        section(a, list, "📖 Learn to read", new Tile[]{
                new Tile("🪜", "Phonics steps", PhonicsCourse.COLOR, PhonicsCourse.open(a)),
                new Tile("🎬", "Phonics videos", 0xFFE53935, menu(a, "Phonics videos", LessonScreen.COLOR,
                        new String[]{"2-letter words  ·  at, up, go", "3-letter words  ·  cat, sun", "4-letter words  ·  frog, fish", "5-letter words  ·  plant, train"},
                        new Runnable[]{LessonScreen.open(a, 0), LessonScreen.open(a, 1), LessonScreen.open(a, 2), LessonScreen.open(a, 3)},
                        new int[]{0, 0, 1, 2})),
                new Tile("👄", "Speak & Lips", 0xFFEC407A, () -> a.show(LipsScreen.build(a), false, LipsScreen.COLOR, MainActivity.AUDIO)),
                new Tile("✍️", "Write & Say", 0xFF00897B, menu(a, "Write & Say", WriteScreen.COLOR,
                        new String[]{"🔤  Letters", "🔢  Numbers", "📝  Words", "ಅ  Kannada", "अ  Hindi"},
                        new Runnable[]{WriteScreen.open(a, 0), WriteScreen.open(a, 1), WriteScreen.open(a, 2), WriteScreen.open(a, 3), WriteScreen.open(a, 4)},
                        new int[]{0, 0, 1, 0, 0})),
                new Tile("🧩", "Make Sentences", 0xFF8E24AA, menu(a, "Make Sentences", SentenceScreen.COLOR,
                        new String[]{"🔀  Jumbled sentences", "🤪  Silly sentence maker", "✏️  Write your own"},
                        new Runnable[]{() -> a.show(SentenceScreen.jumbled(a), false, SentenceScreen.COLOR, MainActivity.QUIZ),
                                () -> a.show(SentenceScreen.silly(a), false, SentenceScreen.COLOR),
                                () -> a.show(SentenceScreen.own(a), false, SentenceScreen.COLOR)},
                        new int[]{2, 1, 2}), 1),
                new Tile("🔤", "Letters", 0xFFF4511E, cards(a, Data.LETTERS, "Letters", 0xFFF4511E)),
                new Tile("🗣️", "Letter sounds", 0xFF3949AB, menu(a, "Letter sounds", 0xFF3949AB,
                        new String[]{"a b c sounds", "Digraphs, vowel teams & blends: sh, ee, oa…", "Sound quiz"},
                        new Runnable[]{cards(a, Data.PH_LETTERS, "Letter sounds", 0xFF3949AB), cards(a, Data.PH_BLENDS, "Sound teams", 0xFF3949AB),
                                QuizScreen.open(a, QuizScreen.PHONICS)})),
                new Tile("📚", "Read-along", StoryScreen.COLOR, StoryScreen.shelf(a)),
                new Tile("👀", "Sight words", QuizScreen.COLORS[QuizScreen.SIGHT], menu(a, "Sight words", QuizScreen.COLORS[QuizScreen.SIGHT],
                        new String[]{"Word cards", "Sight word quiz"},
                        new Runnable[]{cards(a, Data.SIGHT, "Sight words", QuizScreen.COLORS[QuizScreen.SIGHT]), QuizScreen.open(a, QuizScreen.SIGHT)}), 1),
                new Tile("💬", "Talk time", TalkScreen.COLOR, () -> a.show(TalkScreen.build(a), false, TalkScreen.COLOR, MainActivity.AUDIO))});

        section(a, list, "🔤 Words", new Tile[]{
                new Tile("🔡", "2-letter words", WordScreens.COLOR, menu(a, "2-letter words", WordScreens.COLOR,
                        new String[]{"🔤  English  ·  am, go, up", "ಕ  ಕನ್ನಡ  ·  ಮರ, ಹಸು", "क  हिंदी  ·  घर, जल"},
                        new Runnable[]{WordScreens.two(a, "en"), WordScreens.two(a, "kn"), WordScreens.two(a, "hi")})),
                new Tile("📖", "Word bank", 0xFF6D4C41, WordScreens.bank(a), 1),
                new Tile("🕰️", "Tenses", 0xFF6A1B9A, menu(a, "Tenses", 0xFF6A1B9A,
                        new String[]{"🔤  English tenses", "ಕ  ಕನ್ನಡ ಕಾಲಗಳು", "क  हिंदी काल"},
                        new Runnable[]{WordScreens.tenses(a, "en"), WordScreens.tenses(a, "kn"), WordScreens.tenses(a, "hi")}), 2),
                new Tile("🔗", "Match the following", PairGame.COLOR, matchAll(a))});

        section(a, list, "🔢 Maths", new Tile[]{
                new Tile("➕", "Maths", QuizScreen.COLORS[QuizScreen.MATHS], maths(a)),
                new Tile("✖️", "Times tables", TablesScreen.COLOR, TablesScreen.open(a)),
                new Tile("🔟", "Numbers", 0xFF1E88E5, cards(a, Data.NUMBERS, "Numbers", 0xFF1E88E5))});

        section(a, list, "❓ Quiz Time", new Tile[]{
                new Tile("❓", "Quiz Time", QuizScreen.QUIZ_COLOR, quizTime(a)),
                new Tile("📒", "Activity book", 0xFF00897B, menu(a, "Activity book", 0xFF00897B,
                        new String[]{"🧱  Patterns & counting", "🅰️  Missing vowel", "🖼️  Picture & word", "🧐  Odd one out"},
                        new Runnable[]{QuizScreen.open(a, QuizScreen.PATTERNS), QuizScreen.open(a, QuizScreen.VOWELS),
                                QuizScreen.open(a, QuizScreen.PICWORD), QuizScreen.open(a, QuizScreen.ODD)},
                        new int[]{0, 1, 0, 0}))});

        section(a, list, "🏠 Play & make", new Tile[]{
                new Tile("🧱", "Blockhaven Mini", BlockGame.COLOR, BlockGame.open(a)),
                new Tile("🏎️", "Turbo Circuit Mini", RaceGame.COLOR, RaceGame.open(a)),
                new Tile("🔍", "Scavenger hunt", HuntScreen.COLOR, HuntScreen.shelf(a)),
                new Tile("🧺", "Sensory bin", BinScreen.COLOR, menu(a, "Sensory bin", BinScreen.COLOR,
                        new String[]{"🔤  Hidden letters", "🔢  Hidden numbers", "🔺  Hidden shapes"},
                        new Runnable[]{BinScreen.open(a, BinScreen.LETTERS), BinScreen.open(a, BinScreen.NUMBERS), BinScreen.open(a, BinScreen.SHAPES)})),
                new Tile("🧪", "Science lab", GuideScreen.SCIENCE, GuideScreen.shelf(a, true), 1),
                new Tile("🎨", "Colour & paint", ColourScreen.COLOR, () -> a.show(ColourScreen.build(a), false, ColourScreen.COLOR)),
                new Tile("✂️", "Crafts", GuideScreen.CRAFT, GuideScreen.shelf(a, false))});

        section(a, list, "🗺️ Kannada & Hindi", new Tile[]{
                new Tile("ಅ", "Kannada", QuizScreen.COLORS[QuizScreen.KANNADA], language(a, true)),
                new Tile("अ", "Hindi", QuizScreen.COLORS[QuizScreen.HINDI], language(a, false)),
                new Tile("🕉️", "Daily slokas", SlokaScreen.COLOR, SlokaScreen.shelf(a)),
                new Tile("▶️", "My videos", 0xFFC62828, () -> a.show(ParentScreen.videos(a), false, 0xFFC62828))});

        ScrollView scroll = new ScrollView(a);
        scroll.addView(list);
        page.addView(scroll, Ui.lp(Ui.MATCH, 0, 1));
        if (a.voiceGuide() && !greeted) {
            greeted = true;
            page.post(() -> a.speaker.say("Hello! Tap a picture to hear its name. Tap it again to open it."));
        }
        return page;
    }

    private static void section(MainActivity a, LinearLayout list, String title, Tile[] all) {
        List<Tile> tiles = new ArrayList<>();
        for (Tile t : all) if (a.grade() >= t.minGrade) tiles.add(t);
        if (tiles.isEmpty()) return;
        TextView h = Ui.text(a, title, 19, Ui.INK, true);
        h.setGravity(Gravity.START);
        h.setPadding(Ui.dp(a, 8), Ui.dp(a, 14), 0, Ui.dp(a, 2));
        list.addView(h);
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(a);
                list.addView(row, Ui.lp(Ui.MATCH, Ui.dp(a, 118), 0));
            }
            final Tile t = tiles.get(i);
            LinearLayout tile = new LinearLayout(a);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(Ui.tile(a, t.color, 22));
            tile.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 4), Ui.dp(a, 6));
            tile.addView(Ui.text(a, t.icon, 36, Color.WHITE, true));
            TextView label = Ui.text(a, t.label, 15, Color.WHITE, true);
            label.setMaxLines(2);
            tile.addView(label);
            tile.setContentDescription(t.label);
            Grade.guided(a, tile, t.label, t.go);
            row.addView(tile, Ui.lp(a, 0, Ui.MATCH, 1, 5));
            Ui.popIn(tile, 25L * i);
        }
        int rem = tiles.size() % 3;
        if (rem != 0) for (int k = rem; k < 3; k++) row.addView(new View(a), Ui.lp(a, 0, Ui.MATCH, 1, 5));
    }
}
