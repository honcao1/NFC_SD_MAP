package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.os.Message;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.os.Handler;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Declaration Variables
    GoogleMap map;
    Polyline polyline;
    List<LatLng> poly = new ArrayList<LatLng>();
    List<ListLatLng> polyP = new ArrayList<ListLatLng>();

    Intent myFile;
    String path;

    NfcAdapter nfcAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int CONNECT_BT = 2;
    private static final int MESSAGE_READ = 3;

    Handler mHandler;
    StringBuilder dataBluetoorh = new StringBuilder();

    ConnectedThread bluetooth;

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice mBluetoothDevice = null;
    BluetoothSocket mBluetoothSocket = null;

    UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    boolean connect = false;

    private static String MAC = null;

    Button btnGui;
    TextView txtBTData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        addControl();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(),"Bluetooth on", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),"Bluetooth off", Toast.LENGTH_LONG).show();
                }
                break;

            case CONNECT_BT:
                if (resultCode == Activity.RESULT_OK) {
                    MAC = data.getExtras().getString(ListDevices.CONNECT_MAC);

                    //Toast.makeText(getApplicationContext(),"MAC" + MAC, Toast.LENGTH_LONG).show();
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(MAC);
                    try {
                        mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(mUUID);

                        mBluetoothSocket.connect();

                        bluetooth = new ConnectedThread(mBluetoothSocket);
                        bluetooth.start();

                        connect = true;

                        Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_LONG).show();
                    } catch (IOException err){
                        Toast.makeText(getApplicationContext(),"ERR" + err, Toast.LENGTH_LONG).show();
                    }
                } else {
                    //Toast.makeText(getApplicationContext(),"Bluetooth off", Toast.LENGTH_LONG).show();
                }
                break;
            case 10:
                if (resultCode == RESULT_OK) {
                    String folder = data.getData().getPath();

                    String[] folders = folder.split(":");

                    String[] nameFolder = folders[0].split("/");

                    path = nameFolder[2] +"/"+ folders[1];

                    Log.d("file", path);

                    getFileData();

//                    polyline.remove();
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

//                Log.d("dataLatng", String.valueOf(dataLatng.length));

                if (dataLatng.length > 4){
                    checkPointMapNfc(dataLatng);
                } else {
                    Toast.makeText(this, "Khong dung dinh dang", Toast.LENGTH_SHORT).show();
                }
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
            case R.id.menuBlu:
                if (connect){
                    try {
                        mBluetoothSocket.close();
                        Toast.makeText(getApplicationContext(), "DISCONNECT", Toast.LENGTH_LONG).show();
                        connect = false;
                    } catch (IOException err){}
                } else {
                    Intent abreLista = new Intent(MainActivity.this, ListDevices.class);
                    startActivityForResult(abreLista, CONNECT_BT);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.myMap);
        mapFragment.getMapAsync(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        btnGui = findViewById(R.id.btnGui);
        txtBTData = findViewById(R.id.txtBTData);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "App stop", Toast.LENGTH_LONG).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void addControl() {
        btnGui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetooth.write("BT");
            }
        });

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ){
                    String str = (String) msg.obj;

                    dataBluetoorh.append(str);

                    int aa = dataBluetoorh.indexOf("}");
                    if (aa > 0) {
                        String str_1 = dataBluetoorh.substring(0, aa);

                        int aaa = str_1.length();
                        if (dataBluetoorh.charAt(0) == '{'){
                            String str_2 = dataBluetoorh.substring(1, aaa);
                            txtBTData.setText(str_2);
                            Log.d("DataBT", str_2);
                        }
                        dataBluetoorh.delete(0, dataBluetoorh.length());
                    }
                }
            }
        };
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
//                        Log.d("getFile", line);
                        String[] data = null;
                        data = line.split(",");

                        LatLng point = new LatLng(Double.parseDouble(data[2]), Double.parseDouble(data[3]));
                        poly.add(point);

                        ListLatLng listLatLng = new ListLatLng(data[0], data[1], Double.parseDouble(data[2]), Double.parseDouble(data[3]), data[4]);
                        polyP.add(listLatLng);
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

//        for (int i=0; i<poly.size(); i++) {
//            sourcePoints.add(new LatLng(poly.get(i).latitude, poly.get(i).longitude));
//        }

//        Log.d("SizePoly", String.valueOf(polyP.size()));
        for (int i=0; i<polyP.size(); i++) {
            sourcePoints.add(new LatLng(polyP.get(i).getLatitude(), polyP.get(i).getLongitude()));

            if ( (i%100==0) || (i==polyP.size()-1) ){

                MarkerOptions marker = new MarkerOptions();
                marker.title(polyP.get(i).getLatitude()+ " " + polyP.get(i).getLongitude());
                marker.snippet(polyP.get(i).getDate()+" "+polyP.get(i).getTime());
                marker.position(new LatLng(polyP.get(i).getLatitude(), polyP.get(i).getLongitude()));
                marker.icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("icon_tau", 80, 80)));
                map.addMarker(marker);
            }
        }

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(sourcePoints);
        polylineOptions.width(10f);
        polylineOptions.color(Color.rgb(0, 178, 255));

        polyline = map.addPolyline(polylineOptions);
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10f));
        polyline.setPattern(pattern);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sourcePoints.get(0))
                .zoom(12)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void checkPointMapNfc(String[] data) {
//        String date = dataLatng[0].substring(0,2)+"/"+dataLatng[0].substring(2,4)+"/"+ "20" +dataLatng[0].substring(4);
//
//        int hh = Integer.parseInt(dataLatng[1].substring(0,1)) + 7;
//        String time = hh+":"+dataLatng[1].substring(1,3)+":"+dataLatng[1].substring(3,5);

        double lat = Double.parseDouble(data[2]);
        double lng = Double.parseDouble(data[3]);

        ListLatLng pointLatLng = new ListLatLng(data[0], data[1], lat, lng, data[4]);

//        String title = geoLocate(lat, lng);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));

        MarkerOptions marker = new MarkerOptions();
        marker.title(lat + " " + lng);
        marker.snippet(pointLatLng.getDate()+" "+pointLatLng.getTime());
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

            Log.d("tagContent", tagContent);

            if (tagContent.indexOf(",") != 0){
                String[] dataLaLng = tagContent.split(",");
                return dataLaLng;
            }
            else {
                Toast.makeText(this, "Định dạng không đúng", Toast.LENGTH_SHORT).show();
            }
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

    private Bitmap resizeMapIcons(String iconName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] inBuffer = new byte[1024];
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    bytes = mmInStream.read(inBuffer);

//                    String dataBt = new String(inBuffer, 0, bytes);
                    String dataBt = "{241019,4083300,10.777864,106.765650,2.57,7,1,0,1,1}";

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dataBt).sendToTarget();

                } catch (IOException e) {

                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(String dataEvent) {
            try {
                byte[] msgBuffer = dataEvent.getBytes();
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {

            }
        }
    }
}
