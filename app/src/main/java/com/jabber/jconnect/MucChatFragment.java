package com.jabber.jconnect;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MucChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mucId;
    private String mucChat;
    private String sendMsg;

    TextView mucIdView;
    TextView mucChatView;
    ScrollView scrollMucChatView;
    EditText messageText;
    ScrollView scrollMsgView;
    InputMethodManager imm;
    Button sendMessage;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private boolean participantsListIsOpened = false;

    XmppData xmppData = XmppData.getInstance();

    private OnMucChatFragmentInteractionListener mListener;

    public MucChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     */
    // TODO: Rename and change types and number of parameters
    public static MucChatFragment newInstance(String mucId, String sendMsg) {
        MucChatFragment fragment = new MucChatFragment();
        Bundle args = new Bundle();
        args.putString("muc_id", mucId);
        args.putString("send_msg", sendMsg);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mucId = getArguments().getString("muc_id");
            sendMsg = getArguments().getString("send_msg");
        }

        mucChat = xmppData.getMucMessagesList(mucId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_chat_muc, container, false);

        String[] strArray = new String[]{"первый", "второй", "третий", "четвертый", "пятый", "шестой", "седьмой"};
        mDrawerLayout = (DrawerLayout) v.findViewById(R.id.participants_drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                participantsListIsOpened = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                participantsListIsOpened = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        mDrawerListView = (ListView) v.findViewById(R.id.left_drawer);
        mDrawerListView.setAdapter(new ArrayAdapter<>(getContext(), R.layout.participants_drawer_list_item, strArray));

        mucIdView = (TextView) v.findViewById(R.id.muc_id_view);
        mucIdView.setText(mucId);

        mucChatView = (TextView) v.findViewById(R.id.muc_chat_text_view);
        scrollMucChatView = (ScrollView) v.findViewById(R.id.scroll_muc_chat_view);
        mucChatView.setText(mucChat);
        scrollMucChatView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollMucChatView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollMucChatView.fullScroll(View.FOCUS_DOWN);
                        messageText.requestFocus();
                    }
                });
            }
        });

        messageText = (EditText) v.findViewById(R.id.muc_message_text);
        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        scrollMsgView = (ScrollView) v.findViewById(R.id.scroll_muc_msg_send);

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
                    mListener.setMucChatSendMsg(sendMsg);
                }

                scrollMucChatView.fullScroll(View.FOCUS_DOWN);
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

        sendMessage = (Button) v.findViewById(R.id.muc_chat_message_send);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xmppData.setMessageToSend(messageText.getText().toString());

                if(mListener != null){
                    mListener.sendMucChatMessage(mucId);
                }

                // Очищаем поле ввода после отправки сообщения и добавляем в окно чата
                messageText.setText("");
                mucChatView.setText(xmppData.getMucMessagesList(mucId));
            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMucChatFragmentInteractionListener) {
            mListener = (OnMucChatFragmentInteractionListener) context;
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
    public interface OnMucChatFragmentInteractionListener {
        // TODO: Update argument type and name
        void setMucChatSendMsg(String sendMsg);
        void sendMucChatMessage(String mucId);
    }

    // Методы переменной mucId
    public String getMucId(){
        return mucId;
    }

    public void setMucId(String mucId){
        this.mucId = mucId;
    }

    public void setMucIdView(String mucId){
        this.mucId = mucId;
        mucIdView.setText(mucId);
    }

    // Методы переменной mucChat
    public String getMucChat(){
        return mucChat;
    }

    public void setMucChat(String mucChat){
        this.mucChat = mucChat;
    }

    public void setMucChatView(String mucChat){
        this.mucChat = mucChat;
        mucChatView.setText(mucChat);
        scrollMucChatView.fullScroll(View.FOCUS_DOWN);
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

    public void switchParticipantList(){
        if(mDrawerLayout != null){
            if(!participantsListIsOpened){
                participantsListIsOpened = true;
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            } else {
                participantsListIsOpened = false;
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            }
        }
    }
}
