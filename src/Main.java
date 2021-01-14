import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class Main {

    public static final int PORT = 8000;
    public static final String IP = "25.103.130.225";
    public static String EMAIL;


    public static void main(String[] args) {
        argsParser(args);
        new ClientListener(IP, PORT, EMAIL);

    }

    private static void argsParser(String[] args){
        if(args.length == 0){
            System.out.println("No email given. Please enter correct email");
            EMAIL = "guest";
        } else {
            EMAIL = args[0];
        }
    }

}


class ClientListener{
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private BufferedReader userInp;
    private String email;

    public ClientListener(String ip, int port, String email){
        try {
            Socket socket = new Socket(ip, port);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            userInp = new BufferedReader(new InputStreamReader(System.in));
            this.email = email;
            new Write().start();

        } catch (IOException e) {
            System.out.println("Can`t reach the server. Check your internet connection or restart when server becomes online");
            closer();
        }

    }

    private void closer(){
        try {
            System.out.println("disconnected by server");
            in.close();
            out.close();
            userInp.close();
            socket.close();
        } catch (IOException e){
            System.out.println("Critical error with closing resources. Please, restart");
        }
    }

    private class Write extends Thread {
        B b = new B();
        @Override
        public void run(){
            byte[] answerBuffer = new byte[1];
            String userWord;
            try{
                byte[] auth = authorization();
                //System.out.println(b.arrToStr(auth));
                out.write(auth);
                System.out.println("wait for authorization");
                in.read(answerBuffer);
                if (answerBuffer[0] == (byte)0x81){
                    System.out.println("correct authorization");
                }else if(answerBuffer[0] == (byte)0xc1){
                    System.out.println("failed authorization. Restart with correct email");
                }else{
                    System.out.println("Strange situation. Unexpected answer: " + answerBuffer[0]);
                }
            }catch (IOException e){
                System.out.println("exception in authorization");
                e.printStackTrace();
            }
            while (true){
                try{
                    userWord = userInp.readLine();
                    out.write(commandLineParser(userWord));
                    in.read(answerBuffer);
                    if(answerBuffer[0] == (byte)0x81){
                        System.out.println("Correct");
                    }else if(answerBuffer[0] == (byte)0xc2){
                        System.out.println("error with sending message");
                    }else if(answerBuffer[0] == (byte)0xc3){
                        System.out.println("error with deleting message");
                    }else if(answerBuffer[0] == (byte)0xc4){
                        System.out.println("message don`t exist");
                    } else if(answerBuffer[0] == (byte)0x82){
                        System.out.println("checking box state");
                        boxState();
                    }else if(answerBuffer[0] == (byte)0x83){
                        System.out.println("reading message");
                        readingMessage();
                    }else{
                        System.out.println("unexpected output: " + answerBuffer[0]);
                    }

                }catch (IOException e){
                    e.printStackTrace();
                    closer();
                }
            }


        }

        private void readingMessage() throws IOException{
            byte[] senderBlockLength = new byte[2];
            in.read(senderBlockLength);
            int senderLength = b.bToI(senderBlockLength);
            byte[] sender = new byte[senderLength];
            in.read(sender);
            byte[] themeBlockLength = new byte[2];
            in.read(themeBlockLength);
            int themeLength = b.bToI(themeBlockLength);
            byte[] theme = new byte[themeLength];
            in.read(theme);
            byte[] messageBlockLength = new byte[2];
            in.read(messageBlockLength);
            int messageLength = b.bToI(messageBlockLength);
            byte[] message = new byte[messageLength];
            in.read(message);
            System.out.println(b.bToS(sender) + "\n");
            System.out.println(b.bToS(theme) + "\n");
            System.out.println(b.bToS(message) + "\n");
        }

        private void boxState() throws IOException{
            byte[] buf = new byte[2];
            in.read(buf);
            //System.out.println("байты сообщения " + b.arrToStr(buf));
            int messagesCount = b.bToI(buf);
            //System.out.println("количество сообщений "+ messagesCount);
            if (messagesCount == 0) System.out.println("0 messages");
            for (int i = 0; i < messagesCount; i++){
                //System.out.println("вошел в цикл");
                byte[] id = new byte[4];
                in.read(id);
                byte[] senderBlockLength = new byte[2];
                in.read(senderBlockLength);
                int senderLength = b.bToI(senderBlockLength);
                byte[] sender = new byte[senderLength];
                in.read(sender);
                byte[] themeBlockLength = new byte[2];
                in.read(themeBlockLength);
                int themeLength = b.bToI(themeBlockLength);
                byte[] theme = new byte[themeLength];
                in.read(theme);
                int vid = b.bToI4(id);
                //System.out.println("вывожу ID " + vid);
                String senderString = b.bToS(sender);
                //System.out.println("вывожу отправителя " + senderString);
                String themeString = b.bToS(theme);
                //System.out.println("вывожу тему " + themeString);
                System.out.println(vid + " " + senderString + " " + themeString + "\n");
            }


        }

        private byte[] commandLineParser(String command){
            String[] words = command.split(":");
            switch (words[0].trim()){
                case "send": return sendMessage(words);
                case "delete": return deleteMessage(words);
                case "get": return getMessage(words);
                case "ls":
                default: return getMessages();
            }
        }

        private byte[] authorization(){
            byte[] authorisationCode = {0x01};
            byte[] emailBytes = b.sToB(email);
            byte[] emailBlock = getBlock(emailBytes);
            return b.sum(authorisationCode, emailBlock);
        }

        private byte[] getMessages(){
            return new byte[]{0x02};
        }

        private byte[] deleteMessage(String[] words){
            int id = Integer.parseInt(words[1].trim());
            byte[] delCode = {0x04};
            return b.sum(delCode, b.iToB(id));
        }

        private byte[] sendMessage(String[] words){
            byte[] sendCode = {0x03};
            byte[] address = getBlock(b.sToB(words[1].trim()));
            byte[] theme = getBlock(b.sToB(words[2].trim()));
            byte[] message = getBlock(b.sToB(getTextMessage(words)));
            byte[] result = b.sum(sendCode, address);
            result = b.sum(result, theme);
            System.out.println("sending message "+ b.arrToStr(b.sum(result, message)));
            return b.sum(result, message);
        }

        private byte[] getMessage(String[] words){
            int id = Integer.parseInt(words[1].trim());
            byte[] getCode = {0x05};
            return b.sum(getCode, b.iToB(id));
        }

        private byte[] getBlock(byte[] message){
            //System.out.println(message.length + " + " + b.arrToStr(message));
            //System.out.println(b.arrToStr(b.iToB2(message.length)));

            return b.sum(b.iToB2(message.length), message);
        }

        private String getTextMessage(String[] words){
            StringBuilder res = new StringBuilder();
            for (int i = 3; i < words.length; i++){
                res.append(words[i]);
            }
            return res.toString();
        }

    }


}

class B {

    public byte[] sToB(String str) {
        return str.getBytes(StandardCharsets.US_ASCII);
    }

    public String bToS(byte[] b) {
        return new String(b, StandardCharsets.US_ASCII);
    }

    public byte[] sum(byte[] a, byte[] b) {
        byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public int bToI4(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public int bToI(byte[] b) {
        if(b[0] == 0 && b[1] == 0){
            return 0;
        }
        return (int)ByteBuffer.wrap(b).getShort();
    }

    public byte[] iToB(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public byte[] iToB2(int i){
        return ByteBuffer.allocate(2).putShort((short) i).array();
    }

    public String arrToStr(byte[] b) {
        String res = "";
        for (int i = 0; i < b.length; i++) {
            res += b[i];
            if (i != b.length - 1) {
                res += ", ";
            }
        }
        return res;
    }

}



