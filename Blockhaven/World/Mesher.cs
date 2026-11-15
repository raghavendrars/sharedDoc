using System;
using System.Buffers;

namespace Blockhaven
{
    public sealed class MeshData
    {
        public uint[] Data;          // rented from ArrayPool; 2 uints per vertex, opaque quads first
        public int OpaqueQuads, WaterQuads;
        public int Version;
    }

    /// <summary>Turns a chunk (plus a one-block border from its neighbours) into packed quads.</summary>
    public static class Mesher
    {
        const int PW = 18, PH = Chunk.SY + 2;
        const int DX = 1, DZ = PW, DY = PW * PW;

        [ThreadStatic] static byte[] pad;
        [ThreadStatic] static int[] hmap;
        [ThreadStatic] static uint[] bufO;
        [ThreadStatic] static uint[] bufW;

        static readonly int[] FaceN = { DX, -DX, DY, -DY, DZ, -DZ };
        static readonly int[] FaceAxis = { 0, 0, 1, 1, 2, 2 };
        static readonly float[] Shade = { 0.80f, 0.80f, 1.0f, 0.50f, 0.66f, 0.66f };
        static readonly float[] AoF = { 0.45f, 0.64f, 0.82f, 1.0f };

        // Corner offsets (x,y,z), counter-clockwise seen from outside.
        static readonly byte[,,] Corner =
        {
            { {1,0,1}, {1,0,0}, {1,1,0}, {1,1,1} }, // +X
            { {0,0,0}, {0,0,1}, {0,1,1}, {0,1,0} }, // -X
            { {0,1,1}, {1,1,1}, {1,1,0}, {0,1,0} }, // +Y
            { {0,0,0}, {1,0,0}, {1,0,1}, {0,0,1} }, // -Y
            { {0,0,1}, {1,0,1}, {1,1,1}, {0,1,1} }, // +Z
            { {1,0,0}, {0,0,0}, {0,1,0}, {1,1,0} }, // -Z
        };
        static readonly byte[,] UV = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } };
        static readonly int[,,] AoD = new int[6, 4, 2];

        static Mesher()
        {
            int[] axisDelta = { DX, DY, DZ };
            for (int f = 0; f < 6; f++)
                for (int k = 0; k < 4; k++)
                {
                    int n = 0;
                    for (int a = 0; a < 3; a++)
                    {
                        if (a == FaceAxis[f]) continue;
                        AoD[f, k, n++] = Corner[f, k, a] == 1 ? axisDelta[a] : -axisDelta[a];
                    }
                }
        }

        static void Grow(ref uint[] buf, int need)
        {
            if (buf.Length >= need) return;
            Array.Resize(ref buf, Math.Max(need, buf.Length * 2));
        }

        public static MeshData Build(World world, Chunk c, bool ao)
        {
            pad ??= new byte[PW * PW * PH];
            hmap ??= new int[PW * PW];
            bufO ??= new uint[1 << 15];
            bufW ??= new uint[1 << 12];
            var p = pad; var hm = hmap;

            Array.Clear(p, 0, p.Length);
            Array.Fill(p, B.Bedrock, 0, DY); // below the world: hides bottom faces

            for (int pz = -1; pz <= 16; pz++)
                for (int px = -1; px <= 16; px++)
                {
                    int sx = px < 0 ? -1 : px > 15 ? 1 : 0, sz = pz < 0 ? -1 : pz > 15 ? 1 : 0;
                    var src = (sx == 0 && sz == 0) ? c : world.GetChunk(c.CX + sx, c.CZ + sz);
                    int col = (pz + 1) * PW + px + 1;
                    int top = -1;
                    if (src != null && src.Generated)
                    {
                        var sb = src.Blocks;
                        int si = Chunk.Idx(px & 15, 0, pz & 15), di = DY + col;
                        for (int y = 0; y < Chunk.SY; y++, si += Chunk.SX * Chunk.SZ, di += DY)
                        {
                            byte b = sb[si];
                            p[di] = b;
                            if (Blocks.Opaque[b]) top = y;
                        }
                    }
                    hm[col] = top;
                }

            int no = 0, nw = 0;
            var opq = Blocks.Opaque;
            for (int y = 0; y < Chunk.SY; y++)
                for (int z = 0; z < Chunk.SZ; z++)
                {
                    int i = (y + 1) * DY + (z + 1) * PW + 1;
                    for (int x = 0; x < Chunk.SX; x++, i++)
                    {
                        byte b = p[i];
                        if (b == B.Air) continue;
                        var info = Blocks.Info[b];
                        bool water = b == B.Water;
                        bool lower = water && p[i + DY] != B.Water;
                        for (int f = 0; f < 6; f++)
                        {
                            int ni = i + FaceN[f];
                            byte n = p[ni];
                            if (opq[n] || n == b) continue;

                            int d = hm[ni % DY] - (ni / DY - 1);
                            float sky = d <= 0 ? 1f : MathF.Max(0.3f, 1f - 0.1f * d);
                            float baseL = Shade[f] * sky * 255f;
                            uint tile = f == 2 ? info.Top : f == 3 ? info.Bottom : info.Side;

                            int a0 = 3, a1 = 3, a2 = 3, a3 = 3;
                            if (ao && !water)
                            {
                                a0 = Ao(p, ni, AoD[f, 0, 0], AoD[f, 0, 1]);
                                a1 = Ao(p, ni, AoD[f, 1, 0], AoD[f, 1, 1]);
                                a2 = Ao(p, ni, AoD[f, 2, 0], AoD[f, 2, 1]);
                                a3 = Ao(p, ni, AoD[f, 3, 0], AoD[f, 3, 1]);
                            }
                            // Rotate the quad so the diagonal joins the brighter pair (avoids AO seams).
                            int start = (a1 + a3 > a0 + a2) ? 1 : 0;

                            uint[] buf;
                            int o;
                            if (water) { Grow(ref bufW, nw + 8); buf = bufW; o = nw; nw += 8; }
                            else { Grow(ref bufO, no + 8); buf = bufO; o = no; no += 8; }

                            for (int k = 0; k < 4; k++)
                            {
                                int ck = (k + start) & 3;
                                int aoK = ck == 0 ? a0 : ck == 1 ? a1 : ck == 2 ? a2 : a3;
                                uint cx = Corner[f, ck, 0], cy = Corner[f, ck, 1], cz = Corner[f, ck, 2];
                                uint light = (uint)(baseL * AoF[aoK]);
                                uint flag = (lower && cy == 1) ? 1u : 0u;
                                buf[o++] = ((uint)x + cx) | (((uint)y + cy) << 8) | (((uint)z + cz) << 16) | (light << 24);
                                buf[o++] = UV[ck, 0] | ((uint)UV[ck, 1] << 8) | (tile << 16) | (flag << 24);
                            }
                        }
                    }
                }

            var m = new MeshData { OpaqueQuads = no / 8, WaterQuads = nw / 8 };
            if (no + nw > 0)
            {
                m.Data = ArrayPool<uint>.Shared.Rent(no + nw);
                Array.Copy(bufO, 0, m.Data, 0, no);
                Array.Copy(bufW, 0, m.Data, no, nw);
            }
            return m;
        }

        static int Ao(byte[] p, int ni, int d1, int d2)
        {
            int s1 = Blocks.Opaque[p[ni + d1]] ? 1 : 0;
            int s2 = Blocks.Opaque[p[ni + d2]] ? 1 : 0;
            if (s1 + s2 == 2) return 0;
            int cn = Blocks.Opaque[p[ni + d1 + d2]] ? 1 : 0;
            return 3 - (s1 + s2 + cn);
        }
    }
}
