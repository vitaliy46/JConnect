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

public class ServiceDiscoveryActivity extends AppCompatActivity implements
        ServiceDiscoveryFragment.OnServiceDiscoverFragmentInteractionListener {

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
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    String parentEntityID = null;

    FragmentManager fm;
    ServiceDiscoveryFragment serviceDiscoveryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_discovery);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
    public void onServiceDiscoverRequest(String parentEntityID) {
        this.parentEntityID = parentEntityID;

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

        Bundle bundle = new Bundle();
        bundle.putString("service_discover_request", parentEntityID);
        sendMessage(bundle);
    }
}
