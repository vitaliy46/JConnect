package com.jabber.jconnect;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jivesoftware.smackx.disco.packet.DiscoverItems;

import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ServiceDiscoveryFragment.OnServiceDiscoverFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ServiceDiscoveryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ServiceDiscoveryFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARENT_DEFAULT_ENTITY_ID = "parentDefaultEntityID";

    // TODO: Rename and change types of parameters
    private String parentDefaultEntityID = "";

    private OnServiceDiscoverFragmentInteractionListener mListener;

    InputMethodManager imm;
    EditText parentDefaultEntityIDText;
    Button serviceDiscover;

    XmppData xmppData = XmppData.getInstance();

    RVAdapter adapter;
    List<DiscoverItems.Item> items;

    public ServiceDiscoveryFragment() {
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
    public static ServiceDiscoveryFragment newInstance(String parentDefaultEntityID) {
        ServiceDiscoveryFragment fragment = new ServiceDiscoveryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARENT_DEFAULT_ENTITY_ID, parentDefaultEntityID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            parentDefaultEntityID = getArguments().getString(ARG_PARENT_DEFAULT_ENTITY_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_service_discovery, container, false);

        imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        parentDefaultEntityIDText = (EditText) v.findViewById(R.id.service_discover_edit);
        parentDefaultEntityIDText.setText(parentDefaultEntityID);
        parentDefaultEntityIDText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentDefaultEntityIDText.setSelection(parentDefaultEntityIDText.getText().length());
                imm.showSoftInput(parentDefaultEntityIDText, InputMethodManager.SHOW_FORCED);
            }
        });

        serviceDiscover = (Button) v.findViewById(R.id.service_discover_button);
        serviceDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onServiceDiscoverRequest(parentDefaultEntityIDText.getText().toString());
                }
            }
        });

        items = xmppData.getServiceDiscoverItems();

        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.service_discovery_recycler);
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
        if (context instanceof OnServiceDiscoverFragmentInteractionListener) {
            mListener = (OnServiceDiscoverFragmentInteractionListener) context;
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
    public interface OnServiceDiscoverFragmentInteractionListener {
        // TODO: Update argument type and name
        void onServiceDiscoverRequest(String parentEntityID);
        void onServiceDiscoverInteraction(String itemID);
    }

    public String getParentDefaultEntityID() {
        return parentDefaultEntityID;
    }

    public void setParentDefaultEntityID(String parentDefaultEntityID) {
        this.parentDefaultEntityID = parentDefaultEntityID;
    }

    public void setParentDefaultEntityIDView(String parentDefaultEntityID) {
        this.parentDefaultEntityID = parentDefaultEntityID;
        parentDefaultEntityIDText.setText(parentDefaultEntityID);
        parentDefaultEntityIDText.setSelection(parentDefaultEntityIDText.getText().length());
    }

    public void updateServiceDiscoverItemsList(List<DiscoverItems.Item> items){
        adapter.updateValues(items);
        adapter.notifyDataSetChanged();
    }

    // Адаптер для списка recyclerView
    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.DiscoverItemsViewHolder>{
        public class DiscoverItemsViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mItemView;
            public DiscoverItems.Item mItem;

            DiscoverItemsViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                mItemView = (TextView)itemView.findViewById(R.id.service_discovery_item);
            }
        }

        List<DiscoverItems.Item> items;

        private final ServiceDiscoveryFragment.OnServiceDiscoverFragmentInteractionListener mListener;

        RVAdapter(List<DiscoverItems.Item> items,
                  ServiceDiscoveryFragment.OnServiceDiscoverFragmentInteractionListener listener){
            this.items = items;
            this.mListener = listener;
        }

        public void updateValues(List<DiscoverItems.Item> items){
            this.items = items;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public DiscoverItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_service_discovery_item, parent, false);

            return new DiscoverItemsViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final DiscoverItemsViewHolder discoverItemsViewHolder, int position) {
            discoverItemsViewHolder.mItem = items.get(position);

            String name = (items.get(position).getName() != null) ? (items.get(position).getName() + " ") : "";
            String str = name + items.get(position).getEntityID();
            discoverItemsViewHolder.mItemView.setText(str);
            discoverItemsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onServiceDiscoverInteraction(discoverItemsViewHolder.mItem.getEntityID());
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
