package org.pouria.maptest;

import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import wiadevelopers.com.directionlib.DirectionCallback;
import wiadevelopers.com.directionlib.GoogleDirection;
import wiadevelopers.com.directionlib.constant.TransportMode;
import wiadevelopers.com.directionlib.constant.Unit;
import wiadevelopers.com.directionlib.model.Direction;
import wiadevelopers.com.directionlib.model.RouteInfo;
import wiadevelopers.com.directionlib.util.MapUtils;

public class DirectionActivity extends FragmentActivity implements OnMapReadyCallback
{

    private GoogleMap mMap;

    // findView
    @BindView(R.id.txtOrigin)
    TextView txtOrigin;
    @BindView(R.id.txtDestination)
    TextView txtDestination;
    @BindView(R.id.txtWalking)
    TextView txtWalking;
    @BindView(R.id.txtDriving)
    TextView txtDriving;
    @BindView(R.id.imgDriving)
    ImageView imgDriving;
    @BindView(R.id.imgWalking)
    ImageView imgWalking;
    @BindView(R.id.rltvDriving)
    RelativeLayout rltvDriving;
    @BindView(R.id.rltvWalking)
    RelativeLayout rltvWalking;
    @BindView(R.id.btnRequestDirection)
    FloatingActionButton btnRequestDirection;

    private static int COLOR_WHITE;
    private static int COLOR_PRIMARY;

    private final static int NONE = 65;
    private final static int WALKING = 97;
    private final static int DRIVING = 51;

    private final LatLng mIRAN = new LatLng(32.2712623, 51.2585412);

    private Marker markerOrigin = null;
    private ArrayList<Marker> markerDestination = new ArrayList<>();

    private ArrayList<RouteInfo> routeInfosDriving = new ArrayList<>();
    private ArrayList<Polyline> polylinesDriving = new ArrayList<>();

    private ArrayList<RouteInfo> routeInfosWalking = new ArrayList<>();
    private ArrayList<Polyline> polylinesWalking = new ArrayList<>();


    private int index = -1;

    private String transportMode = TransportMode.DRIVING;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);
        ButterKnife.bind(this);

        initialize();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mIRAN, 5));
        setListeners();
    }

    private void setListeners()
    {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener()
        {
            @Override
            public void onMapLongClick(LatLng latLng)
            {
                MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .draggable(false)
                        .rotation(0);

                if (markerOrigin == null)
                    markerOrigin = mMap.addMarker(markerOptions);
                else if (markerDestination.size() < 5)
                        markerDestination.add(mMap.addMarker(markerOptions));



                if (markerDestination.size() > 4  && markerOrigin != null)
                    btnRequestDirection.setVisibility(View.VISIBLE);

                Geocoder geocoder = new Geocoder(DirectionActivity.this);
                List<Address> addresses = null;
                try
                {
                    addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    String location = addresses.get(0).getLocality();

                    if (location == null || location.equals(""))
                        location = addresses.get(0).getSubThoroughfare();
                    if (location == null || location.equals(""))
                        location = addresses.get(0).getLatitude() + " , " + addresses.get(0).getLongitude();

                    if (markerOrigin != null && markerDestination == null)
                        txtOrigin.setText(location);
                    else if (markerOrigin != null && markerDestination != null)
                        txtDestination.setText(location);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });


        btnRequestDirection.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                drivingRequest();
                walkingRequest();
            }
        });


        rltvDriving.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (routeInfosDriving.size() != 0)
                    activator(DRIVING);
            }
        });

        rltvWalking.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (routeInfosWalking.size() != 0)
                    activator(WALKING);
            }
        });

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener()
        {
            @Override
            public void onPolylineClick(Polyline polyline)
            {
                String tag = polyline.getTag().toString();
                index = Integer.parseInt(tag);

                if (transportMode.equals(TransportMode.DRIVING))
                {
                    polylinesDriving.get(index).remove();
                    final Polyline mPolyline = mMap.addPolyline(routeInfosDriving.get(index).getPolylineOptions());
                    mPolyline.setTag(index);
                    polylinesDriving.set(index, mPolyline);

                    for (int i = 0; i < polylinesDriving.size(); i++)
                    {
                        if (i != index)
                            polylinesDriving.get(i).setColor(Direction.UNSELECTED_ROUTE);
                        else
                            polylinesDriving.get(i).setColor(Direction.SELECTED_ROUTE);
                    }

                    txtDriving.setText(routeInfosDriving.get(index).getDurationText());
                }
                else if (transportMode.equals(TransportMode.WALKING))
                {
                    polylinesWalking.get(index).remove();
                    final Polyline mPolyline = mMap.addPolyline(routeInfosWalking.get(index).getPolylineOptions());
                    mPolyline.setTag(index);
                    mPolyline.setPattern(MapUtils.getPattern(MapUtils.patternType.DOT));
                    polylinesWalking.set(index, mPolyline);

                    for (int i = 0; i < polylinesWalking.size(); i++)
                    {
                        if (i != index)
                            polylinesWalking.get(i).setColor(Direction.UNSELECTED_ROUTE);
                        else
                            polylinesWalking.get(i).setColor(Direction.SELECTED_ROUTE);
                    }

                    txtWalking.setText(routeInfosWalking.get(index).getDurationText());
                }
            }
        });
    }

    private void drivingRequest()
    {
        GoogleDirection.withServerKey(Constant.API_KEY)
                .from(markerOrigin.getPosition())
                .to(markerDestination.get(0).getPosition())
                .transportMode(TransportMode.DRIVING)
                .alternativeRoute(true)
                .unit(Unit.METRIC)
                .execute(new DirectionCallback()
                {
                    @Override
                    public void onDirectionSuccess(Direction direction, String s)
                    {
                        if (direction.isOK())
                        {
                            routeInfosDriving = direction.getRouteInfo(DirectionActivity.this, 5);
                            txtDriving.setText(routeInfosDriving.get(routeInfosDriving.size() - 1).getDurationText());
                            activator(DRIVING);
                        }
                        else
                            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onDirectionFailure(Throwable throwable)
                    {

                    }
                });
    }

    private void walkingRequest()
    {
        GoogleDirection.withServerKey(Constant.API_KEY)
                .from(markerOrigin.getPosition())
                .to(markerDestination.get(0).getPosition())
                .transportMode(TransportMode.WALKING)
                .alternativeRoute(true)
                .unit(Unit.METRIC)
                .execute(new DirectionCallback()
                {
                    @Override
                    public void onDirectionSuccess(Direction direction, String s)
                    {
                        if (direction.isOK())
                        {
                            routeInfosWalking = direction.getRouteInfo(DirectionActivity.this, 5);
                            txtWalking.setText(routeInfosWalking.get(routeInfosWalking.size() - 1).getDurationText());
                        }
                        else
                            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onDirectionFailure(Throwable throwable)
                    {

                    }
                });
    }

    private void initialize()
    {

        setup();
        activator(NONE);
    }



    private void drawRouts(int num)
    {
        for (int i = 0; i < polylinesDriving.size(); i++)
            polylinesDriving.get(i).remove();
        for (int i = 0; i < polylinesWalking.size(); i++)
            polylinesWalking.get(i).remove();

        polylinesDriving.clear();
        polylinesWalking.clear();

        if (num == DRIVING)
        {
            for (int i = 0; i < routeInfosDriving.size(); i++)
            {
                final Polyline polyline = mMap.addPolyline(routeInfosDriving.get(i).getPolylineOptions());
                polyline.setTag(i);
                polylinesDriving.add(polyline);
            }
        }
        else if (num == WALKING)
        {
            for (int i = 0; i < routeInfosWalking.size(); i++)
            {
                final Polyline polyline = mMap.addPolyline(routeInfosWalking.get(i).getPolylineOptions());
                polyline.setPattern(MapUtils.getPattern(MapUtils.patternType.DOT));
                polyline.setTag(i);
                polylinesWalking.add(polyline);
            }
        }
    }

    private void setup()
    {
        txtDriving.setText("-");
        txtWalking.setText("-");

        COLOR_WHITE = ContextCompat.getColor(getApplicationContext(), R.color.white);
        COLOR_PRIMARY = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);

        btnRequestDirection.setVisibility(View.GONE);
    }

    private void activator(int num)
    {
        final PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
        if (num == NONE)
        {
            rltvWalking.setBackgroundResource(R.drawable.button_blue);
            txtWalking.setTextColor(COLOR_WHITE);
            imgWalking.setColorFilter(COLOR_WHITE, mode);

            rltvDriving.setBackgroundResource(R.drawable.button_blue);
            txtDriving.setTextColor(COLOR_WHITE);
            imgDriving.setColorFilter(COLOR_WHITE, mode);
        }
        else if (num == DRIVING)
        {
            rltvWalking.setBackgroundResource(R.drawable.button_blue);
            txtWalking.setTextColor(COLOR_WHITE);
            imgWalking.setColorFilter(COLOR_WHITE, mode);

            rltvDriving.setBackgroundResource(R.drawable.button_white);
            txtDriving.setTextColor(COLOR_PRIMARY);
            imgDriving.setColorFilter(COLOR_PRIMARY, mode);
            drawRouts(DRIVING);
            transportMode = TransportMode.DRIVING;
        }
        else if (num == WALKING)
        {
            rltvWalking.setBackgroundResource(R.drawable.button_white);
            txtWalking.setTextColor(COLOR_PRIMARY);
            imgWalking.setColorFilter(COLOR_PRIMARY, mode);

            rltvDriving.setBackgroundResource(R.drawable.button_blue);
            txtDriving.setTextColor(COLOR_WHITE);
            imgDriving.setColorFilter(COLOR_WHITE, mode);
            drawRouts(WALKING);
            transportMode = TransportMode.WALKING;
        }
    }
}
