package com.kidslearn.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Blockhaven Mini: a 2D block-building world (a small cousin of the Blockhaven PC game). Parents decide when and how
 * long it can be played; the game locks itself when the time is used up.
 */
final class BlockGame {
    static final int COLOR = 0xFF33691E;

    private final MainActivity a;
    private BlockView world;
    private TextView timer;
    private final List<View> swatches = new ArrayList<>();

    private BlockGame(MainActivity a) {
        this.a = a;
    }

    static Runnable open(final MainActivity a) {
        return GameTime.open(a, "Blockhaven Mini", COLOR, () -> new BlockGame(a).make());
    }

    // ---- the game screen ----

    private View make() {
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(COLOR);
        page.addView(Ui.topBar(a, "Blockhaven Mini"));
        timer = Ui.text(a, "", 16, Color.WHITE, true);
        page.addView(timer);

        world = new BlockView(a);
        LinearLayout.LayoutParams wlp = Ui.lp(Ui.MATCH, 0, 1);
        wlp.setMargins(Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4));
        page.addView(world, wlp);

        LinearLayout tools = new LinearLayout(a);
        tools.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 6), 0);
        tools.addView(tool("◀", () -> world.scroll(-6)), Ui.lp(a, 0, Ui.dp(a, 46), 1, 2));
        tools.addView(tool("▶", () -> world.scroll(6)), Ui.lp(a, 0, Ui.dp(a, 46), 1, 2));
        tools.addView(tool("🔄 New world", () -> new AlertDialog.Builder(a).setTitle("Start a new world?")
                .setMessage("Your buildings in this world will be cleared.")
                .setPositiveButton("New world", (d, w) -> world.generate(new Random().nextLong()))
                .setNegativeButton("Keep it", null).show()), Ui.lp(a, 0, Ui.dp(a, 46), 2.2f, 2));
        page.addView(tools);

        LinearLayout pal = new LinearLayout(a);
        pal.setPadding(Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 12));
        int[] order = {BlockView.MOVE, BlockView.BREAK, BlockView.GRASS, BlockView.DIRT, BlockView.STONE, BlockView.LOG, BlockView.PLANK,
                BlockView.LEAVES, BlockView.BRICK, BlockView.GLASS, BlockView.SAND, BlockView.WATER, BlockView.FLOWER};
        for (final int b : order) {
            final Swatch s = new Swatch(a, b);
            s.setOnClickListener(v -> {
                world.tool = b;
                markSwatches(b);
                Ui.bounce(v);
                a.speaker.say(BlockView.name(b));
            });
            swatches.add(s);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(a, 56), Ui.dp(a, 64));
            lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
            pal.addView(s, lp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.addView(pal);
        page.addView(hs);
        markSwatches(world.tool);

        a.onLeave(world::save);
        page.post(() -> GameTime.watch(a, timer, world::save, "Blockhaven Mini", COLOR, () -> new BlockGame(a).make()));
        a.speaker.say("Welcome to Blockhaven! Pick a block and tap to build.");
        return page;
    }

    private TextView tool(String label, Runnable r) {
        TextView t = Ui.text(a, label, 16, Ui.INK, true);
        t.setBackground(Ui.round(Color.WHITE, Ui.dp(a, 12)));
        Ui.click(t, r);
        return t;
    }

    private void markSwatches(int sel) {
        for (View v : swatches) ((Swatch) v).selected = ((Swatch) v).type == sel;
        for (View v : swatches) v.invalidate();
    }

    /** A palette button showing the block. */
    private static final class Swatch extends View {
        final int type;
        boolean selected;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Swatch(Context c, int type) {
            super(c);
            this.type = type;
            p.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h) - Ui.dp(getContext(), 16);
            p.setColor(selected ? 0xFFFFD54F : 0xFFFFFFFF);
            c.drawRoundRect(0, 0, w, h, Ui.dp(getContext(), 10), Ui.dp(getContext(), 10), p);
            float l = (w - s) / 2, t = Ui.dp(getContext(), 6);
            if (type == BlockView.MOVE || type == BlockView.BREAK) {
                p.setTextSize(s * 0.8f);
                c.drawText(type == BlockView.MOVE ? "✋" : "⛏️", w / 2, t + s * 0.8f, p);
            } else BlockView.drawBlock(c, p, type, l, t, s, 3, 7);
        }
    }
}

/** The world: a grid of blocks that scrolls sideways. Tap or drag to build; the pick removes blocks; the hand scrolls. */
final class BlockView extends View {
    static final int AIR = 0, GRASS = 1, DIRT = 2, STONE = 3, LOG = 4, LEAVES = 5, SAND = 6, WATER = 7, BRICK = 8, GLASS = 9,
            PLANK = 10, FLOWER = 11, BEDROCK = 12, MOVE = 100, BREAK = 101;
    static final int COLS = 80, ROWS = 22;

    static String name(int b) {
        switch (b) {
            case GRASS: return "Grass";
            case DIRT: return "Dirt";
            case STONE: return "Stone";
            case LOG: return "Wood log";
            case LEAVES: return "Leaves";
            case SAND: return "Sand";
            case WATER: return "Water";
            case BRICK: return "Brick";
            case GLASS: return "Glass";
            case PLANK: return "Wooden planks";
            case FLOWER: return "Flower";
            case MOVE: return "Move around";
            case BREAK: return "Pick, to remove blocks";
            default: return "Block";
        }
    }

    private final byte[] cells = new byte[COLS * ROWS];
    private final MainActivity a;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final File file;
    int tool = GRASS;
    private float offset; // first visible column (can be fractional)
    private float lastX;
    private int edits;

    BlockView(MainActivity a) {
        super(a);
        this.a = a;
        this.file = new File(a.getFilesDir(), "blockhaven-mini.dat");
        if (!load()) generate(42);
    }

    private float cell() {
        return getHeight() / (float) ROWS;
    }

    private int visibleCols() {
        return (int) Math.ceil(getWidth() / Math.max(1f, cell())) + 1;
    }

    void scroll(float cols) {
        offset = Math.max(0, Math.min(COLS - getWidth() / Math.max(1f, cell()), offset + cols));
        invalidate();
    }

    private int at(int x, int y) {
        return x < 0 || y < 0 || x >= COLS || y >= ROWS ? AIR : cells[y * COLS + x];
    }

    private void set(int x, int y, int b) {
        if (x >= 0 && y >= 0 && x < COLS && y < ROWS) cells[y * COLS + x] = (byte) b;
    }

    void generate(long seed) {
        Random r = new Random(seed);
        java.util.Arrays.fill(cells, (byte) AIR);
        double p1 = r.nextDouble() * 6, p2 = r.nextDouble() * 6;
        int[] top = new int[COLS];
        for (int x = 0; x < COLS; x++) top[x] = (int) Math.round(13 + 2.2 * Math.sin(x / 7.0 + p1) + 1.3 * Math.sin(x / 3.1 + p2));
        int pond = 25 + r.nextInt(30), water = 0;
        for (int x = pond; x < pond + 7; x++) {
            top[x] += 2 + (x > pond && x < pond + 6 ? 1 : 0);
            water = Math.max(water, top[x]);
        }
        int waterLevel = Math.min(top[pond - 1], top[pond + 7]);
        for (int x = 0; x < COLS; x++) {
            for (int y = top[x]; y < ROWS; y++) {
                int b = y == ROWS - 1 ? BEDROCK : y == top[x] ? GRASS : y <= top[x] + 3 ? DIRT : STONE;
                set(x, y, b);
            }
        }
        for (int x = pond - 1; x <= pond + 7; x++) {
            set(x, top[x], SAND);
            for (int y = waterLevel; y < top[x]; y++) set(x, y, WATER);
        }
        for (int x = 3; x < COLS - 3; x++) {
            if (x >= pond - 3 && x <= pond + 10) continue;
            int t = top[x];
            if (r.nextInt(9) == 0 && at(x, t) == GRASS) {
                int h = 3 + r.nextInt(2);
                for (int k = 1; k <= h; k++) set(x, t - k, LOG);
                for (int dx = -2; dx <= 2; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (Math.abs(dx) + Math.abs(dy) < 3 && at(x + dx, t - h + dy) == AIR) set(x + dx, t - h + dy, LEAVES);
                set(x, t - h - 2, LEAVES);
                x += 4;
            } else if (r.nextInt(6) == 0 && at(x, t) == GRASS && at(x, t - 1) == AIR) set(x, t - 1, FLOWER);
        }
        offset = 0;
        invalidate();
        save();
    }

    private boolean load() {
        if (!file.exists() || file.length() != cells.length) return false;
        try (FileInputStream in = new FileInputStream(file)) {
            int n = 0, r;
            while (n < cells.length && (r = in.read(cells, n, cells.length - n)) > 0) n += r;
            return n == cells.length;
        } catch (Exception e) {
            return false;
        }
    }

    void save() {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(cells);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float c = cell();
        if (c <= 0) return true;
        int act = e.getActionMasked();
        if (act == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            lastX = e.getX();
        }
        if (tool == MOVE) {
            if (act == MotionEvent.ACTION_MOVE) {
                scroll((lastX - e.getX()) / c);
                lastX = e.getX();
            }
            return true;
        }
        if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_MOVE) {
            int x = (int) Math.floor(e.getX() / c + offset), y = (int) Math.floor(e.getY() / c);
            int cur = at(x, y);
            if (tool == BREAK) {
                if (cur != AIR && cur != BEDROCK) set(x, y, AIR);
            } else if (cur == AIR || cur == WATER || (cur == FLOWER && tool != FLOWER)) {
                if (tool == FLOWER && at(x, y + 1) != GRASS && at(x, y + 1) != DIRT) return true;
                set(x, y, tool);
            }
            if (++edits % 20 == 0) save();
            invalidate();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas cv) {
        float c = cell(), w = getWidth(), h = getHeight();
        p.setColor(0xFF000000); // opaque, so the sky gradient isn't faded by the last block's alpha
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF81D4FA, 0xFFE1F5FE, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setColor(0xFFFFEE58);
        cv.drawCircle(w * 0.82f, c * 2.5f, c * 1.4f, p);
        p.setColor(0xEEFFFFFF);
        float cloud = (-offset * c * 0.3f) % (w + c * 8);
        for (int k = 0; k < 3; k++) {
            float cx = (cloud + k * w * 0.45f + w * 2) % (w + c * 6) - c * 3, cy = c * (2 + k % 2);
            cv.drawCircle(cx, cy, c * 0.9f, p);
            cv.drawCircle(cx + c, cy - c * 0.3f, c * 1.1f, p);
            cv.drawCircle(cx + c * 2, cy, c * 0.8f, p);
        }
        int first = (int) Math.floor(offset);
        for (int x = first; x < first + visibleCols() && x < COLS; x++)
            for (int y = 0; y < ROWS; y++) {
                int b = at(x, y);
                if (b == AIR) continue;
                drawBlock(cv, p, b, (x - offset) * c, y * c, c, x, y);
            }
    }

    /** Draws one block in a simple pixel-art style. */
    static void drawBlock(Canvas cv, Paint p, int b, float l, float t, float s, int x, int y) {
        int base, dot;
        switch (b) {
            case GRASS: base = 0xFF8D6E63; dot = 0xFF6D4C41; break;
            case DIRT: base = 0xFF8D6E63; dot = 0xFF6D4C41; break;
            case STONE: base = 0xFF9E9E9E; dot = 0xFF757575; break;
            case LOG: base = 0xFF795548; dot = 0xFF5D4037; break;
            case LEAVES: base = 0xFF43A047; dot = 0xFF2E7D32; break;
            case SAND: base = 0xFFFFE082; dot = 0xFFFFCA28; break;
            case WATER: base = 0xAA42A5F5; dot = 0x88BBDEFB; break;
            case BRICK: base = 0xFFC62828; dot = 0xFFEEEEEE; break;
            case GLASS: base = 0x44E1F5FE; dot = 0xCCFFFFFF; break;
            case PLANK: base = 0xFFD7A86E; dot = 0xFFA1887F; break;
            case BEDROCK: base = 0xFF424242; dot = 0xFF212121; break;
            default: base = 0; dot = 0; break;
        }
        if (b == FLOWER) {
            p.setColor(0xFF388E3C);
            cv.drawRect(l + s * 0.45f, t + s * 0.45f, l + s * 0.55f, t + s, p);
            p.setColor(0xFFE91E63);
            for (int k = 0; k < 5; k++) {
                double ang = k * Math.PI * 2 / 5;
                cv.drawCircle(l + s / 2 + (float) Math.cos(ang) * s * 0.16f, t + s * 0.35f + (float) Math.sin(ang) * s * 0.16f, s * 0.12f, p);
            }
            p.setColor(0xFFFFEB3B);
            cv.drawCircle(l + s / 2, t + s * 0.35f, s * 0.09f, p);
            return;
        }
        p.setColor(base);
        cv.drawRect(l, t, l + s, t + s, p);
        p.setColor(dot);
        float q = s / 8;
        if (b == BRICK) {
            for (int r = 0; r < 4; r++) {
                cv.drawRect(l, t + r * s / 4, l + s, t + r * s / 4 + q * 0.5f, p);
                float off = (r % 2) * s / 2;
                cv.drawRect(l + off + s / 4 - q * 0.25f, t + r * s / 4, l + off + s / 4 + q * 0.25f, t + (r + 1) * s / 4, p);
            }
        } else if (b == PLANK) {
            for (int r = 1; r < 4; r++) cv.drawRect(l, t + r * s / 4 - q * 0.25f, l + s, t + r * s / 4 + q * 0.25f, p);
        } else if (b == LOG) {
            for (int r = 1; r < 4; r++) cv.drawRect(l + r * s / 4 - q * 0.25f, t, l + r * s / 4 + q * 0.25f, t + s, p);
        } else if (b == GLASS) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(q * 0.6f);
            cv.drawRect(l + q * 0.3f, t + q * 0.3f, l + s - q * 0.3f, t + s - q * 0.3f, p);
            cv.drawLine(l + s * 0.25f, t + s * 0.6f, l + s * 0.6f, t + s * 0.25f, p);
            p.setStyle(Paint.Style.FILL);
        } else if (b == WATER) {
            cv.drawRect(l, t + s * 0.15f, l + s, t + s * 0.22f, p);
        } else {
            int hsh = x * 73856093 ^ y * 19349663 ^ b * 83492791;
            for (int k = 0; k < 6; k++) {
                hsh = hsh * 1103515245 + 12345;
                int px = (hsh >>> 8) & 7, py = (hsh >>> 16) & 7;
                cv.drawRect(l + px * q, t + py * q, l + (px + 1) * q, t + (py + 1) * q, p);
            }
        }
        if (b == GRASS) {
            p.setColor(0xFF66BB6A);
            cv.drawRect(l, t, l + s, t + s * 0.3f, p);
            p.setColor(0xFF43A047);
            for (int k = 0; k < 4; k++) cv.drawRect(l + k * s / 4, t + s * 0.3f, l + k * s / 4 + q, t + s * 0.42f, p);
        }
        p.setColor(0x22000000);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        cv.drawRect(l, t, l + s, t + s, p);
        p.setStyle(Paint.Style.FILL);
    }
}
