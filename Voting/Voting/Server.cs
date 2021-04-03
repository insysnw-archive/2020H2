using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

namespace Voting
{
    class Server
    {
        private readonly string _ip = "127.0.0.1";
        private readonly int _port = 1234;
        private readonly List<Socket> _clients;
        private List<Voting> _votingList;


        public Server()
        {
            _votingList = new List<Voting>();
            _clients = new List<Socket>();
            IPEndPoint ipPoint = new IPEndPoint(IPAddress.Parse(_ip), _port);
            var listenSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);

            _votingList.Add(new Voting("Chocolate", 2, 5));
            _votingList[0].VariantVote.AddRange(
                new (string Variant, int Votes)[] { ("Choco1", 5), ("Choco2", 6) });
            _votingList.Add(new Voting("Milk", 5, 5));


            try
            {
                listenSocket.Bind(ipPoint);
                listenSocket.Listen(int.MaxValue);
                Console.WriteLine("The server is running. Waiting for connections...");
                while (true)
                {
                    Socket handler = listenSocket.Accept();
                    if (_clients.Contains(handler)) continue;
                    var thread = new Thread(() => GetMessages(handler));
                    try
                    {
                        thread.Start();
                    }
                    catch (Exception)
                    {
                        thread.Abort();
                        handler.Shutdown(SocketShutdown.Both);
                        handler.Close();
                    }
                    Console.WriteLine($"{handler.RemoteEndPoint} is connected");
                }
            }
            catch (Exception e)
            {
                Console.WriteLine(e.Message);
            }
        }

        private void GetMessages(Socket socket)
        {
            try
            {
                while (true)
                {
                    GetMessage(socket);
                }
            }
            catch (Exception)
            {
                Console.WriteLine($"{socket.RemoteEndPoint} is disconnected");
                socket.Shutdown(SocketShutdown.Both);
                socket.Close();
            }

        }

        private void GetMessage(Socket socket)
        {
            byte[] data = new byte[4];
            List<byte> packet = new List<byte>();

            try
            {
                do
                {
                    socket.Receive(data);
                    packet.AddRange(data);

                } while (socket.Available > 0);

                if (packet.Count >= 4 && packet[0] != 0) throw new Exception();
                switch (packet[1])
                {
                    case 1:
                        GetDecode(socket, packet.ToArray());
                        break;
                    case 2 when packet.Count >= 14:
                        CreateDecode(socket, packet.ToArray());
                        break;
                    case 3 when packet.Count >= 6:
                        DeleteDecode(socket, packet.ToArray());
                        break;
                    case 4 when packet.Count >= 8:
                        AddDecode(socket, packet.ToArray());
                        break;
                    case 5 when packet.Count >= 7:
                        OpenCloseDecode(socket, packet.ToArray());
                        break;
                    case 6 when packet.Count >= 14:
                        VoteDecode(socket, packet.ToArray());
                        break;
                    default:
                        socket.Send(ErrorPacket("Not defined error"));
                        break;
                }
            }
            catch (Exception)
            {
                throw;
            }
        }

        private void VoteDecode(Socket socket, byte[] packet)
        {
            int id = Decode4Bytes(packet.Skip(2).Take(4).ToArray());
            if (!CheckId(id))
            {
                socket.Send(ErrorPacket("Voting does not found"));
                return;
            }

            if (!_votingList[id].Status)
            {
                socket.Send(ErrorPacket("Voting is closed"));
                return;
            }
            int variant = Decode4Bytes(packet.Skip(6).Take(4).ToArray());
            if (!CheckVariant(_votingList[id], variant))
            {
                socket.Send(ErrorPacket("Variant does not found"));
                return;
            }
            int votes = Decode4Bytes(packet.Skip(10).Take(4).ToArray());
            if (_votingList[id].MaxVotes < votes)
            {
                socket.Send(ErrorPacket("Too much votes"));
                return;
            }

            _votingList[id].VariantVote[variant] = (_votingList[id].VariantVote[variant].Variant,
                _votingList[id].VariantVote[variant].Votes + votes);
            socket.Send(AckPacket(Codes.VOTE));
        }

        private void OpenCloseDecode(Socket socket, byte[] packet)
        {
            int id = Decode4Bytes(packet.Skip(2).Take(4).ToArray());
            if (!CheckId(id))
            {
                socket.Send(ErrorPacket("Voting does not found"));
                return;
            }
            var open = packet[6] == 1;
            var voting = _votingList[id];
            if (voting.Status && open)
            {
                socket.Send(ErrorPacket("Voting is opened"));
                return;
            }
            else if (!(voting.Status || open))
            {
                socket.Send(ErrorPacket("Voting is closed"));
                return;
            }

            _votingList[id].Status = open;
            socket.Send(AckPacket(Codes.OPENCLOSE));

        }

        private void GetDecode(Socket socket, byte[] packet)
        {
            if (packet.Length == 4 && packet[2] == 0 && packet[3] == 1) socket.Send(GetAllRatingPacket());
            else if (packet.Length == 8 && packet[2] == 0 && packet[3] == 2)
                socket.Send(GetRatingPacket(Decode4Bytes(packet.Skip(4).ToArray())));
        }

        private void AddDecode(Socket socket, byte[] packet)
        {
            int id = Decode4Bytes(packet.Skip(2).Take(4).ToArray());
            if (_votingList.Count <= id)
            {
                socket.Send(ErrorPacket("Voting does not found"));
                return;
            }
            var current = 0;
            while (packet[6 + current] != 0)
            {
                current++;
            }

            var variant = Encoding.UTF8.GetString(packet.Skip(6).Take(current).ToArray());
            var voting = _votingList[id];
            if (voting.VariantVote.Any(x => x.Variant == variant))
            {
                socket.Send(ErrorPacket("Variant exists"));
                return;
            }
            else if (voting.VariantVote.Count == voting.MaxSize)
            {
                socket.Send(ErrorPacket("Too much variants"));
                return;
            }
            _votingList[id].VariantVote.Add((variant, 0));
            socket.Send(AckPacket(Codes.ADD));
        }

        private void CreateDecode(Socket socket, byte[] packet)
        {
            int current = 0;
            while (packet[2 + current] != 0)
            {
                current++;
            }

            var name = Encoding.UTF8.GetString(packet.Skip(2).Take(current).ToArray());
            if (_votingList.Any(v => v.Name == name)) throw new Exception();
            current += 3;
            var maxVariants = Decode4Bytes(packet.Skip(current).Take(4).ToArray());
            var maxVotes = Decode4Bytes(packet.Skip(current + 4).Take(4).ToArray());
            _votingList.Add(new Voting(name, maxVariants, maxVotes));
            socket.Send(AckPacket(Codes.CREATE));
        }

        private void DeleteDecode(Socket socket, byte[] packet)
        {
            var id = Decode4Bytes(packet.Skip(2).Take(4).ToArray());
            if (_votingList.Count > id)
            {
                _votingList.RemoveAt(id);
                socket.Send(AckPacket(Codes.DELETE));
            }
            else socket.Send(ErrorPacket("Voting does not found"));
        }

        private byte[] AckPacket(Codes code)
        {
            var packet = new List<byte>();
            packet.AddRange(Encode2Bytes((int)Codes.ACK));
            packet.AddRange(Encode2Bytes((int)code));
            return packet.ToArray();
        }

        private byte[] ErrorPacket(string message)
        {
            return Encode2Bytes((int)Codes.ERROR)
                .Concat(Encoding.UTF8.GetBytes(message))
                .Concat(new List<byte> { 0 }).ToArray();
        }


        private byte[] GetAllRatingPacket()
        {
            var opcode = Encode2Bytes((int)Codes.GET);
            var mode = Encode2Bytes((int)Get.ALL);
            var packet = opcode.Concat(mode).ToList();

            var data = new List<byte>();

            for (int i = 0; i < _votingList.Count; i++)
            {
                var idBytes = BitConverter.GetBytes(i).Reverse().ToArray();
                var nameBytes = Encoding.UTF8.GetBytes(_votingList[i].Name);
                data.AddRange(idBytes);
                data.AddRange(nameBytes);
                data.Add(0);
            }

            packet.AddRange(Encode4Bytes(data.Count));
            packet.AddRange(data);
            return packet.ToArray();
        }

        private byte[] GetRatingPacket(int id)
        {
            var packet = new List<byte>();
            var voting = _votingList[id];

            packet.AddRange(Encode2Bytes((int)Codes.GET));
            packet.AddRange(Encode2Bytes((int)Get.SPECIFIC));
            packet.AddRange(Encode4Bytes(id));

            var data = new List<byte>();
            for (int i = 0; i < voting.VariantVote.Count; i++)
            {
                data.AddRange(Encode4Bytes(i));
                data.AddRange(Encode4Bytes(voting.VariantVote[i].Votes));
                data.AddRange(Encoding.UTF8.GetBytes(voting.VariantVote[i].Variant));
                data.Add(0);
            }

            packet.AddRange(Encode4Bytes(data.Count));
            packet.AddRange(data);

            return packet.ToArray();
        }

        private bool CheckId(int id)
        {
            return _votingList.Count > id;
        }

        private bool CheckVariant(Voting voting, int variant)
        {
            return voting.VariantVote.Count > variant;
        }

        public int Decode4Bytes(byte[] bytes)
        {
            return BitConverter.ToInt32(bytes.Reverse().ToArray());
        }


        private byte[] Encode4Bytes(int number)
        {
            return BitConverter.GetBytes(number).Reverse().ToArray();
        }


        private byte[] Encode2Bytes(int number)
        {
            return BitConverter.GetBytes(number).Reverse().Skip(2).ToArray();
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
