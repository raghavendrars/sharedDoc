package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Phonics "video" lessons: the talking face sounds out each letter of a word, then blends the sounds into the word. */
final class LessonScreen {
    static final int COLOR = 0xFF1A237E;
    static final String[] TITLES = {"2-letter words", "3-letter words", "4-letter words", "5-letter words"};

    /** Sounds of a word joined by "-". A sound may say how it is spoken, as in "o=oh". */
    private static final String[][][] LEVELS = {
            {{"a-t", "📍"}, {"i-n", "📥"}, {"o-n", "💡"}, {"u-p", "⬆️"}, {"i-t", "👉"}, {"a-m", "🙋"}, {"i-s=zuh", "✅"},
                    {"g-o=oh", "🚦"}, {"n-o=oh", "🙅"}, {"m-e=ee", "🧒"}, {"w-e=ee", "👫"}, {"h-e=ee", "👦"}},
            {{"c-a-t", "🐱"}, {"d-o-g", "🐶"}, {"s-u-n", "☀️"}, {"p-i-g", "🐷"}, {"h-a-t", "🎩"}, {"b-u-s", "🚌"}, {"c-u-p", "☕"},
                    {"b-e-d", "🛏️"}, {"h-e-n", "🐔"}, {"f-o-x", "🦊"}, {"b-o-x", "📦"}, {"m-a-p", "🗺️"}, {"p-e-n", "🖊️"},
                    {"b-a-t", "🦇"}, {"n-e-t", "🥅"}, {"v-a-n", "🚐"}, {"w-e-b", "🕸️"}, {"b-a-g", "👜"}},
            {{"f-r-o-g", "🐸"}, {"f-i-sh", "🐟"}, {"sh-i-p", "🚢"}, {"d-u-ck", "🦆"}, {"m-i-l-k", "🥛"}, {"d-r-u-m", "🥁"},
                    {"c-r-a-b", "🦀"}, {"s-t-ar", "⭐"}, {"t-r-ee", "🌳"}, {"m-oo-n", "🌙"}, {"h-a-n-d", "✋"}, {"l-a-m-p", "💡"},
                    {"s-o-ck", "🧦"}, {"b-e-ll", "🔔"}, {"t-e-n-t", "⛺"}, {"g-i-f-t", "🎁"}, {"c-l-a-p", "👏"}, {"s-w-i-m", "🏊"}},
            {{"p-l-a-n-t", "🌱"}, {"t-r-ai-n", "🚂"}, {"s-n-ai-l", "🐌"}, {"b-r-u-sh", "🖌️"}, {"ch-i-ck", "🐥"}, {"sh-ee-p", "🐑"},
                    {"t-ee-th", "🦷"}, {"c-l-o-ck", "⏰"}, {"t-r-u-ck", "🚚"}, {"c-r-ow-n", "👑"}, {"qu-ee-n", "👸"}, {"s-w-ee-t", "🍬"},
                    {"g-r-a-ss", "🌿"}, {"s-p-oo-n", "🥄"}, {"l-u-n-ch", "🍱"}, {"s-t-a-m-p", "📮"}}};

    private final MainActivity a;
    private final int level;
    private final String[][] words;
    private int idx, run;
    private boolean playing = true, finished, starGiven;
    private String[] units, sounds;
    private int[] vis;
    private FaceView face;
    private LinearLayout tiles;
    private TextView pic, big, caption, counter, playBtn;
    private ProgressBar bar;

    private LessonScreen(MainActivity a, int level) {
        this.a = a;
        this.level = level;
        this.words = LEVELS[level];
    }

    static Runnable open(final MainActivity a, final int level) {
        return () -> a.show(new LessonScreen(a, level).make(), false, COLOR, MainActivity.VIDEO);
    }

    static String sound(String unit) {
        switch (unit) {
            case "sh":
                return "shh";
            case "ch":
                return "chuh";
            case "th":
                return "thuh";
            case "ck":
                return "kuh";
            case "oo":
                return "ooh";
            case "ee":
                return "eee";
            case "ai":
                return "ay";
            case "ar":
                return "ar";
            case "ow":
                return "ow";
            case "ll":
                return "luh";
            case "ss":
                return "sss";
            case "qu":
                return "kwuh";
            default:
                for (String[] r : Data.LETTER_SOUND) if (r[0].equals(unit)) return r[1];
                return unit;
        }
    }

    private void parse() {
        String[] parts = words[idx][0].split("-");
        units = new String[parts.length];
        sounds = new String[parts.length];
        vis = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split("=");
            units[i] = kv[0];
            sounds[i] = kv.length > 1 ? kv[1] : sound(kv[0]);
            vis[i] = FaceView.viseme(kv.length > 1 ? kv[1] : kv[0]);
        }
    }

    private String word() {
        StringBuilder b = new StringBuilder();
        for (String u : units) b.append(u);
        return b.toString();
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Phonics videos"));

        LinearLayout screen = new LinearLayout(a);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setBackground(Ui.round(0xFF0D1440, Ui.dp(a, 24)));
        int p = Ui.dp(a, 12);
        screen.setPadding(p, p, p, p);
        face = new FaceView(a);
        screen.addView(face, new LinearLayout.LayoutParams(Ui.MATCH, Ui.dp(a, 190)));
        pic = Ui.text(a, "", 60, Color.WHITE, false);
        screen.addView(pic);
        tiles = new LinearLayout(a);
        tiles.setGravity(Gravity.CENTER);
        screen.addView(tiles, Ui.lp(Ui.MATCH, Ui.dp(a, 84), 0));
        big = Ui.text(a, "", 44, Color.WHITE, true);
        screen.addView(big);
        caption = Ui.text(a, "", 18, 0xFFB3E5FC, true);
        caption.setPadding(0, Ui.dp(a, 6), 0, 0);
        screen.addView(caption);
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, 0, 1);
        slp.setMargins(Ui.dp(a, 12), Ui.dp(a, 2), Ui.dp(a, 12), Ui.dp(a, 6));
        page.addView(screen, slp);

        counter = Ui.text(a, "", 15, Color.WHITE, true);
        page.addView(counter);
        bar = QuizScreen.progressBar(a);
        page.addView(bar, QuizScreen.barParams(a));

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 18));
        TextView prev = Ui.circleButton(a, "⏮", 24, () -> go(idx - 1));
        prev.setContentDescription("Previous word");
        playBtn = Ui.circleButton(a, "⏸", 26, this::toggle);
        playBtn.setContentDescription("Play or pause");
        TextView next = Ui.circleButton(a, "⏭", 24, () -> go(idx + 1));
        next.setContentDescription("Next word");
        TextView again = Ui.circleButton(a, "🔁", 24, () -> {
            playing = true;
            startWord();
        });
        again.setContentDescription("Play this word again");
        for (TextView b : new TextView[]{prev, playBtn, next, again}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 62), Ui.dp(a, 62));
            lp.setMargins(Ui.dp(a, 10), 0, Ui.dp(a, 10), 0);
            nav.addView(b, lp);
        }
        page.addView(nav);
        // start once the screen is showing; steps scheduled earlier would be dropped by the screen change
        page.post(this::startWord);
        return page;
    }

    private void go(int i) {
        if (i < 0 || i >= words.length) return;
        idx = i;
        finished = false;
        startWord();
    }

    private void toggle() {
        if (playing) {
            playing = false;
            a.speaker.stop();
            startWord();
        } else {
            if (finished) {
                idx = 0;
                finished = false;
            }
            playing = true;
            startWord();
        }
    }

    private void at(long ms, Runnable r) {
        final int id = run;
        a.later(ms, () -> {
            if (id == run) r.run();
        });
    }

    private void addTile(int k, boolean animate) {
        TextView t = Ui.text(a, units[k], 38, Color.WHITE, true);
        t.setBackground(Ui.tile(a, Data.BRIGHT[k % Data.BRIGHT.length], 16));
        t.setMinWidth(Ui.dp(a, 60));
        t.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), Ui.dp(a, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.MATCH);
        lp.setMargins(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
        tiles.addView(t, lp);
        if (animate) Ui.popIn(t, 0);
    }

    private void startWord() {
        run++;
        parse();
        tiles.removeAllViews();
        pic.setText(words[idx][1]);
        pic.setVisibility(View.INVISIBLE);
        big.setText("");
        face.auto();
        counter.setText(TITLES[level] + " · word " + (idx + 1) + " of " + words.length);
        bar.setMax(words.length);
        bar.setProgress(idx + 1);
        playBtn.setText(playing ? "⏸" : "▶");
        if (!playing) {
            for (int i = 0; i < units.length; i++) addTile(i, false);
            caption.setText("Paused — press ▶ to play");
            return;
        }
        caption.setText("Listen to each sound");
        long t = 200;
        at(t, () -> a.speaker.say(idx == 0 ? "Let's read!" : "Next word!"));
        t += 1400;
        for (int i = 0; i < units.length; i++) {
            final int k = i;
            at(t, () -> {
                addTile(k, true);
                face.show(vis[k]);
                caption.setText("“" + units[k] + "” says “" + sounds[k] + "”");
                a.speaker.say(sounds[k]);
            });
            t += 1300;
        }
        at(t, () -> {
            face.auto();
            caption.setText("Now blend the sounds together");
            a.speaker.say("Now blend them!");
        });
        t += 1700;
        for (int i = 0; i < units.length; i++) {
            final int k = i;
            at(t, () -> {
                View tile = tiles.getChildAt(k);
                if (tile != null) {
                    tile.setBackground(Ui.tile(a, 0xFFFDD835, 16));
                    Ui.bounce(tile);
                }
                face.show(vis[k]);
                a.speaker.say(sounds[k]);
            });
            t += 700;
        }
        at(t, () -> {
            face.auto();
            for (int i = 0; i < tiles.getChildCount(); i++) tiles.getChildAt(i).setBackground(Ui.tile(a, 0xFF43A047, 16));
            big.setText(word());
            Ui.popIn(big, 0);
            pic.setVisibility(View.VISIBLE);
            Ui.popIn(pic, 120);
            caption.setText(join() + "  →  " + word());
            a.speaker.say(word() + "! " + word() + ".");
        });
        t += 2800;
        at(t, () -> {
            if (idx < words.length - 1) {
                idx++;
                startWord();
            } else finish();
        });
    }

    private String join() {
        StringBuilder b = new StringBuilder();
        for (String u : units) {
            if (b.length() > 0) b.append(" · ");
            b.append(u);
        }
        return b.toString();
    }

    private void finish() {
        playing = false;
        finished = true;
        playBtn.setText("▶");
        caption.setText("All done! You read " + words.length + " words ⭐");
        a.speaker.say("Well done! You read all the words!");
        if (!starGiven) {
            starGiven = true;
            a.addStars(2);
            a.confetti();
        }
    }
}
