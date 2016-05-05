package com.jabber.jconnect;

public class MucParticipant {

    private String nick;
    private String role;

    public MucParticipant(String nick){
        this.nick = nick;
    }

    public String getNick() {
        return nick;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
