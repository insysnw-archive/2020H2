import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;

public class Main {

    public static int PORT = 8000;
    public static LinkedList<ServerListener> serverListeners = new LinkedList<ServerListener>();
    public static volatile ArrayList<Error> errors = new ArrayList<>();
    public static volatile int errorId = 0;

    public static void main(String[] args) throws IOException {
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server starts successfully");
            while (serverSocket.isBound()) {
                Socket socket = serverSocket.accept();
                try {
                    serverListeners.add(new ServerListener(socket));
                    System.out.println("Socket was added");
                } catch (IOException e){
                    System.out.println("Socket was not created");
                    socket.close();
                }
            }
        }
    }
}

class ServerListener extends Thread {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final B b = new B();
    private byte[] name = {(byte)0xff, (byte)0xee, (byte)0xdd, (byte)0xcc, (byte)0xbb, (byte)0xaa};
    private final byte[] profession = {0x03};

    public ServerListener(Socket socket) throws IOException{
        this.socket = socket;
        in = socket.getInputStream();
        out = socket.getOutputStream();
        start();
    }

    @Override
    public void run(){
        try{
            byte[] auth = new byte[1];
            in.read(auth);
            if(auth[0] == 0x01) authorisation();
            else{
                byte authError = (byte)0xc1;
                out.write(authError);
                System.out.println("Bad authorization");
                socket.close();
                Main.serverListeners.remove(this);
                this.stop();
            }
            byte[] code = new byte[1];
            while(socket.isConnected()){
                in.read(code);
                switch(code[0]){
                    case 0x77: break;
                    case 0x02: receivingError(); break;
                    case 0x03: receptionError(); break;
                    case 0x04: confirmFix(); break;
                    case 0x05: issueErrors(); break;
                    case 0x06: fixRequest(); break;
                }
                code[0] = 0x77;

            }
            if (profession[0] == 0x01) System.out.println("Tester " + b.bToS(name) + " is disconnected");
            else System.out.println("Developer " + b.bToS(name) + " is disconnected");

        } catch (IOException e) {
            System.out.println(b.bToS(name) + " disconnected");
            try{
                System.out.println("trying to close socket");
                socket.close();

            } catch (IOException ioException) {
                System.out.println("socket already closed");
            }
            Main.serverListeners.remove(this);
            this.stop();


            e.printStackTrace();
        }
    }

    private void authorisation() throws IOException{
        System.out.println("Request for authorisation");
        byte[] prof = new byte[1];
        in.read(prof);
        profession[0] = prof[0];
        byte[] nameBlockLength = new byte[2];
        in.read(nameBlockLength);
        int nameLength = b.bToI2(nameBlockLength);
        byte[] nameBytes = new byte[nameLength];
        in.read(nameBytes);
        name = nameBytes;
        if(profession[0] == 0x01){
            System.out.println("Tester " + b.bToS(name) + " connected");
            out.write(0x81);
        }
        else if(profession[0] == 0x02) {
            System.out.println("Developer " + b.bToS(name) + " connected");
            out.write(0x81);
        }
        else{
            System.out.println("Unexpected input profession byte. See - " + profession[0]);
            out.write(0xc1);
        }

    }

    private void receivingError() throws IOException{
        byte[] hasFixed = new byte[1];
        in.read(hasFixed);
        if(hasFixed[0] == 0x01){
            if(profession[0] == 0x02){
                System.out.println("Developer can not receive fixed errors");
                out.write(0xc2);
            }else if(profession[0] == 0x01){
                answerReceiving(true);
            } else{
                System.out.println("Incorrect profession");
                out.write(0xc1);
            }
        }else if(hasFixed[0] == 0x00){
            answerReceiving(false);
        }else{
            System.out.println("Error with receiving message");
            out.write(0xc2);
        }
    }

    private void receptionError() throws IOException{
        if(profession[0] == 0x02){
            System.out.println("Developer can not receipt the error");
            out.write(0xc2);
            byte[] buff = new byte[1000];
            in.read(buff);
        }else{
            byte[] idenBlockLength = new byte[2];
            in.read(idenBlockLength);
            int idenLength = b.bToI2(idenBlockLength);
            byte[] iden = new byte[idenLength];
            in.read(iden);
            byte[] projBlockLength = new byte[2];
            in.read(projBlockLength);
            int projLength = b.bToI2(projBlockLength);
            byte[] proj = new byte[projLength];
            in.read(proj);
            byte[] textBlockLength = new byte[2];
            in.read(textBlockLength);
            int textLength = b.bToI2(textBlockLength);
            byte[] text = new byte[textLength];
            in.read(text);
            byte[] devrBlockLength = new byte[2];
            in.read(devrBlockLength);
            int devrLength = b.bToI2(devrBlockLength);
            byte[] devr = new byte[devrLength];
            in.read(devr);
            out.write(0x81);
            Error error = new Error(iden, proj, text, devr);
            Main.errors.add(error);
            System.out.println("error was added");
            System.out.println(b.bToS(error.iden) + " " + b.bToS(error.proj)  + " " + b.bToS(error.text)  + " " + b.bToS(error.devr));
        }
    }

    private void confirmFix() throws IOException{
        if(profession[0] == 0x02){
            System.out.println("Developer can not accept the fix");
            out.write(0xc4);
            byte[] buff = new byte[1000];
            in.read(buff);
        }else{
            byte[] id = new byte[4];
            in.read(id);
            int idd = b.bToI4(id);
            byte[] fixed = new byte[1];
            in.read(fixed);
            if (fixed[0] == 0x01){
                Error err = getById(idd);
                if (err!= null){
                    if (err.fixed){
                        removeById(idd);
                        out.write(0x81);
                    }else{
                        out.write(0xc4);
                    }
                }else{
                    out.write(0xc4);
                }

//                Error err = getById(idd);
//                if(err != null) {
//                    err.fixed = true;
//                    out.write(0x81);
//                } else{
//                    out.write(0xc4);
//                }
            }else{
                Error err = getById(idd);
                if(err != null) {
                    err.fixed = false;
                    out.write(0x81);
                } else{
                    out.write(0xc4);
                }
            }
        }
    }

    private void issueErrors() throws IOException{
        if(profession[0] == 0x01){
            System.out.println("Testers can not get their errors");
            try{
                out.write(0xc2);
            } catch (SocketException e){
                socket.close();
                Main.serverListeners.remove(this);
                this.stop();
            }

        }else{
            ArrayList<Error> arr = new ArrayList<>();
            for (Error err: Main.errors){
                if (b.compare(err.devr, name)){
                    if(!err.fixed){
                        arr.add(err);
                    }
                }
            }
            byte[] res = {(byte)0x82};
            res = b.sum(res, b.iToB2(arr.size()));
            for (Error err : arr){
                res = b.sum(res, err.id);
                res = b.sum(res, err.getBlockError());
            }
            out.write(res);
        }


    }

    private void fixRequest() throws IOException{
        byte[] id = new byte[4];
        in.read(id);
        if(!Main.errors.isEmpty()){
            for(Error err : Main.errors){
                if(b.compare(id, err.id)){
                    if (b.compare(err.devr, name)){
                        if(err.fixed){
                            System.out.println("Error was already fixed");
                            out.write(0xc4);
                            break;
                        }else{
                            err.fixed = true;
                            out.write(0x81);
                            break;
                        }
                    }else{
                        System.out.println("не принадлежит");
                        out.write(0xc4);
                        break;
                    }

                }
            }
        }else{
            out.write(0xc4);
        }


    }

    private void answerReceiving(boolean hasFixed) throws IOException{
        byte[] result = {(byte)0x82};
        ArrayList<byte[]> arr = new ArrayList<>();
        int count = 0;
        if (hasFixed){
            for(Error err : Main.errors){
                if (err.fixed){
                    count++;
                    arr.add(err.id);
                    arr.add(err.getBlockError());
                }
            }
        }else{
            for(Error err : Main.errors){
                if (!err.fixed){
                    count++;
                    arr.add(err.id);
                    arr.add(err.getBlockError());
                }
            }
        }
        result = b.sum(result, b.iToB2(count));
        for (byte[] a: arr){
            result = b.sum(result, a);
        }
        out.write(result);
    }

    private Error getById(int id){
        for(Error err : Main.errors){
            if(b.bToI4(err.id) == id) return err;
        }
        return null;
    }

    private Error removeById(int id){
        Main.errors.removeIf(err -> b.bToI4(err.id) == id);
        return null;
    }

}


class Error{
    B b = new B();
    public byte[] iden;
    public byte[] proj;
    public byte[] text;
    public byte[] devr;
    public boolean fixed = false;
    public byte[] id;

    public Error(byte[] iden, byte[] proj, byte[] text, byte[] devr){
        this.iden = iden;
        this.proj = proj;
        this.text = text;
        this.devr = devr;
        Main.errorId++;
        id = b.iToB4(Main.errorId);
    }

    public byte[] getBlockError(){
        byte[] idenBlock = b.sum(b.iToB2(iden.length), iden);
        byte[] projBlock = b.sum(b.iToB2(proj.length), proj);
        byte[] textBlock = b.sum(b.iToB2(text.length), text);
        byte[] devrBlock = b.sum(b.iToB2(devr.length), devr);
        byte[] result = b.sum(idenBlock, projBlock);
        result = b.sum(result, textBlock);
        return b.sum(result, devrBlock);
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

    public int bToI2(byte[] b) {
        return (int)ByteBuffer.wrap(b).getShort();
    }

    public int bToI4(byte[] b){
        return ByteBuffer.wrap(b).getInt();
    }

    public byte[] iToB4(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public byte[] iToB2(int i){
        return ByteBuffer.allocate(2).putShort((short) i).array();
    }

    public boolean compare(byte[] a, byte[] b){
        if(a.length != b.length) return false;
        else {
            for (int i = 0; i < a.length; i++){
                if (a[i] != b[i]) return false;
            }
            return true;
        }
    }

    public String arrToStr(byte[] b) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            res.append(b[i]);
            if (i != b.length - 1) {
                res.append(", ");
            }
        }
        return res.toString();
    }

}