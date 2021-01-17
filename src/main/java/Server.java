import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    public Server(String hostName, int port) throws Exception {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(hostName, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("The server is running");
    }

    public static void main(String[] args) throws Exception {
        Server server;
        if (args.length<1) {
            server = new Server(Strings.DEFAULT_ADDRESS, Strings.DEFAULT_PORT);
        } else if (args.length == 1) {
            server = new Server(args[0], Strings.DEFAULT_PORT);
        } else server = new Server(args[0], Integer.parseInt(args[1]));
        server.listen();
    }

    public void listen() throws Exception {
        while (true) {
            int count=selector.select();
            if (count > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        readAndSendResponse(key);
                    }
                    keyIterator.remove();
                }
            }
        }
    }

    //Send a message
    public void readAndSendResponse(SelectionKey key) {
        SocketChannel channel = null;
        try {
            channel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);
            if (count > 0) {
                String msg = new String(buffer.array());
                JSONObject jsonClient;
                jsonClient = new JSONObject(msg);
                switch (jsonClient.getString(Strings.TYPE)) {
                    case "0" -> {
                        System.out.println("Message(type = ConnectionRequest)");
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put(Strings.TYPE, "1");
                        send(jsonResponse.toString(), channel);
                    }
                    case "2" -> {
                        System.out.println("Message(type = DisconnectRequest)");
                        channel.close();
                    }
                    case "3" -> {
                        System.out.println("Message(type = CurrenciesListRequest)");
                        FileInputStream inputStream = new FileInputStream("data.txt");
                        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        JSONObject jsonResponse = new JSONObject();
                        StringBuilder text = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            JSONObject json = new JSONObject(line);
                            text.append("Currency: ").append(json.getString(Strings.CODE)).append(", ").append("rate: ")
                                    .append(json.getString(Strings.RATE)).append(", ").append("increment: ")
                                    .append(json.getString(Strings.INCREMENT)).append("\n");

                        }
                        jsonResponse.put(Strings.TYPE, "4");
                        jsonResponse.put("text", (text.toString().isEmpty()) ? "no data" : text.toString().trim());
                        send(jsonResponse.toString(), channel);
                    }
                    case "5" -> {
                        System.out.println("Message(type = AddCurrencyRequest)");
                        String currency = jsonClient.getString(Strings.TEXT);
                        FileInputStream inputStream = new FileInputStream("data.txt");
                        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put(Strings.TYPE, "6");
                        while ((line = reader.readLine()) != null) {
                            JSONObject json = new JSONObject(line);
                            if (json.getString(Strings.CODE).equalsIgnoreCase(currency)) {
                                jsonResponse.put(Strings.TYPE, "-2");
                                break;
                            }
                        }
                        send(jsonResponse.toString(), channel);

                        if (jsonResponse.getString(Strings.TYPE).equals("6")) {
                            FileWriter fw = new FileWriter("data.txt", true);
                            BufferedWriter writer = new BufferedWriter(fw);

                            JSONObject record = new JSONObject();
                            record.put(Strings.CODE, currency);
                            record.put(Strings.RATE,"-");
                            record.put(Strings.INCREMENT, "-");
                            record.put(Strings.HISTORY, "-");

                            writer.write(record.toString()+"\n");
                            writer.close();
                        }
                        reader.close();
                    }
                    case "7" -> {
                        System.out.println("Message(type = RemoveCurrencyRequest)");
                        String currency = jsonClient.getString("text");
                        FileInputStream inputStream = new FileInputStream("data.txt");
                        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put(Strings.TYPE, "-1");
                        List<String> data = new ArrayList<>();
                        while ((line = reader.readLine()) != null) {
                            JSONObject json = new JSONObject(line);
                            if (json.getString(Strings.CODE).equalsIgnoreCase(currency)) {
                                jsonResponse.put(Strings.TYPE, "8");
                            } else {
                                data.add(line);
                            }
                        }
                        send(jsonResponse.toString(), channel);

                        if (jsonResponse.getString(Strings.TYPE).equals("8")) {
                            FileWriter fw = new FileWriter("data.txt", false);
                            BufferedWriter writer = new BufferedWriter(fw);

                            for (String l : data) {
                                writer.write(l +"\n");
                            }

                            writer.close();
                        }
                        reader.close();
                    }
                    case "9" -> {
                        System.out.println("Message(type = AddRateRequest)");
                        if (!jsonClient.getString(Strings.RATE).matches("\\d+(\\.\\d+)?")) {
                            JSONObject jsonResponse = new JSONObject();
                            jsonResponse.put(Strings.TYPE, "-3");
                            send(jsonResponse.toString(), channel);
                            break;
                        }
                        String currency = jsonClient.getString(Strings.CODE);
                        FileInputStream inputStream = new FileInputStream("data.txt");
                        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put(Strings.TYPE, "-1");
                        List<String> data = new ArrayList<>();
                        try {
                            while ((line = reader.readLine()) != null) {
                                JSONObject json = new JSONObject(line);
                                if (json.getString(Strings.CODE).equalsIgnoreCase(currency)) {
                                    jsonResponse.put(Strings.TYPE, "10");
                                    if (json.getString(Strings.HISTORY).equals("-")) {
                                        json.put(Strings.HISTORY, jsonClient.getString("rate"));
                                    } else {
                                        json.put(Strings.HISTORY, json.getString(Strings.HISTORY)+" "
                                                + jsonClient.getString("rate"));
                                        json.put(Strings.INCREMENT, getIncrement(Double.parseDouble(json.getString(Strings.RATE))
                                                , Double.parseDouble(jsonClient.getString(Strings.RATE))));
                                    }
                                    json.put(Strings.RATE, jsonClient.getString(Strings.RATE));
                                    data.add(json.toString());
                                } else {
                                    data.add(line);
                                }
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        send(jsonResponse.toString(), channel);

                        if (jsonResponse.getString(Strings.TYPE).equals("10")) {
                            FileWriter fw = new FileWriter("data.txt", false);
                            BufferedWriter writer = new BufferedWriter(fw);
                            for (String l : data) {
                                writer.write(l +"\n");
                            }
                            writer.close();
                        }
                        reader.close();
                    }
                    case "11" -> {
                        System.out.println("Message(type = HistoryRequest)");
                        String currency = jsonClient.getString(Strings.TEXT);
                        FileInputStream inputStream = new FileInputStream("data.txt");
                        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr);
                        String line;
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put(Strings.TYPE, "-1");
                        while ((line = reader.readLine()) != null) {
                            JSONObject json = new JSONObject(line);
                            if (json.getString(Strings.CODE).equalsIgnoreCase(currency)) {
                                jsonResponse.put(Strings.TYPE, "12");
                                jsonResponse.put(Strings.TEXT, (json.getString(Strings.HISTORY).isEmpty()) ? "no data"
                                        : json.getString(Strings.HISTORY));
                                break;
                            }
                        }
                        send(jsonResponse.toString(), channel);
                        reader.close();
                    }
                }
            }
        } catch (JSONException jsonException){
            System.out.println(jsonException.getMessage());
        }catch (IOException e) {
            try {
                System.out.println(channel.getRemoteAddress() + "Offline...");
                key.cancel();
                channel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }

    public void send(String msg, SocketChannel self) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap((msg).getBytes());
        self.write(buffer);
    }

    private String getIncrement (double first, double last) {
        return (first<last) ? "+" + (double)Math.round((last / first * 100-100)*10)/10 +"%": "-"
                + (double)Math.round((100 - (last / first * 100))*10)/10 + "%";
    }
}

