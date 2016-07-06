package com.jabber.jconnect;


import android.content.Context;

import com.jabber.jconnect.database.JConnectDbContract;

import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class XmppData {
    private static XmppData xmppData = new XmppData();

    List<Contact> contactList = new ArrayList<>();

    List<Contact> checkedContacts = new ArrayList<>();
    Map<String, String> messagesList = new HashMap<>();

    List<MultiUserChat> mucList = new ArrayList<>();
    Map<String, String> mucMessagesList = new HashMap<>();
    Map<String, List<MucParticipant>> mucParticipantList = new HashMap<>();

    Map<String, Integer> messagesCountMap = new HashMap<>();

    private String messageToSend;

    List<BookmarkedConference> bookmarkedConferenceList = new ArrayList<>();
    List<BookmarkedConference> checkedBookmarks = new ArrayList<>();

    List<DiscoverItems.Item> serviceDiscoverItems = new ArrayList<>();

    JConnectDbContract jConnectDbContract = new JConnectDbContract();

    public static XmppData getInstance() {
        return xmppData;
    }

    private XmppData() {
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    public void setContactList(List<Contact> contactList) {
        this.contactList = contactList;
    }

    public List<Contact> getCheckedContacts() {
        return checkedContacts;
    }

    public void setCheckedContacts(List<Contact> checkedContacts) {
        this.checkedContacts = checkedContacts;
    }

    public String getMessagesList(String jid) {
        return messagesList.get(jid);
    }

    public void setMessagesList(String jid, String nick, String message) {
        String messages = messagesList.get(jid);

        if(messages == null) {
            messages = "";
        }

        if(message == null) {
            message = "";
        }

        Date date = new java.util.Date();
        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(date);

        messages += "[" + time + "] " + nick + ": " + message + "\n";
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

        if(message == null) {
            message = "";
        }

        Date date = new java.util.Date();
        String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(date);

        if(nick != null){
            messages += "[" + time + "] " + nick + ": " + message + "\n";
        } else {
            messages += "[" + time + "] " + message + "\n";
        }

        mucMessagesList.put(mucId, messages);
    }

    public void clearMucMessagesList(String mucId) {
        mucMessagesList.put(mucId, null);
    }

    // Сообщение для отправки чат/конференция
    public String getMessageToSend() {
        return messageToSend;
    }

    public void setMessageToSend(String messageToSend) {
        this.messageToSend = messageToSend;
    }

    // Список конференций
    public List<MultiUserChat> getMucList() {
        return mucList;
    }

    public void setMucList(List<MultiUserChat> mucList) {
        this.mucList = mucList;
    }

    // Список участников конференций
    public void addMucParticipant(String mucID, MucParticipant mucParticipant){
        if(mucParticipantList.get(mucID) == null){
            mucParticipantList.put(mucID, new ArrayList<MucParticipant>());
        }

        mucParticipantList.get(mucID).add(mucParticipant);
    }

    public void delMucParticipant(String mucID, String mucParticipantNick){
        List<MucParticipant> list = mucParticipantList.get(mucID);

        List<MucParticipant> newList = new ArrayList<>();
        for(MucParticipant participant:list){
            if(!(mucParticipantNick.equals(participant.getNick()))){
                newList.add(participant);
            }
        }

        mucParticipantList.put(mucID, newList);
    }

    public List<MucParticipant> getMucParticipantList(String mucID){
        return mucParticipantList.get(mucID);
    }

    public void clearMucParticipantList(String mucID){
        mucParticipantList.put(mucID, new ArrayList<MucParticipant>());
    }

    // Подсчет новых сообщений
    public void initializeOrResetMessagesCount(String jid){
        messagesCountMap.put(jid, 0);
    }

    public void incrementMessagesCount(String jid){
        messagesCountMap.put(jid, messagesCountMap.get(jid)+1);
    }

    public int getMessagesCount(String jid){
        return messagesCountMap.get(jid) != null ? messagesCountMap.get(jid) : 0;
    }

    // Закладки
    public List<BookmarkedConference> getBookmarkedConferenceList() {
        return bookmarkedConferenceList;
    }

    public void setBookmarkedConferenceList(List<BookmarkedConference> bookmarkedConferenceList) {
        this.bookmarkedConferenceList = bookmarkedConferenceList;
    }

    public List<BookmarkedConference> getCheckedBookmarks() {
        return checkedBookmarks;
    }

    public void setCheckedBookmarks(List<BookmarkedConference> checkedBookmarks) {
        this.checkedBookmarks = checkedBookmarks;
    }

    public void clearCheckedBookmarks() {
        this.checkedBookmarks = new ArrayList<>();
    }

    // Обзор сервисов
    public List<DiscoverItems.Item> getServiceDiscoverItems() {
        return serviceDiscoverItems;
    }

    public void setServiceDiscoverItems(List<DiscoverItems.Item> serviceDiscoverItems) {
        this.serviceDiscoverItems = serviceDiscoverItems;
    }

    public void clearServiceDiscoverItems() {
        this.serviceDiscoverItems = new ArrayList<>();
    }

    // Работа с базой данных
    public void initializeDatabase(Context context){
        jConnectDbContract.initializeDbHelper(context);
    }

    public void closeDatabase(){
        jConnectDbContract.closeDbHelper();
    }

    public long insertAccount(String serverName, String login, String password, int port, int selected){
        return jConnectDbContract.insertAccount(serverName, login, password, port, selected);
    }

    public List<Account> getAccounts(){
        return jConnectDbContract.getAccounts();
    }

    public Account getSelectedAccount(){
        return jConnectDbContract.getSelectedAccount();
    }

    public void updateAccount(Account account){
        jConnectDbContract.updateAccount(account);
    }

    public void deleteAccount(int id){
        jConnectDbContract.deleteAccount(id);
    }

    public void deleteAccounts(List<Account> accounts){
        jConnectDbContract.deleteAccounts(accounts);
    }
}
