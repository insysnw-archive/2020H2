using System;
using System.Net;
using System.Net.Sockets;

namespace NtpServer
{
    public class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Welcome to NTP Server");
            new Server();
        }
    }

    internal class Server
    {
        private byte[] referenceBytes = new byte[8];
        public Server()
        {
            referenceBytes = NtpFormat((long)(DateTime.Now - new DateTime(1900, 1, 1)).TotalMilliseconds);
            EndPoint fromAddress = new IPEndPoint(IPAddress.Any, 0);
            Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            socket.SetSocketOption(SocketOptionLevel.IP, SocketOptionName.ReuseAddress, true);

            socket.Bind(new IPEndPoint(IPAddress.Parse("127.0.0.1"), 1233));

            var data = new byte[48];

            while (true)
            {
                socket.ReceiveFrom(data, ref fromAddress);
                long receiveTime = (long)(DateTime.Now - new DateTime(1900, 1, 1)).TotalMilliseconds;
                var packet = MakeNtpPacket(data, receiveTime);
                socket.SendTo(packet, fromAddress);
            }
        }

        public byte[] MakeNtpPacket(byte[] receivedPacket, long receiveTime)
        {
            var packet = new byte[48];

            packet[0] = (byte)LI.NoCorrection | (byte)VN.V3 | (byte)Mode.Server;

            var receiveBytes = NtpFormat(receiveTime);
            var transmitBytes = NtpFormat((long)(DateTime.Now - new DateTime(1900, 1, 1)).TotalMilliseconds);

            for (int i = 0; i < 8; i++)
            {
                packet[16 + i] = referenceBytes[i];
                packet[24 + i] = receivedPacket[40 + i]; //origin
                packet[32 + i] = receiveBytes[i];
                packet[40 + i] = transmitBytes[i];
            }

            return packet;
        }

        public byte[] NtpFormat(long milliseconds)
        {
            var bytes = new byte[8];

            decimal intpart = milliseconds / 1000;
            decimal fractpart = ((milliseconds % 1000) * 0x100000000L) / 1000m;

            var temp = intpart;
            for (var i = 3; i >= 0; i--)
            {
                bytes[i] = (byte)(temp % 256);
                temp = temp / 256;
            }

            temp = fractpart;
            for (var i = 7; i >= 4; i--)
            {
                bytes[i] = (byte)(temp % 256);
                temp = temp / 256;
            }
            return bytes;
        }

        internal enum LI : byte
        {
            NoCorrection = 0b_0000_0000,
            SecondsLastMin61 = 0b_0100_0000,
            SecondsLastMin59 = 0b_1000_0000,
            ServerError = 0b_1100_0000
        }

        internal enum VN : byte
        {
            V1 = 0b_0000_1000,
            V2 = 0b_0001_0000,
            V3 = 0b_0001_1000,
            V4 = 0b_0010_0000
        }

        internal enum Mode : byte
        {
            Reserved = 0b_0000_0000,
            SymActive = 0b_0000_0001,
            SymPassive = 0b_0000_0010,
            Client = 0b_0000_0011,
            Server = 0b_0000_0100,
            Broadcast = 0b_0000_0101,
            ControlMess = 0b_0000_0110,
            PrivateUsage = 0b_0000_0111
        }
    }
}
