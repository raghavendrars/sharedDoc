using System.Numerics;
using Raylib_cs;
using static Raylib_cs.Raylib;

namespace TurboCircuit;

class Race
{
    public const int Laps = 3;
    public readonly Track Tr;
    public readonly int TrackIdx, Players;
    public readonly Car[] Cars;
    public readonly Car[] Humans;
    public float Countdown = 3.6f, Time;
    public bool Done;
    public List<Car> Order;

    readonly bool auto;
    readonly float[] camH;
    readonly bool[] boostWas = new bool[2];
    float endTimer = -1, thumpCd;
    int lastBeep = 4;

    static readonly Color[] AiCols = { U.C(245, 200, 40), U.C(60, 190, 90), U.C(240, 130, 40), U.C(170, 90, 220), U.C(230, 230, 235) };
    static readonly string[] AiNames = { "Rex", "Nova", "Blaze", "Ivy", "Dash" };

    public Race(Track tr, int trackIdx, int players, int[] chosen, Profile prof, bool autopilot)
    {
        Tr = tr; TrackIdx = trackIdx; Players = players; auto = autopilot;
        const int total = 6;
        int ai = total - players;
        var def = tr.Def;
        Cars = new Car[total];
        Humans = new Car[players];
        camH = new float[players];
        for (int k = 0; k < total; k++)
        {
            Car c;
            if (k < ai)
            {
                int model = Math.Max(0, def.AiCar - (k >= 2 ? 1 : 0));
                int t = def.AiParts;
                var st = Db.Compute(model, new[] { t, t, t, t, t, 0 });
                st.MaxSpeed *= 0.97f - 0.015f * k;
                c = new Car(model, st, AiCols[k], AiNames[k])
                {
                    Skill = def.AiSkill - 0.025f * k,
                    Lane = (k - (ai - 1) / 2f) * (def.HalfWidth * 0.9f / ai),
                };
            }
            else
            {
                int p = k - ai;
                int model = chosen[p];
                c = new Car(model, Db.Compute(model, prof.Parts[model]), p == 0 ? U.C(225, 40, 45) : U.C(40, 110, 235),
                    players == 1 ? "YOU" : "PLAYER " + (p + 1))
                { Player = p, Skill = 0.86f, Lane = p == 0 ? 1.5f : -1.5f };
                Humans[p] = c;
            }
            c.Place(tr, tr.N - 2 - 2 * k, (k % 2 == 0 ? 1 : -1) * def.HalfWidth * 0.42f);
            c.Lap = -1;
            Cars[k] = c;
        }
        for (int p = 0; p < players; p++) camH[p] = Humans[p].H;
        Order = Cars.ToList();
        Sort();
    }

    void Sort()
    {
        Order.Sort((a, b) =>
        {
            if (a.Finished && b.Finished) return a.FinishTime.CompareTo(b.FinishTime);
            if (a.Finished != b.Finished) return a.Finished ? -1 : 1;
            return b.Progress(Tr).CompareTo(a.Progress(Tr));
        });
    }

    public int PositionOf(Car c) => Order.IndexOf(c) + 1;

    void Ai(Car c, out float thr, out float steer)
    {
        float v = c.Speed;
        int n = Tr.N;
        int look = 3 + (int)(v * 0.14f);
        int ti = (c.Idx + look) % n;
        Vector2 d = Tr.P[ti] + Tr.Nrm[ti] * c.Lane - c.Pos;
        float diff = U.Wrap(MathF.Atan2(d.X, d.Y) - c.H);
        steer = Math.Clamp(diff * 2.5f, -1f, 1f);

        float grip = c.S.Grip * c.Skill, brake = c.S.Brake * 0.7f;
        int K = 8 + (int)(v * v / (2 * brake) / Tr.Spacing);
        float allowed = float.MaxValue;
        for (int k = 0; k <= K; k++)
        {
            float cv = MathF.Max(Tr.Curv[(c.Idx + k) % n], 1e-4f);
            float va = MathF.Sqrt(grip / cv + 2 * brake * k * Tr.Spacing);
            if (va < allowed) allowed = va;
        }
        thr = v > allowed + 1.5f ? -1f : v > allowed ? 0f : 1f;
        if (MathF.Abs(diff) > 1.2f && v > 8) thr = -1f;
    }

    void ReadInput(int p, out float thr, out float steer, out bool nitro, out bool reset)
    {
        bool up, down, left, right;
        if (Players == 1)
        {
            up = IsKeyDown(KeyboardKey.W) || IsKeyDown(KeyboardKey.Up);
            down = IsKeyDown(KeyboardKey.S) || IsKeyDown(KeyboardKey.Down);
            left = IsKeyDown(KeyboardKey.A) || IsKeyDown(KeyboardKey.Left);
            right = IsKeyDown(KeyboardKey.D) || IsKeyDown(KeyboardKey.Right);
            nitro = IsKeyDown(KeyboardKey.Space) || IsKeyDown(KeyboardKey.LeftShift) || IsKeyDown(KeyboardKey.RightShift);
            reset = IsKeyPressed(KeyboardKey.R);
        }
        else if (p == 0)
        {
            up = IsKeyDown(KeyboardKey.W); down = IsKeyDown(KeyboardKey.S);
            left = IsKeyDown(KeyboardKey.A); right = IsKeyDown(KeyboardKey.D);
            nitro = IsKeyDown(KeyboardKey.LeftShift);
            reset = IsKeyPressed(KeyboardKey.R);
        }
        else
        {
            up = IsKeyDown(KeyboardKey.Up); down = IsKeyDown(KeyboardKey.Down);
            left = IsKeyDown(KeyboardKey.Left); right = IsKeyDown(KeyboardKey.Right);
            nitro = IsKeyDown(KeyboardKey.RightShift) || IsKeyDown(KeyboardKey.RightControl);
            reset = IsKeyPressed(KeyboardKey.Backspace);
        }
        thr = (up ? 1 : 0) - (down ? 1 : 0);
        steer = (left ? 1 : 0) - (right ? 1 : 0);
    }

    public void Update(float dt, bool sound = true)
    {
        if (Countdown > 0)
        {
            Countdown -= dt;
            int sec = Math.Max(0, (int)MathF.Ceiling(Countdown));
            if (sec != lastBeep && sec <= 3)
            {
                lastBeep = sec;
                if (sound) { if (sec > 0) Audio.Beep(); else Audio.Go(); }
            }
        }
        bool go = Countdown <= 0;
        if (go) Time += dt;
        thumpCd -= dt;

        foreach (var c in Cars)
        {
            float thr = 0, steer = 0; bool nitro = false, reset = false;
            bool human = c.Player >= 0 && !c.Finished && !auto;
            if (go)
            {
                if (human) ReadInput(c.Player, out thr, out steer, out nitro, out reset);
                else
                {
                    Ai(c, out thr, out steer);
                    if (c.Finished) thr = MathF.Min(thr, c.Speed > 18 ? 0f : 0.4f);
                }
            }
            c.Update(Tr, dt, thr, steer, nitro);

            if (go && !human)
            {
                c.StuckT = c.Speed < 2f ? c.StuckT + dt : 0;
                if (c.StuckT > 2.5f) reset = true;
            }
            if (reset) c.Place(Tr, c.Idx, Math.Clamp(c.Lane, -Tr.Def.HalfWidth * 0.6f, Tr.Def.HalfWidth * 0.6f));

            if (c.Player >= 0 && sound)
            {
                if (c.HitWall > 5 && thumpCd <= 0) { Audio.Thump(c.HitWall / 20f); thumpCd = 0.3f; }
                if (c.NitroOn && !boostWas[c.Player]) Audio.Boost();
                boostWas[c.Player] = c.NitroOn;
            }
            if (!c.Finished && c.Lap >= Laps)
            {
                c.Finished = true; c.FinishTime = Time;
                if (c.Player >= 0 && sound) Audio.Cheer();
            }
        }

        // car-to-car contact
        for (int a = 0; a < Cars.Length; a++)
            for (int b = a + 1; b < Cars.Length; b++)
            {
                Car A = Cars[a], B = Cars[b];
                Vector2 d = B.Pos - A.Pos;
                float dist = d.Length();
                const float min = 2.5f;
                if (dist >= min || dist < 0.001f) continue;
                Vector2 n = d / dist;
                float pen = min - dist;
                A.Pos -= n * pen / 2; B.Pos += n * pen / 2;
                float rel = Vector2.Dot(B.Vel - A.Vel, n);
                if (rel < 0)
                {
                    A.Vel += n * rel * 0.6f; B.Vel -= n * rel * 0.6f;
                    if (sound && rel < -5 && thumpCd <= 0 && (A.Player >= 0 || B.Player >= 0)) { Audio.Thump(-rel / 20f); thumpCd = 0.3f; }
                }
            }

        Sort();

        for (int p = 0; p < Players; p++)
        {
            var c = Humans[p];
            camH[p] = U.Wrap(camH[p] + U.Wrap(c.H - camH[p]) * MathF.Min(1, dt * 5f));
            if (sound)
            {
                float r = c.Speed / c.S.MaxSpeed;
                Audio.Engine(p, 0.55f + 1.5f * r + (c.NitroOn ? 0.2f : 0), (Players == 2 ? 0.22f : 0.3f) + 0.1f * r);
            }
        }

        if (endTimer < 0 && Humans.All(h => h.Finished)) endTimer = 3f;
        if (endTimer > 0)
        {
            endTimer -= dt;
            if (endTimer <= 0) Done = true;
        }
    }

    public void DrawView(int p, int w, int h)
    {
        var me = Humans[p];
        var def = Tr.Def;
        DrawRectangleGradientV(0, 0, w, h, def.SkyTop, def.SkyLow);

        Vector2 dir = new(MathF.Sin(camH[p]), MathF.Cos(camH[p]));
        float sp = me.Speed;
        Vector2 cp = me.Pos - dir * 8.5f;
        var cam = new Camera3D(U.V3(cp, 3.4f), U.V3(me.Pos + dir * 5f, 1.2f), Vector3.UnitY,
            58f + 12f * MathF.Min(1, sp / 90f) + (me.NitroOn ? 6f : 0f), CameraProjection.Perspective);

        BeginMode3D(cam);
        DrawPlane(U.V3((Tr.Min + Tr.Max) / 2, 0), new Vector2(4000, 4000), def.Ground);
        Rlgl.DrawRenderBatchActive();
        Rlgl.DisableDepthTest();
        Rlgl.DisableBackfaceCulling();
        Tr.DrawFlat();
        foreach (var c in Cars)
        {
            var m = Db.Cars[c.Model];
            Vector2 f = c.Fwd * (m.Len / 2 + 0.2f), l = c.Left * (m.Wid / 2 + 0.2f);
            Color sh = U.C(0, 0, 0, 90);
            Vector3 a = U.V3(c.Pos + f + l, 0), b = U.V3(c.Pos + f - l, 0), cc = U.V3(c.Pos - f - l, 0), d = U.V3(c.Pos - f + l, 0);
            DrawTriangle3D(a, b, cc, sh); DrawTriangle3D(a, cc, d, sh);
        }
        Rlgl.DrawRenderBatchActive();
        Rlgl.EnableDepthTest();
        Tr.DrawSolid(cp);
        Rlgl.DrawRenderBatchActive();
        Rlgl.EnableBackfaceCulling();
        foreach (var c in Cars)
            Car.Draw(Db.Cars[c.Model], c.Pos, c.H, c.Col, c.Steer, c.Braking, c.NitroOn, c.WheelSpin);
        EndMode3D();

        DrawHud(p, w, h);
    }

    void DrawHud(int p, int w, int h)
    {
        var me = Humans[p];
        Color panel = U.C(0, 0, 0, 130);
        int pos = PositionOf(me);

        DrawRectangle(14, 14, 210, 92, panel);
        U.Text(U.Ordinal(pos), 26, 20, 50, pos == 1 ? U.C(255, 215, 60) : Color.White);
        U.Text("/ " + Cars.Length, 132, 44, 22, Color.LightGray);
        U.Text($"LAP {Math.Clamp(me.Lap + 1, 1, Laps)}/{Laps}", 26, 76, 22, Color.White);
        U.Text(U.TimeStr(me.Finished ? me.FinishTime : Time), 132, 76, 22, U.C(180, 230, 255));

        // speed + nitro
        int bx = w - 224, by = h - 106;
        DrawRectangle(bx, by, 210, 92, panel);
        U.TextR(((int)(me.Speed * 3.6f)).ToString(), bx + 140, by + 8, 52, Color.White);
        U.Text("km/h", bx + 148, by + 34, 20, Color.LightGray);
        if (me.S.NitroCap > 0)
        {
            DrawRectangle(bx + 12, by + 66, 186, 14, U.C(30, 30, 40));
            DrawRectangle(bx + 12, by + 66, (int)(186 * me.Nitro / me.S.NitroCap), 14, me.NitroOn ? Color.White : U.C(70, 180, 255));
            U.Text("NITRO", bx + 16, by + 67, 12, U.C(10, 20, 40));
        }
        else U.Text("no nitro fitted", bx + 14, by + 66, 16, Color.Gray);

        // minimap
        int ms = Math.Min(170, w / 4), mx = w - ms - 18, my = 16;
        DrawRectangle(mx - 6, my - 6, ms + 12, ms + 12, panel);
        Tr.DrawMini(mx, my, ms, U.C(230, 230, 230), 3f);
        float sc = Tr.MiniScale(ms);
        foreach (var c in Cars)
            if (c.Player < 0) DrawCircleV(Tr.MiniPt(c.Pos, mx, my, ms, sc), 3.5f, c.Col);
        foreach (var c in Humans)
        {
            var q = Tr.MiniPt(c.Pos, mx, my, ms, sc);
            DrawCircleV(q, 6f, Color.Black); DrawCircleV(q, 4.5f, c.Col);
        }

        // running order
        int oy = 116;
        for (int i = 0; i < Order.Count; i++)
        {
            var c = Order[i];
            DrawRectangle(14, oy + i * 22, 150, 20, c == me ? U.C(255, 255, 255, 70) : panel);
            DrawRectangle(18, oy + i * 22 + 4, 12, 12, c.Col);
            U.Text($"{i + 1}  {c.Name}", 36, oy + i * 22 + 2, 16, Color.White);
        }

        if (Countdown > 0)
        {
            int sec = (int)MathF.Ceiling(Countdown);
            string s = sec > 3 ? "READY" : sec.ToString();
            U.TextC(s, w / 2 + 3, h / 3 + 3, 110, Color.Black);
            U.TextC(s, w / 2, h / 3, 110, sec > 3 ? Color.White : U.C(255, 210, 50));
        }
        else if (Time < 1.2f)
        {
            U.TextC("GO!", w / 2 + 3, h / 3 + 3, 110, Color.Black);
            U.TextC("GO!", w / 2, h / 3, 110, U.C(80, 240, 110));
        }

        if (me.Finished)
        {
            DrawRectangle(0, h / 3 - 10, w, 110, U.C(0, 0, 0, 150));
            U.TextC("FINISHED  " + U.Ordinal(pos), w / 2, h / 3, 60, U.C(255, 215, 60));
            U.TextC(U.TimeStr(me.FinishTime), w / 2, h / 3 + 64, 28, Color.White);
        }
        else if (MathF.Abs(me.Lat) > Tr.Def.HalfWidth + 1.2f && Countdown <= 0)
            U.TextC("OFF TRACK", w / 2, h - 60, 26, U.C(255, 120, 60));
    }
}
