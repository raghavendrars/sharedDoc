package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Gunitakshara (Kannada) / Barakhadi (Hindi): one consonant joined with every vowel sign. Tap to hear each syllable. */
final class GuniScreen {
    private final MainActivity a;
    private final String lang;
    private final int color;
    private int consonant;
    private final List<TextView> chips = new ArrayList<>();
    private LinearLayout grid;
    private TextView formula;

    private GuniScreen(MainActivity a, String lang, int color) {
        this.a = a;
        this.lang = lang;
        this.color = color;
    }

    static Runnable open(final MainActivity a, final String lang, final int color) {
        return () -> a.show(new GuniScreen(a, lang, color).make(), false, color, MainActivity.AUDIO);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, lang.equals("kn") ? "ಗುಣಿತಾಕ್ಷರ  Gunitakshara" : "बारहखड़ी  Barakhadi"));

        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        String[][] cons = IndicData.consonants(lang);
        for (int i = 0; i < cons.length; i++) {
            final int k = i;
            TextView c = Ui.text(a, cons[i][0], 24, Ui.INK, true);
            c.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
            c.setPadding(Ui.dp(a, 14), Ui.dp(a, 4), Ui.dp(a, 14), Ui.dp(a, 4));
            Ui.click(c, () -> pick(k));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(Ui.dp(a, 3), Ui.dp(a, 4), Ui.dp(a, 3), Ui.dp(a, 4));
            row.addView(c, lp);
            chips.add(c);
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        page.addView(hs);

        formula = Ui.text(a, "", 24, Color.WHITE, true);
        formula.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        page.addView(formula);

        grid = new LinearLayout(a);
        grid.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 8);
        grid.setPadding(p, 0, p, p);
        ScrollView sv = new ScrollView(a);
        sv.addView(grid);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));

        LinearLayout bottom = new LinearLayout(a);
        bottom.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 14));
        bottom.addView(tool("▶ Say the row", this::playRow), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
        bottom.addView(tool("✍️ Write it", () -> {
            List<String> items = new ArrayList<>();
            for (String[] s : IndicData.row(lang, consonant)) items.add(s[0]);
            WriteScreen.openList(a, lang.equals("kn") ? "Write · ಗುಣಿತಾಕ್ಷರ" : "Write · बारहखड़ी", items, lang, color).run();
        }), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
        page.addView(bottom);
        pick(0);
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private void pick(int k) {
        chips.get(consonant).setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        consonant = k;
        chips.get(k).setBackground(Ui.round(0xFFFFF176, Ui.dp(a, 14)));
        final String[][] syl = IndicData.row(lang, k);
        final String cons = IndicData.consonants(lang)[k][0];
        final String[] signs = IndicData.signs(lang);
        formula.setText("Pick a box to hear it");
        grid.removeAllViews();
        LinearLayout row = null;
        for (int i = 0; i < syl.length; i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(a);
                grid.addView(row, Ui.lp(Ui.MATCH, Ui.dp(a, 96), 0));
            }
            final int j = i;
            LinearLayout tile = new LinearLayout(a);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackground(Ui.tile(a, Color.WHITE, 18));
            tile.setPadding(0, 0, 0, Ui.dp(a, 6));
            tile.addView(Ui.text(a, syl[i][0], 36, Data.BRIGHT[i % Data.BRIGHT.length], true));
            tile.addView(Ui.text(a, syl[i][1], 15, 0xFF78909C, true));
            Ui.click(tile, () -> {
                formula.setText(j == 0 ? cons + "  =  " + syl[0][1] : cons + "  +  ◌" + signs[j] + "  =  " + syl[j][0] + "   (" + syl[j][1] + ")");
                if (!a.speaker.say(syl[j][0], lang)) formula.setText(syl[j][0] + " = " + syl[j][1] + "   (no voice on this phone)");
            });
            row.addView(tile, Ui.lp(a, 0, Ui.MATCH, 1, 4));
            Ui.popIn(tile, 20L * i);
        }
        int rem = syl.length % 3;
        if (rem != 0) for (int x = rem; x < 3; x++) row.addView(new View(a), Ui.lp(a, 0, Ui.MATCH, 1, 4));
        a.speaker.say(cons, lang);
    }

    private void playRow() {
        List<String> lines = new ArrayList<>();
        for (String[] s : IndicData.row(lang, consonant)) lines.add(s[0]);
        if (!a.speaker.sayLines(lines, lang, 250)) formula.setText("No " + (lang.equals("kn") ? "Kannada" : "Hindi") + " voice on this phone");
    }
}

/** Matching game: join each letter (or word) to its picture. Five pairs a round, three rounds. */
final class MatchScreen {
    static final int LETTERS = 0, WORDS = 1;
    private static final int ROUNDS = 3, PAIRS = 4;
    private static final int PLAIN = Color.WHITE, PICKED = 0xFFFFF176, DONE = 0xFFA5D6A7;

    private final MainActivity a;
    private final String lang;
    private final int kind, color;
    private int round, won, total, found, selL = -1, selR = -1;
    private boolean slip;
    private String[][] pairs; // left, picture, spoken
    private int[] order;
    private final TextView[] left = new TextView[PAIRS], right = new TextView[PAIRS];
    private final boolean[] done = new boolean[PAIRS];
    private TextView info;
    private LinearLayout lc, rc;

    private MatchScreen(MainActivity a, String lang, int kind, int color) {
        this.a = a;
        this.lang = lang;
        this.kind = kind;
        this.color = color;
    }

    static Runnable open(final MainActivity a, final String lang, final int kind, final int color) {
        return () -> a.show(new MatchScreen(a, lang, kind, color).make(), false, color, MainActivity.QUIZ);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, kind == LETTERS ? "Match letter & picture" : "Match word & picture"));
        info = Ui.text(a, "", 18, Color.WHITE, true);
        page.addView(info);
        LinearLayout cols = new LinearLayout(a);
        int p = Ui.dp(a, 10);
        cols.setPadding(p, p, p, p);
        lc = new LinearLayout(a);
        lc.setOrientation(LinearLayout.VERTICAL);
        rc = new LinearLayout(a);
        rc.setOrientation(LinearLayout.VERTICAL);
        cols.addView(lc, Ui.lp(0, Ui.MATCH, 1.2f));
        cols.addView(rc, Ui.lp(0, Ui.MATCH, 1));
        page.addView(cols, Ui.lp(Ui.MATCH, 0, 1));
        page.post(this::newRound);
        return page;
    }

    private void newRound() {
        if (round >= ROUNDS) {
            QuizScreen.result(a, (lang.equals("kn") ? "Kannada" : "Hindi") + " matching", won, total, color, open(a, lang, kind, color));
            return;
        }
        List<String[]> pool = new ArrayList<>();
        if (kind == LETTERS) {
            for (String[] r : Data.words(lang)) if (!r[5].isEmpty()) pool.add(new String[]{r[0], r[5], r[2]});
        } else {
            for (String[][] theme : IndicData.vocab(lang)) for (String[] r : theme) pool.add(new String[]{r[0], r[3], r[0]});
            for (String[] r : Data.words(lang)) if (!r[5].isEmpty()) pool.add(new String[]{r[2], r[5], r[2]});
        }
        Collections.shuffle(pool, a.rnd);
        List<String[]> pick = new ArrayList<>();
        List<String> pics = new ArrayList<>();
        for (String[] r : pool) {
            if (pics.contains(r[1])) continue;
            pick.add(r);
            pics.add(r[1]);
            if (pick.size() == PAIRS) break;
        }
        pairs = pick.toArray(new String[0][]);
        List<Integer> ord = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(ord, a.rnd);
        order = new int[PAIRS];
        for (int i = 0; i < PAIRS; i++) order[i] = ord.get(i);
        found = 0;
        selL = selR = -1;
        lc.removeAllViews();
        rc.removeAllViews();
        for (int i = 0; i < PAIRS; i++) {
            final int k = i;
            done[i] = false;
            left[i] = tile(pairs[i][0], kind == LETTERS ? 44 : 30);
            left[i].setOnClickListener(v -> tapLeft(k));
            lc.addView(left[i], Ui.lp(a, Ui.MATCH, 0, 1, 6));
            right[i] = tile(pairs[order[i]][1], 44);
            right[i].setOnClickListener(v -> tapRight(k));
            rc.addView(right[i], Ui.lp(a, Ui.MATCH, 0, 1, 6));
            Ui.popIn(left[i], 50L * i);
            Ui.popIn(right[i], 50L * i + 25);
        }
        info.setText("Round " + (round + 1) + " of " + ROUNDS + " — tap a " + (kind == LETTERS ? "letter" : "word") + ", then its picture");
    }

    private TextView tile(String s, float sp) {
        TextView t = Ui.text(a, s, sp, Ui.INK, true);
        t.setBackground(Ui.tile(a, PLAIN, 18));
        t.setPadding(0, 0, 0, Ui.dp(a, 6));
        return t;
    }

    private void tapLeft(int i) {
        if (done[i]) return;
        if (selL >= 0 && !done[selL]) left[selL].setBackground(Ui.tile(a, PLAIN, 18));
        selL = i;
        left[i].setBackground(Ui.tile(a, PICKED, 18));
        a.speaker.say(kind == LETTERS ? pairs[i][0] : pairs[i][2], lang);
        check();
    }

    private void tapRight(int row) {
        if (done[order[row]]) return;
        if (selR >= 0 && !done[order[selR]]) right[selR].setBackground(Ui.tile(a, PLAIN, 18));
        selR = row;
        right[row].setBackground(Ui.tile(a, PICKED, 18));
        check();
    }

    private void check() {
        if (selL < 0 || selR < 0) return;
        if (order[selR] == selL) {
            done[selL] = true;
            left[selL].setBackground(Ui.tile(a, DONE, 18));
            right[selR].setBackground(Ui.tile(a, DONE, 18));
            Ui.bounce(left[selL]);
            Ui.bounce(right[selR]);
            a.speaker.good();
            a.speaker.say(pairs[selL][2], lang);
            total++;
            if (!slip) {
                won++;
                a.addStars(1);
            }
            slip = false;
            found++;
            if (found == PAIRS) {
                a.confetti();
                round++;
                a.later(1300, this::newRound);
            }
        } else {
            slip = true;
            left[selL].setBackground(Ui.tile(a, PLAIN, 18));
            right[selR].setBackground(Ui.tile(a, PLAIN, 18));
            Ui.shake(left[selL]);
            Ui.shake(right[selR]);
            a.speaker.bad();
        }
        selL = selR = -1;
    }
}
