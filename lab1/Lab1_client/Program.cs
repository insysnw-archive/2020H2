using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace Lab1_client
{
    internal static class Program
    {
        private struct MsgHead
        {
            public uint Size;
            public DateTime Time;
            public string Nik;
        }

        private static MsgHead UnpackMsgHead(IReadOnlyCollection<byte> packet) => new MsgHead
            {
                Size = BitConverter.ToUInt32(packet.Skip(2).Take(4).ToArray()) / 2,
                Time = new DateTime(1970, 1, 1) +
                       TimeSpan.FromSeconds(BitConverter.ToUInt64(packet.Skip(6).Take(8).ToArray())),
                Nik = packet.Count != 15
                    ? Encoding.Unicode.GetString(packet.Skip(14).SkipLast(1).ToArray())
                        .Trim('\u8263', '\u0000', '\n', '\uff00', '�', 'ÿ')
                    : string.Empty
            };

        private static byte[] PackMsgHead(MsgHead head) => new byte[] {99, 130}
            .Concat(BitConverter.GetBytes(head.Size * 2))
            .Concat(BitConverter.GetBytes((ulong) ((DateTimeOffset) head.Time).ToUnixTimeSeconds()))
            .Append((byte) 0xff).ToArray();

        private static byte[] PackCode(int code) => new byte[] {99, 130, (byte) code, 0xff};
        private static int UnpackCode(byte[] packet) => packet[2];

        private static byte[] PackMsg(string msg) => new byte[] {99, 130}
            .Concat(Encoding.Unicode.GetBytes(msg)).Append((byte) 0xff).ToArray();

        private static string UnpackMsg(IEnumerable<byte> packet, int i) =>
            Encoding.Unicode.GetString(packet.Skip(2).Take(i - 3).ToArray())
                .Trim('\u8263', '\u0000', '\n', '\uff00', '�', 'ÿ');

        private static bool IsValidPacket(IReadOnlyList<byte> packet, int i) =>
            packet[0] == 99 && packet[1] == 130 && packet[i-1] == 0xff;

        private static void Main(string[] args)
        {
            if (args.Length < 3)
            {
                Console.Error.WriteLine("Incorrect arguments number");
                Console.Error.WriteLine("[ip] [port] [nik]");
                return;
            }

            IPAddress adr;
            if (Regex.IsMatch(args[0], @"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"))
            {
                try
                {
                    adr = IPAddress.Parse(args[0]);
                }
                catch (FormatException)
                {
                    Console.Error.WriteLine("An invalid IP address was specified");
                    return;
                }
            }
            else
            {
                adr = Dns.GetHostEntry(args[0]).AddressList[0];
            }

            var port = Convert.ToInt32(args[1]);
            var nik = args[2];
            if (nik.Length > 2000)
            {
                Console.Error.WriteLine("An invalid nik was specified");
                return;
            }

            var tcp = new TcpClient();
            var cancel = new CancellationTokenSource();
            NetworkStream socket;
            try
            {
                tcp.Connect(adr, port);
                socket = tcp.GetStream();
                socket.Write(PackMsg(nik));
                byte[] buf;
                int size;
                while (true)
                {
                    buf = new byte[4096];
                    size = socket.Read(buf);
                    if (size == 4)
                    {
                        switch (UnpackCode(buf))
                        {
                            case 1:
                                Console.WriteLine("Incorrect nik");
                                break;
                            case 2:
                                Console.WriteLine("Nik already existed");
                                break;
                        }
                        Console.Write("Choose another nickname\n>");
                        nik = Console.ReadLine();
                        socket.Write(PackMsg(nik));
                    }
                    else break;
                }
                Console.WriteLine("Server`s welcome message: {0}", UnpackMsg(buf, size));
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("Exception {0}", ex);
                return;
            }

            void IncomingTask(object? ct)
            {
                Debug.Assert(ct != null, nameof(ct) + " != null");
                var c = (CancellationToken) ct;
                while (!c.IsCancellationRequested)
                {
                    var tmp = new byte[4096];
                    var n = socket.Read(tmp);
                    switch (n)
                    {
                        case 4:
                            Console.WriteLine("Server disconnect us T.T");
                            return;
                        case 0:
                            Console.WriteLine("Connection error");
                            return;
                    }

                    if (!IsValidPacket(tmp, n))
                    {
                        Console.WriteLine("incorrect head message");
                        continue;
                    }

                    var head = UnpackMsgHead(tmp);
                    n = socket.Read(tmp);

                    var msg = new StringBuilder();
                    if (n == 4096 && head.Size > 4093)
                    {
                        msg.Append(Encoding.Unicode.GetString(tmp.Skip(2).ToArray()));
                        while (msg.Length < head.Size)
                        {
                            n = socket.Read(tmp);
                            msg.Append(n < 4096
                                ? Encoding.Unicode.GetString(tmp)
                                : Encoding.Unicode.GetString(tmp.Take(n - 1).ToArray()));
                        }
                    }
                    else
                        msg.Append(UnpackMsg(tmp, n));

                    Console.Write("\n-> [{0:g}] <{1}> {2}\n>", head.Time, head.Nik, msg);
                }
            }
            
            void OutcomingTask(object? ct)
            {
                Debug.Assert(ct != null, nameof(ct) + " != null");
                var c = (CancellationToken) ct;
                while (!c.IsCancellationRequested)
                {
                    Console.Write(">");
                    try
                    {
                        string text;
                        do
                        {
                            text = (Console.ReadLine() ?? "").Trim();
                        } while (text.Length <= 0);

                        socket.Write(PackMsgHead(new MsgHead {Size = (uint) text.Length, Time = DateTime.Now}));
                        socket.Write(PackMsg(text));
                    }
                    catch (Exception)
                    {
                        // ignored
                    }
                }
            }


            var incoming = Task.Factory.StartNew(IncomingTask, cancel.Token, cancel.Token);
            Task.Factory.StartNew(OutcomingTask, cancel.Token, cancel.Token);

            void ExitEvent(object sender, ConsoleCancelEventArgs eventArgs)
            {
                Console.WriteLine("Shutting down");
                cancel.Cancel();
                socket.Write(PackCode(0));
                socket.Close();
                tcp.Close();
                Environment.Exit(0);
            }

            Console.CancelKeyPress += ExitEvent;
            incoming.Wait(cancel.Token);
            ExitEvent(null, null);
        }

    }
}