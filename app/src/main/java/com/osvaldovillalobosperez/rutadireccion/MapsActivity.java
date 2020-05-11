package com.osvaldovillalobosperez.rutadireccion;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private static final int PERMISSIONS_REQUEST_CODE = 1001;
    private static final int MY_PERMISSION_REQUEST_API = 1002;

    String[] appPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        createLocationRequest();

        // TODO: Verifica permisos en tiempo de ejecución.
        if (checkAndRequestPermissions()) {
            // El permiso ya ha sido concedido.

            // Obtain the SupportMapFragment and get notified when the
            // map is ready to be used.
            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    public boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    perm
            ) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    listPermissionsNeeded.toArray(
                            new String[listPermissionsNeeded.size()]
                    ), PERMISSIONS_REQUEST_CODE
            );
            return false;
        }

        return true;
    }

    public AlertDialog showDialog(String title, String msg, String positiveLavel,
                                  DialogInterface.OnClickListener positiveOnClick,
                                  String negativeLavel,
                                  DialogInterface.OnClickListener negativeOnClick,
                                  boolean isCancelAble) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(isCancelAble);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLavel, positiveOnClick);
        builder.setNegativeButton(negativeLavel, negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            HashMap<String, Integer> permissionResult = new HashMap<>();
            int deniedCount = 0;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResult.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            if (deniedCount == 0) {
                // Todos los permisos (contador) son correctos.
            } else {
                for (Map.Entry<String, Integer> entry : permissionResult.entrySet()) {
                    String permName = entry.getKey();
                    int permResult = entry.getValue();

                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            permName)
                    ) {
                        showDialog(
                                "",
                                "Esta aplicación necesita permisos ubicación del Teléfono",
                                "Si, otorgar permisos",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        checkAndRequestPermissions();
                                    }
                                },
                                "No otorgar permisos",
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                },
                                false
                        );
                    } else {
                        showDialog(
                                "",
                                "Has negado algun(os) permiso(s), concede todos los " +
                                        "permisos en Ajustes",
                                "Ir a Ajustes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts(
                                                        "package",
                                                        getPackageName(),
                                                        null)
                                        );
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                "No, salir",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                },
                                false
                        );
                        break;
                    }
                }
            }
        }
    }

    protected void createLocationRequest() {
        /* Primera parte. */
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* Segunda parte. */
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // Esta bien.
                /*Toast.makeText(
                        MainActivity.this,
                        "El SuccessListener de LocationSettingsResponse funciono" +
                                " correctamente",
                        Toast.LENGTH_LONG
                ).show();*/
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Las instancias no están satisfechas.
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(
                                MapsActivity.this,
                                MY_PERMISSION_REQUEST_API
                        );
                        /*Toast.makeText(
                                MainActivity.this,
                                "Fallo el Listener, se necesita la activación de la API",
                                Toast.LENGTH_LONG
                        ).show();*/
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignorar el error.
                    }
                }
            }
        });
    }

    Boolean actualPosition = true;
    JSONObject jsonObject;
    Double longitudOrigen, latitudOrigen;

    LatLng origen, destino;
    ArrayList markerPoints = new ArrayList();

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(
                this,
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitudOrigen = location.getLatitude();
                            longitudOrigen = location.getLongitude();
                            actualPosition = false;

                            LatLng miPosicion = new LatLng(latitudOrigen, longitudOrigen);

                            /*mMap.addMarker(new MarkerOptions()
                                    .position(miPosicion)
                                    .title("Mi posición actual")
                                    .snippet("Origen"));*/

                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(latitudOrigen, longitudOrigen))
                                    .zoom(14)
                                    .build();
                            mMap.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                            );
                        } else {
                            Toast.makeText(
                                    MapsActivity.this,
                                    "No se pudo obtener su ubicación...",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
                }
        );
        // Coordenadas ITSUR: 20.1394083, -101.1507207
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                markerPoints.add(latLng);

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);

                if (markerPoints.size() == 1) {
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Marcador Origen")
                            .snippet("Origen"));
                } else if (markerPoints.size() == 2) {
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("Marcador Destino")
                            .snippet("Destino"));
                }

                if (markerPoints.size() >= 2) {
                    origen = (LatLng) markerPoints.get(0);
                    destino = (LatLng) markerPoints.get(1);

                    String url =
                            "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                                    origen.latitude + "," + origen.longitude +
                                    "&destination=" + destino.latitude + "," + destino.longitude +
                                    "&key=AIzaSyC2-KpjjCwUXSpCLWh4mt4KRKFEPtGz4Rs";

                    RequestQueue queue = Volley.newRequestQueue(MapsActivity.this);
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                jsonObject = new JSONObject(response);
                                TrazarRuta(jsonObject);

                                Log.i("JSONruta: ", response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

                    queue.add(stringRequest);
                }
            }
        });
    }

    private void TrazarRuta(JSONObject jso) {
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {
            jRoutes = jso.getJSONArray("routes");
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) (jRoutes.get(i))).getJSONArray("legs");

                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "" + ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        Log.i("END", "" + polyline);
                        List<LatLng> list = PolyUtil.decode(polyline);
                        mMap.addPolyline(new PolylineOptions().addAll(list).color(Color.GREEN).width(5));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
