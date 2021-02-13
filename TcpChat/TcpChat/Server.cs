using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;

namespace TcpChat
{
    class Server
    {
        readonly string ip = "127.0.0.1";
        readonly int port = 1234;
        readonly Dictionary<Socket, string> clients;
        readonly Socket listenSocket;
        public Server()
        {
            clients = new Dictionary<Socket, string>();
            IPEndPoint ipPoint = new IPEndPoint(IPAddress.Parse(ip), port);
            listenSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);

            try
            {
                listenSocket.Bind(ipPoint);
                listenSocket.Listen(10);
                Console.WriteLine("The server is running. Waiting for connections...");

                while (true)
                {
                    Socket handler = listenSocket.Accept();
                    if (!clients.ContainsKey(handler))
                    {
                        var thread = new Thread(() => AddClient(handler));
                        try
                        {
                            thread.Start();
                        }
                        catch (Exception)
                        {
                            thread.Abort();
                            handler.Shutdown(SocketShutdown.Both);
                            handler.Close();
                        }
                    }
                    Console.WriteLine(handler.RemoteEndPoint);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
        }

        public void GetMessages(Socket socket)
        {
            while (true)
            {
                string message = GetMessage(socket, true);
                if (!clients.ContainsKey(socket)) break;
                BroadcastMessage(socket, $"{message}");
            }
        }

        public string MessageWithDate(string message, Socket fromSocket)
        {
            string pattern = @"\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}";
            var date = Regex.Match(message, pattern);
            return message.Replace(date.Value, $"<{date.Value}> [{clients[fromSocket]}]: ");
        }

        public string MessageWithoutDate(string message)
        {
            string pattern = @"\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}";
            var date = Regex.Match(message, pattern);
            return message.Replace(date.Value, "");
        }

        public void BroadcastMessage(Socket fromSocket, string message)
        {
            foreach (var client in clients.Where(cl => cl.Key != fromSocket))
                SendMessage(client.Key, message);
        }

        public string GetMessage(Socket socket, bool date)
        {
            StringBuilder builder = new StringBuilder();
            byte[] data = new byte[256];
            try
            {
                do
                {
                    int bytes = socket.Receive(data);
                    builder.Append(Encoding.Unicode.GetString(data, 0, bytes));
                }
                while (socket.Available > 0);
            }
            catch (Exception)
            {
                BroadcastMessage(socket, $"{clients[socket]} left TcpChat");
                clients.Remove(socket);
            }
            if (date) return MessageWithDate(builder.ToString(), socket);
            else return MessageWithoutDate(builder.ToString());
        }
        public void SendMessage(Socket socket, string message) => socket.Send(Encoding.Unicode.GetBytes(message));

        public void AddClient(Socket socket)
        {
            SendMessage(socket, "Enter your username: ");
            string username = GetMessage(socket, false);
            while (clients.ContainsValue(username))
            {
                SendMessage(socket, "This username is already taken. Please enter another username");
                username = GetMessage(socket, false);
            }
            clients.Add(socket, username);
            SendMessage(socket, "Welcome to TcpChat!");
            BroadcastMessage(socket, $"{clients[socket]} just entered TcpChat!");
            GetMessages(socket);
        }
    }
}
