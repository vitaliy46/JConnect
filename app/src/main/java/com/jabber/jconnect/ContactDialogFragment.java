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
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactDialogFragment extends DialogFragment {

    public static int NEW_CONTACT = 1;
    public static int EDIT_CONTACT = 2;

    private int dialogStatus = 1;

    EditText jidEdit;
    EditText nameEdit;
    Button okButton;
    Button cancelButton;

    InputMethodManager imm;

    // Use this instance of the interface to deliver action events
    ContactDialogListener mListener;

    public ContactDialogFragment() {
        // Required empty public constructor
    }

    public static ContactDialogFragment newInstance(int status) {
        ContactDialogFragment fragment = new ContactDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);

        fragment.setArguments(args);
        return fragment;
    }

    public static BookmarksDialogFragment newInstance(String jid, String name, int status) {
        BookmarksDialogFragment fragment = new BookmarksDialogFragment();
        Bundle args = new Bundle();
        args.putInt("status", status);
        args.putString("jid", jid);
        args.putString("name", name);

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (getArguments() != null) {
            dialogStatus = getArguments().getInt("status");
        }

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_contact_add_dialog, null);

        jidEdit = (EditText) v.findViewById(R.id.contact_dialog_add_jid_edit);
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

        nameEdit = (EditText) v.findViewById(R.id.contact_dialog_add_name_edit);
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

        if(dialogStatus == EDIT_CONTACT){
            jidEdit.setText(getArguments().getString("jid"));
            nameEdit.setText(getArguments().getString("name"));
        }

        okButton = (Button) v.findViewById(R.id.contact_dialog_add_submit_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean validated = true;
                String jid = null;
                String name = null;

                String errorBegin = getResources().getString(R.string.contact_dialog_add_field_error_begin);
                String errorEmptyField = getResources().getString(R.string.contact_dialog_add_empty_field_error);
                String errorUserJid = getResources().getString(R.string.contact_dialog_add_jid_field_error);

                String jidTitle = getResources().getString(R.string.contact_dialog_add_jid);
                String nameTitle = getResources().getString(R.string.contact_dialog_add_name);

                if(jidEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBegin + " \"" + jidTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    Pattern pUserJid = Pattern.compile("(\\S+)@(\\S+\\.)(\\w+)");
                    Matcher mUserJid = pUserJid.matcher(jidEdit.getText().toString());
                    if(mUserJid.find()){
                        jid = mUserJid.group();
                    } else {
                        validated = false;
                        Toast.makeText(getContext(), errorBegin + " \"" + jidTitle + "\" " + errorUserJid,
                                Toast.LENGTH_LONG).show();
                    }
                }

                if(nameEdit.getText().toString().equals("")){
                    validated = false;
                    Toast.makeText(getContext(), errorBegin + " \"" + nameTitle + "\" " + errorEmptyField,
                            Toast.LENGTH_LONG).show();
                } else {
                    name = nameEdit.getText().toString();
                }

                if(validated){
                    mListener.onContactSave(jid, name);
                }
            }
        });

        cancelButton = (Button) v.findViewById(R.id.contact_dialog_add_cancel_button);
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
    public interface ContactDialogListener {
        public void onContactSave(String jid, String name);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ContactDialogListener) activity;
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
