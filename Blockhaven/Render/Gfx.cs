using System;
using System.Collections.Generic;
using OpenTK.Graphics.OpenGL4;
using OpenTK.Mathematics;

namespace Blockhaven
{
    public sealed class Shader
    {
        public readonly int Id;
        readonly Dictionary<string, int> locs = new Dictionary<string, int>();

        public Shader(string vs, string fs)
        {
            int v = Compile(ShaderType.VertexShader, vs), f = Compile(ShaderType.FragmentShader, fs);
            Id = GL.CreateProgram();
            GL.AttachShader(Id, v);
            GL.AttachShader(Id, f);
            GL.LinkProgram(Id);
            GL.GetProgram(Id, GetProgramParameterName.LinkStatus, out int ok);
            if (ok == 0) throw new Exception("Shader link failed: " + GL.GetProgramInfoLog(Id));
            GL.DeleteShader(v);
            GL.DeleteShader(f);
        }

        static int Compile(ShaderType type, string src)
        {
            int s = GL.CreateShader(type);
            GL.ShaderSource(s, src);
            GL.CompileShader(s);
            GL.GetShader(s, ShaderParameter.CompileStatus, out int ok);
            if (ok == 0) throw new Exception(type + " compile failed: " + GL.GetShaderInfoLog(s));
            return s;
        }

        public void Use() => GL.UseProgram(Id);

        public int Loc(string name)
        {
            if (!locs.TryGetValue(name, out int l)) locs[name] = l = GL.GetUniformLocation(Id, name);
            return l;
        }

        public const string ChunkVs = @"#version 330 core
layout(location=0) in vec4 aPos;
layout(location=1) in vec4 aTex;
uniform mat4 uViewProj;
uniform vec3 uOffset;
out vec3 vUv;
out float vLight;
out float vDist;
void main(){
    vec3 p = aPos.xyz;
    if (aTex.w > 0.5) p.y -= 0.125;
    vec3 wp = p + uOffset;
    gl_Position = uViewProj * vec4(wp, 1.0);
    vUv = aTex.xyz;
    vLight = aPos.w / 255.0;
    vDist = length(wp);
}";
        public const string ChunkFs = @"#version 330 core
in vec3 vUv;
in float vLight;
in float vDist;
uniform sampler2DArray uTex;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform vec3 uDay;
uniform float uAlpha;
out vec4 o;
void main(){
    vec4 c = texture(uTex, vUv);
    if (c.a < 0.5) discard;
    vec3 rgb = c.rgb * vLight * uDay;
    float f = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
    o = vec4(mix(rgb, uFogColor, f), uAlpha);
}";
        public const string ColorVs = @"#version 330 core
layout(location=0) in vec3 aPos;
layout(location=1) in vec4 aCol;
uniform mat4 uViewProj;
uniform vec3 uOffset;
out vec4 vCol;
out float vDist;
void main(){
    vec3 p = aPos + uOffset;
    gl_Position = uViewProj * vec4(p, 1.0);
    vCol = aCol;
    vDist = length(p.xz);
}";
        public const string ColorFs = @"#version 330 core
in vec4 vCol;
in float vDist;
uniform vec4 uTint;
uniform float uFadeEnd;
out vec4 o;
void main(){
    o = vCol * uTint;
    o.a *= 1.0 - smoothstep(uFadeEnd * 0.55, uFadeEnd, vDist);
}";
        public const string HudVs = @"#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUv;
layout(location=2) in vec4 aCol;
layout(location=3) in float aMode;
uniform vec2 uScreen;
out vec2 vUv;
out vec4 vCol;
flat out float vMode;
void main(){
    gl_Position = vec4(aPos.x / uScreen.x * 2.0 - 1.0, 1.0 - aPos.y / uScreen.y * 2.0, 0.0, 1.0);
    vUv = aUv; vCol = aCol; vMode = aMode;
}";
        public const string HudFs = @"#version 330 core
in vec2 vUv;
in vec4 vCol;
flat in float vMode;
uniform sampler2D uFont;
uniform sampler2DArray uTex;
out vec4 o;
void main(){
    if (vMode < -1.5) {
        o = vec4(vCol.rgb, vCol.a * texture(uFont, vUv).a);
    } else if (vMode < -0.5) {
        o = vCol;
    } else {
        vec4 t = texture(uTex, vec3(vUv, floor(vMode + 0.5)));
        o = vec4(t.rgb * vCol.rgb, max(t.a, 0.45) * vCol.a);
    }
}";
    }

    /// <summary>Batches all 2D HUD drawing (rectangles, text, block icons) into one draw call.</summary>
    public sealed class Hud
    {
        const int Stride = 9;
        float[] v = new float[9 * 6 * 2048];
        int n;
        readonly int vao, vbo, fontTex;
        readonly int atlasW, atlasH, cellW, cellH;
        readonly Shader shader;

        public Hud()
        {
            shader = new Shader(Shader.HudVs, Shader.HudFs);
            var px = FontAtlas.Build(out atlasW, out atlasH, out cellW, out cellH);
            fontTex = GL.GenTexture();
            GL.BindTexture(TextureTarget.Texture2D, fontTex);
            GL.TexImage2D(TextureTarget.Texture2D, 0, PixelInternalFormat.Rgba8, atlasW, atlasH, 0, PixelFormat.Bgra, PixelType.UnsignedByte, px);
            GL.TexParameter(TextureTarget.Texture2D, TextureParameterName.TextureMinFilter, (int)TextureMinFilter.Linear);
            GL.TexParameter(TextureTarget.Texture2D, TextureParameterName.TextureMagFilter, (int)TextureMagFilter.Linear);
            GL.TexParameter(TextureTarget.Texture2D, TextureParameterName.TextureWrapS, (int)TextureWrapMode.ClampToEdge);
            GL.TexParameter(TextureTarget.Texture2D, TextureParameterName.TextureWrapT, (int)TextureWrapMode.ClampToEdge);

            vao = GL.GenVertexArray();
            vbo = GL.GenBuffer();
            GL.BindVertexArray(vao);
            GL.BindBuffer(BufferTarget.ArrayBuffer, vbo);
            int s = Stride * sizeof(float);
            GL.EnableVertexAttribArray(0); GL.VertexAttribPointer(0, 2, VertexAttribPointerType.Float, false, s, 0);
            GL.EnableVertexAttribArray(1); GL.VertexAttribPointer(1, 2, VertexAttribPointerType.Float, false, s, 2 * sizeof(float));
            GL.EnableVertexAttribArray(2); GL.VertexAttribPointer(2, 4, VertexAttribPointerType.Float, false, s, 4 * sizeof(float));
            GL.EnableVertexAttribArray(3); GL.VertexAttribPointer(3, 1, VertexAttribPointerType.Float, false, s, 8 * sizeof(float));
            GL.BindVertexArray(0);
        }

        void Vert(float x, float y, float u, float w, Color4 c, float mode)
        {
            if (n + Stride > v.Length) Array.Resize(ref v, v.Length * 2);
            v[n++] = x; v[n++] = y; v[n++] = u; v[n++] = w;
            v[n++] = c.R; v[n++] = c.G; v[n++] = c.B; v[n++] = c.A;
            v[n++] = mode;
        }

        void Quad(float x, float y, float w, float h, float u0, float v0, float u1, float v1, Color4 c, float mode)
        {
            Vert(x, y, u0, v0, c, mode); Vert(x + w, y, u1, v0, c, mode); Vert(x + w, y + h, u1, v1, c, mode);
            Vert(x, y, u0, v0, c, mode); Vert(x + w, y + h, u1, v1, c, mode); Vert(x, y + h, u0, v1, c, mode);
        }

        public void Rect(float x, float y, float w, float h, Color4 c) => Quad(x, y, w, h, 0, 0, 0, 0, c, -1);

        public void Frame(float x, float y, float w, float h, float t, Color4 c)
        {
            Rect(x, y, w, t, c); Rect(x, y + h - t, w, t, c);
            Rect(x, y + t, t, h - 2 * t, c); Rect(x + w - t, y + t, t, h - 2 * t, c);
        }

        public void Icon(float x, float y, float size, int layer, Color4 tint) => Quad(x, y, size, size, 0, 0, 1, 1, tint, layer);

        public float TextWidth(string s, float h) => s.Length * cellW * (h / cellH);

        public void Text(float x, float y, float h, string s, Color4 c, bool shadow = true)
        {
            if (shadow) TextRaw(x + h * 0.08f, y + h * 0.08f, h, s, new Color4(0, 0, 0, c.A * 0.75f));
            TextRaw(x, y, h, s, c);
        }

        public void TextCentered(float cx, float y, float h, string s, Color4 c) => Text(cx - TextWidth(s, h) / 2, y, h, s, c);

        void TextRaw(float x, float y, float h, string s, Color4 c)
        {
            float adv = cellW * (h / cellH);
            for (int i = 0; i < s.Length; i++, x += adv)
            {
                int ch = s[i];
                if (ch == ' ') continue;
                if (ch < 32 || ch > 126) ch = '?';
                int k = ch - 32;
                float u0 = (k % FontAtlas.Cols) * cellW / (float)atlasW, v0 = (k / FontAtlas.Cols) * cellH / (float)atlasH;
                Quad(x, y, adv, h, u0, v0, u0 + cellW / (float)atlasW, v0 + cellH / (float)atlasH, c, -2);
            }
        }

        public void Flush(int screenW, int screenH, int blockTexArray)
        {
            if (n == 0) return;
            shader.Use();
            GL.Uniform2(shader.Loc("uScreen"), (float)screenW, (float)screenH);
            GL.Uniform1(shader.Loc("uFont"), 1);
            GL.Uniform1(shader.Loc("uTex"), 0);
            GL.ActiveTexture(TextureUnit.Texture1);
            GL.BindTexture(TextureTarget.Texture2D, fontTex);
            GL.ActiveTexture(TextureUnit.Texture0);
            GL.BindTexture(TextureTarget.Texture2DArray, blockTexArray);
            GL.BindVertexArray(vao);
            GL.BindBuffer(BufferTarget.ArrayBuffer, vbo);
            GL.BufferData(BufferTarget.ArrayBuffer, n * sizeof(float), v, BufferUsageHint.StreamDraw);
            GL.DrawArrays(PrimitiveType.Triangles, 0, n / Stride);
            GL.BindVertexArray(0);
            n = 0;
        }
    }

    public struct Frustum
    {
        Vector4 p0, p1, p2, p3, p4, p5;

        public Frustum(Matrix4 m)
        {
            var c0 = new Vector4(m.M11, m.M21, m.M31, m.M41);
            var c1 = new Vector4(m.M12, m.M22, m.M32, m.M42);
            var c2 = new Vector4(m.M13, m.M23, m.M33, m.M43);
            var c3 = new Vector4(m.M14, m.M24, m.M34, m.M44);
            p0 = c3 + c0; p1 = c3 - c0; p2 = c3 + c1; p3 = c3 - c1; p4 = c3 + c2; p5 = c3 - c2;
        }

        static bool Out(in Vector4 p, Vector3 mn, Vector3 mx) =>
            p.X * (p.X >= 0 ? mx.X : mn.X) + p.Y * (p.Y >= 0 ? mx.Y : mn.Y) + p.Z * (p.Z >= 0 ? mx.Z : mn.Z) + p.W < 0;

        public bool Visible(Vector3 mn, Vector3 mx) =>
            !(Out(p0, mn, mx) || Out(p1, mn, mx) || Out(p2, mn, mx) || Out(p3, mn, mx) || Out(p4, mn, mx) || Out(p5, mn, mx));
    }
}
