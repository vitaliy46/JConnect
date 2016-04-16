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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements ChatFragment.OnFragmentInteractionListener {

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
            b.putString("activity", "chat");
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

            if(bundle.getString("text") != null) {
                Toast.makeText(getApplicationContext(), bundle.getString("text"), Toast.LENGTH_SHORT).show();
            }

            if(bundle.getString("recieve") != null) {
                XmppData x = XmppData.getInstance();

                if((fragmentChat != null) && (jid.equals(bundle.getString("recieve")))) {
                    fragmentChat.setChatView(x.getMessagesList(bundle.getString("recieve")));
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    FragmentManager fm;
    ChatFragment fragmentChat;
    Intent startIntent;

    String jid;
    String sendMsg;
    Intent saveParams = new Intent();

    boolean backPressed = false;
    // Result code
    public static final int CHAT_ACTIVITY_BACK_PRESSED = 1;
    public static final int SAVE_CHAT_ACTIVITY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) && isTablet()){
            finish();
            return;
        }

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startIntent = getIntent();

        jid = startIntent.getStringExtra("jid");
        sendMsg = startIntent.getStringExtra("send_msg");

        fm = getSupportFragmentManager();
        fragmentChat = (ChatFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (fragmentChat == null) {
            fragmentChat = ChatFragment.newInstance(jid, sendMsg);
            fm.beginTransaction().add(R.id.fragmentContainer, fragmentChat).commit();
        } else {
            fragmentChat.setJid(jid);
            fragmentChat.setSendMsg(sendMsg);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        backPressed = true;

        setResult(CHAT_ACTIVITY_BACK_PRESSED);
        finish();
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
            setResult(SAVE_CHAT_ACTIVITY, saveParams);
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
    public void setSendMsg(String sendMsg) {
        this.sendMsg = sendMsg;
    }

    @Override
    public void sendMessage(String jid) {
        Bundle b = new Bundle();
        b.putString("send", jid);
        sendMessage(b);
    }
}
