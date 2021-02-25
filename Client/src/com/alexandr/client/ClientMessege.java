package com.alexandr.client;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;

public class ClientMessege implements Serializable {
    private String text ="";
    private String time ="";
    private String name = "";
    private int MAX_SIZE_TEXT = Byte.MAX_VALUE;
    private int MAX_SIZE_NAME = 16;

    public String getText() {
        return text;
    }

    public int setText(String text) {
        int len = text.length();
        if(len > MAX_SIZE_TEXT){
            return -1;

        } else {
            if (len == 0) {
                return -2;
            } else {
                this.text = text;
                return 0;
            }
        }
    }

    public String getName() {
        return name;
    }

    public int setName(String nam) {
        int len = nam.length();
        if(len > MAX_SIZE_NAME){
            return -1;
        } else {
            if (len == 0) {
                return -2;
            } else {
                this.name = nam;
                return 0;
            }
        }
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public byte[] textToBytes(){
        return text.getBytes(StandardCharsets.UTF_8);

    }
    public byte[] timeToBytes(){
        return time.getBytes(StandardCharsets.UTF_8);
    }
    public byte[] nameToBytes(){
        return name.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] createArr(int flag){
        byte[] nb = nameToBytes();
        byte[] tmb = timeToBytes();
        byte[] txtb;
        switch (flag){
            case 0 -> {
                txtb = textToBytes();
            }
            case 1 -> {
                String str = "Отключение сервера";
                txtb = str.getBytes();
            }
            case 2 -> {
                String str = name + " вошел в чат";
                txtb = str.getBytes();
            }
            case 3 -> {
                String str = name + " покинул чат";
                txtb = str.getBytes();
            }
            case 4 -> {
                String str ="Дублирование никнейма";
                txtb = str.getBytes();
            }
            default -> throw new IllegalStateException("Unexpected value: " + flag);
        }

        Integer timeLen = tmb.length;
        Integer nameLen = nb.length;
        Integer txtLen = txtb.length;

        byte timelenb = timeLen.byteValue();
        byte namelenb = nameLen.byteValue();
        byte txtlenb = txtLen.byteValue();


        byte[] answ  = new byte[3+nameLen+txtLen+timeLen];
        answ[0] = timelenb;
        answ[1] = namelenb;
        answ[2] = txtlenb;
        for(int i = 0; i< timeLen; i++){
            int index = i+3;
            answ[index] = tmb[i];
        }
        for(int i = 0; i < nameLen; i++){
            int index = i+timeLen+3;
            answ[index] = nb[i];
        }
        for(int i = 0; i < txtLen; i++){
            int index = i+nameLen+timeLen+3;
            answ[index] = txtb[i];
        }
        return answ;
    }

    public void decodeMsg(byte[] msg){

        byte timelenb = msg[0];
        byte namelenb = msg[1];
        byte txtlenb = msg[2];

        int timeLen = Integer.decode(String.valueOf(timelenb));
        int nameLen = Integer.decode(String.valueOf(namelenb));
        int txtLen = Integer.decode(String.valueOf(txtlenb));

        byte[] anb = new byte[nameLen];
        byte[] atmb = new byte[timeLen];
        byte[] atxtb = new byte[txtLen];

        for (int i = 0; i < timeLen; i++){
            int index = i+3;
            atmb[i] = msg[index];
        }
        for(int i = 0; i < nameLen;i++){
            int index = i+3+timeLen;
            anb[i] = msg[index];
        }
        for(int i = 0; i < txtLen;i++){
            int index = i+3+nameLen+timeLen;
            atxtb[i] = msg[index];
        }
        this.setName(new String(anb));
        this.setText(new String(atxtb));
        this.setTime(new String(atmb));
    }
}
