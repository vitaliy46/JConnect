package com.jabber.jconnect;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountsDialogFragment extends DialogFragment {

    public final static int NEW_ACCOUNT = 1;
    public final static int EDIT_ACCOUNT = 2;

    Account account = null;
    int newAccount;

    TextView accountsDialogTitle;
    EditText serverNameEdit;
    EditText loginEdit;
    EditText passwordEdit;
    EditText portEdit;
    Button okButton;
    Button cancelButton;

    InputMethodManager imm;

    // Use this instance of the interface to deliver action events
    AccountsDialogListener mListener;

    public AccountsDialogFragment() {
        // Required empty public constructor
    }

    public static AccountsDialogFragment newInstance(int status) {
        AccountsDialogFragment fragment = new AccountsDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);

        fragment.setArguments(args);
        return fragment;
    }

    public static AccountsDialogFragment newInstance(Account account, int status) {
        AccountsDialogFragment fragment = new AccountsDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);
        args.putInt("id", account.getId());
        args.putString("server_name", account.getServerName());
        args.putString("login", account.getLogin());
        args.putString("password", account.getPassword());
        args.putInt("port", account.getPort());
        args.putInt("selected", account.getSelected());

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            newAccount = getArguments().getInt("status");

            if(getArguments().getInt("id") != 0){
                account = new Account(getArguments().getInt("id"), getArguments().getString("server_name"),
                        getArguments().getString("login"), getArguments().getString("password"),
                        getArguments().getInt("port"), getArguments().getInt("selected"));
            }
        }

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_accounts_dialog, null);

        accountsDialogTitle = (TextView) v.findViewById(R.id.accounts_dialog_title);

        serverNameEdit = (EditText) v.findViewById(R.id.accounts_dialog_server_edit);
        serverNameEdit.setSelection(serverNameEdit.getText().length());
        serverNameEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverNameEdit.setSelection(serverNameEdit.getText().length());
                imm.showSoftInput(serverNameEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        serverNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    serverNameEdit.setSelection(serverNameEdit.getText().length());
                    imm.showSoftInput(serverNameEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        loginEdit = (EditText) v.findViewById(R.id.accounts_dialog_login_edit);
        loginEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEdit.setSelection(loginEdit.getText().length());
                imm.showSoftInput(loginEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        loginEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    loginEdit.setSelection(loginEdit.getText().length());
                    imm.showSoftInput(loginEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        passwordEdit = (EditText) v.findViewById(R.id.accounts_dialog_password_edit);
        passwordEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordEdit.setSelection(passwordEdit.getText().length());
                imm.showSoftInput(passwordEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        passwordEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    passwordEdit.setSelection(passwordEdit.getText().length());
                    imm.showSoftInput(passwordEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        portEdit = (EditText) v.findViewById(R.id.accounts_dialog_port_edit);
        portEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                portEdit.setSelection(portEdit.getText().length());
                imm.showSoftInput(portEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        portEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    portEdit.setSelection(portEdit.getText().length());
                    imm.showSoftInput(portEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        okButton = (Button) v.findViewById(R.id.accounts_dialog_submit_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean validated = true;
                String serverName = null;
                String login = null;
                String password = null;
                int port = 0;

                String errorBeginning = getResources().getString(R.string.accounts_dialog_field_error_beginning);
                String errorEmptyField = getResources().getString(R.string.accounts_dialog_empty_field_error);
                String errorServerAddress = getResources().getString(R.string.accounts_dialog_server_address_field_error);
                String errorDigitsOnly = getResources().getString(R.string.accounts_dialog_digits_only_error);

                String serverNameTitle = getResources().getString(R.string.accounts_dialog_server);
                String loginTitle = getResources().getString(R.string.accounts_dialog_login);
                String passwordTitle = getResources().getString(R.string.accounts_dialog_password);
                String portTitle = getResources().getString(R.string.accounts_dialog_port);

                if(serverNameEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBeginning + " \"" + serverNameTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    Pattern pServerName = Pattern.compile("(\\S+)@(\\S+\\.)(\\w+)");
                    Matcher mServerName = pServerName.matcher(serverNameEdit.getText().toString());
                    if(mServerName.find()){
                        serverName = mServerName.group();
                    } else {
                        validated = false;
                        Toast.makeText(getContext(), errorBeginning + " \"" + serverNameTitle + "\" " + errorServerAddress,
                                Toast.LENGTH_LONG).show();
                    }
                }

                if(loginEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBeginning + " \"" + loginTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    login = loginEdit.getText().toString();
                }

                if(passwordEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBeginning + " \"" + passwordTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    password = passwordEdit.getText().toString();
                }

                if(portEdit.getText().toString().equals("")){
                    validated = false;
                    // Если поле пустое - выдаем сообщение
                    Toast.makeText(getContext(), errorBeginning + " \"" + portTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    Pattern pPortNotDigit = Pattern.compile("\\D+");
                    Matcher mPortNotDigit = pPortNotDigit.matcher(portEdit.getText().toString());
                    if(mPortNotDigit.find()){
                        validated = false;
                        // Если есть нецифровой символ - выдаем сообщение
                        Toast.makeText(getContext(), errorBeginning + " \"" + portTitle + "\" " + errorDigitsOnly,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Pattern pPortDigit = Pattern.compile("\\d+");
                        Matcher mPortDigit = pPortDigit.matcher(portEdit.getText().toString());
                        if(mPortDigit.find()){
                            port = Integer.parseInt(mPortDigit.group());
                        }
                    }
                }

                if(validated){
                    if(newAccount == NEW_ACCOUNT){
                        mListener.onCreateAccount(serverName, login, password, port, 0);
                    } else if(newAccount == EDIT_ACCOUNT){
                        account.setServerName(serverName);
                        account.setLogin(login);
                        account.setPassword(password);
                        account.setPort(port);

                        mListener.onSaveAccount(account);
                    }
                }
            }
        });

        cancelButton = (Button) v.findViewById(R.id.accounts_dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        if(newAccount == NEW_ACCOUNT){
            accountsDialogTitle.setText(R.string.accounts_dialog_add_title);
            portEdit.setText(String.valueOf(5222));
        } else if(newAccount == EDIT_ACCOUNT) {
            accountsDialogTitle.setText(R.string.accounts_dialog_edit_title);

            if(account != null){
                serverNameEdit.setText(account.getServerName());
                loginEdit.setText(account.getLogin());
                passwordEdit.setText(account.getPassword());
                portEdit.setText(String.valueOf(account.getPort()));
            }
        }

        /*View v;
        if(!passwordProtected){
            v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_muc_join, null);
        } else {
            v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_muc_join_with_password, null);
            passwordView = (EditText) v.findViewById(R.id.join_muc_dialog_password);
            passwordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    imm.showSoftInput(passwordView, InputMethodManager.SHOW_FORCED);
                }
            });
            passwordView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imm.showSoftInput(passwordView, InputMethodManager.SHOW_FORCED);
                }
            });
        }

        mucIdView = (EditText) v.findViewById(R.id.join_muc_dialog_mucname);
        mucIdView.setText(mucID);
        mucIdView.setSelection(mucIdView.getText().length());
        mucIdView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                imm.showSoftInput(mucIdView, InputMethodManager.SHOW_FORCED);
            }
        });
        mucIdView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.showSoftInput(mucIdView, InputMethodManager.SHOW_FORCED);
            }
        });

        nickView = (EditText) v.findViewById(R.id.join_muc_dialog_nick);
        nickView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                imm.showSoftInput(nickView, InputMethodManager.SHOW_FORCED);
            }
        });
        nickView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imm.showSoftInput(nickView, InputMethodManager.SHOW_FORCED);
            }
        });

        okButton = (Button) v.findViewById(R.id.join_muc_dialog_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!passwordProtected){
                    mListener.onSubmitJoinMucDialogInteraction(mucIdView.getText().toString(),
                            nickView.getText().toString());
                } else {
                    mListener.onSubmitJoinMucDialogInteraction(mucIdView.getText().toString(),
                            nickView.getText().toString(), passwordView.getText().toString());
                }

                dismiss();
            }
        });*/

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);

        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface AccountsDialogListener {
        //public void onDialogPositiveClick(DialogFragment dialog);
        //public void onDialogNegativeClick(DialogFragment dialog);
        public void onCreateAccount(String serverName, String login, String password, int port, int selected);
        public void onSaveAccount(Account account);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (AccountsDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AccountsDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
