using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;

namespace Voting
{
    internal class Client
    {
        private readonly string ip = "127.0.0.1";
        private readonly int port = 1234;
        private readonly Socket _socket;
        private readonly IPEndPoint ipPoint;

        public Client()
        {
            ipPoint = new IPEndPoint(IPAddress.Parse(ip), port);
            _socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            try
            {
                _socket.Connect(ipPoint);

                bool loop = true;
                while (loop)
                {
                    Console.WriteLine("Enter command: ");
                    int id = 0;
                    int variant = 0;
                    int votes = 0;
                    var input = Regex.Split(Console.ReadLine() ?? string.Empty, " +");
                    switch (input[0])
                    {
                        case "get" when input.Length == 2 && input[1] == "all":
                            GetAllRatings();
                            break;
                        case "get" when input.Length == 2 && int.TryParse(input[1], out id):
                            GetRating(id);
                            break;
                        case "create" when input.Length == 4 && int.TryParse(input[2], out variant) &&
                                           int.TryParse(input[3], out votes):
                            CreateRating(input[1], variant, votes);
                            break;
                        case "delete" when input.Length == 2 && int.TryParse(input[1], out id):
                            DeleteRating(id);
                            break;
                        case "add" when input.Length == 3 && int.TryParse(input[1], out id):
                            AddVariant(id, input[2]);
                            break;
                        case "open" when input.Length == 2 && int.TryParse(input[1], out id):
                            OpenCloseVoting(id, true);
                            break;
                        case "close" when input.Length == 2 && int.TryParse(input[1], out id):
                            OpenCloseVoting(id, false);
                            break;
                        case "vote" when input.Length == 4 && int.TryParse(input[1], out id)
                                                           && int.TryParse(input[2], out variant)
                                                           && int.TryParse(input[3], out votes):
                            Vote(id, variant, votes);
                            break;
                        case "exit" when input.Length == 1:
                            loop = false;
                            break;
                        default:
                            Console.WriteLine("Undefined command");
                            break;
                    }
                }
            }
            catch (Exception)
            {
                _socket.Shutdown(SocketShutdown.Both);
                _socket.Close();
                Console.WriteLine("Server is disconnected");
            }
        }


        private void GetRatingDecode(IReadOnlyList<byte> packet)
        {
            if (!packet.Take(4).ToArray().SequenceEqual(new byte[] { 0, 1, 0, 2 })) throw new Exception();
            int votingNum = Decode4Bytes(packet.Skip(4).Take(4).ToArray());
            int size = Decode4Bytes(packet.Skip(8).Take(4).ToArray());
            int current = 12;
            Console.WriteLine($"Variants of voting {votingNum}: ");
            while (current < size)
            {
                var id = Decode4Bytes(packet.Skip(current).Take(sizeof(int)).ToArray());
                current += sizeof(int);
                var votes = Decode4Bytes(packet.Skip(current).Take(sizeof(int)).ToArray());
                current += sizeof(int);
                int i = 0;
                while (packet[current + i] != 0)
                {
                    i++;
                }

                var voting = Encoding.UTF8.GetString(packet.Skip(current).Take(i).ToArray());
                current += i;
                current++;
                Console.WriteLine($"{id}. {voting} -- {votes}");
            }
        }

        private void GetAllRatingsDecode(IReadOnlyList<byte> packet)
        {
            if (!packet.Take(4).ToArray().SequenceEqual(new byte[] { 0, 1, 0, 1 })) throw new Exception();
            int size = Decode4Bytes(packet.Skip(4).Take(4).ToArray());
            int current = 8;
            Console.WriteLine("Rating list: ");
            while (current < size)
            {
                var id = Decode4Bytes(packet.Skip(current).Take(sizeof(int)).ToArray());
                current += sizeof(int);
                int i = 0;
                while (packet[current + i] != 0)
                {
                    i++;
                }

                var voting = Encoding.UTF8.GetString(packet.Skip(current).Take(i).ToArray());
                current += i;
                current++;
                Console.WriteLine($"{id}. {voting}");
            }
        }

        private byte[] GetMessage()
        {
            var data = new byte[4];
            var packet = new List<byte>();
            do
            {
                _socket.Receive(data);
                packet.AddRange(data);

            } while (_socket.Available > 0);

            return packet.ToArray();
        }

        private void GetAllRatings()
        {
            _socket.Send(GetAllRatingsPacket());
            GetAllRatingsDecode(GetMessage());
        }

        private void GetRating(int id)
        {
            _socket.Send(GetRatingPacket(id));
            GetRatingDecode(GetMessage());
        }

        public byte[] GetAllRatingsPacket()
        {
            return Encode2Bytes((int)Codes.GET).Concat(Encode2Bytes((int)Get.ALL)).ToArray();
        }

        public byte[] GetRatingPacket(int id)
        {
            return Encode2Bytes((int)Codes.GET)
                .Concat(Encode2Bytes((int)Get.SPECIFIC))
                .Concat(Encode4Bytes(id))
                .ToArray();
        }

        private void Vote(int id, int variant, int votes)
        {
            _socket.Send(VotePacket(id, variant, votes));
            var data = GetMessage();
            AckDecode(data, Codes.VOTE);
        }

        private byte[] VotePacket(int id, int variant, int votes)
        {
            var packet = new List<byte>();
            packet.AddRange(Encode2Bytes((int)Codes.VOTE));
            packet.AddRange(Encode4Bytes(id));
            packet.AddRange(Encode4Bytes(variant));
            packet.AddRange(Encode4Bytes(votes));
            return packet.ToArray();
        }

        private void OpenCloseVoting(int id, bool open)
        {
            _socket.Send(OpenCloseVotingPacket(id, open));
            var data = GetMessage();
            AckDecode(data, Codes.OPENCLOSE);
        }

        private byte[] OpenCloseVotingPacket(int id, bool open)
        {
            var packet = new List<byte>();
            packet.AddRange(Encode2Bytes((int)Codes.OPENCLOSE));
            packet.AddRange(Encode4Bytes(id));
            packet.Add(open ? (byte)1 : (byte)0);
            return packet.ToArray();
        }

        private void AddVariant(int id, string variant)
        {
            _socket.Send(AddVariantPacket(id, variant));
            var data = GetMessage();
            AckDecode(data, Codes.ADD);
        }

        private byte[] AddVariantPacket(int id, string variant)
        {
            var packet = new List<byte>();
            packet.AddRange(Encode2Bytes((int)Codes.ADD));
            packet.AddRange(Encode4Bytes(id));
            packet.AddRange(Encoding.UTF8.GetBytes(variant));
            packet.Add(0);
            return packet.ToArray();
        }
        private void DeleteRating(int id)
        {
            _socket.Send(DeleteRatingPacket(id));
            var data = GetMessage();
            AckDecode(data, Codes.DELETE);
        }

        private byte[] DeleteRatingPacket(int id)
        {
            return Encode2Bytes((int)Codes.DELETE)
                .Concat(Encode4Bytes(id))
                .ToArray();
        }

        private void CreateRating(string name, int maxVariants, int maxValues)
        {
            _socket.Send(CreateRatingPacket("Clothes", 5, 5));
            byte[] data = GetMessage();
            AckDecode(data, Codes.CREATE);
        }

        private byte[] CreateRatingPacket(string name, int maxVariants, int maxValues)
        {
            var packet = new List<byte>();
            packet.AddRange(Encode2Bytes((int)Codes.CREATE));
            packet.AddRange(Encoding.UTF8.GetBytes(name));
            packet.Add(0);
            packet.AddRange(Encode4Bytes(maxVariants));
            packet.AddRange(Encode4Bytes(maxValues));
            return packet.ToArray();
        }

        private void AckDecode(byte[] packet, Codes code)
        {
            if (packet.Take(2).SequenceEqual(Encode2Bytes((int)Codes.ACK))
                && packet.Skip(2).Take(2).SequenceEqual(Encode2Bytes((int)code)))
            {
                Console.WriteLine("Operation is completed!");
            }
            else if (packet.Take(2).SequenceEqual(Encode2Bytes((int)Codes.ERROR)) && packet.Last() == 0)
            {
                int i = 0;
                while (packet[2 + i] != 0)
                {
                    i++;
                }
                Console.WriteLine($"ERROR: {Encoding.UTF8.GetString(packet.Skip(2).Take(i).ToArray())}");
            }
        }

        private byte[] Encode4Bytes(int number)
        {
            return BitConverter.GetBytes(number).Reverse().ToArray();
        }


        private byte[] Encode2Bytes(int number)
        {
            return BitConverter.GetBytes(number).Reverse().Skip(2).ToArray();
        }


        public int Decode4Bytes(byte[] bytes)
        {
            return BitConverter.ToInt32(bytes.Reverse().ToArray());
        }

        private enum Codes
        {
            GET = 1, CREATE = 2, DELETE = 3, ADD = 4, OPENCLOSE = 5, VOTE = 6, ACK = 7, ERROR = 8
        }

        private enum Get
        {
            ALL = 1, SPECIFIC = 2
        }

    }
}
