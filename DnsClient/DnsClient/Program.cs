using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Net;
using System.Net.Sockets;

namespace DnsClient
{
    class Program
    {
        private static Socket _socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
        private static EndPoint serverPoint;
        private static string serverAddress = "8.8.8.8";
        private static DnsPacket packet;
        private static bool error = false;

        public static void Main(string[] arg)
        {
            
            Console.WriteLine("Hi. It is Dns client" +
                "\nWrite domain to get IP" +
                "\nParameters after domain can change configuration" +
                "\n-type <mx>. Type A by default" +
                "\n-host <1.1.1.1>. Host 8.8.8.8 by default");

            string line = "";
            while(line != "exit")
            {
                packet = new DnsPacket();
                packet.qtype = Type.A;
                line = Console.ReadLine();

                var param = line.Split(' ');

                packet.qname = param[0];

                for(int i = 0;i<param.Length;i++)
                {
                   

                    if(param[i] == "-type")
                    {
                        if (param[i + 1].ToLower() == "mx")
                            packet.qtype = Type.MX;
                        if (param[i + 1].ToLower() == "txt")
                            packet.qtype = Type.TXT;
                    }
                    if (param[i] == "-host")
                    {
                        if (IsValid(param[i + 1]))
                        {
                            serverAddress = param[i + 1];
                        }
                        else
                        {
                            Console.WriteLine("Host invalid");
                            error = true;
                            break;
                        }
                    }
                }
                if (error)
                {
                    error = false;
                    continue;
                }
                packet.qr = QR.Request;
                packet.opcode = Opcode.Query;
                Client();
            }
            
            
            


        }

        public static bool IsValid(string str)
        {
            var b = str.Split('.');
            if (b.Length != 4)
                return false;

            foreach(string s in b)
            {
                var i = 0;
                if(!int.TryParse(s,out i))
                {
                    return false;
                }
            }
            return true;
        }

        public static void Client()
        {

            
            byte[] b = Pack(packet);

            IPEndPoint ipRemoteEndPoint = new IPEndPoint(IPAddress.Parse(serverAddress), 53);
            Socket udpClient = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);

            IPEndPoint ipLocalEndPoint = new IPEndPoint(IPAddress.Any, 0);
            EndPoint localEndPoint = (EndPoint)ipLocalEndPoint;
            udpClient.Bind(localEndPoint);

            udpClient.Connect(ipRemoteEndPoint);
            

            //Console.WriteLine("Send to server");
            for (int i = 0; i < b.Length; i++)
            {
                Console.Write(b[i] + " ");
            }
            //Console.WriteLine("");
            udpClient.Send(b);//, serverPoint);
            byte[] r = new byte[512];
            udpClient.Receive(r);//, ref serverPoint);
            Unpack(r,b.Length);
            //Console.WriteLine("Received from server");
            for (int i = 0; i < r.Length; i++)
            {
                Console.Write(r[i] + " ");
            }



        }
        

        public static byte[] FakePack()
        {
            return new byte[] { 0xAA, 0xAA, 0x00, 0x0, 0x0, 0x01, 0x00, 0x0, 0x00, 0x0, 0x00, 0x00, 0x2, 0x76, 0x6b, 0x3, 0x63, 0x6f, 0x6d, 0x0, 0x0, 0x01, 0x00, 0x1 };
        }


        public static void Unpack(byte[] data,int requestLengh)
        {
            List<byte> res = new List<byte>();

            for(int i = requestLengh+12;i < data.Length;i+=12)
            {
                if (data[i] == 0 && data[i + 1] == 0 && data[i + 2] == 0 && data[i + 3] == 0)
                    break;
                res.Add(data[i++]);
                res.Add(data[i++]);
                res.Add(data[i++]);
                res.Add(data[i++]);
                
            }
            int c = 0;
            string ip = "";
            Console.WriteLine("Host addr: " + serverAddress);
            Console.WriteLine("Type: " + packet.qtype);
            Console.WriteLine($"Results for request {packet.name}: " + res.Count/4);
            foreach (byte b in res)
            {
                
                if(c<3)
                ip += b + ".";
                else
                {
                    ip += b;
                    Console.WriteLine(ip);
                    ip = "";
                    c = -1;
                }
                c++;
            } 
        }

        public static byte[] Pack(DnsPacket packet)
        {
            var p = packet;


            //byte[] bID = Encoding.UTF8.GetBytes("AAAA");//BitConverter.GetBytes(p.ID);
            short parameters = (short)((byte)p.qr | (byte)p.opcode >> 1 | (byte)p.autorativeAnswer >> 5
                               | (byte)p.trunCation >> 6 | (byte)p.recursionDesired >> 7
                               | (byte)p.recursionAvailable >> 8 | (byte)p.reserved >> 9 | (byte)p.rcode >> 12);

            //Console.WriteLine("Params " + parameters);

            byte[] bParam = BitConverter.GetBytes(parameters);
            byte[] bQd = BitConverter.GetBytes(p.qdCount);
            byte[] bAn = BitConverter.GetBytes(p.anCount);
            byte[] bSs = BitConverter.GetBytes(p.ssCount);
            byte[] bAr = BitConverter.GetBytes(p.arCount);

            string[] parts = p.qname.Split('.');
            

            byte[] bName = new byte[p.qname.Length+2];
            var counter = 0;
            for (int i = 0; i < parts.Length; i++)
            {
                bName[counter] = BitConverter.GetBytes(parts[i].Length)[0];
                counter++;
                var b = Encoding.Default.GetBytes(parts[i]);
                for (int k = 0;k<b.Length;k++)
                {
                    bName[counter] = b[k];
                    counter++;
                }

            }
            bName[bName.Length - 1] = 0;
            Console.WriteLine("Q TYpe " + (short)p.qtype);
            byte[] bQType = BitConverter.GetBytes((short)p.qtype);
            byte[] bQClass = BitConverter.GetBytes(p.qclass);

            var nameSize = bName.Length;// % 16 + bName.Length;
            byte[] dnsData = new byte[16 + nameSize];

            dnsData[0] = 0xAA;
            dnsData[1] = 0xAA;
            dnsData[2] = bParam[0];
            dnsData[3] = bParam[1];

            dnsData[4] = bQd[1];
            dnsData[5] = bQd[0];
            dnsData[6] = bAn[0];
            dnsData[7] = bAn[1];
            dnsData[8] = bSs[0];
            dnsData[9] = bSs[1];
            dnsData[10] = bAr[0];
            dnsData[11] = bAr[1];
            
            dnsData[12 + nameSize] = bQType[1];
            dnsData[13 + nameSize] = bQType[0];
            dnsData[14 + nameSize] = bQClass[1];
            dnsData[15 + nameSize] = bQClass[0];
            

            for (int i = 0; i < bName.Length; i++)
            {
                dnsData[12 + i] = bName[i];
            }

            dnsData[11 + nameSize] = BitConverter.GetBytes((short)0)[0];

            return dnsData;
        }
        
    }
    
    public class DnsPacket
    {
        //header
        public byte[] ID = new byte[2];                // 16 bit
        public QR qr;                   // 1 bit
        public Opcode opcode;           //4 bits
        public AA autorativeAnswer;     // Равен 1 при ответе от сервера, в ведении которого находится домен, упомянутый в запросе.
        public TC trunCation;           //Равен 1 при укорочении сообщения. Для UDP это означает, что ответ содержал более 512 октетов, но прислано только первые 512.
        public RD recursionDesired;     // Равен 1, если для получения ответа желательна рекурсия.
        public RA recursionAvailable;   //  Равен 1, если рекурсия для запрашиваемого сервера доступна.
        public Z reserved = Z.False;    // Зарезервировано на будущее. Должны равняться нулю.
        public Rcode rcode;

        public short qdCount = 0x1; //should set this field to 1, indicating you have one question.
        public short anCount = 0x0; //You should set this field to 0, indicating you are not providing any answers.
        public short ssCount = 0x0; //You should set this field to 0, and should ignore any response entriesin this section.
        public short arCount = 0x0; //You should set this field to 0,  and should ignore any response entries in thissection.

        // question
        public string qname;
        public Type qtype;
        public short qclass = 0x1;

        //answer
        public string name;
        public short type;
        public short aClass;
        public int ttl;
        public short rdLength;
        public long rData;

    }

    public enum QR { Request = 0x0, Responce = 0x1 }
    public enum Opcode { Query = 0x0, IQuery = 0x1, Status = 0x2 }
    public enum AA { False = 0x0, True = 0x1 }
    public enum TC { False = 0x0, True = 0x1 }
    public enum RD { False = 0x0, True = 0x1 }
    public enum RA { False = 0x0, True = 0x1 }
    public enum Z { False = 0x0, True = 0x1 }
    public enum Rcode { NoError = 0x0, RequestError = 0x1, ServerError = 0x2, NoSuchDomain = 0x3, CantProcessRequest = 0x4, Rejected = 0x5 }
    public enum Type { A = 0x1, MX = 0xe, TXT = 0x10}


       
}
