using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using Newtonsoft.Json;

namespace TestClient
{
    internal static class Program
    {
        private static string Ip = "127.0.0.1";
        private static int Port = 6000;
        
        private class CommandPacket
        {
            public int Type;
        }
        private class LoginPacket
        {
            public int Type = 0;
            public string Login;
        }
        
        private class TestPacket
        {
            public int Id;
            public string Name;
        }
        
        private class StartPacket
        {
            public int Type = 3;
            public int Tid;
        }
        
        private class EndPacket
        {
            public int Type = 4;
            public int Sid;
            public IList<int> Answers;
        }

        class QuestionPacket
        {
            public int Sid;
            public IList<QuestionPart> Test;
        }
        
        private class QuestionPart
        {
            public string Question;
            public IList<string> Answers;
        }

        private static void Main(string[] args)
        {
            if (args.Length >= 2) {
                Ip = args[0];
                Port = Convert.ToInt32(args[1]);
            }
            var errorType = new { Error = (int?)0, Text = "" };
            var resType = new { Res = "" };
            
            var client = new TcpClient();
            client.Connect(new IPEndPoint(IPAddress.Parse(Ip), Port));
            var stream = client.GetStream();
            Console.WriteLine("Client is up");
            while (true)
            {
                byte[] packet, buffer;
                int size;
                string answer;
                switch (ReadNotEmpty(""))
                {
                    case "login":
                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new LoginPacket()
                            { Login = ReadNotEmpty("What is your login?") }));
                        stream.Write(packet, 0, packet.Length);
                        buffer = new byte[128];
                        size = stream.Read(buffer);
                        answer = Encoding.Unicode.GetString(buffer.Take(size).ToArray());
                        if (JsonConvert.DeserializeAnonymousType(answer, errorType).Error.HasValue)
                            Console.WriteLine(JsonConvert.DeserializeAnonymousType(answer, errorType).Text);
                        else
                        {
                            var id = new { Id = 0 };
                            Console.WriteLine("Login successfully. Your id " + JsonConvert.DeserializeAnonymousType(answer, id).Id);
                        }
                        break;
                    case "list":
                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new CommandPacket() { Type = 1 }));
                        stream.Write(packet, 0, packet.Length);
                        buffer = new byte[1024];
                        size = stream.Read(buffer);
                        answer = Encoding.Unicode.GetString(buffer.Take(size).ToArray());
                        var list = JsonConvert.DeserializeObject<List<TestPacket>>(answer);
                        Console.WriteLine("Tests list");
                        foreach (var test in list)
                        {
                            Console.WriteLine("{0}. {1}", test.Id, test.Name);
                        }
                        break;
                    case "result":
                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new CommandPacket() { Type = 2 }));
                        stream.Write(packet, 0, packet.Length);
                        buffer = new byte[128];
                        size = stream.Read(buffer);
                        answer = Encoding.Unicode.GetString(buffer.Take(size).ToArray());
                        Console.WriteLine("Your last mark was {0}", JsonConvert.DeserializeAnonymousType(answer, resType).Res);
                        break;
                    case "newtest":
                        var tid = ReadNotEmpty("Id of new test? (non digit will cancel command)");
                        if (!int.TryParse(tid, out _)) break;
                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new StartPacket()
                            { Tid = Convert.ToInt32(tid) }));
                        stream.Write(packet, 0, packet.Length);
                        buffer = new byte[1024];
                        size = stream.Read(buffer);
                        answer = Encoding.Unicode.GetString(buffer.Take(size).ToArray());
                        var tmp = JsonConvert.DeserializeObject<QuestionPacket>(answer);
                        var questions = tmp.Test;
                        var answers = new List<int>();
                        for (var i = 0; i < questions.Count; i++)
                        {
                            Console.WriteLine("{0} - {1}", i + 1, questions[i].Question);
                            for (var j = 0; j < questions[i].Answers.Count; j++)
                            {
                                Console.WriteLine("{0}. {1}", j + 1, questions[i].Answers[j]);
                            }

                            do answer = ReadNotEmpty("");
                            while (!Enumerable.Range(1, questions[i].Answers.Count).Contains(Convert.ToInt32(answer)));
                            answers.Add(Convert.ToInt32(answer));
                        }

                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new EndPacket()
                            { Sid = tmp.Sid, Answers = answers }));
                        stream.Write(packet, 0, packet.Length);
                        buffer = new byte[128];
                        size = stream.Read(buffer);
                        answer = Encoding.Unicode.GetString(buffer.Take(size).ToArray());
                        Console.WriteLine("Your mark {0}", JsonConvert.DeserializeAnonymousType(answer, resType).Res);
                        break;
                    case "quit":
                        packet = Encoding.Unicode.GetBytes(JsonConvert.SerializeObject(new CommandPacket() { Type = 5 }));
                        stream.Write(packet, 0, packet.Length);
                        stream.Close();
                        client.Close();
                        return;
                    default:
                        Console.WriteLine("Help page");
                        Console.WriteLine("login - login on server");
                        Console.WriteLine("list - get list of tests on server");
                        Console.WriteLine("result - get result of last completed test");
                        Console.WriteLine("newtest - start new test");
                        Console.WriteLine("quit");
                        break;
                }
            }
        }

        private static string ReadNotEmpty(string title)
        {
            var target = "";
            if (title.Length > 0)
                Console.WriteLine(title);
            Console.Write(">");
            while (string.IsNullOrWhiteSpace(target))
                target = Console.ReadLine()?.Trim();
            return target;
        }
    }
}