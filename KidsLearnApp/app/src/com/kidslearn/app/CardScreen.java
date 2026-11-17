package com.kidslearn.app;

import android.graphics.Color;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Flash cards with audio: tap the card to hear it again, swipe or use the arrows, tap the microphone to say it. */
final class CardScreen {
    private final MainActivity a;
    private final int cat;
    private final List<Item> items;
    private int idx;
    private LinearLayout card;
    private TextView counter, mic;

    private CardScreen(MainActivity a, int cat) {
        this.a = a;
        this.cat = cat;
        this.items = Data.items(cat);
    }

    static View build(MainActivity a, int cat, String title, int color) {
        return new CardScreen(a, cat).make(title, color);
    }

    private View make(String title, int color) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, title));

        card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 30)));
        int p = Ui.dp(a, 16);
        card.setPadding(p, p, p, p);
        final GestureDetector gd = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Ui.bounce(card);
                speak();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (Math.abs(vx) <= Math.abs(vy)) return false;
                move(vx < 0 ? 1 : -1);
                return true;
            }
        });
        card.setOnTouchListener((v, e) -> gd.onTouchEvent(e));
        LinearLayout.LayoutParams clp = Ui.lp(Ui.MATCH, 0, 1);
        clp.setMargins(Ui.dp(a, 18), Ui.dp(a, 4), Ui.dp(a, 18), Ui.dp(a, 8));
        page.addView(card, clp);

        counter = Ui.text(a, "", 16, Color.WHITE, true);
        counter.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), 0);
        page.addView(counter);

        LinearLayout nav = new LinearLayout(a);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 20));
        int size = Ui.dp(a, 64);
        TextView prev = Ui.circleButton(a, "◀", 24, () -> move(-1));
        prev.setContentDescription("Previous");
        TextView speak = Ui.circleButton(a, "🔊", 24, this::speak);
        speak.setContentDescription("Say it again");
        mic = Ui.circleButton(a, "🎤", 24, this::listen);
        mic.setContentDescription("Now you say it");
        TextView next = Ui.circleButton(a, "▶", 24, () -> move(1));
        next.setContentDescription("Next");
        for (TextView b : new TextView[]{prev, speak, mic, next}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(Ui.dp(a, 9), 0, Ui.dp(a, 9), 0);
            nav.addView(b, lp);
        }
        page.addView(nav);

        fill(0);
        return page;
    }

    private void move(int d) {
        idx = (idx + d + items.size()) % items.size();
        fill(d);
    }

    private void speak() {
        Item it = items.get(idx);
        if (!a.speaker.say(it.say, it.lang))
            counter.setText("No " + (it.lang.equals("kn") ? "Kannada" : "Hindi") + " voice on this phone yet — see Parents › Help");
    }

    private void listen() {
        final Item it = items.get(idx);
        if (it.hear == null) return;
        a.listen(it.lang.equals("en") ? a.enTag() : it.lang + "-IN", "Say: " + it.hear, results -> {
            String want = it.hear.toLowerCase().trim();
            boolean ok = false;
            for (String r : results) if (r.toLowerCase().contains(want)) ok = true;
            if (ok) {
                counter.setText("Great! You said “" + it.hear + "”");
                a.speaker.good();
                a.confetti();
                a.addStars(1);
                a.speaker.say("Well done!");
            } else {
                counter.setText("I heard “" + results.get(0) + "”. Try again!");
                a.speaker.bad();
            }
        });
    }

    private static float glyphSize(String g) {
        int n = g.length();
        return n <= 3 ? 96 : n <= 5 ? 76 : n <= 7 ? 58 : 42;
    }

    private void fill(int dir) {
        Item it = items.get(idx);
        card.removeAllViews();
        if (it.glyph != null) card.addView(Ui.text(a, it.glyph, glyphSize(it.glyph), it.color, true));
        if (!it.note.isEmpty()) card.addView(Ui.text(a, it.note, 22, 0xFF78909C, true));
        if (it.emoji != null) {
            float sp = cat == Data.NUMBERS ? 24 : 72;
            card.addView(Ui.text(a, it.emoji, sp, Ui.INK, false));
        }
        if (!it.title.isEmpty()) {
            TextView title = Ui.text(a, it.title, it.title.length() > 16 ? 26 : 34, Ui.INK, true);
            title.setPadding(0, Ui.dp(a, 10), 0, 0);
            card.addView(title);
        }
        if (!it.sub.isEmpty()) card.addView(Ui.text(a, it.sub, 20, 0xFF78909C, false));
        counter.setText((idx + 1) + " / " + items.size());
        mic.setVisibility(it.hear == null ? View.GONE : View.VISIBLE);

        card.animate().cancel();
        card.setScaleX(1f);
        card.setScaleY(1f);
        card.setTranslationX(dir * Ui.dp(a, 70));
        card.setAlpha(dir == 0 ? 1f : 0.2f);
        card.animate().translationX(0).alpha(1f).setDuration(220).start();
        speak();
    }
}
