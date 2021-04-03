using System.Collections.Generic;

namespace Voting
{
    internal class Voting
    {
        public string Name { get; }

        public List<(string Variant, int Votes)> VariantVote { get; }

        public bool Status { get; set; }

        public int MaxSize { get; }

        public int MaxVotes { get; }

        public Voting(string name, int maxSize, int maxVotes)
        {
            Name = name;
            VariantVote = new List<(string, int)>();
            MaxSize = maxSize;
            MaxVotes = maxVotes;
            Status = false;
        }
    }
}
