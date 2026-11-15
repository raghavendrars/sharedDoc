namespace Blockhaven
{
    public static class B
    {
        public const byte Air = 0, Grass = 1, Dirt = 2, Stone = 3, Sand = 4, Water = 5, Log = 6, Leaves = 7,
            Planks = 8, Cobble = 9, Glass = 10, Brick = 11, Snow = 12, CoalOre = 13, IronOre = 14, GemOre = 15,
            Bedrock = 16, Gravel = 17, Red = 18, Blue = 19, Yellow = 20, White = 21;
        public const int Count = 22;
    }

    /// <summary>Texture layer indices in the block texture array.</summary>
    public static class T
    {
        public const byte GrassTop = 0, GrassSide = 1, Dirt = 2, Stone = 3, Sand = 4, Water = 5, LogSide = 6, LogTop = 7,
            Leaves = 8, Planks = 9, Cobble = 10, Glass = 11, Brick = 12, Snow = 13, CoalOre = 14, IronOre = 15,
            GemOre = 16, Bedrock = 17, Gravel = 18, Red = 19, Blue = 20, Yellow = 21, White = 22;
        public const int Count = 23;
    }

    public sealed class BlockInfo
    {
        public string Name;
        public byte Top, Side, Bottom;
        public float Hardness;   // seconds to break in survival
        public byte Drop;
        public bool Solid, Opaque;
    }

    public sealed class Recipe
    {
        public (byte id, int n)[] In;
        public byte Out;
        public int OutCount;
    }

    public static class Blocks
    {
        public static readonly BlockInfo[] Info = new BlockInfo[B.Count];
        public static readonly bool[] Opaque = new bool[256];
        public static readonly bool[] Solid = new bool[256];

        public static readonly Recipe[] Recipes =
        {
            R(B.Planks, 4, (B.Log, 1)),
            R(B.Stone, 1, (B.Cobble, 2)),
            R(B.Glass, 1, (B.Sand, 2)),
            R(B.Brick, 2, (B.Cobble, 2), (B.Dirt, 1)),
            R(B.White, 2, (B.Sand, 2), (B.Stone, 1)),
            R(B.Yellow, 2, (B.Sand, 2), (B.Planks, 1)),
            R(B.Red, 2, (B.Sand, 2), (B.IronOre, 1)),
            R(B.Blue, 4, (B.Sand, 2), (B.GemOre, 1)),
        };

        /// <summary>Blocks offered in the creative palette.</summary>
        public static readonly byte[] Palette =
        {
            B.Grass, B.Dirt, B.Stone, B.Cobble, B.Sand, B.Gravel, B.Log, B.Planks, B.Leaves, B.Glass, B.Brick,
            B.Snow, B.Red, B.Blue, B.Yellow, B.White, B.CoalOre, B.IronOre, B.GemOre,
        };

        static Recipe R(byte o, int n, params (byte, int)[] input) => new Recipe { Out = o, OutCount = n, In = input };

        static void Def(byte id, string name, byte top, byte side, byte bottom, float hard, byte drop, bool solid = true, bool opaque = true)
        {
            Info[id] = new BlockInfo { Name = name, Top = top, Side = side, Bottom = bottom, Hardness = hard, Drop = drop, Solid = solid, Opaque = opaque };
            Opaque[id] = opaque;
            Solid[id] = solid;
        }

        static void Def(byte id, string name, byte tex, float hard, bool solid = true, bool opaque = true) =>
            Def(id, name, tex, tex, tex, hard, id, solid, opaque);

        static Blocks()
        {
            Def(B.Air, "Air", 0, 0f, false, false);
            Def(B.Grass, "Grass", T.GrassTop, T.GrassSide, T.Dirt, 0.55f, B.Dirt);
            Def(B.Dirt, "Dirt", T.Dirt, 0.5f);
            Def(B.Stone, "Stone", T.Stone, T.Stone, T.Stone, 1.4f, B.Cobble);
            Def(B.Sand, "Sand", T.Sand, 0.45f);
            Def(B.Water, "Water", T.Water, 0f, false, false);
            Def(B.Log, "Log", T.LogTop, T.LogSide, T.LogTop, 1.0f, B.Log);
            Def(B.Leaves, "Leaves", T.Leaves, 0.25f);
            Def(B.Planks, "Planks", T.Planks, 0.9f);
            Def(B.Cobble, "Cobblestone", T.Cobble, 1.4f);
            Def(B.Glass, "Glass", T.Glass, 0.3f, true, false);
            Def(B.Brick, "Bricks", T.Brick, 1.5f);
            Def(B.Snow, "Snow", T.Snow, 0.4f);
            Def(B.CoalOre, "Coal Ore", T.CoalOre, 1.8f);
            Def(B.IronOre, "Iron Ore", T.IronOre, 2.0f);
            Def(B.GemOre, "Gem Ore", T.GemOre, 2.4f);
            Def(B.Bedrock, "Bedrock", T.Bedrock, float.PositiveInfinity);
            Def(B.Gravel, "Gravel", T.Gravel, 0.5f);
            Def(B.Red, "Red Block", T.Red, 0.6f);
            Def(B.Blue, "Blue Block", T.Blue, 0.6f);
            Def(B.Yellow, "Yellow Block", T.Yellow, 0.6f);
            Def(B.White, "White Block", T.White, 0.6f);
        }
    }
}
