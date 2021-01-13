using System;
using System.IO;
using System.Collections.Concurrent;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Security.Cryptography;
using System.Text.Json;

namespace NotificationSystemServer
{
    static class Storage
    {
        static string pathUsers = @"E:\Education\7Sem\Nets\Lab3\NotificationSystemServer\NotificationSystemServer\Users.txt";
        static string pathSubscribings = @"E:\Education\7Sem\Nets\Lab3\NotificationSystemServer\NotificationSystemServer\Subs.txt";
        static string pathEvents = @"E:\Education\7Sem\Nets\Lab3\NotificationSystemServer\NotificationSystemServer\Events.txt";


        public static void Init()
        {
            var file = File.ReadAllText(pathEvents);
            Data.events = JsonSerializer.Deserialize<ConcurrentBag<Event>>(file);
        }

        public static void SaveAll()
        {

        }

        public static bool ContainsUser(string data)
        {
            using (StreamReader sr = File.OpenText(pathUsers))
            {
                string line;
                while ((line = sr.ReadLine()) != null)
                {
                    if (ComputeSha256Hash(data) == line)
                    {
                        sr.Close();
                        return true;
                    }
                }

                sr.Close();
            }
            
            return false;
        }

        public static bool AddUser(string data)
        {
            var contains = ContainsUser(data);
            if (contains)
            {
                return false;
            }

            var sha = ComputeSha256Hash(data);

            using (StreamWriter sw = File.AppendText(pathUsers))
            {
                sw.WriteLine(sha);
                sw.Close();
            }

            return true;
        }


        public static string ComputeSha256Hash(string rawData)
        {
            using (SHA256 sha256Hash = SHA256.Create())
            {
                byte[] bytes = sha256Hash.ComputeHash(Encoding.ASCII.GetBytes(rawData));
                StringBuilder builder = new StringBuilder();
                return Encoding.ASCII.GetString(bytes, 0, bytes.Length);
                
            }
        }
    }
}
