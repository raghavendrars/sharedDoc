package com.kidslearn.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** A sensory bin: dig through rice with a finger to find the hidden letter, number or shape. */
final class BinScreen {
    static final int COLOR = 0xFF8D6E63;
    static final int LETTERS = 0, NUMBERS = 1, SHAPES = 2;
    private static final String[][] SHAPE_ITEMS = {{"🔺", "triangle"}, {"⭐", "star"}, {"❤️", "heart"}, {"⚫", "circle"}, {"🟦", "square"}, {"🔶", "diamond"}};
    private static final int ROUNDS = 5;

    private final MainActivity a;
    private final int mode;
    private int round, won, target;
    private boolean firstTry, locked;
    private String[][] items; // shown label, spoken name
    private BinView bin;
    private TextView task;

    private BinScreen(MainActivity a, int mode) {
        this.a = a;
        this.mode = mode;
    }

    static Runnable open(final MainActivity a, final int mode) {
        return () -> a.show(new BinScreen(a, mode).make(), false, COLOR, MainActivity.LEARN);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Sensory bin"));
        task = Ui.text(a, "", 20, Color.WHITE, true);
        task.setPadding(Ui.dp(a, 14), 0, Ui.dp(a, 14), Ui.dp(a, 4));
        page.addView(task);
        bin = new BinView(a);
        bin.pick = this::picked;
        LinearLayout.LayoutParams blp = Ui.lp(Ui.MATCH, 0, 1);
        blp.setMargins(Ui.dp(a, 12), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 8));
        page.addView(bin, blp);
        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 16));
        row.addView(tool("🔊 Hear again", this::sayTask), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
        row.addView(tool("🔄 Mix the bin", () -> {
            if (!locked) bin.setItems(labels());
        }), Ui.lp(a, 0, Ui.dp(a, 52), 1, 4));
        page.addView(row);
        page.post(this::nextRound);
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 17, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private String[] labels() {
        String[] l = new String[items.length];
        for (int i = 0; i < l.length; i++) l[i] = items[i][0];
        return l;
    }

    private void nextRound() {
        if (round >= ROUNDS) {
            QuizScreen.result(a, "Sensory bin", won, ROUNDS, COLOR, open(a, mode));
            return;
        }
        List<String[]> pool = new ArrayList<>();
        if (mode == LETTERS) for (String[] r : Data.LETTER_SOUND) pool.add(new String[]{r[0], r[1]});
        else if (mode == NUMBERS) for (int i = 0; i <= 10; i++) pool.add(new String[]{String.valueOf(i), String.valueOf(i)});
        else for (String[] r : SHAPE_ITEMS) pool.add(r);
        Collections.shuffle(pool, a.rnd);
        items = pool.subList(0, 5).toArray(new String[0][]);
        target = a.rnd.nextInt(items.length);
        firstTry = true;
        locked = false;
        bin.setItems(labels());
        sayTask();
    }

    private String taskText() {
        String[] t = items[target];
        if (mode == LETTERS) return "Dig! Find the letter that says “" + t[1] + "”";
        if (mode == NUMBERS) return "Dig! Find the number " + t[0];
        return "Dig! Find the " + t[1] + " " + t[0];
    }

    private void sayTask() {
        task.setText("Round " + (round + 1) + " of " + ROUNDS + "\n" + taskText());
        String[] t = items[target];
        if (mode == LETTERS) a.speaker.say("Dig in the rice. Find the letter that says, " + t[1]);
        else a.speaker.say("Dig in the rice. Find the " + (mode == NUMBERS ? "number " : "") + t[1]);
    }

    private void picked(int i) {
        if (locked) return;
        if (i == target) {
            locked = true;
            a.speaker.good();
            a.speaker.say(QuizScreen.PRAISE[a.rnd.nextInt(QuizScreen.PRAISE.length)]);
            if (firstTry) {
                won++;
                a.addStars(1);
            }
            a.confetti();
            bin.celebrate(i);
            round++;
            a.later(1800, this::nextRound);
        } else {
            firstTry = false;
            String[] t = items[i];
            a.speaker.say(mode == LETTERS ? "That is " + t[0] + ". It says " + t[1] + ". Keep digging!" : "That is " + t[1] + ". Keep digging!");
        }
    }
}

final class BinView extends View {
    interface Pick {
        void picked(int i);
    }

    private static final int[] RICE = {0xFFFFF8E1, 0xFFF5E6C8, 0xFFEDE0C4, 0xFFFFFDE7, 0xFFE8D9B5};
    Pick pick;
    private String[] labels = new String[0];
    private RectF[] rects = new RectF[0];
    private int happy = -1;
    private Bitmap cover;
    private Canvas coverCanvas;
    private final Paint eraser = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Random rnd = new Random();
    private float downX, downY, lastX, lastY;

    BinView(Context c) {
        super(c);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setStyle(Paint.Style.STROKE);
        eraser.setStrokeCap(Paint.Cap.ROUND);
        eraser.setStrokeWidth(Ui.dp(c, 46));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    void setItems(String[] labels) {
        this.labels = labels;
        happy = -1;
        if (getWidth() > 0) {
            place();
            recover();
        }
        invalidate();
    }

    void celebrate(int i) {
        happy = i;
        if (coverCanvas != null && i < rects.length) {
            RectF q = rects[i];
            coverCanvas.drawCircle(q.centerX(), q.centerY(), q.width(), clearFill());
        }
        invalidate();
    }

    private Paint clearFill() {
        Paint p = new Paint(eraser);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        if (cover != null) cover.recycle();
        cover = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        coverCanvas = new Canvas(cover);
        place();
        recover();
    }

    private void place() {
        float s = Ui.dp(getContext(), 84), pad = Ui.dp(getContext(), 16);
        rects = new RectF[labels.length];
        for (int i = 0; i < labels.length; i++) {
            RectF best = null;
            for (int tries = 0; tries < 200; tries++) {
                float x = pad + rnd.nextFloat() * Math.max(1, getWidth() - 2 * pad - s);
                float y = pad + rnd.nextFloat() * Math.max(1, getHeight() - 2 * pad - s);
                RectF c = new RectF(x, y, x + s, y + s);
                boolean clash = false;
                for (int j = 0; j < i; j++) {
                    RectF o = new RectF(rects[j]);
                    o.inset(-pad, -pad);
                    if (RectF.intersects(o, c)) clash = true;
                }
                best = c;
                if (!clash) break;
            }
            rects[i] = best;
        }
    }

    private void recover() {
        if (coverCanvas == null) return;
        coverCanvas.drawColor(0xFFF0DFB8);
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
        float gw = Ui.dp(getContext(), 7), gh = Ui.dp(getContext(), 3.5f);
        int n = (int) (getWidth() * getHeight() / (gw * gh) * 0.9f);
        for (int i = 0; i < n; i++) {
            g.setColor(RICE[rnd.nextInt(RICE.length)]);
            float x = rnd.nextFloat() * getWidth(), y = rnd.nextFloat() * getHeight();
            coverCanvas.save();
            coverCanvas.rotate(rnd.nextInt(180), x, y);
            coverCanvas.drawOval(x - gw / 2, y - gh / 2, x + gw / 2, y + gh / 2, g);
            coverCanvas.restore();
        }
        invalidate();
    }

    /** Share of the item that has been dug free. */
    private float uncovered(int i) {
        RectF q = rects[i];
        int clear = 0, total = 0;
        for (int gy = 0; gy < 8; gy++)
            for (int gx = 0; gx < 8; gx++) {
                int x = (int) (q.left + (gx + 0.5f) * q.width() / 8), y = (int) (q.top + (gy + 0.5f) * q.height() / 8);
                if (x < 0 || y < 0 || x >= cover.getWidth() || y >= cover.getHeight()) continue;
                total++;
                if ((cover.getPixel(x, y) >>> 24) < 40) clear++;
            }
        return total == 0 ? 0 : clear / (float) total;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (coverCanvas == null) return true;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = lastX = x;
                downY = lastY = y;
                coverCanvas.drawPoint(x, y, eraser);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                coverCanvas.drawLine(lastX, lastY, x, y, eraser);
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (Math.hypot(x - downX, y - downY) < Ui.dp(getContext(), 14)) {
                    for (int i = 0; i < rects.length; i++)
                        if (rects[i].contains(x, y) && uncovered(i) >= 0.3f && pick != null) {
                            pick.picked(i);
                            break;
                        }
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float rad = Ui.dp(getContext(), 18);
        paint.setColor(0xFFE1F5FE);
        r.set(0, 0, getWidth(), getHeight());
        c.drawRoundRect(r, rad, rad, paint);
        for (int i = 0; i < rects.length; i++) {
            RectF q = rects[i];
            paint.setColor(i == happy ? 0xFF43A047 : Data.BRIGHT[i % Data.BRIGHT.length]);
            c.drawRoundRect(q, rad / 1.5f, rad / 1.5f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(q.height() * 0.6f);
            Paint.FontMetrics fm = paint.getFontMetrics();
            c.drawText(labels[i], q.centerX(), q.centerY() - (fm.ascent + fm.descent) / 2, paint);
        }
        if (cover != null) c.drawBitmap(cover, 0, 0, null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 6));
        paint.setColor(0xFF5D4037);
        c.drawRoundRect(r, rad, rad, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
