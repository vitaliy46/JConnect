package com.jabber.jconnect;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jivesoftware.smackx.bookmarks.BookmarkedConference;

import java.util.List;

public class BookmarksDialogFragment  extends DialogFragment {

    List<BookmarkedConference> items;

    // Use this instance of the interface to deliver action events
    NoticeBookmarksDialogListener mListener;

    XmppData xmppData = XmppData.getInstance();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        items = xmppData.getBookmarkedConferenceList();

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_bookmarks, null);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.dialog_bookmarks_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RVAdapter adapter = new RVAdapter(items, mListener);
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemDecoration itemDecoration = new DividerContactDecoration(getContext(),
                R.drawable.contact_divider);
        recyclerView.addItemDecoration(itemDecoration);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);

        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeBookmarksDialogListener {
        //public void onDialogPositiveClick(DialogFragment dialog);
        //public void onDialogNegativeClick(DialogFragment dialog);
        public void onBookmarksDialogInteraction(String item);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeBookmarksDialogListener) activity;
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

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.BookmarkViewHolder>{
        public class BookmarkViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView bookmarkId;
            public BookmarkedConference mItem;

            BookmarkViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                bookmarkId = (TextView)itemView.findViewById(R.id.dialog_bookmarks_item);
            }
        }

        List<BookmarkedConference> bookmarks;

        private final BookmarksDialogFragment.NoticeBookmarksDialogListener mListener;

        RVAdapter(List<BookmarkedConference> bookmarks, BookmarksDialogFragment.NoticeBookmarksDialogListener listener){
            this.bookmarks = bookmarks;
            this.mListener = listener;
        }

        @Override
        public int getItemCount() {
            return bookmarks.size();
        }

        @Override
        public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_dialog_bookmarks_item, parent, false);

            return new BookmarkViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final BookmarkViewHolder bookmarkViewHolder, int position) {
            bookmarkViewHolder.mItem = bookmarks.get(position);
            bookmarkViewHolder.bookmarkId.setText(bookmarks.get(position).getJid());
            bookmarkViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onBookmarksDialogInteraction(bookmarkViewHolder.mItem.getJid());
                    }
                }
            });
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
