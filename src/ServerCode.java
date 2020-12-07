import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;

public class Main {

    public static int PORT = 8000;
    public static int buffer = 255;
    public static LinkedList<ServerListener> serverList = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        String path;
        if (args.length == 0){
            path = "default";
        }else{
            path = args[0];
        }
        if ( !path.equals("default")){
            try{
                File file = new File(path);
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                PORT = Integer.parseInt(bufferedReader.readLine());
                buffer = Integer.parseInt(bufferedReader.readLine());
            }catch (FileNotFoundException e){
                System.out.println("Cannot find config file. Program will use default values");
            }
        }
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server starts successfully");
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerListener(socket));
                    System.out.println("socket was added");
                }
                catch (IOException e) {
                    System.out.println("socket was not created");
                    socket.close();
                }
            }
        }
    }
}

class ServerListener extends Thread {

    public Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public ServerListener(Socket socket) throws IOException {
        this.socket = socket;
        //DataInputStream ins = new DataInputStream(socket.getInputStream());
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();

    }
    @Override
    public void run() {
        String word;

        try {
            while (true) {
                word = in.readLine();
                for (ServerListener vr : Main.serverList) {
                    vr.send(word); // отправка сообщения всем клиентам
                }
            }
        } catch (IOException e) {
            try {
                closer();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     *
     * @param msg - передаваемая сырая строка
     * метод send получает на вход строку, которую кто-то отправил на сервер, разбивает её в соответствии с протоколом
     * на составляющие, добавляет к ним время по Гринвичу, чтобы вывести в читаемом виде в консоль сервера;
     * При этом к изначальной сырой строке просто добавляет время и отправляет клиентам, так как уже дело клиента - как
     * разбивать эту строку на составляющие.
     */
    private void send(String msg) {
        try {
            //имплементация протокола для получения читаемых данных из сырого получаемого источника msg
            //System.out.println("msg: "+ msg);
            int nameLength = msg.charAt(0);
            //System.out.println("nameLength before: " + nameLength);
            nameLength = msg.charAt(0) - 30; //получение длины имени
            //System.out.println("nameLength: " + nameLength);
            String name = msg.substring(1, nameLength+1); //откусываем имя из сырой строки
            //System.out.println("name: " + name);
            int msgLength = msg.charAt(nameLength+1) - 30; //получение длины строки пользовательского сообщения
            //System.out.println("msgLength: " + msgLength);
            String message = msg.substring(2+ nameLength); // откусываем само сообщение из сырой строки
            //System.out.println("message: "+ message);
            if (message.length() >= Main.buffer) {
                message = message.substring(0, Main.buffer - 4);
                message = message + "...";
            }
            //конец имплементации
            //получение времени
            Date time = new Date(); // получаем объект для хранения времени
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm"); //из года, месяца, дня итд оставляем только часы и минуты с помощью форматирования
            sdf.setTimeZone(TimeZone.getTimeZone("GMT")); //задаём timezone как время по Гринвичу
            String ti = sdf.format(time); //получаем время по гринвичу
            int timeLength = ti.length(); //получаем его длину, чтобы передать клиентам
            msg = (char)timeLength + ti + msg; //формируем сообщение с временем для передачи клиентам
            //еще одна деталь имплементации
            System.out.println("Server: <" + ti + "> [" + name + "] " + message + "\n");// формируем строку, которая будет видна в консоли сервера. Приводим в читаемый вид
            //timeLength+=30;
            nameLength+=30;
            msgLength+=30;
            //конец еще одной детали имплементации
            out.write( (char)timeLength+ti+(char)nameLength+name+(char)msgLength+message+ "\n"); // пишем в выходной поток и отправляем клиентам
            out.flush(); //очищаем поток
        } catch (IOException ignored) {

        } catch (StringIndexOutOfBoundsException sioobe){
            sioobe.printStackTrace();
            System.out.println("incorrect message, please try again");
        }
    }

    private void closer() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}