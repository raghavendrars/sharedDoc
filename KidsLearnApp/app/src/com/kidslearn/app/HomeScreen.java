package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class HomeScreen {
    static final int COLOR = 0xFF7E57C2;

    private static final class Tile {
        final String icon, label;
        final int color;
        final Runnable go;

        Tile(String icon, String label, int color, Runnable go) {
            this.icon = icon;
            this.label = label;
            this.color = color;
            this.go = go;
        }
    }

    private HomeScreen() {
    }

    static Runnable cards(final MainActivity a, final int cat, final String title, final int color) {
        return () -> a.show(CardScreen.build(a, cat, title, color), false, color, MainActivity.AUDIO);
    }

    /** A sub-menu: a list of big buttons under a title. */
    static Runnable menu(final MainActivity a, final String title, final int color, final String[] labels, final Runnable[] go) {
        return () -> {
            LinearLayout page = new LinearLayout(a);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setBackgroundColor(color);
            page.addView(Ui.topBar(a, title));
            LinearLayout box = new LinearLayout(a);
            box.setOrientation(LinearLayout.VERTICAL);
            int p = Ui.dp(a, 24);
            box.setPadding(p, Ui.dp(a, 6), p, p);
            for (int i = 0; i < labels.length; i++) {
                final Runnable r = go[i];
                TextView b = Ui.button(a, labels[i], Color.WHITE, Ui.INK, () -> a.later(140, r));
                LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
                lp.topMargin = Ui.dp(a, 14);
                box.addView(b, lp);
                Ui.popIn(b, 50L * i);
            }
            ScrollView scroll = new ScrollView(a);
            scroll.addView(box);
            page.addView(scroll, Ui.lp(Ui.MATCH, 0, 1));
            a.show(page, false, color);
        };
    }

    private static Runnable quizTime(MainActivity a) {
        int n = QuizScreen.TITLES.length;
        String[] labels = new String[n];
        Runnable[] go = new Runnable[n];
        for (int i = 0; i < n; i++) {
            labels[i] = QuizScreen.ICONS[i] + "  " + QuizScreen.TITLES[i];
            go[i] = QuizScreen.open(a, i);
        }
        return menu(a, a.prefs.getBoolean("online", false) ? "Quiz Time 🌐" : "Quiz Time", QuizScreen.QUIZ_COLOR, labels, go);
    }

    private static Runnable language(MainActivity a, boolean kannada) {
        int c = kannada ? QuizScreen.COLORS[QuizScreen.KANNADA] : QuizScreen.COLORS[QuizScreen.HINDI];
        if (kannada) {
            return menu(a, "ಕನ್ನಡ  Kannada", c,
                    new String[]{"ಸ್ವರಗಳು  ·  Vowels", "ವ್ಯಂಜನಗಳು  ·  Consonants", "ಪದಗಳು  ·  Words", "ಸಂಖ್ಯೆಗಳು  ·  Numbers", "Kannada quiz"},
                    new Runnable[]{cards(a, Data.KN_VOWELS, "Kannada vowels", c), cards(a, Data.KN_CONS, "Kannada consonants", c),
                            cards(a, Data.KN_WORDS, "Kannada words", c), cards(a, Data.KN_NUMS, "Kannada numbers", c), QuizScreen.open(a, QuizScreen.KANNADA)});
        }
        return menu(a, "हिंदी  Hindi", c,
                new String[]{"स्वर  ·  Vowels", "व्यंजन  ·  Consonants", "शब्द  ·  Words", "संख्याएँ  ·  Numbers", "Hindi quiz"},
                new Runnable[]{cards(a, Data.HI_VOWELS, "Hindi vowels", c), cards(a, Data.HI_CONS, "Hindi consonants", c),
                        cards(a, Data.HI_WORDS, "Hindi words", c), cards(a, Data.HI_NUMS, "Hindi numbers", c), QuizScreen.open(a, QuizScreen.HINDI)});
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
        TextView title = Ui.text(a, "KidsLearnApp", 24, Color.WHITE, true);
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
        String today = "⏱ Today: " + Stats.mins(used) + (lim > 0 ? "  of " + (lim + a.stats.bonus(MainActivity.today())) + " min" : "");
        TextView strip = Ui.text(a, today, 15, Ui.INK, true);
        strip.setPadding(0, Ui.dp(a, 8), 0, 0);
        page.addView(strip);

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(a, 8);
        list.setPadding(pad, 0, pad, pad);

        section(a, list, "📖 Learn to read", new Tile[]{
                new Tile("🎬", "Phonics videos", 0xFFE53935, menu(a, "Phonics videos", LessonScreen.COLOR,
                        new String[]{"2-letter words  ·  at, up, go", "3-letter words  ·  cat, sun", "4-letter words  ·  frog, fish", "5-letter words  ·  plant, train"},
                        new Runnable[]{LessonScreen.open(a, 0), LessonScreen.open(a, 1), LessonScreen.open(a, 2), LessonScreen.open(a, 3)})),
                new Tile("👄", "Speak & Lips", 0xFFEC407A, () -> a.show(LipsScreen.build(a), false, LipsScreen.COLOR, MainActivity.AUDIO)),
                new Tile("✍️", "Write & Say", 0xFF00897B, menu(a, "Write & Say", WriteScreen.COLOR,
                        new String[]{"🔤  Letters", "🔢  Numbers", "📝  Words", "ಅ  Kannada", "अ  Hindi"},
                        new Runnable[]{WriteScreen.open(a, 0), WriteScreen.open(a, 1), WriteScreen.open(a, 2), WriteScreen.open(a, 3), WriteScreen.open(a, 4)})),
                new Tile("🧩", "Make Sentences", 0xFF8E24AA, menu(a, "Make Sentences", SentenceScreen.COLOR,
                        new String[]{"🔀  Jumbled sentences", "🤪  Silly sentence maker", "✏️  Write your own"},
                        new Runnable[]{() -> a.show(SentenceScreen.jumbled(a), false, SentenceScreen.COLOR, MainActivity.QUIZ),
                                () -> a.show(SentenceScreen.silly(a), false, SentenceScreen.COLOR),
                                () -> a.show(SentenceScreen.own(a), false, SentenceScreen.COLOR)})),
                new Tile("🔤", "Letters", 0xFFF4511E, cards(a, Data.LETTERS, "Letters", 0xFFF4511E)),
                new Tile("🗣️", "Letter sounds", 0xFF3949AB, menu(a, "Letter sounds", 0xFF3949AB,
                        new String[]{"a b c sounds", "Blends: sh, ch, th…"},
                        new Runnable[]{cards(a, Data.PH_LETTERS, "Letter sounds", 0xFF3949AB), cards(a, Data.PH_BLENDS, "Blends", 0xFF3949AB)}))});

        section(a, list, "❓ Quiz Time", new Tile[]{
                new Tile("❓", "Quiz Time", QuizScreen.QUIZ_COLOR, quizTime(a)),
                new Tile("➕", "Maths quiz", QuizScreen.COLORS[QuizScreen.MATHS], QuizScreen.open(a, QuizScreen.MATHS)),
                new Tile("🔢", "Numbers", 0xFF1E88E5, cards(a, Data.NUMBERS, "Numbers", 0xFF1E88E5))});

        section(a, list, "🗺️ Kannada & Hindi", new Tile[]{
                new Tile("ಅ", "Kannada", QuizScreen.COLORS[QuizScreen.KANNADA], language(a, true)),
                new Tile("अ", "Hindi", QuizScreen.COLORS[QuizScreen.HINDI], language(a, false)),
                new Tile("▶️", "My videos", 0xFFC62828, () -> a.show(ParentScreen.videos(a), false, 0xFFC62828))});

        ScrollView scroll = new ScrollView(a);
        scroll.addView(list);
        page.addView(scroll, Ui.lp(Ui.MATCH, 0, 1));
        return page;
    }

    private static void section(MainActivity a, LinearLayout list, String title, Tile[] tiles) {
        TextView h = Ui.text(a, title, 19, Ui.INK, true);
        h.setGravity(Gravity.START);
        h.setPadding(Ui.dp(a, 8), Ui.dp(a, 14), 0, Ui.dp(a, 2));
        list.addView(h);
        LinearLayout row = null;
        for (int i = 0; i < tiles.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(a);
                list.addView(row, Ui.lp(Ui.MATCH, Ui.dp(a, 118), 0));
            }
            final Tile t = tiles[i];
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
            Ui.click(tile, () -> a.later(140, t.go));
            row.addView(tile, Ui.lp(a, 0, Ui.MATCH, 1, 5));
            Ui.popIn(tile, 25L * i);
        }
        int rem = tiles.length % 3;
        if (rem != 0) for (int k = rem; k < 3; k++) row.addView(new View(a), Ui.lp(a, 0, Ui.MATCH, 1, 5));
    }
}
