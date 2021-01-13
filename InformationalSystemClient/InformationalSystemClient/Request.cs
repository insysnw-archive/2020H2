using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;


namespace InformationalSystemClient
{
    public static class Request
    {
        public static void Go(string path)
        {
            var state = Data.state;

            if (!Data.currentSections.Contains(path))
            {
                Console.WriteLine("There is no such section or subsection, try once more");
                return;
            }
            
            if (state.section == "")
            {
                state.section = path;
            }
            else if (state.subsection == "")
            {
                state.subsection = path;
            }
            else
            {
                Console.WriteLine("open name To open article");
                return;
            }
        

            Connection.Send(MsgPacker.PostOpenSection(state));
            Connection.OnReceive += Responce.Go;
        }

        public static void Back()
        {
            var state = Data.state;
            if (state.section == "")
            {
                Console.WriteLine("It's no place to go back");
            }
            else if (state.subsection != "")
            {
                state.subsection = "";
            }
            else if (state.section != "")
            {
                state.section = "";
            }

            Connection.Send(MsgPacker.GetPreviousSection());
            Connection.OnReceive += Responce.Back;
        }

        public static void FindByName(string name)
        {
            var t = new Title();
            t.title = name;
            Connection.Send(MsgPacker.PosGetArticlesByName(t));
            Connection.OnReceive += Responce.FindByName;
        }

        public static void FindByAuthor(string author)
        {
            var a = new Author();
            a.author = author;
            Connection.Send(MsgPacker.PosGetArticlesByAutor(a));
            Connection.OnReceive += Responce.FindByAuthor;
        }

        public static void CurrentSection()
        {
            Connection.Send(MsgPacker.GetCurrentSection());
            Connection.OnReceive += Responce.CurrentSection;
        }

        public static void CurrentArticles()
        {
            Connection.Send(MsgPacker.GetCurrentArticles());
            Connection.OnReceive += Responce.CurrentArticles;
        }

        public static void AddArticle()
        {
            Console.WriteLine("Write Section:");
            var section = Console.ReadLine();
            Console.WriteLine("Write Subsection");
            var subsection = Console.ReadLine();
            Console.WriteLine("Write Author");
            var a = Console.ReadLine();
            Console.WriteLine("Write Title");
            var t = Console.ReadLine();
            Console.WriteLine("Write Body");
            var b = Console.ReadLine();

            Article article = new Article();
            article.author = a;
            article.title = t;
            article.body = b;

            ArticleWithSection articleWithSection = new ArticleWithSection();
            articleWithSection.section = section;
            articleWithSection.subsection = subsection;
            articleWithSection.article = article;

            Connection.Send(MsgPacker.PostAddArticle(articleWithSection));
            Connection.OnReceive += Responce.AddArticle;

        }
    }
}
