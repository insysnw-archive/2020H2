using System;
using System.Threading;
using System.Net;
using System.Net.Sockets;
using System.Text;

public class SynchronousSocketClient
{
    private static string name;
    private static Socket sender;

    public static bool StartClient()
    {
        try
        {
            // use port 11000 on the local computer.  
            IPHostEntry ipHostInfo = Dns.GetHostEntry(Dns.GetHostName());
            IPAddress ipAddress = ipHostInfo.AddressList[0];
            IPEndPoint remoteEP = new IPEndPoint(ipAddress, 11000);

            // Create a TCP/IP  socket.  
            sender = new Socket(ipAddress.AddressFamily,
               SocketType.Stream, ProtocolType.Tcp);
            sender.Blocking = true;
            try {
                // Connect the socket to the remote endpoint.
                sender.Connect(remoteEP);
                //sender.Shutdown(SocketShutdown.Both);
                //sender.Close();
            }
            catch (ArgumentNullException ane)
            {
                Console.WriteLine("ArgumentNullException : {0}", ane.ToString());
            }
            catch (SocketException se)
            {
                Console.WriteLine("Connection Error");
                Console.ReadLine();
                return false;
            }
            catch (Exception e)
            {
                Console.WriteLine("Unexpected exception : {0}", e.ToString());
            }
        }
        catch (Exception e)
        {
            Console.WriteLine("Connection Error");
            Console.ReadLine();
            return false;
        }

        return true;
    }

    private static void SendMessage(string message)
    {
        byte[] bytes = new byte[1024];
        byte[] msg = Encoding.ASCII.GetBytes($"< {DateTime.Now.ToString("hh:mm")} > [{name}] {message}<EOF>");
  
        int bytesSent = sender.Send(msg);
    } 

    private static void StartReceiving()
    {
        ThreadStart rec = new ThreadStart(Receiving);
        Thread recThread = new Thread(rec);
        recThread.Start();
    }

    private static void Receiving()
    {
        while (true)
        {
            try
            {
                Thread.Sleep(100);
                byte[] bytes = new byte[1024];
                int bytesRec = sender.Receive(bytes);
                if (bytesRec > 0)
                {
                    var trimArr = new char[] { '<', 'E', 'O', 'F', '>' };
                    var msg = Encoding.ASCII.GetString(bytes, 0, bytesRec).TrimEnd(trimArr);
                    Console.WriteLine(msg);
                }
            }
            catch (SocketException e)
            {
                Console.WriteLine("Connection lost");
                Console.ReadLine();
            }
            
        }
    }

    private static void InitClient()
    {
        Console.WriteLine("Hello. Enter your name");
        name = Console.ReadLine();
    }

    public static int Main(String[] args)
    {
        InitClient();
        var connected = StartClient();
        if (!connected) return 0;

        StartReceiving();

        while (true)
        {
            var msg = Console.ReadLine();
            SendMessage(msg);
        }
        return 0;
    }
}