using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace InformationalSystemClient
{
    public static class UI
    {
        public static void DisplaySections()
        {
            var list = Data.currentSections;
            var where = "Sections";

            if (Data.state.subsection != "")
                where = "Subsections in " + Data.state.subsection;
            

            Console.WriteLine(where);
            foreach(string sec in list)
            {
                Console.WriteLine(sec);
            }
        }

        public static void DisplayArticles(List<Article> articles)
        {
            foreach (Article art in articles)
            {
                Console.WriteLine("Author: " + art.author + " Title: " + art.title);
            }
        }

        public static void DisplayArticle(Article article)
        {
            Console.WriteLine("Title: " + article.title);
            Console.WriteLine("Autor: " + article.author);
            Console.WriteLine(article.body);
        }
    }
}
