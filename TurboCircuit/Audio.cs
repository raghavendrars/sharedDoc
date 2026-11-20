using System.Runtime.InteropServices;
using Raylib_cs;
using static Raylib_cs.Raylib;

namespace TurboCircuit;

/// <summary>All sounds are synthesised at start-up; the game ships no audio files.</summary>
static unsafe class Audio
{
    const int Rate = 22050;
    static bool ok;
    static readonly Music[] engine = new Music[2];
    static readonly bool[] engineOn = new bool[2];
    static Sound beep, go, thump, click, buy, deny, boost, cheer;

    static Sound Make(float secs, Func<float, float> f)
    {
        int n = (int)(secs * Rate);
        short* data = (short*)NativeMemory.Alloc((nuint)(n * 2));
        for (int i = 0; i < n; i++) data[i] = (short)(Math.Clamp(f(i / (float)Rate), -1f, 1f) * 30000);
        var w = new Wave { SampleCount = (uint)n, SampleRate = Rate, SampleSize = 16, Channels = 1, Data = data };
        Sound s = LoadSoundFromWave(w);
        NativeMemory.Free(data);
        return s;
    }

    static Music MakeLoop(float secs, Func<float, float> f)
    {
        int n = (int)(secs * Rate), bytes = n * 2;
        byte* buf = (byte*)NativeMemory.Alloc((nuint)(44 + bytes));   // kept alive for the life of the stream
        var span = new Span<byte>(buf, 44 + bytes);
        "RIFF"u8.CopyTo(span);
        BitConverter.TryWriteBytes(span[4..], 36 + bytes);
        "WAVEfmt "u8.CopyTo(span[8..]);
        BitConverter.TryWriteBytes(span[16..], 16);
        BitConverter.TryWriteBytes(span[20..], (short)1);
        BitConverter.TryWriteBytes(span[22..], (short)1);
        BitConverter.TryWriteBytes(span[24..], Rate);
        BitConverter.TryWriteBytes(span[28..], Rate * 2);
        BitConverter.TryWriteBytes(span[32..], (short)2);
        BitConverter.TryWriteBytes(span[34..], (short)16);
        "data"u8.CopyTo(span[36..]);
        BitConverter.TryWriteBytes(span[40..], bytes);
        short* pcm = (short*)(buf + 44);
        for (int i = 0; i < n; i++) pcm[i] = (short)(Math.Clamp(f(i / (float)Rate), -1f, 1f) * 30000);
        sbyte* ft = stackalloc sbyte[5] { (sbyte)'.', (sbyte)'w', (sbyte)'a', (sbyte)'v', 0 };
        return LoadMusicStreamFromMemory(ft, buf, 44 + bytes);
    }

    static float Saw(float x) => 2f * (x - MathF.Floor(x + 0.5f));
    static float Sin(float hz, float t) => MathF.Sin(MathF.Tau * hz * t);

    public static void Init()
    {
        InitAudioDevice();
        ok = IsAudioDeviceReady();
        if (!ok) return;
        var rng = new Random(7);
        for (int i = 0; i < 2; i++)
        {
            engine[i] = MakeLoop(1f, t => 0.45f * Saw(55 * t) + 0.28f * Saw(110 * t + 0.3f) + 0.18f * Sin(165, t) + 0.08f * Sin(27, t));
            SetMusicVolume(engine[i], 0);
        }
        beep = Make(0.16f, t => Sin(440, t) * 0.5f * MathF.Min(1, (0.16f - t) * 30));
        go = Make(0.5f, t => Sin(880, t) * 0.5f * MathF.Min(1, (0.5f - t) * 8));
        click = Make(0.05f, t => Sin(700, t) * 0.35f * (1 - t / 0.05f));
        deny = Make(0.18f, t => Saw(140 * t) * 0.3f * (1 - t / 0.18f));
        buy = Make(0.36f, t => (t < 0.12f ? Sin(660, t) : t < 0.24f ? Sin(880, t) : Sin(1320, t)) * 0.4f * MathF.Min(1, (0.36f - t) * 12));
        cheer = Make(0.9f, t => (Sin(523, t) + Sin(659, t) + Sin(784, t)) * 0.17f * MathF.Min(1, (0.9f - t) * 4));
        thump = Make(0.25f, t => (Sin(70, t) * 0.7f + ((float)rng.NextDouble() - 0.5f) * 0.5f) * MathF.Exp(-t * 18));
        float lp = 0;
        boost = Make(0.6f, t =>
        {
            lp += (((float)rng.NextDouble() * 2 - 1) - lp) * 0.25f;
            return lp * 0.8f * MathF.Min(1, t * 20) * (1 - t / 0.6f);
        });
    }

    public static void Engine(int i, float pitch, float vol)
    {
        if (!ok) return;
        if (!engineOn[i]) { PlayMusicStream(engine[i]); engineOn[i] = true; }
        SetMusicPitch(engine[i], pitch);
        SetMusicVolume(engine[i], vol);
        UpdateMusicStream(engine[i]);
    }

    public static void StopEngines()
    {
        if (!ok) return;
        for (int i = 0; i < 2; i++)
            if (engineOn[i]) { StopMusicStream(engine[i]); engineOn[i] = false; }
    }

    static void Play(Sound s, float vol = 1f)
    {
        if (!ok) return;
        SetSoundVolume(s, vol);
        PlaySound(s);
    }

    public static void Beep() => Play(beep);
    public static void Go() => Play(go);
    public static void Click() => Play(click, 0.7f);
    public static void Deny() => Play(deny);
    public static void Buy() => Play(buy);
    public static void Cheer() => Play(cheer);
    public static void Boost() => Play(boost, 0.6f);
    public static void Thump(float vol) => Play(thump, Math.Clamp(vol, 0.2f, 1f));

    public static void Close()
    {
        if (ok) CloseAudioDevice();
    }
}
