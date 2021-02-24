using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SnmpClient
{

    public class Snmp
    {
        public enum MessageType
        {
            Get = 0, GetNext = 1, GetResp = 2, Set = 3
        }
        
        // _data = pdut switch
        // {
        //     MessageType.Get => _data.Append((byte) 0xA0).ToList(),
        //     MessageType.GetNext => _data.Append((byte) 0xA1).ToList(),
        //     MessageType.GetResp => _data.Append((byte) 0xA2).ToList(),
        //     MessageType.Set => _data.Append((byte) 0xA3).ToList(),
        //     _ => throw new ArgumentOutOfRangeException(nameof(pdut), pdut, null)
        // };

        public static byte[] GenGetPacket(string com, string mib, ref uint rid)
        {
            List<byte> _data;
            var comm = mib.Split(".");
            var id = new []{(byte)(40 * Convert.ToByte(comm[0]) + Convert.ToByte(comm[1]))};
            for (var i = 2; i < comm.Length; i++)
            {
                var tmp = Convert.ToInt16(comm[i]);
                id = tmp > 127 ? id.Append((byte) (tmp >> 7)).Append((byte) (tmp % 128)).ToArray() : id.Append((byte) tmp).ToArray();
            }

            _data = new List<byte>()
                .Concat(new byte[] {0x30, (byte)(29 + com.Length + id.Length - 2)}) //Head SEQUENCE
                .Concat(new byte[] {2, 1, 1}) //INTEGER version 2
                .Append((byte) 4).Append((byte) com.Length) // OCTET STRING community   
                .Concat(Encoding.ASCII.GetBytes(com)).ToList();
            rid = (uint) new Random().Next();
            _data = _data.Append((byte)0xA0).Append((byte) (20 + id.Length)).Append((byte) 2).Append((byte) 4)
                .Concat(BitConverter.GetBytes(rid)) // INTEGER packet id = Random unit
                .Concat(new byte[] {2, 1, 0}) // INTEGER status
                .Concat(new byte[] {2, 1, 0}) // INTEGER error index
                .Concat(new byte[] {0x30, (byte)(6 + id.Length)}) // SEQUENCE variable binding
                .ToList();
            _data = _data.Concat(new byte[] {0x30, (byte) (6 + id.Length - 2)}).ToList(); // SEQUENCE first variable
            _data = _data.Concat(new byte[] {0x6, (byte) id.Length}).ToList(); // MIB struct
            _data = _data.Concat(id).ToList();
            _data = _data.Concat(new byte[] {0x5, 0}).ToList();
            return _data.ToArray();
        }

        public static byte[] GenSetPacket(string com, string mib, byte[] val, ref uint rid)
        {
            List<byte> _data;
            var comm = mib.Split(".");
            var id = new []{(byte)(40 * Convert.ToByte(comm[0]) + Convert.ToByte(comm[1]))};
            for (var i = 2; i < comm.Length; i++)
            {
                var tmp = Convert.ToInt16(comm[i]);
                id = tmp > 127 ? id.Append((byte) (tmp >> 7)).Append((byte) (tmp % 128)).ToArray() : id.Append((byte) tmp).ToArray();
            }

            _data = new List<byte>()
                .Concat(new byte[] {0x30, (byte)(29 + com.Length + id.Length  + val.Length - 4)}) //Head SEQUENCE
                .Concat(new byte[] {2, 1, 1}) //INTEGER version 2
                .Append((byte) 4).Append((byte) com.Length) // OCTET STRING community   
                .Concat(Encoding.ASCII.GetBytes(com)).ToList();
            rid = (uint) new Random().Next();
            _data = _data.Append((byte)0xa3).Append((byte) (20 + id.Length + val.Length - 2)).Append((byte) 2).Append((byte) 4)
                .Concat(BitConverter.GetBytes(rid)) // INTEGER packet id = Random unit
                .Concat(new byte[] {2, 1, 0}) // INTEGER status
                .Concat(new byte[] {2, 1, 0}) // INTEGER error index
                .Concat(new byte[] {0x30, (byte)(6 + id.Length + val.Length - 2)}) // SEQUENCE variable binding
                .ToList();
            _data = _data.Concat(new byte[] {0x30, (byte) (6 + id.Length + val.Length - 4)}).ToList(); // SEQUENCE first variable
            _data = _data.Concat(new byte[] {0x6, (byte) id.Length}).ToList(); // MIB struct
            _data = _data.Concat(id).Concat(val).ToList();
            return _data.ToArray();
        }
    }
}