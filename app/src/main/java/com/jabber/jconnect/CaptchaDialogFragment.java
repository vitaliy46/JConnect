package com.jabber.jconnect;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CaptchaDialogFragment extends DialogFragment {

    private String captchaLink;

    TextView captchaLinkView;
    Button okButton;

    // Use this instance of the interface to deliver action events
    CaptchaDialogListener mListener;

    public CaptchaDialogFragment() {
        // Required empty public constructor
    }

    public static CaptchaDialogFragment newInstance(String captchaLink) {
        CaptchaDialogFragment fragment = new CaptchaDialogFragment();
        Bundle args = new Bundle();
        args.putString("captcha_link", captchaLink);

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            captchaLink = getArguments().getString("captcha_link");
        }

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_captcha, null);
        captchaLinkView = (TextView) v.findViewById(R.id.captcha_dialog_link);
        captchaLinkView.setText(captchaLink);

        okButton = (Button) v.findViewById(R.id.captcha_ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
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
    public interface CaptchaDialogListener {
        //public void onDialogPositiveClick(DialogFragment dialog);
        //public void onDialogNegativeClick(DialogFragment dialog);
        public void onSubmitCaptchaDialogInteraction();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    /*@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (CaptchaDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }*/

    /*@Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }*/
}
