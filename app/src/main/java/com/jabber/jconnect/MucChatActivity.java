package com.jabber.jconnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.List;

public class MucChatActivity extends AppCompatActivity implements MucChatFragment.OnMucChatFragmentInteractionListener,
        BookmarksDialogFragment.BookmarksDialogListener {

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
            b.putString("activity", "chat_muc");
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

            if(bundle.getString("recieve_muc") != null) {
                if(mucChatFragment != null && mucId.equals(bundle.getString("recieve_muc"))) {
                    mucChatFragment.setMucChatView(xmppData.getMucMessagesList(bundle.getString("recieve_muc")));
                    xmppData.initializeOrResetMessagesCount(mucId);
                }
            }

            if(bundle.getString("muc_participant_list_updated") != null){
                if(mucChatFragment != null && mucId.equals(bundle.getString("muc_participant_list_updated"))){
                    mucChatFragment.updateMucParticipantsList(xmppData.getMucParticipantList(mucId));
                }
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
    MucChatFragment mucChatFragment;
    Intent startIntent;

    BookmarksDialogFragment bookmarksDialogFragment;

    String mucId;
    String sendMsg;
    Intent saveParams = new Intent();

    boolean backPressed = false;
    // Result code
    public static final int MUC_CHAT_ACTIVITY_BACK_PRESSED = 10;
    public static final int SAVE_MUC_CHAT_ACTIVITY = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) && isTablet()){
            finish();
            return;
        }

        setContentView(R.layout.activity_chat_muc);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startIntent = getIntent();

        mucId = startIntent.getStringExtra("muc_id");
        sendMsg = startIntent.getStringExtra("send_msg");

        fm = getSupportFragmentManager();
        mucChatFragment = (MucChatFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (mucChatFragment == null) {
            mucChatFragment = MucChatFragment.newInstance(mucId, sendMsg);
            fm.beginTransaction().add(R.id.fragmentContainer, mucChatFragment).commit();
        } else {
            mucChatFragment.setMucId(mucId);
            mucChatFragment.setSendMsg(sendMsg);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        backPressed = true;

        setResult(MUC_CHAT_ACTIVITY_BACK_PRESSED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_muc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_muc_participant_list:
                if(mucChatFragment != null){
                    mucChatFragment.switchParticipantList();
                }
                break;
            case R.id.muc_menu_leave_muc:
                if (mBound) {
                    if(mucId != null){
                        Bundle b = new Bundle();
                        b.putString("leave_muc", mucId);
                        sendMessage(b);
                    }

                    onBackPressed();
                }
                break;
            case R.id.muc_menu_add_bookmark:
                MultiUserChat muc = null;
                List<MultiUserChat> mucList = xmppData.getMucList();
                for(MultiUserChat m:mucList){
                    if(m.getRoom().equals(mucId)){
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

    @Override
    public void onResume() {
        super.onResume();

        // Подключаемся к сервису при выходе активности на передний план
        bindService(new Intent(this, XmppService.class), mConnection, Context.BIND_ABOVE_CLIENT);
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

        if(!backPressed){
            saveParams.putExtra("send_msg", sendMsg);
            setResult(SAVE_MUC_CHAT_ACTIVITY, saveParams);
        }

        finish();
    }

    // Проверяем является ли устройтсво планшетом
    public boolean isTablet() {
        return (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    // Реализация методов интерфейса mListener во фрагменте ChatFragment

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
}
