import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class RWThread extends Thread {

    private PrintWriter writer;
    private final String clientName;
    private BufferedReader reader;
    private final Socket socket;
    private boolean requestClose = false;
    private boolean testing = false;

    public RWThread(Socket socket, String clientName) {
        this.clientName = clientName;
        this.socket = socket;

        OutputStream outputStream;
        InputStream inputStream;
        try {
            outputStream = socket.getOutputStream();
            writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

            inputStream = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            JSONObject connectionRequest = new JSONObject();
            connectionRequest.put("type", "0");
            connectionRequest.put("text", clientName);
            writer.println(connectionRequest.toString());
            System.out.println("Connecting...");

            String content;
            JSONObject jsonContent;
            do {
                content = reader.readLine();
                jsonContent = new JSONObject(content);
                switch (jsonContent.getString("type")) {
                    case "1" -> {
                        testing = false;
                        System.out.println("Connected!");
                    }
                    case "4" -> {
                        System.out.println(jsonContent.getString("text"));
                        testing = false;
                    }
                    case "6" -> System.out.println(jsonContent.getString("text"));
                    case "8" -> {
                        testing = true;
                        System.out.println(jsonContent.getString("text"));
                        Console console = System.console();
                        String answer = console.readLine("Enter your answer: ");
                        JSONObject json = new JSONObject();
                        try {
                            json.put("type", "9");
                            json.put("text", answer);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        writer.println(json.toString());
                    }
                    case "-2" -> {
                        testing = true;
                        System.out.println("Please try with another name!");
                        Console console = System.console();
                        String name = console.readLine("\nEnter your name: ");
                        JSONObject json = new JSONObject();
                        try {
                            json.put("type", "0");
                            json.put("text", name);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        writer.println(json.toString());
                    }
                    case "-1" -> {
                        testing = true;
                        System.out.println("Please try another test number");
                        Console console = System.console();
                        String testNumber;
                        do {
                            testNumber = console.readLine("Enter the test number: ");
                            if (testNumber.matches("[0-9]+")){
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("type", "7");
                                    json.put("text", testNumber);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                writer.println(json.toString());
                                break;
                            } else if (testNumber.isEmpty()) {
                                testing =false;
                                break;
                            } else System.out.println("The input must be the number!");
                        } while (true);
                    }
                }
                if (!testing) makeRequest();
            } while (!requestClose);
        } catch (SocketException socketException) {
            System.out.println(socketException.getMessage());
        }
        catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeRequest() {
        Console console = System.console();
        String typeRequest = console.readLine("--------------------\n(A) Get the results of the last test\n(B) Get " +
                "a list of tests\n(C) Select the test\n(D) Quit\nEnter your choice: ");
        switch (typeRequest) {
            case "A" -> {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "3");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                writer.println(json.toString());
            }
            case "B" -> {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "5");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                writer.println(json.toString());
            }
            case "C" -> {
                do {
                    String testNumber = console.readLine("Enter the test number: ");
                    if (testNumber.matches("[0-9]+")){
                        JSONObject json = new JSONObject();
                        try {
                            json.put("type", "7");
                            json.put("text", testNumber);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        writer.println(json.toString());
                        break;
                    } else System.out.println("The input must be the number!");
                } while (true);
            }
            case "D" -> {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "2");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                writer.println(json.toString());
                requestClose = true;
            }
            default -> {
                System.out.println("Please choose again!");
                makeRequest();
            }
        }
    }
}
