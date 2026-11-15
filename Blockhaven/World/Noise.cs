using System;

namespace Blockhaven
{
    /// <summary>Classic seeded gradient noise.</summary>
    public sealed class Perlin
    {
        readonly byte[] p = new byte[512];

        public Perlin(int seed)
        {
            var r = new Random(seed);
            var a = new byte[256];
            for (int i = 0; i < 256; i++) a[i] = (byte)i;
            for (int i = 255; i > 0; i--)
            {
                int j = r.Next(i + 1);
                (a[i], a[j]) = (a[j], a[i]);
            }
            for (int i = 0; i < 512; i++) p[i] = a[i & 255];
        }

        static float Fade(float t) => t * t * t * (t * (t * 6 - 15) + 10);
        static float Lerp(float a, float b, float t) => a + (b - a) * t;

        static float Grad(int h, float x, float y, float z)
        {
            h &= 15;
            float u = h < 8 ? x : y;
            float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

        public float Noise(float x, float y, float z)
        {
            int xi = (int)MathF.Floor(x), yi = (int)MathF.Floor(y), zi = (int)MathF.Floor(z);
            x -= xi; y -= yi; z -= zi;
            xi &= 255; yi &= 255; zi &= 255;
            float u = Fade(x), v = Fade(y), w = Fade(z);
            int a = p[xi] + yi, aa = p[a] + zi, ab = p[a + 1] + zi;
            int b = p[xi + 1] + yi, ba = p[b] + zi, bb = p[b + 1] + zi;
            return Lerp(
                Lerp(Lerp(Grad(p[aa], x, y, z), Grad(p[ba], x - 1, y, z), u),
                     Lerp(Grad(p[ab], x, y - 1, z), Grad(p[bb], x - 1, y - 1, z), u), v),
                Lerp(Lerp(Grad(p[aa + 1], x, y, z - 1), Grad(p[ba + 1], x - 1, y, z - 1), u),
                     Lerp(Grad(p[ab + 1], x, y - 1, z - 1), Grad(p[bb + 1], x - 1, y - 1, z - 1), u), v), w);
        }

        public float Fbm2(float x, float z, int octaves)
        {
            float sum = 0, amp = 1, norm = 0;
            for (int i = 0; i < octaves; i++)
            {
                sum += Noise(x, 0.37f + i * 7.1f, z) * amp;
                norm += amp;
                amp *= 0.5f;
                x *= 2.03f; z *= 2.03f;
            }
            return sum / norm;
        }
    }

    public static class Hashing
    {
        public static uint Hash(int x, int y, int z, int seed)
        {
            unchecked
            {
                uint h = (uint)seed * 0x9E3779B1u;
                h ^= (uint)x * 0x85EBCA6Bu; h = (h << 13) | (h >> 19); h *= 0xC2B2AE35u;
                h ^= (uint)y * 0x27D4EB2Fu; h = (h << 15) | (h >> 17); h *= 0x165667B1u;
                h ^= (uint)z * 0x9E3779B1u; h ^= h >> 16; h *= 0x85EBCA6Bu; h ^= h >> 13; h *= 0xC2B2AE35u; h ^= h >> 16;
                return h;
            }
        }
    }
}
