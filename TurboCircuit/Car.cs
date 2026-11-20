using System.Numerics;
using Raylib_cs;
using static Raylib_cs.Raylib;

namespace TurboCircuit;

class Car
{
    public int Model;
    public Stats S;
    public Color Col;
    public string Name;
    public int Player = -1;          // -1 = computer driver
    public float Skill = 0.85f, Lane;

    public Vector2 Pos, Vel;
    public float H, Steer, Lat, Nitro, HitWall, StuckT, WheelSpin;
    public int Idx, Lap;
    public bool NitroOn, Braking, Finished;
    public float FinishTime;

    public Car(int model, Stats s, Color col, string name)
    {
        Model = model; S = s; Col = col; Name = name; Nitro = s.NitroCap;
    }

    public Vector2 Fwd => new(MathF.Sin(H), MathF.Cos(H));
    public Vector2 Left => new(MathF.Cos(H), -MathF.Sin(H));
    public float Speed => Vel.Length();
    public float Progress(Track tr) => Lap * tr.N + Idx;

    public void Place(Track tr, int idx, float lat)
    {
        idx = ((idx % tr.N) + tr.N) % tr.N;
        Idx = idx;
        Pos = tr.P[idx] + tr.Nrm[idx] * lat;
        H = MathF.Atan2(tr.T[idx].X, tr.T[idx].Y);
        Vel = Vector2.Zero; Lat = lat; Steer = 0; StuckT = 0;
    }

    /// <param name="thr">-1 brake/reverse .. +1 full throttle</param>
    /// <param name="steerIn">+1 left .. -1 right</param>
    public void Update(Track tr, float dt, float thr, float steerIn, bool nitro)
    {
        Steer += (steerIn - Steer) * MathF.Min(1, dt * 8);
        float vF0 = Vector2.Dot(Vel, Fwd);
        bool off = MathF.Abs(Lat) > tr.Def.HalfWidth + 1.2f;
        float grip = S.Grip * (off ? 0.55f : 1f);
        float maxYaw = MathF.Min(2.2f, grip / MathF.Max(MathF.Abs(vF0), 4f));
        H = U.Wrap(H + Steer * maxYaw * dt * Math.Clamp(vF0 / 4f, -1f, 1f));

        Vector2 fwd = Fwd, left = Left;
        float vF = Vector2.Dot(Vel, fwd), vL = Vector2.Dot(Vel, left);

        NitroOn = nitro && Nitro > 0 && thr > 0 && S.NitroCap > 0;
        if (NitroOn) Nitro = MathF.Max(0, Nitro - dt);
        else Nitro = MathF.Min(S.NitroCap, Nitro + dt * S.NitroCap * 0.04f);

        float maxV = S.MaxSpeed * (NitroOn ? 1.22f : 1f);
        float acc = S.Accel * (NitroOn ? 1.7f : 1f);
        Braking = false;
        if (thr > 0)
        {
            float r = vF / maxV;
            vF += acc * thr * MathF.Max(0, 1 - r * r) * dt;
        }
        else if (thr < 0)
        {
            if (vF > 1f) { vF = MathF.Max(0, vF + S.Brake * thr * dt); Braking = true; }
            else vF = MathF.Max(-10f, vF + acc * 0.6f * thr * dt);
        }
        else
        {
            float dec = (1.5f + 0.03f * MathF.Abs(vF)) * dt;
            vF = MathF.Abs(vF) <= dec ? 0 : vF - MathF.Sign(vF) * dec;
        }
        if (vF > maxV) vF -= (vF - maxV) * MathF.Min(1, 1.5f * dt);
        if (off && MathF.Abs(vF) > 14f) vF -= MathF.Sign(vF) * (MathF.Abs(vF) - 14f) * MathF.Min(1, 2.2f * dt);

        vL *= MathF.Exp(-7f * dt);
        Vel = fwd * vF + left * vL;
        Pos += Vel * dt;
        WheelSpin += vF * dt * 2.5f;

        tr.Locate(ref Idx, ref Lap, Pos);
        Vector2 n = tr.Nrm[Idx];
        Lat = Vector2.Dot(Pos - tr.P[Idx], n);
        float lim = tr.WallDist - 1.0f;
        HitWall = 0;
        if (MathF.Abs(Lat) > lim)
        {
            float s = MathF.Sign(Lat);
            Pos -= n * s * (MathF.Abs(Lat) - lim);
            float vn = Vector2.Dot(Vel, n) * s;
            if (vn > 0)
            {
                Vel -= n * s * vn * 1.3f;
                Vel *= 0.93f;
                HitWall = vn;
            }
            Lat = s * lim;
        }
    }

    public static void Draw(CarModel m, Vector2 pos, float h, Color col, float steer, bool braking, bool nitro, float spin)
    {
        Rlgl.PushMatrix();
        Rlgl.Translatef(pos.X, 0, pos.Y);
        Rlgl.Rotatef(h * (180f / MathF.PI), 0, 1, 0);

        float wr = m.WheelR, by = wr * 0.75f, L = m.Len, W = m.Wid;
        Color dark = U.Shade(col, 0.55f), glass = U.C(30, 40, 58), trim = U.C(24, 24, 28);

        // chassis: sill, main body, tapered nose and tail
        DrawCube(new Vector3(0, by + 0.09f, 0), W * 0.98f, 0.18f, L * 0.98f, trim);
        DrawCube(new Vector3(0, by + m.BodyH / 2 + 0.1f, 0), W, m.BodyH - 0.1f, L * 0.80f, col);
        DrawCube(new Vector3(0, by + m.BodyH * 0.40f + 0.1f, L * 0.42f), W * 0.94f, m.BodyH * 0.72f, L * 0.16f, U.Shade(col, 0.9f));
        DrawCube(new Vector3(0, by + m.BodyH * 0.45f + 0.1f, -L * 0.43f), W * 0.96f, m.BodyH * 0.8f, L * 0.14f, U.Shade(col, 0.82f));
        DrawCubeWires(new Vector3(0, by + m.BodyH / 2 + 0.1f, 0), W, m.BodyH - 0.1f, L * 0.80f, dark);
        // bonnet stripe
        DrawCube(new Vector3(0, by + m.BodyH + 0.055f, L * 0.2f), W * 0.22f, 0.02f, L * 0.36f, U.Shade(col, 1.35f));

        // cabin
        float cy = by + m.BodyH + m.CabH / 2;
        DrawCube(new Vector3(0, cy, m.CabOff), W * 0.84f, m.CabH, m.CabLen, glass);
        DrawCube(new Vector3(0, cy + m.CabH / 2 + 0.03f, m.CabOff), W * 0.80f, 0.07f, m.CabLen * 0.78f, col);
        DrawCubeWires(new Vector3(0, cy, m.CabOff), W * 0.84f, m.CabH, m.CabLen, dark);

        // lights
        Color tail = braking ? U.C(255, 40, 40) : U.C(120, 16, 16);
        for (int s = -1; s <= 1; s += 2)
        {
            DrawCube(new Vector3(s * W * 0.33f, by + m.BodyH * 0.55f, L * 0.5f), 0.34f, 0.14f, 0.06f, U.C(255, 245, 190));
            DrawCube(new Vector3(s * W * 0.33f, by + m.BodyH * 0.62f, -L * 0.5f), 0.38f, 0.13f, 0.06f, tail);
        }

        if (m.Spoiler)
        {
            float sy = by + m.BodyH + 0.42f;
            DrawCube(new Vector3(0, sy, -L * 0.45f), W * 1.02f, 0.06f, 0.42f, trim);
            DrawCube(new Vector3(W * 0.36f, sy - 0.2f, -L * 0.45f), 0.07f, 0.4f, 0.2f, trim);
            DrawCube(new Vector3(-W * 0.36f, sy - 0.2f, -L * 0.45f), 0.07f, 0.4f, 0.2f, trim);
        }

        // wheels (front pair turn with the steering)
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2)
            {
                Rlgl.PushMatrix();
                Rlgl.Translatef(sx * (W / 2 - 0.05f), wr, sz * L * 0.31f);
                if (sz > 0) Rlgl.Rotatef(steer * 26f, 0, 1, 0);
                DrawCylinderEx(new Vector3(-0.16f, 0, 0), new Vector3(0.16f, 0, 0), wr, wr, 10, U.C(22, 22, 24));
                Rlgl.Rotatef(spin * (180f / MathF.PI), 1, 0, 0);
                DrawCube(new Vector3(sx * 0.165f, 0, 0), 0.02f, wr * 1.3f, wr * 0.35f, U.C(190, 190, 200));
                DrawCube(new Vector3(sx * 0.165f, 0, 0), 0.02f, wr * 0.35f, wr * 1.3f, U.C(190, 190, 200));
                Rlgl.PopMatrix();
            }

        if (nitro)
        {
            float fl = 0.7f + 0.5f * MathF.Abs(MathF.Sin(spin * 3f));
            for (int s = -1; s <= 1; s += 2)
            {
                DrawCube(new Vector3(s * 0.35f, by + 0.22f, -L * 0.5f - fl / 2), 0.2f, 0.2f, fl, U.C(80, 190, 255));
                DrawCube(new Vector3(s * 0.35f, by + 0.22f, -L * 0.5f - fl * 0.3f), 0.12f, 0.12f, fl * 0.6f, Color.White);
            }
        }
        Rlgl.PopMatrix();
    }
}
