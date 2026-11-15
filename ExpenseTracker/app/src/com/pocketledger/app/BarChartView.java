package com.pocketledger.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Two horizontal bars (debit and credit) that grow from zero when a report is generated. */
public class BarChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG), track = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long debit, credit;
    private float progress;
    private ValueAnimator animator;

    public BarChartView(Context c, AttributeSet attrs) {
        super(c, attrs);
        track.setColor(0xFFEDF0F4);
        label.setColor(0xFF667085);
        label.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
    }

    public void animateTo(long debit, long credit) {
        this.debit = debit;
        this.credit = credit;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setStartDelay(150);
        animator.setInterpolator(new OvershootInterpolator(0.8f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        progress = 0;
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth(), rowH = getHeight() / 2f, barH = 16 * d, r = barH / 2;
        long max = Math.max(1, Math.max(debit, credit));
        drawRow(canvas, 0, "Debit", debit / (float) max, 0xFFC62828, w, rowH, barH, r, d);
        drawRow(canvas, rowH, "Credit", credit / (float) max, 0xFF2E7D32, w, rowH, barH, r, d);
    }

    private void drawRow(Canvas canvas, float top, String name, float share, int color, float w, float rowH, float barH, float r, float d) {
        canvas.drawText(name, 0, top + 14 * d, label);
        float y = top + 20 * d;
        rect.set(0, y, w, y + barH);
        canvas.drawRoundRect(rect, r, r, track);
        float len = Math.max(0, Math.min(w, w * share * progress));
        if (len > 0 && share > 0) {
            rect.set(0, y, Math.max(len, barH), y + barH);
            bar.setColor(color);
            canvas.drawRoundRect(rect, r, r, bar);
        }
    }
}
