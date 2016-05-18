package com.jabber.jconnect.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jabber.jconnect.Account;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class JConnectDbContract {

    FeedReaderDbHelper mDbHelper;

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public JConnectDbContract(){}

    /* Inner class that defines the table contents */
    public static abstract class AccountEntry implements BaseColumns{
        public static final String TABLE_NAME = "account";
        public static final String COLUMN_NAME_SERVER_NAME = "server_name";
        public static final String COLUMN_NAME_LOGIN = "login";
        public static final String COLUMN_NAME_PASSWORD = "password";
        public static final String COLUMN_NAME_PORT = "port";
        public static final String COLUMN_NAME_SELECTED = "selected";
    }

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + AccountEntry.TABLE_NAME + " (" +
                    AccountEntry._ID + " INTEGER PRIMARY KEY," +
                    AccountEntry.COLUMN_NAME_SERVER_NAME + TEXT_TYPE + COMMA_SEP +
                    AccountEntry.COLUMN_NAME_LOGIN + TEXT_TYPE + COMMA_SEP +
                    AccountEntry.COLUMN_NAME_PASSWORD + TEXT_TYPE + COMMA_SEP +
                    AccountEntry.COLUMN_NAME_PORT + INTEGER_TYPE + COMMA_SEP +
                    AccountEntry.COLUMN_NAME_SELECTED + INTEGER_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AccountEntry.TABLE_NAME;

    public class FeedReaderDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "JConnect.db";

        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    public void initializeDbHelper(Context context){
        mDbHelper = new FeedReaderDbHelper(context);
    }

    public void closeDbHelper(){
        mDbHelper.close();
    }

    public long insertAccount(String serverName, String login, String password, int port, int selected){
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(AccountEntry.COLUMN_NAME_SERVER_NAME, serverName);
        values.put(AccountEntry.COLUMN_NAME_LOGIN, login);
        values.put(AccountEntry.COLUMN_NAME_PASSWORD, password);
        values.put(AccountEntry.COLUMN_NAME_PORT, port);
        values.put(AccountEntry.COLUMN_NAME_SELECTED, selected);

        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                AccountEntry.TABLE_NAME,
                null,
                values);

        return newRowId;
    }

    public List<Account> getAccounts(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                AccountEntry._ID,
                AccountEntry.COLUMN_NAME_SERVER_NAME,
                AccountEntry.COLUMN_NAME_LOGIN,
                AccountEntry.COLUMN_NAME_PASSWORD,
                AccountEntry.COLUMN_NAME_PORT,
                AccountEntry.COLUMN_NAME_SELECTED
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                AccountEntry._ID + " ASC";

        Cursor cursor = db.query(
                AccountEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        List<Account> accountList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndexOrThrow(AccountEntry._ID);
                int id = cursor.getInt(idIndex);

                int serverNameIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_SERVER_NAME);
                String serverName = cursor.getString(serverNameIndex);

                int loginIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_LOGIN);
                String login = cursor.getString(loginIndex);

                int passwordIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_PASSWORD);
                String password = cursor.getString(passwordIndex);

                int portIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_PORT);
                int port = cursor.getInt(portIndex);

                int selectedIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_SELECTED);
                int selected = cursor.getInt(selectedIndex);

                accountList.add(new Account(id, serverName, login, password, port, selected));
            } while (cursor.moveToNext());
        }

        cursor.close();

        return accountList;
    }

    @Nullable
    public Account getSelectedAccount(){
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                AccountEntry._ID,
                AccountEntry.COLUMN_NAME_SERVER_NAME,
                AccountEntry.COLUMN_NAME_LOGIN,
                AccountEntry.COLUMN_NAME_PASSWORD,
                AccountEntry.COLUMN_NAME_PORT,
                AccountEntry.COLUMN_NAME_SELECTED
        };

        String selection = AccountEntry.COLUMN_NAME_SELECTED + " LIKE ?";
        String[] selectionArgs = { String.valueOf(1) };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = AccountEntry._ID + " ASC";

        Cursor cursor = db.query(
                AccountEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                     // The columns for the WHERE clause
                selectionArgs,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(AccountEntry._ID);
            int id = cursor.getInt(idIndex);

            int serverNameIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_SERVER_NAME);
            String serverName = cursor.getString(serverNameIndex);

            int loginIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_LOGIN);
            String login = cursor.getString(loginIndex);

            int passwordIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_PASSWORD);
            String password = cursor.getString(passwordIndex);

            int portIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_PORT);
            int port = cursor.getInt(portIndex);

            int selectedIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME_SELECTED);
            int selected = cursor.getInt(selectedIndex);

            cursor.close();

            return new Account(id, serverName, login, password, port, selected);
        }

        cursor.close();

        return null;
    }

    public int updateAccount(Account account){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(AccountEntry.COLUMN_NAME_SERVER_NAME, account.getServerName());
        values.put(AccountEntry.COLUMN_NAME_LOGIN, account.getLogin());
        values.put(AccountEntry.COLUMN_NAME_PASSWORD, account.getPassword());
        values.put(AccountEntry.COLUMN_NAME_PORT, account.getPort());
        values.put(AccountEntry.COLUMN_NAME_SELECTED, account.getSelected());

        // Which row to update, based on the ID
        String selection = AccountEntry._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(account.getId()) };

        // returns count
        return db.update(
                AccountEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public void deleteAccount(int id){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Define 'where' part of query.
        String selection = AccountEntry._ID + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(id) };
        // Issue SQL statement.
        db.delete(AccountEntry.TABLE_NAME, selection, selectionArgs);
    }

    public void deleteAccounts(List<Account> accounts){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int size = accounts.size();
        String idListString = "";
        for(int i = 0; i < size; i++){
            if(i < (size - 1)){
                idListString += String.valueOf(accounts.get(i).getId()) + COMMA_SEP;
            } else {
                idListString += String.valueOf(accounts.get(i).getId());
            }
        }

        db.delete(AccountEntry.TABLE_NAME, AccountEntry._ID + " IN(" + idListString + ")", null);
    }
}
