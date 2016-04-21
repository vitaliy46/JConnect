package com.jabber.jconnect;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.bookmarks.BookmarkManager;
import org.jivesoftware.smackx.bookmarks.BookmarkedConference;
import org.jivesoftware.smackx.bookmarks.Bookmarks;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class XmppService extends Service {

    AbstractXMPPConnection connection = null;
    private int connectionStatus = 0;
    private final int NOT_CONNECTED = 0;
    private final int TO_CONNECT = 1;
    private final int CONNECTED = 2;
    private final int TO_DISCONNECT = 3;
    Connection cn;

    // Контакты
    List<Contact> ContactList = new ArrayList<>();

    // Чаты
    ChatManager chatManager;
    Map<String, Chat> chatMap = new HashMap<>();

    // Конференции
    MultiUserChatManager manager;
    //MultiUserChat muc;
    List<String> mucList = new ArrayList<>();
    Map<String, MultiUserChat> mucMap = new HashMap<>();

    // Закладки
    Map<String, BookmarkedConference> bookmarkedConferenceMap = new HashMap<>();

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    /***********************************************************************************************
    * Класс обработчика сообщений активностей
    ***********************************************************************************************/
    class IncomingHandler extends android.os.Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            Bundle msgBundle = msg.getData();

            // инициализация replyMessanger (replyTo - адрес обратной связи)
            if(msg.replyTo != null) {
                replyMessenger = msg.replyTo;
                if("main".equals(msgBundle.getString("activity"))) {
                    XmppService.this.sendContactsToActivity();
                }
            }

            String str = msgBundle.getString("request");
            if (str != null){
                switch(str){
                    case "stop":
                        stopService();
                        break;
                    case "connect":
                        connect();
                        break;
                    case "disconnect":
                        disconnect();
                        break;
                    case "join_muc":
                        XmppService.this.joinToMuc("tty0@conference.jabber.ru", "nic");
                        break;
                    case "leave_muc":
                        XmppService.this.leaveMuc("tty0@conference.jabber.ru");
                        break;
                    case "bookmarks_request":
                        XmppService.this.getBookmarkedConference();
                        break;
                    default:
                        break;
                }
            }

            if(msgBundle.getString("send") != null){
                XmppService.this.sendMessage(msgBundle.getString("send"), xmppData.getMessageToSend());
            }

            if(msgBundle.getString("send_muc") != null){
                XmppService.this.sendMucMessage(msgBundle.getString("send_muc"), xmppData.getMessageToSend());
            }

            if(msgBundle.getString("join_muc") != null){
                XmppService.this.joinToMuc(msgBundle.getString("join_muc"),
                        bookmarkedConferenceMap.get(msgBundle.getString("join_muc")).getNickname());
            }

            if(msgBundle.getString("leave_muc") != null){
                XmppService.this.leaveMuc(msgBundle.getString("leave_muc"));
            }
        }
    }
    // Messenger для отправки сообщений в активность
    Messenger replyMessenger = null;

    // Messenger для приема сообщений к сервису
    Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return messenger.getBinder();
    }
    /**********************************************************************************************/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Вывод сервиса на передний план
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification mNotification = new NotificationCompat.Builder(this)
                .setContentTitle("Connect to jabber service")
                .setTicker("Connect to jabber service")
                .setContentText("Connect to jabber service")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(6711, mNotification);

        return Service.START_STICKY;
    }

    // Остановка сервиса с закрытием соединения
    public void stopService(){
        if(connection != null){
            disconnect();
        }
        stopForeground(true);
        stopSelf();
    }

    /***********************************************************************************************
    * Соединение с Jabber сервером
    ***********************************************************************************************/
    public void connect(){
        connectionStatus = TO_CONNECT;
        cn = new Connection();
        cn.execute();
    }

    public void disconnect(){
        connectionStatus = TO_DISCONNECT;
        cn = new Connection();
        cn.execute();
    }

    class Connection extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            switch(connectionStatus){
                case TO_CONNECT:
                    connect();
                    connectionStatus = CONNECTED;
                    break;
                case TO_DISCONNECT:
                    disconnect();
                    connectionStatus = NOT_CONNECTED;
                    break;
                default:
                    break;
            }

            return null;
        }

        public void connect(){
            // Доверять всем сертификатам, в том числе просроченным
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {return null;}
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType){}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType){}
                    }
            };

            SSLContext sc = null;
            try {
                sc = SSLContext.getInstance("SSL");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            try {
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }


            // Конфигурирование соединения
            /*XMPPTCPConnectionConfiguration configBuilder = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword("vitaliy46", ";tkfnby2+")
                .setResource("jabber.ru")
                .setServiceName("jabber.ru")
                .setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible)
                .setCustomSSLContext(sc)
                .build();*/

            XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
            configBuilder.setUsernameAndPassword("vitaliy446", ";tkfnby1");
            configBuilder.setResource("jabber.ru");
            configBuilder.setServiceName("jabber.ru");
            configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible);
            configBuilder.setCustomSSLContext(sc);

            /*XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
            configBuilder.setUsernameAndPassword("vitaliy46_2", "top02bg");
            configBuilder.setResource("jabber.org.by");
            configBuilder.setServiceName("jabber.org.by");
            configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible);
            configBuilder.setCustomSSLContext(sc);*/

            /*XMPPTCPConnectionConfiguration configBuilder = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword("vitaliy55", "sshti8")
                    .setResource("qip.ru")
                    .setServiceName("qip.ru")
                    .setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible)
                    .setCustomSSLContext(sc)
                    .build();*/

            connection = new XMPPTCPConnection(configBuilder.build());
            //connection = new XMPPTCPConnection(configBuilder);

            // Соединение с jabber сервером
            try {
                connection.connect();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                e.printStackTrace();
            }

            // Авторизация
            try {
                connection.login();
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(connection.isConnected() && connection.isAuthenticated()){
                // Регистрация слушателей
                chatManager = ChatManager.getInstanceFor(connection);
                chatManager.addChatListener(new ChatManagerListener() {
                    @Override
                    public void chatCreated(Chat chat, boolean b) {
                        Pattern pJid = Pattern.compile("(\\S+)@(\\S+\\.)(\\w+)");
                        Matcher m = pJid.matcher(chat.getParticipant());
                        String jid = null;
                        if(m.find()){
                            jid = m.group();
                        }
                        final String finalJid = jid;

                        //XmppService.this.chat = chat;
                        if(chatMap.get(jid) == null) {
                            chatMap.put(jid, chat);
                        }

                        chat.addMessageListener(new ChatMessageListener() {
                            @Override
                            public void processMessage(Chat chat, Message message) {
                                xmppData.setMessagesList(finalJid, finalJid, message.getBody());

                                android.os.Message chatMsg = new android.os.Message();

                                Bundle bundle = new Bundle();
                                bundle.putString("recieve", finalJid);
                                chatMsg.setData(bundle);

                                try {
                                    replyMessenger.send(chatMsg);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });

                try {
                    Roster.getInstanceFor(connection).reloadAndWait();
                } catch (SmackException.NotLoggedInException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                XmppService.this.setContactList();
                XmppService.this.sendContactsToActivity();
            }
        }

        // Закрытие соединения
        public void disconnect(){
            if(connection != null && connection.isConnected()) connection.disconnect();
        }
    }

    private void sendTextMessage(String text){
        android.os.Message m = new android.os.Message();
        Bundle b = new Bundle();
        b.putString("text", text);
        m.setData(b);

        try {
            replyMessenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getConnectionStatus(){
        if((connection != null) && connection.isConnected()) return "Подключен";

        return "Разъединен";
    }

    public String isAuthenticated(){
        if((connection != null) && connection.isAuthenticated()) return "Авторизован";

        return "Отключен";
    }

    private void setContactList(){
        if(connection != null && connection.isAuthenticated()){
            Roster roster = Roster.getInstanceFor(connection);
            Collection<RosterEntry> entries = roster.getEntries();

            Pattern pJid = Pattern.compile("(\\S+)@(\\S+\\.)(\\w+)");

            String jid;
            String name;
            String group;

            String str;

            for (RosterEntry entry : entries) {
                str = entry.toString();

                // Jabber ID
                Matcher m = pJid.matcher(str);
                if(m.find()){
                    jid = m.group();
                    Contact c = new Contact(jid);

                    // Nick
                    if(str.indexOf(':') != -1) {
                        name = str.substring(0, str.indexOf(':'));
                        c.setName(name);
                    }

                    // группа контакта в ростере
                    if(str.lastIndexOf('[') != -1) {
                        group = str.substring(str.lastIndexOf('[') + 1, str.lastIndexOf(']'));
                        c.setGroup(group);
                    }

                    ContactList.add(c);
                }
            }
        }
    }

    public void sendContactsToActivity(){
        if(connection != null && connection.isAuthenticated()){
            android.os.Message contactsMsg = new android.os.Message();
            Bundle bundle = new Bundle();

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("ContactList", ContactList);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            bundle.putString("roster", jsonObject.toString());
            contactsMsg.setData(bundle);
            try {
                replyMessenger.send(contactsMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Отправка сообщений в чат
    public void sendMessage(String jid, String message){
        if(connection != null && connection.isAuthenticated()){
            if(chatMap.get(jid) == null){
                chatMap.put(jid, chatManager.createChat(jid));
            }
            Chat chat = chatMap.get(jid);

            try {
                chat.sendMessage(xmppData.getMessageToSend());
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    // Вход в конференцию
    private void joinToMuc(final String mucId, String nick){
        if(connection != null && connection.isAuthenticated()){
            // Соединение с комнатой
            manager = MultiUserChatManager.getInstanceFor(connection);

            MultiUserChat muc = manager.getMultiUserChat(mucId);

            try {
                // Вход в комнату
                muc.join(nick);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }

            if(muc.isJoined()){
                // Слушатель комнаты
                muc.addMessageListener(new MessageListener() {
                    @Override
                    public void processMessage(Message message) {
                        String[] msgFrom = message.getFrom().split("/");
                        xmppData.setMucMessagesList(mucId, msgFrom[1], message.getBody());

                        android.os.Message mucChatMsg = new android.os.Message();

                        Bundle bundle = new Bundle();
                        bundle.putString("recieve_muc", mucId);
                        mucChatMsg.setData(bundle);

                        try {
                            replyMessenger.send(mucChatMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });

                // Добавление в список комнат
                mucList.add(mucId);
                // Добавление в коллекцию комнат
                mucMap.put(mucId, muc);

                // Обновление списка комнат в синглетоне
                xmppData.setMucList(mucList);

                // Оповещение активности об обновлении списка комнат
                Bundle bundle = new Bundle();
                bundle.putString("muc_list_update", "join");
                android.os.Message mucUpdate = new android.os.Message();
                mucUpdate.setData(bundle);
                try {
                    replyMessenger.send(mucUpdate);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Отправка сообщения в конференцию
    private void sendMucMessage(String mucId, String message){
        if(connection != null && connection.isAuthenticated()){
            MultiUserChat muc = mucMap.get(mucId);

            if(muc.isJoined()){
                try {
                    muc.sendMessage(message);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Покинуть конференцию
    private void leaveMuc(String mucId){
        if(connection != null && connection.isAuthenticated()){
            MultiUserChat muc = mucMap.get(mucId);

            if(muc.isJoined()){
                try {
                    // Выход из комнаты
                    muc.leave();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                // Удаление комнаты из списка
                mucList.remove(mucId);
                // Удаление комнаты из коллекции
                mucMap.remove(mucId);

                // Обновление списка комнат в синглетоне
                xmppData.setMucList(mucList);

                // Оповещение активности об обновлении списка комнат
                Bundle bundle = new Bundle();
                bundle.putString("muc_list_update", "leave");
                android.os.Message mucUpdate = new android.os.Message();
                mucUpdate.setData(bundle);
                try {
                    replyMessenger.send(mucUpdate);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Сохранение закладок (комнат) в синглетон
    private void getBookmarkedConference(){
        if(connection != null && connection.isAuthenticated()){
            BookmarkManager bm = null;
            try {
                bm = BookmarkManager.getBookmarkManager(connection);
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            }

            List<BookmarkedConference> rooms = null;
            if(bm != null){
                try {
                    rooms = bm.getBookmarkedConferences();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }

            if(rooms != null){
                for(BookmarkedConference bookmarkedConference:rooms){
                    bookmarkedConferenceMap.put(bookmarkedConference.getJid(), bookmarkedConference);
                }

                xmppData.setBookmarkedConferenceList(rooms);
                Bundle bundle = new Bundle();
                bundle.putString("bookmarks", "loaded");
                android.os.Message bookmarks = new android.os.Message();
                bookmarks.setData(bundle);
                try {
                    replyMessenger.send(bookmarks);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
