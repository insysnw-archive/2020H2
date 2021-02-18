using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace Lab2
{
    internal static class Program
    {
        private static readonly List<Socket> Pull = new List<Socket>();
        private static List<string> Niks = new List<string>();
        
        private readonly struct MsgHead
        {
            public readonly uint Size;
            public readonly DateTime Time;

            public MsgHead(byte[] packet)
            {
                Size = BitConverter.ToUInt32(packet.Skip(2).Take(4).ToArray()) / 2;
                Time = new DateTime(1970, 1, 1) +
                        TimeSpan.FromSeconds(BitConverter.ToUInt64(packet.Skip(6).Take(8).ToArray()));
            }

            public byte[] GetHead(string nik) => new byte[] {99, 130}
                .Concat(BitConverter.GetBytes(Size * 2))
                .Concat(BitConverter.GetBytes((ulong) ((DateTimeOffset) Time).ToUnixTimeSeconds()))
                .Concat(Encoding.Unicode.GetBytes(nik))
                .Append((byte) 0xff).ToArray();
        }

        private static byte[] PackCode(int code) => new byte[] {99, 130, (byte) code, 0xff};
        private static byte[] PackMsg(string msg) => new byte[] {99, 130}
            .Concat(Encoding.Unicode.GetBytes(msg)).Append((byte) 0xff).ToArray();
        private static string UnpackMsg(IEnumerable<byte> packet, int size) =>
            Encoding.Unicode.GetString(packet.Skip(2).Take(size - 3).ToArray())
                .Trim('\u8263', '\u0000', '\n', '\uff00', '�', 'ÿ');
        private static bool IsValidPacket(IReadOnlyList<byte> packet, int size) => 
            packet[0] == 99 && packet[1] == 130 && packet[size - 1] == 0xff;

        private static void Handle(Socket socket, CancellationToken token)
        {
            var data = new byte[4096];
            var nik = string.Empty;
            do
            {
                if (!socket.Poll(1000, SelectMode.SelectRead)) continue;
                var size = socket.Receive(data);
                if (size > 4003 || !IsValidPacket(data, size))
                    socket.Send(PackCode(1));
                else
                {
                    if (data.Length == 4)
                    {
                        Pull.Remove(socket);
                        return;
                    }
                    nik = UnpackMsg(data, size);
                    if (Niks.Contains(nik))
                    {
                        socket.Send(PackCode(2));
                        nik = string.Empty;
                    }
                    else
                    {
                        Niks.Add(nik);
                    }
                }
            } while (nik == string.Empty && !token.IsCancellationRequested);
            Console.WriteLine("New client: {0}", nik);
            socket.Send(PackMsg("Hello " + nik + " on our server!"));
            socket.ReceiveTimeout = 600_000;
            while (!token.IsCancellationRequested)
            {
                var size = 0;
                try
                {
                    data = new byte[15];
                    while (!socket.Poll(1000, SelectMode.SelectRead)) {}
                    size = socket.Receive(data);
                }
                catch (SocketException)
                {
                    Console.WriteLine("Client {0} disconnected by timeout", nik);
                    socket.Send(PackCode(0));
                    Niks.Remove(nik);
                    Pull.Remove(socket);
                    socket.Close();
                    return;
                }

                if (!IsValidPacket(data, size) || size == 3) continue;
                if (size == 4)
                {
                    Console.WriteLine("Client {0} live us", nik);
                    Niks.Remove(nik);
                    Pull.Remove(socket);
                    socket.Close();
                    return;
                }

                var head = new MsgHead(data);
                var msg = new StringBuilder();

                data = new byte[4096];
                while (!socket.Poll(1000, SelectMode.SelectRead)) {}
                size = socket.Receive(data);
                if (size == 4096 && head.Size > 4093)
                {
                    msg.Append(Encoding.Unicode.GetString(data.Skip(2).ToArray()));
                    while (msg.Length < head.Size)
                    {
                        if (!socket.Poll(1000, SelectMode.SelectRead)) continue;
                        size = socket.Receive(data);
                        msg.Append(size < 4096
                            ? Encoding.Unicode.GetString(data)
                            : Encoding.Unicode.GetString(data.Take(size - 1).ToArray()));
                    }
                }
                else
                    msg.Append(UnpackMsg(data, size));

                if (msg.Length != head.Size) continue;
                Console.WriteLine("[{0:g}] <{1}> {2}", head.Time, nik, msg);
                
                Parallel.ForEach(Pull, socket1 =>
                {
                    if (socket1 == socket) return;
                    socket1.Send(head.GetHead(nik));
                    socket1.Send(PackMsg(msg.ToString()));
                });
            }

            Pull.Remove(socket);
        }

        private static void Main(string[] args)
        {
            if (args.Length < 1)
            {
                Console.Error.WriteLine("No port specified");
                return;
            }

            if (!int.TryParse(args[0], out _))
            {
                Console.Error.WriteLine("An invalid port number was specified.");
                return;
            }

            var adr = IPAddress.Any;
            if (args.Length >= 2)
                if (Regex.IsMatch(args[1], @"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"))
                    try
                    {
                        adr = IPAddress.Parse(args[1]);
                    }
                    catch (FormatException)
                    {
                        Console.Error.WriteLine("An invalid IP address was specified");
                        return;
                    }
                else
                    adr = Dns.GetHostEntry(args[1]).AddressList[0];

            var ip = new IPEndPoint(adr, Convert.ToInt32(args[0]));
            var listener =
                new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp) {Blocking = false};
            var cancel = new CancellationTokenSource();

            Console.CancelKeyPress += (sender, eventArgs) => { cancel.Cancel(); Environment.Exit(0); };
            listener.Bind(ip);
            listener.Listen(10);
            Console.WriteLine("Server started...");
            while (true)
            {
                if (!listener.Poll(100, SelectMode.SelectRead)) continue;
                var nsocket = listener.Accept();
                Pull.Add(nsocket);
                Task.Factory.StartNew(delegate { Handle(nsocket, cancel.Token); }, cancel.Token);
            }
        }
    }
}