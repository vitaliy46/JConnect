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
import android.widget.Toast;

import org.jivesoftware.smackx.bookmarks.BookmarkedConference;

import java.util.ArrayList;
import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarksFragment.OnBookmarkInteractionListener,
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

            if("update".equals(bundle.getString("bookmark"))){
                BookmarksActivity.this.updateBookmarksList();
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

            if(bundle.getString("recieve_captcha") != null){
                captchaDialogFragment = CaptchaDialogFragment.newInstance(bundle.getString("recieve_captcha"));
                captchaDialogFragment.show(fm, "captcha_dialog_fragment");
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Синглетон для доступа к данным
    XmppData xmppData = XmppData.getInstance();

    FragmentManager fm;

    BookmarksFragment bookmarksFragment;
    BookmarksDialogFragment bookmarksDialogFragment;

    MenuItem cancelChoiseMenuItem;
    MenuItem deleteChosenMenuItem;

    List<BookmarkedConference> checkedBookmarks = new ArrayList<>();

    CaptchaDialogFragment captchaDialogFragment;

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
        bookmarksFragment = (BookmarksFragment) fm.findFragmentById(R.id.fragmentContainer);
        if (bookmarksFragment == null) {
            bookmarksFragment = BookmarksFragment.newInstance();
            fm.beginTransaction().add(R.id.fragmentContainer, bookmarksFragment).commit();
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
        getMenuInflater().inflate(R.menu.menu_bookmarks, menu);
        cancelChoiseMenuItem = menu.findItem(R.id.bookmarks_cancel_choise);
        deleteChosenMenuItem = menu.findItem(R.id.bookmarks_delete_chosen);

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
            case R.id.bookmarks_menu_choose:
                bookmarksFragment.updateBookmarksListViewWithCheckBox(true);
                cancelChoiseMenuItem.setVisible(true);
                deleteChosenMenuItem.setVisible(true);

                break;
            case R.id.bookmarks_cancel_choise:
                bookmarksFragment.updateBookmarksListViewWithCheckBox(false);
                cancelChoiseMenuItem.setVisible(false);
                deleteChosenMenuItem.setVisible(false);
                checkedBookmarks = new ArrayList<>();

                break;
            case R.id.bookmarks_delete_chosen:
                xmppData.setCheckedBookmarks(checkedBookmarks);

                bookmarksFragment.updateBookmarksListViewWithCheckBox(false);
                cancelChoiseMenuItem.setVisible(false);
                deleteChosenMenuItem.setVisible(false);

                checkedBookmarks = new ArrayList<>();

                Bundle bundle = new Bundle();
                bundle.putString("bookmark", "delete");
                sendMessage(bundle);

                break;
            case R.id.bookmarks_menu_add:
                bookmarksDialogFragment = BookmarksDialogFragment.newInstance(BookmarksDialogFragment.NEW_BOOKMARK);
                bookmarksDialogFragment.show(fm, "bokmarks_dialog_fragment");

                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateBookmarksList(){
        bookmarksFragment.updateBookmarksList(xmppData.getBookmarkedConferenceList());
    }

    // Реализация методов mListener в BookmarksFragment
    @Override
    public void onBookmarkInteraction(BookmarkedConference bookmark) {
        Bundle b = new Bundle();
        b.putString("join_muc", bookmark.getJid());
        b.putString("join_muc_nick", bookmark.getNickname());
        b.putString("join_muc_password", bookmark.getPassword());
        sendMessage(b);
    }

    @Override
    public void onBookmarkChecked(BookmarkedConference bookmark) {
        checkedBookmarks.add(bookmark);
    }

    @Override
    public void onBookmarkUnChecked(BookmarkedConference bookmark) {
        checkedBookmarks.remove(bookmark);
    }

    @Override
    public void onBookmarkEditButtonClicked(BookmarkedConference bookmarkedConference) {
        bookmarksDialogFragment = BookmarksDialogFragment.newInstance(bookmarkedConference,
                BookmarksDialogFragment.EDIT_BOOKMARK);
        bookmarksDialogFragment.show(fm, "bookmarks_dialog_fragment");
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
