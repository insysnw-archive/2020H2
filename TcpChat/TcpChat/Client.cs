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

                Console.WriteLine("Enter your username: ");
                SendMessage();

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
            byte[] data = Encoding.Unicode.GetBytes($"{Console.ReadLine()}");
            byte[] dataSize = BitConverter.GetBytes(data.Length);
            byte[] packet = new byte[data.Length + dataSize.Length];
            dataSize.CopyTo(packet, 0);
            data.CopyTo(packet, dataSize.Length);
            socket.Send(packet);
        }

        public void GetMessage()
        {
            StringBuilder builder = new StringBuilder();
            byte[] data = new byte[8];

            byte[] bytesSize = new byte[4];
            int bytes = socket.Receive(bytesSize);
            int size = BitConverter.ToInt32(bytesSize);

            byte[] bytesDate = new byte[8];
            socket.Receive(bytesDate);
            var date = DateTimeOffset.FromUnixTimeSeconds(BitConverter.ToInt64(bytesDate)).LocalDateTime;

            do
            {
                bytes = socket.Receive(data);
                builder.Append(Encoding.Unicode.GetString(data, 0, bytes));
            }
            while (socket.Available > 0 && socket.Available < size);

            Console.WriteLine($"<{date}> {builder}");
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
