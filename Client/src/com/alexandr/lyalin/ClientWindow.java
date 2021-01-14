package com.alexandr.lyalin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ClientWindow extends JFrame {
    // адрес сервера
    private static final String SERVER_HOST = "localhost";
    // порт
    private static final int SERVER_PORT = 6666;
    // клиентский сокет
    private Socket clientSocket;
    // входящее сообщение
    private Scanner inMessage;
    // исходящее сообщение
    private PrintWriter outMessage;
    // следующие поля отвечают за элементы формы
    private JTextField jtfMessage;
    private JTextField jtfName;
    private JTextArea jtaTextAreaMessage;
    // имя клиента
    private String clientName = "";
    // получаем имя клиента
    public String getClientName() {
        return this.clientName;
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    private int connecting(){
        try {
            // подключаемся к серверу
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            inMessage = new Scanner(clientSocket.getInputStream());
            outMessage = new PrintWriter(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Нет соединения с сервером. Зайдите позже:(");
            return -1;
        }
        return 0;
    }


    // конструктор
    public ClientWindow() {
       int connection =  connecting();
       if(connection ==0) {
           // Задаём настройки элементов на форме
           setBounds(500, 300, 500, 500);
           setTitle("Client");
           setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

           jtaTextAreaMessage = new JTextArea();
           jtaTextAreaMessage.setEditable(false);
           jtaTextAreaMessage.setLineWrap(true);

           JScrollPane jsp = new JScrollPane(jtaTextAreaMessage);
           add(jsp, BorderLayout.CENTER);
           // label, который будет отражать количество клиентов в чате

           JLabel jlNumberOfClients = new JLabel("Количество клиентов в чате: ");
           add(jlNumberOfClients, BorderLayout.NORTH);
           JPanel bottomPanel = new JPanel(new BorderLayout());
           add(bottomPanel, BorderLayout.SOUTH);

           JButton jbSendMessage = new JButton("Отправить");
           bottomPanel.add(jbSendMessage, BorderLayout.EAST);

           jtfMessage = new JTextField("Введите ваше сообщение: ");
           bottomPanel.add(jtfMessage, BorderLayout.CENTER);

           jtfName = new JTextField("Введите ваше имя: ");
           bottomPanel.add(jtfName, BorderLayout.WEST);

           // обработчик события нажатия кнопки отправки сообщения
           jbSendMessage.addActionListener(new ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                   // если имя клиента, и сообщение непустые, то отправляем сообщение
                   if (!jtfMessage.getText().trim().isEmpty() && !jtfName.getText().trim().isEmpty() && !jtfMessage.getText().equals("Введите ваше сообщение: ")) {
                       clientName = jtfName.getText();
                       sendMsg();
                       // фокус на текстовое поле с сообщением
                       jtfMessage.grabFocus();
                   }
               }
           });
           // при фокусе поле сообщения очищается
           jtfMessage.addFocusListener(new FocusAdapter() {
               @Override
               public void focusGained(FocusEvent e) {
                   jtfMessage.setText("");
               }
           });
           // при фокусе поле имя очищается
           jtfName.addFocusListener(new FocusAdapter() {
               @Override
               public void focusGained(FocusEvent e) {
                   jtfName.setText("");
               }
           });
           // в отдельном потоке начинаем работу с сервером
           new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       // бесконечный цикл
                       while (true) {
                           // если есть входящее сообщение
                           if (inMessage.hasNext()) {
                               // считываем его
                               String inMes = inMessage.nextLine();
                               if (inMes.equals("###end###")) {
                                   // добавляем строку перехода
                                   jtaTextAreaMessage.append("\n");
                                   jtaTextAreaMessage.append("Cервер был отключен");
                                   Thread.sleep(1000);
                                   System.exit(0);
                               } else {
                                   String clientsInChat = "Клиентов в чате = ";
                                   if (inMes.indexOf(clientsInChat) == 0) {
                                       jlNumberOfClients.setText(inMes);
                                   } else {
                                       // выводим сообщение
                                       jtaTextAreaMessage.append(inMes);
                                       // добавляем строку перехода
                                       jtaTextAreaMessage.append("\n");
                                   }
                               }
                           }
                       }
                   } catch (Exception e) {
                       System.out.println("Some error :(");
                       System.exit(-1);
                   }
               }
           }).start();
           // добавляем обработчик события закрытия окна клиентского приложения
           addWindowListener(new WindowAdapter() {
               @Override
               public void windowClosing(WindowEvent e) {
                   super.windowClosing(e);
                   try {
                       // здесь проверяем, что имя клиента непустое и не равно значению по умолчанию
                       if (!clientName.isEmpty() && clientName != "Введите ваше имя: ") {
                           outMessage.println(clientName + " вышел из чата!");
                       } else {
                           outMessage.println("Участник вышел из чата, так и не представившись!");
                       }
                       // отправляем служебное сообщение, которое является признаком того, что клиент вышел из чата
                       outMessage.println("##session##end##");
                       outMessage.flush();
                       outMessage.close();
                       inMessage.close();
                       clientSocket.close();
                   } catch (IOException exc) {

                   }
               }
           });
           // отображаем форму
           setVisible(true);
       } else{
           System.exit(-1);
       }
    }

    // отправка сообщения
    public void sendMsg() {
        // формируем сообщение для отправки на сервер
        LocalDateTime now = LocalDateTime.now();
        String messageStr = "[" + dtf.format(now) +"]   " + jtfName.getText() + ": " + jtfMessage.getText();
        // отправляем сообщение
        outMessage.println(messageStr);
        outMessage.flush();
        jtfMessage.setText("Введите ваше сообщение: ");
    }
}

