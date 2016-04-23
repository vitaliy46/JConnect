package com.jabber.jconnect;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;


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
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";
    private static final String ARG_PARENT_DEFAULT_ENTITY_ID = "parentDefaultEntityID";

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;
    private String parentDefaultEntityID = "";

    private OnServiceDiscoverFragmentInteractionListener mListener;

    InputMethodManager imm;
    EditText parentDefaultEntityIDText;

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
    public static ServiceDiscoveryFragment newInstance() {
        ServiceDiscoveryFragment fragment = new ServiceDiscoveryFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        //args.putString(ARG_PARENT_DEFAULT_ENTITY_ID, parentDefaultEntityID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //parentDefaultEntityID = getArguments().getString(ARG_PARENT_DEFAULT_ENTITY_ID);
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

        return v;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onServiceDiscoverInteraction(uri);
        }
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
        void onServiceDiscoverInteraction(Uri uri);
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
}
