using System;
using System.Collections.Concurrent;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Threading;

namespace NotificationSystemServer
{
    static class EventManager
    {
        static bool isWorking = false;
        static int nextEventTime;

        public static void StartManager()
        {
            
            isWorking = true;
            Thread conservationThread = new Thread(() => MainThread());
            conservationThread.Start();
        }


        private static void MainThread()
        {
            while (isWorking)
            {
                Thread.Sleep(5000);
                Console.WriteLine("Current min " + DateTimeOffset.Now.ToUnixTimeSeconds()/60 + " nextEventTime " + nextEventTime );
                if(DateTimeOffset.Now.ToUnixTimeSeconds() / 60 >= nextEventTime)
                    
                {
                    
                    //send to all subscribers
                    SendToAllSubscribers();
                    UpdateEvents();
                }
            }

        }

        private static void UpdateEvents()
        {
            if (Data.events.Count == 0)
                return;
            var ev = Data.GetEventWithMinTime();
            if (ev.period == 0)
            {
                Console.WriteLine("Delete event without period with id: " + ev.id);
                Data.RemoveFromEvents<Event>(ev.id);
            }
            else
                ev.time += ev.period;

            ReorderList();
        }

        private static void SendToAllSubscribers()
        {
            if (Data.events.Count == 0)
                return;
            var ev = Data.GetEventWithMinTime();
            ConcurrentBag<string> tokens;
            Data.subscribings.TryGetValue(ev.id, out tokens);

            foreach (User user in Data.users)
            {
                if (tokens.Contains(user.token) || ev.period == 0)
                {
                    Console.WriteLine("Send notification: " + ev.id + " to user: " + user.token);
                    Responce.SendNotification(user,ev);
                }
            }
            
        }

        public static void ReorderList()
        {
            if (Data.events.Count == 0)
                return;
            var ev = Data.GetEventWithMinTime();

            Console.WriteLine("Reorderi List and event with id: " + ev.id + " now first ");
            foreach (Event e in Data.events)
            {
                Console.WriteLine(e.id + " time " + e.time);
            }
            nextEventTime = ev.time;
        }

        
    }
}
