package com.kidslearn.app;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Phonics Steps: a synthetic-phonics course. Sounds are taught in small sets from simple to complex; children blend
 * words made only of sounds they know, spell by choosing the sound for each part of a word, learn the tricky words
 * for each step and read a sentence that uses only taught sounds.
 */
final class PhonicsCourse {
    static final int COLOR = 0xFF1565C0;

    static final String[] STEPS = {"s a t p i n", "m d g o c k", "ck e u r", "h b f ff l ll ss", "j v w x",
            "y z zz qu", "ch sh th ng", "ai ee igh oa oo", "ar or ur ow oi", "ear air er"};

    /** sound spelling | how it sounds | example word | picture */
    private static final String[] SOUNDS = {
            "s|sss|sun|☀️", "a|ah|ant|🐜", "t|tuh|tap|🚰", "p|puh|pan|🍳", "i|ih|ink|🖋️", "n|nnn|net|🥅",
            "m|mmm|map|🗺️", "d|duh|dog|🐶", "g|guh|goat|🐐", "o|aw|octopus|🐙", "c|kuh|cat|🐱", "k|kuh|kid|🧒",
            "ck|kuh|duck|🦆", "e|eh|egg|🥚", "u|uh|up|⬆️", "r|rrr|rat|🐀",
            "h|huh|hat|🎩", "b|buh|bat|🦇", "f|fff|fish|🐟", "ff|fff|puff|💨", "l|lll|leg|🦵", "ll|lll|bell|🔔", "ss|sss|kiss|💋",
            "j|juh|jet|✈️", "v|vvv|van|🚐", "w|wuh|web|🕸️", "x|ks|fox|🦊",
            "y|yuh|yes|✅", "z|zzz|zip|🤐", "zz|zzz|buzz|🐝", "qu|kwuh|queen|👸",
            "ch|chuh|chick|🐥", "sh|shh|ship|🚢", "th|thuh|thumb|👍", "ng|nng|ring|💍",
            "ai|ay|rain|🌧️", "ee|eee|bee|🐝", "igh|eye|night|🌃", "oa|oh|boat|⛵", "oo|ooh|moon|🌙",
            "ar|ar|car|🚗", "or|or|fork|🍴", "ur|er|surf|🏄", "ow|ow|cow|🐄", "oi|oy|coin|🪙",
            "ear|ear|ear|👂", "air|air|hair|💇", "er|er|her|👩"};

    /** Words split into sounds with "-", and a picture when there is a clear one. */
    private static final String[] WORDS = {
            "s-a-t|", "p-a-t|", "t-a-p|🚰", "p-i-n|📍", "t-i-n|🥫", "s-i-t|🪑", "p-a-n|🍳", "a-n-t|🐜", "n-a-p|😴", "t-i-p|", "s-n-i-p|✂️",
            "m-a-p|🗺️", "d-o-g|🐶", "d-i-g|⛏️", "p-o-t|🍯", "c-a-t|🐱", "c-a-n|🥫", "k-i-d|🧒", "m-o-p|", "d-a-d|👨", "t-o-p|", "p-i-g|🐷", "g-o-t|",
            "d-u-ck|🦆", "s-o-ck|🧦", "r-e-d|🔴", "s-u-n|☀️", "c-u-p|☕", "r-a-t|🐀", "p-e-n|🖊️", "n-e-t|🥅", "t-e-n|🔟", "r-u-n|🏃", "r-o-ck|🪨", "k-i-ck|",
            "h-a-t|🎩", "b-a-t|🦇", "b-e-d|🛏️", "b-u-s|🚌", "h-e-n|🐔", "l-e-g|🦵", "b-e-ll|🔔", "h-i-ll|⛰️", "k-i-ss|💋", "p-u-ff|💨", "l-o-g|🪵", "b-a-g|👜", "f-u-n|🎉",
            "j-e-t|✈️", "v-a-n|🚐", "w-e-b|🕸️", "w-i-g|", "f-o-x|🦊", "b-o-x|📦", "s-i-x|6️⃣", "w-e-t|💧", "j-o-g|",
            "y-e-s|✅", "z-i-p|🤐", "b-u-zz|🐝", "qu-i-z|❓", "qu-a-ck|", "y-u-m|😋", "z-a-p|⚡",
            "sh-i-p|🚢", "f-i-sh|🐟", "ch-i-p|🍟", "ch-i-n|", "b-a-th|🛁", "r-i-ng|💍", "s-o-ng|🎵", "sh-o-p|🏪", "k-i-ng|👑", "d-i-sh|🍽️", "l-u-n-ch|🍱",
            "r-ai-n|🌧️", "t-ai-l|", "b-ee|🐝", "s-ee|👀", "f-ee-t|🦶", "n-igh-t|🌃", "l-igh-t|💡", "b-oa-t|⛵", "g-oa-t|🐐", "c-oa-t|🧥",
            "m-oo-n|🌙", "f-oo-d|🍲", "b-oo-t|👢", "sh-ee-p|🐑",
            "c-ar|🚗", "f-ar-m|🚜", "s-t-ar|⭐", "f-or-k|🍴", "c-or-n|🌽", "s-ur-f|🏄", "t-ur-n|", "c-ow|🐄", "d-ow-n|⬇️", "c-oi-n|🪙", "ow-l|🦉",
            "ear|👂", "h-ear|", "n-ear|", "f-ear|😨", "h-air|💇", "ch-air|🪑", "f-air|", "h-er|", "f-er-n|🌿"};

    static final String[][] TRICKY = {{"I", "the"}, {"to", "no", "go"}, {"into", "put"}, {"he", "she", "we", "me", "be"},
            {"was", "my", "you"}, {"they", "all", "are"}, {"her", "said", "so"}, {"have", "like", "some"},
            {"come", "were", "there"}, {"little", "one", "do", "what"}};

    /** A sentence for each step, using only the sounds and tricky words taught so far. */
    static final String[] SENTENCES = {"Sit, Pip, sit!", "The cat is on the mat.", "The duck can run.", "He has a red bell.",
            "The fox is in the box.", "Yes, it is a quiz!", "She has a fish and a ship.", "The goat can see the moon at night.",
            "The cow is on the farm.", "Her hair is near her ear."};

    /** One sound, several spellings: sound, spellings, examples. */
    private static final String[][] FAMILIES = {
            {"eee", "ee · ea · y · e", "bee 🐝 · sea 🌊 · happy 😊 · me 🙋"},
            {"ay", "ai · ay · a-e", "rain 🌧️ · day ☀️ · cake 🎂"},
            {"eye", "igh · ie · y · i-e", "night 🌃 · pie 🥧 · fly 🪰 · kite 🪁"},
            {"oh", "oa · ow · o-e", "boat ⛵ · snow ❄️ · bone 🦴"},
            {"ooh", "oo · ue · u-e · ew", "moon 🌙 · blue 🔵 · flute 🎶 · stew 🍲"}};

    private static final String[] ALL_SOUNDS;

    static {
        List<String> g = new ArrayList<>();
        for (String s : STEPS) g.addAll(Arrays.asList(s.split(" ")));
        Collections.sort(g, (x, y) -> y.length() - x.length());
        ALL_SOUNDS = g.toArray(new String[0]);
    }

    private PhonicsCourse() {
    }

    static int stepOf(String sound) {
        for (int i = 0; i < STEPS.length; i++) if (Arrays.asList(STEPS[i].split(" ")).contains(sound)) return i;
        return 99;
    }

    static String[] sound(String g) {
        for (String s : SOUNDS) if (s.startsWith(g + "|")) return s.split("\\|", -1);
        return new String[]{g, g, "", ""};
    }

    /** {sounds joined by "-", picture, the word} for all words that use only sounds up to this step. */
    static List<String[]> words(int step, boolean onlyPictures) {
        List<String[]> out = new ArrayList<>();
        for (String w : WORDS) {
            String[] p = w.split("\\|", -1);
            int max = 0;
            for (String g : p[0].split("-")) max = Math.max(max, stepOf(g));
            if (max > step || (onlyPictures && p[1].isEmpty())) continue;
            out.add(new String[]{p[0], p[1], p[0].replace("-", "")});
        }
        return out;
    }

    /** Splits a word into known sound spellings (longest first); null if it can't be sounded out with the course sounds. */
    static List<String> split(String word) {
        List<String> out = new ArrayList<>();
        String w = word.toLowerCase();
        int i = 0;
        while (i < w.length()) {
            String hit = null;
            for (String g : ALL_SOUNDS)
                if (w.startsWith(g, i)) {
                    hit = g;
                    break;
                }
            if (hit == null) return null;
            out.add(hit);
            i += hit.length();
        }
        return out;
    }

    static int unlocked(MainActivity a) {
        int g = a.grade();
        int base = g >= 3 ? STEPS.length - 1 : g == 2 ? 5 : 0;
        return Math.max(base, Math.min(STEPS.length - 1, a.prefs.getInt("course_step", 0)));
    }

    static void passed(MainActivity a, int step) {
        a.prefs.edit().putBoolean("course_done_" + step, true).putInt("course_step", Math.max(a.prefs.getInt("course_step", 0), step + 1)).apply();
    }

    // ---- menus ----

    static Runnable open(final MainActivity a) {
        return () -> {
            int open = unlocked(a);
            String[] labels = new String[STEPS.length];
            Runnable[] go = new Runnable[STEPS.length];
            for (int i = 0; i < STEPS.length; i++) {
                final int s = i;
                boolean done = a.prefs.getBoolean("course_done_" + i, false);
                labels[i] = (i <= open ? (done ? "⭐ " : "🔓 ") : "🔒 ") + "Step " + (i + 1) + "  ·  " + STEPS[i];
                go[i] = i <= open ? step(a, s) : () -> {
                    a.speaker.say("Finish step " + s + " first.");
                    Toast.makeText(a, "Pass “Spell it” in Step " + s + " to open this step", Toast.LENGTH_LONG).show();
                };
            }
            HomeScreen.menu(a, "🪜 Phonics steps", COLOR, labels, go).run();
        };
    }

    private static Runnable step(final MainActivity a, final int s) {
        return () -> {
            List<String> labels = new ArrayList<>();
            List<Runnable> go = new ArrayList<>();
            labels.add("🔊  Learn the sounds:  " + STEPS[s]);
            go.add(CardScreen.open(a, soundCards(s), "Step " + (s + 1) + " sounds", COLOR));
            labels.add("🧩  Blend & read");
            go.add(() -> a.show(new Blend(a, s).make(), false, COLOR, MainActivity.LEARN));
            if (words(s, true).size() >= 4) {
                labels.add("🖼️  Read & match the picture");
                go.add(() -> a.show(new Match(a, s).make(), false, COLOR, MainActivity.QUIZ));
            }
            labels.add("✍️  Spell it (opens the next step)");
            go.add(() -> a.show(new Spell(a, s).make(), false, COLOR, MainActivity.QUIZ));
            labels.add("⭐  Tricky words: " + String.join(", ", TRICKY[s]));
            go.add(CardScreen.open(a, trickyCards(s), "Tricky words", COLOR));
            labels.add("📜  Read a sentence");
            go.add(() -> a.show(sentence(a, s), false, COLOR, MainActivity.LEARN));
            if (s == STEPS.length - 1) {
                labels.add("👪  Sound families: one sound, many spellings");
                go.add(CardScreen.open(a, familyCards(), "Sound families", COLOR));
            }
            HomeScreen.menu(a, "Step " + (s + 1) + "  ·  " + STEPS[s], COLOR, labels.toArray(new String[0]), go.toArray(new Runnable[0])).run();
        };
    }

    private static List<Item> soundCards(int s) {
        List<Item> out = new ArrayList<>();
        String[] gs = STEPS[s].split(" ");
        for (int i = 0; i < gs.length; i++) {
            String[] r = sound(gs[i]);
            Item it = new Item(r[0], r[3].isEmpty() ? null : r[3], r[2], "", r[1] + ". " + r[1] + ". As in " + r[2] + ".", Data.BRIGHT[i % Data.BRIGHT.length]);
            it.note = "says “" + r[1] + "”";
            it.hear = r[2];
            out.add(it);
        }
        return out;
    }

    private static List<Item> trickyCards(int s) {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < TRICKY[s].length; i++) {
            String w = TRICKY[s][i];
            Item it = new Item(w, null, "", "You can't sound this one out — learn it by sight", w + ". " + w + ".", 0xFFE53935);
            it.note = "tricky word";
            it.hear = w;
            out.add(it);
        }
        return out;
    }

    private static List<Item> familyCards() {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < FAMILIES.length; i++) {
            String[] f = FAMILIES[i];
            Item it = new Item(f[1], null, f[2], "All of these say “" + f[0] + "”", f[0] + ". " + f[2].replaceAll("[^a-z ·]", "").replace("·", ","),
                    Data.BRIGHT[i % Data.BRIGHT.length]);
            it.note = "same sound: “" + f[0] + "”";
            out.add(it);
        }
        return out;
    }

    private static LinearLayout page(MainActivity a, String title) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, title));
        return page;
    }

    private static TextView tool(MainActivity a, String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private static TextView tile(MainActivity a, String g, int color) {
        TextView t = Ui.text(a, g, 38, Color.WHITE, true);
        t.setBackground(Ui.tile(a, color, 14));
        t.setMinWidth(Ui.dp(a, 58));
        t.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), Ui.dp(a, 6));
        return t;
    }

    // ---- blend & read ----

    private static final class Blend {
        private final MainActivity a;
        private final int s;
        private final List<String[]> words;
        private int idx, run;
        private LinearLayout tiles;
        private TextView pic, info;

        Blend(MainActivity a, int s) {
            this.a = a;
            this.s = s;
            List<String[]> now = new ArrayList<>(), before = new ArrayList<>();
            for (String[] w : PhonicsCourse.words(s, false)) {
                int max = 0;
                for (String g : w[0].split("-")) max = Math.max(max, stepOf(g));
                (max == s ? now : before).add(w);
            }
            Collections.shuffle(before, a.rnd);
            words = new ArrayList<>(now);
            words.addAll(before.subList(0, Math.min(6, before.size())));
        }

        View make() {
            LinearLayout page = page(a, "Blend & read · Step " + (s + 1));
            info = Ui.text(a, "", 17, Color.WHITE, true);
            info.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), 0);
            page.addView(info);
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 26)));
            pic = Ui.text(a, "", 80, Ui.INK, false);
            card.addView(pic);
            tiles = new LinearLayout(a);
            tiles.setGravity(Gravity.CENTER);
            card.addView(tiles, Ui.lp(Ui.MATCH, Ui.dp(a, 90), 0));
            card.addView(Ui.text(a, "Tap each sound, then blend them together", 15, 0xFF78909C, false));
            LinearLayout.LayoutParams clp = Ui.lp(Ui.MATCH, 0, 1);
            clp.setMargins(Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 14), Ui.dp(a, 8));
            page.addView(card, clp);
            LinearLayout row = new LinearLayout(a);
            row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 4));
            row.addView(tool(a, "▶ Sound it out", this::blend), Ui.lp(a, 0, Ui.dp(a, 52), 1.2f, 3));
            row.addView(tool(a, "🎤 Say it", this::sayIt), Ui.lp(a, 0, Ui.dp(a, 52), 1, 3));
            page.addView(row);
            LinearLayout row2 = new LinearLayout(a);
            row2.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 14));
            row2.addView(tool(a, "◀ Back", () -> go(idx - 1)), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
            row2.addView(tool(a, "Next word ▶", () -> go(idx + 1)), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
            page.addView(row2);
            page.post(() -> go(0));
            return page;
        }

        private void go(int i) {
            if (i < 0 || i >= words.size()) return;
            idx = i;
            run++;
            String[] w = words.get(i);
            tiles.removeAllViews();
            String[] gs = w[0].split("-");
            for (int k = 0; k < gs.length; k++) {
                final String g = gs[k];
                TextView t = tile(a, g, Data.BRIGHT[k % Data.BRIGHT.length]);
                Ui.click(t, () -> a.speaker.say(sound(g)[1]));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.MATCH);
                lp.setMargins(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
                tiles.addView(t, lp);
            }
            pic.setText("❓");
            info.setText("Word " + (i + 1) + " of " + words.size());
        }

        private void blend() {
            final int r = ++run;
            final String[] w = words.get(idx);
            final String[] gs = w[0].split("-");
            long t = 0;
            for (int k = 0; k < gs.length; k++) {
                final int kk = k;
                a.later(t, () -> {
                    if (r != run) return;
                    for (int j = 0; j < tiles.getChildCount(); j++)
                        tiles.getChildAt(j).setBackground(Ui.tile(a, j == kk ? 0xFFFDD835 : Data.BRIGHT[j % Data.BRIGHT.length], 14));
                    Ui.bounce(tiles.getChildAt(kk));
                    a.speaker.say(sound(gs[kk])[1]);
                });
                t += 900;
            }
            a.later(t + 200, () -> {
                if (r != run) return;
                for (int j = 0; j < tiles.getChildCount(); j++) tiles.getChildAt(j).setBackground(Ui.tile(a, 0xFF43A047, 14));
                pic.setText(w[1].isEmpty() ? "✅" : w[1]);
                Ui.popIn(pic, 0);
                info.setText(String.join(" - ", gs) + "  →  " + w[2]);
                a.speaker.say(w[2] + "! " + w[2] + ".");
            });
        }

        private void sayIt() {
            final String word = words.get(idx)[2];
            a.listen(a.enTag(), "Read the word aloud", res -> {
                boolean ok = false;
                for (String x : res) if (x.toLowerCase().contains(word)) ok = true;
                if (ok) {
                    info.setText("✅ Yes! You read “" + word + "”");
                    a.speaker.good();
                    a.addStars(1);
                    a.confetti();
                } else {
                    info.setText("I heard “" + res.get(0) + "”. Sound it out and try again!");
                    a.speaker.bad();
                }
            });
        }
    }

    // ---- read & match: read the word, pick its picture ----

    private static final class Match {
        private static final int ROUNDS = 8;
        private final MainActivity a;
        private final int s;
        private final List<String[]> pool;
        private int n, won;
        private boolean first, locked;
        private LinearLayout body;
        private TextView prog;

        Match(MainActivity a, int s) {
            this.a = a;
            this.s = s;
            this.pool = PhonicsCourse.words(s, true);
        }

        View make() {
            LinearLayout page = page(a, "Read & match · Step " + (s + 1));
            prog = Ui.text(a, "", 16, Color.WHITE, true);
            page.addView(prog);
            body = new LinearLayout(a);
            body.setOrientation(LinearLayout.VERTICAL);
            int p = Ui.dp(a, 12);
            body.setPadding(p, 0, p, p);
            page.addView(body, Ui.lp(Ui.MATCH, 0, 1));
            page.post(this::next);
            return page;
        }

        private void next() {
            if (n >= ROUNDS) {
                QuizScreen.result(a, "Phonics read & match " + (s + 1), won, ROUNDS, COLOR, () -> a.show(new Match(a, s).make(), false, COLOR, MainActivity.QUIZ));
                return;
            }
            Collections.shuffle(pool, a.rnd);
            List<String[]> opts = new ArrayList<>();
            List<String> pics = new ArrayList<>();
            for (String[] w : pool) {
                if (pics.contains(w[1])) continue;
                opts.add(w);
                pics.add(w[1]);
                if (opts.size() == 3) break;
            }
            final String[] right = opts.get(0);
            Collections.shuffle(opts, a.rnd);
            first = true;
            locked = false;
            prog.setText("Read the word, then tap its picture  ·  " + (n + 1) + " of " + ROUNDS);
            body.removeAllViews();
            LinearLayout word = new LinearLayout(a);
            word.setGravity(Gravity.CENTER);
            String[] gs = right[0].split("-");
            for (int k = 0; k < gs.length; k++) {
                final String g = gs[k];
                TextView t = tile(a, g, Data.BRIGHT[k % Data.BRIGHT.length]);
                Ui.click(t, () -> a.speaker.say(sound(g)[1]));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.dp(a, 80));
                lp.setMargins(Ui.dp(a, 3), Ui.dp(a, 8), Ui.dp(a, 3), Ui.dp(a, 8));
                word.addView(t, lp);
            }
            body.addView(word);
            for (final String[] o : opts) {
                final TextView t = Ui.text(a, o[1], 64, Ui.INK, false);
                t.setBackground(Ui.tile(a, Color.WHITE, 22));
                t.setOnClickListener(v -> {
                    if (locked) return;
                    if (o == right) {
                        locked = true;
                        t.setBackground(Ui.tile(a, 0xFF66BB6A, 22));
                        a.speaker.good();
                        a.speaker.say(right[2] + "!");
                        if (first) {
                            won++;
                            a.addStars(1);
                        }
                        a.confetti();
                        n++;
                        a.later(1300, this::next);
                    } else {
                        first = false;
                        t.setBackground(Ui.tile(a, 0xFFEF9A9A, 22));
                        Ui.shake(t);
                        a.speaker.say("Sound it out again");
                    }
                });
                body.addView(t, Ui.lp(a, Ui.MATCH, 0, 1, 6));
            }
        }
    }

    // ---- spell it: hear a word, build it from sound tiles ----

    private static final class Spell {
        private static final int ROUNDS = 8;
        private final MainActivity a;
        private final int s;
        private final List<String[]> pool;
        private int n, won;
        private boolean first, locked;
        private String[] word, parts;
        private final List<String> built = new ArrayList<>();
        private LinearLayout slots, tray;
        private TextView prog, pic;

        Spell(MainActivity a, int s) {
            this.a = a;
            this.s = s;
            List<String[]> pics = PhonicsCourse.words(s, true);
            this.pool = pics.size() >= ROUNDS ? pics : PhonicsCourse.words(s, false);
            Collections.shuffle(pool, a.rnd);
        }

        View make() {
            LinearLayout page = page(a, "Spell it · Step " + (s + 1));
            prog = Ui.text(a, "", 16, Color.WHITE, true);
            page.addView(prog);
            pic = Ui.text(a, "", 80, Color.WHITE, false);
            Ui.click(pic, this::say);
            page.addView(pic);
            page.addView(Ui.text(a, "Listen, then tap the sounds in order", 17, Color.WHITE, true));
            slots = new LinearLayout(a);
            slots.setGravity(Gravity.CENTER);
            page.addView(slots, Ui.lp(Ui.MATCH, Ui.dp(a, 84), 0));
            tray = new LinearLayout(a);
            tray.setGravity(Gravity.CENTER);
            page.addView(tray, Ui.lp(Ui.MATCH, 0, 1));
            LinearLayout row = new LinearLayout(a);
            row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 14));
            row.addView(tool(a, "🔊 Hear it", this::say), Ui.lp(a, 0, Ui.dp(a, 52), 1, 3));
            row.addView(tool(a, "⌫ Undo", () -> {
                if (!locked && !built.isEmpty()) {
                    built.remove(built.size() - 1);
                    render(Color.WHITE);
                }
            }), Ui.lp(a, 0, Ui.dp(a, 52), 1, 3));
            page.addView(row);
            page.post(this::next);
            return page;
        }

        private void say() {
            if (word != null && !a.speaker.say(word[2] + ". " + word[2] + ".", "en"))
                prog.setText("No voice on this phone — the word is “" + word[2] + "”");
        }

        private void next() {
            if (n >= ROUNDS) {
                if (won >= 6) {
                    passed(a, s);
                    Toast.makeText(a, s + 1 < STEPS.length ? "🎉 Step " + (s + 2) + " is open!" : "🎉 You finished all the steps!", Toast.LENGTH_LONG).show();
                }
                QuizScreen.result(a, "Phonics spelling " + (s + 1), won, ROUNDS, COLOR, () -> a.show(new Spell(a, s).make(), false, COLOR, MainActivity.QUIZ));
                return;
            }
            word = pool.get(n % pool.size());
            parts = word[0].split("-");
            built.clear();
            first = true;
            locked = false;
            prog.setText("Word " + (n + 1) + " of " + ROUNDS + "  ·  get 6 right to open the next step");
            pic.setText(word[1].isEmpty() ? "🔊" : word[1]);
            Ui.popIn(pic, 0);
            List<String> choice = new ArrayList<>(Arrays.asList(parts));
            List<String> known = new ArrayList<>();
            for (int i = 0; i <= s; i++) known.addAll(Arrays.asList(STEPS[i].split(" ")));
            Collections.shuffle(known, a.rnd);
            for (String g : known) {
                if (choice.size() >= parts.length + 4) break;
                if (!choice.contains(g)) choice.add(g);
            }
            Collections.shuffle(choice, a.rnd);
            tray.removeAllViews();
            FlowLayout flow = new FlowLayout(a);
            int p = Ui.dp(a, 10);
            flow.setPadding(p, p, p, p);
            for (int i = 0; i < choice.size(); i++) {
                final String g = choice.get(i);
                TextView t = tile(a, g, Data.BRIGHT[i % Data.BRIGHT.length]);
                t.setTextSize(32);
                Ui.click(t, () -> pick(g));
                flow.addView(t);
            }
            tray.addView(flow, Ui.lp(Ui.MATCH, Ui.WRAP, 0));
            render(Color.WHITE);
            say();
        }

        private void pick(String g) {
            if (locked || built.size() >= parts.length) return;
            built.add(g);
            a.speaker.say(sound(g)[1]);
            render(Color.WHITE);
            if (built.size() == parts.length) check();
        }

        private void render(int bg) {
            slots.removeAllViews();
            for (int i = 0; i < parts.length; i++) {
                TextView t = Ui.text(a, i < built.size() ? built.get(i) : "", 32, Ui.INK, true);
                t.setBackground(Ui.round(i < built.size() ? bg : 0x66FFFFFF, Ui.dp(a, 12)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 70), Ui.dp(a, 70));
                lp.setMargins(Ui.dp(a, 4), 0, Ui.dp(a, 4), 0);
                slots.addView(t, lp);
            }
        }

        private void check() {
            locked = true;
            if (built.equals(Arrays.asList(parts))) {
                render(0xFFA5D6A7);
                a.speaker.good();
                a.speaker.say(QuizScreen.PRAISE[a.rnd.nextInt(QuizScreen.PRAISE.length)] + " " + word[2] + ".");
                if (first) {
                    won++;
                    a.addStars(1);
                }
                a.confetti();
                n++;
                a.later(1600, this::next);
            } else {
                first = false;
                render(0xFFEF9A9A);
                Ui.shake(slots);
                a.speaker.say("Not quite. Listen again.");
                a.later(900, () -> {
                    built.clear();
                    locked = false;
                    render(Color.WHITE);
                    say();
                });
            }
        }
    }

    // ---- read a sentence (sound spellings underlined, tricky words in red) ----

    private static View sentence(final MainActivity a, final int s) {
        LinearLayout page = page(a, "Read a sentence · Step " + (s + 1));
        final TextView info = Ui.text(a, "Tap a word to hear it. Red words are tricky words.", 16, Color.WHITE, true);
        info.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), 0);
        page.addView(info);
        FlowLayout flow = new FlowLayout(a);
        int p = Ui.dp(a, 16);
        flow.setPadding(p, p, p, p);
        flow.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 24)));
        final List<String> tricky = new ArrayList<>();
        for (int i = 0; i <= s; i++) for (String w : TRICKY[i]) tricky.add(w.toLowerCase());
        final String sent = SENTENCES[s];
        final List<TextView> chips = new ArrayList<>();
        for (String raw : sent.split(" ")) {
            final String clean = raw.replaceAll("[^A-Za-z]", "");
            boolean isTricky = tricky.contains(clean.toLowerCase());
            List<String> parts = isTricky ? null : split(clean);
            SpannableString sp = new SpannableString(raw);
            if (isTricky) sp.setSpan(new ForegroundColorSpan(0xFFE53935), 0, raw.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (parts != null) {
                int at = raw.indexOf(clean.isEmpty() ? raw : clean.substring(0, 1)), k = 0;
                for (String g : parts) {
                    if (at < 0 || at + g.length() > raw.length()) break;
                    sp.setSpan(new UnderlineSpan(), at, at + g.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sp.setSpan(new ForegroundColorSpan(Data.BRIGHT[(k++ % 4) + 3]), at, at + g.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    at += g.length();
                }
            }
            TextView t = Ui.text(a, "", 34, Ui.INK, true);
            t.setText(sp);
            t.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 2));
            Ui.click(t, () -> a.speaker.say(clean));
            chips.add(t);
            flow.addView(t);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(flow);
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, 0, 1);
        slp.setMargins(Ui.dp(a, 14), Ui.dp(a, 10), Ui.dp(a, 14), Ui.dp(a, 10));
        page.addView(sv, slp);
        TextView read = Ui.button(a, "▶ Read it with me", 0xFFFFD54F, Ui.INK, () -> {
            for (int i = 0; i < chips.size(); i++) {
                final int k = i;
                a.later(i * 700L, () -> {
                    for (int j = 0; j < chips.size(); j++) chips.get(j).setBackground(j == k ? Ui.round(0xFFFFF59D, Ui.dp(a, 10)) : null);
                });
            }
            a.later(chips.size() * 700L + 300, () -> {
                for (TextView c : chips) c.setBackground(null);
                info.setText("⭐ Well read!");
            });
            a.speaker.say(sent);
            if (!a.prefs.getBoolean("course_read_" + s, false)) {
                a.prefs.edit().putBoolean("course_read_" + s, true).apply();
                a.addStars(1);
            }
        });
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.setMargins(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 16));
        page.addView(read, lp);
        return page;
    }
}
