using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Reflection;
using System.Text.Json;

namespace InformationalSystemClient
{
    public static class MsgUnpacker
    {
        public static int requestCode;

        public static string Unpack(string message)
        {
            var arr = message.Split('\n');
            var code = arr[0];
            var json = "";
            for (int i = 1; i < arr.Length; i++) {
                json += arr[i];
            }
            
            requestCode = int.Parse(code.Substring(0, 3));

            return json;
        }

        public static Article GetArticle(string message)
        {
            //var json = Unpack(message);
            return JsonSerializer.Deserialize<Article>(message);
        }

        public static List<string> GetSectionList(string message)
        {
            //var json = Unpack(message);
            return JsonSerializer.Deserialize<List<string>>(message);
        }


        public static List<Article> GetArticleList(string message)
        {
            //var json = Unpack(message);
            return JsonSerializer.Deserialize<List<Article>>(message);
        }
    }
}
