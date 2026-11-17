package com.kidslearn.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** A friendly cartoon face. Its mouth can hold the shape of one sound, or move while the voice is talking. */
final class FaceView extends View implements Speaker.Talk {
    static final int REST = 0, A = 1, E = 2, O = 3, U = 4, MBP = 5, FV = 6, TH = 7, L = 8, S = 9, SH = 10, R = 11, K = 12;

    /** width, height, roundness, lip thickness, top teeth, bottom teeth, tongue (0 none, 1 low, 2 tip up, 3 out, 4 back). */
    private static final float[][] SHAPE = {
            null,
            {0.62f, 0.50f, 1f, 1f, 1, 0, 1},
            {0.74f, 0.22f, 0.8f, 1f, 1, 1, 0},
            {0.40f, 0.44f, 1f, 1.4f, 0, 0, 1},
            {0.22f, 0.20f, 1f, 1.9f, 0, 0, 0},
            null,
            {0.50f, 0.14f, 0.8f, 1f, 1, 0, 0},
            {0.50f, 0.20f, 0.8f, 1f, 1, 1, 3},
            {0.50f, 0.32f, 0.9f, 1f, 1, 0, 2},
            {0.62f, 0.13f, 0.7f, 1f, 1, 1, 0},
            {0.36f, 0.30f, 0.6f, 1.7f, 1, 1, 0},
            {0.40f, 0.26f, 0.9f, 1.3f, 1, 0, 4},
            {0.50f, 0.32f, 0.9f, 1f, 1, 0, 4}};

    static final String[] TIP = {
            "Pick a sound to see the lips.",
            "Open your mouth wide, like at the doctor: aah.",
            "Smile wide with your teeth a little apart: eh, ee.",
            "Make your lips round like a ball: o.",
            "Push your lips into a small, tight circle: oo, w.",
            "Press your lips together, then pop them open: m, b, p.",
            "Rest your top teeth on your bottom lip and blow: f, v.",
            "Put the tip of your tongue between your teeth and blow: th.",
            "Touch your tongue just behind your top teeth: l, n, t, d.",
            "Keep your teeth close, smile and hiss like a snake: s, z.",
            "Push your lips forward like a kiss and blow: sh, ch, j.",
            "Round your lips a little and pull your tongue back: r.",
            "Open a little and lift the back of your tongue: k, g."};

    private static final int SKIN = 0xFFFFCC80, HAIR = 0xFF4E342E, LIP = 0xFFE57373, MOUTH = 0xFF6D1B1B, TONGUE = 0xFFF48FB1, INK = 0xFF3E2723;
    private static final int[] TALK = {A, E, O, K, L, A, E, MBP, S, A};

    private final MainActivity a;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Random rnd = new Random();
    private int fixed = -1, shown = REST;
    private boolean talking, blink;
    private float pulse = 0.5f;
    private long nextBlink;
    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            step();
            postDelayed(this, 110);
        }
    };

    FaceView(MainActivity a) {
        super(a);
        this.a = a;
        setContentDescription("Talking face");
    }

    /** Which mouth shape makes this sound (a letter or a pair like "sh"). */
    static int viseme(String unit) {
        String u = unit.toLowerCase();
        switch (u) {
            case "sh":
            case "ch":
                return SH;
            case "th":
                return TH;
            case "oo":
            case "qu":
            case "wh":
                return U;
            case "ee":
            case "ai":
                return E;
            case "ar":
            case "ow":
                return A;
            case "ck":
                return K;
            case "ll":
                return L;
            case "ss":
                return S;
            case "ph":
                return FV;
            default:
                break;
        }
        char c = u.isEmpty() ? ' ' : u.charAt(0);
        switch (c) {
            case 'a':
            case 'h':
            case 'u':
                return A;
            case 'e':
            case 'i':
            case 'y':
                return E;
            case 'o':
                return O;
            case 'w':
            case 'q':
                return U;
            case 'b':
            case 'm':
            case 'p':
                return MBP;
            case 'f':
            case 'v':
                return FV;
            case 'l':
            case 'n':
            case 't':
            case 'd':
                return L;
            case 's':
            case 'z':
            case 'x':
                return S;
            case 'j':
                return SH;
            case 'r':
                return R;
            case 'c':
            case 'k':
            case 'g':
                return K;
            default:
                return REST;
        }
    }

    /** Holds one mouth shape (it still moves a little while the voice speaks). */
    void show(int v) {
        fixed = v;
        shown = v;
        invalidate();
    }

    /** Lets the mouth move freely with the voice. */
    void auto() {
        fixed = -1;
        shown = talking ? A : REST;
        invalidate();
    }

    @Override
    public void talking(boolean on) {
        talking = on;
        if (!on && fixed < 0) shown = REST;
        invalidate();
    }

    private void step() {
        long now = SystemClock.uptimeMillis();
        if (now > nextBlink) {
            blink = true;
            nextBlink = now + 2500 + rnd.nextInt(2500);
        } else blink = false;
        if (talking) {
            if (fixed < 0) shown = TALK[rnd.nextInt(TALK.length)];
            pulse = rnd.nextFloat();
        } else pulse = 0.5f;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        a.speaker.talk = this;
        nextBlink = SystemClock.uptimeMillis() + 2000;
        post(loop);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (a.speaker.talk == this) a.speaker.talk = null;
        removeCallbacks(loop);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), s = Math.min(w, h), cx = w / 2, cy = h / 2, R0 = s * 0.42f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(SKIN);
        c.drawCircle(cx - R0 * 0.98f, cy + R0 * 0.05f, R0 * 0.17f, p);
        c.drawCircle(cx + R0 * 0.98f, cy + R0 * 0.05f, R0 * 0.17f, p);
        c.drawCircle(cx, cy, R0, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(HAIR);
        p.setStrokeWidth(R0 * 0.32f);
        r.set(cx - R0 * 0.86f, cy - R0 * 0.86f, cx + R0 * 0.86f, cy + R0 * 0.86f);
        c.drawArc(r, 200, 140, false, p);

        float ex = R0 * 0.36f, ey = cy - R0 * 0.1f;
        for (int sign = -1; sign <= 1; sign += 2) {
            float x = cx + sign * ex;
            p.setColor(INK);
            p.setStrokeWidth(R0 * 0.045f);
            r.set(x - R0 * 0.15f, ey - R0 * 0.36f, x + R0 * 0.15f, ey - R0 * 0.16f);
            c.drawArc(r, 200, 140, false, p);
            if (blink) {
                c.drawLine(x - R0 * 0.13f, ey, x + R0 * 0.13f, ey, p);
            } else {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.WHITE);
                r.set(x - R0 * 0.15f, ey - R0 * 0.17f, x + R0 * 0.15f, ey + R0 * 0.17f);
                c.drawOval(r, p);
                p.setColor(INK);
                c.drawCircle(x, ey + R0 * 0.02f, R0 * 0.085f, p);
                p.setColor(Color.WHITE);
                c.drawCircle(x + R0 * 0.03f, ey - R0 * 0.02f, R0 * 0.028f, p);
                p.setStyle(Paint.Style.STROKE);
            }
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x55F06292);
        c.drawCircle(cx - R0 * 0.58f, cy + R0 * 0.25f, R0 * 0.13f, p);
        c.drawCircle(cx + R0 * 0.58f, cy + R0 * 0.25f, R0 * 0.13f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0xFFE0A060);
        p.setStrokeWidth(R0 * 0.04f);
        r.set(cx - R0 * 0.08f, cy + R0 * 0.02f, cx + R0 * 0.08f, cy + R0 * 0.2f);
        c.drawArc(r, 20, 140, false, p);
        p.setStyle(Paint.Style.FILL);
        mouth(c, cx, cy + R0 * 0.52f, R0);
    }

    private void mouth(Canvas c, float mx, float my, float R0) {
        int v = shown;
        if (v == REST) {
            p.setStyle(Paint.Style.STROKE);
            p.setColor(LIP);
            p.setStrokeWidth(R0 * 0.06f);
            path.reset();
            path.moveTo(mx - R0 * 0.28f, my - R0 * 0.04f);
            path.quadTo(mx, my + R0 * 0.16f, mx + R0 * 0.28f, my - R0 * 0.04f);
            c.drawPath(path, p);
            p.setStyle(Paint.Style.FILL);
            return;
        }
        if (v == MBP) {
            float sq = talking ? 1f + (pulse - 0.5f) * 0.1f : 1f;
            p.setColor(LIP);
            r.set(mx - R0 * 0.27f * sq, my - R0 * 0.07f, mx + R0 * 0.27f * sq, my + R0 * 0.07f);
            c.drawRoundRect(r, R0 * 0.07f, R0 * 0.07f, p);
            p.setColor(0xFFB71C1C);
            p.setStrokeWidth(R0 * 0.025f);
            c.drawLine(mx - R0 * 0.24f * sq, my, mx + R0 * 0.24f * sq, my, p);
            return;
        }
        float[] sh = SHAPE[v];
        float open = 1f + (talking ? (pulse - 0.5f) * 0.25f : 0f);
        float mw = R0 * sh[0], mh = R0 * sh[1] * open, lip = R0 * 0.07f * sh[3];
        boolean round = sh[2] >= 1f;
        float rad = round ? 0 : (mh / 2) * sh[2];
        p.setColor(LIP);
        r.set(mx - mw / 2 - lip, my - mh / 2 - lip, mx + mw / 2 + lip, my + mh / 2 + lip);
        if (round) c.drawOval(r, p);
        else c.drawRoundRect(r, rad + lip, rad + lip, p);
        path.reset();
        r.set(mx - mw / 2, my - mh / 2, mx + mw / 2, my + mh / 2);
        if (round) path.addOval(r, Path.Direction.CW);
        else path.addRoundRect(r, rad, rad, Path.Direction.CW);
        p.setColor(MOUTH);
        c.drawPath(path, p);
        c.save();
        c.clipPath(path);
        float th = v == FV ? mh : Math.max(R0 * 0.07f, mh * 0.28f);
        p.setColor(Color.WHITE);
        if (sh[4] > 0) c.drawRect(mx - mw / 2, my - mh / 2, mx + mw / 2, my - mh / 2 + th, p);
        if (sh[5] > 0) c.drawRect(mx - mw / 2, my + mh / 2 - th, mx + mw / 2, my + mh / 2, p);
        int tongue = (int) sh[6];
        p.setColor(TONGUE);
        if (tongue == 1) c.drawOval(mx - mw * 0.32f, my + mh * 0.05f, mx + mw * 0.32f, my + mh * 0.75f, p);
        if (tongue == 2) c.drawOval(mx - mw * 0.18f, my - mh * 0.35f, mx + mw * 0.18f, my + mh * 0.6f, p);
        if (tongue == 4) c.drawOval(mx - mw * 0.4f, my + mh * 0.15f, mx + mw * 0.4f, my + mh * 0.9f, p);
        c.restore();
        if (tongue == 3) {
            p.setColor(TONGUE);
            c.drawOval(mx - mw * 0.22f, my - mh * 0.1f, mx + mw * 0.22f, my + mh / 2 + lip * 1.4f, p);
        }
    }
}
