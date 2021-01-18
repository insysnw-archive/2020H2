using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;

//  E:\Education\7Sem\Nets\Labs2\NTP_Client.py

namespace NtpServer
{
    class Program
    {
        private static Socket _socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
        private static Socket _serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
        private static EndPoint epFrom = new IPEndPoint(IPAddress.Any, 0);

        public static void Main(string[] arg)
        {
            Console.WriteLine("Hi. It is NTP Server");
            Thread conservationThread = new Thread(() => Server());
            conservationThread.Start();
            
            Console.ReadKey();
            
        }

        public static void Server()
        {
            _serverSocket.SetSocketOption(SocketOptionLevel.IP, SocketOptionName.ReuseAddress, true);
            _serverSocket.Bind(new IPEndPoint(IPAddress.Parse("127.0.0.1"), 123));

            var ntpData = new byte[48];
            NtpPacket n = new NtpPacket(3, Mode.Server);

            while (true)
            {
                _serverSocket.ReceiveFrom(ntpData, ref epFrom);
                long milliseconds = (long)(DateTime.Now - new DateTime(1900, 1, 1)).TotalMilliseconds;
                n.receive = ConvertToNtp(milliseconds);
                Pack(n, ntpData);
                _serverSocket.SendTo(ntpData, epFrom);
                
            }
        }

        public static void Pack(NtpPacket ntp,byte[] ntpData)
        {
            ntpData[0] = (byte)((byte)ntp.mode | ntp.versionNumber << 3 | (byte) ntp.leapIndicator << 6); 

            long milliseconds = (long)(DateTime.Now - new DateTime(1900, 1, 1)).TotalMilliseconds;
            ntp.transmit = ConvertToNtp(milliseconds);

            for (int i = 0; i <8; i++)
            {
                ntpData[i + 24] = ntpData[i+40]; //ordinate
                ntpData[i+32] = ntp.receive[i]; // receive
                ntpData[i+40] = ntp.transmit[i]; //transmit
            }

            
        }

        public static byte[] ConvertToNtp(long milliseconds)
        {
            var bytes = new byte[8];
            
            decimal intpart = 0, fractpart = 0;
            
            intpart = milliseconds / 1000;
            fractpart = ((milliseconds % 1000) * 0x100000000L) / 1000m;
            
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
        
    }



    class NtpPacket
    {
        // Necessary of enter leap second (2 bits)
        public LeapIndicator leapIndicator = LeapIndicator.NoCorrection;
        // Version of protocol (3 bits)
        public byte versionNumber = 3;
        // Mode of sender (3 bits)
        public Mode mode = Mode.Server;
        // The level of "layering" reading time (1 byte)
        public byte stratum = 0;
        // Interval between requests (1 byte)
        public byte pool = 0;
        // Precision (log2) (1 byte)
        public byte precision = 0;
        // Interval for the clock reach NTP server (4 bytes)
        public int rootDelay = 0;
        // Scatter the clock NTP-server (4 bytes)
        public int rootDispersion = 0;
        // Indicator of clocks (4 bytes)
        public int refId = 0;
        // Last update time on server (8 bytes)
        public byte[] reference = new byte[8];
        // Time of sending packet from local machine (8 bytes)
        public byte[] originate = new byte[8];
        // Time of receipt on server (8 bytes)
        public byte[] receive = new byte[8];
        // Time of sending answer from server (8 bytes)
        public byte[] transmit = new byte[8];


        public NtpPacket(byte versionNum, Mode mode)
        {
            versionNumber = versionNum;
            this.mode = mode;
        }
        
    }

    enum Mode {Reserved, SymActive,SymPassive , Client, Server, Broadcast , ControlMess,PrivateUsage}
    enum LeapIndicator { NoCorrection,SecondsLastMin61, SecondsLastMin59,ServerError }
}
