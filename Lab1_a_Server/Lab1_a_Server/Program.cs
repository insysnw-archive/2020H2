using System;
using System.Threading;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;

public class SynchronousSocketListener
{
    private static Socket listener;
    private static List<Socket> clients;
  
    public static string data = null;

    public static void StartServer()
    {
        clients = new List<Socket>();  
        IPHostEntry ipHostInfo = Dns.GetHostEntry(Dns.GetHostName());
        IPAddress ipAddress = ipHostInfo.AddressList[0];
        IPEndPoint localEndPoint = new IPEndPoint(ipAddress, 11000);

        // Create a TCP/IP socket.  
        listener = new Socket(ipAddress.AddressFamily,
            SocketType.Stream, ProtocolType.Tcp);

        // Bind the socket to the local endpoint and
        // listen for incoming connections.  
        try
        {
            listener.Bind(localEndPoint);
            listener.Listen(10);
        }
        catch (Exception e)
        {
            Console.WriteLine();
        }
    }

    private static void AppendingConnections()
    {

        byte[] msg = Encoding.ASCII.GetBytes("One more joined this chat");
        // Start listening for connections.  
        while (true)
        {
            Console.WriteLine("Waiting for connection...");
            // Program is suspended while waiting for an incoming connection.  
            Socket handler = listener.Accept();
            
            clients.Add(handler);
            Console.WriteLine("Count of clients: " + clients.Count);

            foreach (Socket socket in clients)
            {
                socket.Send(msg);
            }

            StartReciving(handler);
        }
    }

    private static void ReceiveMessage(Socket client)
    {
        byte[] bytes = new Byte[1024];
        

        // An incoming connection needs to be processed.  
        while (true)
        {
            try
            {

                int bytesRec = client.Receive(bytes);
                data += Encoding.ASCII.GetString(bytes, 0, bytesRec);
                if (data.IndexOf("<EOF>") > -1)
                {

                    foreach (Socket socket in clients)
                    {
                        byte[] msg = Encoding.ASCII.GetBytes(data);

                        if (socket != client)
                            socket.Send(msg);
                    }
                    Console.WriteLine(data);
                    data = null;
                }
            }
            catch(SocketException e)
            {
                byte[] msg = Encoding.ASCII.GetBytes("Client leaved this chat");
                clients.Remove(client);
                foreach (Socket socket in clients)
                {
                    socket.Send(msg);
                }
                return;
            }
        }
    }

    private static void StartReciving(Socket client)
    {
        Thread conservationThread = new Thread(() => ReceiveMessage(client));
        conservationThread.Start();

    }

    public static int Main(String[] args)
    {
        StartServer();
        AppendingConnections();
        
        return 0;
    }
}
