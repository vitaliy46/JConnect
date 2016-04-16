package com.jabber.jconnect;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String jid;
    private String chat;
    private String sendMsg;

    TextView jidView;
    TextView chatView;
    ScrollView scrollChatView;
    EditText messageText;
    ScrollView scrollMsgView;
    InputMethodManager imm;
    Button sendMessage;

    XmppData xmppData = XmppData.getInstance();

    private OnFragmentInteractionListener mListener;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String jid, String sendMsg) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("jid", jid);
        args.putString("send_msg", sendMsg);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jid = getArguments().getString("jid");
            sendMsg = getArguments().getString("send_msg");
        }

        chat = xmppData.getMessagesList(jid);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        jidView = (TextView) v.findViewById(R.id.jid_view);
        jidView.setText(jid);

        chatView = (TextView) v.findViewById(R.id.chat_text_view);
        scrollChatView = (ScrollView) v.findViewById(R.id.scroll_chat_view);
        chatView.setText(chat);
        scrollChatView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollChatView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollChatView.fullScroll(View.FOCUS_DOWN);
                        messageText.requestFocus();
                    }
                });
            }
        });

        messageText = (EditText) v.findViewById(R.id.message_text);
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        scrollMsgView = (ScrollView) v.findViewById(R.id.scroll_msg_send);

        messageText.setText(sendMsg);
        scrollMsgView.post(new Runnable() {
            @Override
            public void run() {
                if (sendMsg != null) {
                    messageText.setSelection(sendMsg.length());
                }
                scrollMsgView.fullScroll(View.FOCUS_DOWN);

                //imm.showSoftInput(messageText, InputMethodManager.SHOW_FORCED);
            }
        });

        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendMsg = messageText.getText().toString();

                if(mListener != null){
                    mListener.setSendMsg(sendMsg);
                }

                scrollChatView.fullScroll(View.FOCUS_DOWN);
                messageText.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        messageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageText.setSelection(messageText.getText().length());
                imm.showSoftInput(messageText, InputMethodManager.SHOW_FORCED);
            }
        });

        sendMessage = (Button) v.findViewById(R.id.chat_message_send);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xmppData.setMessagesList(jid, "Я", messageText.getText().toString());
                xmppData.setMessageToSend(messageText.getText().toString());

                if(mListener != null){
                    mListener.sendMessage(jid);
                }

                // Очищаем поле ввода после отправки сообщения и добавляем в окно чата
                messageText.setText("");
                chatView.setText(xmppData.getMessagesList(jid));
            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void setSendMsg(String sendMsg);
        void sendMessage(String jid);
    }

    // Методы переменной jid
    public String getJid(){
        return jid;
    }

    public void setJid(String jid){
        this.jid = jid;
    }

    public void setJidView(String jid){
        this.jid = jid;
        jidView.setText(jid);
    }

    // Методы переменной chat
    public String getChat(){
        return chat;
    }

    public void setChat(String chat){
        this.chat = chat;
    }

    public void setChatView(String chat){
        this.chat = chat;
        chatView.setText(chat);
        scrollChatView.fullScroll(View.FOCUS_DOWN);
        messageText.requestFocus();
    }

    // Методы переменной sendMsg
    public String getSendMsg(){
        return sendMsg;
    }

    public void setSendMsg(String sendMsg){
        this.sendMsg = sendMsg;
    }

    public void setSendMsgView(String sendMsg){
        this.sendMsg = sendMsg;
        messageText.setText(sendMsg);
        scrollMsgView.fullScroll(View.FOCUS_DOWN);
    }

    public void scroll(){
        scrollMsgView.fullScroll(View.FOCUS_DOWN);
    }
}
