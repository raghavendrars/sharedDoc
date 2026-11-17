package com.kidslearn.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/** Colouring sheets (tap to fill) plus crayon, rainbow and toy-car-track brushes. */
final class ColourScreen {
    static final int COLOR = 0xFF00897B;
    private static final String[] SHEETS = {"🏠 House", "🐟 Fish", "🌸 Flower", "🚀 Rocket", "⬜ Blank"};
    private static final int[] PALETTE = {0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA,
            0xFFEC407A, 0xFF8D6E63, 0xFF80DEEA, 0xFFFFFFFF};
    private static final String[] TOOLS = {"🪣 Fill", "🖍️ Crayon", "🌈 Rainbow", "🚗 Car"};

    private ColourScreen() {
    }

    static View build(final MainActivity a) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Colour & paint"));
        final ColourView art = new ColourView(a);

        LinearLayout sheets = new LinearLayout(a);
        sheets.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 4));
        for (int i = 0; i < SHEETS.length; i++) {
            final int k = i;
            sheets.addView(chip(a, SHEETS[i], () -> {
                art.setSheet(k);
                a.speaker.say(SHEETS[k].substring(SHEETS[k].indexOf(' ') + 1));
            }));
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(sheets);
        page.addView(hs);

        LinearLayout.LayoutParams alp = Ui.lp(Ui.MATCH, 0, 1);
        alp.setMargins(Ui.dp(a, 10), 0, Ui.dp(a, 10), Ui.dp(a, 6));
        page.addView(art, alp);

        LinearLayout palette = new LinearLayout(a);
        palette.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        final View[] dots = new View[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            final int k = i;
            View d = new View(a);
            dots[i] = d;
            d.setContentDescription("Colour " + (i + 1));
            d.setOnClickListener(v -> {
                art.color = PALETTE[k];
                mark(a, dots, k);
                Ui.bounce(v);
            });
            palette.addView(d, Ui.lp(a, 0, Ui.dp(a, 36), 1, 3));
        }
        mark(a, dots, 0);
        page.addView(palette);

        LinearLayout tools = new LinearLayout(a);
        tools.setPadding(Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 14));
        final TextView[] tb = new TextView[TOOLS.length];
        for (int i = 0; i < TOOLS.length; i++) {
            final int k = i;
            tb[i] = tool(a, TOOLS[i], () -> {
                art.mode = k;
                for (int j = 0; j < tb.length; j++) tb[j].setBackground(Ui.round(j == k ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 12)));
            });
            tools.addView(tb[i], Ui.lp(a, 0, Ui.dp(a, 46), 1.15f, 2));
        }
        tb[0].setBackground(Ui.round(0xFFFFD54F, Ui.dp(a, 12)));
        tools.addView(tool(a, "↶", art::undo), Ui.lp(a, 0, Ui.dp(a, 46), 0.6f, 2));
        tools.addView(tool(a, "🧽", art::reset), Ui.lp(a, 0, Ui.dp(a, 46), 0.6f, 2));
        page.addView(tools);
        return page;
    }

    private static TextView chip(MainActivity a, String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 18)));
        t.setPadding(Ui.dp(a, 14), Ui.dp(a, 7), Ui.dp(a, 14), Ui.dp(a, 7));
        Ui.click(t, r);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
        lp.setMargins(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
        t.setLayoutParams(lp);
        return t;
    }

    private static TextView tool(MainActivity a, String label, Runnable r) {
        TextView t = Ui.text(a, label, 13, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 12)));
        Ui.click(t, r);
        return t;
    }

    private static void mark(MainActivity a, View[] dots, int sel) {
        for (int i = 0; i < dots.length; i++) {
            GradientDrawable g = Ui.round(PALETTE[i], Ui.dp(a, 18));
            g.setStroke(Ui.dp(a, i == sel ? 4 : 1), i == sel ? Ui.INK : 0x55000000);
            dots[i].setBackground(g);
        }
    }
}

final class ColourView extends View {
    static final int FILL = 0, CRAYON = 1, RAINBOW = 2, CAR = 3;
    int mode = FILL, color = 0xFFE53935;
    private int sheet;
    private Bitmap bmp;
    private Canvas cv;
    private final ArrayList<Bitmap> undo = new ArrayList<>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), brush = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private float lx, ly, dist, hue;

    ColourView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(Color.BLACK);
        line.setStrokeWidth(Ui.dp(c, 4));
        brush.setStyle(Paint.Style.STROKE);
        brush.setStrokeCap(Paint.Cap.ROUND);
        brush.setStrokeJoin(Paint.Join.ROUND);
    }

    void setSheet(int s) {
        sheet = s;
        reset();
    }

    void reset() {
        if (cv == null) return;
        push();
        drawSheet();
        invalidate();
    }

    void undo() {
        if (undo.isEmpty() || cv == null) return;
        Bitmap b = undo.remove(undo.size() - 1);
        cv.drawBitmap(b, 0, 0, null);
        b.recycle();
        invalidate();
    }

    private void push() {
        if (bmp == null) return;
        if (undo.size() >= 6) undo.remove(0).recycle();
        undo.add(bmp.copy(Bitmap.Config.ARGB_8888, false));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        if (w <= 0 || h <= 0) return;
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cv = new Canvas(bmp);
        drawSheet();
    }

    @Override
    protected void onDetachedFromWindow() {
        for (Bitmap b : undo) b.recycle();
        undo.clear();
        super.onDetachedFromWindow();
    }

    private void drawSheet() {
        cv.drawColor(Color.WHITE);
        float w = getWidth(), h = getHeight(), s = Math.min(w, h) * 0.94f, ox = (w - s) / 2, oy = (h - s) / 2;
        switch (sheet) {
            case 0: // house
                rect(ox, oy, s, 0.2f, 0.45f, 0.8f, 0.86f);
                poly(ox, oy, s, 0.12f, 0.47f, 0.5f, 0.17f, 0.88f, 0.47f);
                rect(ox, oy, s, 0.43f, 0.62f, 0.57f, 0.86f);
                rect(ox, oy, s, 0.26f, 0.53f, 0.38f, 0.65f);
                rect(ox, oy, s, 0.62f, 0.53f, 0.74f, 0.65f);
                circle(ox, oy, s, 0.86f, 0.12f, 0.08f);
                cv.drawLine(ox, oy + s * 0.86f, ox + s, oy + s * 0.86f, line);
                circle(ox, oy, s, 0.15f, 0.12f, 0.06f);
                circle(ox, oy, s, 0.23f, 0.1f, 0.07f);
                break;
            case 1: // fish
                oval(ox, oy, s, 0.14f, 0.3f, 0.7f, 0.7f);
                poly(ox, oy, s, 0.68f, 0.5f, 0.9f, 0.3f, 0.9f, 0.7f);
                circle(ox, oy, s, 0.27f, 0.45f, 0.045f);
                poly(ox, oy, s, 0.38f, 0.31f, 0.47f, 0.17f, 0.56f, 0.33f);
                cv.drawArc(ox + s * 0.36f, oy + s * 0.33f, ox + s * 0.52f, oy + s * 0.67f, -60, 120, false, line);
                circle(ox, oy, s, 0.84f, 0.18f, 0.035f);
                circle(ox, oy, s, 0.9f, 0.1f, 0.025f);
                cv.drawLine(ox, oy + s * 0.88f, ox + s, oy + s * 0.88f, line);
                break;
            case 2: // flower
                for (int i = 0; i < 6; i++) {
                    double ang = i * Math.PI / 3;
                    circle(ox, oy, s, 0.5f + (float) Math.cos(ang) * 0.15f, 0.33f + (float) Math.sin(ang) * 0.15f, 0.09f);
                }
                circle(ox, oy, s, 0.5f, 0.33f, 0.08f);
                rect(ox, oy, s, 0.48f, 0.5f, 0.52f, 0.9f);
                oval(ox, oy, s, 0.3f, 0.62f, 0.47f, 0.72f);
                oval(ox, oy, s, 0.53f, 0.7f, 0.7f, 0.8f);
                cv.drawLine(ox, oy + s * 0.9f, ox + s, oy + s * 0.9f, line);
                break;
            case 3: // rocket
                path.reset();
                path.moveTo(ox + s * 0.5f, oy + s * 0.08f);
                path.quadTo(ox + s * 0.66f, oy + s * 0.25f, ox + s * 0.63f, oy + s * 0.68f);
                path.lineTo(ox + s * 0.37f, oy + s * 0.68f);
                path.quadTo(ox + s * 0.34f, oy + s * 0.25f, ox + s * 0.5f, oy + s * 0.08f);
                path.close();
                cv.drawPath(path, line);
                circle(ox, oy, s, 0.5f, 0.36f, 0.06f);
                poly(ox, oy, s, 0.37f, 0.52f, 0.25f, 0.72f, 0.37f, 0.68f);
                poly(ox, oy, s, 0.63f, 0.52f, 0.75f, 0.72f, 0.63f, 0.68f);
                poly(ox, oy, s, 0.42f, 0.68f, 0.5f, 0.9f, 0.58f, 0.68f);
                star(ox, oy, s, 0.15f, 0.2f);
                star(ox, oy, s, 0.85f, 0.35f);
                star(ox, oy, s, 0.2f, 0.85f);
                break;
            default:
                break;
        }
    }

    private void rect(float ox, float oy, float s, float l, float t, float rr, float b) {
        cv.drawRect(ox + s * l, oy + s * t, ox + s * rr, oy + s * b, line);
    }

    private void oval(float ox, float oy, float s, float l, float t, float rr, float b) {
        r.set(ox + s * l, oy + s * t, ox + s * rr, oy + s * b);
        cv.drawOval(r, line);
    }

    private void circle(float ox, float oy, float s, float x, float y, float rad) {
        cv.drawCircle(ox + s * x, oy + s * y, s * rad, line);
    }

    private void poly(float ox, float oy, float s, float... p) {
        path.reset();
        path.moveTo(ox + s * p[0], oy + s * p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(ox + s * p[i], oy + s * p[i + 1]);
        path.close();
        cv.drawPath(path, line);
    }

    private void star(float ox, float oy, float s, float x, float y) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            double ang = -Math.PI / 2 + i * Math.PI / 5;
            float rr = s * (i % 2 == 0 ? 0.06f : 0.025f);
            float px = ox + s * x + (float) Math.cos(ang) * rr, py = oy + s * y + (float) Math.sin(ang) * rr;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        cv.drawPath(path, line);
    }

    // ---- painting ----

    private static boolean dark(int c) {
        return 0.3f * Color.red(c) + 0.59f * Color.green(c) + 0.11f * Color.blue(c) < 80;
    }

    private static boolean near(int c, int d) {
        return Math.abs(Color.red(c) - Color.red(d)) < 90 && Math.abs(Color.green(c) - Color.green(d)) < 90 && Math.abs(Color.blue(c) - Color.blue(d)) < 90;
    }

    /** Scanline flood fill of the area under (x, y), stopping at the dark outlines. */
    private void floodFill(int x, int y, int newColor) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        int seed = px[y * w + x];
        if (dark(seed)) return;
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[w * h / 4 + 16];
        int sp = 0;
        stack[sp++] = y * w + x;
        while (sp > 0) {
            int p = stack[--sp];
            int py = p / w, lx0 = p % w;
            while (lx0 > 0 && !seen[py * w + lx0 - 1] && !dark(px[py * w + lx0 - 1]) && near(px[py * w + lx0 - 1], seed)) lx0--;
            boolean up = false, down = false;
            for (int cx = lx0; cx < w; cx++) {
                int i = py * w + cx;
                if (seen[i] || dark(px[i]) || !near(px[i], seed)) break;
                px[i] = newColor;
                seen[i] = true;
                if (py > 0) {
                    int j = i - w;
                    boolean ok = !seen[j] && !dark(px[j]) && near(px[j], seed);
                    if (ok && !up && sp < stack.length) {
                        stack[sp++] = j;
                        up = true;
                    } else if (!ok) up = false;
                }
                if (py < h - 1) {
                    int j = i + w;
                    boolean ok = !seen[j] && !dark(px[j]) && near(px[j], seed);
                    if (ok && !down && sp < stack.length) {
                        stack[sp++] = j;
                        down = true;
                    } else if (!ok) down = false;
                }
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h);
    }

    private void stroke(float x0, float y0, float x1, float y1) {
        if (mode == CAR) {
            float dx = x1 - x0, dy = y1 - y0, len = (float) Math.hypot(dx, dy);
            if (len < 1) return;
            float nx = -dy / len, ny = dx / len, off = Ui.dp(getContext(), 10), step = Ui.dp(getContext(), 9);
            fill.setColor(color);
            float t = (step - dist % step);
            while (t <= len) {
                float px = x0 + dx * t / len, py = y0 + dy * t / len;
                for (int side = -1; side <= 1; side += 2) {
                    float cx = px + nx * off * side, cy = py + ny * off * side;
                    cv.save();
                    cv.rotate((float) Math.toDegrees(Math.atan2(dy, dx)), cx, cy);
                    cv.drawRect(cx - Ui.dp(getContext(), 2), cy - Ui.dp(getContext(), 4), cx + Ui.dp(getContext(), 2), cy + Ui.dp(getContext(), 4), fill);
                    cv.restore();
                }
                t += step;
            }
            dist += len;
            return;
        }
        brush.setStrokeWidth(Ui.dp(getContext(), mode == RAINBOW ? 14 : 10));
        if (mode == RAINBOW) {
            hue = (hue + (float) Math.hypot(x1 - x0, y1 - y0) * 0.6f) % 360;
            brush.setColor(Color.HSVToColor(new float[]{hue, 0.85f, 1f}));
        } else brush.setColor(color);
        cv.drawLine(x0, y0, x1, y1, brush);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (cv == null) return true;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                push();
                if (mode == FILL) {
                    floodFill((int) x, (int) y, color);
                    invalidate();
                    return true;
                }
                lx = x;
                ly = y;
                dist = 0;
                stroke(x, y, x + 0.1f, y + 0.1f);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mode != FILL) {
                    stroke(lx, ly, x, y);
                    lx = x;
                    ly = y;
                    invalidate();
                }
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bmp != null) c.drawBitmap(bmp, 0, 0, null);
    }
}
