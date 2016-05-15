package com.jabber.jconnect;

import android.app.Application;

public class JConnect extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        XmppData.getInstance().initializeDatabase(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        XmppData.getInstance().closeDatabase();
    }
}
