using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;
using System.Text;


namespace InformationalSystemClient
{
    static class MsgPacker
    {
        
        static byte[] Pack(byte operation, Object obj)
        {
            
            var json = JsonSerializer.Serialize(obj);
            byte[] data = Encoding.UTF8.GetBytes(json);
            byte[] bodyLenght = BitConverter.GetBytes(Convert.ToInt16(data.Length));
            byte[] res = new byte[data.Length+3];
            res[0] = operation;
            res[1] = bodyLenght[0];
            res[2] = bodyLenght[1];
            for (int i = 0;i<data.Length;i++)
            {
                res[3 + i] = data[i];
            }
            Console.WriteLine("Json Lenght " + data.Length + " || lenght: " + bodyLenght.Length + " 0: " + bodyLenght[0] + " 1: " + bodyLenght[1]);
            return res;
        }

        static byte[] Pack(byte operation)
        {
            byte[] res = new byte[3];
            res[0] = operation;
            res[1] = 0;
            res[2] = 0;
            Console.WriteLine("Just pack");
            return res;
        }

        public static byte[] GetCurrentSection()
        {
            return Pack(0);
        }

        public static byte[] GetCurrentArticles()
        {
            return Pack(1);
        }

        public static byte[] PostOpenSection(State section)
        {
            return Pack(2, section);
        }

        public static byte[] GetPreviousSection()
        {
            return Pack(3);
        }

        public static byte[] PosGetArticlesByName(Title title)
        {
            return Pack(4, title);
        }

        public static byte[] PosGetArticlesByAutor(Author autor)
        {
            return Pack(5, autor);
        }

        public static byte[] PostAddArticle(ArticleWithSection article)
        {
            return Pack(6,article);
        }
        
    }
}
