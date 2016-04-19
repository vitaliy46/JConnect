package com.jabber.jconnect;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BookmarksDialogFragment  extends DialogFragment {

    /*@NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String[] items = new String[]{"первый", "второй", "третий", "четвертый", "пятый", "шестой", "седьмой"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bookmarks_dialog_title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), items[which], Toast.LENGTH_SHORT).show();
                    }
                });

        return builder.create();
    }*/

    ListView mBookmarksList;
    List<String> items;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //final String[] items = new String[]{"первый", "второй", "третий", "четвертый", "пятый", "шестой", "седьмой"};
        items = new ArrayList<>();
        items.add("первый");
        items.add("второй");
        items.add("третий");
        items.add("четвертый");
        items.add("пятый");
        items.add("шестой");
        items.add("седьмой");

        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_bookmarks, null);
        //mBookmarksList = (ListView) v.findViewById(R.id.dialog_bookmarks_listview);

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        //mBookmarksList.setAdapter(adapter);

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.dialog_bookmarks_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RVAdapter adapter = new RVAdapter(items);
        recyclerView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v);

        return builder.create();
    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.BookmarkViewHolder>{
        public class BookmarkViewHolder extends RecyclerView.ViewHolder {
            public final TextView bookmarkId;
            public String mItem;

            BookmarkViewHolder(View itemView) {
                super(itemView);
                bookmarkId = (TextView)itemView.findViewById(R.id.dialog_bookmarks_item);
            }
        }

        List<String> bookmarks;

        RVAdapter(List<String> bookmarks){
            this.bookmarks = bookmarks;
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
        public void onBindViewHolder(BookmarkViewHolder bookmarkViewHolder, int position) {
            bookmarkViewHolder.mItem = bookmarks.get(position);
            bookmarkViewHolder.bookmarkId.setText(bookmarks.get(position));
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
