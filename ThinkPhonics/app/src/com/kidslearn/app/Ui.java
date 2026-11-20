package com.kidslearn.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

/** Small view helpers shared by every screen. */
final class Ui {
    static final int INK = 0xFF37474F;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static int darker(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    /** A chunky button face with a darker lip underneath. */
    static Drawable tile(Context c, int color, float radiusDp) {
        float r = dp(c, radiusDp);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{round(darker(color), r), round(color, r)});
        ld.setLayerInset(1, 0, 0, 0, dp(c, 6));
        return ld;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    static LinearLayout.LayoutParams lp(Context c, int w, int h, float weight, int marginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        int m = dp(c, marginDp);
        p.setMargins(m, m, m, m);
        return p;
    }

    static void bounce(View v) {
        v.animate().cancel();
        v.setScaleX(0.86f);
        v.setScaleY(0.86f);
        v.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new OvershootInterpolator(3f)).start();
    }

    static void shake(View v) {
        ObjectAnimator.ofFloat(v, "translationX", 0, -20, 20, -14, 14, -6, 6, 0).setDuration(420).start();
    }

    static void popIn(View v, long delay) {
        v.setScaleX(0.6f);
        v.setScaleY(0.6f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(320)
                .setInterpolator(new OvershootInterpolator(2f)).start();
    }

    static void click(final View v, final Runnable r) {
        v.setOnClickListener(x -> {
            bounce(v);
            r.run();
        });
    }

    static TextView button(Context c, String label, int bg, int fg, Runnable r) {
        TextView b = text(c, label, 22, fg, true);
        b.setBackground(tile(c, bg, 22));
        b.setPadding(dp(c, 20), dp(c, 14), dp(c, 20), dp(c, 20));
        click(b, r);
        return b;
    }

    static TextView circleButton(Context c, String label, float sp, Runnable r) {
        TextView b = text(c, label, sp, INK, true);
        b.setBackground(round(Color.WHITE, dp(c, 40)));
        click(b, r);
        return b;
    }

    static TextView starPill(MainActivity a) {
        TextView s = text(a, "⭐ " + a.stars(), 18, INK, true);
        s.setTag("stars");
        s.setBackground(round(Color.WHITE, dp(a, 20)));
        s.setPadding(dp(a, 14), dp(a, 6), dp(a, 14), dp(a, 6));
        return s;
    }

    /** Back button, screen title and the star counter. */
    static LinearLayout topBar(final MainActivity a, String title) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(a, 12);
        bar.setPadding(p, p, p, p);
        TextView back = circleButton(a, "←", 24, a::home);
        back.setContentDescription("Back");
        bar.addView(back, new LinearLayout.LayoutParams(dp(a, 48), dp(a, 48)));
        TextView t = text(a, title, title.length() > 18 ? 19 : 23, Color.WHITE, true);
        t.setMaxLines(2);
        bar.addView(t, lp(0, WRAP, 1));
        bar.addView(starPill(a));
        return bar;
    }
}

/** A short shower of paper bits over the whole screen; removes itself when done and never blocks touches. */
final class ConfettiView extends View {
    private static final int N = 80;
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N], spin = new float[N], size = new float[N];
    private final int[] col = new int[N];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private float t;
    private ValueAnimator anim;

    ConfettiView(Context c) {
        super(c);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        for (int i = 0; i < N; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = -rnd.nextFloat() * h * 0.35f;
            vx[i] = (rnd.nextFloat() - 0.5f) * w * 0.3f;
            vy[i] = h * (0.9f + rnd.nextFloat() * 0.8f);
            spin[i] = (rnd.nextFloat() - 0.5f) * 1400f;
            size[i] = Ui.dp(getContext(), 6 + rnd.nextInt(7));
            col[i] = Data.BRIGHT[rnd.nextInt(Data.BRIGHT.length)];
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1700);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(v -> {
            t = (float) v.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                post(() -> {
                    if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(ConfettiView.this);
                });
            }
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        int alpha = t < 0.75f ? 255 : Math.max(0, (int) (255 * (1f - t) / 0.25f));
        for (int i = 0; i < N; i++) {
            float px = x[i] + vx[i] * t, py = y[i] + vy[i] * t + 0.25f * getHeight() * t * t;
            paint.setColor(col[i]);
            paint.setAlpha(alpha);
            c.save();
            c.rotate(spin[i] * t, px, py);
            c.drawRect(px - size[i] / 2, py - size[i] / 3, px + size[i] / 2, py + size[i] / 3, paint);
            c.restore();
        }
    }
}
