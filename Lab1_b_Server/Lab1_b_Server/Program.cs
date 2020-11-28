using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

public class ClientObject
{
    public const int BufferSize = 1024;
    public byte[] buffer = new byte[BufferSize];
    public StringBuilder sb = new StringBuilder();
    public Socket workSocket = null;
    public ManualResetEvent progressEvent = new ManualResetEvent(false);
}

public class AsynchronousSocketListener
{
    public static List<ClientObject> clients = new List<ClientObject>();
    public static ManualResetEvent connectorEvent = new ManualResetEvent(false);


    public AsynchronousSocketListener()
    {
    }

    public static void StartListening()
    {  
        IPHostEntry ipHostInfo = Dns.GetHostEntry(Dns.GetHostName());
        IPAddress ipAddress = ipHostInfo.AddressList[0];
        IPEndPoint localEndPoint = new IPEndPoint(ipAddress, 11000);
        
        Socket listener = new Socket(ipAddress.AddressFamily,
            SocketType.Stream, ProtocolType.Tcp);
        listener.Blocking = false;
 
        try
        {
            listener.Bind(localEndPoint);
            listener.Listen(100);

            while (true)
            {
                connectorEvent.Reset();
  
                Console.WriteLine("Waiting for a connection...");
                listener.BeginAccept(
                    new AsyncCallback(AcceptCallback),
                    listener);
                
                connectorEvent.WaitOne();

            }

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }

        Console.WriteLine("\nPress ENTER to continue...");
        Console.Read();

    }

    public static void AcceptCallback(IAsyncResult ar)
    {
        connectorEvent.Set();
  
        Socket listener = (Socket)ar.AsyncState;
        Socket handler = listener.EndAccept(ar);
        
        ClientObject state = new ClientObject();
        state.workSocket = handler;
        state.workSocket.Blocking = false;
        //state.progressEvent.WaitOne();
        clients.Add(state);
        Console.WriteLine("Connected clients: " + clients.Count);
        handler.BeginReceive(state.buffer, 0, ClientObject.BufferSize, 0,
            new AsyncCallback(ReadCallback), state);
    }

    public static void ReadCallback(IAsyncResult ar)
    {
        String content = String.Empty;
        
        ClientObject state = (ClientObject)ar.AsyncState;
        Socket handler = state.workSocket;
        state.sb = new StringBuilder("");
        int bytesRead = handler.EndReceive(ar);

        if (bytesRead > 0)
        { 
            state.sb.Append(Encoding.ASCII.GetString(
                state.buffer, 0, bytesRead));
            
            content = state.sb.ToString();
            if (content.IndexOf("<EOF>") > -1)
            {
                Console.WriteLine("Read {0} bytes from socket. \n Data : {1}",
                    content.Length, content);
                SendAll(handler,content);
            }
            else
            {
                handler.BeginReceive(state.buffer, 0, ClientObject.BufferSize, 0,
                new AsyncCallback(ReadCallback), state);
            }
        }

        handler.BeginReceive(state.buffer, 0, ClientObject.BufferSize, 0,
            new AsyncCallback(ReadCallback), state);
    }

    private static void SendAll(Socket except, String data)
    { 
        byte[] byteData = Encoding.ASCII.GetBytes(data);
        Console.WriteLine("Sending data: " + data);
        foreach (ClientObject client in clients)
        {
            var handler = client.workSocket;
            if (except == handler)
                continue;
            handler.BeginSend(byteData, 0, byteData.Length, 0,
                new AsyncCallback(SendCallback), handler);
        }
        
    }

    private static void SendCallback(IAsyncResult ar)
    {
        try
        {
            Socket handler = (Socket)ar.AsyncState;
            
            int bytesSent = handler.EndSend(ar);
            //Console.WriteLine("Sent {0} bytes to client.", bytesSent);

            //handler.Shutdown(SocketShutdown.Both);
            //handler.Close();

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }

    public static int Main(String[] args)
    {
        StartListening();
        return 0;
    }
}
