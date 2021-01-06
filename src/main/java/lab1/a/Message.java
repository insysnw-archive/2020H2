package lab1.a;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    private String username;
    private String message;
    private int code;
    private boolean complete;

    public Message(String username, String message, int code, boolean complete) {
        this.username = username;
        this.message = message;
        this.code = code;
        this.complete = complete;
    }

    public Message(String username, String message, boolean complete) {
        this(username, message, 0, complete);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "username='" + username + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
