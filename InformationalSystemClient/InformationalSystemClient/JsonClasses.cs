using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace InformationalSystemClient
{
    public class ArticleWithSection
    {
        public string section { get; set; }
        public string subsection { get; set; }
        public Article article { get; set; }
    }

    public class Article
    {
        public string author { get; set; }
        public string title { get; set; }
        public string body { get; set; }
    }

    public class ArticleWithDiscription
    {
        public string autor { get; set; }
        public string title { get; set; }
        public string discription { get; set; }
    }

    public class Author
    {
        public string author { get; set; }
    }

    public class Title
    {
        public string title { get; set; }
    }

    public class State
    {
        public string section { get; set; }
        public string subsection { get; set; }

        public State()
        {
            section = "";
            subsection = "";
        }
    }

    public class Section
    {
        public string section { get; set; }
    }

    public static class Data
    {
        public static State state = new State();
        public static List<string> currentSections = new List<string>();
        
    }
}
