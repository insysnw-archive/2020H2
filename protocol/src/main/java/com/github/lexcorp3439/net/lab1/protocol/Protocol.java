package com.github.lexcorp3439.net.lab1.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class Protocol {
    private Byte nameLength;
    private Byte messageLength;
    private Boolean end;
    private String message;
    private String name;

    public Protocol() {
    }

    public Protocol(Byte nameLength, String name, Byte messageLength, String message, Boolean end) {
        this.nameLength = nameLength;
        this.messageLength = messageLength;
        this.end = end;
        this.message = message;
        this.name = name;
    }

    public short getNameLength() {
        return nameLength;
    }

    public Protocol setNameLength(byte nameLength) {
        this.nameLength = nameLength;
        return this;
    }

    public short getMessageLength() {
        return messageLength;
    }

    public Protocol setMessageLength(byte messageLength) {
        this.messageLength = messageLength;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Protocol setMessage(String message) {
        if (this.messageLength == null) {
            throw new IllegalArgumentException("Сначала необходимо указать размер собщения");
        }
        if (message.length() > messageLength.intValue()) {
            throw new IllegalArgumentException("Количетсво символов в сообщении не должно превышать " + messageLength.intValue());
        }
        this.message = message;
        return this;
    }

    public String getName() {
        return name;
    }

    public Protocol setName(String name) {
        if (this.nameLength == null) {
            throw new IllegalArgumentException("Сначала необходимо указать размер собщения");
        }
        if (name.length() > nameLength.intValue()) {
            throw new IllegalArgumentException("Количетсво символов в сообщении не должно превышать 127");
        }
        this.name = name;
        return this;
    }

    public Protocol setEnd(boolean end) {
        this.end = end;
        return this;
    }

    public Boolean getEnd() {
        return end;
    }

    public static byte[] serialize(Protocol protocol) {
        int arraySize = 3 + protocol.nameLength + protocol.messageLength;
        byte[] array = new byte[arraySize];
        array[0] = protocol.nameLength;
        System.arraycopy(protocol.name.getBytes(), 0, array, 1, protocol.nameLength);
        array[protocol.nameLength + 1] = protocol.messageLength;
        System.arraycopy(protocol.message.getBytes(), 0, array, protocol.nameLength + 2, protocol.messageLength);
        array[arraySize - 1] = (byte) (protocol.end ? 1 : 0);
        return array;
    }

    public static Protocol deserialize(byte[] array) {

        byte nameLength = array[0];
        byte messageLength = array[nameLength + 1];
        int messageSize = nameLength + 2 + messageLength;
        String name = new String(Arrays.copyOfRange(array, 1, nameLength + 1), StandardCharsets.UTF_8);
        String message = new String(Arrays.copyOfRange(array, nameLength + 2, messageSize), StandardCharsets.UTF_8);
        boolean end = array[messageSize] == 1;

        Protocol protocol = new Protocol();
        protocol.setNameLength(nameLength)
                .setName(name)
                .setMessageLength(messageLength)
                .setMessage(message)
                .setEnd(end);
        return protocol;
    }

    @Override
    public String toString() {
        return "Protocol{" +
                "nameLength=" + nameLength +
                ", name='" + name + '\'' +
                ", messageLength=" + messageLength +
                ", message='" + message + '\'' +
                ", end=" + end +
                '}';
    }

    public static void main(String[] args) {
        byte[] arr = Protocol.serialize(
                new Protocol()
                        .setNameLength((byte) 2)
                        .setName("Lo")
                        .setMessageLength((byte) 1)
                        .setMessage("j")
                        .setEnd(true)
        );
        Protocol protocol = Protocol.deserialize(arr);
        System.out.println(
                protocol.toString()
        );
    }
}
