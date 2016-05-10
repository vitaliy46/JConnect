package com.jabber.jconnect;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

    public ContactRecyclerViewAdapter(List<Contact> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public void updateValues(List<Contact> d){
        mValues = d;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_contact, parent, false);
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

        /*holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final Button mContentView;
        public Contact mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (Button) view.findViewById(R.id.contact);
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener){
                        mListener.onListFragmentInteraction(mItem);
                    }
                }
            });
        }

        @Override
        public String toString() {
            return "'" + mContentView.getText() + "'";
        }
    }
}
