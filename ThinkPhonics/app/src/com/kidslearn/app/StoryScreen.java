package com.kidslearn.app;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Read-along stories and the phonics chant: the face reads aloud and each word lights up as it is spoken. */
final class StoryScreen {
    static final int COLOR = 0xFF5E35B1;

    /** First row: title, icon. Then: picture, text. */
    private static final String[][][] STORIES = {
            {{"Pip the Pup", "🐶"},
                    {"🐶", "Pip is a little pup. Pip can run and hop."},
                    {"🦴", "Pip has a bone. Pip digs a hole in the mud."},
                    {"🐱", "A cat sat on the wall. The cat saw the hole."},
                    {"😺", "The cat dug and dug. The cat got the bone!"},
                    {"😢", "Pip was sad. Then the cat came back and gave the bone to Pip."},
                    {"🤝", "Now Pip and the cat are best friends."}},
            {{"Ben and the Bus", "🚌"},
                    {"🧢", "Ben has a red cap. Ben gets on the bus."},
                    {"🚌", "The bus is big. The bus goes fast."},
                    {"🌧️", "It is wet. Rain taps on the bus."},
                    {"🏫", "The bus stops at the school. Ben gets off."},
                    {"😀", "Ben runs in. His friends say, Hello, Ben!"}},
            {{"The Little Seed", "🌱"},
                    {"🌰", "Mia has a seed. She puts it in the soil."},
                    {"💧", "She gives it water every day."},
                    {"☀️", "The sun is hot. The seed wakes up."},
                    {"🌱", "A green shoot pops up!"},
                    {"🌻", "Now it is a tall sunflower. Mia is so happy."}},
    };

    private final MainActivity a;
    private final String title;
    private final String[][] pages; // picture, text
    private int idx;
    private boolean auto;
    private String speaking;
    private FaceView face;
    private TextView pic, text, info, autoBtn;

    private StoryScreen(MainActivity a, String title, String[][] pages) {
        this.a = a;
        this.title = title;
        this.pages = pages;
    }

    static Runnable shelf(final MainActivity a) {
        int n = STORIES.length + 1;
        String[] labels = new String[n];
        Runnable[] go = new Runnable[n];
        labels[0] = "🎵  ABC phonics chant";
        go[0] = () -> {
            String[][] p = new String[Data.LETTER_SOUND.length][];
            for (int i = 0; i < p.length; i++) {
                String[] r = Data.LETTER_SOUND[i];
                String up = r[0].toUpperCase();
                p[i] = new String[]{r[3], up + ", " + r[0] + ", " + r[2] + ". " + up + " says " + r[1] + ", " + r[1] + ", " + r[2] + "!"};
            }
            a.show(new StoryScreen(a, "ABC phonics chant", p).make(true), false, COLOR, MainActivity.AUDIO);
        };
        for (int i = 0; i < STORIES.length; i++) {
            final String[][] s = STORIES[i];
            labels[i + 1] = s[0][1] + "  " + s[0][0];
            go[i + 1] = () -> {
                String[][] p = new String[s.length - 1][];
                System.arraycopy(s, 1, p, 0, p.length);
                a.show(new StoryScreen(a, s[0][0], p).make(false), false, COLOR, MainActivity.AUDIO);
            };
        }
        return HomeScreen.menu(a, "Read-along", COLOR, labels, go);
    }

    private View make(boolean autoPlay) {
        auto = autoPlay;
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, title));

        LinearLayout top = new LinearLayout(a);
        top.setGravity(Gravity.CENTER);
        face = new FaceView(a);
        top.addView(face, new LinearLayout.LayoutParams(Ui.dp(a, 130), Ui.dp(a, 130)));
        pic = Ui.text(a, "", 76, Color.WHITE, false);
        top.addView(pic, Ui.lp(0, Ui.WRAP, 1));
        page.addView(top);

        text = Ui.text(a, "", 30, Ui.INK, true);
        text.setLineSpacing(0, 1.35f);
        text.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 26)));
        int p = Ui.dp(a, 20);
        text.setPadding(p, p, p, p);
        text.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_UP) wordAt(e.getX(), e.getY());
            return true;
        });
        LinearLayout.LayoutParams tlp = Ui.lp(Ui.MATCH, 0, 1);
        tlp.setMargins(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 6));
        page.addView(text, tlp);

        info = Ui.text(a, "", 15, Color.WHITE, true);
        page.addView(info);

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 16));
        TextView prev = Ui.circleButton(a, "◀", 24, () -> go(idx - 1));
        TextView read = Ui.circleButton(a, "🔊", 24, this::read);
        autoBtn = Ui.circleButton(a, auto ? "⏸" : "⏯", 24, () -> {
            auto = !auto;
            autoBtn.setText(auto ? "⏸" : "⏯");
            if (auto) read();
            else a.speaker.stop();
        });
        autoBtn.setContentDescription("Read the whole story");
        TextView next = Ui.circleButton(a, "▶", 24, () -> go(idx + 1));
        for (TextView b : new TextView[]{prev, read, autoBtn, next}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 62), Ui.dp(a, 62));
            lp.setMargins(Ui.dp(a, 9), 0, Ui.dp(a, 9), 0);
            nav.addView(b, lp);
        }
        page.addView(nav);

        a.speaker.track = new Speaker.Track() {
            @Override
            public void range(String id, int start, int end) {
                if (id.equals(speaking)) highlight(start, end);
            }

            @Override
            public void done(String id) {
                if (!id.equals(speaking)) return;
                speaking = null;
                highlight(-1, -1);
                if (auto) {
                    if (idx < pages.length - 1) a.later(900, () -> go(idx + 1));
                    else finish();
                }
            }
        };
        page.post(() -> show(true));
        return page;
    }

    private void go(int i) {
        if (i < 0 || i >= pages.length) return;
        idx = i;
        show(true);
    }

    private void show(boolean speak) {
        pic.setText(pages[idx][0]);
        Ui.popIn(pic, 0);
        highlight(-1, -1);
        info.setText((idx + 1) + " / " + pages.length + "   ·   tap a word to hear it");
        if (speak) read();
    }

    private int silentRun;

    private void read() {
        silentRun++;
        speaking = a.speaker.sayTracked(pages[idx][1]);
        if (speaking == null) readSilently();
    }

    /** No voice (sound off or no speech engine): still light up the words at reading speed. */
    private void readSilently() {
        final int run = silentRun, page = idx;
        final String s = pages[idx][1];
        long t = 0;
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && !Character.isLetter(s.charAt(i))) i++;
            final int st = i;
            while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
            final int en = i;
            if (en > st) {
                a.later(t, () -> {
                    if (run == silentRun && page == idx) highlight(st, en);
                });
                t += 380;
            }
        }
        a.later(t + 300, () -> {
            if (run != silentRun || page != idx) return;
            highlight(-1, -1);
            if (auto) {
                if (idx < pages.length - 1) a.later(700, () -> go(idx + 1));
                else finish();
            }
        });
    }

    private void highlight(int start, int end) {
        String s = pages[idx][1];
        SpannableString sp = new SpannableString(s);
        if (start >= 0 && end <= s.length() && end > start)
            sp.setSpan(new BackgroundColorSpan(0xFFFFF176), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(sp);
    }

    private void wordAt(float x, float y) {
        int off = text.getOffsetForPosition(x, y);
        String s = pages[idx][1];
        if (off < 0 || off >= s.length() || !Character.isLetter(s.charAt(off))) return;
        int st = off, en = off;
        while (st > 0 && Character.isLetter(s.charAt(st - 1))) st--;
        while (en < s.length() && Character.isLetter(s.charAt(en))) en++;
        boolean wasAuto = auto;
        auto = false;
        silentRun++;
        autoBtn.setText("⏯");
        speaking = null;
        highlight(st, en);
        a.speaker.say(s.substring(st, en));
        if (wasAuto) info.setText("Paused — press ⏯ to keep reading");
    }

    private void finish() {
        auto = false;
        autoBtn.setText("⏯");
        info.setText("The end! ⭐");
        a.addStars(2);
        a.confetti();
        a.speaker.say("The end! Well done!");
    }
}

/** Talk time: the face asks a simple question, the child answers aloud, and the face replies. */
final class TalkScreen {
    static final int COLOR = 0xFF0097A7;
    private static final String[][] QUESTIONS = {
            {"What is your name?", "Nice to meet you, %s!"},
            {"How old are you?", "Wow, %s! You are growing up fast!"},
            {"What is your favourite colour?", "%s is a lovely colour!"},
            {"Which animal do you like?", "I like %s too!"},
            {"What did you eat today?", "Yummy! %s sounds tasty."},
            {"What do you like to play?", "%s sounds like fun!"},
            {"Who is your best friend?", "%s sounds like a great friend!"},
            {"What is the weather like today?", "Thank you! It is %s today."},
    };
    private static final String[] PREFIXES = {"my name is ", "i am ", "i'm ", "my favourite colour is ", "my favorite color is ",
            "my favourite is ", "i like ", "i ate ", "i like to play ", "my best friend is ", "it is ", "it's "};

    private TalkScreen() {
    }

    static View build(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Talk time"));
        final FaceView face = new FaceView(a);
        page.addView(face, Ui.lp(Ui.MATCH, Ui.dp(a, 220), 0));
        final TextView bubble = Ui.text(a, "", 26, Ui.INK, true);
        bubble.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 24)));
        int p = Ui.dp(a, 18);
        bubble.setPadding(p, p, p, p);
        LinearLayout.LayoutParams blp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        blp.setMargins(Ui.dp(a, 16), 0, Ui.dp(a, 16), Ui.dp(a, 10));
        page.addView(bubble, blp);
        final TextView you = Ui.text(a, "", 22, Color.WHITE, true);
        you.setPadding(p, 0, p, 0);
        page.addView(you, Ui.lp(Ui.MATCH, 0, 1));

        final int[] q = {0};
        final Runnable ask = () -> {
            bubble.setText("🙂 " + QUESTIONS[q[0]][0]);
            you.setText("Tap 🎤 and answer out loud");
            a.speaker.say(QUESTIONS[q[0]][0]);
        };
        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 16));
        TextView mic = Ui.button(a, "🎤 Answer", 0xFFFFD54F, Ui.INK, () -> a.listen(a.enTag(), QUESTIONS[q[0]][0], res -> {
            String said = res.get(0).trim();
            String core = said;
            String low = said.toLowerCase();
            for (String pre : PREFIXES)
                if (low.startsWith(pre)) {
                    core = said.substring(pre.length());
                    break;
                }
            if (!core.isEmpty()) core = Character.toUpperCase(core.charAt(0)) + core.substring(1);
            you.setText("You said: “" + said + "”");
            String reply = String.format(QUESTIONS[q[0]][1], core);
            bubble.setText("😄 " + reply);
            a.speaker.say(reply);
            a.addStars(1);
        }));
        row.addView(mic, Ui.lp(a, 0, Ui.WRAP, 1, 4));
        row.addView(Ui.button(a, "Next ▶", Color.WHITE, Ui.INK, () -> {
            q[0] = (q[0] + 1) % QUESTIONS.length;
            ask.run();
        }), Ui.lp(a, 0, Ui.WRAP, 1, 4));
        page.addView(row);
        page.post(ask);
        return page;
    }
}
