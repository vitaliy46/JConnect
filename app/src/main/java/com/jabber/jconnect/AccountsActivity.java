package com.jabber.jconnect;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class AccountsActivity extends AppCompatActivity implements AccountsFragment.OnAccountInteractionListener,
        AccountsDialogFragment.AccountsDialogListener{

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    FragmentManager fm;

    AccountsFragment accountsFragment;
    AccountsDialogFragment accountsDialogFragment;

    MenuItem cancelChoiseMenuItem;
    MenuItem deleteChosenMenuItem;

    List<Account> checkedAccounts = new ArrayList<>();

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
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_accounts, menu);
        cancelChoiseMenuItem = menu.findItem(R.id.accounts_menu_cancel_choise);
        deleteChosenMenuItem = menu.findItem(R.id.accounts_menu_delete_chosen);

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
                accountsFragment.updateAccountsListViewWithCheckBox(true);
                cancelChoiseMenuItem.setVisible(true);
                deleteChosenMenuItem.setVisible(true);
                break;
            case R.id.accounts_menu_cancel_choise:
                accountsFragment.updateAccountsListViewWithCheckBox(false);
                cancelChoiseMenuItem.setVisible(false);
                deleteChosenMenuItem.setVisible(false);
                checkedAccounts = new ArrayList<>();
                break;
            case R.id.accounts_menu_delete_chosen:
                xmppData.deleteAccounts(checkedAccounts);
                accountsFragment.updateAccountsListViewWithCheckBox(false);
                accountsFragment.updateAccountsList(xmppData.getAccounts());
                cancelChoiseMenuItem.setVisible(false);
                deleteChosenMenuItem.setVisible(false);
                checkedAccounts = new ArrayList<>();
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

    @Override
    public void onAccountChecked(Account account) {
        checkedAccounts.add(account);
    }

    @Override
    public void onAccountUnChecked(Account account) {
        checkedAccounts.remove(account);
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
