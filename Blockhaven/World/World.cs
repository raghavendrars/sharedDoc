using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Text.Json;

namespace Blockhaven
{
    public sealed class Chunk
    {
        public const int SX = 16, SY = 128, SZ = 16;
        public readonly int CX, CZ;
        public readonly byte[] Blocks = new byte[SX * SY * SZ];
        public volatile bool Generated;
        public bool Modified;

        // Mesh state: only touched on the main thread.
        public bool MeshDirty = true, Meshing;
        public int MeshVersion;
        public int Vao, Vbo, OpaqueQuads, WaterQuads;
        public bool InRenderList;

        public Chunk(int cx, int cz) { CX = cx; CZ = cz; }
        public static int Idx(int x, int y, int z) => (y * SZ + z) * SX + x;
    }

    public sealed class WorldMeta
    {
        public int Seed { get; set; }
        public bool HasPlayer { get; set; }
        public double PX { get; set; }
        public double PY { get; set; }
        public double PZ { get; set; }
        public float Yaw { get; set; }
        public float Pitch { get; set; }
        public float TimeOfDay { get; set; } = 0.08f;
        public float Health { get; set; } = 20;
        public int[] Counts { get; set; }
        public int[] Hotbar { get; set; }
        public int Selected { get; set; }
    }

    public sealed class World
    {
        public readonly string Dir;
        public readonly WorldMeta Meta;
        public readonly TerrainGen Gen;
        public bool SavingEnabled = true;
        readonly ConcurrentDictionary<long, Chunk> chunks = new ConcurrentDictionary<long, Chunk>();
        readonly string chunkDir;
        Chunk last;

        public World(string dir)
        {
            Dir = dir;
            chunkDir = Path.Combine(dir, "chunks");
            Directory.CreateDirectory(chunkDir);
            string metaPath = Path.Combine(dir, "world.json");
            try
            {
                if (File.Exists(metaPath)) Meta = JsonSerializer.Deserialize<WorldMeta>(File.ReadAllText(metaPath));
            }
            catch { }
            if (Meta == null) Meta = new WorldMeta { Seed = new Random().Next(1, int.MaxValue) };
            Gen = new TerrainGen(Meta.Seed);
        }

        static long Key(int cx, int cz) => ((long)cx << 32) | (uint)cz;

        public ICollection<Chunk> Chunks => chunks.Values;
        public int ChunkCount => chunks.Count;

        public Chunk GetChunk(int cx, int cz) => chunks.TryGetValue(Key(cx, cz), out var c) ? c : null;

        public Chunk CreateChunk(int cx, int cz)
        {
            var c = new Chunk(cx, cz);
            chunks[Key(cx, cz)] = c;
            return c;
        }

        public void Remove(Chunk c)
        {
            chunks.TryRemove(Key(c.CX, c.CZ), out _);
            if (last == c) last = null;
        }

        public bool NeighborsReady(int cx, int cz)
        {
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++)
                {
                    var n = GetChunk(cx + dx, cz + dz);
                    if (n == null || !n.Generated) return false;
                }
            return true;
        }

        /// <summary>Main-thread block lookup (uses a one-entry cache).</summary>
        public byte GetBlock(int x, int y, int z)
        {
            if (y < 0) return B.Bedrock;
            if (y >= Chunk.SY) return B.Air;
            int cx = x >> 4, cz = z >> 4;
            var c = last;
            if (c == null || c.CX != cx || c.CZ != cz)
            {
                c = GetChunk(cx, cz);
                if (c == null) return B.Air;
                last = c;
            }
            return c.Generated ? c.Blocks[Chunk.Idx(x & 15, y, z & 15)] : B.Air;
        }

        public bool IsLoaded(int x, int z)
        {
            var c = GetChunk(x >> 4, z >> 4);
            return c != null && c.Generated;
        }

        /// <summary>Sets a block. Returns the owning chunk, or null if it is not loaded.</summary>
        public Chunk SetBlock(int x, int y, int z, byte b)
        {
            if (y < 0 || y >= Chunk.SY) return null;
            var c = GetChunk(x >> 4, z >> 4);
            if (c == null || !c.Generated) return null;
            c.Blocks[Chunk.Idx(x & 15, y, z & 15)] = b;
            c.Modified = true;
            return c;
        }

        string ChunkPath(int cx, int cz) => Path.Combine(chunkDir, $"c_{cx}_{cz}.bin");

        /// <summary>Runs on a worker thread.</summary>
        public void LoadOrGenerate(Chunk c)
        {
            try
            {
                string p = ChunkPath(c.CX, c.CZ);
                bool loaded = false;
                if (File.Exists(p))
                {
                    try
                    {
                        using var fs = File.OpenRead(p);
                        using var gz = new GZipStream(fs, CompressionMode.Decompress);
                        int off = 0;
                        while (off < c.Blocks.Length)
                        {
                            int n = gz.Read(c.Blocks, off, c.Blocks.Length - off);
                            if (n <= 0) break;
                            off += n;
                        }
                        loaded = off == c.Blocks.Length;
                    }
                    catch { loaded = false; }
                    if (!loaded) Array.Clear(c.Blocks, 0, c.Blocks.Length);
                }
                if (!loaded) Gen.Generate(c);
            }
            finally
            {
                c.Generated = true;
            }
        }

        public void SaveChunk(Chunk c)
        {
            if (!SavingEnabled || !c.Modified || !c.Generated) return;
            try
            {
                string p = ChunkPath(c.CX, c.CZ), tmp = p + ".tmp";
                using (var fs = File.Create(tmp))
                using (var gz = new GZipStream(fs, CompressionLevel.Fastest))
                    gz.Write(c.Blocks, 0, c.Blocks.Length);
                File.Move(tmp, p, true);
                c.Modified = false;
            }
            catch { }
        }

        public void SaveAll()
        {
            if (!SavingEnabled) return;
            foreach (var c in chunks.Values) SaveChunk(c);
            try
            {
                File.WriteAllText(Path.Combine(Dir, "world.json"), JsonSerializer.Serialize(Meta));
            }
            catch { }
        }
    }
}
