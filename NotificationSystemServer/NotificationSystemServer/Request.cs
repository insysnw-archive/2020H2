using System;
using System.Text;
using System.Text.Json;
using System.Collections.Concurrent;
using System.Linq;
using System.Text;
using System.Threading.Tasks;


namespace NotificationSystemServer
{
    static class Request
    {
        public static void ProcessRequest(User user, byte requestCode,string token,string json)
        {
            //var token = user.token;

            if(token == "" && (requestCode != 5 || requestCode != 6))
            {
                //todo reject
            }

            switch (requestCode)
            {
                case 0:
                    GetEventList(user);
                    break;
                case 1:
                    AddEvent(user,json);
                    break;
                case 2:
                    DeleteEvent(user,json);
                    break;
                case 3:
                    Subscribe(user,json,token);
                    break;
                case 4:
                    Unsubscribe(user,json,token);
                    break;
                case 5:
                    AddNotificationSocket(user,token);
                    break;
                case 6:
                    Register(user, json);
                    break;
                case 7:
                    
                    break;
            }
        }

        static void GetEventList(User user)
        {
            Responce.SendEvents(user);
        }

        static void AddEvent(User user, string json)
        {
            var e = JsonSerializer.Deserialize<Event>(json);
            Data.AddToEvents(e);
            EventManager.ReorderList();
            Responce.SendOK(user);
        }

        static void DeleteEvent(User user, string json)
        {
            var id = JsonSerializer.Deserialize<ID>(json);
            if(!Data.EventsContainsId(id.id))
            {
                Responce.SendBad(user);
                return;
            }
            Data.RemoveFromEvents<Event>(id.id);
            EventManager.ReorderList();
            Responce.SendOK(user);
        }
        

        static void Subscribe(User user,string json,string token)
        {
            var id = JsonSerializer.Deserialize<ID>(json);
            ConcurrentBag<string> list;
            var res = Data.subscribings.TryGetValue(id.id, out list);

            if (!res)
            {
                Responce.SendBad(user);
                Console.WriteLine("EROR User: " + token + " unsubscribed to: " + id);
            }

            if (!list.Contains(token) && !list.IsEmpty)
            {
                list.Add(token);
                Responce.SendOK(user);
                Console.WriteLine("User: " + token + " subscribed to: " + id);
            }
            else
            {
                Console.WriteLine("EROR User: " + token + " subscribed to: " + id);
                Responce.SendBad(user);
            }
        }

        static void Unsubscribe(User user, string json,string token)
        {
            var id = JsonSerializer.Deserialize<ID>(json);
            ConcurrentBag<string> list;
            var res = Data.subscribings.TryGetValue(id.id, out list);

            if (!res)
            {
                Responce.SendBad(user);
                Console.WriteLine("EROR User: " + token + " unsubscribed to: " + id);
            }

            if (list.Contains(token) && !list.IsEmpty)
            {
                //foreach(ConcurrentBag<string> bag Data.subscribings. = 
                Data.RemoveFromUsers<string>(list,token);
                Responce.SendOK(user);

                Console.WriteLine("User: " + token + " unsubscribed to: " + id);
            }
            else
            {

                Responce.SendBad(user);
                Console.WriteLine("EROR User: " + token + " unsubscribed to: " + id);
            }

        }

        static void AddNotificationSocket(User user, string token)
        {
            Console.WriteLine("Adding nitification socket to user with token " + token);
            Console.WriteLine("Users before " + Data.users.Count );

            foreach (User u in Data.users)
            {
                //var t = Convert.ToBase64String(Encoding.UTF8.GetBytes(token));
                if (u.token.Equals(token) && !u.socket.Equals(user.socket))
                {
                    u.notificationSocket = user.socket;
                }

            }
            Data.RemoveFromUsers<User>(user);
            var us = Data.users.First();
            Responce.SendOK(user);
        }

        static void Register(User user,string json)
        {
            var personal = JsonSerializer.Deserialize<PersonalData>(json);
            if (!Storage.ContainsUser(personal.credentials))
            {
                Storage.AddUser(personal.credentials);
                Console.WriteLine("New user registered");
            }
            else
            {
                Console.WriteLine("User autorized");
            }

            var sha = Storage.ComputeSha256Hash(personal.credentials);
            //var token = Convert.FromBase64String(sha);
            var token = Convert.ToBase64String(Encoding.UTF8.GetBytes(sha));
            user.token = token;

            var t = Convert.FromBase64String(token);

            //TODO responce token;
            Responce.SendToken(user,token);
        }
    }

    
}
