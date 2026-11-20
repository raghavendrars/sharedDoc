package com.kidslearn.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Turbo Circuit Mini race: a top-down view that turns with the player's car, three computer drivers, three laps.
 * The track shapes and car numbers follow the Turbo Circuit PC game, scaled down for small hands.
 */
final class RaceView extends View {
    interface Done {
        void on(int place, float time);
    }

    static final int LAPS = 3;
    private static final float SPEED_SCALE = 0.7f, ACC_SCALE = 0.8f;

    static final class Car {
        int model, col;
        String name;
        boolean player, finished, braking, boosting;
        float maxV, acc, grip, brake, skill, lane;
        float x, y, vx, vy, h, steer, lat, finishT, tank, stuck, spin;
        int idx, lap = -1;

        float speed() {
            return (float) Math.hypot(vx, vy);
        }
    }

    private final MainActivity a;
    private final int track;
    private final Done done;
    // track centre line
    private int n;
    private float[] px, py, nx, ny, curv;
    private float hw, wallDist, minX, minY, maxX, maxY;
    // scenery
    private float[] propX, propY, propS;
    private int[] propKind, propCol;
    // cars
    final Car[] cars = new Car[4];
    final Car me;
    volatile boolean left, right, brake, boost;
    private float countdown = 3.6f, clock, scale = 20f, camH, endWait = -1;
    private boolean running = true, finishedSaid;
    final boolean steerHelp;
    private int lastLapSaid = -1;
    private long last;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mini = new Path();
    private final RectF rf = new RectF();
    private int[] vis = new int[0];
    private int miniSize;

    RaceView(MainActivity a, int track, int playerCar, Done done) {
        super(a);
        this.a = a;
        this.track = track;
        this.done = done;
        build(RaceGame.TRACK_PTS[track], RaceGame.TRACK_HW[track]);
        props(track * 7919 + 13);
        // grid: two computer cars in front, the player and one more behind
        float kid = a.grade() <= 1 ? 0.86f : a.grade() <= 3 ? 0.91f : 0.96f;
        steerHelp = a.grade() <= 2;
        int[] colors = {0xFFF5C828, 0xFF3CBE5A, 0xFFF08228};
        String[] names = {"Rex", "Nova", "Blaze"};
        int[] gridIdx = {n - 3, n - 3, n - 8, n - 8};
        float[] gridLat = {2.6f, -2.6f, 2.6f, -2.6f};
        int ai = 0;
        Car player = null;
        for (int k = 0; k < 4; k++) {
            Car c = new Car();
            if (k == 2) {
                c.player = true;
                c.model = playerCar;
                c.col = 0xFFE53935;
                c.name = "You";
                player = c;
            } else {
                c.model = Math.max(0, RaceGame.TRACK_AI_CAR[track] - (ai >= 2 ? 1 : 0));
                c.col = colors[ai];
                c.name = names[ai];
                c.skill = (RaceGame.TRACK_AI_SKILL[track] - 0.025f * ai) * kid;
                ai++;
            }
            c.maxV = RaceGame.CAR_MAXV[c.model] * SPEED_SCALE;
            c.acc = RaceGame.CAR_ACC[c.model] * ACC_SCALE;
            c.grip = RaceGame.CAR_GRIP[c.model];
            c.brake = RaceGame.CAR_BRAKE[c.model];
            c.tank = 2.5f;
            c.lane = gridLat[k] * 0.6f;
            place(c, gridIdx[k], gridLat[k]);
            c.lap = -1;
            cars[k] = c;
        }
        me = player;
        camH = me.h;
    }

    void stop() {
        running = false;
    }

    // ---- track ----

    private static float cr(float a, float b, float c, float d, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * (2f * b + (c - a) * t + (2f * a - 5f * b + 4f * c - d) * t2 + (3f * b - a - 3f * c + d) * t3);
    }

    private void build(float[] pts, float halfWidth) {
        int m = pts.length / 2;
        int cnt = m * 24;
        float[] dx = new float[cnt], dy = new float[cnt];
        for (int i = 0; i < m; i++)
            for (int s = 0; s < 24; s++) {
                int i0 = (i - 1 + m) % m, i2 = (i + 1) % m, i3 = (i + 2) % m;
                dx[i * 24 + s] = cr(pts[i0 * 2], pts[i * 2], pts[i2 * 2], pts[i3 * 2], s / 24f);
                dy[i * 24 + s] = cr(pts[i0 * 2 + 1], pts[i * 2 + 1], pts[i2 * 2 + 1], pts[i3 * 2 + 1], s / 24f);
            }
        float[] cum = new float[cnt + 1];
        for (int i = 0; i < cnt; i++) cum[i + 1] = cum[i] + (float) Math.hypot(dx[(i + 1) % cnt] - dx[i], dy[(i + 1) % cnt] - dy[i]);
        float length = cum[cnt];
        n = Math.round(length / 4f);
        float spacing = length / n;
        hw = halfWidth;
        wallDist = hw + 5f;
        px = new float[n];
        py = new float[n];
        nx = new float[n];
        ny = new float[n];
        curv = new float[n];
        int seg = 0;
        for (int k = 0; k < n; k++) {
            float d = (50f + k * spacing) % length;
            seg = 0;
            int lo = 0, hi = cnt - 1;
            while (lo <= hi) { // last cum[seg] <= d
                int mid = (lo + hi) >>> 1;
                if (cum[mid] <= d) {
                    seg = mid;
                    lo = mid + 1;
                } else hi = mid - 1;
            }
            float segLen = cum[seg + 1] - cum[seg];
            float f = segLen > 1e-5f ? (d - cum[seg]) / segLen : 0;
            px[k] = dx[seg] + (dx[(seg + 1) % cnt] - dx[seg]) * f;
            py[k] = dy[seg] + (dy[(seg + 1) % cnt] - dy[seg]) * f;
        }
        float[] tx = new float[n], ty = new float[n];
        for (int i = 0; i < n; i++) {
            float ex = px[(i + 1) % n] - px[(i - 1 + n) % n], ey = py[(i + 1) % n] - py[(i - 1 + n) % n];
            float l = (float) Math.hypot(ex, ey);
            tx[i] = ex / l;
            ty[i] = ey / l;
            nx[i] = ty[i];
            ny[i] = -tx[i];
        }
        float[] raw = new float[n];
        for (int i = 0; i < n; i++) {
            float ax = tx[(i - 1 + n) % n], ay = ty[(i - 1 + n) % n], bx = tx[(i + 1) % n], by = ty[(i + 1) % n];
            raw[i] = (float) Math.abs(Math.atan2(ax * by - ay * bx, ax * bx + ay * by)) / (2 * spacing);
        }
        for (int i = 0; i < n; i++) {
            float s = 0;
            for (int k = -2; k <= 2; k++) s += raw[(i + k + n) % n];
            curv[i] = s / 5f;
        }
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            minX = Math.min(minX, px[i]);
            maxX = Math.max(maxX, px[i]);
            minY = Math.min(minY, py[i]);
            maxY = Math.max(maxY, py[i]);
        }
    }

    private void props(int seed) {
        Random r = new Random(seed);
        int want = track == 3 ? 110 : 200;
        float[] xs = new float[want], ys = new float[want], ss = new float[want];
        int[] ks = new int[want], cs = new int[want];
        int got = 0;
        float clear = wallDist + (track == 3 ? 9f : 3f);
        for (int tries = 0; tries < 4000 && got < want; tries++) {
            float x = minX - 70 + r.nextFloat() * (maxX - minX + 140), y = minY - 70 + r.nextFloat() * (maxY - minY + 140);
            float best = Float.MAX_VALUE;
            for (int i = 0; i < n; i += 2) best = Math.min(best, (px[i] - x) * (px[i] - x) + (py[i] - y) * (py[i] - y));
            best = (float) Math.sqrt(best);
            float size = track == 3 ? 4 + r.nextFloat() * 5 : 1.6f + r.nextFloat() * 2.2f;
            if (best < clear + size || best > clear + 55) continue;
            xs[got] = x;
            ys[got] = y;
            ss[got] = size;
            ks[got] = r.nextInt(4);
            int[] neon = {0xFF00E5FF, 0xFFFF4081, 0xFFFFEA00, 0xFF76FF03};
            cs[got] = neon[r.nextInt(neon.length)];
            got++;
        }
        propX = java.util.Arrays.copyOf(xs, got);
        propY = java.util.Arrays.copyOf(ys, got);
        propS = java.util.Arrays.copyOf(ss, got);
        propKind = java.util.Arrays.copyOf(ks, got);
        propCol = java.util.Arrays.copyOf(cs, got);
    }

    private void place(Car c, int idx, float lat) {
        idx = ((idx % n) + n) % n;
        c.idx = idx;
        c.x = px[idx] + nx[idx] * lat;
        c.y = py[idx] + ny[idx] * lat;
        c.h = (float) Math.atan2(-ny[idx], nx[idx]); // heading along the track: forward = (sin h, cos h) = tangent
        c.vx = c.vy = 0;
        c.lat = lat;
        c.steer = 0;
        c.stuck = 0;
    }

    private static float wrap(float x) {
        while (x > Math.PI) x -= 2 * Math.PI;
        while (x < -Math.PI) x += 2 * Math.PI;
        return x;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    private void locate(Car c) {
        int best = c.idx;
        float bd = Float.MAX_VALUE;
        for (int k = -4; k <= 8; k++) {
            int i = ((c.idx + k) % n + n) % n;
            float d = (px[i] - c.x) * (px[i] - c.x) + (py[i] - c.y) * (py[i] - c.y);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        if (c.idx > n * 3 / 4 && best < n / 4) c.lap++;
        else if (c.idx < n / 4 && best > n * 3 / 4) c.lap--;
        c.idx = best;
    }

    float progress(Car c) {
        return c.lap * n + c.idx;
    }

    // ---- physics (after Car.Update in the PC game) ----

    private void drive(Car c, float dt, float thr, float steerIn, boolean boostIn) {
        c.steer += (steerIn - c.steer) * Math.min(1, dt * 8);
        float fx = (float) Math.sin(c.h), fy = (float) Math.cos(c.h);
        float vF0 = c.vx * fx + c.vy * fy;
        boolean off = Math.abs(c.lat) > hw + 1.2f;
        float grip = c.grip * (off ? 0.55f : 1f);
        float maxYaw = Math.min(2.2f, grip / Math.max(Math.abs(vF0), 4f));
        c.h = wrap(c.h + c.steer * maxYaw * dt * clamp(vF0 / 4f, -1f, 1f));
        fx = (float) Math.sin(c.h);
        fy = (float) Math.cos(c.h);
        float lx = (float) Math.cos(c.h), ly = (float) -Math.sin(c.h);
        float vF = c.vx * fx + c.vy * fy, vL = c.vx * lx + c.vy * ly;

        c.boosting = boostIn && c.tank > 0 && thr > 0;
        if (c.boosting) c.tank = Math.max(0, c.tank - dt);
        else c.tank = Math.min(2.5f, c.tank + dt * 0.12f);
        float maxV = c.maxV * (c.boosting ? 1.22f : 1f), acc = c.acc * (c.boosting ? 1.7f : 1f);
        c.braking = false;
        if (thr > 0) {
            float r = vF / maxV;
            vF += acc * thr * Math.max(0, 1 - r * r) * dt;
        } else if (thr < 0) {
            if (vF > 1f) {
                vF = Math.max(0, vF + c.brake * thr * dt);
                c.braking = true;
            } else vF = Math.max(-7f, vF + acc * 0.6f * thr * dt);
        } else {
            float dec = (1.5f + 0.03f * Math.abs(vF)) * dt;
            vF = Math.abs(vF) <= dec ? 0 : vF - Math.signum(vF) * dec;
        }
        if (vF > maxV) vF -= (vF - maxV) * Math.min(1, 1.5f * dt);
        if (off && Math.abs(vF) > 10f) vF -= Math.signum(vF) * (Math.abs(vF) - 10f) * Math.min(1, 2.2f * dt);
        vL *= (float) Math.exp(-7f * dt);
        c.vx = fx * vF + lx * vL;
        c.vy = fy * vF + ly * vL;
        c.x += c.vx * dt;
        c.y += c.vy * dt;
        c.spin += vF * dt;

        locate(c);
        float nnx = nx[c.idx], nny = ny[c.idx];
        c.lat = (c.x - px[c.idx]) * nnx + (c.y - py[c.idx]) * nny;
        float lim = wallDist - 1.0f;
        if (Math.abs(c.lat) > lim) {
            float s = Math.signum(c.lat), over = Math.abs(c.lat) - lim;
            c.x -= nnx * s * over;
            c.y -= nny * s * over;
            float vn = (c.vx * nnx + c.vy * nny) * s;
            if (vn > 0) {
                c.vx -= nnx * s * vn * 1.3f;
                c.vy -= nny * s * vn * 1.3f;
                c.vx *= 0.93f;
                c.vy *= 0.93f;
            }
            c.lat = s * lim;
        }
    }

    /** Steering (-1..1) that points the car at the road a little ahead, in the given lane. */
    private float follow(Car c, float lane, float sp) {
        int look = (c.idx + 3 + (int) (sp * 0.35f / 4f)) % n;
        float tx = px[look] + nx[look] * lane, ty = py[look] + ny[look] * lane;
        float want = (float) Math.atan2(tx - c.x, ty - c.y);
        return clamp(wrap(want - c.h) * 2.5f, -1, 1);
    }

    private void ai(Car c, float dt) {
        float sp = c.speed();
        float steer = follow(c, c.lane, sp);
        float k = 0;
        int ahead = 6 + (int) (sp * 1.5f / 4f);
        for (int i = 0; i < ahead; i++) k = Math.max(k, curv[(c.idx + i) % n]);
        float vt = Math.min(c.maxV * c.skill, (float) Math.sqrt(c.grip * 0.8f / Math.max(k, 1e-4f)) * c.skill + 2f);
        float thr = sp < vt ? 1 : sp > vt + 2 ? -1 : 0;
        // unstick after a crash
        c.stuck = sp < 1.5f ? c.stuck + dt : 0;
        if (c.stuck > 2.5f) place(c, c.idx + 1, 0);
        drive(c, dt, thr, steer, false);
    }

    private void bump() {
        for (int i = 0; i < cars.length; i++)
            for (int j = i + 1; j < cars.length; j++) {
                Car c1 = cars[i], c2 = cars[j];
                float dx = c2.x - c1.x, dy = c2.y - c1.y, d = (float) Math.hypot(dx, dy);
                if (d >= 2.3f || d < 1e-3f) continue;
                float ux = dx / d, uy = dy / d, push = (2.3f - d) / 2;
                c1.x -= ux * push;
                c1.y -= uy * push;
                c2.x += ux * push;
                c2.y += uy * push;
                float rel = (c2.vx - c1.vx) * ux + (c2.vy - c1.vy) * uy;
                if (rel < 0) {
                    c1.vx += ux * rel * 0.5f;
                    c1.vy += uy * rel * 0.5f;
                    c2.vx -= ux * rel * 0.5f;
                    c2.vy -= uy * rel * 0.5f;
                }
            }
    }

    private void step(float dt) {
        if (countdown > 0) {
            float before = countdown;
            countdown -= dt;
            if ((int) Math.ceil(before) != (int) Math.ceil(countdown)) {
                int s = (int) Math.ceil(countdown);
                a.speaker.say(s > 0 ? String.valueOf(s) : "Go!");
            }
            return;
        }
        if (countdown > -1f) countdown -= dt; // keeps "GO!" on screen for a moment, then hides it
        clock += dt;
        for (Car c : cars) {
            if (c.finished) {
                drive(c, dt, 0, 0, false); // roll to a stop after the line
                continue;
            }
            if (c.player) {
                float steer = (left ? 1 : 0) - (right ? 1 : 0);
                // steering help for small children: with no arrow held the car follows the road in its own lane
                if (steer == 0 && steerHelp) steer = follow(c, clamp(c.lat, -hw + 2.5f, hw - 2.5f) * 0.85f, c.speed()) * 0.8f;
                drive(c, dt, brake ? -1 : 1, steer, boost);
            }
            else ai(c, dt);
            if (c.lap >= LAPS) {
                c.finished = true;
                c.finishT = clock;
            }
        }
        bump();
        if (me.lap == LAPS - 1 && lastLapSaid != me.lap && !me.finished) {
            lastLapSaid = me.lap;
            a.speaker.say("Final lap!");
        }
        if (me.finished && !finishedSaid) {
            finishedSaid = true;
            endWait = 1.6f;
        }
        if (endWait > 0) {
            endWait -= dt;
            if (endWait <= 0) {
                running = false;
                int place = 1;
                for (Car c : cars) if (c != me && c.finished && c.finishT < me.finishT) place++;
                final int pl = place;
                post(() -> done.on(pl, me.finishT)); // not in the middle of drawing
            }
        }
    }

    int place() {
        int place = 1;
        for (Car c : cars) {
            if (c == me) continue;
            if (me.finished ? c.finished && c.finishT < me.finishT : c.finished || progress(c) > progress(me)) place++;
        }
        return place;
    }

    // ---- drawing ----

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        miniSize = Ui.dp(a, 104);
        float span = Math.max(maxX - minX, maxY - minY), s = (miniSize - 12) / span;
        mini.reset();
        for (int i = 0; i <= n; i++) {
            float x = 6 + (px[i % n] - minX) * s, y = 6 + (py[i % n] - minY) * s;
            if (i == 0) mini.moveTo(x, y);
            else mini.lineTo(x, y);
        }
    }

    private float miniX(float x) {
        return 6 + (x - minX) * (miniSize - 12) / Math.max(maxX - minX, maxY - minY);
    }

    private float miniY(float y) {
        return 6 + (y - minY) * (miniSize - 12) / Math.max(maxX - minX, maxY - minY);
    }

    private static int shade(int c, float f) {
        return Color.argb(Color.alpha(c), (int) Math.min(255, Color.red(c) * f), (int) Math.min(255, Color.green(c) * f), (int) Math.min(255, Color.blue(c) * f));
    }

    @Override
    protected void onDraw(Canvas cv) {
        long now = SystemClock.uptimeMillis();
        float dt = last == 0 ? 0.016f : Math.min(0.05f, (now - last) / 1000f);
        last = now;
        if (running) {
            step(dt / 2);
            if (running) step(dt / 2);
        }
        int w = getWidth(), h = getHeight();
        int ground = RaceGame.TRACK_GROUND[track], road = RaceGame.TRACK_ROAD[track];
        cv.drawColor(ground);

        float target = w / (34f + me.speed() * 0.7f);
        scale += (target - scale) * Math.min(1, dt * 2);
        camH += wrap(me.h - camH) * Math.min(1, dt * 5);
        float lookX = me.x + (float) Math.sin(camH) * (h * 0.16f / scale), lookY = me.y + (float) Math.cos(camH) * (h * 0.16f / scale);
        float radius = (float) Math.hypot(w, h) / scale * 0.6f + 12;

        cv.save();
        cv.translate(w / 2f, h * 0.66f);
        cv.rotate((float) Math.toDegrees(camH) - 180f);
        cv.scale(scale, scale);
        cv.translate(-me.x, -me.y);

        // visible track points
        if (vis.length < n) vis = new int[n];
        int nv = 0;
        for (int i = 0; i < n; i++) {
            float dx = px[i] - lookX, dy = py[i] - lookY;
            if (dx * dx + dy * dy < radius * radius) vis[nv++] = i;
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        boolean neon = track == 3;
        // run-off area
        p.setColor(shade(ground, neon ? 1.25f : 0.9f));
        p.setStrokeWidth(wallDist * 2);
        for (int k = 0; k < nv; k++) seg(cv, vis[k], 0);
        // tyre walls
        p.setStrokeWidth(0.9f);
        p.setStrokeCap(Paint.Cap.BUTT);
        for (int k = 0; k < nv; k++) {
            int i = vis[k];
            p.setColor(neon ? (i % 2 == 0 ? 0xFF00E5FF : 0xFFFF4081) : (i % 2 == 0 ? 0xFFECEFF1 : 0xFFD32F2F));
            seg(cv, i, wallDist);
            seg(cv, i, -wallDist);
        }
        // kerbs, road, centre dashes
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth((hw + 1.2f) * 2);
        for (int k = 0; k < nv; k++) {
            int i = vis[k];
            p.setColor(neon ? (i % 2 == 0 ? 0xFFFFEA00 : 0xFF263238) : (i % 2 == 0 ? 0xFFE53935 : 0xFFFAFAFA));
            seg(cv, i, 0);
        }
        p.setColor(road);
        p.setStrokeWidth(hw * 2);
        for (int k = 0; k < nv; k++) seg(cv, vis[k], 0);
        p.setColor(0xCCFFFFFF);
        p.setStrokeWidth(0.35f);
        p.setStrokeCap(Paint.Cap.BUTT);
        for (int k = 0; k < nv; k++) if (vis[k] % 3 == 0) seg(cv, vis[k], 0);
        p.setStyle(Paint.Style.FILL);
        startLine(cv);
        drawProps(cv, lookX, lookY, radius);
        for (Car c : cars) {
            cv.save();
            cv.translate(c.x, c.y);
            cv.rotate((float) -Math.toDegrees(c.h));
            drawCar(cv, p, c.model, c.col, c.braking, c.boosting, c.spin);
            cv.restore();
        }
        cv.restore();
        hud(cv, w, h);
        if (running) postInvalidateOnAnimation();
    }

    /** One segment of the centre line from point i to i+1, shifted sideways by off metres. */
    private void seg(Canvas cv, int i, float off) {
        int j = (i + 1) % n;
        cv.drawLine(px[i] + nx[i] * off, py[i] + ny[i] * off, px[j] + nx[j] * off, py[j] + ny[j] * off, p);
    }

    private void startLine(Canvas cv) {
        cv.save();
        cv.translate(px[0], py[0]);
        cv.rotate((float) -Math.toDegrees(Math.atan2(-ny[0], nx[0])));
        int cells = (int) (hw * 2 / 1.6f);
        float cw = hw * 2 / cells;
        for (int r = 0; r < 2; r++)
            for (int c = 0; c < cells; c++) {
                p.setColor((r + c) % 2 == 0 ? Color.WHITE : 0xFF212121);
                cv.drawRect(-hw + c * cw, r * cw, -hw + (c + 1) * cw, (r + 1) * cw, p);
            }
        cv.restore();
    }

    private void drawProps(Canvas cv, float lx, float ly, float radius) {
        for (int i = 0; i < propX.length; i++) {
            float x = propX[i], y = propY[i], s = propS[i];
            if ((x - lx) * (x - lx) + (y - ly) * (y - ly) > (radius + s) * (radius + s)) continue;
            int k = propKind[i];
            switch (track) {
                case 1: // desert: cactus or rock
                    if (k < 2) {
                        p.setColor(0xFF558B2F);
                        cv.drawCircle(x, y, s * 0.55f, p);
                        p.setColor(0xFF7CB342);
                        cv.drawCircle(x - s * 0.15f, y - s * 0.15f, s * 0.3f, p);
                    } else {
                        p.setColor(0xFFA1887F);
                        cv.drawCircle(x, y, s * 0.7f, p);
                        p.setColor(0xFFBCAAA4);
                        cv.drawCircle(x - s * 0.2f, y - s * 0.2f, s * 0.35f, p);
                    }
                    break;
                case 2: // snow: pines with white tops
                    p.setColor(k == 3 ? 0xFF90A4AE : 0xFF2E7D32);
                    cv.drawCircle(x, y, s, p);
                    p.setColor(Color.WHITE);
                    cv.drawCircle(x - s * 0.2f, y - s * 0.2f, s * 0.45f, p);
                    break;
                case 3: // city: buildings with neon edges
                    p.setColor(0xFF1A1C2C);
                    cv.drawRect(x - s, y - s, x + s, y + s, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(0.4f);
                    p.setColor(propCol[i]);
                    cv.drawRect(x - s, y - s, x + s, y + s, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0x99FFF59D);
                    for (int wx = 0; wx < 3; wx++)
                        for (int wy = 0; wy < 3; wy++)
                            if ((wx + wy + k) % 3 != 0) cv.drawRect(x - s * 0.7f + wx * s * 0.5f, y - s * 0.7f + wy * s * 0.5f,
                                    x - s * 0.5f + wx * s * 0.5f, y - s * 0.5f + wy * s * 0.5f, p);
                    break;
                case 4: // sunset: autumn trees
                    p.setColor(k % 2 == 0 ? 0xFFEF6C00 : 0xFF827717);
                    cv.drawCircle(x, y, s, p);
                    p.setColor(k % 2 == 0 ? 0xFFFFA726 : 0xFFAFB42B);
                    cv.drawCircle(x - s * 0.25f, y - s * 0.25f, s * 0.5f, p);
                    break;
                default: // meadow: trees and flower bushes
                    p.setColor(k == 3 ? 0xFF66BB6A : 0xFF2E7D32);
                    cv.drawCircle(x, y, s, p);
                    p.setColor(k == 3 ? 0xFFF06292 : 0xFF43A047);
                    cv.drawCircle(x - s * 0.25f, y - s * 0.25f, s * (k == 3 ? 0.3f : 0.55f), p);
            }
        }
    }

    /** A car seen from above in metres, nose towards +y. */
    static void drawCar(Canvas cv, Paint p, int model, int col, boolean braking, boolean boosting, float spin) {
        float L = RaceGame.CAR_LEN[model], W = RaceGame.CAR_WID[model];
        RectF r = new RectF();
        p.setStyle(Paint.Style.FILL);
        if (boosting) {
            float fl = 1.0f + 0.6f * (float) Math.abs(Math.sin(spin * 3));
            p.setColor(0xFF50BEFF);
            cv.drawRect(-0.55f, -L / 2 - fl, -0.15f, -L / 2, p);
            cv.drawRect(0.15f, -L / 2 - fl, 0.55f, -L / 2, p);
            p.setColor(Color.WHITE);
            cv.drawRect(-0.45f, -L / 2 - fl * 0.55f, -0.25f, -L / 2, p);
            cv.drawRect(0.25f, -L / 2 - fl * 0.55f, 0.45f, -L / 2, p);
        }
        // shadow and wheels
        p.setColor(0x40000000);
        r.set(-W / 2 + 0.15f, -L / 2 + 0.25f, W / 2 + 0.25f, L / 2 + 0.05f);
        cv.drawRoundRect(r, 0.5f, 0.5f, p);
        p.setColor(0xFF161618);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                float cx = sx * (W / 2 - 0.05f), cy = sz * L * 0.31f;
                cv.drawRect(cx - 0.17f, cy - 0.36f, cx + 0.17f, cy + 0.36f, p);
            }
        // body
        p.setColor(col);
        r.set(-W / 2 + 0.08f, -L / 2, W / 2 - 0.08f, L / 2);
        cv.drawRoundRect(r, 0.55f, 0.55f, p);
        // bonnet stripe
        p.setColor(shade(col, 1.35f));
        cv.drawRect(-W * 0.11f, L * 0.05f, W * 0.11f, L * 0.45f, p);
        // cabin glass
        float cab = RaceGame.CAR_CABLEN[model], off = RaceGame.CAR_CABOFF[model];
        p.setColor(0xFF1E283A);
        r.set(-W * 0.4f, off - cab / 2, W * 0.4f, off + cab / 2);
        cv.drawRoundRect(r, 0.3f, 0.3f, p);
        p.setColor(shade(col, 0.85f));
        r.set(-W * 0.36f, off - cab * 0.32f, W * 0.36f, off + cab * 0.28f);
        cv.drawRoundRect(r, 0.2f, 0.2f, p);
        // lights
        p.setColor(0xFFFFF5BE);
        cv.drawRect(-W * 0.42f, L / 2 - 0.12f, -W * 0.22f, L / 2, p);
        cv.drawRect(W * 0.22f, L / 2 - 0.12f, W * 0.42f, L / 2, p);
        p.setColor(braking ? 0xFFFF2828 : 0xFF781010);
        cv.drawRect(-W * 0.44f, -L / 2, -W * 0.22f, -L / 2 + 0.12f, p);
        cv.drawRect(W * 0.22f, -L / 2, W * 0.44f, -L / 2 + 0.12f, p);
        if (RaceGame.CAR_SPOILER[model]) {
            p.setColor(0xFF18181C);
            cv.drawRect(-W * 0.51f, -L * 0.47f, W * 0.51f, -L * 0.40f, p);
        }
    }

    private void hud(Canvas cv, int w, int h) {
        float d = getResources().getDisplayMetrics().density;
        tp.setColor(Color.WHITE);
        tp.setFakeBoldText(true);
        tp.setShadowLayer(4 * d, 0, d, 0xAA000000);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setTextSize(22 * d);
        float x = 14 * d, y = 34 * d;
        cv.drawText("Lap " + Math.max(1, Math.min(LAPS, me.lap + 1)) + "/" + LAPS, x, y, tp);
        String[] ord = {"", "1st", "2nd", "3rd", "4th"};
        tp.setTextSize(30 * d);
        cv.drawText(ord[place()] + " of 4", x, y + 36 * d, tp);
        tp.setTextSize(17 * d);
        cv.drawText(RaceGame.time(me.finished ? me.finishT : clock) + "   " + Math.round(me.speed() * 3.6f / SPEED_SCALE) + " km/h", x, y + 64 * d, tp);
        // boost tank
        float bx = x, by = y + 78 * d, bw = 110 * d, bh = 9 * d;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x66000000);
        rf.set(bx, by, bx + bw, by + bh);
        cv.drawRoundRect(rf, bh / 2, bh / 2, p);
        p.setColor(me.boosting ? 0xFFFFFFFF : 0xFF50BEFF);
        rf.set(bx, by, bx + bw * me.tank / 2.5f, by + bh);
        cv.drawRoundRect(rf, bh / 2, bh / 2, p);
        tp.setTextSize(12 * d);
        cv.drawText("🚀 boost", bx + bw + 6 * d, by + bh, tp);
        // minimap
        cv.save();
        cv.translate(w - miniSize - 10 * d, 10 * d);
        p.setColor(0x55000000);
        rf.set(0, 0, miniSize, miniSize);
        cv.drawRoundRect(rf, 12 * d, 12 * d, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4 * d);
        p.setColor(0xDDFFFFFF);
        cv.drawPath(mini, p);
        p.setStyle(Paint.Style.FILL);
        for (Car c : cars) {
            if (c == me) continue;
            p.setColor(c.col);
            cv.drawCircle(miniX(c.x), miniY(c.y), 4 * d, p);
        }
        p.setColor(Color.WHITE);
        cv.drawCircle(miniX(me.x), miniY(me.y), 6.5f * d, p);
        p.setColor(me.col);
        cv.drawCircle(miniX(me.x), miniY(me.y), 4.5f * d, p);
        cv.restore();
        // countdown
        if (countdown > -0.8f) {
            String s = countdown > 0 ? String.valueOf((int) Math.ceil(countdown)) : "GO!";
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTextSize(110 * d);
            tp.setColor(countdown > 0 ? 0xFFFFEB3B : 0xFF76FF03);
            cv.drawText(s, w / 2f, h * 0.42f, tp);
        }
        if (me.finished) {
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTextSize(52 * d);
            tp.setColor(0xFFFFEB3B);
            cv.drawText("FINISH! 🏁", w / 2f, h * 0.42f, tp);
        }
        tp.setShadowLayer(0, 0, 0, 0);
    }
}
