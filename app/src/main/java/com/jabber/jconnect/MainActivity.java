package com.jabber.jconnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ContactFragment.OnListFragmentInteractionListener,
        ChatFragment.OnFragmentInteractionListener, MucFragment.OnMucListFragmentInteractionListener,
        MucChatFragment.OnMucChatFragmentInteractionListener, BookmarksDialogFragment.NoticeBookmarksDialogListener{

    /***********************************************************************************************
     * Реализация связи с сервисом через Messenger
     **********************************************************************************************/
    Messenger msgService;
    boolean mBound = false;
    //Messenger для обработки сообщений сервиса
    Messenger replyMessenger = new Messenger(new HandlerReplyMsg());

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) { mBound = false; }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBound = true;
            msgService = new Messenger(service);

            Bundle b = new Bundle();
            b.putString("activity", "main");
            Message message = new Message();
            message.setData(b);
            message.replyTo = replyMessenger; // адрес обратной связи

            try {
                msgService.send(message); // отправка пустого сообщения для обратной связи с сервисом
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    // Отправка сообщений сервису
    private void sendMessage(Bundle b){
        android.os.Message m = new android.os.Message();
        m.setData(b);

        try {
            msgService.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**********************************************************************************************/

    // Класс обработчика сообщений сервиса
    class HandlerReplyMsg extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            if(bundle.getString("recieve") != null) {
                if(fragmentChat != null && selectedContactJid.equals(bundle.getString("recieve"))) {
                    fragmentChat.setChatView(xmppData.getMessagesList(bundle.getString("recieve")));
                }
            }

            if(bundle.getString("recieve_muc") != null) {
                if(mucChatFragment != null && selectedMucId.equals(bundle.getString("recieve_muc"))) {
                    mucChatFragment.setMucChatView(xmppData.getMucMessagesList(bundle.getString("recieve_muc")));
                }
            }

            if(bundle.getString("muc_list_update") != null){
                if(mucFragment != null){
                    mucFragment.updateMucList(xmppData.getMucList());
                }
            }

            if(bundle.getString("muc_participant_list_updated") != null){
                if(mucChatFragment != null && selectedMucId.equals(bundle.getString("muc_participant_list_updated"))){
                    mucChatFragment.updateMucParticipantsList(xmppData.getMucParticipantList(selectedMucId));
                }
            }

            if("loaded".equals(bundle.getString("bookmarks"))){
                MainActivity.this.showBookmarks();
            }

            if(bundle.getString("roster") != null) {
                String str = bundle.getString("roster");
                List<Contact> contacts = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    JSONArray ar = new JSONArray((String)jsonObject.get("ContactList"));

                    for(int i=0; i<ar.length(); i++){
                        String[] attr = String.valueOf(ar.get(i)).split(",");
                        Contact c = new Contact(attr[0]);
                        c.setName(attr[1]);
                        c.setGroup(attr[2]);
                        contacts.add(c);
                    }

                    fragmentContact.updateContacts(contacts);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    FragmentManager fm;

    // Фрагмент списка контактов
    ContactFragment fragmentContact;
    // Фрагмент чата
    ChatFragment fragmentChat;

    // Фрагмент списка комнат
    MucFragment mucFragment;
    // Фрагмент чата комнаты
    MucChatFragment mucChatFragment;

    // Показ чат-фрагмента
    boolean showWithChatFragment;

    // Параметры для восстановления ChatFragment
    String selectedContactJid;

    // Параметры для восстановления MucChatFragment
    String selectedMucId;

    // Сообщение для отправки
    String sendMsg;

    // Параметры ChatActivity
    boolean showChatActivity = false;
    // Request code для ChatActivity
    private static final int SHOW_CHAT_ACTIVITY = 1;

    // Параметры MucChatActivity
    boolean showMucChatActivity = false;
    private static final int SHOW_MUC_CHAT_ACTIVITY = 10;

    // Диалоговое окно выбора закладок
    BookmarksDialogFragment bookmarksDialogFragment;

    // Participants Drawer
    private ListView mDrawerListView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState != null) {
            // Восстановление значений сохраненных в savedInstanceState
            selectedContactJid = savedInstanceState.getString("jid");
            selectedMucId = savedInstanceState.getString("muc_id");
            showChatActivity = savedInstanceState.getBoolean("show_chat_activity");
            sendMsg = savedInstanceState.getString("send_msg");
        }

        // Подключение фрагментов
        fm = getSupportFragmentManager();

        fragmentContact = (ContactFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (fragmentContact == null) {
            fragmentContact = new ContactFragment();
            fm.beginTransaction().add(R.id.fragmentContainer, fragmentContact).commit();
        }

        mucFragment = (MucFragment) fm.findFragmentById(R.id.fragmentMucContainer);
        if (mucFragment == null) {
            mucFragment = new MucFragment();
            fm.beginTransaction().add(R.id.fragmentMucContainer, mucFragment).commit();
        }

        // Определяем условия, необходимые для показа 2-х фрагментов одновременно
        showWithChatFragment = (findViewById(R.id.fragmentChatContainer) != null) &&
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // Если выбран контакт, показываем чат
        if(selectedContactJid != null){
            showChat();
        }

        if(selectedMucId != null){
            showMucChat();
        }
    }

    // Сохранение параметров для восстановления активности
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("jid", selectedContactJid);
        state.putString("muc_id", selectedMucId);
        state.putBoolean("show_chat_activity", showChatActivity);
        state.putString("send_msg", sendMsg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Обработка результатов ChatActivity
        if(requestCode == SHOW_CHAT_ACTIVITY){
            // Была нажата кнопка Back, чат закрыт
            if(resultCode == ChatActivity.CHAT_ACTIVITY_BACK_PRESSED){
                showChatActivity = false;
                selectedContactJid = null;
                sendMsg = "";

                Fragment f = fm.findFragmentById(R.id.fragmentChatContainer);
                if(f != null) {
                    fm.beginTransaction().remove(f).commit();
                }
            }

            // ChatActivity уничтожена при смене ориентации экрана
            if(resultCode == ChatActivity.SAVE_CHAT_ACTIVITY){
                /*
                * Если результат поля для ввода сообщения не пустой - сохраняем в переменную
                * onActivityResult выдает результат 2 раза, 1-й сохраняем, 2-й всегда null
                */
                if (data.getStringExtra("send_msg") != null){
                    sendMsg = data.getStringExtra("send_msg");
                }

                /*
                * Восстанавливаем поле ввода сообщения, если ChatFragment прорисован,
                * иначе передаем значения активности
                */
                if(showWithChatFragment && (fragmentChat != null)){
                    fragmentChat.setChatView(xmppData.getMessagesList(selectedContactJid));
                    fragmentChat.setSendMsgView(sendMsg);
                } else {
                    startActivityForResult(new Intent(this, ChatActivity.class)
                                    .putExtra("jid", selectedContactJid)
                                    .putExtra("send_msg", sendMsg),
                            SHOW_CHAT_ACTIVITY);
                }
            }
        }

        // Обработка результатов MucChatActivity
        if(requestCode == SHOW_MUC_CHAT_ACTIVITY){
            // Была нажата кнопка Back, чат закрыт
            if(resultCode == MucChatActivity.MUC_CHAT_ACTIVITY_BACK_PRESSED){
                showChatActivity = false;
                selectedMucId = null;
                sendMsg = "";

                Fragment f = fm.findFragmentById(R.id.fragmentChatContainer);
                if(f != null) {
                    fm.beginTransaction().remove(f).commit();
                }
            }

            // ChatActivity уничтожена при смене ориентации экрана
            if(resultCode == MucChatActivity.SAVE_MUC_CHAT_ACTIVITY){
                /*
                * Если результат поля для ввода сообщения не пустой - сохраняем в переменную
                * onActivityResult выдает результат 2 раза, 1-й сохраняем, 2-й всегда null
                */
                if (data.getStringExtra("send_msg") != null){
                    sendMsg = data.getStringExtra("send_msg");
                }

                /*
                * Восстанавливаем поле ввода сообщения, если ChatFragment прорисован,
                * иначе передаем значения активности
                */
                if(showWithChatFragment && (mucChatFragment != null)){
                    mucChatFragment.setMucChatView(xmppData.getMucMessagesList(selectedMucId));
                    mucChatFragment.setSendMsgView(sendMsg);
                } else {
                    startActivityForResult(new Intent(this, MucChatActivity.class)
                                    .putExtra("muc_id", selectedMucId)
                                    .putExtra("send_msg", sendMsg),
                            SHOW_MUC_CHAT_ACTIVITY);
                }
            }
        }
    }

    // Показ ChatFragment в главной или отдельной активности в зависимости от значения showWithChatFragment
    public void showChat(){
        if(showWithChatFragment) {
            Fragment f = fm.findFragmentById(R.id.fragmentChatContainer);
            if(f instanceof ChatFragment){
                fragmentChat = (ChatFragment) fm.findFragmentById(R.id.fragmentChatContainer);
            }

            /*
            * Если ChatFragment не существует, создаем объект,
            * если создавался ранее, передаем ему агрументы
            */
            if(fragmentChat == null){
                fragmentChat = ChatFragment.newInstance(selectedContactJid, sendMsg);
                fm.beginTransaction().add(R.id.fragmentChatContainer, fragmentChat).commit();
            } else {
                fragmentChat.setJid(selectedContactJid);
                fragmentChat.setSendMsg(sendMsg);
            }
        } else {
            /*
            * Создаем ChatActivity, если вызывается впервые (showChatActivity == false),
            * если устройство планшет - создаем всегда (else + isTablet() = планшет в вертикальном
            * положении
            *
            * если showChatActivity == true, активность уже запускалась, следующий вызов делается в
            * onActivityResult с передачей сохраненных параметров
            */
            if(!showChatActivity || isTablet()){
                showChatActivity = true;
                startActivityForResult(new Intent(this, ChatActivity.class)
                                .putExtra("jid", selectedContactJid)
                                .putExtra("send_msg", sendMsg),
                        SHOW_CHAT_ACTIVITY);
            }
        }
    }

    // Показ MucChatFragment в главной или отдельной активности в зависимости от значения showWithChatFragment
    public void showMucChat(){
        if(showWithChatFragment) {
            Fragment f = fm.findFragmentById(R.id.fragmentChatContainer);
            if(f instanceof MucChatFragment){
                mucChatFragment = (MucChatFragment) fm.findFragmentById(R.id.fragmentChatContainer);
            }

            /*
            * Если MucChatFragment не существует, создаем объект,
            * если создавался ранее, передаем ему агрументы
            */
            if(mucChatFragment == null){
                mucChatFragment = MucChatFragment.newInstance(selectedMucId, sendMsg);
                fm.beginTransaction().add(R.id.fragmentChatContainer, mucChatFragment).commit();
            } else {
                mucChatFragment.setMucId(selectedMucId);
                mucChatFragment.setSendMsg(sendMsg);
            }
        } else {
            /*
            * Создаем MucChatActivity, если вызывается впервые (showMucChatActivity == false),
            * если устройство планшет - создаем всегда (else + isTablet() = планшет в вертикальном
            * положении
            *
            * если showMucChatActivity == true, активность уже запускалась, следующий вызов делается в
            * onActivityResult с передачей сохраненных параметров
            */
            if(!showChatActivity || isTablet()){
                showChatActivity = true;
                startActivityForResult(new Intent(this, MucChatActivity.class)
                                .putExtra("muc_id", selectedMucId)
                                .putExtra("send_msg", sendMsg),
                        SHOW_MUC_CHAT_ACTIVITY);
            }
        }
    }

    // Определяем, является ли устройство планшетом
    private boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Подключаемся к сервису при выходе активности на передний план
        bindService(new Intent(this, XmppService.class), mConnection, Context.BIND_ABOVE_CLIENT);
        onStartService();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Отключаемся от сервиса при уходе активности с переднего плана
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.menu_start_service:
                onStartService();
                break;
            case R.id.menu_stop_service:
                onStopService();
                break;
            case R.id.menu_connect:
                onConnect();
                break;
            case R.id.menu_disconnect:
                onDisconnect();
                break;
            case R.id.menu_join_muc:
                joinToMuc();
                break;
            case R.id.menu_leave_muc:
                leaveMuc();
                break;
            case R.id.menu_bookmarks_muc:
                sendRequestMessage("bookmarks_request");
                break;
            case R.id.menu_service_discover:
                startActivity(new Intent(this, ServiceDiscoveryActivity.class));
                break;
            case R.id.menu_muc_participant_list:
                if(mucChatFragment != null){
                    mucChatFragment.switchParticipantList();
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // Реализация методов меню
    public void onStartService() {
        if (!mBound) {
            Intent intent = new Intent(this, XmppService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
        }
    }

    public void onStopService() {
        if (mBound) {
            sendRequestMessage("stop");
        }
    }

    public void onConnect() {
        if (mBound) {
            sendRequestMessage("connect");
        }
    }

    public void onDisconnect() {
        if (mBound) {
            sendRequestMessage("disconnect");
        }
    }

    public void joinToMuc() {
        if (mBound) {
            sendRequestMessage("join_muc");
        }
    }

    public void leaveMuc() {
        if (mBound) {
            //sendRequestMessage("leave_muc");
            if(selectedMucId != null){
                Bundle b = new Bundle();
                b.putString("leave_muc", selectedMucId);
                sendMessage(b);
            }
        }
    }
    ///////////////////////////////////////////////////////////////////

    // Реализация методов интерфейса mListener во фрагменте ContactFragment
    @Override
    public void onListFragmentInteraction(Contact c) {
        // Сохраняем выбранный контакт в переменную
        selectedContactJid = c.getJid();
        selectedMucId = null;

        if(mucChatFragment != null) {
            fm.beginTransaction().remove(mucChatFragment).commit();
            mucChatFragment = null;
        }

        if(fragmentChat != null){
            fragmentChat.setJidView(selectedContactJid);
            fragmentChat.setChatView(xmppData.getMessagesList(selectedContactJid));
            fragmentChat.setSendMsgView("");
        } else {
            showChat();
        }
    }

    // Реализация методов интерфейса mListener во фрагменте MucFragment
    @Override
    public void onMucListFragmentInteraction(String item) {
        selectedMucId = item;
        selectedContactJid = null;

        if(fragmentChat != null) {
            fm.beginTransaction().remove(fragmentChat).commit();
            fragmentChat = null;
        }

        if(mucChatFragment != null){
            mucChatFragment.setMucIdView(selectedMucId);
            mucChatFragment.setMucChatView(xmppData.getMucMessagesList(selectedMucId));
            mucChatFragment.setSendMsgView("");
            mucChatFragment.updateMucParticipantsList(xmppData.getMucParticipantList(selectedMucId));
        } else {
            showMucChat();
        }
    }

    // Реализация методов интерфейса mListener во фрагменте ChatFragment
    /*@Override
    public void setChat(String chat) {
        this.chat = chat;
    }*/

    @Override
    public void setSendMsg(String sendMsg) {
        this.sendMsg = sendMsg;
    }

    @Override
    public void sendMessage(String jid) {
        Bundle b = new Bundle();
        b.putString("send", jid);
        sendMessage(b);
    }

    // Реализация методов интерфейса mListener во фрагменте MucChatFragment
    @Override
    public void setMucChatSendMsg(String sendMsg) {
        this.sendMsg = sendMsg;
    }

    @Override
    public void sendMucChatMessage(String mucId) {
        Bundle b = new Bundle();
        b.putString("send_muc", mucId);
        sendMessage(b);
    }

    // Реализация методов интерфейса mListener во фрагменте BookmarksDialogFragment
    @Override
    public void onBookmarksDialogInteraction(String item) {
        bookmarksDialogFragment.dismiss();

        Bundle b = new Bundle();
        b.putString("join_muc_from_bookmarks", item);
        sendMessage(b);
    }
    ///////////////////////////////////////////////////////////////////

    // Метод для отсылки команд сервису
    private void sendRequestMessage(String request){
        android.os.Message m = new android.os.Message();
        Bundle b = new Bundle();
        b.putString("request", request);
        m.setData(b);

        try {
            msgService.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void showBookmarks(){
        bookmarksDialogFragment = new BookmarksDialogFragment();
        bookmarksDialogFragment.show(fm, "bookmarks_dialog_fragment");
    }
}
