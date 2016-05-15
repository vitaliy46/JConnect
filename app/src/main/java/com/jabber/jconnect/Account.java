package com.jabber.jconnect;

public class Account {

    private int id;
    private String serverName;
    private String login;
    private String password;
    private int port;
    private int selected;

    public Account(int id, String serverName, String login, String password, int port, int selected){
        this.id = id;
        this.serverName = serverName;
        this.login = login;
        this.password = password;
        this.port = port;
        this.selected = selected;
    }

    public int getId() {
        return id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }
}
