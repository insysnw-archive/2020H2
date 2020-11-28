using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Text;
 
public class StateObject
{
    public Socket workSocket = null;
    public const int BufferSize = 256;
    public byte[] buffer = new byte[BufferSize];
    public StringBuilder sb = new StringBuilder();
}

public class AsynchronousClient
{
    private const int port = 11000;
    private static string name;
    static Socket client;
    
    private static ManualResetEvent connectDone =
        new ManualResetEvent(false);
    private static ManualResetEvent sendDone =
        new ManualResetEvent(false);
    private static ManualResetEvent receiveDone =
        new ManualResetEvent(false);
    
    private static String response = String.Empty;

    private static void StartClient()
    {
        try
        { 
            IPHostEntry ipHostInfo = Dns.GetHostEntry(Dns.GetHostName());
            IPAddress ipAddress = ipHostInfo.AddressList[0];
            IPEndPoint remoteEP = new IPEndPoint(ipAddress, port);
            
            client = new Socket(ipAddress.AddressFamily,
                SocketType.Stream, ProtocolType.Tcp);
            client.Blocking = false;

            client.BeginConnect(remoteEP,
                new AsyncCallback(ConnectCallback), client);
            connectDone.WaitOne();
            
            Thread receiveThread = new Thread(() => Receive());
            receiveThread.Start();
            
            // Release the socket.  
            //Console.ReadLine();
            //client.Shutdown(SocketShutdown.Both);
            //client.Close();

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }

    private static void ConnectCallback(IAsyncResult ar)
    {
        try
        {
           // Socket client = (Socket)ar.AsyncState;
           
            client.EndConnect(ar);

            Console.WriteLine("Socket connected to {0}",
                client.RemoteEndPoint.ToString());
 
            connectDone.Set();
        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }

    private static void Receive()
    {
        try
        { 
            StateObject state = new StateObject();
            state.workSocket = client;
  
            client.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0,
                new AsyncCallback(ReceiveCallback), state);
        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }

    private static void ReceiveCallback(IAsyncResult ar)
    {
        try
        { 
            StateObject state = (StateObject)ar.AsyncState;
            //Socket client = state.workSocket;
            
            int bytesRead = client.EndReceive(ar);

            if (bytesRead > 0)
            { 
                state.sb.Append(Encoding.ASCII.GetString(
                    state.buffer, 0, bytesRead));
                
                var content = state.sb.ToString();
                if (content.IndexOf("<EOF>") > -1)
                {
                    var trimArr = new char[] { '<', 'E', 'O', 'F', '>' };
                    var msg = content.TrimEnd(trimArr);
                    Console.WriteLine(msg);

                    receiveDone.Set();
                }
                else
                {
                    client.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0,
                    new AsyncCallback(ReceiveCallback), state);
                }
            }
            

            Receive();
            //receiveDone.WaitOne();
        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
    }

    private static void Send( String data)
    {
        byte[] byteData = Encoding.ASCII.GetBytes(data);
        
        client.BeginSend(byteData, 0, byteData.Length, 0,
            new AsyncCallback(SendCallback), client);
    }

    private static void SendCallback(IAsyncResult ar)
    {
        try
        {
            //Socket client = (Socket)ar.AsyncState;
            int bytesSent = client.EndSend(ar);
            
            sendDone.Set();
        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
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
        StartClient();
        while (true)
        {
            var msg = Console.ReadLine();
            Send($"< {DateTime.Now.ToString("hh:mm")} > [{name}] {msg}<EOF>");
            sendDone.WaitOne();
        }
        return 0;
    }
}
