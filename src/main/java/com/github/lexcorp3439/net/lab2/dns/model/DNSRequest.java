package com.github.lexcorp3439.net.lab2.dns.model;

import java.nio.ByteBuffer;
import java.util.Random;


public class DNSRequest {
    private String domain;
    private QueryType type;

    public DNSRequest(String domain, QueryType type) {
        this.domain = domain;
        this.type = type;
    }

    public byte[] getRequest() {
        int qNameLength = getQNameLength();
        ByteBuffer request = ByteBuffer.allocate(12 + 5 + qNameLength);
        request.put(createRequestHeader());
        request.put(createQuestionHeader(qNameLength));
        return request.array();
    }

    private byte[] createRequestHeader() {
        ByteBuffer header = ByteBuffer.allocate(12);
        byte[] randomID = new byte[2];
        new Random().nextBytes(randomID);
        header.put(randomID);
        header.put((byte) 0x01);
        header.put((byte) 0x00);
        header.put((byte) 0x00);
        header.put((byte) 0x01);

        return header.array();
    }

    private int getQNameLength() {
        int byteLength = 0;
        String[] items = domain.split("\\.");
        for (String item : items) {
            byteLength += item.length() + 1;
        }
        return byteLength;
    }

    private byte[] createQuestionHeader(int qNameLength) {
        ByteBuffer question = ByteBuffer.allocate(qNameLength + 5);

        String[] items = domain.split("\\.");
        for (String item : items) {
            question.put((byte) item.length());
            for (int j = 0; j < item.length(); j++) {
                question.put((byte) ((int) item.charAt(j)));
            }
        }

        question.put((byte) 0x00);

        //Add Query Type
        question.put(hexStringToByteArray("000" + hexValueFromQueryType(type)));
        question.put((byte) 0x00);
        //Add Query Class - always  0x0001 for internet addresses
        question.put((byte) 0x0001);

        return question.array();
    }

    private char hexValueFromQueryType(QueryType type) {
        switch (type) {
            case A:
                return '1';
            case NS:
                return '2';
            default:
                return 'F';
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
