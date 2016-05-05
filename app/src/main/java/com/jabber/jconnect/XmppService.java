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

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
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
import org.jivesoftware.smackx.disco.NodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.InvitationRejectionListener;
import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCNotJoinedException;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smackx.pep.packet.PEPEvent;
import org.jivesoftware.smackx.pep.packet.PEPItem;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
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
    List<String> mucList = new ArrayList<>();
    Map<String, MultiUserChat> mucMap = new HashMap<>();
    RoomInfo roomInfo = null;

    // Закладки
    Map<String, BookmarkedConference> bookmarkedConferenceMap = new HashMap<>();

    // Обзор сервисов
    ServiceDiscoveryManager serviceDiscoveryManager = null;

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

                    Bundle bundle = new Bundle();
                    bundle.putString("muc_list_update", "resume");
                    XmppService.this.sendMessage(bundle);
                }

                if("service_discover".equals(msgBundle.getString("activity"))) {
                    Bundle bundle = new Bundle();
                    bundle.putString("parent_entity_id", "jabber.ru");
                    XmppService.this.sendMessage(bundle);
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
                        //XmppService.this.joinToMuc("tty0@conference.jabber.ru", "nic");
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

            if(msgBundle.getString("join_muc_from_bookmarks") != null){
                XmppService.this.joinToMuc(msgBundle.getString("join_muc_from_bookmarks"),
                        bookmarkedConferenceMap.get(msgBundle.getString("join_muc_from_bookmarks")).getNickname(), null);
            }

            if(msgBundle.getString("join_muc_from_service_discover") != null){
                XmppService.this.joinToMuc(msgBundle.getString("join_muc_from_service_discover"),
                        msgBundle.getString("join_muc_from_service_discover_nick"),
                        msgBundle.getString("join_muc_from_service_discover_password"));
            }

            if(msgBundle.getString("leave_muc") != null){
                XmppService.this.leaveMuc(msgBundle.getString("leave_muc"));
            }

            if(msgBundle.getString("service_discover_request") != null){
                XmppService.this.getServiceDiscoverItems(msgBundle.getString("service_discover_request"));
            }

            if(msgBundle.getString("request_muc_protection_info") != null){
                XmppService.this.getMucProtectionInfo(msgBundle.getString("request_muc_protection_info"));
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

    // Отправка сообщений сервису
    private void sendMessage(Bundle b){
        android.os.Message m = new android.os.Message();
        m.setData(b);

        try {
            replyMessenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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

            //ProviderManager.addExtensionProvider("captcha", "urn:xmpp:captcha", new CaptchaExtension.Provider());
            //ProviderManager.addExtensionProvider("data", "urn:xmpp:bob", new BobExtension.Provider());
            //CaptchaExtension

            // Конфигурирование соединения
            /*XMPPTCPConnectionConfiguration configBuilder = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword("", "")
                .setResource("")
                .setServiceName("")
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
            configBuilder.setUsernameAndPassword("", "");
            configBuilder.setResource("");
            configBuilder.setServiceName("");
            configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible);
            configBuilder.setCustomSSLContext(sc);*/

            /*XMPPTCPConnectionConfiguration configBuilder = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword("", "")
                    .setResource("")
                    .setServiceName("")
                    .setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.ifpossible)
                    .setCustomSSLContext(sc)
                    .build();*/

            connection = new XMPPTCPConnection(configBuilder.build());
            //connection = new XMPPTCPConnection(configBuilder);

            connection.setPacketReplyTimeout(10000);

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

                // Менеджер конференций
                manager = MultiUserChatManager.getInstanceFor(connection);
                // Менеджер сервисов
                serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

                /*StanzaListener stanzaListener = new StanzaListener() {
                    @Override
                    public void processPacket(Stanza stanza) throws SmackException.NotConnectedException {
                        Log.i("stanza", stanza.toXML().toString());
                    }
                };
                StanzaFilter stanzaFilter = new StanzaFilter() {
                    @Override
                    public boolean accept(Stanza stanza) {
                        return true;
                    }
                };
                connection.addPacketInterceptor(stanzaListener, stanzaFilter);*/

                /*manager.addInvitationListener(new InvitationListener() {
                    @Override
                    public void invitationReceived(XMPPConnection xmppConnection, MultiUserChat multiUserChat,
                                                   String s, String s1, String s2, Message message) {
                        Log.i("message", message.toXML().toString());
                        Log.i("getExtensions", message.getExtensions().toString());
                        Log.i("s", s);
                        Log.i("s1", s1);
                        Log.i("s2", s2);
                    }
                });*/
            }
        }

        // Закрытие соединения
        public void disconnect(){
            if(connection != null && connection.isConnected()) connection.disconnect();
        }
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

    private void getMucProtectionInfo(String mucID){
        try {
            roomInfo = manager.getRoomInfo(mucID);
        } catch (SmackException.NoResponseException e) {
            e.printStackTrace();
        } catch (XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        }
        if(roomInfo != null){
            Bundle bundle = new Bundle();

            if(roomInfo.isPasswordProtected()){
                bundle.putString("muc_is_password_protected", mucID);
            }

            if(roomInfo.isMembersOnly()){
                bundle.putString("muc_is_members_only", mucID);
            }

            sendMessage(bundle);
        }
    }

    // Вход в конференцию
    private void joinToMuc(final String mucId, final String nick, final String password){
        if(connection != null && connection.isAuthenticated()){
            // Соединение с комнатой
            final MultiUserChat muc = manager.getMultiUserChat(mucId);
            xmppData.clearMucParticipantList(mucId);

            // Прием сообщений об изменении статуса
            muc.addParticipantListener(new PresenceListener() {
                @Override
                public void processPresence(Presence presence) {
                    //Log.i("participant_listener", presence.toXML().toString());

                    String[] from = presence.getFrom().split("/");
                    //Log.i("from", from[1]);
                    MucParticipant mucParticipant = new MucParticipant(from[1]);

                    XmlPullParserFactory factory = null;
                    try {
                        factory = XmlPullParserFactory.newInstance();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }
                    if(factory != null){
                        factory.setNamespaceAware(true);

                        XmlPullParser xpp = null;
                        try {
                            xpp = factory.newPullParser();
                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        }
                        if(xpp != null){
                            try {
                                xpp.setInput(new StringReader(presence.toXML().toString()));
                            } catch (XmlPullParserException e) {
                                e.printStackTrace();
                            }

                            int eventType = 0;
                            try {
                                eventType = xpp.getEventType();
                            } catch (XmlPullParserException e) {
                                e.printStackTrace();
                            }
                            boolean extentionFound = false;
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                if(eventType ==  XmlPullParser.START_TAG &&
                                        xpp.getNamespace().equals("http://jabber.org/protocol/muc#user")){
                                    extentionFound = true;
                                }

                                if(extentionFound){
                                    if(eventType ==  XmlPullParser.START_TAG && xpp.getName().equals("item")){
                                        if(xpp.getAttributeValue(1) != null){
                                            //Log.i("role", xpp.getAttributeValue(1));
                                            mucParticipant.setRole(xpp.getAttributeValue(1));
                                        }
                                    }
                                }

                                try {
                                    eventType = xpp.next();
                                } catch (XmlPullParserException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    if(!("none".equals(mucParticipant.getRole()))){
                        xmppData.addMucParticipant(mucId, mucParticipant);
                    } else {
                        xmppData.delMucParticipant(mucId, mucParticipant.getNick());
                    }

                    Bundle bundle = new Bundle();
                    bundle.putString("muc_participant_list_updated", mucId);
                    sendMessage(bundle);
                }
            });

            // Запрос Captcha
            Stanza stanza = new Stanza() {
                @Override
                public CharSequence toXML() {
                    return "request";
                }
            };
            StanzaFilter stanzaFilter = new StanzaFilter() {
                @Override
                public boolean accept(Stanza stanza) {
                    return true;
                }
            };
            StanzaListener stanzaListener = new StanzaListener() {
                @Override
                public void processPacket(Stanza stanza) throws SmackException.NotConnectedException {
                    //Log.i("stanza", stanza.toXML().toString());
                    if(stanza instanceof Message){
                        Message message = (Message) stanza;

                        if(message.getExtension("captcha", "urn:xmpp:captcha") != null){
                            XmlPullParserFactory factory = null;
                            try {
                                factory = XmlPullParserFactory.newInstance();
                            } catch (XmlPullParserException e) {
                                e.printStackTrace();
                            }
                            if(factory != null){
                                factory.setNamespaceAware(true);

                                XmlPullParser xpp = null;
                                try {
                                    xpp = factory.newPullParser();
                                } catch (XmlPullParserException e) {
                                    e.printStackTrace();
                                }
                                if(xpp != null){
                                    try {
                                        xpp.setInput(new StringReader(message.toXML().toString()));
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    }

                                    int eventType = 0;
                                    boolean urlFound = false;
                                    try {
                                        eventType = xpp.getEventType();
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    }
                                    while (eventType != XmlPullParser.END_DOCUMENT) {
                                        if(eventType ==  XmlPullParser.START_TAG && xpp.getName().equals("url")){
                                            urlFound = true;
                                        }

                                        if(eventType ==  XmlPullParser.TEXT ){
                                            if(urlFound){
                                                Bundle bundle = new Bundle();
                                                bundle.putString("recieve_captcha", xpp.getText());
                                                sendMessage(bundle);

                                                urlFound = false;
                                            }
                                        }

                                        try {
                                            eventType = xpp.next();
                                        } catch (XmlPullParserException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            xmppData.setMucMessagesList(mucId, null, message.getBody());

                            Bundle bundle = new Bundle();
                            bundle.putString("recieve_muc", mucId);
                            sendMessage(bundle);
                        }
                    }
                }
            };
            try {
                connection.sendStanzaWithResponseCallback(stanza, stanzaFilter, stanzaListener);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Вход в комнату
                        muc.join(nick, password, null, 30000);
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
                                if(msgFrom.length > 1){
                                    xmppData.setMucMessagesList(mucId, msgFrom[1], message.getBody());
                                } else {
                                    xmppData.setMucMessagesList(mucId, "Тема", message.getSubject());
                                }

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
                    } else {
                        if(roomInfo != null){
                            if(roomInfo.isMembersOnly()){
                                Bundle bundle = new Bundle();
                                bundle.putString("not_authorized_muc_is_members_only", mucId);
                                sendMessage(bundle);
                            }
                        }
                    }
                }
            }).start();

            // Добавление в список комнат
            boolean mucIdFound = false;
            for(String addedMucId:mucList){
                if(addedMucId.equals(mucId)){
                    mucIdFound = true;
                }
            }
            if(!mucIdFound){
                mucList.add(mucId);
            }
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

            if(muc != null){
                if(muc.isJoined()){
                    try {
                        // Выход из комнаты
                        muc.leave();
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }

                    xmppData.clearMucParticipantList(mucId);
                }
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

    private void getServiceDiscoverItems(final String parentEntityID){
        if(serviceDiscoveryManager != null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DiscoverItems discoverItems = null;
                    try {
                        discoverItems = serviceDiscoveryManager.discoverItems(parentEntityID);
                    } catch (SmackException.NoResponseException e) {
                        e.printStackTrace();
                    } catch (XMPPException.XMPPErrorException e) {
                        e.printStackTrace();
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }

                    if(discoverItems != null){
                        xmppData.setServiceDiscoverItems(discoverItems.getItems());

                        Bundle bundle = new Bundle();
                        bundle.putString("service_discover_items", "loaded");
                        sendMessage(bundle);
                    }
                }
            }).start();
        }
    }
}
