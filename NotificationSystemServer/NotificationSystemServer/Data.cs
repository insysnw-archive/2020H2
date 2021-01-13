using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.Collections.Concurrent;

namespace NotificationSystemServer
{
    public static class Data
    {
        public static ConcurrentBag<User> users = new ConcurrentBag<User>();
        public static ConcurrentBag<Event> events = new ConcurrentBag<Event>();
        public static ConcurrentDictionary<int, ConcurrentBag<string>> subscribings = new ConcurrentDictionary<int, ConcurrentBag<string>>();

        private static int globalId = 0;
        
        public static bool EventsContainsId(int id)
        {
            foreach (Event e in events)
            {
                if (e.id == id)
                    return true;
            }

            return false;
        }

        public static Event GetEventWithMinTime()
        {
            var ev = events.First<Event>(); 
            foreach (Event e in events)
            {
                if (e.time < ev.time)
                {
                    ev = e;
                }
            }
            return ev;
        }

        public static void AddToEvents(Event e)
        {
            globalId++;

            e.id = globalId;
            events.Add(e);
            subscribings.TryAdd(globalId, new ConcurrentBag<string>());
        }

        public static void RemoveFromEvents<T>(int id)
        {
            Console.WriteLine("Removing from events " + events.Count);
            ConcurrentBag<Event> list = events;
            events = new ConcurrentBag<Event>();

            foreach (Event e in list)
            {
                if (e.id != id)
                    events.Add(e);
            }
            Console.WriteLine("After removing from events " + events.Count);
        }

        public static void RemoveFromUsers<T>(ConcurrentBag<string> bag, string item)
        {
            Console.WriteLine("Removing from users " + bag.Count);
            while (bag.Count > 0)
            {
                string result;
                bag.TryTake(out result);

                if (result.Equals(item))
                {
                    break;
                }

                bag.Add(result);
            }
            Console.WriteLine("After removing from users " + bag.Count);
        }

        public static void RemoveFromUsers<T>(User item)
        {
            ConcurrentBag<User> list = users;
            users = new ConcurrentBag<User>();

            foreach (User u in list)
            {
                if(u != item)
                    users.Add(u);
            }
        }

    }

    public class ID
    {
        public int id { get; set; }
    }

    public class User
    {
        public string token { get; set; }
        public Socket socket { get; set; }
        public Socket notificationSocket { get; set; }
    }

    public class Event
    {
        public int id { get; set; }
        public string name { get; set; }
        public int time { get; set; }
        public int period { get; set; }
        public string place { get; set; }
        public string description { get; set; }
        public string organizer { get; set; }
    }

    public class PersonalData
    {
        public string credentials { get; set; }
        
        //(формат "login:password" в base64)
    }

    public class Token
    {
        public string token { get; set; }
    }
}
