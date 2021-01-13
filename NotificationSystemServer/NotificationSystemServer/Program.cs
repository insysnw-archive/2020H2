using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace NotificationSystemServer
{
    class Program
    {
        static void Main(string[] args)
        {
            //Storage.Init();
            EventManager.StartManager();
            Connection.StartServer();
        }
    }
}
