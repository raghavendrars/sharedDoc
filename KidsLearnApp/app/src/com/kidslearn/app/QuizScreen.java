package com.kidslearn.app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Quiz Time: ten pick-one-of-four questions. A star for every question answered right the first time. */
final class QuizScreen {
    static final int MATHS = 0, ENGLISH = 1, EVS = 2, GK = 3, ANIMALS = 4, PLANTS = 5, PHONICS = 6, KANNADA = 7, HINDI = 8,
            PATTERNS = 9, VOWELS = 10, PICWORD = 11, ODD = 12, SIGHT = 13, KN_GUNI = 14, HI_GUNI = 15, KN_VOCAB = 16, HI_VOCAB = 17,
            ADD = 18, SUB = 19, MUL = 20, DIV = 21, TENSE_EN = 22, TENSE_KN = 23, TENSE_HI = 24, TABLE = 25;
    /** How many of the modes above are listed under Quiz Time; the rest belong to the Activity book and Sight words. */
    static final int QUIZ_TIME = 9;
    static final String[] TITLES = {"Maths", "English", "EVS", "General Knowledge", "Animals", "Plants", "Phonics", "Kannada", "Hindi",
            "Patterns & counting", "Missing vowel", "Picture & word", "Odd one out", "Sight words", "Kannada gunitakshara", "Hindi barakhadi", "Kannada words", "Hindi words",
            "Addition", "Subtraction", "Multiplication", "Division", "English tenses", "Kannada tenses", "Hindi tenses", "Times table"};
    static final String[] ICONS = {"➕", "🔤", "🌍", "💡", "🐾", "🌱", "🗣️", "ಅ", "अ", "🧱", "🅰️", "🖼️", "🧐", "👀", "ಕಿ", "कि", "ಪದ", "शब्द", "➕", "➖", "✖️", "➗", "🕰️", "🕰️", "🕰️", "🔢"};
    static final int[] COLORS = {0xFF00ACC1, 0xFF6A1B9A, 0xFF2E7D32, 0xFF1565C0, 0xFF6D4C41, 0xFF388E3C, 0xFF3949AB, 0xFFD81B60, 0xFFEF6C00,
            0xFF00897B, 0xFFAD1457, 0xFF7B1FA2, 0xFF455A64, 0xFFF4511E, 0xFFD81B60, 0xFFEF6C00, 0xFFD81B60, 0xFFEF6C00,
            0xFF00ACC1, 0xFF0097A7, 0xFF00838F, 0xFF006064, 0xFF6A1B9A, 0xFFD81B60, 0xFFEF6C00, 0xFF0097A7};
    /** Open Trivia DB category used when a parent allows online questions (0 = offline only). */
    private static final int[] ONLINE = {0, 0, 17, 9, 27, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    static final int QUIZ_COLOR = 0xFF5C6BC0;
    private static final int ROUND = 10;
    static final String[] PRAISE = {"Well done!", "Great job!", "Super!", "Yes! That is right!", "Awesome!"};

    private static final class Opt {
        String text;
    }

    private static final class Q {
        String prompt, say, visual, why, lang = "en";
        Opt[] o;
        int ok;
    }

    private final MainActivity a;
    private final int mode, color;
    private final Random rnd;
    private int n, won, combo, param;
    private boolean firstTry, locked, online;
    private Q q;
    private List<String[]> order;
    private LinearLayout body;
    private TextView prog;
    private ProgressBar bar;

    private QuizScreen(MainActivity a, int mode) {
        this.a = a;
        this.mode = mode;
        this.color = COLORS[mode];
        this.rnd = a.rnd;
    }

    static Runnable open(final MainActivity a, final int mode, final int param) {
        return () -> {
            QuizScreen q = new QuizScreen(a, mode);
            q.param = param;
            a.show(q.make(), false, COLORS[mode], MainActivity.QUIZ);
        };
    }

    static Runnable open(final MainActivity a, final int mode) {
        return () -> a.show(new QuizScreen(a, mode).make(), false, COLORS[mode], MainActivity.QUIZ);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, mode == TABLE ? "Table of " + param : TITLES[mode] + " quiz"));

        prog = Ui.text(a, "", 15, Color.WHITE, true);
        page.addView(prog);
        bar = progressBar(a);
        bar.setMax(ROUND);
        page.addView(bar, barParams(a));

        body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 12);
        body.setPadding(p, 0, p, p);
        page.addView(body, Ui.lp(Ui.MATCH, 0, 1));

        final int cat = ONLINE[mode];
        if (cat > 0 && a.prefs.getBoolean("online", false)) {
            body.addView(Ui.text(a, "🌐\nGetting questions from the internet…", 22, Color.WHITE, true), Ui.lp(Ui.MATCH, Ui.MATCH, 0));
            new Thread(() -> {
                final List<String[]> got = fetch(cat);
                a.runOnUiThread(() -> {
                    if (!body.isAttachedToWindow()) return;
                    if (got != null && got.size() >= ROUND) {
                        online = true;
                        order = got;
                    }
                    ask();
                });
            }, "quiz-online").start();
        } else ask();
        return page;
    }

    static ProgressBar progressBar(MainActivity a) {
        ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        return bar;
    }

    static LinearLayout.LayoutParams barParams(MainActivity a) {
        LinearLayout.LayoutParams blp = Ui.lp(Ui.MATCH, Ui.dp(a, 8), 0);
        blp.setMargins(Ui.dp(a, 24), Ui.dp(a, 6), Ui.dp(a, 24), Ui.dp(a, 6));
        return blp;
    }

    /** Ten easy questions from opentdb.com, or null when offline. */
    private static List<String[]> fetch(int category) {
        HttpURLConnection c = null;
        try {
            URL u = new URL("https://opentdb.com/api.php?amount=10&category=" + category + "&difficulty=easy&type=multiple");
            c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            if (c.getResponseCode() != 200) return null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0 && out.size() < 512 * 1024) out.write(buf, 0, r);
            }
            JSONObject o = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
            if (o.optInt("response_code", 1) != 0) return null;
            JSONArray res = o.getJSONArray("results");
            List<String[]> list = new ArrayList<>();
            for (int i = 0; i < res.length(); i++) {
                JSONObject x = res.getJSONObject(i);
                JSONArray wrong = x.getJSONArray("incorrect_answers");
                if (wrong.length() < 3) continue;
                list.add(new String[]{plain(x.getString("question")), plain(x.getString("correct_answer")),
                        plain(wrong.getString(0)), plain(wrong.getString(1)), plain(wrong.getString(2))});
            }
            return list;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String plain(String html) {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    private void ask() {
        if (n >= ROUND) {
            result(a, mode == TABLE ? "Table of " + param : TITLES[mode], won, ROUND, color, open(a, mode, param));
            return;
        }
        switch (mode) {
            case MATHS:
                q = maths();
                break;
            case PHONICS:
                q = phonics();
                break;
            case KANNADA:
                q = indic("kn");
                break;
            case HINDI:
                q = indic("hi");
                break;
            case ENGLISH:
                q = bank(QBank.ENGLISH);
                break;
            case EVS:
                q = bank(QBank.EVS);
                break;
            case GK:
                q = bank(QBank.GK);
                break;
            case ANIMALS:
                q = bank(QBank.ANIMALS);
                break;
            case PATTERNS:
                q = patterns();
                break;
            case VOWELS:
                q = vowels();
                break;
            case PICWORD:
                q = picWord();
                break;
            case ODD:
                q = odd();
                break;
            case KN_GUNI:
            case HI_GUNI:
                q = guni(mode == KN_GUNI ? "kn" : "hi");
                break;
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                q = arith(mode - ADD);
                break;
            case TABLE:
                q = table();
                break;
            case TENSE_EN:
            case TENSE_KN:
            case TENSE_HI:
                q = tense(mode == TENSE_KN ? "kn" : mode == TENSE_HI ? "hi" : "en");
                break;
            case KN_VOCAB:
            case HI_VOCAB:
                q = vocabQ(mode == KN_VOCAB ? "kn" : "hi");
                break;
            case SIGHT:
                q = sight();
                break;
            default:
                q = bank(QBank.PLANTS);
                break;
        }
        firstTry = true;
        locked = false;
        prog.setText((online ? "🌐 " : "") + "Question " + (n + 1) + " of " + ROUND);
        bar.setProgress(n);
        body.removeAllViews();

        int len = q.prompt.length();
        final TextView prompt = Ui.text(a, q.prompt, len > 40 ? 20 : len > 24 ? 24 : len > 14 ? 28 : 40, Color.WHITE, true);
        prompt.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 4));
        Ui.click(prompt, this::sayQuestion);
        body.addView(prompt);
        if (q.visual != null) body.addView(Ui.text(a, q.visual, q.visual.length() > 12 ? 26 : 56, Color.WHITE, true));

        boolean wide = false;
        for (Opt o : q.o) if (o.text.length() > 4) wide = true;
        LinearLayout row = null;
        for (int i = 0; i < 4; i++) {
            if (wide || i % 2 == 0) {
                row = new LinearLayout(a);
                body.addView(row, Ui.lp(Ui.MATCH, 0, 1));
            }
            final Opt o = q.o[i];
            final FrameLayout t = new FrameLayout(a);
            t.setBackground(Ui.tile(a, Color.WHITE, 22));
            int pad = Ui.dp(a, 8);
            t.setPadding(pad, pad, pad, pad + Ui.dp(a, 6));
            float size = wide ? (o.text.length() > 18 ? 18 : 24) : 50;
            t.addView(Ui.text(a, o.text, size, Ui.INK, true), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            final int index = i;
            t.setOnClickListener(v -> pick(t, index));
            row.addView(t, Ui.lp(a, 0, Ui.MATCH, 1, wide ? 4 : 7));
            Ui.popIn(t, 50L * i);
        }
        sayQuestion();
    }

    private void sayQuestion() {
        if (q.say == null) return;
        if (!a.speaker.say(q.say, q.lang)) prog.setText("No " + TITLES[mode] + " voice on this phone");
    }

    private void pick(FrameLayout t, int i) {
        if (locked) return;
        if (i == q.ok) {
            locked = true;
            t.setBackground(Ui.tile(a, 0xFF66BB6A, 22));
            Ui.bounce(t);
            a.speaker.good();
            a.speaker.say(PRAISE[rnd.nextInt(PRAISE.length)] + (q.why != null ? " " + q.why : ""));
            if (firstTry) {
                won++;
                combo++;
                a.addStars(combo > 0 && combo % 5 == 0 ? 2 : 1);
                if (combo >= 2) prog.setText("🔥 " + combo + " in a row!" + (combo % 5 == 0 ? "  Bonus ⭐" : ""));
            } else combo = 0;
            a.confetti();
            n++;
            a.later(1400, this::ask);
        } else {
            firstTry = false;
            combo = 0;
            t.setBackground(Ui.tile(a, 0xFFEF9A9A, 22));
            t.setOnClickListener(null);
            t.setClickable(false);
            Ui.shake(t);
            a.speaker.bad();
            a.speaker.say("Try again");
        }
    }

    /** End-of-round screen; also saves the score for parents. */
    static void result(final MainActivity a, String title, int won, int total, final int color, final Runnable again) {
        a.stats.addScore(title, won, total);
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(color);
        int p = Ui.dp(a, 28);
        page.setPadding(p, p, p, p);
        TextView party = Ui.text(a, "🎉", 84, Color.WHITE, false);
        page.addView(party);
        page.addView(Ui.text(a, won * 10 >= total * 8 ? "Brilliant!" : won * 2 >= total ? "Well done!" : "Good try!", 38, Color.WHITE, true));
        page.addView(Ui.text(a, "You won " + won + " of " + total + " stars", 22, Color.WHITE, true));
        TextView row = Ui.text(a, won == 0 ? " " : Data.repeat("⭐", won, 5), 30, Color.WHITE, false);
        row.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 12));
        page.addView(row);
        TextView againBtn = Ui.button(a, "Play again", Color.WHITE, Ui.INK, () -> a.later(140, again));
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.topMargin = Ui.dp(a, 14);
        page.addView(againBtn, lp);
        TextView home = Ui.button(a, "Home", 0xFFFFD54F, Ui.INK, () -> a.later(140, a::home));
        LinearLayout.LayoutParams lp2 = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp2.topMargin = Ui.dp(a, 14);
        page.addView(home, lp2);
        a.show(page, false, color, MainActivity.QUIZ);
        Ui.popIn(party, 100);
        a.confetti();
        a.speaker.say("You won " + won + (won == 1 ? " star" : " stars"));
    }

    // ---- question makers ----

    private int[] four(int n) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) all.add(i);
        Collections.shuffle(all, rnd);
        return new int[]{all.get(0), all.get(1), all.get(2), all.get(3)};
    }

    /** Four different rows whose given column also differs. */
    private int[] fourDistinct(String[][] src, int col) {
        while (true) {
            int[] ix = four(src.length);
            boolean clash = false;
            for (int i = 0; i < 4; i++)
                for (int j = i + 1; j < 4; j++)
                    if (src[ix[i]][col].equalsIgnoreCase(src[ix[j]][col])) clash = true;
            if (!clash) return ix;
        }
    }

    private static Opt opt(String s) {
        Opt o = new Opt();
        o.text = s;
        return o;
    }

    private void fill(Q q, List<String> vals, String right) {
        Collections.shuffle(vals, rnd);
        q.o = new Opt[4];
        for (int i = 0; i < 4; i++) q.o[i] = opt(vals.get(i));
        q.ok = vals.indexOf(right);
    }

    private Q bank(String[][] src) {
        if (order == null) {
            order = new ArrayList<>(Arrays.asList(src));
            Collections.shuffle(order, rnd);
        }
        String[] r = order.get(n % order.size());
        Q q = new Q();
        q.prompt = r[0];
        q.say = r[0];
        fill(q, new ArrayList<>(Arrays.asList(r[1], r[2], r[3], r[4])), r[1]);
        return q;
    }

    // ---- maths sized to the child's class ----

    /** op: 0 add, 1 subtract, 2 multiply, 3 divide. */
    private Q arith(int op) {
        Q q = new Q();
        int g = a.grade(), x, y, ans;
        int max = g <= 0 ? 10 : g == 1 ? 20 : g == 2 ? 100 : g == 3 ? 1000 : 10000;
        if (op == 0) {
            x = 1 + rnd.nextInt(max - 1);
            y = 1 + rnd.nextInt(Math.max(1, max - x));
            ans = x + y;
            q.prompt = x + " + " + y + " = ?";
            q.say = x + " plus " + y;
            if (g <= 0 && ans <= 10) q.visual = Data.repeat("🍎", x, 0) + "  +  " + Data.repeat("🍎", y, 0) + "        ";
        } else if (op == 1) {
            x = 2 + rnd.nextInt(max - 1);
            y = 1 + rnd.nextInt(x - 1);
            ans = x - y;
            q.prompt = x + " − " + y + " = ?";
            q.say = x + " minus " + y;
        } else if (op == 2) {
            if (g <= 2) {
                x = 2 + rnd.nextInt(4);
                y = 1 + rnd.nextInt(10);
            } else if (g == 3) {
                x = 2 + rnd.nextInt(9);
                y = 1 + rnd.nextInt(10);
            } else if (g == 4) {
                x = 11 + rnd.nextInt(89);
                y = 2 + rnd.nextInt(8);
            } else {
                x = 11 + rnd.nextInt(89);
                y = 11 + rnd.nextInt(20);
            }
            ans = x * y;
            q.prompt = x + " × " + y + " = ?";
            q.say = x + " times " + y;
        } else {
            if (g <= 3) {
                y = 2 + rnd.nextInt(9);
                ans = 1 + rnd.nextInt(10);
            } else if (g == 4) {
                y = 2 + rnd.nextInt(8);
                ans = 10 + rnd.nextInt(90);
            } else {
                y = 2 + rnd.nextInt(11);
                ans = 10 + rnd.nextInt(190);
            }
            x = y * ans;
            q.prompt = x + " ÷ " + y + " = ?";
            q.say = x + " divided by " + y;
        }
        near(q, ans);
        return q;
    }

    private Q table() {
        Q q = new Q();
        int k = 1 + rnd.nextInt(10);
        q.prompt = param + " × " + k + " = ?";
        q.say = param + " times " + k;
        near(q, param * k);
        return q;
    }

    /** The right answer and three different wrong ones close to it. */
    private void near(Q q, int ans) {
        List<String> vals = new ArrayList<>();
        vals.add(String.valueOf(ans));
        int spread = Math.max(5, Math.abs(ans) / 10);
        int tries = 0;
        while (vals.size() < 4 && tries++ < 200) {
            int d = 1 + rnd.nextInt(spread);
            int v = rnd.nextBoolean() ? ans + d : ans - d;
            if (v < 0) continue;
            if (!vals.contains(String.valueOf(v))) vals.add(String.valueOf(v));
        }
        while (vals.size() < 4) vals.add(String.valueOf(ans + 10 * vals.size()));
        fill(q, vals, String.valueOf(ans));
    }

    /** Pick the sentence in the right tense: three forms of one verb plus one other verb. */
    private Q tense(String lang) {
        Q q = new Q();
        q.lang = lang;
        String[][] all = WordsData.TENSES;
        int v = rnd.nextInt(all.length), other;
        do other = rnd.nextInt(all.length);
        while (other == v);
        int t = rnd.nextInt(3);
        List<String> vals = new ArrayList<>();
        for (int i = 0; i < 3; i++) vals.add(WordsData.tense(all[v], lang, i));
        vals.add(WordsData.tense(all[other], lang, t));
        String right = WordsData.tense(all[v], lang, t);
        q.prompt = WordsData.when(lang, t) + " — " + all[v][0] + " " + WordsData.verbName(all[v], lang) + (lang.equals("en") ? "" : " (" + all[v][1] + ")");
        q.say = lang.equals("en") ? WordsData.when(lang, t) + ". Which sentence is right for " + all[v][1] + "?" : WordsData.when(lang, t);
        fill(q, vals, right);
        q.why = WordsData.TENSE_NAMES[t] + " tense.";
        return q;
    }

    private Q maths() {
        int g = a.grade();
        int pickOp = rnd.nextInt(g <= 1 ? 2 : g == 2 ? 3 : 4);
        if (g <= 1 || rnd.nextInt(3) > 0) return arith(pickOp);
        Q q = new Q();
        int type = 4 + rnd.nextInt(3), ans;
        if (type == 0) {
            int x = 1 + rnd.nextInt(9), y = 1 + rnd.nextInt(10);
            ans = x + y;
            q.prompt = x + " + " + y + " = ?";
            q.say = x + " plus " + y;
        } else if (type == 1) {
            int x = 5 + rnd.nextInt(45), y = 1 + rnd.nextInt(x - 1);
            ans = x - y;
            q.prompt = x + " − " + y + " = ?";
            q.say = x + " minus " + y;
        } else if (type == 2) {
            int x = 2 + rnd.nextInt(9), y = 2 + rnd.nextInt(9);
            ans = x * y;
            q.prompt = x + " × " + y + " = ?";
            q.say = x + " times " + y;
        } else if (type == 3) {
            int y = 2 + rnd.nextInt(8), z = 2 + rnd.nextInt(9);
            ans = z;
            q.prompt = (y * z) + " ÷ " + y + " = ?";
            q.say = (y * z) + " divided by " + y;
        } else if (type == 4) {
            int x = 2 + rnd.nextInt(20), y = 2 + rnd.nextInt(20);
            ans = y;
            q.prompt = x + " + ? = " + (x + y);
            q.say = x + " plus what makes " + (x + y) + "?";
        } else if (type == 5) {
            int h = 1 + rnd.nextInt(9), t = rnd.nextInt(10), o = rnd.nextInt(10);
            int num = h * 100 + t * 10 + o;
            q.prompt = "How many tens are in " + num + "?";
            q.say = q.prompt;
            ans = t;
            List<String> vals = new ArrayList<>(Arrays.asList(String.valueOf(t)));
            for (String s : new String[]{String.valueOf(h), String.valueOf(o), String.valueOf((t + 1) % 10), String.valueOf((t + 3) % 10)})
                if (!vals.contains(s) && vals.size() < 4) vals.add(s);
            while (vals.size() < 4) {
                String s = String.valueOf(rnd.nextInt(10));
                if (!vals.contains(s)) vals.add(s);
            }
            fill(q, vals, String.valueOf(t));
            return q;
        } else {
            List<String> vals = new ArrayList<>();
            while (vals.size() < 4) {
                String s = String.valueOf(10 + rnd.nextInt(990));
                if (!vals.contains(s)) vals.add(s);
            }
            boolean big = rnd.nextBoolean();
            String right = vals.get(0);
            for (String s : vals) if (big ? Integer.parseInt(s) > Integer.parseInt(right) : Integer.parseInt(s) < Integer.parseInt(right)) right = s;
            q.prompt = big ? "Which number is the biggest?" : "Which number is the smallest?";
            q.say = q.prompt;
            fill(q, vals, right);
            return q;
        }
        List<String> vals = new ArrayList<>();
        vals.add(String.valueOf(ans));
        while (vals.size() < 4) {
            String s = String.valueOf(Math.max(0, ans + rnd.nextInt(11) - 5));
            if (!vals.contains(s)) vals.add(s);
        }
        fill(q, vals, String.valueOf(ans));
        return q;
    }

    /** The voice says a sound and a word; the child picks the letters that make the sound. */
    private Q phonics() {
        Q q = new Q();
        boolean pairs = rnd.nextBoolean();
        String[][] src = pairs ? Data.BLEND : Data.LETTER_SOUND;
        int[] ix = fourDistinct(src, 1);
        int ok = rnd.nextInt(4);
        List<String> vals = new ArrayList<>();
        for (int i : ix) vals.add(src[i][0]);
        String[] r = src[ix[ok]];
        q.prompt = "🔊 Which sound do you hear?";
        q.visual = r[3];
        q.say = r[1] + ". " + r[1] + ". As in " + r[2] + ".";
        fill(q, vals, r[0]);
        return q;
    }

    private Q indic(String lang) {
        Q q = new Q();
        q.lang = lang;
        String[][] letters = lang.equals("kn") ? Data.join(Data.KN_V, Data.KN_C) : Data.join(Data.HI_V, Data.HI_C);
        String[][] words = Data.words(lang).toArray(new String[0][]);
        String[] nums = lang.equals("kn") ? Data.KN_NUM : Data.HI_NUM;
        int type = rnd.nextInt(4);
        List<String> vals = new ArrayList<>();
        String right;
        if (type == 0) {
            int[] ix = fourDistinct(letters, 1);
            String[] r = letters[ix[0]];
            for (int i : ix) vals.add(letters[i][0]);
            right = r[0];
            q.prompt = "🔊 Find the letter “" + r[1] + "”";
            q.say = r[0];
        } else if (type == 1) {
            int[] ix = fourDistinct(words, 4);
            String[] r = words[ix[0]];
            for (int i : ix) vals.add(words[i][4]);
            right = r[4];
            q.prompt = "What does " + r[2] + " mean?";
            q.say = r[2];
        } else if (type == 2) {
            List<String[]> pics = new ArrayList<>();
            for (String[] w : words) if (!w[5].isEmpty()) pics.add(w);
            String[][] arr = pics.toArray(new String[0][]);
            int[] ix = fourDistinct(arr, 2);
            String[] r = arr[ix[0]];
            for (int i : ix) vals.add(arr[i][2]);
            right = r[2];
            q.prompt = "Which word is this?";
            q.visual = r[5];
            q.say = null;
        } else {
            int[] ix = four(10);
            for (int i : ix) vals.add(String.valueOf(i + 1));
            right = String.valueOf(ix[0] + 1);
            q.prompt = "🔊 Which number is " + nums[ix[0]] + "?";
            q.say = nums[ix[0]];
        }
        fill(q, vals, right);
        return q;
    }

    // ---- activity book ----

    private static final String[] BLOCKS = {"🟥", "🟦", "🟨", "🟩", "🟪", "🟧"};
    private static final String[] SHAPE_SET = {"🔺", "⭐", "❤️", "⚫", "🔷", "🌙"};
    private static final String[][] CVC = {{"cat", "🐱"}, {"dog", "🐶"}, {"sun", "☀️"}, {"pig", "🐷"}, {"hat", "🎩"}, {"bus", "🚌"},
            {"cup", "☕"}, {"bed", "🛏️"}, {"hen", "🐔"}, {"fox", "🦊"}, {"box", "📦"}, {"map", "🗺️"}, {"pen", "🖊️"}, {"bat", "🦇"},
            {"net", "🥅"}, {"van", "🚐"}, {"web", "🕸️"}, {"bag", "👜"}, {"lip", "👄"}, {"bug", "🐛"}};
    private static final String[][] MORE_WORDS = {{"frog", "🐸"}, {"fish", "🐟"}, {"ship", "🚢"}, {"duck", "🦆"}, {"milk", "🥛"},
            {"drum", "🥁"}, {"crab", "🦀"}, {"star", "⭐"}, {"tree", "🌳"}, {"moon", "🌙"}, {"hand", "✋"}, {"sock", "🧦"},
            {"bell", "🔔"}, {"tent", "⛺"}, {"gift", "🎁"}, {"boat", "⛵"}, {"rain", "🌧️"}, {"cake", "🎂"}};
    /** Group name, then members. */
    private static final String[][] GROUPS = {
            {"fruits", "🍎", "🍌", "🍇", "🍊", "🍓"}, {"vehicles", "🚗", "🚌", "🚲", "🚂", "✈️"},
            {"clothes", "👕", "👖", "🧦", "👗", "🧢"}, {"weather", "☀️", "🌧️", "❄️", "🌈", "⛈️"},
            {"body parts", "👃", "👂", "👀", "✋", "🦶"}, {"musical instruments", "🎸", "🥁", "🎺", "🎻", "🎹"},
            {"sea animals", "🐟", "🐙", "🐬", "🦀", "🐳"}, {"vegetables", "🥕", "🥦", "🌽", "🥔", "🍆"}};

    private Q patterns() {
        Q q = new Q();
        List<String> pool = new ArrayList<>(Arrays.asList(rnd.nextBoolean() ? BLOCKS : SHAPE_SET));
        Collections.shuffle(pool, rnd);
        if (rnd.nextInt(3) == 0) {
            String one = pool.get(0), other = pool.get(1);
            int count = 2 + rnd.nextInt(7), extra = 1 + rnd.nextInt(4);
            List<String> items = new ArrayList<>();
            for (int i = 0; i < count; i++) items.add(one);
            for (int i = 0; i < extra; i++) items.add(other);
            Collections.shuffle(items, rnd);
            StringBuilder b = new StringBuilder();
            for (String s : items) b.append(s);
            q.prompt = "How many " + one + " ?";
            q.visual = b.toString();
            q.say = "Count them carefully!";
            List<String> vals = new ArrayList<>();
            vals.add(String.valueOf(count));
            while (vals.size() < 4) {
                String s = String.valueOf(Math.max(1, count + rnd.nextInt(7) - 3));
                if (!vals.contains(s)) vals.add(s);
            }
            fill(q, vals, String.valueOf(count));
            return q;
        }
        String[] units = {"AB", "AAB", "ABB", "ABC", "AABB"};
        String u = units[rnd.nextInt(units.length)];
        int n = Math.max(6, u.length() * 2);
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < n; i++) seq.append(pool.get(u.charAt(i % u.length()) - 'A'));
        String right = pool.get(u.charAt(n % u.length()) - 'A');
        q.prompt = "What comes next?";
        q.visual = seq + "❓";
        q.say = q.prompt;
        fill(q, new ArrayList<>(pool.subList(0, 4)), right);
        return q;
    }

    private Q vowels() {
        Q q = new Q();
        String[] r = CVC[rnd.nextInt(CVC.length)];
        String w = r[0], vowel = w.substring(1, 2);
        q.prompt = w.charAt(0) + " _ " + w.charAt(2);
        q.visual = r[1];
        q.say = "Which vowel is missing? " + w;
        List<String> vals = new ArrayList<>(Arrays.asList("a", "e", "i", "o", "u"));
        vals.remove(vowel);
        Collections.shuffle(vals, rnd);
        vals = new ArrayList<>(vals.subList(0, 3));
        vals.add(vowel);
        fill(q, vals, vowel);
        q.why = "It says " + w + ".";
        return q;
    }

    private Q picWord() {
        Q q = new Q();
        String[][] all = Data.join(CVC, MORE_WORDS);
        int[] ix = fourDistinct(all, 0);
        List<String> vals = new ArrayList<>();
        for (int i : ix) vals.add(all[i][0]);
        q.prompt = "Read the words. Which one is this?";
        q.visual = all[ix[0]][1];
        q.say = "Which word matches the picture?";
        fill(q, vals, all[ix[0]][0]);
        q.why = "It is a " + all[ix[0]][0] + ".";
        return q;
    }

    private Q odd() {
        Q q = new Q();
        int g = rnd.nextInt(GROUPS.length), o;
        do o = rnd.nextInt(GROUPS.length);
        while (o == g);
        List<String> members = new ArrayList<>(Arrays.asList(GROUPS[g]).subList(1, GROUPS[g].length));
        Collections.shuffle(members, rnd);
        String odd = GROUPS[o][1 + rnd.nextInt(GROUPS[o].length - 1)];
        List<String> vals = new ArrayList<>(members.subList(0, 3));
        vals.add(odd);
        q.prompt = "Which one does not belong?";
        q.say = q.prompt;
        fill(q, vals, odd);
        q.why = "The others are all " + GROUPS[g][0] + ".";
        return q;
    }

    private Q sight() {
        Q q = new Q();
        String[][] w = Data.SIGHT_WORDS;
        int[] ix = fourDistinct(w, 0);
        List<String> vals = new ArrayList<>();
        for (int i : ix) vals.add(w[i][0]);
        q.prompt = "🔊 Find the missing word";
        q.say = w[ix[0]][0] + ". " + w[ix[0]][1] + " " + w[ix[0]][0] + ".";
        // the sentence with the word blanked out, so it also works by reading alone
        q.visual = w[ix[0]][1].replaceFirst("(?i)\\b" + w[ix[0]][0] + "\\b", "___");
        fill(q, vals, w[ix[0]][0]);
        return q;
    }

    // ---- Kannada & Hindi ----

    /** Gunitakshara / barakhadi: find the syllable that is spoken (and written in English letters). */
    private Q guni(String lang) {
        Q q = new Q();
        q.lang = lang;
        String[][] cons = IndicData.consonants(lang);
        String[][] row = IndicData.row(lang, rnd.nextInt(Math.min(cons.length, 33)));
        int[] ix = fourDistinct(row, 1);
        List<String> vals = new ArrayList<>();
        for (int i : ix) vals.add(row[i][0]);
        q.prompt = "🔊 Find “" + row[ix[0]][1] + "”";
        q.say = row[ix[0]][0];
        fill(q, vals, row[ix[0]][0]);
        return q;
    }

    /** Vocabulary: picture → word, or meaning → word. */
    private Q vocabQ(String lang) {
        Q q = new Q();
        List<String[]> all = new ArrayList<>();
        for (String[][] theme : IndicData.vocab(lang)) all.addAll(Arrays.asList(theme));
        String[][] arr = all.toArray(new String[0][]);
        int[] ix = fourDistinct(arr, 0);
        List<String> vals = new ArrayList<>();
        for (int i : ix) vals.add(arr[i][0]);
        String[] r = arr[ix[0]];
        if (rnd.nextBoolean()) {
            q.prompt = "Which word is this?";
            q.visual = r[3];
            q.say = null;
        } else {
            q.prompt = "Which word means “" + r[2] + "”?";
            q.say = "Which word means " + r[2] + "?";
        }
        fill(q, vals, r[0]);
        q.why = r[0].isEmpty() ? null : "It is " + r[1] + ".";
        return q;
    }
}
