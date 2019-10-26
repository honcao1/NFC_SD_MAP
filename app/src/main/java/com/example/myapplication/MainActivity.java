package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Declaration Variables
    GoogleMap map;
    List<LatLng> poly = new ArrayList<LatLng>();
    List<ListLatLng> pointPoly = new ArrayList<ListLatLng>();

    Intent myFile;
    String path;

    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK) {
                    String folder = data.getData().getPath();

                    String[] folders = folder.split(":");

                    String[] nameFolder = folders[0].split("/");

                    path = nameFolder[2] +"/"+ folders[1];

                    Log.d("file", path);

                    getFileData();

                    drawPolyLatLng();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disableForegroundDispatchSystem();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)){
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (parcelables != null && parcelables.length > 0) {
                String[] dataLatng = readTextFromMesage((NdefMessage)parcelables[0]);

                checkPointMapNfc(dataLatng);

            } else {
                Toast.makeText(this, "No ndef message found!!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

//         Initialize Markers
//        map.addMarker(new MarkerOptions().position(startLatLng));
//        map.addMarker(new MarkerOptions().position(endLatLng));
//         Tranform the string into a json object
//        double[] latAll = {10.797836, 10.852781, 10.855738, 10.841301, 10.693279   };
//        double[] lngAll = {106.656218, 106.620827, 106.600209, 106.595871, 106.595710  };
//        int lengthData = latAll.length;
//        try {
//            // Tranform the string into a json object
//            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latAll[0], lngAll[0]), 12));
//            List<LatLng> poly = new ArrayList<LatLng>();
//            for (int i = 0; i < lengthData; i++)
//            {
//                LatLng point = new LatLng(latAll[i], lngAll[i]);
//                poly.add(point);
//                map.addMarker(new MarkerOptions().position(point));
//            }

//            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(poly.get(0).latitude, poly.get(0).longitude), 12));
//
//            for (int z = 0; z < poly.size() - 1; z++) {
//                LatLng src = poly.get(z);
//                LatLng dest = poly.get(z + 1);
//                line = map.addPolyline(new PolylineOptions()
//                        .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude, dest.longitude))
//                        .width(5).color(Color.RED));
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sd, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuFile:
                myFile = new Intent(Intent.ACTION_GET_CONTENT);
                myFile.setType("*/*");
                startActivityForResult(myFile, 10);
                Log.d("OpenFile", "OpenFile");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.myMap);
        mapFragment.getMapAsync(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private boolean checkExternalStorageState(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//            String state = Environment.getExternalStorageState();
            String state = Environment.getExternalStorageState(new File("storage/C474-8515"));

            if (Environment.MEDIA_MOUNTED.equals(state)){
                Log.d("State", "true");
                return true;
            } else {
                Log.d("State", "false");
                return false;
            }
        }
        return false;
    }

    private void getFileData(){
//        File file = new File("/storage/"+path, "test.txt");
        File file = new File("/storage" + "/" + path);

        Log.d("getFile", String.valueOf(file));

        String[] dataLat = null;

        if (file.exists()){
            Log.d("getFile", "load data");
            StringBuilder sp = new StringBuilder();

            try {
                FileInputStream fis = new FileInputStream(file);

                if (fis != null){
                    InputStreamReader isr = new InputStreamReader(fis);
                    BufferedReader buff = new BufferedReader(isr);

                    String line = null;

                    while ((line = buff.readLine()) != null){
                        Log.d("getFile", line);
                        String[] data = null;
                        data = line.split(",");

                        LatLng point = new LatLng(Double.parseDouble(data[2]), Double.parseDouble(data[3]));
                        poly.add(point);

//                        ListLatLng listLatLng = new ListLatLng(data[0], data[1], Double.parseDouble(data[2]), Double.parseDouble(data[3]), data[4]);
//                        pointPoly.add(listLatLng);
                    }
                    fis.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("File exists", "Sorry file doesn't exist!!");
        }
    }

    private void drawPolyLatLng(){
        List<LatLng> sourcePoints = new ArrayList<>();
        for (int i=0; i<poly.size(); i++) {
            sourcePoints.add(new LatLng(poly.get(i).latitude, poly.get(i).longitude));

            if (i/50==0) {
                MarkerOptions marker = new MarkerOptions();
                marker.title(poly.get(i).latitude+" "+poly.get(i).longitude);
                marker.snippet("Date/Time");
                marker.position(new LatLng(poly.get(i).latitude, poly.get(i).longitude));
                marker.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("icon_tau", 100, 100)));
                map.addMarker(marker);
            }
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(sourcePoints);
        polylineOptions.width(10f);
        polylineOptions.color(Color.rgb(0, 178, 255));

        Polyline polyline = map.addPolyline(polylineOptions);
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10f));
        polyline.setPattern(pattern);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sourcePoints.get(0))
                .zoom(12)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void checkPointMapNfc(String[] dataLatng) {
        String date = dataLatng[0].substring(0,2)+"/"+dataLatng[0].substring(2,4)+"/"+ "20" +dataLatng[0].substring(4);

        int hh = Integer.parseInt(dataLatng[1].substring(0,1)) + 7;
        String time = hh+":"+dataLatng[1].substring(1,3)+":"+dataLatng[1].substring(3,5);

        double lat = Double.parseDouble(dataLatng[2]);
        double lng = Double.parseDouble(dataLatng[3]);

        String title = geoLocate(lat, lng);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));

        MarkerOptions marker = new MarkerOptions();
        marker.title(title);
        marker.snippet(date+" "+time);
        marker.position(new LatLng(lat, lng));
        marker.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("icon_ca", 100, 100)));
        map.addMarker(marker);
    }

    private void enableForegroundDispatchSystem(){
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilter = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    private void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
    }

    private String[] readTextFromMesage(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if (ndefRecords != null && ndefRecords.length > 0){
            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);

//            if (tagContent.indexOf("geo:") >= 0) {
//                String[] latlng= tagContent.split(",");
//
//                edtLat.setText(latlng[0].substring(4).trim());
//                edtLng.setText(latlng[1].trim());
//            } else {
//                Toast.makeText(this, "Khong dung dinh dang", Toast.LENGTH_SHORT).show();
//                return;
//            }
            String[] dataLaLng = tagContent.split(",");
            return dataLaLng;
        } else {
            Toast.makeText(this, "No ndef records found!!!", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public String getTextFromNdefRecord(NdefRecord ndefRecord){
        String tagContent = null;

        try {
            byte[] payLoad = ndefRecord.getPayload();
            String textEncoding = ((payLoad[0] & 128)==0) ? "UTF-8" : "UTF-16";
            int lanSize = payLoad[0] & 0063;

            tagContent = new String(payLoad, lanSize + 1, payLoad.length - lanSize - 1, textEncoding);

        } catch (UnsupportedEncodingException e){
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }

        return tagContent;
    }

    private String geoLocate(double lat, double lng){
        Geocoder geocoder = new Geocoder(this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocation(lat, lng, 1);
        } catch (IOException e) {
            Log.e("geoLocate IOException: ", e.getMessage());
        }

        String str = null;
        String datatp = "Hồ Chí Minh";
        if (list.size() > 0) {
            Address address = list.get(0);

            if (address.getAdminArea().indexOf(datatp) >= 0) {
                str = "Nằm trong TpHCM";
            } else {
                str = "Nằm ngoài TpHCM";
            }
            Log.d("Address", address.getAdminArea());
        }
        Log.d("checkstr", str);
        return str;
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }
}
