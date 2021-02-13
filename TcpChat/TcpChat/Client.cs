using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;

namespace TcpChat
{
    class Client
    {
        readonly string ip = "127.0.0.1";
        readonly int port = 1234;
        readonly Socket socket;

        public Client()
        {
            try
            {
                IPEndPoint ipPoint = new IPEndPoint(IPAddress.Parse(ip), port);
                socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                socket.Connect(ipPoint);
                Console.WriteLine(socket.LocalEndPoint);

                var thread = new Thread(() => GetMessages());
                thread.Start();

                while (true)
                {
                    SendMessage();
                }
            }
            catch (Exception ex)
            {
                socket.Shutdown(SocketShutdown.Both);
                socket.Close();
                Console.WriteLine(ex.Message);
            }
        }

        public void GetMessages()
        {
            while (true)
            {
                try
                {
                    GetMessage();
                }
                catch (Exception)
                {
                    Console.WriteLine("Oops, the server was down...");
                    Environment.Exit(0);
                }
            }
        }

        public void SendMessage()
        {
            string message = $"{DateTime.UtcNow}{Console.ReadLine()}";
            byte[] data = Encoding.Unicode.GetBytes(message);
            socket.Send(data);
        }

        public void SendMessage(DateTime dateTime)
        {
            string message = $"{dateTime} {Console.ReadLine()}";
            byte[] data = Encoding.Unicode.GetBytes(message);
            socket.Send(data);
        }

        public void GetMessage()
        {
            var data = new byte[256];
            StringBuilder builder = new StringBuilder();
            do
            {
                int bytes = socket.Receive(data, data.Length, 0);
                builder.Append(Encoding.Unicode.GetString(data, 0, bytes));
            }
            while (socket.Available > 0);
            Console.WriteLine(MessageWithTime(builder.ToString()));
        }

        public string MessageWithTime(string message)
        {
            string pattern = @"\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}";
            var date = Regex.Match(message, pattern);
            if (!DateTime.TryParse(date.Value, out DateTime dateTime)) return message;
            return message.Replace(date.Value, dateTime.ToLocalTime().ToString());
        }
    }
}
