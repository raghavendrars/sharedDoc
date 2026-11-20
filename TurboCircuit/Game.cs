using System.Numerics;
using System.Text;
using Raylib_cs;
using static Raylib_cs.Raylib;

namespace TurboCircuit;

enum Screen { Menu, Garage, TrackSelect, Race, Results }

record ResultRow(string Name, string CarName, Color Col, float Time, bool Human, int Prize);

class Game
{
    readonly bool test;
    Profile prof = new();
    Track[] tracks = Array.Empty<Track>();
    Screen screen = Screen.Menu;
    Race? race;
    bool quit, paused;

    int menuSel, players = 1, garagePlayer, garageCar, garagePart, trackSel;
    bool garageChoose;
    readonly int[] chosen = { 0, 0 };
    float clock, toastT;
    string toast = "";
    readonly List<ResultRow> results = new();
    string resultNote = "";

    readonly RenderTexture2D[] rt = new RenderTexture2D[2];
    int rtW, rtH;
    int frame;
    string? shot;

    static readonly Color Gold = U.C(255, 210, 60), Dim = U.C(160, 170, 190), Bg1 = U.C(16, 20, 34), Bg2 = U.C(44, 52, 84);
    static readonly Color[] PlayerCol = { U.C(225, 40, 45), U.C(40, 110, 235) };

    public Game(bool selfTest) { test = selfTest; }

    public void Run()
    {
        SetConfigFlags(ConfigFlags.Msaa4xHint | ConfigFlags.ResizableWindow | (test ? 0 : ConfigFlags.VSyncHint));
        InitWindow(1280, 720, "Turbo Circuit");
        SetWindowMinSize(960, 540);
        SetExitKey(KeyboardKey.Null);
        if (!test) SetTargetFPS(60);
        Rlgl.SetClipPlanes(0.3, 3000);
        Audio.Init();

        tracks = Db.Tracks.Select((d, i) => new Track(d, 1000 + i * 17)).ToArray();
        if (test) { prof.Normalize(); SimAll(); }
        else prof = Profile.Load();

        while (!WindowShouldClose() && !quit)
        {
            float dt = test ? 1f / 60f : MathF.Min(GetFrameTime(), 0.05f);
            clock += dt;
            toastT -= dt;
            frame++;
            if (test) Script();
            Update(dt);
            BeginDrawing();
            Draw();
            if (shot != null)
            {
                Rlgl.DrawRenderBatchActive();
                TakeScreenshot(shot);
                shot = null;
            }
            EndDrawing();
        }
        if (!test) prof.Save();
        Audio.Close();
        CloseWindow();
    }

    // ------------------------------------------------------------------ helpers

    static bool KUp => IsKeyPressed(KeyboardKey.Up) || IsKeyPressed(KeyboardKey.W);
    static bool KDown => IsKeyPressed(KeyboardKey.Down) || IsKeyPressed(KeyboardKey.S);
    static bool KLeft => IsKeyPressed(KeyboardKey.Left) || IsKeyPressed(KeyboardKey.A);
    static bool KRight => IsKeyPressed(KeyboardKey.Right) || IsKeyPressed(KeyboardKey.D);
    static bool KOk => IsKeyPressed(KeyboardKey.Enter) || IsKeyPressed(KeyboardKey.KpEnter) || IsKeyPressed(KeyboardKey.Space);
    static bool KBack => IsKeyPressed(KeyboardKey.Escape);

    void Toast(string s) { toast = s; toastT = 2.2f; }

    void OpenGarage(int player, bool choose)
    {
        garagePlayer = player; garageChoose = choose; garageCar = chosen[player]; garagePart = 0;
        screen = Screen.Garage;
    }

    void StartRace(bool autopilot = false)
    {
        race = new Race(tracks[trackSel], trackSel, players, chosen, prof, autopilot);
        paused = false;
        screen = Screen.Race;
    }

    void ToMenu()
    {
        Audio.StopEngines();
        race = null;
        screen = Screen.Menu;
        if (!test) prof.Save();
    }

    // ------------------------------------------------------------------ update

    void Update(float dt)
    {
        if (IsKeyPressed(KeyboardKey.F11)) ToggleBorderlessWindowed();
        switch (screen)
        {
            case Screen.Menu: UpdateMenu(); break;
            case Screen.Garage: UpdateGarage(); break;
            case Screen.TrackSelect: UpdateTrackSelect(); break;
            case Screen.Race: UpdateRace(dt); break;
            case Screen.Results:
                if (KOk || KBack) { Audio.Click(); ToMenu(); }
                break;
        }
    }

    void UpdateMenu()
    {
        if (KUp) { menuSel = (menuSel + 3) % 4; Audio.Click(); }
        if (KDown) { menuSel = (menuSel + 1) % 4; Audio.Click(); }
        if (!KOk) return;
        Audio.Click();
        switch (menuSel)
        {
            case 0: players = 1; OpenGarage(0, true); break;
            case 1: players = 2; OpenGarage(0, true); break;
            case 2: OpenGarage(0, false); break;
            case 3: quit = true; break;
        }
    }

    void UpdateGarage()
    {
        int nc = Db.Cars.Length, np = Db.Parts.Length;
        if (KLeft) { garageCar = (garageCar + nc - 1) % nc; Audio.Click(); }
        if (KRight) { garageCar = (garageCar + 1) % nc; Audio.Click(); }
        if (KUp) { garagePart = (garagePart + np - 1) % np; Audio.Click(); }
        if (KDown) { garagePart = (garagePart + 1) % np; Audio.Click(); }

        bool owned = prof.Owned[garageCar];
        if (IsKeyPressed(KeyboardKey.B))
        {
            if (!owned)
            {
                int price = Db.Cars[garageCar].Price;
                if (prof.Money >= price)
                {
                    prof.Money -= price; prof.Owned[garageCar] = true;
                    Audio.Buy(); Toast($"{Db.Cars[garageCar].Name} purchased!");
                }
                else { Audio.Deny(); Toast($"Not enough money - need ${price - prof.Money:N0} more"); }
            }
            else
            {
                int tier = prof.Parts[garageCar][garagePart];
                if (tier >= Db.MaxTier) { Audio.Deny(); Toast($"{Db.Parts[garagePart].Name} is already fully upgraded"); }
                else
                {
                    int price = Db.PartPrice(garageCar, garagePart, tier + 1);
                    if (prof.Money >= price)
                    {
                        prof.Money -= price; prof.Parts[garageCar][garagePart]++;
                        Audio.Buy(); Toast($"{Db.Parts[garagePart].Name} upgraded to level {tier + 1}");
                    }
                    else { Audio.Deny(); Toast($"Not enough money - need ${price - prof.Money:N0} more"); }
                }
            }
            if (!test) prof.Save();
        }

        if (IsKeyPressed(KeyboardKey.Enter) || IsKeyPressed(KeyboardKey.KpEnter))
        {
            if (!garageChoose) { }
            else if (!owned) { Audio.Deny(); Toast("You don't own this car yet - press B to buy it"); }
            else
            {
                Audio.Click();
                chosen[garagePlayer] = garageCar;
                if (players == 2 && garagePlayer == 0) OpenGarage(1, true);
                else { trackSel = Math.Min(trackSel, prof.Unlocked - 1); screen = Screen.TrackSelect; }
            }
        }
        if (KBack)
        {
            Audio.Click();
            if (garageChoose && garagePlayer == 1) OpenGarage(0, true);
            else ToMenu();
        }
    }

    void UpdateTrackSelect()
    {
        int nt = tracks.Length;
        if (KUp) { trackSel = (trackSel + nt - 1) % nt; Audio.Click(); }
        if (KDown) { trackSel = (trackSel + 1) % nt; Audio.Click(); }
        if (KOk)
        {
            if (trackSel < prof.Unlocked) { Audio.Click(); StartRace(); }
            else { Audio.Deny(); Toast($"Locked - finish in the top 3 on {tracks[trackSel - 1].Def.Name} first"); }
        }
        if (KBack) { Audio.Click(); OpenGarage(players - 1, true); }
    }

    void UpdateRace(float dt)
    {
        if (race == null) return;
        if (KBack) { paused = !paused; if (paused) Audio.StopEngines(); }
        if (paused)
        {
            if (IsKeyPressed(KeyboardKey.Enter)) paused = false;
            if (IsKeyPressed(KeyboardKey.Q)) ToMenu();
            return;
        }
        race.Update(dt, !test);
        if (race.Done) FinishRace();
    }

    void FinishRace()
    {
        var r = race!;
        Audio.StopEngines();
        results.Clear();
        resultNote = "";
        int bestHuman = 99, total = 0;
        for (int i = 0; i < r.Order.Count; i++)
        {
            var c = r.Order[i];
            int prize = 0;
            if (c.Player >= 0)
            {
                prize = (int)(r.Tr.Def.Prize * Db.PrizeShare[i]);
                total += prize;
                bestHuman = Math.Min(bestHuman, i + 1);
                if (c.Finished && (prof.Best[r.TrackIdx] <= 0 || c.FinishTime < prof.Best[r.TrackIdx])) prof.Best[r.TrackIdx] = c.FinishTime;
            }
            results.Add(new ResultRow(c.Name, Db.Cars[c.Model].Name, c.Col, c.Finished ? c.FinishTime : 0, c.Player >= 0, prize));
        }
        prof.Money += total;
        if (bestHuman <= 3 && r.TrackIdx + 1 < tracks.Length && prof.Unlocked < r.TrackIdx + 2)
        {
            prof.Unlocked = r.TrackIdx + 2;
            resultNote = $"New track unlocked: {tracks[r.TrackIdx + 1].Def.Name}!";
        }
        else if (bestHuman > 3 && r.TrackIdx + 1 < tracks.Length && prof.Unlocked < r.TrackIdx + 2)
            resultNote = "Finish in the top 3 to unlock the next track. Upgrade your car in the garage!";
        if (!test) prof.Save();
        screen = Screen.Results;
    }

    // ------------------------------------------------------------------ drawing

    void Draw()
    {
        int w = GetScreenWidth(), h = GetScreenHeight();
        ClearBackground(Color.Black);   // also clears the depth buffer
        switch (screen)
        {
            case Screen.Menu: DrawMenu(w, h); break;
            case Screen.Garage: DrawGarage(w, h); break;
            case Screen.TrackSelect: DrawTrackSelect(w, h); break;
            case Screen.Race: DrawRace(w, h); break;
            case Screen.Results: DrawResults(w, h); break;
        }
        if (toastT > 0)
        {
            int tw = MeasureText(toast, 22) + 40;
            DrawRectangle(w / 2 - tw / 2, h - 110, tw, 40, U.C(0, 0, 0, 210));
            DrawRectangleLines(w / 2 - tw / 2, h - 110, tw, 40, Gold);
            U.TextC(toast, w / 2, h - 101, 22, Color.White);
        }
    }

    void Backdrop(int w, int h) => DrawRectangleGradientV(0, 0, w, h, Bg1, Bg2);

    void Showcase(int model, Color col, float yOff = 0)
    {
        var cam = new Camera3D(new Vector3(6.2f, 3.0f, 7.0f), new Vector3(0, 0.9f + yOff, 0), Vector3.UnitY, 40, CameraProjection.Perspective);
        BeginMode3D(cam);
        DrawCylinder(new Vector3(0, -0.25f, 0), 4.2f, 4.4f, 0.25f, 40, U.C(70, 76, 96));
        DrawCylinder(new Vector3(0, 0, 0), 3.9f, 3.9f, 0.03f, 40, U.C(92, 100, 124));
        Car.Draw(Db.Cars[model], Vector2.Zero, clock * 0.6f, col, MathF.Sin(clock) * 0.5f, false, false, 0);
        EndMode3D();
    }

    void MoneyBadge(int w)
    {
        string s = $"$ {prof.Money:N0}";
        int tw = MeasureText(s, 30);
        DrawRectangle(w - tw - 50, 18, tw + 32, 44, U.C(0, 0, 0, 150));
        U.Text(s, w - tw - 34, 25, 30, U.C(110, 235, 130));
    }

    void DrawMenu(int w, int h)
    {
        Backdrop(w, h);
        Showcase(chosen[0], PlayerCol[0], -1.0f);
        U.TextC("TURBO CIRCUIT", w / 2 + 4, 54, 84, Color.Black);
        U.TextC("TURBO CIRCUIT", w / 2, 50, 84, Gold);
        U.TextC("3D arcade racing", w / 2, 138, 24, Dim);
        MoneyBadge(w);

        string[] items = { "SINGLE PLAYER", "TWO PLAYERS  (split screen)", "GARAGE & SHOP", "QUIT" };
        int y0 = h - 290;
        for (int i = 0; i < items.Length; i++)
        {
            bool sel = i == menuSel;
            int y = y0 + i * 52;
            if (sel) DrawRectangle(w / 2 - 260, y - 6, 520, 44, U.C(255, 210, 60, 50));
            U.TextC(items[i], w / 2, y, 32, sel ? Gold : Color.White);
        }
        U.TextC("Up/Down: select     Enter: confirm     F11: full screen", w / 2, h - 44, 20, Dim);
    }

    void StatBar(string label, float baseV, float v, float max, int x, int y, int bw)
    {
        U.Text(label, x, y, 20, Color.White);
        int bx = x + 142;
        DrawRectangle(bx, y + 3, bw, 14, U.C(20, 24, 36));
        DrawRectangle(bx, y + 3, (int)(bw * MathF.Min(1, v / max)), 14, U.C(90, 220, 120));
        DrawRectangle(bx, y + 3, (int)(bw * MathF.Min(1, baseV / max)), 14, U.C(80, 160, 240));
    }

    void DrawGarage(int w, int h)
    {
        Backdrop(w, h);
        var m = Db.Cars[garageCar];
        bool owned = prof.Owned[garageCar];
        Color col = PlayerCol[garagePlayer];
        Showcase(garageCar, owned ? col : U.C(110, 110, 120));

        string title = !garageChoose ? "GARAGE & SHOP" : players == 2 ? $"PLAYER {garagePlayer + 1} - CHOOSE YOUR CAR" : "CHOOSE YOUR CAR";
        U.Text(title, 40, 24, 36, garageChoose ? col : Gold);
        MoneyBadge(w);

        // left: car card
        int px = 40, py = 96, pw = 370;
        DrawRectangle(px, py, pw, 330, U.C(0, 0, 0, 140));
        U.Text($"<  {garageCar + 1}/{Db.Cars.Length}  >", px + 16, py + 12, 20, Dim);
        U.Text(m.Name, px + 16, py + 40, 44, Color.White);
        U.Text(m.Kind, px + 16, py + 88, 22, Dim);
        if (owned) U.Text("OWNED", px + 16, py + 122, 26, U.C(110, 235, 130));
        else U.Text($"PRICE  $ {m.Price:N0}", px + 16, py + 122, 26, prof.Money >= m.Price ? Gold : U.C(240, 100, 90));

        var st0 = Db.Compute(garageCar, new int[Db.Parts.Length]);
        var st = Db.Compute(garageCar, prof.Parts[garageCar]);
        int sy = py + 170;
        StatBar("Top speed", st0.MaxSpeed, st.MaxSpeed, 96f, px + 16, sy, 200);
        StatBar("Acceleration", st0.Accel, st.Accel, 22.5f, px + 16, sy + 30, 200);
        StatBar("Grip", st0.Grip, st.Grip, 29.5f, px + 16, sy + 60, 200);
        StatBar("Brakes", st0.Brake, st.Brake, 38.5f, px + 16, sy + 90, 200);
        U.Text($"{(int)(st.MaxSpeed * 3.6f)} km/h top speed   (green = from upgrades)", px + 16, sy + 124, 16, Dim);

        // right: parts
        int rx = w - 440, ry = 96, rw = 400;
        DrawRectangle(rx, ry, rw, 96 + Db.Parts.Length * 56, U.C(0, 0, 0, 140));
        U.Text("SPARE PARTS", rx + 16, ry + 12, 26, Gold);
        U.Text(owned ? "upgrades apply to this car only" : "buy the car to fit parts", rx + 16, ry + 44, 18, Dim);
        for (int i = 0; i < Db.Parts.Length; i++)
        {
            int y = ry + 78 + i * 56;
            bool sel = i == garagePart;
            int tier = prof.Parts[garageCar][i];
            if (sel) DrawRectangle(rx + 6, y - 4, rw - 12, 52, U.C(255, 210, 60, 45));
            U.Text(Db.Parts[i].Name, rx + 16, y, 24, sel ? Gold : Color.White);
            U.Text(Db.Parts[i].Effect, rx + 16, y + 26, 16, Dim);
            for (int t = 0; t < Db.MaxTier; t++)
            {
                int bx = rx + 150 + t * 26;
                if (t < tier) DrawRectangle(bx, y + 3, 20, 18, U.C(90, 220, 120));
                else DrawRectangleLines(bx, y + 3, 20, 18, Dim);
            }
            string ps = tier >= Db.MaxTier ? "MAX" : $"$ {Db.PartPrice(garageCar, i, tier + 1):N0}";
            Color pc = tier >= Db.MaxTier ? U.C(110, 235, 130) : !owned ? Dim : prof.Money >= Db.PartPrice(garageCar, i, tier + 1) ? Color.White : U.C(240, 100, 90);
            U.TextR(ps, rx + rw - 16, y + 2, 22, pc);
        }

        string action = !owned ? $"B: buy {m.Name}" : $"B: upgrade {Db.Parts[garagePart].Name}";
        string help = $"Left/Right: car     Up/Down: part     {action}     " + (garageChoose ? "Enter: race this car     " : "") + "Esc: back";
        DrawRectangle(0, h - 56, w, 56, U.C(0, 0, 0, 160));
        U.TextC(help, w / 2, h - 40, 20, Color.White);
    }

    void DrawTrackSelect(int w, int h)
    {
        Backdrop(w, h);
        U.Text("CHOOSE A TRACK", 40, 24, 36, Gold);
        MoneyBadge(w);
        for (int i = 0; i < tracks.Length; i++)
        {
            var d = tracks[i].Def;
            bool sel = i == trackSel, open = i < prof.Unlocked;
            int y = 100 + i * 92;
            DrawRectangle(40, y, 540, 80, sel ? U.C(255, 210, 60, 55) : U.C(0, 0, 0, 140));
            if (sel) DrawRectangleLines(40, y, 540, 80, Gold);
            DrawRectangle(52, y + 12, 56, 56, open ? d.Ground : U.C(50, 50, 60));
            DrawRectangle(52, y + 12, 56, 22, open ? d.SkyTop : U.C(40, 40, 50));
            U.Text(d.Name, 124, y + 10, 28, open ? Color.White : Dim);
            U.Text(open ? d.Blurb : "LOCKED - top 3 on the previous track", 124, y + 44, 18, Dim);
            if (open)
            {
                U.TextR($"1st: $ {d.Prize:N0}", 566, y + 12, 20, U.C(110, 235, 130));
                U.TextR("best " + U.TimeStr(prof.Best[i]), 566, y + 46, 18, Dim);
            }
        }
        var tr = tracks[trackSel];
        int ms = Math.Min(h - 260, w - 700);
        int mx = 620 + (w - 660 - ms) / 2, my = 110;
        DrawRectangle(620, 100, w - 660, h - 190, U.C(0, 0, 0, 140));
        tr.DrawMini(mx, my + 10, ms, trackSel < prof.Unlocked ? Color.White : U.C(90, 90, 100), 5f);
        DrawCircleV(tr.MiniPt(tr.P[0], mx, my + 10, ms, tr.MiniScale(ms)), 7, Gold);
        U.TextC($"{tr.Length / 1000f:0.00} km per lap   -   {Race.Laps} laps   -   6 cars", 620 + (w - 660) / 2, h - 122, 20, Dim);

        DrawRectangle(0, h - 56, w, 56, U.C(0, 0, 0, 160));
        U.TextC("Up/Down: track     Enter: start race     Esc: back", w / 2, h - 40, 20, Color.White);
    }

    void DrawRace(int w, int h)
    {
        var r = race!;
        if (r.Players == 1) r.DrawView(0, w, h);
        else
        {
            int vw = w / 2;
            if (rtW != vw || rtH != h)
            {
                if (rtW != 0) { UnloadRenderTexture(rt[0]); UnloadRenderTexture(rt[1]); }
                rt[0] = LoadRenderTexture(vw, h); rt[1] = LoadRenderTexture(vw, h);
                rtW = vw; rtH = h;
            }
            for (int p = 0; p < 2; p++)
            {
                BeginTextureMode(rt[p]);
                ClearBackground(Color.Black);
                r.DrawView(p, vw, h);
                U.TextC(p == 0 ? "PLAYER 1" : "PLAYER 2", vw / 2, 12, 20, PlayerCol[p]);
                EndTextureMode();
            }
            ClearBackground(Color.Black);
            for (int p = 0; p < 2; p++)
                DrawTextureRec(rt[p].Texture, new Rectangle(0, 0, vw, -h), new Vector2(p * (w - vw), 0), Color.White);
            DrawRectangle(w / 2 - 2, 0, 4, h, Color.Black);
        }
        if (r.Time < 6 && !paused)
        {
            string s = r.Players == 1
                ? "W/S or Up/Down: gas/brake    A/D or Left/Right: steer    Space: nitro    R: back on track    Esc: pause"
                : "P1: W A S D + Left Shift (nitro), R reset        P2: Arrows + Right Shift (nitro), Backspace reset";
            int tw = MeasureText(s, 18) + 24;
            DrawRectangle(w / 2 - tw / 2, h - 34, tw, 28, U.C(0, 0, 0, 170));
            U.TextC(s, w / 2, h - 29, 18, Color.White);
        }
        if (paused)
        {
            DrawRectangle(0, 0, w, h, U.C(0, 0, 0, 170));
            U.TextC("PAUSED", w / 2, h / 2 - 90, 70, Gold);
            U.TextC("Enter or Esc: resume", w / 2, h / 2, 26, Color.White);
            U.TextC("Q: quit to menu (no prize money)", w / 2, h / 2 + 40, 26, Color.White);
        }
    }

    void DrawResults(int w, int h)
    {
        Backdrop(w, h);
        U.TextC("RACE RESULTS", w / 2, 36, 56, Gold);
        if (race != null) U.TextC(race.Tr.Def.Name, w / 2, 100, 24, Dim);
        int x = w / 2 - 400, y0 = 150;
        for (int i = 0; i < results.Count; i++)
        {
            var r = results[i];
            int y = y0 + i * 58;
            DrawRectangle(x, y, 800, 50, r.Human ? U.C(255, 255, 255, 40) : U.C(0, 0, 0, 140));
            U.Text(U.Ordinal(i + 1), x + 16, y + 10, 30, i == 0 ? Gold : Color.White);
            DrawRectangle(x + 96, y + 14, 22, 22, r.Col);
            U.Text(r.Name, x + 132, y + 11, 28, Color.White);
            U.Text(r.CarName, x + 330, y + 15, 20, Dim);
            U.Text(r.Time > 0 ? U.TimeStr(r.Time) : "still racing", x + 490, y + 13, 24, U.C(180, 230, 255));
            if (r.Human) U.TextR($"+ $ {r.Prize:N0}", x + 784, y + 11, 28, U.C(110, 235, 130));
        }
        int ny = y0 + results.Count * 58 + 16;
        if (resultNote != "") U.TextC(resultNote, w / 2, ny, 24, Gold);
        U.TextC($"Wallet: $ {prof.Money:N0}", w / 2, ny + 40, 26, U.C(110, 235, 130));
        U.TextC("Enter: continue", w / 2, h - 44, 22, Color.White);
    }

    // ------------------------------------------------------------------ self test (--selftest)

    void SimAll()
    {
        var sb = new StringBuilder();
        for (int i = 0; i < tracks.Length; i++)
        {
            var tr = tracks[i];
            sb.AppendLine($"{tr.Def.Name}: samples={tr.N} length={tr.Length:0}m minRadius={tr.MinRadius:0.0} (wall {tr.WallDist}) minGap={tr.MinGap:0.0} props={tr.Props.Count}");
            var r = new Race(tr, i, 1, new[] { Math.Min(i, Db.Cars.Length - 1), 0 }, prof, true);
            float topSpeed = 0; int resets = 0;
            for (int f = 0; f < 60 * 420 && !r.Done; f++)
            {
                r.Update(1f / 60f, false);
                foreach (var c in r.Cars) { topSpeed = MathF.Max(topSpeed, c.Speed); if (c.StuckT > 2.4f) resets++; }
            }
            sb.AppendLine($"  done={r.Done} top={topSpeed * 3.6f:0}km/h stuckResets={resets}");
            foreach (var c in r.Order)
                sb.AppendLine($"    {c.Name,-6} {Db.Cars[c.Model].Name,-10} lap={c.Lap} time={U.TimeStr(c.Finished ? c.FinishTime : 0)}");
        }
        File.WriteAllText("selftest.txt", sb.ToString());
    }

    void Script()
    {
        switch (frame)
        {
            case 20: shot = "shot_menu.png"; break;
            case 21: prof.Money = 9000; prof.Parts[0][0] = 2; prof.Parts[0][5] = 1; OpenGarage(0, true); break;
            case 50: shot = "shot_garage.png"; break;
            case 51: garageCar = 4; break;
            case 60: shot = "shot_garage2.png"; break;
            case 61: prof.Unlocked = 3; trackSel = 1; screen = Screen.TrackSelect; break;
            case 70: shot = "shot_tracks.png"; break;
            case 71: players = 1; trackSel = 0; chosen[0] = 0; StartRace(true); break;
            case 240: shot = "shot_grid.png"; break;
            case 900: shot = "shot_race1.png"; break;
            case 901: players = 2; trackSel = 3; chosen[0] = 3; chosen[1] = 5; StartRace(true); break;
            case 1700: shot = "shot_race2.png"; break;
            case 1701: players = 1; trackSel = 2; chosen[0] = 2; StartRace(true); break;
            case 2500: shot = "shot_race3.png"; break;
            case 2501: players = 1; trackSel = 4; chosen[0] = 5; StartRace(true); break;
            case 3300: shot = "shot_race4.png"; break;
            case 3301: players = 1; trackSel = 1; chosen[0] = 1; StartRace(true); break;
            case 4000: shot = "shot_race5.png"; break;
            case 4001: FinishRace(); break;
            case 4010: shot = "shot_results.png"; break;
            case 4012: quit = true; break;
        }
    }
}
