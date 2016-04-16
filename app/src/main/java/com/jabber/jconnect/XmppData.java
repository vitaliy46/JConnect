package com.jabber.jconnect;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmppData {
    private static XmppData xmppData = new XmppData();

    Map<String, String> messagesList = new HashMap<>();
    private String jid;

    List<String> mucList = new ArrayList<>();
    Map<String, String> mucMessagesList = new HashMap<>();

    private String messageToSend;

    public static XmppData getInstance() {
        return xmppData;
    }

    private XmppData() {
    }

    public String getMessagesList(String jid) {
        return messagesList.get(jid);
    }

    public void setMessagesList(String jid, String nick, String message) {
        String messages = messagesList.get(jid);

        if(messages == null) {
            messages = "";
        }

        messages += nick + ": " + message + "\n";
        messagesList.put(jid, messages);
    }

    public String getMucMessagesList(String mucId) {
        return mucMessagesList.get(mucId);
    }

    public void setMucMessagesList(String mucId, String nick, String message) {
        String messages = mucMessagesList.get(mucId);

        if(messages == null) {
            messages = "";
        }

        messages += nick + ": " + message + "\n";
        mucMessagesList.put(mucId, messages);
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getMessageToSend() {
        return messageToSend;
    }

    public void setMessageToSend(String messageToSend) {
        this.messageToSend = messageToSend;
    }

    public List<String> getMucList() {
        return mucList;
    }

    public void setMucList(List<String> mucList) {
        this.mucList = mucList;
    }
}
