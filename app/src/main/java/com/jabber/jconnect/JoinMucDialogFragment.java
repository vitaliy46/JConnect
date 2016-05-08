package com.jabber.jconnect;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class JoinMucDialogFragment extends DialogFragment {

    private String mucID;
    private boolean passwordProtected = false;

    EditText mucIdView;
    EditText nickView;
    EditText passwordView = null;
    Button okButton;

    InputMethodManager imm;

    // Use this instance of the interface to deliver action events
    JoinMucDialogListener mListener;

    public JoinMucDialogFragment() {
        // Required empty public constructor
    }

    public static JoinMucDialogFragment newInstance(String mucID, boolean passwordProtected) {
        JoinMucDialogFragment fragment = new JoinMucDialogFragment();
        Bundle args = new Bundle();
        args.putString("muc_id", mucID);
        args.putBoolean("password_protected", passwordProtected);

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            mucID = getArguments().getString("muc_id");
            passwordProtected = getArguments().getBoolean("password_protected");
        }

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        View v;
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
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);

        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface JoinMucDialogListener {
        //public void onDialogPositiveClick(DialogFragment dialog);
        //public void onDialogNegativeClick(DialogFragment dialog);
        public void onSubmitJoinMucDialogInteraction(String mucID, String nick);
        public void onSubmitJoinMucDialogInteraction(String mucID, String nick, String password);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (JoinMucDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
