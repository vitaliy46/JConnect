package com.jabber.jconnect;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ContactFragment.OnListFragmentInteractionListener,
        ChatFragment.OnFragmentInteractionListener, MucFragment.OnMucListFragmentInteractionListener,
        MucChatFragment.OnMucChatFragmentInteractionListener, BookmarksDialogFragment.BookmarksDialogListener {

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

            if("update".equals(bundle.getString("roster"))){
                if(fragmentContact != null){
                    fragmentContact.updateContacts(xmppData.getContactList());
                }
            }

            if("not_selected".equals(bundle.getString("error"))){
                String errorAccountNotSelected = getResources().getString(R.string.account_not_selected);
                Toast.makeText(getApplicationContext(), errorAccountNotSelected, Toast.LENGTH_LONG).show();
            }

            if(bundle.getString("muc_joined") != null){
                Toast.makeText(getApplicationContext(), bundle.getString("muc_joined") + ": " +
                        getResources().getString(R.string.muc_joined), Toast.LENGTH_SHORT)
                        .show();
            }

            String error = bundle.getString("toast");
            if(error != null){
                String toast = "";

                switch(error){
                    case "account_not_selected":
                        toast = getResources().getString(R.string.account_not_selected);
                        break;
                    case "not_authenticated":
                        toast = getResources().getString(R.string.not_authenticated);
                        break;
                    default:
                        break;
                }

                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
            }

            if(bundle.getString("not_authorized_muc_is_members_only") != null){
                String membersOnlyMsg = "Комната " + bundle.getString("not_authorized_muc_is_members_only") +
                        " только для зарегистрированных участников";
                Toast.makeText(getApplicationContext(), membersOnlyMsg, Toast.LENGTH_LONG).show();
            }

            if("update".equals(bundle.getString("bookmark"))){
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.bookmark_added_begin) + " " +
                                bundle.getString("muc_jid") + " " + getResources().getString(R.string.bookmark_added_end),
                        Toast.LENGTH_SHORT).show();
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
    // Диалоговое окно для добавления закладок
    BookmarksDialogFragment bookmarksDialogFragment;

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
    boolean showMucActivity = false;
    private static final int SHOW_MUC_CHAT_ACTIVITY = 10;

    Menu menu;

    // Application menu drawer
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
            showMucActivity = savedInstanceState.getBoolean("show_muc_activity");
            sendMsg = savedInstanceState.getString("send_msg");
        }

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_drawer, null);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, Color.WHITE);
        actionBar.setHomeAsUpIndicator(drawable);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_activity_drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, 0, 0) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //Toast.makeText(getApplicationContext(), "closed", Toast.LENGTH_SHORT).show();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //Toast.makeText(getApplicationContext(), "opened", Toast.LENGTH_SHORT).show();
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        NavigationView navigationView = (NavigationView) findViewById(R.id.main_activity_drawer_menu);

        if(navigationView != null){
            navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem item) {
                    int id = item.getItemId();
                    switch (id){
                        case R.id.main_activity_drawer_menu_service_discover:
                            mDrawerLayout.closeDrawer(Gravity.LEFT);
                            startActivity(new Intent(MainActivity.this, ServiceDiscoveryActivity.class));
                            break;
                        case R.id.main_activity_drawer_menu_bookmarks:
                            mDrawerLayout.closeDrawer(Gravity.LEFT);
                            sendRequestMessage("bookmarks_request");
                            break;
                        case R.id.main_activity_drawer_menu_accounts:
                            mDrawerLayout.closeDrawer(Gravity.LEFT);
                            startActivity(new Intent(MainActivity.this, AccountsActivity.class));
                            break;
                        case R.id.main_activity_drawer_menu_exit:
                            mDrawerLayout.closeDrawer(Gravity.LEFT);
                            if (mBound) {
                                sendRequestMessage("stop");
                            }
                            finish();
                            break;
                        default:
                            break;
                    }

                    return false;
                }
            });
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // Сохранение параметров для восстановления активности
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("jid", selectedContactJid);
        state.putString("muc_id", selectedMucId);
        state.putBoolean("show_chat_activity", showChatActivity);
        state.putBoolean("show_muc_activity", showMucActivity);
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
                //selectedContactJid = null;
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
                showMucActivity = false;
                //selectedMucId = null;
                if(menu != null){
                    menu.setGroupVisible(R.id.muc_menu, false);
                }
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

            if(menu != null){
                menu.setGroupVisible(R.id.muc_menu, true);
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
            if(!showMucActivity || isTablet()){
                showMucActivity = true;
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
        if (!mBound) {
            Intent intent = new Intent(this, XmppService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
        }

        if(fragmentContact != null){
            fragmentContact.updateContacts(xmppData.getContactList());
        }
        if(mucFragment != null){
            mucFragment.updateMucList(xmppData.getMucList());
        }
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

        this.menu = menu;
        if(selectedMucId != null){
            this.menu.setGroupVisible(R.id.muc_menu, true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.menu_exit:
                if (mBound) {
                    sendRequestMessage("stop");
                }
                finish();
                break;
            case R.id.menu_connect:
                if (mBound) {
                    sendRequestMessage("connect");
                }
                break;
            case R.id.menu_disconnect:
                if (mBound) {
                    sendRequestMessage("disconnect");
                }
                break;
            case R.id.menu_bookmarks_muc:
                sendRequestMessage("bookmarks_request");
                break;
            case R.id.menu_service_discover:
                startActivity(new Intent(this, ServiceDiscoveryActivity.class));
                break;
            case R.id.menu_accounts:
                startActivity(new Intent(this, AccountsActivity.class));
                break;
            case R.id.menu_leave_muc:
                if (mBound) {
                    if(selectedMucId != null){
                        Bundle b = new Bundle();
                        b.putString("leave_muc", selectedMucId);
                        sendMessage(b);

                        if(mucChatFragment != null) {
                            fm.beginTransaction().remove(mucChatFragment).commit();
                            mucChatFragment = null;
                        }

                        if(menu != null){
                            menu.setGroupVisible(R.id.muc_menu, false);
                        }
                    }
                }
                break;
            case R.id.menu_muc_participant_list:
                if(mucChatFragment != null){
                    mucChatFragment.switchParticipantList();
                }
                break;
            case R.id.menu_add_bookmark:
                MultiUserChat muc = null;
                List<MultiUserChat> mucList = xmppData.getMucList();
                for(MultiUserChat m:mucList){
                    if(m.getRoom().equals(selectedMucId)){
                        muc = m;
                    }
                }

                if(muc != null){
                    bookmarksDialogFragment = BookmarksDialogFragment.newInstance(muc.getRoom(),
                            muc.getNickname(),
                            BookmarksDialogFragment.NEW_BOOKMARK_FROM_CHAT_MENU);
                    bookmarksDialogFragment.show(fm, "bokmarks_dialog_fragment");
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    ///////////////////////////////////////////////////////////////////

    // Реализация методов интерфейса mListener во фрагменте ContactFragment
    @Override
    public void onListFragmentInteraction(Contact c) {
        // Сохраняем выбранный контакт в переменную
        selectedContactJid = c.getJid();

        selectedMucId = null;
        if(menu != null){
            menu.setGroupVisible(R.id.muc_menu, false);
        }

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
    public void onMucListFragmentInteraction(MultiUserChat item) {
        selectedMucId = item.getRoom();
        selectedContactJid = null;

        if(fragmentChat != null) {
            fm.beginTransaction().remove(fragmentChat).commit();
            fragmentChat = null;
        }

        if(mucChatFragment != null){
            mucChatFragment.setMucIdView(selectedMucId);
            mucChatFragment.setMucChatView(xmppData.getMucMessagesList(selectedMucId));
            mucChatFragment.setSendMsgView("");
            mucChatFragment.closeParticipantList();
            mucChatFragment.updateMucParticipantsList(xmppData.getMucParticipantList(selectedMucId));
            if(menu != null){
                menu.setGroupVisible(R.id.muc_menu, true);
            }
        } else {
            showMucChat();
        }


    }

    // Реализация методов интерфейса mListener во фрагменте ChatFragment
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

    // Реализация методов mListener в BookmarksDialogFragment
    @Override
    public void onBookmarkSave(String jid, String name, String nick, String password) {
        Bundle bundle = new Bundle();
        bundle.putString("bookmark", "save");
        bundle.putString("jid", jid);
        bundle.putString("name", name);
        bundle.putString("nick", nick);
        bundle.putString("password", password);
        sendMessage(bundle);

        bookmarksDialogFragment.dismiss();
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
        startActivity(new Intent(this, BookmarksActivity.class));
    }
}
