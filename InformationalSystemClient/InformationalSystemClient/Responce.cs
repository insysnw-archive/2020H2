using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Reflection;
using System.Text.Json;

namespace InformationalSystemClient
{
    class Responce
    {


        public static void Go(string responce)
        {
            //MsgUnpacker.Unpack(responce);
            if (MsgUnpacker.requestCode>299)
            {
               // if (Data.state.subsection != "")
               //     Data.state.subsection = "";
               // else
               //     Data.state.section = "";

                Console.WriteLine("Error " + MsgUnpacker.requestCode);
            }
            else
            {

            }
            Connection.OnReceive -= Go;

            CurrentStateFromServer();

        }

        public static void Back(string responce)
        {
            Connection.OnReceive -= Back;
            Request.CurrentSection();
        }

        public static void FindByName(string responce)
        {
            Connection.OnReceive -= FindByName;
            if (MsgUnpacker.requestCode > 299)
            {
                Console.WriteLine("Not found, something went wrtong");
            }
            else
            {

                var article = MsgUnpacker.GetArticle(responce);
                UI.DisplayArticle(article);
            }
            
        }

        public static void FindByAuthor(string responce)
        {
            Connection.OnReceive -= FindByAuthor;
            
            if (MsgUnpacker.requestCode > 299)
            {
                //MsgUnpacker.Unpack(responce);
                Console.WriteLine("Not found, something went wrtong");
            }
            else
            {
                var articles = MsgUnpacker.GetArticleList(responce);
                UI.DisplayArticles(articles);
            }
        }

        public static void AddArticle(string responce)
        {
            Connection.OnReceive -= AddArticle;
           // MsgUnpacker.Unpack(responce);
            if(MsgUnpacker.requestCode > 299)
            {
                
                Console.WriteLine("Article was not added, something went wrtong");
            }
            else
            {
                Console.WriteLine("Article added successfully");
            }

            CurrentStateFromServer();
        }

        public static void CurrentSection(string responce)
        {
            Connection.OnReceive -= CurrentSection;
            Data.currentSections = MsgUnpacker.GetSectionList(responce);
            UI.DisplaySections();
        }

        public static void CurrentArticles(string responce)
        {
            var articles = MsgUnpacker.GetArticleList(responce);
            UI.DisplayArticles(articles);

            Connection.OnReceive -= CurrentArticles;
        }


        private static void CurrentStateFromServer()
        {
            Console.WriteLine("ASK server state");
            var state = Data.state;
            if (state.section == "")
            {
                Request.CurrentSection();
            }
            else if (state.subsection == "")
            {
                Request.CurrentSection();
            }
            else
            {
                Request.CurrentArticles();
            }
        }
    }
}
