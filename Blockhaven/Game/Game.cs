using System;
using System.Buffers;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using OpenTK.Graphics.OpenGL4;
using OpenTK.Mathematics;
using OpenTK.Windowing.Common;
using OpenTK.Windowing.Desktop;
using OpenTK.Windowing.GraphicsLibraryFramework;

namespace Blockhaven
{
    public sealed class GameOptions
    {
        public GraphicsSettings Graphics;
        public bool AutoQuality;
        public ParentalStore Parental;   // null = no time tracking (self-test)
        public string WorldDir;
        public bool Creative;
        public bool SavingEnabled = true;
        public string SelfTestDir;
    }

    public sealed class Game : GameWindow
    {
        enum Screen { Playing, Paused, Inventory }

        const int MaxQuads = 1 << 17;
        const float DayLength = 720f;

        readonly GameOptions opt;
        GraphicsSettings gs;
        readonly World world;
        readonly Player player = new Player();
        public bool TimeUp;

        // GL resources
        Shader chunkShader, colorShader;
        Hud hud;
        int blockTex, sharedEbo, dynVao, dynVbo, cloudVao, cloudVbo, cloudVerts;
        readonly float[] dyn = new float[7 * 64];

        // streaming
        readonly List<Chunk> renderList = new List<Chunk>();
        readonly ConcurrentQueue<(Chunk c, MeshData m)> uploads = new ConcurrentQueue<(Chunk, MeshData)>();
        (int dx, int dz, int d2)[] offsets;
        int genInFlight, meshInFlight, meshR2;
        float unloadTimer, autosaveTimer;

        // state
        Screen screen = Screen.Playing;
        bool ready, debug, uiClick;
        int skipMouse = 3;
        float timeOfDay, breakProgress, breakCooldown, placeCooldown, msgTimer, cloudScroll;
        string msg = "";
        (int x, int y, int z) breakTarget;
        RayHit hit;
        Vector3d spawn;
        string glRenderer = "";

        // parental timing
        readonly Stopwatch clock = Stopwatch.StartNew();
        double lastTick, sinceSave, remaining = double.PositiveInfinity;
        bool warned5, warned1;

        // fps
        int frames; double fpsTime; float fps;

        // self-test
        int shotStage; double readyAt = -1;

        public Game(GameOptions opt) : base(
            new GameWindowSettings { UpdateFrequency = 0 },
            new NativeWindowSettings
            {
                ClientSize = new Vector2i(1280, 720),
                Title = "Blockhaven",
                APIVersion = new Version(3, 3),
                Profile = ContextProfile.Core,
                Flags = ContextFlags.ForwardCompatible,
                NumberOfSamples = opt.Graphics.Msaa,
                Vsync = VSyncMode.On,
                WindowState = opt.SelfTestDir != null ? WindowState.Normal : WindowState.Maximized,
            })
        {
            this.opt = opt;
            gs = opt.Graphics;
            world = new World(opt.WorldDir) { SavingEnabled = opt.SavingEnabled };
            player.Creative = opt.Creative;
            LoadPlayer();
        }

        // ------------------------------------------------------------------ setup

        void LoadPlayer()
        {
            var m = world.Meta;
            spawn = FindSpawn();
            if (m.HasPlayer)
            {
                player.Pos = new Vector3d(m.PX, m.PY, m.PZ);
                player.Yaw = m.Yaw; player.Pitch = m.Pitch;
                player.Health = m.Health <= 0 ? 20 : m.Health;
                if (m.Counts != null) for (int i = 0; i < Math.Min(m.Counts.Length, B.Count); i++) player.Counts[i] = m.Counts[i];
                if (m.Hotbar != null) for (int i = 0; i < Math.Min(m.Hotbar.Length, 9); i++) player.Hotbar[i] = (byte)(m.Hotbar[i] < B.Count ? m.Hotbar[i] : 0);
                player.Selected = Math.Clamp(m.Selected, 0, 8);
            }
            else
            {
                player.Pos = spawn;
                player.Pitch = -0.15f;
            }
            timeOfDay = m.TimeOfDay;
            bool emptyBar = true;
            foreach (var b in player.Hotbar) if (b != 0) emptyBar = false;
            if (player.Creative && emptyBar)
            {
                byte[] def = { B.Grass, B.Stone, B.Planks, B.Log, B.Glass, B.Brick, B.Cobble, B.Sand, B.Leaves };
                def.CopyTo(player.Hotbar, 0);
            }
            player.ResetFall();
        }

        Vector3d FindSpawn()
        {
            for (int r = 0; r < 600; r++)
            {
                int x = (r % 2 == 0 ? 1 : -1) * r * 4, z = (r * 7) % 23 - 11;
                int h = world.Gen.Height(x, z);
                if (h > TerrainGen.Sea + 1 && h < 80 && !world.Gen.IsCarved(x, h, z, h))
                    return new Vector3d(x + 0.5, h + 1.01, z + 0.5);
            }
            return new Vector3d(0.5, 110, 0.5);
        }

        void SavePlayer()
        {
            var m = world.Meta;
            m.HasPlayer = true;
            m.PX = player.Pos.X; m.PY = player.Pos.Y; m.PZ = player.Pos.Z;
            m.Yaw = player.Yaw; m.Pitch = player.Pitch;
            m.Health = player.Health;
            m.TimeOfDay = timeOfDay;
            m.Counts = (int[])player.Counts.Clone();
            m.Hotbar = Array.ConvertAll(player.Hotbar, b => (int)b);
            m.Selected = player.Selected;
        }

        void SaveEverything()
        {
            SavePlayer();
            world.SaveAll();
            opt.Parental?.Save();
        }

        protected override void OnLoad()
        {
            base.OnLoad();
            glRenderer = GL.GetString(StringName.Renderer) ?? "";
            if (opt.AutoQuality)
            {
                // The launcher sized the preset from RAM/CPU/disk; adjust once more for the actual graphics chip.
                string r = glRenderer.ToLowerInvariant();
                bool software = r.Contains("gdi generic") || r.Contains("llvmpipe") || r.Contains("basic render") || r.Contains("swiftshader");
                bool integrated = r.Contains("intel") && !r.Contains("arc");
                Quality q = gs.Quality;
                if (software) q = Quality.Low;
                else if (integrated && q > Quality.Medium) q = Quality.Medium;
                if (q != gs.Quality)
                {
                    int msaa = gs.Msaa;
                    gs = GraphicsSettings.For(q, Environment.ProcessorCount);
                    gs.Msaa = msaa;
                }
            }

            GL.Enable(EnableCap.DepthTest);
            GL.Enable(EnableCap.CullFace);
            GL.BlendFunc(BlendingFactor.SrcAlpha, BlendingFactor.OneMinusSrcAlpha);
            if (gs.Msaa > 0) GL.Enable(EnableCap.Multisample);

            chunkShader = new Shader(Shader.ChunkVs, Shader.ChunkFs);
            colorShader = new Shader(Shader.ColorVs, Shader.ColorFs);
            hud = new Hud();

            blockTex = GL.GenTexture();
            GL.BindTexture(TextureTarget.Texture2DArray, blockTex);
            GL.TexImage3D(TextureTarget.Texture2DArray, 0, PixelInternalFormat.Rgba8, TextureGen.S, TextureGen.S, T.Count, 0, PixelFormat.Rgba, PixelType.UnsignedByte, TextureGen.Build());
            GL.TexParameter(TextureTarget.Texture2DArray, TextureParameterName.TextureMinFilter, (int)TextureMinFilter.NearestMipmapLinear);
            GL.TexParameter(TextureTarget.Texture2DArray, TextureParameterName.TextureMagFilter, (int)TextureMagFilter.Nearest);
            GL.TexParameter(TextureTarget.Texture2DArray, TextureParameterName.TextureWrapS, (int)TextureWrapMode.ClampToEdge);
            GL.TexParameter(TextureTarget.Texture2DArray, TextureParameterName.TextureWrapT, (int)TextureWrapMode.ClampToEdge);
            GL.TexParameter(TextureTarget.Texture2DArray, TextureParameterName.TextureMaxLevel, 3);
            GL.GenerateMipmap(GenerateMipmapTarget.Texture2DArray);

            // One index buffer shared by every chunk: 0,1,2, 2,3,0 repeated.
            var idx = new uint[MaxQuads * 6];
            for (uint q = 0, i = 0; q < MaxQuads; q++)
            {
                uint b = q * 4;
                idx[i++] = b; idx[i++] = b + 1; idx[i++] = b + 2; idx[i++] = b + 2; idx[i++] = b + 3; idx[i++] = b;
            }
            sharedEbo = GL.GenBuffer();
            GL.BindBuffer(BufferTarget.ElementArrayBuffer, sharedEbo);
            GL.BufferData(BufferTarget.ElementArrayBuffer, idx.Length * sizeof(uint), idx, BufferUsageHint.StaticDraw);

            dynVao = GL.GenVertexArray(); dynVbo = GL.GenBuffer();
            SetupColorVao(dynVao, dynVbo);
            if (gs.Clouds) BuildClouds();

            int R = gs.RenderDistance, G = R + 2;
            meshR2 = (int)((R + 0.5f) * (R + 0.5f));
            var list = new List<(int, int, int)>();
            for (int dz = -G; dz <= G; dz++)
                for (int dx = -G; dx <= G; dx++)
                    if (dx * dx + dz * dz <= G * G) list.Add((dx, dz, dx * dx + dz * dz));
            list.Sort((a, b) => a.Item3.CompareTo(b.Item3));
            offsets = list.ToArray();

            SetScreen(Screen.Playing);
            lastTick = clock.Elapsed.TotalSeconds;
            if (opt.Parental != null) remaining = opt.Parental.RemainingSeconds();
            if (!world.SavingEnabled) Say("Disk is almost full - this session will not be saved.", 8);
        }

        static void SetupColorVao(int vao, int vbo)
        {
            GL.BindVertexArray(vao);
            GL.BindBuffer(BufferTarget.ArrayBuffer, vbo);
            GL.EnableVertexAttribArray(0); GL.VertexAttribPointer(0, 3, VertexAttribPointerType.Float, false, 28, 0);
            GL.EnableVertexAttribArray(1); GL.VertexAttribPointer(1, 4, VertexAttribPointerType.Float, false, 28, 12);
            GL.BindVertexArray(0);
        }

        const int CloudN = 64;
        const float CloudCell = 12f, CloudPeriod = CloudN * CloudCell;

        void BuildClouds()
        {
            var noise = new Perlin(world.Meta.Seed + 909);
            var v = new List<float>();
            for (int z = 0; z < CloudN; z++)
                for (int x = 0; x < CloudN; x++)
                {
                    if (noise.Fbm2(x * 0.09f, z * 0.09f, 2) < 0.1f) continue;
                    float x0 = x * CloudCell, z0 = z * CloudCell, x1 = x0 + CloudCell, z1 = z0 + CloudCell;
                    void P(float px, float pz) { v.Add(px); v.Add(0); v.Add(pz); v.Add(1); v.Add(1); v.Add(1); v.Add(0.82f); }
                    P(x0, z0); P(x1, z0); P(x1, z1); P(x0, z0); P(x1, z1); P(x0, z1);
                }
            cloudVerts = v.Count / 7;
            cloudVao = GL.GenVertexArray(); cloudVbo = GL.GenBuffer();
            SetupColorVao(cloudVao, cloudVbo);
            GL.BindBuffer(BufferTarget.ArrayBuffer, cloudVbo);
            GL.BufferData(BufferTarget.ArrayBuffer, v.Count * sizeof(float), v.ToArray(), BufferUsageHint.StaticDraw);
        }

        void SetScreen(Screen s)
        {
            screen = s;
            CursorState = s == Screen.Playing ? CursorState.Grabbed : CursorState.Normal;
            skipMouse = 3;
            breakProgress = 0;
        }

        void Say(string text, float seconds = 3) { msg = text; msgTimer = seconds; }

        // ------------------------------------------------------------------ chunk streaming

        void StreamChunks()
        {
            int pcx = (int)Math.Floor(player.Pos.X / 16), pcz = (int)Math.Floor(player.Pos.Z / 16);
            int maxGen = gs.Workers * 2, maxMesh = gs.Workers * 2;
            foreach (var (dx, dz, d2) in offsets)
            {
                if (genInFlight >= maxGen && meshInFlight >= maxMesh) break;
                int cx = pcx + dx, cz = pcz + dz;
                var c = world.GetChunk(cx, cz);
                if (c == null)
                {
                    if (genInFlight >= maxGen) continue;
                    var nc = world.CreateChunk(cx, cz);
                    Interlocked.Increment(ref genInFlight);
                    Task.Run(() =>
                    {
                        try { world.LoadOrGenerate(nc); }
                        finally { Interlocked.Decrement(ref genInFlight); }
                    });
                    continue;
                }
                if (!c.Generated || d2 > meshR2 || !c.MeshDirty || c.Meshing || meshInFlight >= maxMesh) continue;
                if (!world.NeighborsReady(cx, cz)) continue;
                c.Meshing = true;
                c.MeshDirty = false;
                int ver = c.MeshVersion;
                bool ao = gs.AO;
                Interlocked.Increment(ref meshInFlight);
                Task.Run(() =>
                {
                    MeshData m = null;
                    try { m = Mesher.Build(world, c, ao); m.Version = ver; }
                    catch { }
                    finally { Interlocked.Decrement(ref meshInFlight); }
                    uploads.Enqueue((c, m));
                });
            }
        }

        void ProcessUploads()
        {
            int budget = ready ? gs.UploadsPerFrame : 32;
            while (budget-- > 0 && uploads.TryDequeue(out var u))
            {
                var (c, m) = u;
                c.Meshing = false;
                if (m == null) { c.MeshDirty = true; continue; }
                if (m.Version != c.MeshVersion || world.GetChunk(c.CX, c.CZ) != c)
                {
                    if (m.Data != null) ArrayPool<uint>.Shared.Return(m.Data);
                    continue;
                }
                Upload(c, m);
            }
        }

        void Upload(Chunk c, MeshData m)
        {
            c.OpaqueQuads = Math.Min(m.OpaqueQuads, MaxQuads);
            c.WaterQuads = Math.Min(m.WaterQuads, MaxQuads);
            int quads = m.OpaqueQuads + m.WaterQuads;
            if (quads == 0) return;
            if (c.Vao == 0)
            {
                c.Vao = GL.GenVertexArray();
                c.Vbo = GL.GenBuffer();
                GL.BindVertexArray(c.Vao);
                GL.BindBuffer(BufferTarget.ArrayBuffer, c.Vbo);
                GL.EnableVertexAttribArray(0); GL.VertexAttribPointer(0, 4, VertexAttribPointerType.UnsignedByte, false, 8, 0);
                GL.EnableVertexAttribArray(1); GL.VertexAttribPointer(1, 4, VertexAttribPointerType.UnsignedByte, false, 8, 4);
                GL.BindBuffer(BufferTarget.ElementArrayBuffer, sharedEbo);
                GL.BindVertexArray(0);
            }
            GL.BindBuffer(BufferTarget.ArrayBuffer, c.Vbo);
            GL.BufferData(BufferTarget.ArrayBuffer, quads * 32, m.Data, BufferUsageHint.StaticDraw);
            ArrayPool<uint>.Shared.Return(m.Data);
            if (!c.InRenderList) { renderList.Add(c); c.InRenderList = true; }
        }

        void RemeshNow(Chunk c)
        {
            c.MeshVersion++;
            if (!world.NeighborsReady(c.CX, c.CZ)) { c.MeshDirty = true; return; }
            Upload(c, Mesher.Build(world, c, gs.AO));
            c.MeshDirty = false;
        }

        void SetBlockAndRemesh(int x, int y, int z, byte b)
        {
            if (world.SetBlock(x, y, z, b) == null) return;
            int cx = x >> 4, cz = z >> 4, lx = x & 15, lz = z & 15;
            for (int dz = -1; dz <= 1; dz++)
                for (int dx = -1; dx <= 1; dx++)
                {
                    if ((dx == -1 && lx != 0) || (dx == 1 && lx != 15) || (dz == -1 && lz != 0) || (dz == 1 && lz != 15)) continue;
                    var c = world.GetChunk(cx + dx, cz + dz);
                    if (c != null && c.Generated) RemeshNow(c);
                }
        }

        void UnloadFar()
        {
            int pcx = (int)Math.Floor(player.Pos.X / 16), pcz = (int)Math.Floor(player.Pos.Z / 16);
            int lim = gs.RenderDistance + 4;
            List<Chunk> drop = null;
            foreach (var c in world.Chunks)
            {
                if (Math.Abs(c.CX - pcx) <= lim && Math.Abs(c.CZ - pcz) <= lim) continue;
                if (!c.Generated || c.Meshing) continue;
                (drop ??= new List<Chunk>()).Add(c);
            }
            if (drop == null) return;
            foreach (var c in drop)
            {
                world.SaveChunk(c);
                if (c.Vao != 0) { GL.DeleteVertexArray(c.Vao); GL.DeleteBuffer(c.Vbo); c.Vao = 0; c.Vbo = 0; }
                if (c.InRenderList) { renderList.Remove(c); c.InRenderList = false; }
                world.Remove(c);
            }
        }

        // ------------------------------------------------------------------ update

        void TickParental()
        {
            if (opt.Parental == null) return;
            double now = clock.Elapsed.TotalSeconds, el = now - lastTick;
            if (el < 1) return;
            lastTick = now;
            opt.Parental.AddUsage(Math.Min(el, 30));
            sinceSave += el;
            if (sinceSave >= 15) { sinceSave = 0; opt.Parental.Save(); }
            remaining = opt.Parental.RemainingSeconds();
            if (remaining <= 0)
            {
                TimeUp = true;
                SaveEverything();
                Close();
                return;
            }
            if (remaining <= 300 && !warned5) { warned5 = true; if (remaining > 60) Say("5 minutes of play time left!", 6); }
            if (remaining <= 60 && !warned1) { warned1 = true; Say("1 minute left - the game will save and close.", 8); }
        }

        protected override void OnUpdateFrame(FrameEventArgs e)
        {
            base.OnUpdateFrame(e);
            float dt = (float)Math.Min(e.Time, 0.1);
            TickParental();
            if (IsExiting) return;

            var kb = KeyboardState;
            var ms = MouseState;
            if (ms.IsButtonPressed(MouseButton.Left)) uiClick = true;
            if (!IsFocused && screen == Screen.Playing && opt.SelfTestDir == null) SetScreen(Screen.Paused);

            if (kb.IsKeyPressed(Keys.F11)) WindowState = WindowState == WindowState.Fullscreen ? WindowState.Maximized : WindowState.Fullscreen;
            if (kb.IsKeyPressed(Keys.F3)) debug = !debug;
            if (kb.IsKeyPressed(Keys.Escape)) SetScreen(screen == Screen.Playing ? Screen.Paused : Screen.Playing);
            if (kb.IsKeyPressed(Keys.E) && screen != Screen.Paused) SetScreen(screen == Screen.Inventory ? Screen.Playing : Screen.Inventory);
            for (int i = 0; i < 9; i++) if (kb.IsKeyPressed(Keys.D1 + i)) player.Selected = i;

            StreamChunks();
            ProcessUploads();
            unloadTimer += dt;
            if (unloadTimer > 0.5f) { unloadTimer = 0; UnloadFar(); }
            autosaveTimer += dt;
            if (autosaveTimer > 60) { autosaveTimer = 0; SavePlayer(); world.SaveAll(); }

            if (!ready)
            {
                if (!world.IsLoaded((int)Math.Floor(player.Pos.X), (int)Math.Floor(player.Pos.Z))) return;
                // Never start inside a tree or wall.
                for (int i = 0; i < Chunk.SY && Blocks.Solid[world.GetBlock((int)Math.Floor(player.Pos.X), (int)Math.Floor(player.Pos.Y), (int)Math.Floor(player.Pos.Z))] ; i++) player.Pos.Y += 1;
                for (int i = 0; i < Chunk.SY && Blocks.Solid[world.GetBlock((int)Math.Floor(player.Pos.X), (int)Math.Floor(player.Pos.Y + 1), (int)Math.Floor(player.Pos.Z))]; i++) player.Pos.Y += 1;
                player.ResetFall();
                ready = true;
                readyAt = clock.Elapsed.TotalSeconds;
            }

            if (screen == Screen.Paused) return;
            timeOfDay = (timeOfDay + dt / DayLength) % 1f;
            cloudScroll += dt * 1.5f;
            if (msgTimer > 0) msgTimer -= dt;
            breakCooldown -= dt; placeCooldown -= dt;

            Vector3 wish = Vector3.Zero;
            bool jump = false, descend = false, sprint = false;
            if (screen == Screen.Playing)
            {
                if (skipMouse > 0) skipMouse--;
                else
                {
                    player.Yaw += ms.Delta.X * 0.0024f;
                    player.Pitch = Math.Clamp(player.Pitch - ms.Delta.Y * 0.0024f, -1.55f, 1.55f);
                }
                float sy = MathF.Sin(player.Yaw), cy = MathF.Cos(player.Yaw);
                var fwd = new Vector3(sy, 0, -cy);
                var right = new Vector3(cy, 0, sy);
                if (kb.IsKeyDown(Keys.W)) wish += fwd;
                if (kb.IsKeyDown(Keys.S)) wish -= fwd;
                if (kb.IsKeyDown(Keys.D)) wish += right;
                if (kb.IsKeyDown(Keys.A)) wish -= right;
                if (wish.LengthSquared > 0) wish.Normalize();
                jump = kb.IsKeyDown(Keys.Space);
                descend = kb.IsKeyDown(Keys.LeftShift);
                sprint = kb.IsKeyDown(Keys.LeftControl);
                if (kb.IsKeyPressed(Keys.F) && player.Creative)
                {
                    player.Flying = !player.Flying;
                    Say(player.Flying ? "Flying on (Space up, Shift down)" : "Flying off", 2);
                }
                if (ms.ScrollDelta.Y != 0) player.Selected = ((player.Selected - Math.Sign(ms.ScrollDelta.Y)) % 9 + 9) % 9;
            }

            player.Update(world, dt, wish, jump, descend, sprint);
            if (player.Pos.Y < -20 || player.Health <= 0)
            {
                player.Pos = spawn; player.Vel = Vector3d.Zero; player.Health = 20; player.Air = 10;
                player.ResetFall();
                Say("Ouch! You woke up back at camp.", 4);
            }

            hit = Ray.Cast(world, player.EyePos, player.Forward, player.Creative ? 7f : 5f);
            if (screen == Screen.Playing) Interact(ms, dt);
        }

        void Interact(MouseState ms, float dt)
        {
            if (ms.IsButtonDown(MouseButton.Left) && hit.Hit)
            {
                if (player.Creative)
                {
                    if (breakCooldown <= 0) { BreakBlock(); breakCooldown = 0.2f; }
                }
                else
                {
                    if (breakTarget != (hit.X, hit.Y, hit.Z)) { breakTarget = (hit.X, hit.Y, hit.Z); breakProgress = 0; }
                    float hard = Blocks.Info[hit.Block].Hardness;
                    if (!float.IsInfinity(hard))
                    {
                        breakProgress += dt / hard;
                        if (breakProgress >= 1) { BreakBlock(); breakProgress = 0; }
                    }
                }
            }
            else breakProgress = 0;

            if (hit.Hit && (ms.IsButtonPressed(MouseButton.Right) || (ms.IsButtonDown(MouseButton.Right) && placeCooldown <= 0)))
            {
                PlaceBlock();
                placeCooldown = 0.25f;
            }

            if (ms.IsButtonPressed(MouseButton.Middle) && hit.Hit)
            {
                int slot = Array.IndexOf(player.Hotbar, hit.Block);
                if (slot >= 0) player.Selected = slot;
                else if (player.Creative && hit.Block != B.Bedrock) player.Hotbar[player.Selected] = hit.Block;
            }
        }

        void BreakBlock()
        {
            if (hit.Block == B.Bedrock && !player.Creative) return;
            if (hit.Y <= 0) return;
            byte fill = B.Air;
            if (hit.Y <= TerrainGen.Sea &&
                (world.GetBlock(hit.X, hit.Y + 1, hit.Z) == B.Water || world.GetBlock(hit.X + 1, hit.Y, hit.Z) == B.Water ||
                 world.GetBlock(hit.X - 1, hit.Y, hit.Z) == B.Water || world.GetBlock(hit.X, hit.Y, hit.Z + 1) == B.Water ||
                 world.GetBlock(hit.X, hit.Y, hit.Z - 1) == B.Water))
                fill = B.Water; // water flows into the gap
            SetBlockAndRemesh(hit.X, hit.Y, hit.Z, fill);
            if (!player.Creative) player.Give(Blocks.Info[hit.Block].Drop, 1);
        }

        void PlaceBlock()
        {
            byte id = player.Hotbar[player.Selected];
            if (id == B.Air) return;
            if (!player.Creative && player.Counts[id] <= 0) { Say("You have no " + Blocks.Info[id].Name + " left.", 2); return; }
            int x = hit.PX, y = hit.PY, z = hit.PZ;
            if (y < 1 || y >= Chunk.SY) return;
            byte cur = world.GetBlock(x, y, z);
            if (cur != B.Air && cur != B.Water) return;
            if (player.IntersectsBlock(x, y, z)) return;
            SetBlockAndRemesh(x, y, z, id);
            if (!player.Creative) player.Counts[id]--;
        }

        // ------------------------------------------------------------------ render

        protected override void OnResize(ResizeEventArgs e)
        {
            base.OnResize(e);
            GL.Viewport(0, 0, FramebufferSize.X, FramebufferSize.Y);
        }

        static float Smooth(float a, float b, float x)
        {
            float t = Math.Clamp((x - a) / (b - a), 0, 1);
            return t * t * (3 - 2 * t);
        }

        protected override void OnRenderFrame(FrameEventArgs e)
        {
            base.OnRenderFrame(e);
            int W = FramebufferSize.X, H = FramebufferSize.Y;
            if (W <= 0 || H <= 0 || IsExiting) return;

            frames++; fpsTime += e.Time;
            if (fpsTime >= 0.5) { fps = (float)(frames / fpsTime); frames = 0; fpsTime = 0; }

            // Sky and light from the time of day.
            float ang = timeOfDay * MathF.PI * 2, sunH = MathF.Sin(ang);
            float day = Smooth(-0.12f, 0.22f, sunH);
            float light = 0.24f + 0.76f * day;
            var sky = Vector3.Lerp(new Vector3(0.02f, 0.03f, 0.09f), new Vector3(0.50f, 0.73f, 0.96f), day);
            float dusk = MathF.Max(0, 1 - MathF.Abs(sunH) / 0.3f) * 0.45f;
            sky = Vector3.Lerp(sky, new Vector3(0.96f, 0.56f, 0.30f), dusk);
            var dayTint = Vector3.Lerp(new Vector3(0.62f, 0.72f, 1f), Vector3.One, day) * light;

            float fogEnd = gs.RenderDistance * 16 - 8, fogStart = fogEnd * 0.62f;
            var fogCol = sky;
            if (player.HeadInWater) { fogCol = new Vector3(0.06f, 0.22f, 0.48f) * light; fogStart = 1; fogEnd = 26; }

            GL.ClearColor(fogCol.X, fogCol.Y, fogCol.Z, 1);
            GL.Clear(ClearBufferMask.ColorBufferBit | ClearBufferMask.DepthBufferBit);

            var cam = player.EyePos;
            var view = Matrix4.LookAt(Vector3.Zero, player.Forward, Vector3.UnitY);
            var proj = Matrix4.CreatePerspectiveFieldOfView(MathHelper.DegreesToRadians(70), W / (float)H, 0.12f, 2000f);
            var vp = view * proj;
            var frustum = new Frustum(vp);

            // Sun and moon.
            colorShader.Use();
            GL.UniformMatrix4(colorShader.Loc("uViewProj"), false, ref vp);
            GL.Uniform3(colorShader.Loc("uOffset"), Vector3.Zero);
            GL.Uniform4(colorShader.Loc("uTint"), Vector4.One);
            GL.Uniform1(colorShader.Loc("uFadeEnd"), 1e9f);
            if (!player.HeadInWater)
            {
                GL.Disable(EnableCap.DepthTest);
                GL.Disable(EnableCap.CullFace);
                var sunDir = Vector3.Normalize(new Vector3(MathF.Cos(ang), sunH, 0.3f));
                DrawBillboard(sunDir, 70, new Color4(1f, 0.95f, 0.62f, 1f));
                DrawBillboard(-sunDir, 50, new Color4(0.86f, 0.9f, 1f, 1f));
                GL.Enable(EnableCap.DepthTest);
                GL.Enable(EnableCap.CullFace);
            }

            // Terrain.
            chunkShader.Use();
            GL.UniformMatrix4(chunkShader.Loc("uViewProj"), false, ref vp);
            GL.Uniform3(chunkShader.Loc("uFogColor"), fogCol);
            GL.Uniform1(chunkShader.Loc("uFogStart"), fogStart);
            GL.Uniform1(chunkShader.Loc("uFogEnd"), fogEnd);
            GL.Uniform3(chunkShader.Loc("uDay"), dayTint);
            GL.Uniform1(chunkShader.Loc("uAlpha"), 1f);
            GL.Uniform1(chunkShader.Loc("uTex"), 0);
            GL.ActiveTexture(TextureUnit.Texture0);
            GL.BindTexture(TextureTarget.Texture2DArray, blockTex);
            int offLoc = chunkShader.Loc("uOffset");
            float maxDist = gs.RenderDistance * 16 + 24;
            int drawn = 0;
            visible.Clear();
            foreach (var c in renderList)
            {
                var o = new Vector3((float)(c.CX * 16 - cam.X), (float)-cam.Y, (float)(c.CZ * 16 - cam.Z));
                float ddx = o.X + 8, ddz = o.Z + 8;
                if (ddx * ddx + ddz * ddz > maxDist * maxDist) continue;
                if (!frustum.Visible(o, o + new Vector3(16, Chunk.SY, 16))) continue;
                visible.Add((c, o));
                if (c.OpaqueQuads == 0) continue;
                GL.Uniform3(offLoc, o);
                GL.BindVertexArray(c.Vao);
                GL.DrawElements(PrimitiveType.Triangles, c.OpaqueQuads * 6, DrawElementsType.UnsignedInt, 0);
                drawn++;
            }

            // Outline around the targeted block.
            colorShader.Use();
            if (hit.Hit && screen == Screen.Playing)
            {
                GL.Uniform3(colorShader.Loc("uOffset"), new Vector3((float)(hit.X - cam.X), (float)(hit.Y - cam.Y), (float)(hit.Z - cam.Z)));
                DrawOutline();
            }

            GL.Enable(EnableCap.Blend);
            GL.Disable(EnableCap.CullFace);
            if (gs.Clouds && cloudVerts > 0)
            {
                GL.DepthMask(false);
                GL.Uniform4(colorShader.Loc("uTint"), new Vector4(light, light, light, 1));
                GL.Uniform1(colorShader.Loc("uFadeEnd"), MathF.Max(260, fogEnd * 2.4f));
                GL.BindVertexArray(cloudVao);
                double bx = Math.Floor((cam.X - cloudScroll) / CloudPeriod) * CloudPeriod + cloudScroll;
                double bz = Math.Floor(cam.Z / CloudPeriod) * CloudPeriod;
                for (int iz = -1; iz <= 1; iz++)
                    for (int ix = -1; ix <= 1; ix++)
                    {
                        GL.Uniform3(colorShader.Loc("uOffset"), new Vector3((float)(bx + ix * CloudPeriod - cam.X), (float)(150 - cam.Y), (float)(bz + iz * CloudPeriod - cam.Z)));
                        GL.DrawArrays(PrimitiveType.Triangles, 0, cloudVerts);
                    }
                GL.DepthMask(true);
            }

            // Water last, see-through.
            chunkShader.Use();
            GL.Uniform1(chunkShader.Loc("uAlpha"), 0.72f);
            foreach (var (c, o) in visible)
            {
                if (c.WaterQuads == 0) continue;
                GL.Uniform3(offLoc, o);
                GL.BindVertexArray(c.Vao);
                GL.DrawElementsBaseVertex(PrimitiveType.Triangles, c.WaterQuads * 6, DrawElementsType.UnsignedInt, IntPtr.Zero, c.OpaqueQuads * 4);
            }

            // HUD (2D, so no depth test or face culling).
            GL.Disable(EnableCap.DepthTest);
            DrawHud(W, H, drawn);
            hud.Flush(W, H, blockTex);
            GL.Enable(EnableCap.CullFace);
            GL.Disable(EnableCap.Blend);
            GL.Enable(EnableCap.DepthTest);
            GL.BindVertexArray(0);

            uiClick = false;
            if (opt.SelfTestDir != null) SelfTest(W, H);
            SwapBuffers();
        }

        readonly List<(Chunk c, Vector3 o)> visible = new List<(Chunk, Vector3)>();

        void DynDraw(int floats, PrimitiveType type)
        {
            GL.BindVertexArray(dynVao);
            GL.BindBuffer(BufferTarget.ArrayBuffer, dynVbo);
            GL.BufferData(BufferTarget.ArrayBuffer, floats * sizeof(float), dyn, BufferUsageHint.StreamDraw);
            GL.DrawArrays(type, 0, floats / 7);
        }

        void DrawBillboard(Vector3 dir, float size, Color4 col)
        {
            var c = dir * 800;
            var r = Vector3.Normalize(Vector3.Cross(dir, Vector3.UnitZ)) * size;
            var u = Vector3.Normalize(Vector3.Cross(r, dir)) * size;
            Vector3[] p = { c - r - u, c + r - u, c + r + u, c - r - u, c + r + u, c - r + u };
            int n = 0;
            foreach (var v in p)
            {
                dyn[n++] = v.X; dyn[n++] = v.Y; dyn[n++] = v.Z;
                dyn[n++] = col.R; dyn[n++] = col.G; dyn[n++] = col.B; dyn[n++] = col.A;
            }
            DynDraw(n, PrimitiveType.Triangles);
        }

        static readonly int[] BoxEdges = { 0, 1, 1, 3, 3, 2, 2, 0, 4, 5, 5, 7, 7, 6, 6, 4, 0, 4, 1, 5, 2, 6, 3, 7 };

        void DrawOutline()
        {
            const float lo = -0.004f, hi = 1.004f;
            int n = 0;
            foreach (int k in BoxEdges)
            {
                dyn[n++] = (k & 1) != 0 ? hi : lo; dyn[n++] = (k & 2) != 0 ? hi : lo; dyn[n++] = (k & 4) != 0 ? hi : lo;
                dyn[n++] = 0; dyn[n++] = 0; dyn[n++] = 0; dyn[n++] = 0.9f;
            }
            DynDraw(n, PrimitiveType.Lines);
        }

        // ------------------------------------------------------------------ HUD

        static readonly Color4 White = new Color4(1f, 1f, 1f, 1f);
        static readonly Color4 Dim = new Color4(0.75f, 0.8f, 0.86f, 1f);
        static readonly string[] Heart = { ".##.##.", "#######", "#######", ".#####.", "..###..", "...#..." };

        bool Button(float x, float y, float w, float h, string label, float ui, bool enabled = true)
        {
            var mp = MouseState.Position;
            bool hover = enabled && screen != Screen.Playing && mp.X >= x && mp.X < x + w && mp.Y >= y && mp.Y < y + h;
            hud.Rect(x, y, w, h, hover ? new Color4(0.30f, 0.62f, 0.33f, 0.95f) : new Color4(0.16f, 0.2f, 0.26f, 0.92f));
            hud.Frame(x, y, w, h, MathF.Max(1, 1.5f * ui), new Color4(1f, 1f, 1f, enabled ? 0.35f : 0.12f));
            float th = MathF.Min(h * 0.5f, 20 * ui);
            hud.TextCentered(x + w / 2, y + (h - th) / 2, th, label, enabled ? White : new Color4(0.55f, 0.58f, 0.62f, 1f));
            return hover && uiClick;
        }

        static string Clock(double sec)
        {
            int s = (int)Math.Ceiling(sec);
            return s >= 3600 ? $"{s / 3600}:{s / 60 % 60:00}:{s % 60:00}" : $"{s / 60}:{s % 60:00}";
        }

        void DrawHud(int W, int H, int drawn)
        {
            float ui = MathF.Max(0.7f, H / 720f);

            if (!ready)
            {
                hud.Rect(0, 0, W, H, new Color4(0.1f, 0.13f, 0.18f, 1f));
                hud.TextCentered(W / 2f, H / 2f - 20 * ui, 34 * ui, "Building the world...", White);
                return;
            }
            if (player.HeadInWater) hud.Rect(0, 0, W, H, new Color4(0.05f, 0.25f, 0.6f, 0.28f));
            if (player.HurtFlash > 0) hud.Rect(0, 0, W, H, new Color4(0.9f, 0.1f, 0.1f, player.HurtFlash * 0.6f));

            // Crosshair
            if (screen == Screen.Playing)
            {
                float cl = 9 * ui, ct = MathF.Max(2, 2 * ui);
                hud.Rect(W / 2f - cl, H / 2f - ct / 2, cl * 2, ct, new Color4(1f, 1f, 1f, 0.85f));
                hud.Rect(W / 2f - ct / 2, H / 2f - cl, ct, cl * 2, new Color4(1f, 1f, 1f, 0.85f));
                if (breakProgress > 0)
                {
                    float bw = 70 * ui;
                    hud.Rect(W / 2f - bw / 2, H / 2f + 22 * ui, bw, 7 * ui, new Color4(0, 0, 0, 0.6f));
                    hud.Rect(W / 2f - bw / 2, H / 2f + 22 * ui, bw * MathF.Min(1, breakProgress), 7 * ui, new Color4(0.95f, 0.85f, 0.3f, 1f));
                }
            }

            // Hotbar
            float slot = 54 * ui, barW = slot * 9, bx = (W - barW) / 2, by = H - slot - 12 * ui;
            for (int i = 0; i < 9; i++)
            {
                float x = bx + i * slot;
                hud.Rect(x, by, slot, slot, new Color4(0, 0, 0, 0.5f));
                byte id = player.Hotbar[i];
                if (id != 0)
                {
                    bool have = player.Creative || player.Counts[id] > 0;
                    hud.Icon(x + 9 * ui, by + 9 * ui, slot - 18 * ui, Blocks.Info[id].Side, have ? White : new Color4(0.35f, 0.35f, 0.35f, 0.8f));
                    if (!player.Creative)
                    {
                        string n = player.Counts[id].ToString();
                        hud.Text(x + slot - 5 * ui - hud.TextWidth(n, 15 * ui), by + slot - 19 * ui, 15 * ui, n, White);
                    }
                }
                bool sel = i == player.Selected;
                hud.Frame(x, by, slot, slot, sel ? 3 * ui : 1.2f * ui, sel ? White : new Color4(1f, 1f, 1f, 0.3f));
            }
            byte held = player.Hotbar[player.Selected];
            if (held != 0) hud.TextCentered(W / 2f, by - 54 * ui, 17 * ui, Blocks.Info[held].Name, White);

            // Hearts and air (survival)
            if (!player.Creative)
            {
                float px = 2.6f * ui, hy = by - 24 * ui;
                for (int i = 0; i < 10; i++)
                {
                    float hp = player.Health - i * 2;
                    var col = hp >= 2 ? new Color4(0.92f, 0.16f, 0.2f, 1f) : hp >= 1 ? new Color4(0.95f, 0.55f, 0.55f, 1f) : new Color4(0.15f, 0.15f, 0.17f, 0.8f);
                    float hx = bx + i * 9 * px;
                    for (int r = 0; r < Heart.Length; r++)
                        for (int cix = 0; cix < 7; cix++)
                            if (Heart[r][cix] == '#') hud.Rect(hx + cix * px, hy + r * px, px, px, col);
                }
                if (player.Air < 10)
                {
                    int bubbles = (int)MathF.Ceiling(MathF.Max(0, player.Air));
                    for (int i = 0; i < bubbles; i++)
                        hud.Rect(bx + barW - (i + 1) * 20 * ui, hy + 2 * ui, 13 * ui, 13 * ui, new Color4(0.45f, 0.75f, 1f, 0.95f));
                }
            }

            // Play time left
            if (!double.IsInfinity(remaining))
            {
                string t = "Play time left " + Clock(remaining);
                var col = remaining <= 60 ? new Color4(1f, 0.35f, 0.3f, 1f) : remaining <= 300 ? new Color4(1f, 0.85f, 0.3f, 1f) : White;
                float tw = hud.TextWidth(t, 20 * ui);
                hud.Rect(W - tw - 26 * ui, 10 * ui, tw + 16 * ui, 30 * ui, new Color4(0, 0, 0, 0.45f));
                hud.Text(W - tw - 18 * ui, 15 * ui, 20 * ui, t, col);
            }

            if (msgTimer > 0)
            {
                float a = MathF.Min(1, msgTimer * 2);
                float tw = hud.TextWidth(msg, 22 * ui);
                hud.Rect(W / 2f - tw / 2 - 14 * ui, 62 * ui, tw + 28 * ui, 36 * ui, new Color4(0, 0, 0, 0.55f * a));
                hud.TextCentered(W / 2f, 69 * ui, 22 * ui, msg, new Color4(1f, 1f, 1f, a));
            }

            if (debug)
            {
                string[] lines =
                {
                    $"{fps:0} fps   {gs.Quality} ({gs.RenderDistance} chunks, AA x{gs.Msaa})",
                    $"pos {player.Pos.X:0.0} {player.Pos.Y:0.0} {player.Pos.Z:0.0}",
                    $"chunks {world.ChunkCount} loaded, {drawn} drawn",
                    glRenderer,
                };
                for (int i = 0; i < lines.Length; i++) hud.Text(10 * ui, (10 + i * 20) * ui, 16 * ui, lines[i], White);
            }
            else hud.Text(10 * ui, 10 * ui, 14 * ui, (player.Creative ? "Creative" : "Survival") + "  |  E blocks  |  Esc menu", new Color4(1f, 1f, 1f, 0.6f));

            if (screen == Screen.Inventory) DrawInventory(W, H, ui);
            else if (screen == Screen.Paused) DrawPause(W, H, ui);
        }

        void DrawInventory(int W, int H, float ui)
        {
            hud.Rect(0, 0, W, H, new Color4(0, 0, 0, 0.45f));
            float pw = 860 * ui, ph = 470 * ui, x0 = (W - pw) / 2, y0 = (H - ph) / 2 - 30 * ui;
            hud.Rect(x0, y0, pw, ph, new Color4(0.12f, 0.15f, 0.2f, 0.96f));
            hud.Frame(x0, y0, pw, ph, 2 * ui, new Color4(1f, 1f, 1f, 0.3f));
            hud.Text(x0 + 20 * ui, y0 + 14 * ui, 24 * ui, player.Creative ? "All blocks" : "Backpack", White);
            hud.Text(x0 + 20 * ui, y0 + 44 * ui, 14 * ui, "Click a block to put it in the selected slot. Keys 1-9 choose the slot.", Dim);

            var mp = MouseState.Position;
            float cell = 62 * ui, gx = x0 + 20 * ui, gy = y0 + 74 * ui;
            int cols = player.Creative ? 12 : 6, shown = 0;
            string hoverName = null;
            for (int k = 0; k < Blocks.Palette.Length; k++)
            {
                byte id = Blocks.Palette[k];
                if (!player.Creative && player.Counts[id] <= 0) continue;
                float x = gx + (shown % cols) * cell, y = gy + (shown / cols) * cell;
                shown++;
                bool hover = mp.X >= x && mp.X < x + cell - 6 * ui && mp.Y >= y && mp.Y < y + cell - 6 * ui;
                hud.Rect(x, y, cell - 6 * ui, cell - 6 * ui, hover ? new Color4(1f, 1f, 1f, 0.22f) : new Color4(0, 0, 0, 0.4f));
                hud.Icon(x + 8 * ui, y + 8 * ui, cell - 22 * ui, Blocks.Info[id].Side, White);
                if (!player.Creative)
                {
                    string n = player.Counts[id].ToString();
                    hud.Text(x + cell - 10 * ui - hud.TextWidth(n, 15 * ui), y + cell - 26 * ui, 15 * ui, n, White);
                }
                if (hover)
                {
                    hoverName = Blocks.Info[id].Name;
                    if (uiClick) player.Hotbar[player.Selected] = id;
                }
            }
            if (shown == 0) hud.Text(gx, gy + 10 * ui, 17 * ui, "Empty. Hold the left mouse button on a block to dig it.", Dim);
            if (hoverName != null) hud.Text(x0 + 20 * ui, y0 + ph - 32 * ui, 18 * ui, hoverName, White);

            if (!player.Creative)
            {
                float rx = x0 + 420 * ui, ry = y0 + 74 * ui, rw = pw - 440 * ui, rh = 40 * ui;
                hud.Text(rx, y0 + 14 * ui, 24 * ui, "Crafting", White);
                foreach (var r in Blocks.Recipes)
                {
                    bool can = player.CanCraft(r);
                    string need = "";
                    foreach (var (id, n) in r.In) need += (need.Length > 0 ? " + " : "") + n + " " + Blocks.Info[id].Name;
                    if (Button(rx, ry, rw, rh, "", ui, can)) player.Craft(r);
                    hud.Icon(rx + 6 * ui, ry + 5 * ui, rh - 10 * ui, Blocks.Info[r.Out].Side, can ? White : new Color4(0.5f, 0.5f, 0.5f, 1f));
                    hud.Text(rx + rh + 4 * ui, ry + 4 * ui, 15 * ui, r.OutCount + " " + Blocks.Info[r.Out].Name, can ? White : Dim);
                    hud.Text(rx + rh + 4 * ui, ry + 21 * ui, 12.5f * ui, "needs " + need, can ? new Color4(0.7f, 1f, 0.7f, 1f) : new Color4(0.6f, 0.62f, 0.66f, 1f));
                    ry += rh + 6 * ui;
                }
            }
        }

        void DrawPause(int W, int H, float ui)
        {
            hud.Rect(0, 0, W, H, new Color4(0, 0, 0, 0.55f));
            float bw = 320 * ui, bh = 50 * ui, x = (W - bw) / 2, y = H * 0.2f;
            hud.TextCentered(W / 2f, y - 64 * ui, 40 * ui, "Paused", White);
            if (Button(x, y, bw, bh, "Back to game", ui)) SetScreen(Screen.Playing);
            if (Button(x, y + 62 * ui, bw, bh, "Save and quit", ui)) { SaveEverything(); Close(); }
            string[] help =
            {
                "W A S D  move        Space  jump / swim up     Ctrl  run",
                "Left mouse  dig      Right mouse  place        Middle mouse  pick block",
                "1-9 or wheel  choose block      E  backpack and crafting",
                "F  fly (Creative)    F3  details    F11  full screen",
            };
            for (int i = 0; i < help.Length; i++)
                hud.TextCentered(W / 2f, y + (150 + i * 26) * ui, 16 * ui, help[i], Dim);
        }

        // ------------------------------------------------------------------ shutdown / self-test

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            if (opt.SelfTestDir == null) SaveEverything();
            base.OnClosing(e);
        }

        void SelfTest(int W, int H)
        {
            if (!ready) return;
            double t = clock.Elapsed.TotalSeconds - readyAt;
            int target = (int)(Math.PI * gs.RenderDistance * gs.RenderDistance * 0.85);
            void Shot(string name)
            {
                var px = new byte[W * H * 4];
                GL.ReadBuffer(ReadBufferMode.Back);
                GL.ReadPixels(0, 0, W, H, PixelFormat.Bgra, PixelType.UnsignedByte, px);
                FontAtlas.SavePng(Path.Combine(opt.SelfTestDir, name), px, W, H);
            }
            if (shotStage == 0 && (renderList.Count >= target || t > 25))
            {
                Shot("world.png");
                File.WriteAllText(Path.Combine(opt.SelfTestDir, "stats.txt"),
                    $"renderer={glRenderer}\nquality={gs.Quality}\nfps={fps:0}\nchunksMeshed={renderList.Count}\nloadSeconds={t:0.0}\npos={player.Pos}\nerror={GL.GetError()}\n");
                player.Counts[B.Log] = 3; player.Counts[B.Sand] = 5; player.Counts[B.Cobble] = 12;
                SetScreen(Screen.Inventory);
                shotStage = 1;
            }
            else if (shotStage >= 1 && shotStage < 4) shotStage++;
            else if (shotStage == 4) { Shot("inventory.png"); SetScreen(Screen.Paused); shotStage = 5; }
            else if (shotStage >= 5 && shotStage < 8) shotStage++;
            else if (shotStage == 8) { Shot("pause.png"); shotStage = 9; Close(); }
        }
    }
}
