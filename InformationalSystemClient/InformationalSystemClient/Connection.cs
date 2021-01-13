using System;
using System.Collections.Generic;
using System.Threading;
using System.Text;
using System.Threading.Tasks;
using System.Net;
using System.Net.Sockets;

namespace InformationalSystemClient
{
    static class Connection
    {

        public static Socket socket;
        public static event Action<string> OnReceive;

        // адрес и порт сервера, к которому будем подключаться
        static int port = 1490; // порт сервера
        static string address = "185.86.125.90"; // адрес сервера

        private static bool isReceiving = true;

        public static void Connect()
        {
            try
            {
                IPEndPoint ipPoint = new IPEndPoint(IPAddress.Parse(address), port);

                socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                // подключаемся к удаленному хосту
                socket.Connect(ipPoint);
                Console.WriteLine("Connected");
                StartReciving();

            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }
            //Console.Read();
        }

        public static void Send(byte[] message)
        {
            // Console.Write("Введите сообщение:");
            //  string message = Console.ReadLine();
            
            //Console.WriteLine(message);
            byte[] b = new byte[2];
            b[0] = message[1];
            b[1] = message[2];
            //Console.WriteLine(BitConverter.ToInt16(b,0));
            //Console.WriteLine("0: " + message[0]+ " 1: " + message[1]+ " 2: " + message[2]);
            //Console.WriteLine("++++++++++++++++++++++++++");
            for (int i = 0; i < message.Length; i++)
            {
                //Console.Write(message[i]);
            }
            //Console.WriteLine("++++++++++++++++++++++++++");
            //Console.WriteLine("|"+Encoding.UTF8.GetString(message,0,message.Length));
                socket.Send(message);
           // Console.WriteLine("sent bytes: " + data.Length);
        }

        private static void Receive()
        {
            // получаем ответ
            while (isReceiving)
            {
                Thread.Sleep(100);
                byte[] responceCodeByte = new byte[2];
                byte[] jsonSizeByte = new byte[2];

                socket.Receive(responceCodeByte,2,0);
                socket.Receive(jsonSizeByte, 2, 0);

                var responceCode = BitConverter.ToInt16(responceCodeByte, 0);
                MsgUnpacker.requestCode = responceCode;
                var jsonSize = BitConverter.ToInt16(jsonSizeByte, 0);
                byte[] data = new byte[jsonSize];
                //Console.WriteLine(responceCode + " " + jsonSize);

                if (jsonSize == 0)
                {
                    OnReceive?.Invoke("");
                    continue;
                }

                socket.Receive(data, data.Length, 0);

                StringBuilder builder = new StringBuilder();
                builder.Append(Encoding.UTF8.GetString(data, 0, jsonSize));

                //int bytes = 0; // количество полученных байт

                //do
                //{
                //Console.WriteLine("Before");
                //bytes = socket.Receive(data, data.Length, 0);
                //Console.WriteLine(data);
                //Console.WriteLine("length: " + data.Length + " avaailable "+socket.Available +" what "+ builder.ToString());
                //builder.Append(Encoding.UTF8.GetString(data, 0, bytes));
                //}
                //while (socket.Available > 0);

                for (int i = 0; i < data.Length; i++)
                {
                    //Console.Write(data[i]);
                }

                //Console.WriteLine("ответ сервера: " + builder.ToString());
                OnReceive?.Invoke(builder.ToString());
            }
        }

        public static void StartReciving()
        {
            Thread conversationThread = new Thread(() => Receive());
            conversationThread.Start();

        }

        public static void Disconnect()
        {
            // закрываем сокет
            isReceiving = false;
            socket.Shutdown(SocketShutdown.Both);
            socket.Close();
        }
    }
}
