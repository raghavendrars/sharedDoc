package com.kidslearn.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/**
 * The parent's rules for the games (Blockhaven Mini and Turbo Circuit Mini share one budget): on/off, minutes per
 * weekday / weekend day, allowed hours, minutes of learning first, and extra time given with the PIN.
 */
final class GameTime {
    interface Maker {
        View make();
    }

    private GameTime() {
    }

    static boolean weekend() {
        int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY;
    }

    static int minutesToday(MainActivity a) {
        return (weekend() ? a.prefs.getInt("game_we", 60) : a.prefs.getInt("game_wd", 30)) + a.stats.gameBonus(MainActivity.today());
    }

    static long remainingMs(MainActivity a) {
        return minutesToday(a) * 60000L - a.liveUsed(MainActivity.GAME);
    }

    static String hourName(int h) {
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (h < 12 || h == 24 ? " AM" : " PM");
    }

    /** A parent gave time with the PIN: for that many minutes the hours and learn-first rules are skipped. */
    static boolean parentPass(MainActivity a) {
        return System.currentTimeMillis() < a.prefs.getLong("game_pass_until", 0);
    }

    static boolean inHours(MainActivity a) {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= a.prefs.getInt("game_from", 7) && h < a.prefs.getInt("game_to", 20);
    }

    private static boolean allowedNow(MainActivity a) {
        return a.prefs.getBoolean("game_on", true) && inHours(a);
    }

    /** Why the games can't be played right now, or null if they can. */
    static String why(MainActivity a) {
        if (parentPass(a)) return remainingMs(a) <= 0 ? "Game time is over for today. Come back tomorrow! 🌙" : null;
        if (!a.prefs.getBoolean("game_on", true)) return "Games are switched off by your parent.";
        int from = a.prefs.getInt("game_from", 7), to = a.prefs.getInt("game_to", 20);
        if (!inHours(a)) return "Games are open from " + hourName(from) + " to " + hourName(to) + ".";
        int learnFirst = a.prefs.getInt("game_learn_first", 10);
        long learned = a.usedToday() - a.stats.get(MainActivity.today(), MainActivity.GAME);
        if (learned < learnFirst * 60000L) {
            long left = (learnFirst * 60000L - learned + 59999) / 60000;
            return "Learn for " + left + " more minute" + (left == 1 ? "" : "s") + " first, then you can play! 📚";
        }
        if (remainingMs(a) <= 0) return "Game time is over for today. Come back tomorrow! 🌙";
        return null;
    }

    /** Game time left right now; outside the allowed hours only the parent's pass counts. */
    static long leftNow(MainActivity a) {
        long left = remainingMs(a);
        if (!allowedNow(a)) left = parentPass(a) ? Math.min(left, a.prefs.getLong("game_pass_until", 0) - System.currentTimeMillis()) : 0;
        return left;
    }

    /** Opens the game if the rules allow it, otherwise a friendly lock screen. */
    static Runnable open(final MainActivity a, final String title, final int color, final Maker maker) {
        return () -> {
            String why = why(a);
            if (why == null) a.show(maker.make(), false, color, MainActivity.GAME);
            else a.show(locked(a, why, title, color, maker), false, color, MainActivity.NONE);
        };
    }

    static View locked(final MainActivity a, String why, final String title, final int color, final Maker maker) {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color);
        page.addView(Ui.topBar(a, title));
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int p = Ui.dp(a, 24);
        box.setPadding(p, p, p, p);
        box.addView(Ui.text(a, "🎮", 90, Color.WHITE, false));
        TextView msg = Ui.text(a, why, 24, Color.WHITE, true);
        msg.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 6));
        box.addView(msg);
        box.addView(Ui.text(a, "Today: played " + Stats.mins(a.stats.get(MainActivity.today(), MainActivity.GAME)) + " of " + minutesToday(a) + " min",
                16, 0xFFE3F2FD, false));
        TextView learn = Ui.button(a, "📚 Go and learn", Color.WHITE, Ui.INK, a::home);
        LinearLayout.LayoutParams lp = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp.topMargin = Ui.dp(a, 22);
        box.addView(learn, lp);
        TextView more = Ui.button(a, "👪 Parent: more game time", 0xFFFFD54F, Ui.INK, () -> ParentScreen.askPin(a, "More game time", () ->
                new AlertDialog.Builder(a).setTitle("Give more game time today")
                        .setItems(new String[]{"Play 10 more minutes", "Play 20 more minutes", "Play 30 more minutes"}, (d, w) -> {
                            int m = (w + 1) * 10;
                            // top up today's minutes if needed; the pass lets the child play now even outside the hours
                            long left = Math.max(0, remainingMs(a));
                            a.stats.addGameBonus(MainActivity.today(), (int) Math.max(0, m - left / 60000));
                            a.prefs.edit().putLong("game_pass_until", System.currentTimeMillis() + m * 60000L).apply();
                            open(a, title, color, maker).run();
                        }).show()));
        LinearLayout.LayoutParams lp2 = Ui.lp(Ui.MATCH, Ui.WRAP, 0);
        lp2.topMargin = Ui.dp(a, 12);
        box.addView(more, lp2);
        page.addView(box, Ui.lp(Ui.MATCH, 0, 1));
        a.speaker.say(Grade.speakable(why));
        return page;
    }

    /**
     * Once a second: shows the time left in {@code timer} and, when it runs out or the allowed hours end, runs
     * {@code onStop} (to save) and swaps the game for the lock screen.
     */
    static void watch(final MainActivity a, final TextView timer, final Runnable onStop, final String title, final int color, final Maker maker) {
        long left = leftNow(a);
        if (left <= 0) {
            if (onStop != null) onStop.run();
            String why = why(a);
            a.show(locked(a, remainingMs(a) <= 0 ? "Time's up! Great playing today. 🌙" : why == null ? "Game time is over for now." : why,
                    title, color, maker), false, color, MainActivity.NONE);
            return;
        }
        long sec = left / 1000;
        timer.setText("⏱ " + (sec / 60) + ":" + String.format(Locale.ROOT, "%02d", sec % 60) + " left" + (sec < 60 ? "  — almost time to stop!" : ""));
        if (sec == 60) a.speaker.say("One minute left!");
        a.later(1000, () -> watch(a, timer, onStop, title, color, maker));
    }
}
