using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace SnmpClient
{
    internal static class Program
    {
        public static void Main(string[] args)
        {
            Console.Write("Manager ip> ");
            var ip = Console.ReadLine();
            Debug.Assert(ip != null, nameof(ip) + " != null");
            if (ip.Trim().Length == 0)
            {
                Console.WriteLine("Wrong ip");
                return;
            }
            var sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
                ProtocolType.Udp);
            sock.SetSocketOption(SocketOptionLevel.Socket,
                SocketOptionName.ReceiveTimeout, 5000);
            
            var iep = new IPEndPoint(IPAddress.Parse(ip!), 161);
            var ep = (EndPoint)iep;
            uint rid = 0;
            Console.Write("Your community tag> ");
            var com = Console.ReadLine()?.Trim();
            while (true)
            {
                Console.Write(">");
                string oib;
                int offset;
                byte[] packet;
                switch (Console.ReadLine()?.Trim())
                {
                    case "get":
                        Console.Write("oib>");
                        oib = Console.ReadLine()?.Trim();
                        packet = Snmp.GenGetPacket(com, oib, ref rid);
                        sock.SendTo(packet, packet.Length, SocketFlags.None, iep);
                        try
                        {
                            var recv = sock.ReceiveFrom(packet, ref ep);
                        }
                        catch (SocketException)
                        {
                            Console.WriteLine("Receive error. Maybe wrong ip?");
                            break;
                        }

                        offset = 6 + packet[6] + 1;
                        if (packet[offset] == 0xa2) //not Response
                        {
                            offset += 4;
                            var arid = (uint) BitConverter.ToInt32(packet, offset);
                            if (arid != rid || packet[offset + 6] != 0 || packet[offset + 9] != 0)
                            {
                                Console.WriteLine("Wrong answer");
                                break;
                            }

                            offset += 16 + packet[offset + 15];
                            switch (packet[offset])
                            {
                                case 2:
                                    Console.WriteLine("Integer type [{0}]", packet[offset + 1]);
                                    Console.WriteLine(BitConverter.ToInt32(packet, offset + 2));
                                    break;
                                case 4:
                                    Console.WriteLine("String type [{0}]", packet[offset + 1]);
                                    Console.WriteLine(Encoding.ASCII.GetString(packet.Skip(offset + 2).ToArray()));
                                    break;
                                case 5:
                                    Console.WriteLine("Null type");
                                    break;
                            }
                        }
                        else
                        {
                            Console.WriteLine("Not RESPONSE was received");
                        }
                        break;
                    case "set":
                        Console.Write("oib> ");
                        oib = Console.ReadLine()?.Trim();
                        Console.Write("type(1 - int, 2 - string, 3 - null)> ");
                        var type = Console.ReadLine()?.Trim();
                        Console.Write("val> ");
                        var val = Console.ReadLine()?.Trim();
                        var data = type switch
                        {
                            "1" => new byte[] {2, (byte) BitConverter
                                .GetBytes(Convert.ToInt32(val)).Count(i => i > 0)}
                                .Concat(BitConverter.GetBytes(Convert.ToInt32(val))
                                .Where(i => i > 0)).ToArray(),
                            "2" => new byte[] {4, (byte) (val.Length % 128)}.Concat(Encoding.ASCII.GetBytes(val)).ToArray(),
                            "3" => new byte[] {5, 0}
                        };
                        packet = Snmp.GenSetPacket(com, oib, data, ref rid);
                        sock.SendTo(packet, packet.Length, SocketFlags.None, iep);
                        try
                        {
                            var recv = sock.ReceiveFrom(packet, ref ep);
                        }
                        catch (SocketException)
                        {
                            Console.WriteLine("Receive error. Maybe wrong ip?");
                            break;
                        }

                        offset = 6 + packet[6] + 1;
                        if (packet[offset] == 0xa2) //not Response
                        {
                            offset += 4;
                            var arid = (uint) BitConverter.ToInt32(packet, offset);
                            if (arid != rid || packet[offset + 6] != 0 || packet[offset + 9] != 0)
                            {
                                Console.WriteLine("Wrong answer");
                                break;
                            }

                            offset += 16 + packet[offset + 15];
                            switch (packet[offset])
                            {
                                case 2:
                                    Console.WriteLine("Integer type [{0}]", packet[offset + 1]);
                                    Console.WriteLine(BitConverter.ToInt32(packet, offset + 2));
                                    break;
                                case 4:
                                    Console.WriteLine("String type [{0}]", packet[offset + 1]);
                                    Console.WriteLine(Encoding.ASCII.GetString(packet.Skip(offset + 2).ToArray()));
                                    break;
                                case 5:
                                    Console.WriteLine("Null type");
                                    break;
                            }
                        }
                        else
                        {
                            Console.WriteLine("Not RESPONSE was received");
                        }
                        break;
                    default:
                        Console.WriteLine("Only get and set implemented");
                        break;
                }
            }
        }
    }
}