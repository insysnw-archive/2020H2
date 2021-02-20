package server;

import java.util.Random;

public class User {
    private String login;
    private int id;
    private String password;
    private int amount;

    public User (String login, String password) {
        this.login = login;
        Random random = new Random();
        this.id = random.nextInt(999999);
        this.password = password;
        this.amount = 0;
    }

    public User (String[] info) {
        this.login = info[0];
        this.id = Integer.parseInt(info[1]);
        this.password = info[2];
        this.amount = Integer.parseInt(info[3]);
    }

    public String getInfo() {
        return login + " " +
                id + " " +
                password + " " +
                amount;
    }

    public String getLogin() {
        return login;
    }
    public String getPassword() { return password; }
    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
