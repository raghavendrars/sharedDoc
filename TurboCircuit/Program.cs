namespace TurboCircuit;

static class Program
{
    static void Main(string[] args)
    {
        new Game(args.Contains("--selftest")).Run();
    }
}
