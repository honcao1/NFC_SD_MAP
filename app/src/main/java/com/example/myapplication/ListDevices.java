package com.example.myapplication;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Set;

public class ListDevices extends ListActivity {
    BluetoothAdapter mBluetoothAdapter1 = null;

    static String CONNECT_MAC = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        mBluetoothAdapter1 = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter1.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                ArrayBluetooth.add(deviceName + "\n" + deviceHardwareAddress);
            }
        }
        setListAdapter(ArrayBluetooth);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String inforbluetooth = ((TextView)v).getText().toString();
        //Toast.makeText(getApplicationContext(),"infor: " + inforbluetooth, Toast.LENGTH_LONG).show();
        String enderMac = inforbluetooth.substring(inforbluetooth.length()-17);
        //Toast.makeText(getApplicationContext(),"mac: " + enderMac, Toast.LENGTH_LONG).show();

        Intent retorMac = new Intent();
        retorMac.putExtra(CONNECT_MAC, enderMac);
        setResult(RESULT_OK, retorMac);
        finish();
    }
}
