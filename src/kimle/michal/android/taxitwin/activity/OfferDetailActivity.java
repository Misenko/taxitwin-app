package kimle.michal.android.taxitwin.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.skd.centeredcontentbutton.CenteredContentButton;
import java.util.ArrayList;
import java.util.List;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;
import kimle.michal.android.taxitwin.dialog.error.OfferErrorDialogFragment;
import kimle.michal.android.taxitwin.gcm.GcmHandler;

public class OfferDetailActivity extends Activity {

    private static final int PADDING = 60;
    private LatLng start;
    private LatLng end;
    private Uri offerUri;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.offer_detail);

        Bundle extras = getIntent().getExtras();
        Uri taskUri = (icicle == null) ? null : (Uri) icicle.getParcelable(TaxiTwinContentProvider.OFFER_CONTENT_ITEM_TYPE);
        if (extras != null) {
            taskUri = extras.getParcelable(TaxiTwinContentProvider.OFFER_CONTENT_ITEM_TYPE);
        }
        fillData(taskUri);
        offerUri = taskUri;

        MapsInitializer.initialize(getApplicationContext());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        final GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                List<Marker> markers = new ArrayList<Marker>();

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(start);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                markerOptions.anchor(0.07f, 0.93f);
                markers.add(map.addMarker(markerOptions));

                markerOptions.position(end);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                markerOptions.anchor(0.11f, 0.93f);
                markers.add(map.addMarker(markerOptions));

                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                markerOptions.position(current);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
                markerOptions.anchor(0.14f, 0.67f);
                markers.add(map.addMarker(markerOptions));

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : markers) {
                    builder.include(marker.getPosition());
                }

                LatLngBounds bounds = builder.build();

                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING));
            }
        });

        CenteredContentButton accept = (CenteredContentButton) findViewById(R.id.accept_button);
        accept.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                String[] projection = {DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN};
                Cursor cursor = getContentResolver().query(offerUri, projection, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();

                    long taxitwinId = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry.OFFER_TAXITWIN_ID_COLUMN));

                    intent.putExtra(GcmHandler.GCM_DATA_TAXITWIN_ID, taxitwinId);
                    setResult(MainActivity.RESULT_ACCEPT_OFFER, intent);
                } else {
                    DialogFragment errorFragment = new OfferErrorDialogFragment();
                    errorFragment.show(getFragmentManager(), "offer_error");
                    setResult(RESULT_CANCELED, null);
                }

                finish();
            }
        });
    }

    private void fillData(Uri taskUri) {
        String[] projection = {
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN,
            DbContract.DbEntry.POINT_START_TABLE + "." + DbContract.DbEntry.POINT_TEXTUAL_COLUMN + " as " + DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN,
            DbContract.DbEntry.TAXITWIN_NAME_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN,
            DbContract.DbEntry.OFFER_PASSENGERS_COLUMN};

        Cursor cursor = getContentResolver().query(taskUri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            ((TextView) findViewById(R.id.name_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.TAXITWIN_NAME_COLUMN)));
            ((TextView) findViewById(R.id.start_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_TEXTUAL_COLUMN)));
            ((TextView) findViewById(R.id.end_address_content)).setText(cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_TEXTUAL_COLUMN)));
            String passengersText = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.OFFER_PASSENGERS_COLUMN)) + "/" + cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.DbEntry.OFFER_PASSENGERS_TOTAL_COLUMN));
            ((TextView) findViewById(R.id.passengers_content)).setText(passengersText);

            start = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_START_POINT_LONGITUDE_COLUMN)));
            end = new LatLng(cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LATITUDE_COLUMN)), cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.AS_END_POINT_LONGITUDE_COLUMN)));
        }
    }

}
