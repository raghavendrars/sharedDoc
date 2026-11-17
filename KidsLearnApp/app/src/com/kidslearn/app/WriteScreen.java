package com.kidslearn.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finger-writing with automatic checking, three levels (Easy: a hand traces the shape for you; Normal: trace the grey
 * shape; Free: write from memory), pencil sizes and colours, a chalkboard look, and recording your own voice.
 */
final class WriteScreen {
    static final int COLOR = 0xFF00695C;
    static final String[] MODES = {"Letters", "Numbers", "Words", "Kannada letters", "Hindi letters", "Kannada numbers", "Hindi numbers"};
    private static final String[] WORDS = {"cat", "dog", "sun", "hat", "pen", "bus", "cup", "box", "fish", "frog", "milk", "tree", "moon", "star", "ball", "book"};
    private static final String[] LEVELS = {"✋ Easy", "✏️ Normal", "🌟 Free"};
    private static final int[] INKS = {0xFF1565C0, 0xFFE53935, 0xFF43A047, 0xFF8E24AA, 0xFFFB8C00};
    private static final int[] SIZES = {7, 12, 18};

    private final MainActivity a;
    private final String title, lang;
    private final int color;
    private final boolean chalkStart;
    private final List<String> items;
    private final Set<Integer> starred = new HashSet<>();
    private int idx;
    private WritePad pad;
    private TextView target, result, recBtn, levelBtn, boardBtn;
    private final List<View> inkDots = new ArrayList<>(), sizeDots = new ArrayList<>();
    private Voice voice;

    private WriteScreen(MainActivity a, String title, List<String> items, String lang, int color, boolean chalk) {
        this.a = a;
        this.title = title;
        this.items = items;
        this.lang = lang;
        this.color = color;
        this.chalkStart = chalk;
    }

    static Runnable open(final MainActivity a, final int mode) {
        List<String> items = new ArrayList<>();
        String lang = "en";
        switch (mode) {
            case 0:
                for (char c = 'A'; c <= 'Z'; c++) items.add(String.valueOf(c));
                for (char c = 'a'; c <= 'z'; c++) items.add(String.valueOf(c));
                break;
            case 1:
                for (int i = 0; i <= 20; i++) items.add(String.valueOf(i));
                break;
            case 2:
                for (String w : WORDS) items.add(w);
                break;
            case 3:
            case 4:
                lang = mode == 3 ? "kn" : "hi";
                for (String[] r : mode == 3 ? Data.join(Data.KN_V, Data.KN_C) : Data.join(Data.HI_V, Data.HI_C)) items.add(r[0]);
                break;
            default:
                lang = mode == 5 ? "kn" : "hi";
                for (int i = 0; i <= 10; i++) items.add(i == 0 ? Data.digits(0, lang) : Data.digits(i, lang));
                break;
        }
        return openList(a, "Write · " + MODES[mode], items, lang, COLOR, lang.equals("hi"));
    }

    static Runnable openList(MainActivity a, String title, List<String> items, String lang, int color) {
        return openList(a, title, items, lang, color, lang.equals("hi"));
    }

    static Runnable openList(final MainActivity a, final String title, final List<String> items, final String lang, final int color, final boolean chalk) {
        return () -> a.show(new WriteScreen(a, title, items, lang, color, chalk).make(), false, color, MainActivity.LEARN);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, title));

        target = Ui.text(a, "", 22, Color.WHITE, true);
        Ui.click(target, this::say);
        page.addView(target);

        pad = new WritePad(a);
        pad.setChalk(chalkStart);
        LinearLayout.LayoutParams plp = Ui.lp(Ui.MATCH, 0, 1);
        plp.setMargins(Ui.dp(a, 12), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 4));
        page.addView(pad, plp);

        result = Ui.text(a, "", 17, 0xFFFFF59D, true);
        page.addView(result);
        WaveView wave = new WaveView(a);
        LinearLayout.LayoutParams wlp = Ui.lp(Ui.MATCH, Ui.dp(a, 26), 0);
        wlp.setMargins(Ui.dp(a, 20), 0, Ui.dp(a, 20), 0);
        page.addView(wave, wlp);
        voice = new Voice(a, wave, "write-voice", (msg, recording) -> {
            result.setText(msg);
            recBtn.setText(recording ? "⏹ Stop" : "🎙 Record");
        });

        // pencil: sizes, colours, board
        LinearLayout pens = new LinearLayout(a);
        pens.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), Ui.dp(a, 8), 0);
        for (int i = 0; i < SIZES.length; i++) {
            final int k = i;
            TextView s = Ui.text(a, i == 0 ? "•" : i == 1 ? "●" : "⬤", 12 + i * 5, Ui.INK, true);
            s.setOnClickListener(v -> {
                pad.width = Ui.dp(a, SIZES[k]);
                markSizes(k);
                Ui.bounce(v);
            });
            sizeDots.add(s);
            pens.addView(s, Ui.lp(a, 0, Ui.dp(a, 34), 1, 2));
        }
        for (int i = 0; i < INKS.length; i++) {
            final int k = i;
            View d = new View(a);
            d.setOnClickListener(v -> {
                pad.ink = INKS[k];
                markInks(k);
                Ui.bounce(v);
            });
            inkDots.add(d);
            pens.addView(d, Ui.lp(a, 0, Ui.dp(a, 34), 1, 2));
        }
        boardBtn = Ui.text(a, "", 13, Ui.INK, true);
        boardBtn.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 10)));
        Ui.click(boardBtn, () -> {
            pad.setChalk(!pad.chalk);
            boardBtn.setText(pad.chalk ? "⬜ Paper" : "🟩 Board");
            markInks(-1);
        });
        boardBtn.setText(chalkStart ? "⬜ Paper" : "🟩 Board");
        pens.addView(boardBtn, Ui.lp(a, 0, Ui.dp(a, 34), 2.2f, 2));
        page.addView(pens);
        markSizes(1);
        markInks(-1);

        LinearLayout row1 = new LinearLayout(a);
        row1.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), 0);
        row1.addView(tool("◀", () -> show(idx - 1)), Ui.lp(a, 0, Ui.dp(a, 46), 0.7f, 3));
        levelBtn = tool(LEVELS[1], this::nextLevel);
        row1.addView(levelBtn, Ui.lp(a, 0, Ui.dp(a, 46), 1.4f, 3));
        row1.addView(tool("Undo", () -> pad.undo()), Ui.lp(a, 0, Ui.dp(a, 46), 1f, 3));
        row1.addView(tool("Clear", () -> {
            pad.clear();
            result.setText("");
        }), Ui.lp(a, 0, Ui.dp(a, 46), 1f, 3));
        row1.addView(tool("▶", () -> show(idx + 1)), Ui.lp(a, 0, Ui.dp(a, 46), 0.7f, 3));
        page.addView(row1);

        LinearLayout row2 = new LinearLayout(a);
        row2.setPadding(Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 6), Ui.dp(a, 12));
        TextView check = tool("✓ Check", this::check);
        check.setBackground(Ui.round(0xFFFFD54F, Ui.dp(a, 14)));
        row2.addView(check, Ui.lp(a, 0, Ui.dp(a, 50), 1f, 3));
        recBtn = tool("🎙 Record", () -> voice.toggleRecord(items.get(idx)));
        row2.addView(recBtn, Ui.lp(a, 0, Ui.dp(a, 50), 1f, 3));
        row2.addView(tool("▶ Hear me", () -> voice.play()), Ui.lp(a, 0, Ui.dp(a, 50), 1f, 3));
        page.addView(row2);

        a.onLeave(voice::release);
        show(0);
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 15, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private void markSizes(int sel) {
        for (int i = 0; i < sizeDots.size(); i++)
            sizeDots.get(i).setBackground(Ui.round(i == sel ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 10)));
    }

    private void markInks(int sel) {
        for (int i = 0; i < inkDots.size(); i++) {
            GradientDrawable g = Ui.round(INKS[i], Ui.dp(a, 17));
            boolean on = sel >= 0 ? i == sel : INKS[i] == pad.ink;
            g.setStroke(Ui.dp(a, on ? 4 : 1), on ? Color.WHITE : 0x55000000);
            inkDots.get(i).setBackground(g);
        }
    }

    private void nextLevel() {
        pad.setLevel((pad.level + 1) % 3);
        levelBtn.setText(LEVELS[pad.level]);
        result.setText(pad.level == 0 ? "Watch the hand go round the shape, then trace it" : pad.level == 1 ? "Trace over the grey shape" : "Write it from memory!");
    }

    private void show(int i) {
        if (i < 0 || i >= items.size()) return;
        idx = i;
        String it = items.get(i);
        target.setText("Write:  " + it + "    (" + (i + 1) + "/" + items.size() + ")  🔊");
        pad.set(it);
        result.setText(pad.level == 0 ? "Watch the hand, then trace it" : "Write it, then press ✓ Check");
        say();
    }

    private void say() {
        if (!a.speaker.say(items.get(idx), lang)) result.setText("No " + (lang.equals("kn") ? "Kannada" : "Hindi") + " voice on this phone");
    }

    private void check() {
        float s = pad.score();
        if (s < 0) {
            result.setText("Write something first ✏️");
            return;
        }
        float great = pad.level == 0 ? 0.62f : 0.72f, good = pad.level == 0 ? 0.45f : 0.52f;
        if (s >= great) {
            result.setText("⭐⭐⭐ Excellent writing!");
            a.speaker.good();
            a.speaker.say("Excellent!");
            reward(pad.level == 2 ? 3 : 2);
        } else if (s >= good) {
            result.setText("⭐⭐ Good! Now say it aloud and record it 🎙");
            a.speaker.good();
            a.speaker.say("Good job!");
            reward(1);
        } else if (s >= 0.36f) {
            result.setText("⭐ Nearly! Clear and try once more");
            a.speaker.say("Nearly! Try again.");
        } else {
            result.setText("Try again — follow the shape carefully");
            a.speaker.bad();
            Ui.shake(pad);
        }
    }

    private void reward(int stars) {
        a.confetti();
        if (starred.add(idx)) a.addStars(stars);
    }
}

/** A writing pad. score() compares what was written with the shape of the target. */
final class WritePad extends View {
    private static final int G = 40;

    private static final class Stroke {
        final Path path = new Path();
        final int color;
        final float width;

        Stroke(int color, float width) {
            this.color = color;
            this.width = width;
        }
    }

    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Stroke cur;
    private float lx, ly;
    private String target = "";
    int level = 1, ink = 0xFF1565C0;
    float width;
    boolean chalk;
    // Easy level: the shape's outline, walked by an animated hand
    private final List<PathMeasure> contours = new ArrayList<>();
    private float totalLen, hintT;
    private ValueAnimator hint;
    private final Path traced = new Path();
    private final float[] pos = new float[2];

    WritePad(Context c) {
        super(c);
        width = Ui.dp(c, 12);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeJoin(Paint.Join.ROUND);
        guidePaint.setTypeface(Typeface.DEFAULT_BOLD);
        guidePaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setStrokeWidth(Ui.dp(c, 1.5f));
        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setStrokeWidth(Ui.dp(c, 5));
        hintPaint.setStrokeCap(Paint.Cap.ROUND);
        hintPaint.setColor(0xFFFF7043);
        handPaint.setTextSize(Ui.dp(c, 40));
        setChalk(false);
    }

    void setChalk(boolean on) {
        chalk = on;
        setBackground(Ui.round(on ? 0xFF2E4D3A : Color.WHITE, Ui.dp(getContext(), 22)));
        linePaint.setColor(on ? 0x44FFFFFF : 0xFFFFCDD2);
        ink = on ? 0xFFFFFDE7 : 0xFF1565C0;
        invalidate();
    }

    void setLevel(int l) {
        level = l;
        startHint();
        invalidate();
    }

    void set(String t) {
        target = t;
        clear();
        post(this::startHint);
    }

    void undo() {
        if (!strokes.isEmpty()) strokes.remove(strokes.size() - 1);
        invalidate();
    }

    void clear() {
        strokes.clear();
        invalidate();
    }

    private void sizeText(Paint paint, float w, float h) {
        paint.setTextSize(100);
        float tw = Math.max(1, paint.measureText(target));
        paint.setTextSize(Math.min(h * 0.7f, 100 * w * 0.85f / tw));
    }

    private float baseline(Paint paint, float h) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return h / 2 - (fm.ascent + fm.descent) / 2;
    }

    private void drawTarget(Canvas c, Paint paint, float w, float h) {
        sizeText(paint, w, h);
        c.drawText(target, w / 2, baseline(paint, h), paint);
    }

    /** Easy level: build the outline of the letter and send a hand round it. */
    private void startHint() {
        if (hint != null) hint.cancel();
        contours.clear();
        totalLen = 0;
        if (level != 0 || target.isEmpty() || getWidth() == 0) return;
        Paint p = new Paint(guidePaint);
        sizeText(p, getWidth(), getHeight());
        Path outline = new Path();
        p.getTextPath(target, 0, target.length(), getWidth() / 2f, baseline(p, getHeight()), outline);
        PathMeasure pm = new PathMeasure(outline, false);
        do {
            float len = pm.getLength();
            if (len > Ui.dp(getContext(), 20)) {
                Path seg = new Path();
                pm.getSegment(0, len, seg, true);
                contours.add(new PathMeasure(seg, false));
                totalLen += len;
            }
        } while (pm.nextContour());
        if (contours.isEmpty()) return;
        hint = ValueAnimator.ofFloat(0f, 1f);
        hint.setDuration((long) Math.min(9000, Math.max(3500, totalLen * 3)));
        hint.setInterpolator(new LinearInterpolator());
        hint.setRepeatCount(ValueAnimator.INFINITE);
        hint.addUpdateListener(v -> {
            hintT = (float) v.getAnimatedValue();
            invalidate();
        });
        hint.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        startHint();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (hint != null) hint.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                cur = new Stroke(ink, width);
                cur.path.moveTo(x, y);
                cur.path.lineTo(x + 0.1f, y + 0.1f);
                strokes.add(cur);
                lx = x;
                ly = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (cur != null) {
                    cur.path.quadTo(lx, ly, (x + lx) / 2, (y + ly) / 2);
                    lx = x;
                    ly = y;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (cur != null) cur.path.lineTo(x, y);
                cur = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        c.drawLine(Ui.dp(getContext(), 16), h * 0.15f, w - Ui.dp(getContext(), 16), h * 0.15f, linePaint);
        c.drawLine(Ui.dp(getContext(), 16), h * 0.85f, w - Ui.dp(getContext(), 16), h * 0.85f, linePaint);
        if (level < 2 && !target.isEmpty()) {
            guidePaint.setColor(chalk ? (level == 0 ? 0x66FFFFFF : 0x40FFFFFF) : (level == 0 ? 0xFFCFD8DC : 0xFFE0E4E8));
            drawTarget(c, guidePaint, w, h);
        }
        if (level == 0 && !contours.isEmpty()) {
            float d = hintT * totalLen;
            boolean placed = false;
            for (PathMeasure pm : contours) {
                float len = pm.getLength();
                if (d >= len) {
                    traced.reset();
                    pm.getSegment(0, len, traced, true);
                    c.drawPath(traced, hintPaint);
                    d -= len;
                } else {
                    traced.reset();
                    pm.getSegment(0, d, traced, true);
                    c.drawPath(traced, hintPaint);
                    pm.getPosTan(d, pos, null);
                    placed = true;
                    break;
                }
            }
            if (placed) c.drawText("👆", pos[0] - handPaint.getTextSize() * 0.35f, pos[1] + handPaint.getTextSize() * 0.95f, handPaint);
        }
        for (Stroke s : strokes) {
            pen.setColor(s.color);
            pen.setStrokeWidth(s.width);
            c.drawPath(s.path, pen);
        }
    }

    /** 0..1 for how well the writing matches the target's shape, or -1 if nothing is written. */
    float score() {
        if (strokes.isEmpty() || getWidth() == 0) return -1;
        int w = Math.max(1, getWidth() / 4), h = Math.max(1, getHeight() / 4);
        Bitmap tb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas tc = new Canvas(tb);
        tc.scale(0.25f, 0.25f);
        Paint solid = new Paint(guidePaint);
        solid.setColor(Color.BLACK);
        drawTarget(tc, solid, getWidth(), getHeight());
        Bitmap sb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas sc = new Canvas(sb);
        sc.scale(0.25f, 0.25f);
        pen.setColor(Color.BLACK);
        pen.setStrokeWidth(Ui.dp(getContext(), 12));
        for (Stroke s : strokes) sc.drawPath(s.path, pen);
        boolean[] t = grid(tb), s = grid(sb);
        tb.recycle();
        sb.recycle();
        if (t == null || s == null) return -1;
        boolean[] td = dilate(t), sd = dilate(s);
        int tn = 0, tHit = 0, sn = 0, sHit = 0;
        for (int i = 0; i < G * G; i++) {
            if (t[i]) {
                tn++;
                if (sd[i]) tHit++;
            }
            if (s[i]) {
                sn++;
                if (td[i]) sHit++;
            }
        }
        float coverage = tHit / (float) tn, precision = sHit / (float) sn;
        return Math.min(coverage, precision);
    }

    /** The ink, cropped to its bounding box and scaled into a G×G grid (keeping its shape). */
    private static boolean[] grid(Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        int minx = w, miny = h, maxx = -1, maxy = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((px[y * w + x] >>> 24) > 64) {
                    if (x < minx) minx = x;
                    if (x > maxx) maxx = x;
                    if (y < miny) miny = y;
                    if (y > maxy) maxy = y;
                }
        if (maxx < 0) return null;
        int bw = maxx - minx + 1, bh = maxy - miny + 1, size = Math.max(bw, bh);
        float ox = (size - bw) / 2f, oy = (size - bh) / 2f;
        boolean[] g = new boolean[G * G];
        for (int y = miny; y <= maxy; y++)
            for (int x = minx; x <= maxx; x++)
                if ((px[y * w + x] >>> 24) > 64) {
                    int gx = Math.min(G - 1, (int) ((x - minx + ox) * G / size));
                    int gy = Math.min(G - 1, (int) ((y - miny + oy) * G / size));
                    g[gy * G + gx] = true;
                }
        return g;
    }

    private static boolean[] dilate(boolean[] g) {
        boolean[] out = new boolean[G * G];
        int r = 2;
        for (int y = 0; y < G; y++)
            for (int x = 0; x < G; x++) {
                if (!g[y * G + x]) continue;
                for (int dy = -r; dy <= r; dy++)
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < G && ny < G) out[ny * G + nx] = true;
                    }
            }
        return out;
    }
}
