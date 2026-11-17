package com.kidslearn.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Match the following: two columns; tap one on the left, then its partner on the right, and a line joins them. */
final class PairGame {
    interface Source {
        /** n pairs {left, right}, all lefts different and all rights different. */
        String[][] pairs(Random rnd, int n);
    }

    static final int COLOR = 0xFF00838F;
    private static final int ROUNDS = 3, N = 5;

    private final MainActivity a;
    private final String title, leftLang, rightLang;
    private final Source src;
    private int round, won, total, found, selL = -1, selR = -1;
    private boolean slip;
    private String[][] pairs;
    private int[] order;
    private final TextView[] left = new TextView[N], right = new TextView[N];
    private final boolean[] done = new boolean[N];
    private LinearLayout lc, rc;
    private Lines lines;
    private TextView info;

    private PairGame(MainActivity a, String title, String leftLang, String rightLang, Source src) {
        this.a = a;
        this.title = title;
        this.leftLang = leftLang;
        this.rightLang = rightLang;
        this.src = src;
    }

    static Runnable open(final MainActivity a, final String title, final String leftLang, final String rightLang, final Source src) {
        return () -> a.show(new PairGame(a, title, leftLang, rightLang, src).make(), false, COLOR, MainActivity.QUIZ);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, title));
        info = Ui.text(a, "", 17, Color.WHITE, true);
        page.addView(info);
        FrameLayout area = new FrameLayout(a);
        LinearLayout cols = new LinearLayout(a);
        int p = Ui.dp(a, 10);
        cols.setPadding(p, p, p, p);
        lc = new LinearLayout(a);
        lc.setOrientation(LinearLayout.VERTICAL);
        rc = new LinearLayout(a);
        rc.setOrientation(LinearLayout.VERTICAL);
        cols.addView(lc, Ui.lp(0, Ui.MATCH, 1));
        cols.addView(new View(a), Ui.lp(0, Ui.MATCH, 0.35f));
        cols.addView(rc, Ui.lp(0, Ui.MATCH, 1));
        area.addView(cols, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        lines = new Lines(a);
        area.addView(lines, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        page.addView(area, Ui.lp(Ui.MATCH, 0, 1));
        page.post(this::newRound);
        return page;
    }

    private void newRound() {
        if (round >= ROUNDS) {
            QuizScreen.result(a, title, won, total, COLOR, open(a, title, leftLang, rightLang, src));
            return;
        }
        pairs = src.pairs(a.rnd, N);
        List<Integer> ord = new ArrayList<>();
        for (int i = 0; i < pairs.length; i++) ord.add(i);
        Collections.shuffle(ord, a.rnd);
        order = new int[pairs.length];
        for (int i = 0; i < pairs.length; i++) order[i] = ord.get(i);
        found = 0;
        selL = selR = -1;
        lines.clear();
        lc.removeAllViews();
        rc.removeAllViews();
        for (int i = 0; i < pairs.length; i++) {
            final int k = i;
            done[i] = false;
            left[i] = tile(pairs[i][0]);
            left[i].setOnClickListener(v -> tapLeft(k));
            lc.addView(left[i], Ui.lp(a, Ui.MATCH, 0, 1, 5));
            right[i] = tile(pairs[order[i]][1]);
            right[i].setOnClickListener(v -> tapRight(k));
            rc.addView(right[i], Ui.lp(a, Ui.MATCH, 0, 1, 5));
        }
        info.setText("Round " + (round + 1) + " of " + ROUNDS + " — match the following");
        if (a.voiceGuide()) a.speaker.say("Match the following. Tap one on the left, then its partner on the right.");
    }

    private TextView tile(String s) {
        int len = s.length();
        TextView t = Ui.text(a, s, len <= 3 ? 30 : len <= 8 ? 22 : len <= 14 ? 17 : 14, Ui.INK, true);
        t.setBackground(Ui.tile(a, Color.WHITE, 16));
        t.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 4), Ui.dp(a, 6));
        return t;
    }

    private void tapLeft(int i) {
        if (done[i]) return;
        if (selL >= 0 && !done[selL]) left[selL].setBackground(Ui.tile(a, Color.WHITE, 16));
        selL = i;
        left[i].setBackground(Ui.tile(a, 0xFFFFF176, 16));
        a.speaker.say(pairs[i][0], leftLang);
        check();
    }

    private void tapRight(int row) {
        if (done[order[row]]) return;
        if (selR >= 0 && !done[order[selR]]) right[selR].setBackground(Ui.tile(a, Color.WHITE, 16));
        selR = row;
        right[row].setBackground(Ui.tile(a, 0xFFFFF176, 16));
        a.speaker.say(pairs[order[row]][1], rightLang);
        check();
    }

    private void check() {
        if (selL < 0 || selR < 0) return;
        if (order[selR] == selL) {
            int c = Data.BRIGHT[found % Data.BRIGHT.length];
            done[selL] = true;
            left[selL].setBackground(Ui.tile(a, 0xFFE8F5E9, 16));
            right[selR].setBackground(Ui.tile(a, 0xFFE8F5E9, 16));
            lines.add(left[selL], right[selR], c);
            Ui.bounce(left[selL]);
            Ui.bounce(right[selR]);
            a.speaker.good();
            total++;
            if (!slip) {
                won++;
                a.addStars(1);
            }
            slip = false;
            found++;
            if (found == pairs.length) {
                a.confetti();
                a.speaker.say("All matched!");
                round++;
                a.later(1500, this::newRound);
            }
        } else {
            slip = true;
            left[selL].setBackground(Ui.tile(a, Color.WHITE, 16));
            right[selR].setBackground(Ui.tile(a, Color.WHITE, 16));
            Ui.shake(left[selL]);
            Ui.shake(right[selR]);
            a.speaker.bad();
        }
        selL = selR = -1;
    }

    /** Draws a line from each matched left tile to its right tile. */
    static final class Lines extends View {
        private final List<View[]> pairs = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] me = new int[2], va = new int[2], vb = new int[2];

        Lines(Context c) {
            super(c);
            p.setStrokeWidth(Ui.dp(c, 4));
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        void add(View x, View y, int color) {
            pairs.add(new View[]{x, y});
            colors.add(color);
            invalidate();
            postDelayed(this::invalidate, 450); // again once the tiles' bounce has finished
        }

        void clear() {
            pairs.clear();
            colors.clear();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            getLocationInWindow(me);
            for (int i = 0; i < pairs.size(); i++) {
                View x = pairs.get(i)[0], y = pairs.get(i)[1];
                x.getLocationInWindow(va);
                y.getLocationInWindow(vb);
                float x1 = va[0] - me[0] + x.getWidth(), y1 = va[1] - me[1] + x.getHeight() / 2f;
                float x2 = vb[0] - me[0], y2 = vb[1] - me[1] + y.getHeight() / 2f;
                p.setColor(colors.get(i));
                c.drawLine(x1, y1, x2, y2, p);
                c.drawCircle(x1, y1, Ui.dp(getContext(), 6), p);
                c.drawCircle(x2, y2, Ui.dp(getContext(), 6), p);
            }
        }
    }

    // ---- sources ----

    private static String[][] pick(String[][] pool, Random rnd, int n) {
        List<String[]> list = new ArrayList<>();
        Collections.addAll(list, pool);
        Collections.shuffle(list, rnd);
        List<String[]> out = new ArrayList<>();
        List<String> ls = new ArrayList<>(), rs = new ArrayList<>();
        for (String[] r : list) {
            if (ls.contains(r[0]) || rs.contains(r[1]) || r[0].isEmpty() || r[1].isEmpty()) continue;
            out.add(r);
            ls.add(r[0]);
            rs.add(r[1]);
            if (out.size() == n) break;
        }
        return out.toArray(new String[0][]);
    }

    static Source maths(final int grade) {
        return (rnd, n) -> {
            List<String[]> out = new ArrayList<>();
            List<String> answers = new ArrayList<>();
            while (out.size() < n) {
                int op = grade <= 1 ? rnd.nextInt(2) : grade == 2 ? rnd.nextInt(3) : rnd.nextInt(4);
                int x, y, ans;
                String e;
                int max = grade <= 0 ? 10 : grade == 1 ? 20 : grade == 2 ? 50 : 100;
                if (op == 0) {
                    x = 1 + rnd.nextInt(max / 2);
                    y = 1 + rnd.nextInt(max / 2);
                    ans = x + y;
                    e = x + " + " + y;
                } else if (op == 1) {
                    x = 2 + rnd.nextInt(max - 1);
                    y = 1 + rnd.nextInt(x - 1);
                    ans = x - y;
                    e = x + " − " + y;
                } else if (op == 2) {
                    x = 2 + rnd.nextInt(grade <= 2 ? 4 : 9);
                    y = 1 + rnd.nextInt(10);
                    ans = x * y;
                    e = x + " × " + y;
                } else {
                    y = 2 + rnd.nextInt(8);
                    ans = 1 + rnd.nextInt(10);
                    x = y * ans;
                    e = x + " ÷ " + y;
                }
                String s = String.valueOf(ans);
                if (answers.contains(s)) continue;
                answers.add(s);
                out.add(new String[]{e, s});
            }
            return out.toArray(new String[0][]);
        };
    }

    static Source fixed(final String[][] pool) {
        return (rnd, n) -> pick(pool, rnd, n);
    }

    /** From the word bank: e.g. ("kn", "en") pairs Kannada words with English, ("en", "pic") with pictures. */
    static Source bank(final Context c, final String from, final String to) {
        return (rnd, n) -> {
            List<String[]> pool = new ArrayList<>();
            for (WordsData.Word w : WordsData.bank(c)) {
                String r = to.equals("pic") ? w.pic : w.in(to);
                if (to.equals("pic") && w.pic.isEmpty()) continue;
                pool.add(new String[]{w.in(from), r});
            }
            return pick(pool.toArray(new String[0][]), rnd, n);
        };
    }

    static Source numbers(final String lang) {
        return (rnd, n) -> {
            String[][] pool = new String[20][];
            for (int i = 0; i < 20; i++) {
                String word = lang.equals("kn") ? Data.KN_NUM[i] : lang.equals("hi") ? Data.HI_NUM[i] : Data.NUM[i].toLowerCase();
                String num = lang.equals("en") ? String.valueOf(i + 1) : Data.digits(i + 1, lang) + " (" + (i + 1) + ")";
                pool[i] = new String[]{num, word};
            }
            return pick(pool, rnd, n);
        };
    }
}
