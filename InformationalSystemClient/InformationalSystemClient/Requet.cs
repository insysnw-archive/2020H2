using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;


namespace InformationalSystemClient
{
    static class Requset
    {
        static string PackGet(string Uri)
        {
            string res = "GET " + Uri + "\n";
            return res;
        }

        public static string PackPost(string Uri,Object obj)
        {
            string res = "POST " + Uri + "\n";
            
            res += JsonSerializer.Serialize(obj);

            return res;
        }

        static string GetCurrentSection()
        {
            return PackGet("/current-section");
        }

        static string GetCurrentArticles()
        {
            return PackGet("/current-articles");
        }

        static string GetPreviousSection()
        {
            return PackGet("/previous-section");
        }

        public static string PostAddArticle(ArticleWithSection article)
        {
            return PackPost("/add-article",article);
        }

        static string PostOpenSection(Section section)
        {
            return PackPost("/open-section", section);
        }

        static string PosGetArticlesByAutor(Autor autor)
        {
            return PackPost("/get-articles-by-autor", autor);
        }

        static string PosGetArticlesByName(Title title)
        {
            return PackPost("/get-articles-by-name", title);
        }
    }
}
