package kimle.michal.android.taxitwin.fragment;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kimle.michal.android.taxitwin.R;
import kimle.michal.android.taxitwin.contentprovider.TaxiTwinContentProvider;
import kimle.michal.android.taxitwin.db.DbContract;

public class TaxiTwinMapFragment extends MapFragment {

    public interface MapViewListener {

        public abstract void onMapCreated();
    }

    private static final String LOG = "TaxiTwinMapFragment";
    private static final int PADDING = 50;
    private MapViewListener mapViewListener;
    private Map<Marker, Long> markers;
    private Marker currentMarker;
    private Location currentLocation;
    private boolean mapReady = false;
    private boolean updateMap = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(LOG, "in onCreateView");
        Log.d(LOG, "mapViewListener: " + mapViewListener);
        getMap().setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mapReady = true;
                if (updateMap) {
                    updateCamera();
                }
            }
        });
        if (mapViewListener != null) {
            mapViewListener.onMapCreated();
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(LOG, "in onAttach");
        try {
            mapViewListener = (MapViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MapViewListener");
        }
    }

    public void loadData() {
        if (markers != null) {
            for (Marker m : markers.keySet()) {
                m.remove();
            }
        }
        markers = new HashMap<Marker, Long>();

        String[] projection = {
            DbContract.DbEntry.OFFER_ID_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LONGITUDE_COLUMN,
            DbContract.DbEntry.POINT_END_TABLE + "." + DbContract.DbEntry.POINT_LATITUDE_COLUMN};

        Cursor cursor = getActivity().getContentResolver().query(TaxiTwinContentProvider.OFFERS_URI, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.POINT_LONGITUDE_COLUMN));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.DbEntry.POINT_LATITUDE_COLUMN));
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DbEntry._ID));

                MarkerOptions markerOptions = new MarkerOptions();
                LatLng markerPos = new LatLng(latitude, longitude);
                markerOptions.position(markerPos);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.flag));
                markerOptions.anchor(0.11f, 0.93f);
                Marker marker = getMap().addMarker(markerOptions);
                Log.d(LOG, "marker: " + marker.toString());

                markers.put(marker, id);
            } while (cursor.moveToNext());
            cursor.close();
        }

        updateCamera();
        Log.d(LOG, "markers: " + markers);
    }

    public void updateCurrentLocation(Location location) {
        Log.d(LOG, "location: " + location);
        currentLocation = location;
        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        if (currentMarker == null) {
            MarkerOptions currentMarkerOptions = new MarkerOptions();
            currentMarkerOptions.position(currentLatLng);
            currentMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.you_pin));
            currentMarkerOptions.anchor(0.14f, 0.67f);
            currentMarker = getMap().addMarker(currentMarkerOptions);
        } else {
            currentMarker.setPosition(currentLatLng);
        }

        updateCamera();
    }

    private void updateCamera() {
        if (!mapReady) {
            updateMap = true;
            return;
        }

        List<Marker> tmpList = new ArrayList<Marker>();
        if (currentMarker != null) {
            tmpList.add(currentMarker);
        }

        if (markers != null) {
            tmpList.addAll(markers.keySet());
        }

        if (tmpList.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : tmpList) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();

        getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING));
    }
}
