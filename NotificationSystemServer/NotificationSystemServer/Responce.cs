using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Text.Json;

namespace NotificationSystemServer
{
    static class Responce
    {
        public static void SendEvents(User user)
        {
            var json = JsonSerializer.Serialize(Data.events);
            Connection.Send(user.socket,200,json);
        }

        public static void SendToken(User user,string token)
        {
            
            Token t = new Token();
            t.token = token; //Convert.ToBase64String(Encoding.UTF8.GetBytes(user.token));
            //t.token = user.token;
            var json = JsonSerializer.Serialize(t);
            Connection.Send(user.socket,200, json);
        }

        public static void SendOK(User user)
        {
            Connection.Send(user.socket, 200, "");
        }

        public static void SendBad(User user)
        {
            Connection.Send(user.socket, 414, "");
        }

        public static void SendNotification(User user,Event e)
        {
            var json = JsonSerializer.Serialize(e);
            Connection.Send(user.notificationSocket, 200, json);
        }

    }
}
