using System.Numerics;
using System.Text.Json;
using Raylib_cs;

namespace TurboCircuit;

record CarModel(string Name, string Kind, int Price, float MaxSpeed, float Accel, float Grip, float Brake,
    float Len, float Wid, float BodyH, float CabLen, float CabH, float CabOff, bool Spoiler, float WheelR);

record PartCat(string Name, string Effect, int BasePrice);

record TrackDef(string Name, string Blurb, int Theme, float HalfWidth, int Prize, int AiCar, int AiParts, float AiSkill,
    Color SkyTop, Color SkyLow, Color Ground, Color Road, Vector2[] Pts);

struct Stats
{
    public float MaxSpeed, Accel, Grip, Brake, NitroCap;
}

static class U
{
    public static Color C(int r, int g, int b, int a = 255) => new Color((byte)r, (byte)g, (byte)b, (byte)a);
    public static Color Shade(Color c, float f) => C((int)Math.Clamp(c.R * f, 0, 255), (int)Math.Clamp(c.G * f, 0, 255), (int)Math.Clamp(c.B * f, 0, 255), c.A);
    public static Color Alpha(Color c, int a) => C(c.R, c.G, c.B, a);
    public static Vector3 V3(Vector2 p, float y) => new(p.X, y, p.Y);
    public static float Wrap(float a)
    {
        while (a > MathF.PI) a -= MathF.Tau;
        while (a < -MathF.PI) a += MathF.Tau;
        return a;
    }
    public static string TimeStr(float t)
    {
        if (t <= 0) return "--:--.--";
        int m = (int)(t / 60);
        float s = t - m * 60;
        return $"{m}:{s:00.00}";
    }
    public static string Ordinal(int n) => n switch { 1 => "1st", 2 => "2nd", 3 => "3rd", _ => n + "th" };
    public static void Text(string s, int x, int y, int size, Color c) => Raylib.DrawText(s, x, y, size, c);
    public static void TextC(string s, int cx, int y, int size, Color c) => Raylib.DrawText(s, cx - Raylib.MeasureText(s, size) / 2, y, size, c);
    public static void TextR(string s, int rx, int y, int size, Color c) => Raylib.DrawText(s, rx - Raylib.MeasureText(s, size), y, size, c);
}

static class Db
{
    public const int MaxTier = 3;

    public static readonly CarModel[] Cars =
    {
        //            name        kind              price  vmax  acc   grip  brake  len   wid   bodyH cabL  cabH  cabOff spoiler wheel
        new CarModel("Sparrow",   "City hatchback",     0, 42f,  7.0f, 15f,  16f,   3.6f, 1.7f, 0.70f, 1.9f, 0.60f, -0.35f, false, 0.33f),
        new CarModel("Courier",   "Family sedan",    6000, 48f,  8.0f, 16f,  18f,   4.4f, 1.8f, 0.65f, 2.0f, 0.55f, -0.10f, false, 0.34f),
        new CarModel("Bison",     "Muscle coupe",   14000, 56f, 10.0f, 15f,  18f,   4.7f, 1.95f, 0.70f, 1.7f, 0.50f, -0.45f, false, 0.38f),
        new CarModel("Drift Fox", "Rally special",  24000, 54f, 10.5f, 20f,  22f,   4.0f, 1.85f, 0.72f, 1.9f, 0.58f, -0.20f, true,  0.37f),
        new CarModel("Vortex",    "Supercar",       42000, 66f, 12.5f, 21f,  24f,   4.5f, 2.0f, 0.52f, 1.6f, 0.42f, -0.15f, true,  0.36f),
        new CarModel("Apex One",  "Hypercar",       75000, 76f, 15.0f, 24f,  28f,   4.8f, 2.05f, 0.45f, 1.4f, 0.38f,  0.00f, true,  0.37f),
    };

    public static readonly PartCat[] Parts =
    {
        new PartCat("Engine",  "Top speed + acceleration", 2000),
        new PartCat("Turbo",   "Acceleration",             1500),
        new PartCat("Gearbox", "Top speed",                1500),
        new PartCat("Tyres",   "Cornering grip",           1200),
        new PartCat("Brakes",  "Braking power",             800),
        new PartCat("Nitro",   "Boost tank (hold boost key)", 1800),
    };

    public static readonly float[] PrizeShare = { 1f, 0.6f, 0.4f, 0.25f, 0.15f, 0.1f };

    public static Stats Compute(int car, int[] p)
    {
        var m = Cars[car];
        return new Stats
        {
            MaxSpeed = m.MaxSpeed * (1 + 0.04f * p[0]) * (1 + 0.04f * p[2]),
            Accel = m.Accel * (1 + 0.06f * p[0]) * (1 + 0.08f * p[1]),
            Grip = m.Grip * (1 + 0.07f * p[3]),
            Brake = m.Brake * (1 + 0.12f * p[4]),
            NitroCap = p[5] == 0 ? 0 : 1f + 1.5f * p[5],
        };
    }

    public static int PartPrice(int car, int cat, int tier) => (int)(Parts[cat].BasePrice * tier * (1 + 0.6f * car));

    static Vector2[] V(params float[] xy)
    {
        var r = new Vector2[xy.Length / 2];
        for (int i = 0; i < r.Length; i++) r[i] = new Vector2(xy[i * 2], xy[i * 2 + 1]);
        return r;
    }

    public static readonly TrackDef[] Tracks =
    {
        new TrackDef("Meadow Oval", "Wide, fast and forgiving", 0, 8f, 2500, 0, 0, 0.80f,
            U.C(70, 140, 230), U.C(190, 225, 250), U.C(72, 150, 62), U.C(62, 62, 68),
            V(0,0, 140,0, 230,25, 270,95, 230,165, 140,190, 0,190, -90,165, -130,95, -90,25)),

        new TrackDef("Dune Circuit", "Sweeping desert bends", 1, 7.5f, 4000, 1, 1, 0.82f,
            U.C(240, 170, 90), U.C(250, 225, 170), U.C(214, 180, 112), U.C(78, 70, 64),
            V(0,0, 120,0, 200,-30, 280,10, 300,100, 240,170, 160,140, 90,170, 40,250, -60,260, -130,190, -140,90, -90,20)),

        new TrackDef("Frost Pass", "Twisty mountain snow run", 2, 7f, 6000, 2, 1, 0.84f,
            U.C(120, 160, 210), U.C(225, 235, 245), U.C(232, 238, 245), U.C(84, 88, 98),
            V(0,0, 150,0, 230,30, 250,110, 200,170, 130,150, 100,90, 50,80, 0,110, -10,180, 40,240, 20,310, -70,320, -140,270, -150,180, -110,120, -140,50, -80,5)),

        new TrackDef("Neon City", "Tight street corners at night", 3, 6.5f, 9000, 3, 2, 0.86f,
            U.C(8, 10, 32), U.C(48, 30, 80), U.C(30, 32, 40), U.C(50, 50, 58),
            V(0,0, 160,0, 205,15, 220,60, 220,140, 205,185, 160,200, 110,200, 80,180, 70,140, 50,110, 0,100, -50,100, -90,85, -100,50, -90,15, -50,0)),

        new TrackDef("Sunset Ridge", "The long championship lap", 4, 7f, 14000, 4, 2, 0.88f,
            U.C(90, 60, 140), U.C(255, 150, 90), U.C(120, 110, 60), U.C(66, 60, 64),
            V(0,0, 160,0, 250,-40, 340,0, 370,90, 320,160, 230,150, 170,100, 110,110, 80,170, 130,230, 220,240, 310,260, 350,330, 290,400, 170,390, 50,360, -50,300, -110,210, -70,130, -130,70, -100,10)),
    };
}

class Profile
{
    public int Money { get; set; } = 1500;
    public int Unlocked { get; set; } = 1;
    public bool[] Owned { get; set; } = Array.Empty<bool>();
    public int[][] Parts { get; set; } = Array.Empty<int[]>();
    public float[] Best { get; set; } = Array.Empty<float>();

    static string PathName => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "TurboCircuit", "save.json");

    public void Normalize()
    {
        int nc = Db.Cars.Length, np = Db.Parts.Length, nt = Db.Tracks.Length;
        var owned = new bool[nc];
        var parts = new int[nc][];
        var best = new float[nt];
        for (int i = 0; i < nc; i++)
        {
            owned[i] = i == 0 || (Owned != null && i < Owned.Length && Owned[i]);
            parts[i] = new int[np];
            if (Parts != null && i < Parts.Length && Parts[i] != null)
                for (int j = 0; j < np && j < Parts[i].Length; j++) parts[i][j] = Math.Clamp(Parts[i][j], 0, Db.MaxTier);
        }
        for (int i = 0; i < nt; i++) if (Best != null && i < Best.Length) best[i] = Best[i];
        Owned = owned; Parts = parts; Best = best;
        Unlocked = Math.Clamp(Unlocked, 1, nt);
        Money = Math.Max(0, Money);
    }

    public static Profile Load()
    {
        Profile? p = null;
        try
        {
            if (File.Exists(PathName)) p = JsonSerializer.Deserialize<Profile>(File.ReadAllText(PathName));
        }
        catch { }
        p ??= new Profile();
        p.Normalize();
        return p;
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(PathName)!);
            File.WriteAllText(PathName, JsonSerializer.Serialize(this));
        }
        catch { }
    }
}
