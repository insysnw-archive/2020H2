using System;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Text.Json;
 
namespace NotificationSystemServer
{
    public static class Connection
    {
        static int port = 1999; // порт для приема входящих запросов
        //static IPHostEntry ipHost = Dns.GetHostEntry("localhost");
        //static IPAddress ipAddr = ipHost.AddressList[0];
        //static IPEndPoint ipPoint = new IPEndPoint(IPAddress.Parse("127.0.0.1"), port);
        static Socket listener;

        public static void StartServer()
        {
            listener = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            try
            {
                 IPEndPoint ipPoint = new IPEndPoint(IPAddress.Any, port);
                
                // связываем сокет с локальной точкой, по которой будем принимать данные
                listener.Bind(ipPoint);

                // начинаем прослушивание
                listener.Listen(1000);

                Console.WriteLine("Сервер запущен. Ожидание подключений...");

                while (true)
                {
                    Socket handler = listener.Accept();
                    // получаем сообщение

                    var u = new User();
                    u.token = "";
                    u.socket = handler;
                    Data.users.Add(u);
                    Console.WriteLine("Client connected " + handler.ToString());
                    StartReceiving(u);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                Console.ReadLine();
            }
        }

        private static void StartReceiving(User user)
        {
            Thread conservationThread = new Thread(() => Receive(user));
            conservationThread.Start();
        }

        private static void Receive(User user)
        {
            try
            {
                var handler = user.socket;

                while (true)
                {
                    Thread.Sleep(100);
                    byte[] requestCodeByte = new byte[1];
                    byte[] tokenByte = new byte[32];
                    byte[] jsonSizeByte = new byte[2];

                    handler.Receive(requestCodeByte, 1, 0);
                    handler.Receive(tokenByte, 32, 0);
                    handler.Receive(jsonSizeByte, 2, 0);

                    var requestCode = requestCodeByte[0];
                    //var token = Encoding.UTF8.GetString(tokenByte, 0, 32);
                    //var token = Convert.ToBase64String(Encoding.UTF8.GetBytes(user.token));
                    var token = Convert.ToBase64String(tokenByte);
                    
                    var jsonSize = BitConverter.ToInt16(jsonSizeByte, 0);
                    byte[] data = new byte[jsonSize];
                    
                    if(jsonSize > 0)
                        handler.Receive(data, data.Length, 0);

                    StringBuilder builder = new StringBuilder();
                    builder.Append(Encoding.UTF8.GetString(data, 0, jsonSize));
                  
                    Console.WriteLine("Received : " + builder.ToString());
                    Request.ProcessRequest(user, requestCode,token, builder.ToString());
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                Console.ReadLine();
            }
            

        }

        public static void Send(Socket socket,int code,string json)
        {
            try
            {
                byte[] data = Encoding.UTF8.GetBytes(json);
                byte[] codeBytes = BitConverter.GetBytes(Convert.ToInt16(code));
                byte[] bodyLenght = BitConverter.GetBytes(Convert.ToInt16(data.Length));
                byte[] res = new byte[data.Length + 4];
                res[0] = codeBytes[0];
                res[1] = codeBytes[1];
                res[2] = bodyLenght[0];
                res[3] = bodyLenght[1];
                Console.WriteLine("Send joson " + json);
                for (int i = 0; i < data.Length; i++)
                {
                    res[4 + i] = data[i];
                }

                socket.Send(res);
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
                Console.ReadLine();
            }
        }

        public static void DisconnectUser(User user)
        {
            user.socket.Shutdown(SocketShutdown.Both);
            user.socket.Close();
        }
    }
}