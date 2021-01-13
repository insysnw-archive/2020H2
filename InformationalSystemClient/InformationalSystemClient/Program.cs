using System;
using System.Text;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Windows.Forms;

namespace InformationalSystemClient
{
    class Program
    {
        static bool isHelping = true;

        static void CurrentDomain_ProcessExit(object sender, EventArgs e)
        {
            Connection.Disconnect();
        }

        static void Main(string[] args)
        {
            Connection.Connect();

            //Request.AddArticle();
            //Request.FindByName("Светлана");
            Request.CurrentSection();
            AppDomain.CurrentDomain.ProcessExit += new EventHandler(CurrentDomain_ProcessExit);

            while (true)
            {
                HelpMsg();
                var command = Console.ReadLine();
                CommandDetector(command);
            }

        }

        static void CommandDetector(string cmd)
        {
            var command = cmd.Split(' ')[0];
            var param = "";
            if(cmd.Split(' ').Length>1)
                param = cmd.Remove(0, command.Length + 1);

            if (command == "go")
            {
                
                Request.Go(param);
            }
            else if (command == "back")
            {
                Request.Back();
            }
            else if (command == "help")
            {
                isHelping = !isHelping;
            }
            else if (command == "finda")
            {
                Request.FindByAuthor(param);
            }
            else if (command == "findn" || command == "open")
            {
                Request.FindByName(param);
            }
            else if (command == "add")
            {
                Request.AddArticle();
            }
        }

        static void HelpMsg()
        {
            if(isHelping)
                Console.WriteLine("______________________________________________________" +
                                  "\n Write commands to do some actions. " +
                                  "\n help To hide/unhide help tips write " +
                                  "\n go <name> To choose section, subsction, article " +
                                  "\n back To go back by ierarhy" +
                                  "\n findn <name> To find concrete article by it's name" +
                                  "\n finda <autor> To find all article by autor" +
                                  "\n add To add own state" +
                                  "\n______________________________________________________");
        }

    }
}
