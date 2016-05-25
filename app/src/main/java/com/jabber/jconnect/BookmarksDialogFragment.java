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

import org.jivesoftware.smackx.bookmarks.BookmarkedConference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookmarksDialogFragment extends DialogFragment {

    public final static int NEW_BOOKMARK = 1;
    public final static int EDIT_BOOKMARK = 2;
    public final static int NEW_BOOKMARK_FROM_CHAT_MENU = 1;

    int newBookmark = NEW_BOOKMARK;

    TextView bookmarksDialogTitle;
    EditText jidEdit;
    EditText nameEdit;
    EditText nickEdit;
    EditText passwordEdit;
    Button okButton;
    Button cancelButton;

    InputMethodManager imm;

    // Use this instance of the interface to deliver action events
    BookmarksDialogListener mListener;

    public BookmarksDialogFragment() {
        // Required empty public constructor
    }

    public static BookmarksDialogFragment newInstance(int status) {
        BookmarksDialogFragment fragment = new BookmarksDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);

        fragment.setArguments(args);
        return fragment;
    }

    public static BookmarksDialogFragment newInstance(BookmarkedConference bookmarkedConference, int status) {
        BookmarksDialogFragment fragment = new BookmarksDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);
        args.putString("jid", bookmarkedConference.getJid());
        args.putString("name", bookmarkedConference.getName());
        args.putString("nick", bookmarkedConference.getNickname());
        args.putString("password", bookmarkedConference.getPassword());

        fragment.setArguments(args);
        return fragment;
    }

    public static BookmarksDialogFragment newInstance(String jid, String nick, int status) {
        BookmarksDialogFragment fragment = new BookmarksDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);
        args.putString("jid", jid);
        args.putString("nick", nick);

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getArguments() != null) {
            newBookmark = getArguments().getInt("status");
        }

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_bookmarks, null);

        bookmarksDialogTitle = (TextView) v.findViewById(R.id.bookmarks_dialog_title);

        jidEdit = (EditText) v.findViewById(R.id.bookmarks_dialog_jid_edit);
        jidEdit.setSelection(jidEdit.getText().length());
        jidEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jidEdit.setSelection(jidEdit.getText().length());
                imm.showSoftInput(jidEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        jidEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    jidEdit.setSelection(jidEdit.getText().length());
                    imm.showSoftInput(jidEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        nameEdit = (EditText) v.findViewById(R.id.bookmarks_dialog_name_edit);
        nameEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameEdit.setSelection(nameEdit.getText().length());
                imm.showSoftInput(nameEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        nameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    nameEdit.setSelection(nameEdit.getText().length());
                    imm.showSoftInput(nameEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        nickEdit = (EditText) v.findViewById(R.id.bookmarks_dialog_nick_edit);
        nickEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nickEdit.setSelection(nickEdit.getText().length());
                imm.showSoftInput(nickEdit, InputMethodManager.SHOW_FORCED);
            }
        });
        nickEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    nickEdit.setSelection(nickEdit.getText().length());
                    imm.showSoftInput(nickEdit, InputMethodManager.SHOW_FORCED);
                }
            }
        });

        passwordEdit = (EditText) v.findViewById(R.id.bookmarks_dialog_password_edit);
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

        if(newBookmark == EDIT_BOOKMARK){
            bookmarksDialogTitle.setText(getResources().getString(R.string.bookmarks_dialog_edit_title));
            jidEdit.setText(getArguments().getString("jid"));
            nameEdit.setText(getArguments().getString("name"));
            nickEdit.setText(getArguments().getString("nick"));
            passwordEdit.setText(getArguments().getString("password"));
        }
        if(newBookmark == NEW_BOOKMARK_FROM_CHAT_MENU){
            jidEdit.setText(getArguments().getString("jid"));
            nickEdit.setText(getArguments().getString("nick"));
        }

        okButton = (Button) v.findViewById(R.id.bookmarks_dialog_submit_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean validated = true;
                String jid = null;
                String name;
                String nick = null;
                String password;

                String errorBeginning = getResources().getString(R.string.bookmarks_dialog_field_error_beginning);
                String errorEmptyField = getResources().getString(R.string.bookmarks_dialog_empty_field_error);
                String errorRoomAddress = getResources().getString(R.string.bookmarks_dialog_room_address_field_error);

                String jidTitle = getResources().getString(R.string.bookmarks_dialog_jid);
                String nickTitle = getResources().getString(R.string.bookmarks_dialog_nick);

                if(jidEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBeginning + " \"" + jidTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    Pattern pRoomAddress = Pattern.compile("(\\S+)@(conference)(\\.)(\\S+\\.)(\\w+)");
                    Matcher mRoomAddress = pRoomAddress.matcher(jidEdit.getText().toString());
                    if(mRoomAddress.find()){
                        jid = mRoomAddress.group();
                    } else {
                        validated = false;
                        Toast.makeText(getContext(), errorBeginning + " \"" + jidTitle + "\" " + errorRoomAddress,
                                Toast.LENGTH_LONG).show();
                    }
                }

                name = nameEdit.getText().toString();

                if(nickEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBeginning + " \"" + nickTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    nick = nickEdit.getText().toString();
                }

                password = passwordEdit.getText().toString();

                if(validated){
                    mListener.onBookmarkSave(jid, name, nick, password);
                }
            }
        });

        cancelButton = (Button) v.findViewById(R.id.bookmarks_dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);

        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface BookmarksDialogListener {
        public void onBookmarkSave(String jid, String name, String nick, String password);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (BookmarksDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement BookmarksDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
