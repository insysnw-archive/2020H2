using System;

namespace TcpChat
{
    class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Who are you?");

            var input = Console.ReadLine();

            if (input == "server")
            {
                new Server();
            }
            else if (input == "client")
            {
                new Client();
            }
        }
    }
}
