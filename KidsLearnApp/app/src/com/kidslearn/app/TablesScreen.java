package com.kidslearn.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Times tables: pick a number, see and hear the table line by line, then practise it. */
final class TablesScreen {
    static final int COLOR = 0xFF0097A7;

    private final MainActivity a;
    private int n = 2, run, line = -1;
    private String speaking;
    private LinearLayout rows;
    private TextView status;
    private final List<TextView> chips = new ArrayList<>();

    private TablesScreen(MainActivity a) {
        this.a = a;
    }

    /** Highest table for the child's class. */
    static int maxTable(int grade) {
        return grade <= 1 ? 5 : grade == 2 ? 10 : grade == 3 ? 12 : grade == 4 ? 15 : 20;
    }

    static Runnable open(final MainActivity a) {
        return () -> a.show(new TablesScreen(a).make(), false, COLOR, MainActivity.LEARN);
    }

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Times tables"));

        LinearLayout row = new LinearLayout(a);
        row.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), Ui.dp(a, 4));
        for (int i = 1; i <= maxTable(a.grade()); i++) {
            final int k = i;
            TextView c = Ui.text(a, String.valueOf(i), 22, Ui.INK, true);
            c.setMinWidth(Ui.dp(a, 48));
            c.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4));
            Ui.click(c, () -> pick(k));
            chips.add(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
            lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
            row.addView(c, lp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(row);
        page.addView(hs);

        rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 22)));
        int p = Ui.dp(a, 10);
        rows.setPadding(p, p, p, p);
        ScrollView sv = new ScrollView(a);
        sv.addView(rows);
        LinearLayout.LayoutParams slp = Ui.lp(Ui.MATCH, 0, 1);
        slp.setMargins(Ui.dp(a, 14), Ui.dp(a, 4), Ui.dp(a, 14), Ui.dp(a, 4));
        page.addView(sv, slp);

        status = Ui.text(a, "", 16, Color.WHITE, true);
        page.addView(status);
        LinearLayout btns = new LinearLayout(a);
        btns.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 14));
        btns.addView(tool("▶ Say the table", () -> recite(0, ++run)), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
        btns.addView(tool("🏆 Practise", () -> QuizScreen.open(a, QuizScreen.TABLE, n).run()), Ui.lp(a, 0, Ui.dp(a, 50), 1, 3));
        page.addView(btns);

        final Speaker.Track tracker = new Speaker.Track() {
            @Override
            public void range(String id, int start, int end) {
            }

            @Override
            public void done(String id) {
                if (id.equals(speaking)) {
                    speaking = null;
                    final int r = run, next = line + 1;
                    a.later(250, () -> recite(next, r));
                }
            }
        };
        page.post(() -> {
            a.speaker.track = tracker;
            pick(2);
        });
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 14)));
        Ui.click(t, r);
        return t;
    }

    private void pick(int k) {
        run++;
        a.speaker.stop();
        n = k;
        for (int i = 0; i < chips.size(); i++) chips.get(i).setBackground(Ui.round(i + 1 == k ? 0xFFFFD54F : Color.WHITE, Ui.dp(a, 14)));
        rows.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            final int m = i;
            TextView t = Ui.text(a, n + "  ×  " + i + "  =  " + (n * i), 26, Data.BRIGHT[i % Data.BRIGHT.length], true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
            Ui.click(t, () -> {
                run++;
                mark(m - 1);
                a.speaker.say(n + " times " + m + " is " + (n * m));
            });
            rows.addView(t);
        }
        status.setText("The table of " + n);
        a.speaker.say("The table of " + n);
    }

    private void mark(int k) {
        for (int i = 0; i < rows.getChildCount(); i++) rows.getChildAt(i).setBackground(i == k ? Ui.round(0xFFFFF59D, Ui.dp(a, 10)) : null);
    }

    private void recite(final int i, final int r) {
        if (r != run) return;
        if (i >= 10) {
            mark(-1);
            line = -1;
            status.setText("Well done! Now press 🏆 Practise");
            return;
        }
        line = i;
        mark(i);
        speaking = a.speaker.sayTracked(n + " times " + (i + 1) + " is " + (n * (i + 1)));
        if (speaking == null) a.later(1300, () -> recite(i + 1, r));
    }
}
