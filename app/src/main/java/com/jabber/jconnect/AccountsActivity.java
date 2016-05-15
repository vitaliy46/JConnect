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

public class AccountsActivity extends AppCompatActivity implements AccountsFragment.OnAccountInteractionListener,
        AccountsDialogFragment.AccountsDialogListener{

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

            //
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    FragmentManager fm;

    AccountsFragment accountsFragment;
    AccountsDialogFragment accountsDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null) {
            // Восстановление значений сохраненных в savedInstanceState
        }

        fm = getSupportFragmentManager();
        accountsFragment = (AccountsFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (accountsFragment == null) {
            accountsFragment = AccountsFragment.newInstance();
            fm.beginTransaction().add(R.id.fragmentContainer, accountsFragment).commit();
        }
    }

    // Сохранение параметров для восстановления активности
    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_accounts, menu);

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
            case R.id.accounts_menu_choose:
                //
                break;
            case R.id.accounts_menu_add:
                accountsDialogFragment = AccountsDialogFragment.newInstance(AccountsDialogFragment.NEW_ACCOUNT);
                accountsDialogFragment.show(fm, "accounts_dialog_fragment");
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    // Реализация методов mListener в AccountsFragment
    @Override
    public void onAccountEditButtonClicked(Account account) {
        accountsDialogFragment = AccountsDialogFragment.newInstance(account, AccountsDialogFragment.EDIT_ACCOUNT);
        accountsDialogFragment.show(fm, "accounts_dialog_fragment");
    }

    // Реализация методов mListener в AccountsDialogFragment
    @Override
    public void onCreateAccount(String serverName, String login, String password, int port, int selected) {
        xmppData.insertAccount(serverName, login, password, port, selected);
        accountsDialogFragment.dismiss();

        accountsFragment.updateAccountsList(xmppData.getAccounts());
    }

    @Override
    public void onSaveAccount(Account account) {
        xmppData.updateAccount(account);
        accountsDialogFragment.dismiss();

        accountsFragment.updateAccountsList(xmppData.getAccounts());
    }
}
