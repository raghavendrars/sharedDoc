using System;

namespace Blockhaven
{
    public sealed class TerrainGen
    {
        public const int Sea = 46;
        readonly int seed;
        readonly Perlin cont, hills, mount, forest, cave1, cave2;

        public TerrainGen(int seed)
        {
            this.seed = seed;
            cont = new Perlin(seed);
            hills = new Perlin(seed + 101);
            mount = new Perlin(seed + 202);
            forest = new Perlin(seed + 303);
            cave1 = new Perlin(seed + 404);
            cave2 = new Perlin(seed + 505);
        }

        public int Height(int x, int z)
        {
            float c = cont.Fbm2(x * 0.0035f, z * 0.0035f, 4);
            float h = hills.Fbm2(x * 0.013f, z * 0.013f, 4);
            float m = MathF.Max(0, mount.Fbm2(x * 0.0055f, z * 0.0055f, 3) - 0.05f);
            float v = 52 + c * 34 + h * 11 + m * m * 260;
            return Math.Clamp((int)v, 4, 118);
        }

        bool CaveNoise(int x, int y, int z)
        {
            float n1 = cave1.Noise(x * 0.045f, y * 0.07f, z * 0.045f);
            if (n1 > 0.11f || n1 < -0.11f) return false;
            float n2 = cave2.Noise(x * 0.045f + 31.7f, y * 0.07f + 11.3f, z * 0.045f + 5.9f);
            return n1 * n1 + n2 * n2 < 0.012f;
        }

        /// <summary>True when the terrain at this position has been carved out by a cave.</summary>
        public bool IsCarved(int x, int y, int z, int h)
        {
            if (y < 1 || y > h) return false;
            if (h <= Sea + 2 && y >= h - 6) return false; // keep sea and beach floors watertight
            return CaveNoise(x, y, z);
        }

        public static bool Beach(int h) => h <= Sea + 1;

        public void Generate(Chunk c)
        {
            var blocks = c.Blocks;
            int ox = c.CX * Chunk.SX, oz = c.CZ * Chunk.SZ;
            const int M = 2, W = Chunk.SX + M * 2;
            var hm = new int[W * W];
            for (int z = -M; z < Chunk.SZ + M; z++)
                for (int x = -M; x < Chunk.SX + M; x++)
                    hm[(z + M) * W + x + M] = Height(ox + x, oz + z);

            for (int z = 0; z < Chunk.SZ; z++)
                for (int x = 0; x < Chunk.SX; x++)
                {
                    int wx = ox + x, wz = oz + z;
                    int h = hm[(z + M) * W + x + M];
                    bool beach = Beach(h), snowy = h >= 94, rocky = h >= 82;
                    int top = Math.Max(h, Sea);
                    for (int y = 0; y <= top; y++)
                    {
                        byte b;
                        if (y == 0) b = B.Bedrock;
                        else if (y > h) b = B.Water;
                        else if (IsCarved(wx, y, wz, h)) b = B.Air;
                        else if (y == h)
                            b = h < Sea - 6 ? B.Gravel : beach ? B.Sand : snowy ? B.Snow : rocky ? B.Stone : B.Grass;
                        else if (y >= h - 3)
                            b = beach ? B.Sand : (rocky || snowy) ? B.Stone : B.Dirt;
                        else
                        {
                            b = B.Stone;
                            uint r = Hashing.Hash(wx >> 1, y >> 1, wz >> 1, seed) % 1000;
                            if (r < 14 && y < 90) b = B.CoalOre;
                            else if (r < 22 && y < 52) b = B.IronOre;
                            else if (r < 26 && y < 22) b = B.GemOre;
                        }
                        blocks[Chunk.Idx(x, y, z)] = b;
                    }
                }

            // Trees are a pure function of world position, so they line up across chunk borders.
            for (int tz = -M; tz < Chunk.SZ + M; tz++)
                for (int tx = -M; tx < Chunk.SX + M; tx++)
                {
                    int wx = ox + tx, wz = oz + tz;
                    int h = hm[(tz + M) * W + tx + M];
                    if (h <= Sea + 1 || h >= 82) continue;
                    uint r = Hashing.Hash(wx, 7, wz, seed);
                    float density = 3 + 26 * MathF.Max(0, forest.Fbm2(wx * 0.01f, wz * 0.01f, 2) + 0.1f);
                    if (r % 1000 >= density) continue;
                    if (IsCarved(wx, h, wz, h)) continue;
                    int th = 4 + (int)((r >> 12) % 3);
                    for (int dy = th - 2; dy <= th + 1; dy++)
                    {
                        int rad = dy < th ? 2 : 1;
                        for (int dz = -rad; dz <= rad; dz++)
                            for (int dx = -rad; dx <= rad; dx++)
                            {
                                if (Math.Abs(dx) == rad && Math.Abs(dz) == rad && (rad == 1 || ((Hashing.Hash(wx + dx, dy, wz + dz, seed) & 1) == 0))) continue;
                                SetIfAir(blocks, tx + dx, h + dy, tz + dz, B.Leaves);
                            }
                    }
                    if (tx >= 0 && tx < Chunk.SX && tz >= 0 && tz < Chunk.SZ)
                        for (int dy = 1; dy <= th; dy++)
                            if (h + dy < Chunk.SY) blocks[Chunk.Idx(tx, h + dy, tz)] = B.Log;
                }
        }

        static void SetIfAir(byte[] blocks, int x, int y, int z, byte b)
        {
            if (x < 0 || x >= Chunk.SX || z < 0 || z >= Chunk.SZ || y < 0 || y >= Chunk.SY) return;
            int i = Chunk.Idx(x, y, z);
            if (blocks[i] == B.Air) blocks[i] = b;
        }
    }
}
