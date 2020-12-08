using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Lab1
{
    internal class Program
    {
        private static readonly List<Socket> Pull = new List<Socket>();
        private static readonly List<Task> Tasks = new List<Task>();

        private static bool CheckHead(byte[] buffer, int n)
        {
            return buffer[0] != 99 || buffer[1] != 130 || buffer[n - 1] != 255;
        }

        private static void Handle(Socket socket, CancellationToken token)
        {
            try
            {
                var buffer = new byte[1024];
                var n = socket.Receive(buffer);
                if (CheckHead(buffer, n))
                {
                    Console.WriteLine("Client not send handshake packet");
                    socket.Close();
                    return;
                }

                var nik = Encoding.Unicode.GetString(buffer.Skip(2).Take(n - 3).ToArray()).Trim();
                Console.WriteLine("New client: " + nik);
                string msg = "Hello! Glad to see you " + nik + " on our server";

                socket.Send(new byte[] {99, 130}
                    .Concat(BitConverter.GetBytes(msg.Length * 2))
                    .Concat(BitConverter.GetBytes("Server".Length * 2))
                    .Concat(BitConverter.GetBytes((ulong) DateTimeOffset.Now.ToUnixTimeSeconds()))
                    .Concat(Encoding.Unicode.GetBytes("Server"))
                    .Append((byte) 0x00).Append((byte) 0xff)
                    .ToArray());

                socket.Send(new byte[] {99, 130}
                    .Concat(Encoding.Unicode.GetBytes(msg))
                    .Append((byte) 0x00).Append((byte) 0xff)
                    .ToArray());
                var disc = 0;
                while (!token.IsCancellationRequested)
                {
                    var text = new StringBuilder();
                    n = socket.Receive(buffer);
                    if (n != 15 || CheckHead(buffer, n))
                    {
                        disc++;
                        if (disc == 3)
                        {
                            Console.WriteLine("Client not send head packet");
                            Pull.Remove(socket);
                            return;
                        }

                        continue;
                    }

                    var size = BitConverter.ToUInt32(buffer.Skip(2).Take(4).ToArray());
                    var dt = new DateTime(1970, 1, 1) +
                             TimeSpan.FromSeconds(BitConverter.ToUInt64(buffer.Skip(6).Take(8).ToArray()));
                    for (var i = 0; i < size;)
                    {
                        n = socket.Receive(buffer);
                        text.Append(Encoding.Unicode.GetString(buffer, 0, n)
                            .Trim('\u8263', '\u0000', '\n', '\uff00', '�'));
                        i += n;
                    }

                    var res = text.ToString();
                    Console.WriteLine("[{0:g}] <{1}> {2}", dt, nik, text);
                    var t = (ulong) DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
                    var head = new List<byte> {99, 130}
                        .Concat(BitConverter.GetBytes(res.Length * 2))
                        .Concat(BitConverter.GetBytes(nik.Length * 2))
                        .Concat(BitConverter.GetBytes((ulong) DateTimeOffset.Now.ToUnixTimeSeconds()))
                        .Concat(Encoding.Unicode.GetBytes(nik)).Append((byte) 255).ToArray();
                    foreach (var s in Pull)
                        if (!s.Equals(socket))
                        {
                            s.Send(head);
                            s.Send(Encoding.Unicode.GetBytes(text.ToString())
                                .Append((byte)00).Append((byte) 255).ToArray());
                        }

                    disc = 0;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exception {}", ex);
            }
        }

        private static void Main(string[] args)
        {
            if (args.Length != 1)
            {
                Console.Error.WriteLine("No port specified");
                return;
            }

            var ip = new IPEndPoint(IPAddress.Any, Convert.ToInt32(args[0]));
            var socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            var cancel = new CancellationTokenSource();
            Console.CancelKeyPress += (sender, eventArgs) =>
            {
                Console.WriteLine("Shutting down");
                try
                {
                    cancel.Cancel();
                }
                catch (AggregateException)
                {
                    Pull.ForEach(p =>
                    {
                        p.Send(new byte[] {99, 130}
                            .Concat(BitConverter.GetBytes(0))
                            .Append((byte) 0xff)
                            .ToArray());
                        p.Close();
                    });
                    Tasks.ForEach(t => t.Dispose());
                    socket.Close();
                }
            };
            try
            {
                socket.Bind(ip);
                socket.Listen(10);
                Console.WriteLine("Server started...");

                while (true)
                {
                    var tmp = socket.Accept();
                    Pull.Add(tmp);
                    Tasks.Add(Task.Factory.StartNew(() => Handle(tmp, cancel.Token), cancel.Token));
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }
    }
}