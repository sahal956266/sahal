package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowCaseActivity;


public class GenericAlertDialog extends DialogFragment {

    public static GenericAlertDialog newInstance(int title, String fragMes) {
        GenericAlertDialog frag = new GenericAlertDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putString("fragMessage", fragMes);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        String fragMessage = getArguments().getString("fragMessage");

        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setTitle(title)
                .setMessage(fragMessage)
                .setPositiveButton(R.string.confirm,
                        (dialog, whichButton) -> {
                            Activity activity = getActivity();
                            if(activity instanceof ShowCaseActivity)
                                ((ShowCaseActivity)activity).doPositiveClick();
                            else if(activity instanceof MyBookActivity)
                                ((BorrowRequestsFragment)((MyBookActivity)activity).mSectionsPagerAdapter.getCurrentFragment()).requestAdapter.doPositiveClick();
                        }
                )
                .setNegativeButton(R.string.undo,
                        (dialog, whichButton) -> {
                            Activity activity = getActivity();
                            if(activity instanceof ShowCaseActivity)
                                ((ShowCaseActivity)activity).doNegativeClick();
                            else
                                dialog.dismiss();
                        }
                )
                .create();
    }

}