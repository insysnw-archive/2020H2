using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
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
                string message = GetMessage(socket);
                if (!clients.ContainsKey(socket)) break;
                BroadcastMessage(socket, $"[{clients[socket]}]: {message}");
            }
        }

        public void BroadcastMessage(Socket fromSocket, string message)
        {
            foreach (var client in clients.Where(cl => cl.Key != fromSocket))
                SendMessage(client.Key, message);
        }

        public string GetMessage(Socket socket)
        {
            StringBuilder builder = new StringBuilder();
            byte[] data = new byte[8];
            try
            {
                byte[] bytesSize = new byte[4];
                int bytes = socket.Receive(bytesSize);
                int size = BitConverter.ToInt32(bytesSize);
                do
                {
                    bytes = socket.Receive(data);
                    builder.Append(Encoding.Unicode.GetString(data, 0, bytes));
                }
                while (socket.Available > 0 && socket.Available < size);
            }
            catch (Exception)
            {
                BroadcastMessage(socket, $"{clients[socket]} left TcpChat");
                clients.Remove(socket);
            }
            return builder.ToString();
        }
        public void SendMessage(Socket socket, string message)
        {
            byte[] bytesMessage = Encoding.Unicode.GetBytes(message);
            byte[] bytesSize = BitConverter.GetBytes(bytesMessage.Length);
            byte[] bytesDate = BitConverter.GetBytes(DateTimeOffset.Now.ToUnixTimeSeconds());
            byte[] packet = new byte[bytesMessage.Length + bytesSize.Length + bytesDate.Length];
            bytesSize.CopyTo(packet, 0);
            bytesDate.CopyTo(packet, bytesSize.Length);
            bytesMessage.CopyTo(packet, bytesSize.Length + bytesDate.Length);
            socket.Send(packet);
        }

        public void AddClient(Socket socket)
        {
            string username = GetMessage(socket);
            while (clients.ContainsValue(username))
            {
                SendMessage(socket, "This username is already taken. Please enter another username");
                username = GetMessage(socket);
            }
            clients.Add(socket, username);
            SendMessage(socket, "Welcome to TcpChat!");
            BroadcastMessage(socket, $"{clients[socket]} just entered TcpChat!");
            GetMessages(socket);
        }
    }
}
