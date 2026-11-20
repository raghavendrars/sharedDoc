package com.kidslearn.app;

import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Jumbled sentences, the silly sentence maker, and writing your own sentence. */
final class SentenceScreen {
    static final int COLOR = 0xFF6A1B9A;
    private static final String[] SENTENCES = {
            "I like to read books.", "The cat is on the mat.", "My mother cooks tasty food.", "We play in the park.",
            "The sun is very hot.", "Birds fly in the sky.", "I brush my teeth every day.", "The dog has a red ball.",
            "She is my best friend.", "We go to school by bus.", "Fish live in the water.", "My father reads the newspaper.",
            "The baby is sleeping now.", "I drink a glass of milk.", "Please close the door.", "The farmer grows rice.",
            "Plants need water and sunlight.", "Can you help me?", "It is raining today.", "The elephant has a long trunk."};

    private static final String[][][] SILLY = {
            {{"🐘", "The pink elephant"}, {"👩‍🏫", "My teacher"}, {"🐜", "A tiny ant"}, {"👴", "Grandpa"}, {"🤖", "A silly robot"},
                    {"👑", "The king"}, {"🐒", "A cheeky monkey"}, {"👧", "My little sister"}},
            {{"💃", "danced with"}, {"🎤", "sang to"}, {"😋", "ate"}, {"🦘", "jumped over"}, {"🤭", "tickled"}, {"🎨", "painted"},
                    {"🤗", "hugged"}, {"🚀", "flew away with"}},
            {{"🍌", "a giant banana"}, {"🌙", "the moon"}, {"🍜", "ten noodles"}, {"🐄", "a purple cow"}, {"📒", "my homework"},
                    {"🦁", "a sleepy lion"}, {"🎂", "a chocolate cake"}, {"🧦", "a smelly sock"}},
            {{"🚌", "on the bus"}, {"🛁", "in the bathtub"}, {"🏫", "at school"}, {"🛏️", "under the bed"}, {"🌌", "in outer space"},
                    {"🍳", "in the kitchen"}, {"🌳", "up a tree"}, {"🏖️", "at the beach"}}};

    private static final String[] HELP_WORDS = {"I", "We", "The", "My", "like", "love", "can", "see", "is", "am", "a", "big",
            "small", "happy", "cat", "dog", "school", "play", "to", "and", "."};

    private SentenceScreen() {
    }

    private static LinearLayout page(MainActivity a, String title) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, title));
        return page;
    }

    private static TextView tool(MainActivity a, String label, Runnable r) {
        TextView t = Ui.text(a, label, 17, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    // ---- jumbled sentences ----

    static View jumbled(MainActivity a) {
        return new Jumble(a).make();
    }

    private static final class Jumble {
        private static final int ROUND = 8;
        private final MainActivity a;
        private final List<String> order = new ArrayList<>(Arrays.asList(SENTENCES));
        private int n, won;
        private boolean firstTry, locked;
        private String sentence;
        private FlowLayout pool, answer;
        private TextView prog, msg;
        private ProgressBar bar;

        Jumble(MainActivity a) {
            this.a = a;
            Collections.shuffle(order, a.rnd);
        }

        View make() {
            LinearLayout page = page(a, "Jumbled sentences");
            prog = Ui.text(a, "", 15, Color.WHITE, true);
            page.addView(prog);
            bar = QuizScreen.progressBar(a);
            bar.setMax(ROUND);
            page.addView(bar, QuizScreen.barParams(a));
            page.addView(Ui.text(a, "Tap the words in the right order", 20, Color.WHITE, true));

            answer = new FlowLayout(a);
            int p = Ui.dp(a, 12);
            answer.setPadding(p, p, p, p);
            answer.setMinimumHeight(Ui.dp(a, 130));
            answer.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 18)));
            LinearLayout.LayoutParams alp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            alp.setMargins(Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 14), Ui.dp(a, 8));
            page.addView(answer, alp);

            msg = Ui.text(a, "", 18, 0xFFFFF59D, true);
            page.addView(msg);

            pool = new FlowLayout(a);
            pool.setPadding(p, p, p, p);
            ScrollView sv = new ScrollView(a);
            sv.addView(pool);
            page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));

            LinearLayout row = new LinearLayout(a);
            row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 16));
            row.addView(tool(a, "🔊 Hear it", () -> a.speaker.say(sentence)), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
            row.addView(tool(a, "↺ Start again", this::reset), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
            page.addView(row);
            next();
            return page;
        }

        private void next() {
            if (n >= ROUND) {
                QuizScreen.result(a, "Jumbled sentences", won, ROUND, COLOR, () -> a.show(jumbled(a), false, COLOR, MainActivity.QUIZ));
                return;
            }
            sentence = order.get(n);
            firstTry = true;
            locked = false;
            prog.setText("Sentence " + (n + 1) + " of " + ROUND);
            bar.setProgress(n);
            msg.setText("");
            reset();
        }

        private void reset() {
            if (locked) return;
            answer.removeAllViews();
            pool.removeAllViews();
            List<String> words = new ArrayList<>(Arrays.asList(sentence.split(" ")));
            do Collections.shuffle(words, a.rnd);
            while (words.size() > 1 && String.join(" ", words).equals(sentence));
            for (int i = 0; i < words.size(); i++) {
                TextView chip = Ui.text(a, words.get(i), 22, Color.WHITE, true);
                chip.setBackground(Ui.tile(a, Data.BRIGHT[i % Data.BRIGHT.length], 14));
                chip.setPadding(Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 14), Ui.dp(a, 12));
                chip.setOnClickListener(this::move);
                pool.addView(chip);
                Ui.popIn(chip, 40L * i);
            }
        }

        private void move(View chip) {
            if (locked) return;
            if (chip.getParent() == pool) {
                pool.removeView(chip);
                answer.addView(chip);
            } else {
                answer.removeView(chip);
                pool.addView(chip);
            }
            Ui.bounce(chip);
            if (pool.getChildCount() == 0) check();
        }

        private void check() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < answer.getChildCount(); i++) {
                if (i > 0) b.append(' ');
                b.append(((TextView) answer.getChildAt(i)).getText());
            }
            if (b.toString().equals(sentence)) {
                locked = true;
                msg.setText("✅ " + QuizScreen.PRAISE[a.rnd.nextInt(QuizScreen.PRAISE.length)]);
                a.speaker.good();
                a.speaker.say(sentence);
                if (firstTry) {
                    won++;
                    a.addStars(1);
                }
                a.confetti();
                n++;
                a.later(2200, this::next);
            } else {
                firstTry = false;
                msg.setText("Not quite — try again!");
                a.speaker.bad();
                Ui.shake(answer);
                a.later(900, this::reset);
            }
        }
    }

    // ---- silly sentence maker ----

    static View silly(final MainActivity a) {
        LinearLayout page = page(a, "Silly sentences");
        final TextView line = Ui.text(a, "", 22, Color.WHITE, true);
        int p = Ui.dp(a, 14);
        line.setPadding(p, Ui.dp(a, 4), p, Ui.dp(a, 8));
        page.addView(line);
        final int[] pick = new int[4];
        final TextView[] cards = new TextView[4];
        final String[] heads = {"Who?", "Did what?", "To what?", "Where?"};
        final Runnable render = () -> {
            StringBuilder b = new StringBuilder();
            for (int k = 0; k < 4; k++) {
                String[] part = SILLY[k][pick[k]];
                cards[k].setText(heads[k] + "   " + part[0] + "  " + part[1]);
                if (k > 0) b.append(' ');
                b.append(part[1]);
            }
            line.setText(b + ".");
        };
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(p, 0, p, 0);
        for (int k = 0; k < 4; k++) {
            final int kk = k;
            pick[k] = a.rnd.nextInt(SILLY[k].length);
            TextView c = Ui.text(a, "", 20, Ui.INK, true);
            c.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            c.setBackground(Ui.tile(a, Color.WHITE, 18));
            c.setPadding(p, 0, p, Ui.dp(a, 6));
            Ui.click(c, () -> {
                pick[kk] = (pick[kk] + 1) % SILLY[kk].length;
                render.run();
                a.speaker.say(SILLY[kk][pick[kk]][1]);
            });
            cards[k] = c;
            list.addView(c, Ui.lp(a, Ui.MATCH, 0, 1, 5));
        }
        page.addView(list, Ui.lp(Ui.MATCH, 0, 1));
        page.addView(Ui.text(a, "Tap a card to change it", 15, Color.WHITE, false));
        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 16));
        row.addView(tool(a, "🎰 Spin!", () -> {
            for (int k = 0; k < 4; k++) {
                final int kk = k;
                for (int step = 0; step < 8; step++) {
                    a.later(step * 90L + k * 140L, () -> {
                        pick[kk] = a.rnd.nextInt(SILLY[kk].length);
                        render.run();
                        Ui.bounce(cards[kk]);
                    });
                }
            }
            a.later(8 * 90L + 3 * 140L + 250, () -> {
                Ui.bounce(line);
                a.speaker.say(line.getText().toString());
            });
        }), Ui.lp(a, 0, Ui.dp(a, 54), 1, 4));
        row.addView(tool(a, "🔊 Read it", () -> a.speaker.say(line.getText().toString())), Ui.lp(a, 0, Ui.dp(a, 54), 1, 4));
        page.addView(row);
        render.run();
        return page;
    }

    // ---- write your own ----

    static View own(final MainActivity a) {
        LinearLayout page = page(a, "Write a sentence");
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 14);
        box.setPadding(p, 0, p, p);

        box.addView(Ui.text(a, "Write your own sentence:", 19, Color.WHITE, true));
        final EditText et = new EditText(a);
        et.setTextSize(24);
        et.setHint("I like to play cricket.");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 16)));
        et.setPadding(p, p, p, p);
        et.setMinLines(2);
        LinearLayout.LayoutParams elp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        elp.topMargin = Ui.dp(a, 8);
        box.addView(et, elp);

        FlowLayout helpers = new FlowLayout(a);
        helpers.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 6));
        for (final String w : HELP_WORDS) {
            TextView chip = Ui.text(a, w, 17, Ui.INK, true);
            chip.setBackground(Ui.round(0xFFE1BEE7, Ui.dp(a, 12)));
            chip.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
            Ui.click(chip, () -> {
                String cur = et.getText().toString();
                String add = w.equals(".") || cur.isEmpty() || cur.endsWith(" ") ? w : " " + w;
                et.append(add);
            });
            helpers.addView(chip);
        }
        box.addView(helpers);

        final TextView checks = Ui.text(a, "", 18, Color.WHITE, false);
        checks.setGravity(Gravity.START);
        final TextView saved = Ui.text(a, "", 17, 0xFFF3E5F5, false);
        saved.setGravity(Gravity.START);

        LinearLayout row = new LinearLayout(a);
        row.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        row.addView(tool(a, "🔊 Read it", () -> a.speaker.say(et.getText().toString())), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
        row.addView(tool(a, "✓ Check", () -> {
            String s = et.getText().toString().trim();
            boolean cap = !s.isEmpty() && Character.isUpperCase(s.charAt(0));
            boolean end = s.endsWith(".") || s.endsWith("!") || s.endsWith("?");
            boolean three = s.split("\\s+").length >= 3 && !s.isEmpty();
            boolean spaces = !s.contains("  ");
            checks.setText((cap ? "✅" : "❌") + " Starts with a capital letter\n"
                    + (end ? "✅" : "❌") + " Ends with . ! or ?\n"
                    + (three ? "✅" : "❌") + " Has 3 or more words\n"
                    + (spaces ? "✅" : "❌") + " One space between words");
            if (cap && end && three && spaces) {
                a.speaker.good();
                a.speaker.say("Super sentence! " + s);
                if (save(a, s)) a.addStars(1);
                a.confetti();
                saved.setText(list(a));
            } else {
                a.speaker.say("Almost! Look at the list.");
            }
        }), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
        box.addView(row);
        box.addView(checks);
        TextView head = Ui.text(a, "📒 My sentences", 19, Color.WHITE, true);
        head.setGravity(Gravity.START);
        head.setPadding(0, Ui.dp(a, 14), 0, Ui.dp(a, 4));
        box.addView(head);
        saved.setText(list(a));
        box.addView(saved);

        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        return page;
    }

    private static JSONArray mine(MainActivity a) {
        try {
            return new JSONArray(a.prefs.getString("mysentences", "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /** Saves the sentence; false if it was already saved. */
    private static boolean save(MainActivity a, String s) {
        JSONArray old = mine(a), out = new JSONArray();
        for (int i = 0; i < old.length(); i++) if (s.equals(old.optString(i))) return false;
        out.put(s);
        for (int i = 0; i < old.length() && i < 49; i++) out.put(old.optString(i));
        a.prefs.edit().putString("mysentences", out.toString()).apply();
        return true;
    }

    private static String list(MainActivity a) {
        JSONArray arr = mine(a);
        if (arr.length() == 0) return "Your good sentences will be saved here.";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) b.append("• ").append(arr.optString(i)).append('\n');
        return b.toString().trim();
    }
}
