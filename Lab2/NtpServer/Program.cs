using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace NtpServer
{
    internal class Packet
    {
        public readonly byte Head = 0x1C;
        public byte Layer;
        public byte Interval;
        public byte Precisions;
        public byte[] Delay;
        public byte[] Disp;
        public byte[] Id = Encoding.ASCII.GetBytes("ZHMR");
        public byte[] TimeUpdate;
        public byte[] TimeStart;
        public byte[] TimeIncome;
        public byte[] TimeSend;

        public byte[] GetBytes() => new[] {Head, Layer, Interval, Precisions}
            .Concat(Delay).Concat(Disp).Concat(Id).Concat(TimeUpdate)
            .Concat(TimeStart).Concat(TimeIncome).Concat(TimeSend).ToArray();
    }
    
    internal static class Program
    {
        private static readonly DateTime Century = 
            new DateTime(1900, 1, 1, 0, 0, 0, DateTimeKind.Utc);
        
        private static byte[] GetTimestamp(DateTime? timestamp) {
            if (timestamp == null) { return new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }; }

            DateTime time = timestamp.Value;
            if (time.Kind == DateTimeKind.Local) { time = time.ToUniversalTime(); }

            var eraTime = Century;
            var n1 = (UInt32)(time - eraTime).TotalSeconds;
            var buffer1LE = BitConverter.GetBytes(n1);

            var n2 = (UInt32)Math.Min(Math.Max((time.Ticks - eraTime.AddSeconds(n1).Ticks) / 10_000_000.0 * 0x100000000, 0), 0x100000000);
            var buffer2LE = BitConverter.GetBytes(n2);

            return buffer1LE.Reverse().Concat(buffer2LE.Reverse()).ToArray();
        }

        private static DateTime? GetTimestamp(byte[] bytes, int offset) {
            long n1 = BitConverter.ToUInt32(bytes.Skip(offset).Take(4).Reverse().ToArray(), 0);
            long n2 = BitConverter.ToUInt32(bytes.Skip(offset + 4).Take(4).Reverse().ToArray(), 0);

            if (n1 == 0 && n2 == 0) { return null; }

            var time = Century;
            time = time.AddTicks(n1 * 10_000_000);
            time = time.AddTicks((long)(n2 / 4294967296.0 * 10_000_000));
            return time;
        }
        
        private static void Main(string[] args)
        {      
            var buffer = new byte[48];
            buffer[0] = 0x1B; //LI = 0 (no warning), VN = 3 (IPv4 only), Mode = 3 (Client Mode)
            var ipEndPoint = new IPEndPoint(Dns.GetHostEntry("pool.ntp.org").AddressList[0], 123);
            // ReSharper disable once ConvertToUsingDeclaration
            using (var socket =
                new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp) {ReceiveTimeout = 3000})
            {
                socket.Connect(ipEndPoint);
                socket.Send(buffer);
                socket.Receive(buffer);
            }
            var time = GetTimestamp(buffer, 40);
            Console.WriteLine("Server online");
            Console.WriteLine("Global time: {0}", time);
            Console.WriteLine("Server time: {0}", GetTimestamp(DateTime.UtcNow));
            Debug.Assert(time != null, nameof(time) + " != null");
            Console.WriteLine(DateTime.UtcNow.Subtract((DateTime) time).Seconds <= 1
                ? "Server time correct"
                : "Server time need to correction");
            var template = new Packet {Layer = (byte) (buffer[1] + 1), Interval = buffer[2], 
                Precisions = buffer[3], Delay = buffer.Skip(4).Take(4).ToArray(), 
                Disp = buffer.Skip(8).Take(4).ToArray(), TimeUpdate = GetTimestamp(time)};
            using (var socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp))
            {
                Console.CancelKeyPress += (sender, eventArgs) => { socket.Shutdown(SocketShutdown.Both); Environment.Exit(0); };
                socket.Bind(new IPEndPoint(IPAddress.Any, 123));
                while (true)
                {
                    var package = template;
                    EndPoint ip = new IPEndPoint(IPAddress.Any, 0);
                    var buf = new byte[49];
                    var i = socket.ReceiveFrom(buf, ref ip);
                    package.TimeIncome = GetTimestamp(DateTime.UtcNow);
                    if (i != 48) return;
                    package.TimeStart = buffer.Skip(24).Take(8).ToArray();
                    package.TimeSend = GetTimestamp(DateTime.UtcNow);
                    Console.WriteLine("{0}", ip);
                    socket.SendTo(package.GetBytes(), ip);
                }
            }
        }
    }
}