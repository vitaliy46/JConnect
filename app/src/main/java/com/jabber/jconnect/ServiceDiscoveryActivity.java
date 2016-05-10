package com.jabber.jconnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceDiscoveryActivity extends AppCompatActivity implements
        ServiceDiscoveryFragment.OnServiceDiscoverFragmentInteractionListener, JoinMucDialogFragment.JoinMucDialogListener {

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
            b.putString("activity", "service_discover");
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

            if(bundle.getString("parent_entity_id") != null){
                if(serviceDiscoveryFragment != null && parentEntityID == null){
                    serviceDiscoveryFragment.setParentDefaultEntityIDView(bundle.getString("parent_entity_id"));
                }
            }

            if("loaded".equals(bundle.getString("service_discover_items"))){
                if(serviceDiscoveryFragment != null){
                    serviceDiscoveryFragment.updateServiceDiscoverItemsList(xmppData.getServiceDiscoverItems());
                }
            }

            if(bundle.getString("recieve_captcha") != null){
                captchaDialogFragment = CaptchaDialogFragment.newInstance(bundle.getString("recieve_captcha"));
                captchaDialogFragment.show(fm, "captcha_dialog_fragment");
            }

            if(bundle.getString("muc_is_password_protected") != null){
                mucIsPasswordProtected = true;
            }

            if(bundle.getString("muc_is_members_only") != null){
                mucIsMembersOnly = true;
            }

            if(bundle.getString("not_authorized_muc_is_members_only") != null){
                String membersOnlyMsg = "Комната " + mucEntity + " только для зарегистрированных участников";
                Toast.makeText(getApplicationContext(), membersOnlyMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    String parentEntityID = null;

    FragmentManager fm;
    ServiceDiscoveryFragment serviceDiscoveryFragment;

    CaptchaDialogFragment captchaDialogFragment;

    JoinMucDialogFragment joinMucDialogFragment;
    boolean mucIsPasswordProtected = false;
    boolean mucIsMembersOnly = false;

    MenuItem joinMucMenuItem;
    String mucEntity;
    Pattern pMucEntity = Pattern.compile("(\\S+)@(conference)(\\.)(\\S+\\.)(\\w+)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_discovery);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            // Восстановление значений сохраненных в savedInstanceState
            parentEntityID = savedInstanceState.getString("parent_entity_id");
        }

        fm = getSupportFragmentManager();
        serviceDiscoveryFragment = (ServiceDiscoveryFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (serviceDiscoveryFragment == null) {
            serviceDiscoveryFragment = ServiceDiscoveryFragment.newInstance(parentEntityID);
            fm.beginTransaction().add(R.id.fragmentContainer, serviceDiscoveryFragment).commit();
        }
    }

    // Сохранение параметров для восстановления активности
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("parent_entity_id", parentEntityID);
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
    public void onBackPressed() {
        super.onBackPressed();

        xmppData.clearServiceDiscoverItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_service_discover, menu);
        joinMucMenuItem = menu.findItem(R.id.menu_service_discover_join_muc);

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
            case R.id.menu_service_discover_join_muc:
                if(mucIsMembersOnly){
                    String membersOnlyMsg = "Комната " + mucEntity + " только для зарегитсрированных участников";
                    //Toast.makeText(getApplicationContext(), membersOnlyMsg, Toast.LENGTH_LONG).show();
                }

                joinMucDialogFragment = JoinMucDialogFragment.newInstance(mucEntity, mucIsPasswordProtected);
                joinMucDialogFragment.show(fm, "join_muc_dialog_fragment");

                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void joinMuc(String mucID, String nick, String password){
        Bundle b = new Bundle();
        b.putString("join_muc_from_service_discover", mucID);
        b.putString("join_muc_from_service_discover_nick", nick);
        b.putString("join_muc_from_service_discover_password", password);
        sendMessage(b);
    }

    @Override
    public void onServiceDiscoverRequest(String parentEntityID) {
        this.parentEntityID = parentEntityID;

        Matcher m = pMucEntity.matcher(parentEntityID);
        if(m.find()){
            mucEntity = m.group();

            mucIsPasswordProtected = false;
            mucIsMembersOnly = false;
            Bundle bundle = new Bundle();
            bundle.putString("request_muc_protection_info", mucEntity);
            sendMessage(bundle);

            joinMucMenuItem.setVisible(true);
        } else {
            joinMucMenuItem.setVisible(false);
        }

        Bundle bundle = new Bundle();
        bundle.putString("service_discover_request", parentEntityID);
        sendMessage(bundle);
    }

    @Override
    public void onServiceDiscoverInteraction(String itemID) {
        this.parentEntityID = itemID;
        serviceDiscoveryFragment.setParentDefaultEntityIDView(itemID);
        xmppData.clearServiceDiscoverItems();
        serviceDiscoveryFragment.updateServiceDiscoverItemsList(xmppData.getServiceDiscoverItems());

        Matcher m = pMucEntity.matcher(itemID);
        if(m.find()){
            mucEntity = m.group();

            mucIsPasswordProtected = false;
            mucIsMembersOnly = false;
            Bundle bundle = new Bundle();
            bundle.putString("request_muc_protection_info", mucEntity);
            sendMessage(bundle);

            joinMucMenuItem.setVisible(true);
        } else {
            joinMucMenuItem.setVisible(false);
        }

        Bundle bundle = new Bundle();
        bundle.putString("service_discover_request", parentEntityID);
        sendMessage(bundle);
    }

    @Override
    public void onSubmitJoinMucDialogInteraction(String MucID, String nick) {
        joinMuc(MucID, nick, null);
    }

    @Override
    public void onSubmitJoinMucDialogInteraction(String MucID, String nick, String password) {
        joinMuc(MucID, nick, password);
    }
}
