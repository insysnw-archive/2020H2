using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;

namespace TftpClient
{
    public class Program
    {
        static void Main(string[] args)
        {
            Console.WriteLine("Hello World!");
            new TftpClient();
        }
    }

    internal class TftpClient
    {
        private Socket socket;
        private EndPoint serverCommand = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 69);
        private EndPoint serverData = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 0);

        private Dictionary<int, string> error = new Dictionary<int, string>
        {
            {0, "Not defined" },
            {1, "File not found." },
            {2, "Access violation." },
            {3, "Disk full or allocation exceeded." },
            {4, "Illegal TFTP operation." },
            {5, "Unknown transfer ID." },
            {6, "File already exists." },
            {7, "No such user." }
        };

        internal enum Opcode
        {
            RRQ = 1, WRQ = 2, DATA = 0b_0011, ACK = 0b_0100, ERROR = 0b_0101
        }

        public TftpClient()
        {
            socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp) {ReceiveTimeout = 1000};

            var loop = true;

            while (loop)
            {
                Console.WriteLine("Enter command: ");
                var input = Regex.Split(Console.ReadLine() ?? string.Empty, " +");
                switch (input[0])
                {
                    case "get" when input.Length == 3: Read(input[1], input[2]); break;
                    case "put" when input.Length == 3: Write(input[1], input[2]); break;
                    case "exit": loop = false; break;
                    default: Console.WriteLine("Undefined command"); break;
                }
            }
        }

        public void Read(string filename, string mode)
        {
            var rrq = MakeRRQorWRQ(Opcode.RRQ, filename, mode);
            socket.SendTo(rrq, serverCommand);

            byte[] data = new byte[516];
            StringBuilder builder = new StringBuilder();

            int size = 0;
            int totalSize = 0;

            do
            {
                size = socket.ReceiveFrom(data, ref serverData);
                if (IsError(data)) return;
                totalSize += size - 4;
                var block = Decode2Bytes(data.Skip(2).Take(2).ToArray());
                builder.Append(Encoding.UTF8.GetString(data, 4, size - 4));
                socket.SendTo(MakeACK(block), serverData);

            } while (size == 516);

            using var sw = new StreamWriter($"../../../{filename}", false, Encoding.UTF8);
            sw.WriteLine(builder);
            ((IPEndPoint)serverData).Port = 0;
            Console.WriteLine($"{totalSize} bytes were received");
        }

        public void Write(string filename, string mode)
        {
            var wrq = MakeRRQorWRQ(Opcode.WRQ, filename, mode);
            socket.SendTo(wrq, serverCommand);

            byte[] data = new byte[516];
            StringBuilder builder = new StringBuilder();

            int size = socket.ReceiveFrom(data, ref serverData);
            if (IsError(data)) return;

            if (!File.Exists($"../../../{filename}"))
            {
                Console.WriteLine("File does not exist");
                return;
            }
            using var sr = new StreamReader($"../../../{filename}", Encoding.UTF8);
            var text = sr.ReadToEnd();
            byte[] textBytes = Encoding.UTF8.GetBytes(text);
            int block = 1;

            do
            {
                var ack = new byte[4];
                var packet = MakeData(block, textBytes.Skip(512 * (block - 1)).Take(512).ToArray());

                int time = 1;
                while (!ack.SequenceEqual(MakeACK(block)))
                {
                    socket.SendTo(packet, serverData);
                    socket.ReceiveFrom(ack, ref serverData);
                    if (time++ != 6) continue;
                    Console.WriteLine("Operation was unsuccessful");
                    return;
                }

            } while (textBytes.Length - block++ * 512 > 0);
            ((IPEndPoint)serverData).Port = 0;
            Console.WriteLine($"{textBytes.Length} bytes were sent");
        }

        public bool IsError(byte[] packet)
        {
            var opcode = Decode2Bytes(packet.Take(2).ToArray());
            if (opcode != 5) return false;
            var errorcode = Decode2Bytes(packet.Skip(2).Take(2).ToArray());
            var message = Encoding.UTF8.GetString(packet.Skip(4).ToArray());
            Console.WriteLine($"ERR {errorcode}: {message}");
            return true;
        }

        public int Decode2Bytes(byte[] bytes)
        {
            var array = new byte[4];
            Array.Copy(bytes, 0, array, 2, 2);
            return BitConverter.ToInt32(array.Reverse().ToArray(), 0);
        }

        public byte[] MakeData(int block, byte[] data)
        {
            byte[] opcodeBytes = Encode2Bytes((int)Opcode.DATA);
            byte[] blockBytes = BitConverter.GetBytes(block).Reverse().Skip(2).ToArray();
            byte[] packet = new byte[opcodeBytes.Length + blockBytes.Length + data.Length];

            opcodeBytes.CopyTo(packet, 0);
            blockBytes.CopyTo(packet, opcodeBytes.Length);
            data.CopyTo(packet, opcodeBytes.Length + blockBytes.Length);

            return packet;
        }

        public byte[] MakeACK(int block)
        {
            byte[] packet = new byte[4];
            Array.Copy(Encode2Bytes((int)Opcode.ACK), packet, 2);
            Array.Copy(Encode2Bytes(block), 0, packet, 2, 2);
            return packet;
        }


        public byte[] MakeRRQorWRQ(Opcode opcode, string filename, string mode)
        {
            byte[] opcodeBytes = Encode2Bytes((int)opcode);
            byte[] filenameBytes = Encoding.Default.GetBytes(filename);
            byte[] modeBytes = Encoding.Default.GetBytes(mode);
            byte[] packet = new byte[2 + filename.Length + 1 + mode.Length + 1];

            opcodeBytes.CopyTo(packet, 0);
            filenameBytes.CopyTo(packet, 2);
            modeBytes.CopyTo(packet, 2 + filenameBytes.Length + 1);

            return packet;
        }

        public byte[] MakeERR(int errorcode)
        {
            byte[] opcodeBytes = Encode2Bytes((int)Opcode.ERROR);
            byte[] errorcodeBytes = Encode2Bytes(errorcode);
            byte[] messageBytes = Encoding.UTF8.GetBytes(error[errorcode]);
            byte[] packet = new byte[opcodeBytes.Length + errorcodeBytes.Length + messageBytes.Length + 1];

            opcodeBytes.CopyTo(packet, 0);
            errorcodeBytes.CopyTo(packet, opcodeBytes.Length);
            messageBytes.CopyTo(packet, opcodeBytes.Length + errorcodeBytes.Length);

            return packet;
        }

        public byte[] Encode2Bytes(int number)
        {
            return BitConverter.GetBytes(number).Reverse().Skip(2).ToArray();
        }


    }

}
