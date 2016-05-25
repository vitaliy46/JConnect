package com.jabber.jconnect;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.jivesoftware.smackx.bookmarks.BookmarkedConference;

import java.util.List;

public class BookmarksFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARENT_DEFAULT_ENTITY_ID = "parentDefaultEntityID";

    // TODO: Rename and change types of parameters
    //private String parentDefaultEntityID = "";

    //InputMethodManager imm;
    //EditText parentDefaultEntityIDText;
    //Button serviceDiscover;

    List<BookmarkedConference> items;

    XmppData xmppData = XmppData.getInstance();

    RVAdapter adapter;

    OnBookmarkInteractionListener mListener;

    public BookmarksFragment() {
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
    public static BookmarksFragment newInstance() {
        BookmarksFragment fragment = new BookmarksFragment();
        Bundle args = new Bundle();

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
        View v = inflater.inflate(R.layout.fragment_bookmarks, container, false);

        //imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        items = xmppData.getBookmarkedConferenceList();

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.bookmarks_recycler);
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

        if (context instanceof OnBookmarkInteractionListener) {
            mListener = (OnBookmarkInteractionListener) context;
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

    public interface OnBookmarkInteractionListener{
        void onBookmarkInteraction(BookmarkedConference bookmark);
        void onBookmarkChecked(BookmarkedConference bookmark);
        void onBookmarkUnChecked(BookmarkedConference bookmark);
        void onBookmarkEditButtonClicked(BookmarkedConference bookmark);
    }

    public void updateBookmarksList(List<BookmarkedConference> bookmarks){
        adapter.updateValues(bookmarks);
        adapter.notifyDataSetChanged();
    }

    public void updateBookmarksListViewWithCheckBox(boolean withCheckBox){
        adapter.withCheckBox(withCheckBox);
        adapter.notifyDataSetChanged();
    }

    // Адаптер для списка recyclerView
    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.BookmarkViewHolder>{
        public class BookmarkViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public TextView mItemTextView = null;
            public Button mEditButton = null;
            public CheckBox mCheckBox = null;
            public BookmarkedConference mItem;

            BookmarkViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                if(!withCheckBox){
                    mItemTextView = (TextView)itemView.findViewById(R.id.bookmarks_item_view);
                    mEditButton = (Button)itemView.findViewById(R.id.bookmarks_item_edit_button);
                } else {
                    mCheckBox = (CheckBox) itemView.findViewById(R.id.bookmarks_item_check_box);
                    mItemTextView = (TextView)itemView.findViewById(R.id.bookmarks_item_view);
                }
            }
        }

        List<BookmarkedConference> items;
        BookmarksFragment.OnBookmarkInteractionListener mListener;

        private boolean withCheckBox = false;

        RVAdapter(List<BookmarkedConference> items, BookmarksFragment.OnBookmarkInteractionListener listener){
            this.items = items;
            mListener = listener;
        }

        public void updateValues(List<BookmarkedConference> items){
            this.items = items;
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
        public BookmarkViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            if(viewType == 0){
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_bookmarks_item, parent, false);
            } else {
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_bookmarks_item_with_checkbox, parent, false);
            }

            return new BookmarkViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final BookmarkViewHolder bookmarkViewHolder, final int position) {
            bookmarkViewHolder.mItem = items.get(position);

            String bookmark = bookmarkViewHolder.mItem.getName() + " " +
                    bookmarkViewHolder.mItem.getJid() + " " +
                    bookmarkViewHolder.mItem.getNickname();

            if(!withCheckBox){
                bookmarkViewHolder.mItemTextView.setText(bookmark);

                bookmarkViewHolder.mItemTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mListener != null){
                            mListener.onBookmarkInteraction(bookmarkViewHolder.mItem);
                        }
                    }
                });

                bookmarkViewHolder.mEditButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mListener != null){
                            mListener.onBookmarkEditButtonClicked(bookmarkViewHolder.mItem);
                        }
                    }
                });
            } else {
                bookmarkViewHolder.mItemTextView.setText(bookmark);

                bookmarkViewHolder.mCheckBox.setChecked(false);
                bookmarkViewHolder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(mListener != null){
                            if(isChecked){
                                mListener.onBookmarkChecked(bookmarkViewHolder.mItem);
                            } else {
                                mListener.onBookmarkUnChecked(bookmarkViewHolder.mItem);
                            }
                        }
                    }
                });

                bookmarkViewHolder.mItemTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!bookmarkViewHolder.mCheckBox.isChecked()){
                            bookmarkViewHolder.mCheckBox.setChecked(true);
                        } else {
                            bookmarkViewHolder.mCheckBox.setChecked(false);
                        }
                    }
                });
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }
}
