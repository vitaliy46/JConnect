package com.jabber.jconnect;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.jabber.jconnect.ContactFragment.OnListFragmentInteractionListener;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link } and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ContactRecyclerViewAdapter extends RecyclerView.Adapter<ContactRecyclerViewAdapter.ViewHolder> {

    private List<Contact> mValues;
    private final OnListFragmentInteractionListener mListener;

    private boolean withCheckBox = false;

    public ContactRecyclerViewAdapter(List<Contact> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public void updateValues(List<Contact> d){
        mValues = d;
    }

    public void withCheckBox(boolean withCheckBox){
        this.withCheckBox = withCheckBox;
    }

    @Override
    public int getItemViewType(int position) {
        return (withCheckBox ? 1 : 0);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if(viewType == 0){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_contact, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_contact_with_checkbox, parent, false);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        String jid = holder.mItem.getJid();
        String name = holder.mItem.getName();
        String status = holder.mItem.getStatus();
        String contentText = ((name != null) ? (name + " - ") : "") + jid;
        //contentText = (status != null) ? (contentText + " - " + status) : contentText;
        holder.mContentView.setText(contentText);



        if(!withCheckBox){
            holder.mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener){
                        mListener.onListFragmentInteraction(holder.mItem);
                    }
                }
            });
        } else {
            holder.mCheckBox.setChecked(false);

            holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        mListener.onContactCheched(holder.mItem);
                    } else {
                        mListener.onContactUnCheched(holder.mItem);
                    }
                }
            });

            holder.mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!holder.mCheckBox.isChecked()){
                        holder.mCheckBox.setChecked(true);
                    } else {
                        holder.mCheckBox.setChecked(false);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mContentView;
        public Contact mItem;
        public CheckBox mCheckBox = null;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.contact);
            /*mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener){
                        mListener.onListFragmentInteraction(mItem);
                    }
                }
            });*/

            if(withCheckBox){
                mCheckBox = (CheckBox) itemView.findViewById(R.id.contact_item_check_box);
            }
        }

        @Override
        public String toString() {
            return "'" + mContentView.getText() + "'";
        }
    }
}
