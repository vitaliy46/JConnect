package com.jabber.jconnect;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARENT_DEFAULT_ENTITY_ID = "parentDefaultEntityID";

    // TODO: Rename and change types of parameters
    //private String parentDefaultEntityID = "";

    //InputMethodManager imm;
    //EditText parentDefaultEntityIDText;
    //Button serviceDiscover;

    XmppData xmppData = XmppData.getInstance();

    RVAdapter adapter;
    List<Account> items;

    OnAccountInteractionListener mListener;

    public AccountsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     *
     * @return A new instance of fragment ServiceDiscoveryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AccountsFragment newInstance() {
        AccountsFragment fragment = new AccountsFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARENT_DEFAULT_ENTITY_ID, parentDefaultEntityID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_accounts, container, false);

        //imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        items = xmppData.getAccounts();

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.accounts_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RVAdapter(items, mListener);
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemDecoration itemDecoration = new DividerContactDecoration(getContext(),
                R.drawable.contact_divider);
        recyclerView.addItemDecoration(itemDecoration);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnAccountInteractionListener) {
            mListener = (OnAccountInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAccountInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    public void onAccountInteraction(Account account) {
        xmppData.updateAccount(account);
    }

    public interface OnAccountInteractionListener{
        void onAccountEditButtonClicked(Account account);
        void onAccountChecked(Account account);
        void onAccountUnChecked(Account account);
    }

    public void updateAccountsList(List<Account> accounts){
        adapter.updateValues(accounts);
        adapter.notifyDataSetChanged();
    }

    public void updateAccountsListViewWithCheckBox(boolean withCheckBox){
        adapter.withCheckBox(withCheckBox);
        adapter.notifyDataSetChanged();
    }

    // Адаптер для списка recyclerView
    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.AccountViewHolder>{
        public class AccountViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public RadioButton mItemViewRadio = null;
            public TextView mItemTextView = null;
            public Button mEditButton = null;
            public CheckBox mCheckBox = null;
            public Account mItem;

            AccountViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                if(!withCheckBox){
                    mItemViewRadio = (RadioButton)itemView.findViewById(R.id.accounts_item_radio_button);
                    mEditButton = (Button)itemView.findViewById(R.id.accounts_item_edit_button);
                } else {
                    mCheckBox = (CheckBox) itemView.findViewById(R.id.accounts_item_check_box);
                    mItemTextView = (TextView)itemView.findViewById(R.id.accounts_item_textview);
                }
            }
        }

        List<Account> items;
        AccountsFragment.OnAccountInteractionListener mListener;

        private RadioButton lastChecked = null;
        private int lastCheckedPosition = 0;

        private boolean withCheckBox = false;

        RVAdapter(List<Account> items, AccountsFragment.OnAccountInteractionListener listener){
            this.items = items;
            mListener = listener;
        }

        public void updateValues(List<Account> accounts){
            items = accounts;
        }

        public void withCheckBox(boolean withCheckBox){
            this.withCheckBox = withCheckBox;
        }

        @Override
        public int getItemViewType(int position) {
            return (withCheckBox ? 1 : 0);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            if(viewType == 0){
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_accounts_item, parent, false);
            } else {
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_accounts_item_with_checkbox, parent, false);
            }

            return new AccountViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final AccountViewHolder accountViewHolder, final int position) {
            accountViewHolder.mItem = items.get(position);

            String account = items.get(position).getLogin() + "@" + items.get(position).getServerName();

            if(!withCheckBox){
                accountViewHolder.mItemViewRadio.setText(account);

                if(items.get(position).getSelected() == 1){
                    accountViewHolder.mItemViewRadio.setChecked(true);
                    lastChecked = accountViewHolder.mItemViewRadio;
                    lastCheckedPosition = position;
                }

                accountViewHolder.mItemViewRadio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // lastChecked обрабатывается первее т.к. тот же аккаунт может быть нажат дважды
                        if(lastChecked != null){
                            lastChecked.setChecked(false);
                            items.get(lastCheckedPosition).setSelected(0);
                            AccountsFragment.this.onAccountInteraction(items.get(lastCheckedPosition));
                        }

                        accountViewHolder.mItemViewRadio.setChecked(true);
                        accountViewHolder.mItem.setSelected(1);
                        AccountsFragment.this.onAccountInteraction(accountViewHolder.mItem);

                        lastChecked = accountViewHolder.mItemViewRadio;
                        lastCheckedPosition = position;
                    }
                });

                accountViewHolder.mEditButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mListener != null){
                            mListener.onAccountEditButtonClicked(accountViewHolder.mItem);
                        }
                    }
                });
            } else {
                accountViewHolder.mItemTextView.setText(account);

                accountViewHolder.mCheckBox.setChecked(false);
                accountViewHolder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(isChecked){
                            if(mListener != null){
                                mListener.onAccountChecked(accountViewHolder.mItem);
                            }
                        } else {
                            if(mListener != null){
                                mListener.onAccountUnChecked(accountViewHolder.mItem);
                            }
                        }
                    }
                });

                accountViewHolder.mItemTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!accountViewHolder.mCheckBox.isChecked()){
                            accountViewHolder.mCheckBox.setChecked(true);
                        } else {
                            accountViewHolder.mCheckBox.setChecked(false);
                        }
                    }
                });
            }

            /*accountViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });*/
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
