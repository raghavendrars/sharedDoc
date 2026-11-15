using System;

namespace Blockhaven
{
    /// <summary>Paints every block texture in code (16x16 RGBA each), so the game ships with no image files.</summary>
    public static class TextureGen
    {
        public const int S = 16;

        static uint H(int layer, int x, int y, int salt = 0) => Hashing.Hash(x, y, layer * 31 + salt, 1234567);
        static float R(int layer, int x, int y, int salt = 0) => (H(layer, x, y, salt) & 0xFFFF) / 65535f; // 0..1

        static void Put(byte[] d, int layer, int x, int y, float r, float g, float b, int a = 255)
        {
            int i = ((layer * S + y) * S + x) * 4;
            d[i] = (byte)Math.Clamp(r, 0, 255);
            d[i + 1] = (byte)Math.Clamp(g, 0, 255);
            d[i + 2] = (byte)Math.Clamp(b, 0, 255);
            d[i + 3] = (byte)a;
        }

        delegate (float r, float g, float b, int a) Px(int x, int y);

        static void Fill(byte[] d, int layer, Px f)
        {
            for (int y = 0; y < S; y++)
                for (int x = 0; x < S; x++)
                {
                    var (r, g, b, a) = f(x, y);
                    Put(d, layer, x, y, r, g, b, a);
                }
        }

        static (float, float, float, int) Speckle(int layer, int x, int y, float r, float g, float b, float amount)
        {
            float n = (R(layer, x, y) - 0.5f) * 2 * amount;
            return (r + n, g + n, b + n, 255);
        }

        static (float, float, float, int) StonePx(int layer, int x, int y)
        {
            float blot = (R(T.Stone, x / 3, y / 2, 5) - 0.5f) * 16;
            float n = (R(T.Stone, x, y) - 0.5f) * 14;
            float v = 126 + blot + n;
            return (v, v, v + 3, 255);
        }

        static (float, float, float, int) DirtPx(int x, int y)
        {
            float n = (R(T.Dirt, x, y) - 0.5f) * 30;
            if (R(T.Dirt, x, y, 9) > 0.92f) n -= 28;
            return (134 + n, 96 + n * 0.8f, 66 + n * 0.6f, 255);
        }

        static (float, float, float, int) GrassPx(int x, int y)
        {
            float n = (R(T.GrassTop, x, y) - 0.5f) * 36;
            return (88 + n * 0.6f, 164 + n, 64 + n * 0.5f, 255);
        }

        static void Ore(byte[] d, int layer, float r, float g, float b)
        {
            Fill(d, layer, (x, y) => StonePx(layer, x, y));
            for (int k = 0; k < 7; k++)
            {
                int cx = 1 + (int)(H(layer, k, 0, 77) % 13), cy = 1 + (int)(H(layer, k, 1, 77) % 13);
                for (int dy = 0; dy < 2; dy++)
                    for (int dx = 0; dx < 2; dx++)
                    {
                        if (H(layer, k, dx + dy * 2, 78) % 5 == 0) continue;
                        float s = dy == 0 ? 1f : 0.8f;
                        Put(d, layer, cx + dx, cy + dy, r * s, g * s, b * s);
                    }
            }
        }

        static void Solid(byte[] d, int layer, float r, float g, float b)
        {
            Fill(d, layer, (x, y) =>
            {
                float n = (R(layer, x, y) - 0.5f) * 10;
                float e = (x == 0 || y == 0 || x == S - 1 || y == S - 1) ? 0.86f : 1f;
                return ((r + n) * e, (g + n) * e, (b + n) * e, 255);
            });
        }

        public static byte[] Build()
        {
            var d = new byte[T.Count * S * S * 4];

            Fill(d, T.GrassTop, GrassPx);
            Fill(d, T.Dirt, DirtPx);
            Fill(d, T.GrassSide, (x, y) => y < 3 + (int)(H(T.GrassSide, x, 0) % 3) ? GrassPx(x, y) : DirtPx(x, y));
            Fill(d, T.Stone, (x, y) => StonePx(T.Stone, x, y));
            Fill(d, T.Sand, (x, y) => { float n = (R(T.Sand, x, y) - 0.5f) * 20; return (224 + n, 208 + n, 152 + n, 255); });
            Fill(d, T.Water, (x, y) =>
            {
                float n = (R(T.Water, x, y) - 0.5f) * 14;
                float wave = ((x + y * 3) % 8 == 0) ? 26 : 0;
                return (46 + n + wave, 112 + n + wave, 206 + n + wave * 0.5f, 255);
            });
            Fill(d, T.LogSide, (x, y) =>
            {
                float stripe = (R(T.LogSide, x, 0, 3) - 0.5f) * 34;
                float n = (R(T.LogSide, x, y) - 0.5f) * 12;
                return (104 + stripe + n, 80 + stripe * 0.8f + n, 48 + stripe * 0.5f + n, 255);
            });
            Fill(d, T.LogTop, (x, y) =>
            {
                int ring = (int)Math.Max(Math.Abs(x - 7.5f), Math.Abs(y - 7.5f));
                float n = (R(T.LogTop, x, y) - 0.5f) * 10;
                if (ring >= 7) return (96 + n, 72 + n, 44 + n, 255);
                float v = ring % 2 == 0 ? 0 : -26;
                return (186 + v + n, 150 + v + n, 96 + v * 0.7f + n, 255);
            });
            Fill(d, T.Leaves, (x, y) =>
            {
                float r = R(T.Leaves, x, y);
                if (r > 0.8f) return (30, 84, 30, 255);
                float n = (r - 0.4f) * 50;
                return (50 + n * 0.5f, 124 + n, 46 + n * 0.4f, 255);
            });
            Fill(d, T.Planks, (x, y) =>
            {
                float n = (R(T.Planks, x, y / 4, 2) - 0.5f) * 14 + (R(T.Planks, x, y) - 0.5f) * 8;
                bool seam = y % 4 == 3 || (x == (int)(H(T.Planks, y / 4, 0, 6) % 16));
                float s = seam ? 0.72f : 1f;
                return ((178 + n) * s, (140 + n) * s, 86 * s + n * 0.5f, 255);
            });
            // Cobblestone: nearest-point cells with dark gaps between the stones.
            {
                const int N = 11;
                var px = new float[N]; var py = new float[N]; var tone = new float[N];
                for (int k = 0; k < N; k++)
                {
                    px[k] = H(T.Cobble, k, 0, 40) % 160 / 10f;
                    py[k] = H(T.Cobble, k, 1, 40) % 160 / 10f;
                    tone[k] = 100 + H(T.Cobble, k, 2, 40) % 60;
                }
                Fill(d, T.Cobble, (x, y) =>
                {
                    float best = 1e9f, second = 1e9f; int bi = 0;
                    for (int k = 0; k < N; k++)
                    {
                        float dx = Math.Abs(x + 0.5f - px[k]), dy = Math.Abs(y + 0.5f - py[k]);
                        if (dx > 8) dx = 16 - dx;
                        if (dy > 8) dy = 16 - dy;
                        float dist = dx * dx + dy * dy;
                        if (dist < best) { second = best; best = dist; bi = k; }
                        else if (dist < second) second = dist;
                    }
                    float v = tone[bi] + (R(T.Cobble, x, y) - 0.5f) * 12;
                    if (MathF.Sqrt(second) - MathF.Sqrt(best) < 0.9f) v = 62;
                    return (v, v, v + 2, 255);
                });
            }
            Fill(d, T.Glass, (x, y) =>
            {
                bool border = x == 0 || y == 0 || x == S - 1 || y == S - 1;
                bool shine = (x + y == 6 && x > 1 && x < 5) || (x + y == 9 && x > 2 && x < 7);
                if (border) return (196, 224, 236, 255);
                if (shine) return (240, 250, 255, 255);
                return (210, 235, 245, 0);
            });
            Fill(d, T.Brick, (x, y) =>
            {
                int row = y / 4;
                bool mortar = y % 4 == 3 || (x + (row % 2) * 4) % 8 == 7;
                if (mortar) return (192, 186, 174, 255);
                float n = (R(T.Brick, (x + (row % 2) * 4) / 8, row, 3) - 0.5f) * 30 + (R(T.Brick, x, y) - 0.5f) * 10;
                return (164 + n, 74 + n * 0.6f, 58 + n * 0.5f, 255);
            });
            Fill(d, T.Snow, (x, y) => { float n = (R(T.Snow, x, y) - 0.5f) * 10; return (240 + n, 245 + n, 250 + n, 255); });
            Ore(d, T.CoalOre, 38, 38, 40);
            Ore(d, T.IronOre, 214, 160, 118);
            Ore(d, T.GemOre, 70, 226, 214);
            Fill(d, T.Bedrock, (x, y) => { float n = (R(T.Bedrock, x, y) - 0.5f) * 70; return (72 + n, 72 + n, 76 + n, 255); });
            Fill(d, T.Gravel, (x, y) => { float n = (R(T.Gravel, x / 2, y / 2, 4) - 0.5f) * 44 + (R(T.Gravel, x, y) - 0.5f) * 20; return (132 + n, 126 + n, 122 + n, 255); });
            Solid(d, T.Red, 204, 62, 56);
            Solid(d, T.Blue, 58, 96, 208);
            Solid(d, T.Yellow, 238, 202, 62);
            Solid(d, T.White, 236, 236, 236);
            return d;
        }
    }
}
