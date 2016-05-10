package com.jabber.jconnect;

public class Contact {

    private String jid;
    private String name = " ";
    private String group = " ";
    private String status = " ";

    public Contact(String jid){
        this.jid = jid;
    }

    public String getJid() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString(){
        return "\"" + jid + "," + name +"," + group + "\"";
    }
}
