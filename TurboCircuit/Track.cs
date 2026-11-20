using System.Numerics;
using Raylib_cs;
using static Raylib_cs.Raylib;

namespace TurboCircuit;

class Prop
{
    public Vector2 P;
    public float S, W, H, D;
    public int Kind;
    public Color C;
}

class Track
{
    public readonly TrackDef Def;
    public readonly int N;
    public readonly float Spacing, WallDist, Length;
    public readonly Vector2[] P, T, Nrm;
    public readonly float[] Curv;
    public Vector2 Min, Max;
    public readonly List<Prop> Props = new();
    public float MinRadius, MinGap;

    readonly Vector3[] eL, eR, kL, kR, wL, wR;

    static Vector2 CatmullRom(Vector2 a, Vector2 b, Vector2 c, Vector2 d, float t)
    {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * (2f * b + (c - a) * t + (2f * a - 5f * b + 4f * c - d) * t2 + (3f * b - a - 3f * c + d) * t3);
    }

    public Track(TrackDef def, int seed)
    {
        Def = def;
        var pts = def.Pts;
        int m = pts.Length;
        var dense = new List<Vector2>();
        for (int i = 0; i < m; i++)
            for (int s = 0; s < 24; s++)
                dense.Add(CatmullRom(pts[(i - 1 + m) % m], pts[i], pts[(i + 1) % m], pts[(i + 2) % m], s / 24f));

        int cnt = dense.Count;
        var cum = new float[cnt + 1];
        for (int i = 0; i < cnt; i++) cum[i + 1] = cum[i] + Vector2.Distance(dense[i], dense[(i + 1) % cnt]);
        Length = cum[cnt];
        N = (int)MathF.Round(Length / 4f);
        Spacing = Length / N;
        WallDist = def.HalfWidth + 5f;

        P = new Vector2[N]; T = new Vector2[N]; Nrm = new Vector2[N]; Curv = new float[N];
        for (int k = 0; k < N; k++)
        {
            float d = (50f + k * Spacing) % Length;
            int seg = Array.BinarySearch(cum, d);
            if (seg < 0) seg = ~seg - 1;
            seg = Math.Clamp(seg, 0, cnt - 1);
            float segLen = cum[seg + 1] - cum[seg];
            float f = segLen > 1e-5f ? (d - cum[seg]) / segLen : 0;
            P[k] = Vector2.Lerp(dense[seg], dense[(seg + 1) % cnt], f);
        }
        for (int i = 0; i < N; i++)
        {
            T[i] = Vector2.Normalize(P[(i + 1) % N] - P[(i - 1 + N) % N]);
            Nrm[i] = new Vector2(T[i].Y, -T[i].X);
        }
        var raw = new float[N];
        for (int i = 0; i < N; i++)
        {
            var a = T[(i - 1 + N) % N]; var b = T[(i + 1) % N];
            float ang = MathF.Abs(MathF.Atan2(a.X * b.Y - a.Y * b.X, Vector2.Dot(a, b)));
            raw[i] = ang / (2 * Spacing);
        }
        float maxC = 0;
        for (int i = 0; i < N; i++)
        {
            float s = 0;
            for (int k = -2; k <= 2; k++) s += raw[(i + k + N) % N];
            Curv[i] = s / 5f;
            maxC = MathF.Max(maxC, Curv[i]);
        }
        MinRadius = 1f / MathF.Max(maxC, 1e-5f);

        // closest approach between separate parts of the circuit (used by the self test)
        int skip = (int)(90f / Spacing);
        MinGap = float.MaxValue;
        for (int i = 0; i < N; i++)
            for (int j = i + skip; j < N; j++)
            {
                if (N - j + i < skip) continue;
                MinGap = MathF.Min(MinGap, Vector2.Distance(P[i], P[j]));
            }

        Min = new Vector2(float.MaxValue); Max = new Vector2(float.MinValue);
        foreach (var p in P) { Min = Vector2.Min(Min, p); Max = Vector2.Max(Max, p); }

        eL = new Vector3[N]; eR = new Vector3[N]; kL = new Vector3[N]; kR = new Vector3[N]; wL = new Vector3[N]; wR = new Vector3[N];
        float hw = def.HalfWidth;
        for (int i = 0; i < N; i++)
        {
            eL[i] = U.V3(P[i] + Nrm[i] * hw, 0); eR[i] = U.V3(P[i] - Nrm[i] * hw, 0);
            kL[i] = U.V3(P[i] + Nrm[i] * (hw + 1.2f), 0); kR[i] = U.V3(P[i] - Nrm[i] * (hw + 1.2f), 0);
            wL[i] = U.V3(P[i] + Nrm[i] * WallDist, 0); wR[i] = U.V3(P[i] - Nrm[i] * WallDist, 0);
        }

        BuildProps(seed);
    }

    void BuildProps(int seed)
    {
        var rng = new Random(seed);
        int theme = Def.Theme;
        float clear = WallDist + (theme == 3 ? 16f : 5f);
        int want = theme == 3 ? 120 : 200;
        for (int tries = 0; tries < 3000 && Props.Count < want; tries++)
        {
            var p = new Vector2(Min.X - 90 + (float)rng.NextDouble() * (Max.X - Min.X + 180),
                                Min.Y - 90 + (float)rng.NextDouble() * (Max.Y - Min.Y + 180));
            float best = float.MaxValue;
            for (int i = 0; i < N; i += 2) best = MathF.Min(best, Vector2.DistanceSquared(p, P[i]));
            if (best < clear * clear) continue;
            var pr = new Prop { P = p, S = 0.8f + (float)rng.NextDouble() * 0.9f, Kind = rng.Next(100) };
            if (theme == 3)
            {
                bool tooClose = false;
                foreach (var o in Props) if (Vector2.Distance(o.P, p) < 22) { tooClose = true; break; }
                if (tooClose) continue;
                pr.W = 12 + rng.Next(9); pr.D = 12 + rng.Next(9); pr.H = 10 + rng.Next(34);
                int g = 40 + rng.Next(40);
                pr.C = U.C(g, g + rng.Next(10), g + 15 + rng.Next(25));
            }
            Props.Add(pr);
        }
    }

    public void Locate(ref int idx, ref int lap, Vector2 pos)
    {
        int bestK = 0; float bd = float.MaxValue;
        for (int k = -5; k <= 12; k++)
        {
            int j = ((idx + k) % N + N) % N;
            float d = Vector2.DistanceSquared(pos, P[j]);
            if (d < bd) { bd = d; bestK = k; }
        }
        int ni = idx + bestK;
        if (ni >= N) { ni -= N; lap++; }
        else if (ni < 0) { ni += N; lap--; }
        idx = ni;
    }

    static void Quad(Vector3 a, Vector3 b, Vector3 c, Vector3 d, Color col)
    {
        DrawTriangle3D(a, b, c, col);
        DrawTriangle3D(a, c, d, col);
    }

    /// <summary>Ground-level geometry. Drawn with the depth test off, in painter's order.</summary>
    public void DrawFlat()
    {
        Color road = Def.Road, road2 = U.Shade(Def.Road, 0.92f);
        Color k1 = U.C(215, 40, 40), k2 = U.C(240, 240, 240);
        if (Def.Theme == 3) { k1 = U.C(255, 60, 200); k2 = U.C(60, 230, 255); }
        Color line = U.C(240, 240, 220);
        for (int i = 0; i < N; i++)
        {
            int j = (i + 1) % N;
            Quad(eL[i], eR[i], eR[j], eL[j], (i / 2) % 2 == 0 ? road : road2);
            Color kc = i % 2 == 0 ? k1 : k2;
            Quad(kL[i], eL[i], eL[j], kL[j], kc);
            Quad(eR[i], kR[i], kR[j], eR[j], kc);
            if (i % 4 < 2)
            {
                var a = P[i]; var b = P[j];
                Quad(U.V3(a + Nrm[i] * 0.15f, 0), U.V3(a - Nrm[i] * 0.15f, 0), U.V3(b - Nrm[j] * 0.15f, 0), U.V3(b + Nrm[j] * 0.15f, 0), line);
            }
        }
        // chequered start line
        const int cols = 10;
        for (int r = 0; r < 2; r++)
            for (int c = 0; c < cols; c++)
            {
                float f0 = c / (float)cols, f1 = (c + 1) / (float)cols;
                Vector3 a0 = Vector3.Lerp(eL[0], eR[0], f0), a1 = Vector3.Lerp(eL[0], eR[0], f1);
                Vector3 off0 = U.V3(T[0] * (r * 1.2f), 0), off1 = U.V3(T[0] * ((r + 1) * 1.2f), 0);
                Quad(a0 + off0, a1 + off0, a1 + off1, a0 + off1, (r + c) % 2 == 0 ? Color.White : Color.Black);
            }
    }

    /// <summary>Barriers, scenery and the start gantry (depth tested).</summary>
    public void DrawSolid(Vector2 camPos)
    {
        Color w1 = U.C(225, 225, 230), w2 = U.C(200, 45, 45);
        if (Def.Theme == 3) { w1 = U.C(70, 70, 90); w2 = U.C(250, 210, 60); }
        var up = new Vector3(0, 0.9f, 0);
        for (int i = 0; i < N; i++)
        {
            int j = (i + 1) % N;
            Color c = (i / 2) % 2 == 0 ? w1 : w2;
            Quad(wL[i], wL[j], wL[j] + up, wL[i] + up, c);
            Quad(wR[i], wR[j], wR[j] + up, wR[i] + up, U.Shade(c, 0.85f));
        }

        // gantry over the start line
        float hw = Def.HalfWidth + 2f;
        Rlgl.PushMatrix();
        Rlgl.Translatef(P[0].X, 0, P[0].Y);
        Rlgl.Rotatef(MathF.Atan2(T[0].X, T[0].Y) * (180f / MathF.PI), 0, 1, 0);
        DrawCube(new Vector3(hw, 3.5f, 0), 0.6f, 7f, 0.6f, U.C(60, 60, 70));
        DrawCube(new Vector3(-hw, 3.5f, 0), 0.6f, 7f, 0.6f, U.C(60, 60, 70));
        DrawCube(new Vector3(0, 7f, 0), hw * 2 + 0.6f, 1.4f, 0.5f, U.C(230, 200, 40));
        DrawCube(new Vector3(0, 7f, 0), hw * 2 - 2f, 0.7f, 0.6f, U.C(30, 30, 36));
        Rlgl.PopMatrix();

        foreach (var p in Props)
        {
            if (Vector2.DistanceSquared(p.P, camPos) > 420 * 420) continue;
            var b = U.V3(p.P, 0);
            float s = p.S;
            switch (Def.Theme)
            {
                case 0:
                    DrawCylinder(b, 0.3f * s, 0.4f * s, 2.4f * s, 6, U.C(110, 78, 46));
                    DrawCylinder(b + new Vector3(0, 2f * s, 0), 0, 2.3f * s, 5f * s, 7, p.Kind % 2 == 0 ? U.C(38, 118, 50) : U.C(52, 136, 58));
                    break;
                case 1:
                    if (p.Kind < 55)
                    {
                        DrawCylinder(b, 0.4f * s, 0.5f * s, 3.4f * s, 6, U.C(70, 140, 80));
                        DrawCube(b + new Vector3(0.8f * s, 1.8f * s, 0), 1.0f * s, 0.35f * s, 0.35f * s, U.C(70, 140, 80));
                        DrawCube(b + new Vector3(1.2f * s, 2.3f * s, 0), 0.35f * s, 1.2f * s, 0.35f * s, U.C(62, 128, 72));
                    }
                    else
                    {
                        DrawCube(b + new Vector3(0, 0.7f * s, 0), 2.6f * s, 1.4f * s, 2.2f * s, U.C(170, 130, 90));
                        DrawCube(b + new Vector3(0.6f * s, 1.5f * s, 0.2f), 1.4f * s, 1.0f * s, 1.3f * s, U.C(150, 112, 78));
                    }
                    break;
                case 2:
                    DrawCylinder(b, 0.3f * s, 0.4f * s, 2f * s, 6, U.C(90, 70, 55));
                    DrawCylinder(b + new Vector3(0, 1.6f * s, 0), 0, 2.4f * s, 5.5f * s, 7, U.C(40, 96, 78));
                    DrawCylinder(b + new Vector3(0, 4.6f * s, 0), 0, 1.15f * s, 2.6f * s, 7, U.C(245, 248, 252));
                    break;
                case 3:
                    DrawCube(b + new Vector3(0, p.H / 2, 0), p.W, p.H, p.D, p.C);
                    DrawCubeWires(b + new Vector3(0, p.H / 2, 0), p.W, p.H, p.D, U.Shade(p.C, 0.5f));
                    Color win = p.Kind % 3 == 0 ? U.C(90, 220, 255) : p.Kind % 3 == 1 ? U.C(255, 220, 110) : U.C(255, 110, 210);
                    for (float y = 4; y < p.H - 2; y += 6)
                        DrawCube(b + new Vector3(0, y, 0), p.W + 0.2f, 1.0f, p.D + 0.2f, win);
                    break;
                default:
                    DrawCylinder(b, 0.3f * s, 0.4f * s, 2.6f * s, 6, U.C(92, 62, 44));
                    DrawCylinder(b + new Vector3(0, 2.1f * s, 0), 0.4f * s, 2.2f * s, 4.2f * s, 7,
                        p.Kind % 3 == 0 ? U.C(220, 110, 40) : p.Kind % 3 == 1 ? U.C(200, 70, 50) : U.C(230, 170, 50));
                    break;
            }
        }
    }

    public void DrawMini(int x, int y, int size, Color col, float thick)
    {
        float sc = MiniScale(size);
        for (int i = 0; i < N; i += 2)
            DrawLineEx(MiniPt(P[i], x, y, size, sc), MiniPt(P[(i + 2) % N], x, y, size, sc), thick, col);
    }

    public float MiniScale(int size) => size / MathF.Max(Max.X - Min.X, Max.Y - Min.Y);

    public Vector2 MiniPt(Vector2 p, int x, int y, int size, float sc)
    {
        float ox = (size - (Max.X - Min.X) * sc) / 2, oy = (size - (Max.Y - Min.Y) * sc) / 2;
        return new Vector2(x + ox + (p.X - Min.X) * sc, y + oy + (p.Y - Min.Y) * sc);
    }
}
