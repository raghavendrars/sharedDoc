using System;
using OpenTK.Mathematics;

namespace Blockhaven
{
    public sealed class Player
    {
        public const double HalfW = 0.3, Height = 1.8, Eye = 1.62;
        public Vector3d Pos, Vel;
        public float Yaw, Pitch;
        public bool OnGround, Flying, InWater, HeadInWater;
        public float Health = 20, Air = 10;
        public bool Creative;
        public readonly int[] Counts = new int[B.Count];
        public readonly byte[] Hotbar = new byte[9];
        public int Selected;
        public float HurtFlash;
        double fallTop;
        float regenTimer, drownTimer;

        public Vector3d EyePos => new Vector3d(Pos.X, Pos.Y + Eye, Pos.Z);

        public Vector3 Forward
        {
            get
            {
                float cp = MathF.Cos(Pitch);
                return new Vector3(MathF.Sin(Yaw) * cp, MathF.Sin(Pitch), -MathF.Cos(Yaw) * cp);
            }
        }

        static int Fl(double v) => (int)Math.Floor(v);

        bool Collides(World w, double x, double y, double z)
        {
            int x0 = Fl(x - HalfW), x1 = Fl(x + HalfW), y0 = Fl(y), y1 = Fl(y + Height), z0 = Fl(z - HalfW), z1 = Fl(z + HalfW);
            for (int by = y0; by <= y1; by++)
                for (int bz = z0; bz <= z1; bz++)
                    for (int bx = x0; bx <= x1; bx++)
                        if (Blocks.Solid[w.GetBlock(bx, by, bz)]) return true;
            return false;
        }

        public bool IntersectsBlock(int bx, int by, int bz) =>
            bx + 1 > Pos.X - HalfW && bx < Pos.X + HalfW && by + 1 > Pos.Y && by < Pos.Y + Height && bz + 1 > Pos.Z - HalfW && bz < Pos.Z + HalfW;

        /// <summary>Returns damage taken this step (survival only).</summary>
        public float Update(World w, float dt, Vector3 wish, bool jump, bool descend, bool sprint)
        {
            float damage = 0;
            const double eps = 1e-4;
            InWater = w.GetBlock(Fl(Pos.X), Fl(Pos.Y + 0.1), Fl(Pos.Z)) == B.Water || w.GetBlock(Fl(Pos.X), Fl(Pos.Y + 0.9), Fl(Pos.Z)) == B.Water;
            HeadInWater = w.GetBlock(Fl(Pos.X), Fl(Pos.Y + Eye), Fl(Pos.Z)) == B.Water;

            if (Flying)
            {
                float sp = sprint ? 24f : 11f;
                float k = MathF.Min(1, 10 * dt);
                Vel.X += (wish.X * sp - Vel.X) * k;
                Vel.Z += (wish.Z * sp - Vel.Z) * k;
                float vy = (jump ? 1 : 0) - (descend ? 1 : 0);
                Vel.Y += (vy * sp * 0.8f - Vel.Y) * k;
            }
            else
            {
                float sp = InWater ? 2.6f : sprint ? 6.2f : 4.4f;
                float k = MathF.Min(1, (OnGround ? 14f : InWater ? 6f : 3.5f) * dt);
                Vel.X += (wish.X * sp - Vel.X) * k;
                Vel.Z += (wish.Z * sp - Vel.Z) * k;
                if (InWater)
                {
                    Vel.Y -= 7 * dt;
                    Vel.Y *= 1 - MathF.Min(1, 2.5f * dt);
                    if (jump) Vel.Y = Math.Min(Vel.Y + 26 * dt, 3.4);
                }
                else
                {
                    Vel.Y -= 28 * dt;
                    if (jump && OnGround) Vel.Y = 8.4;
                }
                if (Vel.Y < -55) Vel.Y = -55;
            }

            Vector3d move = Vel * dt;
            int steps = Math.Max(1, (int)Math.Ceiling(Math.Max(Math.Abs(move.X), Math.Max(Math.Abs(move.Y), Math.Abs(move.Z))) / 0.4));
            Vector3d step = move / steps;
            bool wasOnGround = OnGround;
            OnGround = false;
            for (int s = 0; s < steps; s++)
            {
                double nx = Pos.X + step.X;
                if (Collides(w, nx, Pos.Y, Pos.Z))
                {
                    nx = step.X > 0 ? Fl(nx + HalfW) - HalfW - eps : Fl(nx - HalfW) + 1 + HalfW + eps;
                    Vel.X = 0; step.X = 0;
                }
                Pos.X = nx;

                double nz = Pos.Z + step.Z;
                if (Collides(w, Pos.X, Pos.Y, nz))
                {
                    nz = step.Z > 0 ? Fl(nz + HalfW) - HalfW - eps : Fl(nz - HalfW) + 1 + HalfW + eps;
                    Vel.Z = 0; step.Z = 0;
                }
                Pos.Z = nz;

                double ny = Pos.Y + step.Y;
                if (Collides(w, Pos.X, ny, Pos.Z))
                {
                    if (step.Y < 0) { ny = Fl(ny) + 1 + eps; OnGround = true; }
                    else ny = Fl(ny + Height) - Height - eps;
                    Vel.Y = 0; step.Y = 0;
                }
                Pos.Y = ny;
            }
            if (Pos.Y > Chunk.SY + 60) { Pos.Y = Chunk.SY + 60; if (Vel.Y > 0) Vel.Y = 0; }

            // Fall damage, drowning and slow healing.
            if (Flying || InWater) fallTop = Pos.Y;
            else if (OnGround)
            {
                double fell = fallTop - Pos.Y;
                if (!wasOnGround && fell > 3.5 && !Creative) damage += (float)Math.Floor(fell - 3);
                fallTop = Pos.Y;
            }
            else if (Pos.Y > fallTop) fallTop = Pos.Y;

            if (!Creative)
            {
                if (HeadInWater)
                {
                    Air -= dt;
                    if (Air <= 0)
                    {
                        drownTimer += dt;
                        if (drownTimer >= 1) { drownTimer = 0; damage += 2; }
                    }
                }
                else { Air = MathF.Min(10, Air + dt * 4); drownTimer = 0; }

                regenTimer += dt;
                if (regenTimer >= 4) { regenTimer = 0; if (Health < 20) Health = MathF.Min(20, Health + 1); }
                if (damage > 0) { Health -= damage; HurtFlash = 0.4f; regenTimer = 0; }
            }
            else { Air = 10; Health = 20; }
            if (HurtFlash > 0) HurtFlash -= dt;
            return damage;
        }

        public void ResetFall() => fallTop = Pos.Y;

        public void Give(byte id, int n)
        {
            if (id == B.Air) return;
            Counts[id] += n;
            for (int i = 0; i < 9; i++) if (Hotbar[i] == id) return;
            for (int i = 0; i < 9; i++) if (Hotbar[i] == 0) { Hotbar[i] = id; return; }
        }

        public bool CanCraft(Recipe r)
        {
            foreach (var (id, n) in r.In) if (Counts[id] < n) return false;
            return true;
        }

        public void Craft(Recipe r)
        {
            if (!CanCraft(r)) return;
            foreach (var (id, n) in r.In) Counts[id] -= n;
            Give(r.Out, r.OutCount);
        }
    }

    public struct RayHit
    {
        public bool Hit;
        public int X, Y, Z, PX, PY, PZ;
        public byte Block;
    }

    public static class Ray
    {
        /// <summary>Walks the voxel grid along a ray and returns the first solid block plus the empty cell before it.</summary>
        public static RayHit Cast(World w, Vector3d o, Vector3 dir, float maxDist)
        {
            int x = (int)Math.Floor(o.X), y = (int)Math.Floor(o.Y), z = (int)Math.Floor(o.Z);
            int sx = dir.X > 0 ? 1 : -1, sy = dir.Y > 0 ? 1 : -1, sz = dir.Z > 0 ? 1 : -1;
            double tdx = dir.X == 0 ? double.MaxValue : Math.Abs(1.0 / dir.X);
            double tdy = dir.Y == 0 ? double.MaxValue : Math.Abs(1.0 / dir.Y);
            double tdz = dir.Z == 0 ? double.MaxValue : Math.Abs(1.0 / dir.Z);
            double tx = dir.X == 0 ? double.MaxValue : (dir.X > 0 ? x + 1 - o.X : o.X - x) * tdx;
            double ty = dir.Y == 0 ? double.MaxValue : (dir.Y > 0 ? y + 1 - o.Y : o.Y - y) * tdy;
            double tz = dir.Z == 0 ? double.MaxValue : (dir.Z > 0 ? z + 1 - o.Z : o.Z - z) * tdz;
            int px = x, py = y, pz = z;
            double t = 0;
            while (t <= maxDist)
            {
                byte b = w.GetBlock(x, y, z);
                if (Blocks.Solid[b])
                    return new RayHit { Hit = true, X = x, Y = y, Z = z, PX = px, PY = py, PZ = pz, Block = b };
                px = x; py = y; pz = z;
                if (tx < ty && tx < tz) { x += sx; t = tx; tx += tdx; }
                else if (ty < tz) { y += sy; t = ty; ty += tdy; }
                else { z += sz; t = tz; tz += tdz; }
            }
            return default;
        }
    }
}
