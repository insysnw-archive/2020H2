using System;
using System.Linq;
using System.Linq.Expressions;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Lab1_client
{
    internal class Program
    {
        private static bool CheckHead(byte[] buffer, int n)
        {
            return buffer[0] != 99 || buffer[1] != 130 || buffer[n - 1] != 255;
        }

        private static void Main(string[] args)
        {
            if (args.Length < 3)
            {
                Console.Error.WriteLine("Incorrect arguments number");
                Console.Error.WriteLine("[ip] [port] [nik]");
                return;
            }

            var port = Convert.ToInt32(args[1]);
            var nik = args[2];

            var tcp = new TcpClient();
            var cancel = new CancellationTokenSource();
            try
            {
                tcp.Connect(IPAddress.Parse(args[0]), port);
                var socket = tcp.GetStream();
                var tmp = new byte[] {99, 130}
                    .Concat(Encoding.Unicode.GetBytes(nik)).Append((byte) 255)
                    .ToArray();
                socket.Write(tmp, 0, tmp.Length);
                
                var incoming = Task.Factory.StartNew(ct =>
                {
                    CancellationToken c = (CancellationToken) ct;
                    while (!c.IsCancellationRequested)
                    {
                        var tmp = new byte[1024];
                        var n = socket.Read(tmp);
                        if (!CheckHead(tmp, n))
                        {
                            var size = BitConverter.ToUInt32(tmp.Skip(2).Take(4).ToArray()) / 2;
                            if (size == 0)
                            {
                                return;
                            }

                            var nsize = BitConverter.ToInt32(tmp.Skip(6).Take(4).ToArray());

                            DateTime dt = new DateTime(1970, 1, 1) +
                                         TimeSpan.FromSeconds(BitConverter.ToUInt64(tmp.Skip(10).Take(8).ToArray()));

                            var nik = Encoding.Unicode.GetString(tmp.Skip(18).Take(nsize).ToArray())
                                .Trim(Convert.ToChar(0), '\n', '�');
                            var text = new StringBuilder(Encoding.Unicode.GetString(tmp.Skip(18 + nsize).ToArray())
                                .Trim(Convert.ToChar(0x8263), Convert.ToChar(0), '\n', Convert.ToChar(0xff00)));
                            for (var i = text.Length; i < size;)
                            {
                                n = socket.Read(tmp);
                                text.Append(Encoding.Unicode.GetString(tmp.Take(n).ToArray())
                                    .Trim('\u8263', '\u0000', '\n', '\uff00', '�', 'ÿ'));
                                i += n;
                            }

                            Console.WriteLine("[{0:g}] <{1}> {2}", dt, nik, text.ToString().Trim('ÿ'));
                        }
                    }
                }, cancel.Token, cancel.Token);
                var output = Task.Factory.StartNew(ct =>
                {
                    CancellationToken c = (CancellationToken) ct;
                    while (!c.IsCancellationRequested)
                    {
                        string text;
                        do
                        {
                            text = (Console.ReadLine() ?? "").Trim();
                        } while (text.Length < 0);
                        socket.Write(new byte[] {99, 130}
                            .Concat(BitConverter.GetBytes(text.Length * 2))
                            .Concat(BitConverter.GetBytes((ulong) DateTimeOffset.Now.ToUnixTimeSeconds()))
                            .Append((byte) 0x00).Append((byte) 0xff)
                            .ToArray());
                        socket.Write(new byte[] {99, 130}
                            .Concat(Encoding.Unicode.GetBytes(text))
                            .Append((byte) 0x00).Append((byte) 0xff).ToArray());
                    }
                }, cancel.Token, cancel.Token);
                Console.CancelKeyPress += (sender, eventArgs) =>
                {
                    Console.WriteLine("Shutting down");
                    try
                    {
                        cancel.Cancel();
                        incoming.Wait(cancel.Token);
                    }
                    catch (OperationCanceledException)
                    {
                        // output.Dispose();
                        // incoming.Dispose();
                        socket.Close();
                        tcp.Close();
                    }
                };
                output.Wait(cancel.Token);
                incoming.Wait(cancel.Token);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exception {0}", ex);
            }
        }
    }
}