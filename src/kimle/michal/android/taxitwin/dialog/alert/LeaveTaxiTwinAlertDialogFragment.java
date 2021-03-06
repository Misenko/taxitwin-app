package kimle.michal.android.taxitwin.dialog.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.activity.MainActivity;
import kimle.michal.android.taxitwin.application.TaxiTwinApplication;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.enumerate.UserState;
import kimle.michal.android.taxitwin.gcm.GcmHandler;

public class LeaveTaxiTwinAlertDialogFragment extends DialogFragment {

    private static final String OWNER_KEY = "owner";

    public static LeaveTaxiTwinAlertDialogFragment newInstance(boolean owner) {
        LeaveTaxiTwinAlertDialogFragment frag = new LeaveTaxiTwinAlertDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(OWNER_KEY, owner);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final boolean owner = getArguments().getBoolean(OWNER_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.leave_taxitwin_alert_message)
                .setTitle(R.string.leave_taxitwin_alert_title)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GcmHandler gcmHandler = new GcmHandler(getActivity());
                        gcmHandler.leaveTaxiTwin();

                        long taxitwinId = 0;
                        String[] projection = {DbContract.DbEntry.TAXITWIN_ID_COLUMN};
                        Cursor cursor = getActivity().getContentResolver().query(TaxiTwinContentProvider.RIDES_URI, projection, null, null, null);
                        if (cursor != null && cursor.getCount() != 0) {
                            cursor.moveToFirst();
                            taxitwinId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));
                        }

                        if (taxitwinId != 0) {
                            if (owner) {
                                Uri uri = Uri.parse(TaxiTwinContentProvider.TAXITWINS_URI + "/" + taxitwinId);
                                getActivity().getContentResolver().delete(uri, null, null);
                            } else {
                                String[] proj = {DbContract.DbEntry.OFFER_ID_COLUMN};
                                String selection = DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN + " = ?";
                                String[] selectionArgs = {String.valueOf(taxitwinId)};
                                Cursor c = getActivity().getContentResolver().query(TaxiTwinContentProvider.OFFERS_URI, proj, selection, selectionArgs, null);
                                if (c != null && c.getCount() != 0) {
                                    c.moveToFirst();
                                    long offerId = c.getLong(c.getColumnIndexOrThrow(DbContract.DbEntry._ID));

                                    Uri uri = Uri.parse(TaxiTwinContentProvider.OFFERS_URI + "/" + offerId);
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(DbContract.DbEntry.OFFER_PASSENGERS_COLUMN, 0);
                                    getActivity().getContentResolver().update(uri, contentValues, null, null);
                                }
                            }
                        }

                        getActivity().getContentResolver().delete(TaxiTwinContentProvider.RIDES_URI, null, null);

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                        TaxiTwinApplication.setUserState(UserState.SUBSCRIBED);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }
}
