package com.jabber.jconnect;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jabber.jconnect.MucFragment.OnMucListFragmentInteractionListener;

import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a  and makes a call to the
 * specified {@link OnMucListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MucRecyclerViewAdapter extends RecyclerView.Adapter<MucRecyclerViewAdapter.ViewHolder> {

    private List<MultiUserChat> mValues;
    private final MucFragment.OnMucListFragmentInteractionListener mListener;

    XmppData xmppData = XmppData.getInstance();

    public MucRecyclerViewAdapter(List<MultiUserChat> items, MucFragment.OnMucListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    public void updateValues(List<MultiUserChat> d){
        mValues = d;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_muc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        String mucID = mValues.get(position).getRoom();

        int messageCount = xmppData.getMessagesCount(mucID);

        String contentText = mucID + ((messageCount != 0) ? (" (" + messageCount + ")") : "");
        holder.mContentView.setText(contentText);

        /*holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onMucListFragmentInteraction(holder.mItem);
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
        public final TextView mContentView;
        public MultiUserChat mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.muc);
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener){
                        mListener.onMucListFragmentInteraction(mItem);
                    }

                    xmppData.initializeOrResetMessagesCount(mItem.getRoom());
                    mContentView.setText(mItem.getRoom());
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
