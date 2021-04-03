using System;

namespace Voting
{
    public class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Who are you?");

            switch (Console.ReadLine())
            {
                case "server":
                    new Server();
                    break;
                case "client":
                    new Client();
                    break;
            }
        }
    }
}
