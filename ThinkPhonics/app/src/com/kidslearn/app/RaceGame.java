package com.kidslearn.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Turbo Circuit Mini: a top-down racing game (a small cousin of the Turbo Circuit PC game). Win coins in races, buy
 * faster cars and open new tracks. Shares the parent's game time with Blockhaven Mini.
 */
final class RaceGame {
    static final int COLOR = 0xFF1565C0;
    static final String TITLE = "Turbo Circuit Mini";

    // cars (numbers from the PC game)
    static final String[] CAR_NAME = {"Sparrow", "Courier", "Bison", "Drift Fox", "Vortex", "Apex One"};
    static final String[] CAR_KIND = {"City hatchback", "Family sedan", "Muscle coupe", "Rally special", "Supercar", "Hypercar"};
    static final int[] CAR_PRICE = {0, 60, 150, 260, 420, 650};
    static final float[] CAR_MAXV = {42f, 48f, 56f, 54f, 66f, 76f};
    static final float[] CAR_ACC = {7.0f, 8.0f, 10.0f, 10.5f, 12.5f, 15.0f};
    static final float[] CAR_GRIP = {15f, 16f, 15f, 20f, 21f, 24f};
    static final float[] CAR_BRAKE = {16f, 18f, 18f, 22f, 24f, 28f};
    static final float[] CAR_LEN = {3.6f, 4.4f, 4.7f, 4.0f, 4.5f, 4.8f};
    static final float[] CAR_WID = {1.7f, 1.8f, 1.95f, 1.85f, 2.0f, 2.05f};
    static final float[] CAR_CABLEN = {1.9f, 2.0f, 1.7f, 1.9f, 1.6f, 1.4f};
    static final float[] CAR_CABOFF = {-0.35f, -0.10f, -0.45f, -0.20f, -0.15f, 0.00f};
    static final boolean[] CAR_SPOILER = {false, false, false, true, true, true};

    // tracks
    static final String[] TRACK_NAME = {"Meadow Oval", "Dune Circuit", "Frost Pass", "Neon City", "Sunset Ridge"};
    static final String[] TRACK_BLURB = {"Wide, fast and forgiving", "Sweeping desert bends", "Twisty mountain snow run",
            "Tight street corners at night", "The long championship lap"};
    static final float[] TRACK_HW = {8f, 7.5f, 7f, 6.5f, 7f};
    static final int[] TRACK_PRIZE = {30, 45, 60, 80, 110};
    static final int[] TRACK_AI_CAR = {0, 1, 2, 3, 4};
    static final float[] TRACK_AI_SKILL = {0.80f, 0.82f, 0.84f, 0.86f, 0.88f};
    static final int[] TRACK_GROUND = {0xFF48963E, 0xFFD6B470, 0xFFE8EEF5, 0xFF1E2028, 0xFF786E3C};
    static final int[] TRACK_ROAD = {0xFF3E3E44, 0xFF4E4640, 0xFF545862, 0xFF32323A, 0xFF423C40};
    static final int[] TRACK_SKY = {0xFF46A0E6, 0xFFF0AA5A, 0xFF78A0D2, 0xFF080A20, 0xFF5A3C8C};
    static final float[][] TRACK_PTS = {
            {0, 0, 140, 0, 230, 25, 270, 95, 230, 165, 140, 190, 0, 190, -90, 165, -130, 95, -90, 25},
            {0, 0, 120, 0, 200, -30, 280, 10, 300, 100, 240, 170, 160, 140, 90, 170, 40, 250, -60, 260, -130, 190, -140, 90, -90, 20},
            {0, 0, 150, 0, 230, 30, 250, 110, 200, 170, 130, 150, 100, 90, 50, 80, 0, 110, -10, 180, 40, 240, 20, 310, -70, 320,
                    -140, 270, -150, 180, -110, 120, -140, 50, -80, 5},
            {0, 0, 160, 0, 205, 15, 220, 60, 220, 140, 205, 185, 160, 200, 110, 200, 80, 180, 70, 140, 50, 110, 0, 100, -50, 100,
                    -90, 85, -100, 50, -90, 15, -50, 0},
            {0, 0, 160, 0, 250, -40, 340, 0, 370, 90, 320, 160, 230, 150, 170, 100, 110, 110, 80, 170, 130, 230, 220, 240, 310, 260,
                    350, 330, 290, 400, 170, 390, 50, 360, -50, 300, -110, 210, -70, 130, -130, 70, -100, 10}};
    static final float[] SHARE = {1f, 0.6f, 0.4f, 0.25f};

    private RaceGame() {
    }

    static Runnable open(final MainActivity a) {
        return GameTime.open(a, TITLE, COLOR, () -> menu(a));
    }

    static String time(float t) {
        if (t <= 0) return "0:00.0";
        int m = (int) (t / 60);
        return m + ":" + String.format(Locale.ROOT, "%04.1f", t - m * 60);
    }

    private static int coins(MainActivity a) {
        return a.prefs.getInt("tc_coins", 40);
    }

    private static boolean owned(MainActivity a, int car) {
        return car == 0 || (a.prefs.getInt("tc_owned", 1) & (1 << car)) != 0;
    }

    private static int unlocked(MainActivity a) {
        return a.prefs.getInt("tc_unlocked", 1);
    }

    // ---- garage and track list ----

    private static View menu(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, TITLE));
        TextView timer = Ui.text(a, "", 16, Color.WHITE, true);
        page.addView(timer);

        ScrollView sv = new ScrollView(a);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(a, 14);
        list.setPadding(pad, Ui.dp(a, 6), pad, Ui.dp(a, 24));
        sv.addView(list);
        page.addView(sv, Ui.lp(Ui.MATCH, 0, 1));

        list.addView(Ui.text(a, "🪙 " + coins(a) + " coins", 26, 0xFFFFE082, true));
        list.addView(Ui.text(a, "Win races to earn coins, buy faster cars and open new tracks!", 14, 0xFFBBDEFB, false));

        list.addView(heading(a, "🚗 Your car"));
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(a);
        int sel = a.prefs.getInt("tc_car", 0);
        for (int c = 0; c < CAR_NAME.length; c++) row.addView(carCard(a, c, c == sel), Ui.lp(a, Ui.dp(a, 128), Ui.WRAP, 0, 4));
        hs.addView(row);
        list.addView(hs);

        list.addView(heading(a, "🏁 Pick a track"));
        for (int t = 0; t < TRACK_NAME.length; t++) list.addView(trackCard(a, t), Ui.lp(a, Ui.MATCH, Ui.WRAP, 0, 5));

        page.post(() -> GameTime.watch(a, timer, null, TITLE, COLOR, () -> menu(a)));
        if (a.voiceGuide()) a.speaker.say("Pick your car, then pick a track to race!");
        return page;
    }

    private static TextView heading(Context c, String s) {
        TextView t = Ui.text(c, s, 20, Color.WHITE, true);
        t.setGravity(Gravity.START);
        t.setPadding(Ui.dp(c, 4), Ui.dp(c, 18), 0, Ui.dp(c, 6));
        return t;
    }

    private static View carCard(final MainActivity a, final int car, boolean selected) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(a, 8);
        card.setPadding(p, p, p, p);
        card.setBackground(Ui.round(selected ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 16)));
        card.addView(new CarIcon(a, car, owned(a, car) ? 0xFFE53935 : 0xFF9E9E9E), new LinearLayout.LayoutParams(Ui.dp(a, 60), Ui.dp(a, 92)));
        card.addView(Ui.text(a, CAR_NAME[car], 16, Ui.INK, true));
        card.addView(Ui.text(a, CAR_KIND[car], 11, 0xFF607D8B, false));
        card.addView(bar(a, "⚡", CAR_MAXV[car] / 76f));
        card.addView(bar(a, "🛞", CAR_GRIP[car] / 24f));
        String status = selected ? "✔ Driving" : owned(a, car) ? "Tap to drive" : "🪙 " + CAR_PRICE[car];
        card.addView(Ui.text(a, status, 14, selected ? 0xFF2E7D32 : Ui.INK, true));
        Ui.click(card, () -> {
            if (owned(a, car)) {
                a.prefs.edit().putInt("tc_car", car).apply();
                a.speaker.say(CAR_NAME[car]);
                a.show(menu(a), false, COLOR, MainActivity.GAME);
            } else if (coins(a) >= CAR_PRICE[car]) {
                new AlertDialog.Builder(a).setTitle("Buy " + CAR_NAME[car] + "?")
                        .setMessage("It costs " + CAR_PRICE[car] + " coins. You have " + coins(a) + ".")
                        .setPositiveButton("Buy it", (d, w) -> {
                            a.prefs.edit().putInt("tc_coins", coins(a) - CAR_PRICE[car])
                                    .putInt("tc_owned", a.prefs.getInt("tc_owned", 1) | (1 << car)).putInt("tc_car", car).apply();
                            a.speaker.say("Yay! You bought the " + CAR_NAME[car] + "!");
                            a.confetti();
                            a.show(menu(a), false, COLOR, MainActivity.GAME);
                        })
                        .setNegativeButton("Not now", null).show();
            } else {
                a.speaker.say("You need " + (CAR_PRICE[car] - coins(a)) + " more coins for the " + CAR_NAME[car] + ". Win races to get coins!");
                Ui.shake(card);
            }
        });
        return card;
    }

    private static View bar(Context c, String icon, float f) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(c, icon, 11, Ui.INK, false));
        FrameLayout track = new FrameLayout(c);
        track.setBackground(Ui.round(0xFFE0E0E0, Ui.dp(c, 4)));
        View fill = new View(c);
        fill.setBackground(Ui.round(0xFF42A5F5, Ui.dp(c, 4)));
        track.addView(fill, new FrameLayout.LayoutParams((int) (Ui.dp(c, 70) * Math.min(1f, f)), Ui.dp(c, 7)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 70), Ui.dp(c, 7));
        lp.leftMargin = Ui.dp(c, 4);
        row.addView(track, lp);
        row.setPadding(0, Ui.dp(c, 3), 0, 0);
        return row;
    }

    private static View trackCard(final MainActivity a, final int t) {
        boolean open = t < unlocked(a);
        LinearLayout card = new LinearLayout(a);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(a, 10);
        card.setPadding(p, p, p, p);
        card.setBackground(Ui.round(open ? Color.WHITE : 0xFFB0BEC5, Ui.dp(a, 16)));
        card.addView(new TrackIcon(a, t), new LinearLayout.LayoutParams(Ui.dp(a, 76), Ui.dp(a, 76)));
        LinearLayout info = new LinearLayout(a);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(Ui.dp(a, 12), 0, 0, 0);
        TextView name = Ui.text(a, (open ? "" : "🔒 ") + TRACK_NAME[t], 19, Ui.INK, true);
        name.setGravity(Gravity.START);
        info.addView(name);
        TextView blurb = Ui.text(a, TRACK_BLURB[t], 13, 0xFF607D8B, false);
        blurb.setGravity(Gravity.START);
        info.addView(blurb);
        float best = a.prefs.getFloat("tc_best_" + t, 0);
        String more = open ? "🏆 Win " + TRACK_PRIZE[t] + " coins" + (best > 0 ? "   ·   Best " + time(best) : "")
                : "Finish in the top 3 at " + TRACK_NAME[t - 1] + " to open";
        TextView m = Ui.text(a, more, 13, open ? 0xFF1565C0 : 0xFF455A64, true);
        m.setGravity(Gravity.START);
        info.addView(m);
        card.addView(info, Ui.lp(0, Ui.WRAP, 1));
        Ui.click(card, () -> {
            if (!open) {
                a.speaker.say("Finish in the top 3 at " + TRACK_NAME[t - 1] + " to open " + TRACK_NAME[t]);
                Ui.shake(card);
                return;
            }
            String why = GameTime.why(a);
            if (why != null) {
                a.show(GameTime.locked(a, why, TITLE, COLOR, () -> menu(a)), false, COLOR, MainActivity.NONE);
                return;
            }
            a.show(race(a, t), false, TRACK_SKY[t], MainActivity.GAME);
        });
        return card;
    }

    // ---- the race ----

    @SuppressLint("ClickableViewAccessibility")
    private static View race(final MainActivity a, final int t) {
        final int car = a.prefs.getInt("tc_car", 0);
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(TRACK_SKY[t]);
        page.addView(Ui.topBar(a, TRACK_NAME[t]));
        FrameLayout frame = new FrameLayout(a);
        page.addView(frame, Ui.lp(Ui.MATCH, 0, 1));

        final RaceView rv = new RaceView(a, t, car, (place, time) -> a.show(results(a, t, place, time), false, COLOR, MainActivity.GAME));
        frame.addView(rv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView timer = Ui.text(a, "", 14, Color.WHITE, true);
        timer.setShadowLayer(6, 0, 2, 0xAA000000);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        tlp.bottomMargin = Ui.dp(a, 108);
        frame.addView(timer, tlp);

        // controls: steer on the left, brake and boost on the right; the car speeds up by itself
        LinearLayout pads = new LinearLayout(a);
        pads.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), Ui.dp(a, 14));
        pads.addView(pad(a, "◀", 0xDDFFFFFF, down -> rv.left = down));
        pads.addView(pad(a, "▶", 0xDDFFFFFF, down -> rv.right = down));
        pads.addView(new View(a), Ui.lp(0, 1, 1));
        pads.addView(pad(a, "🛑", 0xDDFFCDD2, down -> rv.brake = down));
        pads.addView(pad(a, "🚀", 0xDDB3E5FC, down -> rv.boost = down));
        frame.addView(pads, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        a.onLeave(rv::stop);
        page.post(() -> GameTime.watch(a, timer, rv::stop, TITLE, COLOR, () -> menu(a)));
        return page;
    }

    interface Hold {
        void on(boolean down);
    }

    @SuppressLint("ClickableViewAccessibility")
    private static View pad(Context c, String label, int bg, final Hold hold) {
        final TextView b = Ui.text(c, label, 30, Ui.INK, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.round(bg, Ui.dp(c, 40)));
        b.setOnTouchListener((v, e) -> {
            int act = e.getActionMasked();
            if (act == MotionEvent.ACTION_DOWN) {
                hold.on(true);
                v.setScaleX(0.9f);
                v.setScaleY(0.9f);
            } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                hold.on(false);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
            return true;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 80), Ui.dp(c, 80));
        lp.setMargins(Ui.dp(c, 5), 0, Ui.dp(c, 5), 0);
        b.setLayoutParams(lp);
        return b;
    }

    // ---- results ----

    private static View results(final MainActivity a, final int t, int place, float time) {
        int prize = Math.round(TRACK_PRIZE[t] * SHARE[place - 1]);
        float best = a.prefs.getFloat("tc_best_" + t, 0);
        boolean newBest = best <= 0 || time < best;
        boolean opens = place <= 3 && t + 1 < TRACK_NAME.length && unlocked(a) == t + 1;
        android.content.SharedPreferences.Editor ed = a.prefs.edit().putInt("tc_coins", coins(a) + prize);
        if (newBest) ed.putFloat("tc_best_" + t, time);
        if (opens) ed.putInt("tc_unlocked", t + 2);
        ed.apply();
        a.stats.addScore(TITLE + ": " + TRACK_NAME[t], 5 - place, 4);
        if (place <= 3) a.addStars(1);

        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, TITLE));
        TextView timer = Ui.text(a, "", 16, Color.WHITE, true);
        page.addView(timer);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int p = Ui.dp(a, 24);
        box.setPadding(p, p, p, p);
        String[] medal = {"", "🥇", "🥈", "🥉", "🏅"}, ord = {"", "1st", "2nd", "3rd", "4th"};
        box.addView(Ui.text(a, medal[place], 96, Color.WHITE, false));
        box.addView(Ui.text(a, "You came " + ord[place] + "!", 32, Color.WHITE, true));
        box.addView(Ui.text(a, "Time " + time(time) + (newBest ? "   ⭐ new best!" : ""), 18, 0xFFBBDEFB, true));
        box.addView(Ui.text(a, "+" + prize + " coins 🪙", 26, 0xFFFFE082, true));
        if (opens) box.addView(Ui.text(a, "🔓 " + TRACK_NAME[t + 1] + " is open now!", 20, 0xFFB9F6CA, true));
        TextView again = Ui.button(a, "🔁 Race again", Color.WHITE, Ui.INK, () -> a.show(race(a, t), false, TRACK_SKY[t], MainActivity.GAME));
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.topMargin = Ui.dp(a, 24);
        box.addView(again, lp);
        TextView menu = Ui.button(a, "🏁 Cars and tracks", 0xFFFFD54F, Ui.INK, () -> a.show(menu(a), false, COLOR, MainActivity.GAME));
        LinearLayout.LayoutParams lp2 = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp2.topMargin = Ui.dp(a, 12);
        box.addView(menu, lp2);
        page.addView(box, Ui.lp(Ui.MATCH, 0, 1));
        if (place == 1) a.confetti();
        a.speaker.say("You came " + ord[place] + "! You won " + prize + " coins." + (opens ? " " + TRACK_NAME[t + 1] + " is open now!" : ""));
        page.post(() -> GameTime.watch(a, timer, null, TITLE, COLOR, () -> menu(a)));
        return page;
    }

    // ---- little pictures for the menu ----

    private static final class CarIcon extends View {
        private final int car, col;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        CarIcon(Context c, int car, int col) {
            super(c);
            this.car = car;
            this.col = col;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth() / 2.6f, getHeight() / 5.4f);
            cv.save();
            cv.translate(getWidth() / 2f, getHeight() / 2f);
            cv.scale(s, -s); // nose up
            RaceView.drawCar(cv, p, car, col, false, false, 0);
            cv.restore();
        }
    }

    private static final class TrackIcon extends View {
        private final int t;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        TrackIcon(Context c, int t) {
            super(c);
            this.t = t;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), r = Ui.dp(getContext(), 14);
            p.setStyle(Paint.Style.FILL);
            p.setColor(TRACK_GROUND[t]);
            cv.drawRoundRect(0, 0, w, h, r, r, p);
            float[] pts = TRACK_PTS[t];
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (int i = 0; i < pts.length; i += 2) {
                minX = Math.min(minX, pts[i]);
                maxX = Math.max(maxX, pts[i]);
                minY = Math.min(minY, pts[i + 1]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            float span = Math.max(maxX - minX, maxY - minY), s = (w - 20) / span;
            float ox = (w - (maxX - minX) * s) / 2, oy = (h - (maxY - minY) * s) / 2;
            path.reset();
            for (int i = 0; i < pts.length; i += 2) {
                float x = ox + (pts[i] - minX) * s, y = oy + (pts[i + 1] - minY) * s;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(Ui.dp(getContext(), 7));
            p.setColor(TRACK_ROAD[t]);
            cv.drawPath(path, p);
            p.setStrokeWidth(Ui.dp(getContext(), 1.5f));
            p.setColor(0xCCFFFFFF);
            cv.drawPath(path, p);
        }
    }
}
