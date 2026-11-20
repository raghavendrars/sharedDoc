package com.kidslearn.app;

import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/** The child's class (pre-school to Class 5), which decides what is shown, and the voice guide for children who can't read yet. */
final class Grade {
    static final String[] NAMES = {"Pre-school / UKG", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5"};
    private static final String[] ICONS = {"🧸", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣"};
    static final int COLOR = 0xFF5E35B1;

    private static WeakReference<View> pending = new WeakReference<>(null);
    private static long pendingAt;
    private static boolean hintGiven;

    private Grade() {
    }

    /** "Which class are you in?" — shown on first start and from the parents area. */
    static View picker(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        int p = Ui.dp(a, 20);
        page.setPadding(p, p, p, p);
        page.addView(Ui.text(a, "🎒", 64, Color.WHITE, false));
        page.addView(Ui.text(a, "Which class are you in?", 28, Color.WHITE, true));
        TextView sub = Ui.text(a, "The app shows the right lessons for your class. A parent can change it later.", 16, 0xFFD1C4E9, false);
        sub.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 10));
        page.addView(sub);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < NAMES.length; i++) {
            final int g = i;
            TextView b = Ui.button(a, ICONS[i] + "  " + NAMES[i], Color.WHITE, Ui.INK, () -> {
                boolean first = !a.prefs.contains("grade");
                a.prefs.edit().putInt("grade", g).apply();
                if (first || !a.prefs.getBoolean("voiceGuideSet", false))
                    a.prefs.edit().putBoolean("voiceGuide", g <= 1).apply();
                a.speaker.say(NAMES[g] + ". Let's learn!");
                a.later(600, a::home);
            });
            LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
            lp.topMargin = Ui.dp(a, 10);
            box.addView(b, lp);
            Ui.popIn(b, 60L * i);
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));
        page.post(() -> a.speaker.say("Which class are you in? Tap your class."));
        return page;
    }

    /** Text the voice can read: drops emoji and symbols, keeps letters in any script. */
    static String speakable(String label) {
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < label.length()) {
            int cp = label.codePointAt(i);
            if (Character.isLetterOrDigit(cp) || Character.getType(cp) == Character.NON_SPACING_MARK
                    || Character.getType(cp) == Character.COMBINING_SPACING_MARK) b.appendCodePoint(cp);
            else if (cp == '&') b.append(" and ");
            else if (cp == '·' || cp == '—' || cp == ',') b.append(", ");
            else if (Character.isWhitespace(cp) || cp == '\'' || cp == '?' || cp == '.' || cp == '-') b.append((char) cp);
            i += Character.charCount(cp);
        }
        return b.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Click handling for menu tiles and buttons. With the voice guide on, the first tap reads the name aloud and a
     * second tap opens it; otherwise one tap opens it.
     */
    static void guided(final MainActivity a, final View v, final String label, final Runnable go) {
        Ui.click(v, () -> {
            if (!a.voiceGuide()) {
                a.later(140, go);
                return;
            }
            long now = SystemClock.uptimeMillis();
            if (pending.get() == v && now - pendingAt < 8000) {
                pending = new WeakReference<>(null);
                a.later(140, go);
                return;
            }
            pending = new WeakReference<>(v);
            pendingAt = now;
            String say = speakable(label);
            if (!hintGiven) {
                hintGiven = true;
                say += ". Tap again to open.";
            }
            a.speaker.say(say);
        });
    }

    /** Tile and menu visibility by class. */
    static boolean allowed(MainActivity a, int minGrade) {
        return a.grade() >= minGrade;
    }

    static View banner(MainActivity a, String s) {
        TextView t = Ui.text(a, s, 14, 0xFF5D4037, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
        return t;
    }
}
