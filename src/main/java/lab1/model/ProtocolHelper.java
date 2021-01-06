package lab1.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProtocolHelper {
    public static List<Protocol> build(String name, String message) {
        assert name.length() < 128;
        int currentIndex = 0;
        int maxMessageLength = 127;
        List<Protocol> protocols = new ArrayList<>();

        byte nameLength = (byte) name.length();
        while (currentIndex < message.length() && currentIndex + maxMessageLength < message.length()) {
            String messagePart = message.substring(currentIndex, currentIndex + maxMessageLength);
            currentIndex = currentIndex + maxMessageLength;
            Protocol protocol = new Protocol(nameLength, name, (byte) messagePart.length(), messagePart, false);
            protocols.add(protocol);
        }

        String messagePart = message.substring(currentIndex, currentIndex + message.length() - currentIndex);
        Protocol protocol = new Protocol(nameLength, name, (byte) messagePart.length(), messagePart, true);
        protocols.add(protocol);

        return protocols;
    }

    public static List<Protocol> read(InputStream inputStream) {
        Protocol lastMsg = null;
        List<Protocol> msgs = new ArrayList<>();
        try {
            while (lastMsg == null || !lastMsg.getEnd()) {
                byte[] buff = new byte[127 + 127 + 3];
                int code = inputStream.read(buff);
                if (code == 1) {
                    System.out.println("System.exit(1);");
                    System.exit(1);
                }
                Protocol msg = Protocol.deserialize(buff);
                lastMsg = msg;
                msgs.add(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msgs;
    }

    public static List<Protocol> read(SocketChannel socket) {
        Protocol lastMsg = null;
        List<Protocol> msgs = new ArrayList<>();
        try {
            while (lastMsg == null || !lastMsg.getEnd()) {
                ByteBuffer buff = ByteBuffer.wrap(new byte[127+127+3]);
                int code = socket.read(buff);
                if (code == 1) {
                    System.out.println("System.exit(1);");
                    System.exit(1);
                }
                Protocol msg = Protocol.deserialize(buff.array());
                lastMsg = msg;
                msgs.add(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msgs;
    }

    public static void write(List<Protocol> messages, OutputStream outputStream) {
        messages.forEach(msg -> {
            try {
                outputStream.write(Protocol.serialize(msg));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void write(List<Protocol> messages, SocketChannel socket) {
        messages.forEach(msg -> {
            try {
                ByteBuffer buff = ByteBuffer.wrap(Protocol.serialize(msg));
                socket.write(buff);
                buff.rewind();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static String toLine(List<Protocol> msgs) {
        return msgs.stream().map(Protocol::getMessage).collect(Collectors.joining());
    }


    public static void main(String[] args) {
        String text = "Изначально язык назывался Oak («Дуб»), разрабатывался Джеймсом Гослингом для программирования бытовых электронных устройств. Из-за того, что язык с таким названием уже существовал, Oak был переименован в Java[4]. Назван в честь марки кофе Java, которая, в свою очередь, получила наименование одноимённого острова (Ява), поэтому на официальной эмблеме языка изображена чашка с горячим кофе. Существует и другая версия происхождения названия языка, связанная с аллюзией на кофе-машину как пример бытового устройства, для программирования которого изначально язык создавался. В соответствии с этимологией в русскоязычной литературе с конца двадцатого и до первых лет двадцать первого века название языка нередко переводилось как Ява, а не транскрибировалось.\n" +
                "\n" +
                "В результате работы проекта мир увидел принципиально новое устройство, карманный персональный компьютер Star7, который опередил своё время более чем на 10 лет, но из-за большой стоимости в 50 долларов не смог произвести переворот в мире технологии и был забыт.\n" +
                "\n" +
                "Устройство Star7 не пользовалось популярностью в отличие от языка программирования Java и его окружения. Следующим этапом жизни языка стала разработка интерактивного телевидения. В 1994 году стало очевидным, что интерактивное телевидение было ошибкой.\n" +
                "\n" +
                "С середины 1990-х годов язык стал широко использоваться для написания клиентских приложений и серверного программного обеспечения. Тогда же определённое распространение получила технология Java-апплетов — графических Java-приложений, встраиваемых в веб-страницы; с развитием возможностей динамических веб-страниц в 2000-е годы технология стала применяться редко.\n" +
                "\n" +
                "В веб-разработке применяется Spring Framework; для документирования используется утилита Javadoc.\n" +
                "\n";
        List<Protocol> protocols = build("lexcorp", text);

        StringBuilder result = new StringBuilder();
        for (Protocol it : protocols) {
            result.append(it.getMessage());
        }
        assert text.equals(result.toString());
    }
}
