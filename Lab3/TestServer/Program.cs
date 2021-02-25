using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Newtonsoft.Json;
using static Newtonsoft.Json.JsonConvert;

namespace TestServer
{
    public static class Program
    {
        private const string Ip = "0.0.0.0";
        private static int Port = 6000;
        private static SqliteConnection _connection;
        private static List<int> users = new List<int>();

        private static void Main(string[] args)
        {
            if (args.Length >= 1)
                Port = Convert.ToInt32(args[0]);
            _connection = new SqliteConnection("Data Source=flash.db;");
            _connection.Open();

            var socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            socket.Bind(new IPEndPoint(IPAddress.Parse(Ip), Port));
            socket.Listen(10);
            Console.CancelKeyPress += (sender, eventArgs) => socket.Close();
            while (true)
            {
                var handler = socket.Accept();
                Task.Factory.StartNew(() => ClientThread(handler));
            }

            // ReSharper disable once FunctionNeverReturns
        }

        private static void ClientThread(Socket handler)
        {
            var typeCommand = new { Type = Commands.Start };
            var id = -1;
            while (true)
            {
                var tmp = new byte[1024];
                var size = handler.Receive(tmp);
                var request = Encoding.Unicode.GetString(tmp.Take(size).ToArray());
                var type = DeserializeAnonymousType(request, typeCommand)?.Type;
                if (!type.HasValue)
                {
                    handler.Send(
                        Encoding.Unicode.GetBytes(@"{'Error': -2, 'Text': 'Type field was excepted'}"));
                    continue;
                }
                switch (type.Value)
                {
                    case Commands.Login:
                        if (id != -1)
                            handler.Send(
                                Encoding.Unicode.GetBytes(@"{'Error': 1, 'Text': 'Login has already been completed'}"));
                        else LoginAction(handler, request, ref id);
                        Console.WriteLine("Login succeeded. ID {0}", id);
                        break;
                    case Commands.List:
                        ListAction(handler);
                        break;
                    case Commands.Result:
                        if (id == -1)
                            handler.Send(
                                Encoding.Unicode.GetBytes(@"{'Error': 2, 'Text': 'Login was not performed'}"));
                        else
                            LastResultAction(handler, id);
                        break;
                    case Commands.Start:
                        if (id == -1)
                            handler.Send(
                                Encoding.Unicode.GetBytes(@"{'Error': 2, 'Text': 'Login was not performed'}"));
                        else
                            StartTestAction(handler, request, id);
                        break;
                    case Commands.End:
                        if (id == -1)
                            handler.Send(
                                Encoding.Unicode.GetBytes(@"{'Error': 2, 'Text': 'Login was not performed'}"));
                        else
                        {
                            EndTestAction(handler, request);
                        }
                        break;
                    case Commands.Logout:
                        users.Remove(id);
                        return;
                    default: throw new ArgumentOutOfRangeException();
                }
            }
        }

        private static void EndTestAction(Socket handler, string request)
        {
            var result = DeserializeObject<TypeEnd>(request);
            if (result == null || result.Sid == -1)
            {
                handler.Send(
                    Encoding.Unicode.GetBytes(@"{'Error': 4, 'Text': 'Tid field was excepted'}"));
                return;
            }

            var sql = new SqliteCommand(
                "Select t.size from tests t inner join results r on r.tid = t.id where r.id = @id",
                _connection);
            sql.Parameters.AddWithValue("@id", result.Sid);
            var tsize = Convert.ToInt32(sql.ExecuteScalar());
            if (tsize != result.Answers.Count)
            {
                handler.Send(
                    Encoding.Unicode.GetBytes(@"{'Error': 5, 'Text': 'Answers field has wrong size'}"));
                return;
            }

            sql = new SqliteCommand(
                @"select rnum from questions q inner join results r on r.tid = q.tid where r.id = @id order by r.id",
                _connection);
            sql.Parameters.AddWithValue("@id", result.Sid);
            int pos = 0, res = 0;
            using (var read = sql.ExecuteReader())
                while (read.Read())
                    if (read.GetInt32(0) == result.Answers[pos++])
                        res++;
            sql = new SqliteCommand("update results set res = @res where id = @sid", _connection);
            sql.Parameters.AddWithValue("@res", res);
            sql.Parameters.AddWithValue("@sid", result.Sid);
            sql.ExecuteNonQuery();
            handler.Send(Encoding.Unicode.GetBytes("{'Res': '" + res + "/" + tsize + "'}"));
        }

        private static void StartTestAction(Socket handler, string request, int id)
        {
            var typeStart = new { Tid = 0 };
            var start = DeserializeAnonymousType(request, typeStart)?.Tid;
            if (!start.HasValue)
            {
                handler.Send(
                    Encoding.Unicode.GetBytes(@"{'Error': 3, 'Text': 'Tid field was excepted'}"));
                return;
            }

            var sql = new SqliteCommand(@"INSERT INTO results (uid, tid) values (@uid, @tid)", _connection);
            sql.Parameters.AddWithValue("@uid", id);
            sql.Parameters.AddWithValue("@tid", start.Value);
            sql.ExecuteNonQuery();
            var sid = Convert.ToInt32(new SqliteCommand(@"select last_insert_rowid()", _connection)
                .ExecuteScalar());
            var res = new List<TypeQuestion>();
            sql = new SqliteCommand(@"Select body, variant from questions where tid = @tid order by id",
                _connection);
            sql.Parameters.AddWithValue("@tid", start.Value);
            using (var read = sql.ExecuteReader())
                while (read.Read())
                    res.Add(new TypeQuestion()
                        { Question = read.GetString(0), Answers = DeserializeObject<List<string>>(read.GetString(1)) });
            handler.Send(Encoding.Unicode.GetBytes(@"{'Sid': " + sid + ", 'Test': " + SerializeObject(res) + "}"));
        }

        private static void LastResultAction(Socket handler, int id)
        {
            var cmd = new SqliteCommand(
                @"select r.res || '/' || t.size from results r inner join tests t on r.tid = t.id where r.res is not null and r.uid = @id order by r.id desc limit 1",
                _connection);
            cmd.Parameters.AddWithValue("@id", id);
            string res;
            try
            {
                res = Convert.ToString(cmd.ExecuteScalar());
            } catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
            handler.Send(Encoding.Unicode.GetBytes(@"{'Res': '" + res + "'}"));
        }

        private static void ListAction(Socket handler)
        {
            var res = new List<TypeTest>();
            var cmd = new SqliteCommand(@"Select id, name from tests", _connection);
            using (var sql = cmd.ExecuteReader())
                while (sql.Read())
                    res.Add(new TypeTest() { Id = sql.GetInt32(0), Name = sql.GetString(1) });

            handler.Send(Encoding.Unicode.GetBytes(SerializeObject(res.ToArray())));
        }

        private static void LoginAction(Socket handler, string request, ref int id)
        {
            var typeLogin = new { Login = "" };
            var login = DeserializeAnonymousType(request, typeLogin)?.Login;
            if (login == null)
            {
                handler.Send(
                    Encoding.Unicode.GetBytes(@"{'Error': -1, 'Text': 'Login field was excepted'}"));
                return;
            }

            var cmd = new SqliteCommand(@"select id from users where login = @login", _connection);
            cmd.Parameters.AddWithValue("@login", login);
            id = Convert.ToInt32(cmd.ExecuteScalar());
            if (id == -1)
            {
                cmd = new SqliteCommand(@"insert into users values (@login)", _connection);
                cmd.Parameters.AddWithValue("@login", login);
                cmd.ExecuteNonQuery();
                id = Convert.ToInt32(new SqliteCommand(@"select last_insert_rowid()", _connection)
                    .ExecuteScalar());
            } else
            {
                if (users.Contains(id))
                {
                    handler.Send(Encoding.Unicode.GetBytes(@"{'Error': 0, 'Text': 'User already logged in'}"));
                    return;
                }
            }

            users.Add(id);
            handler.Send(Encoding.Unicode.GetBytes(@"{'Id': " + id + "}"));
        }

        private enum Commands
        {
            Login, List, Result, Start, End, Logout
        }

        private class TypeTest
        {
            public int Id;
            public string Name;
        }

        private class TypeQuestion
        {
            public string Question;
            public List<string> Answers;
        }
        
        private class TypeEnd
        {
            public int Sid = -1;
            public IList<int> Answers;
        }
    }
}