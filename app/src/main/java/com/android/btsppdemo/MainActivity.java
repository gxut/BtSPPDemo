package com.android.btsppdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.btsppdemo.client.BtClientActivity;
import com.android.btsppdemo.server.BtServerActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private Button mClient;
    private Button mServer;
    private TextView mBtStatus;
    private ImageView mBtStatusImg;
    private static final String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };
    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermission();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            updateBtStatus(BluetoothAdapter.STATE_OFF);
        } else {
            updateBtStatus(BluetoothAdapter.STATE_ON);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.client:
                final Intent intentClient = new Intent(this, BtClientActivity.class);
                startActivity(intentClient);
                break;
            case R.id.server:
                final Intent intentServer = new Intent(this, BtServerActivity.class);
                startActivity(intentServer);
                break;
            case R.id.bluetooth_status_img:
                enableBt();
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                for (int i = 0; i < permissions.length; ++i) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Request " + permissions[i] + " succeeded", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Request " + permissions[i] + " failed", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    updateBtStatus(BluetoothAdapter.STATE_ON);
                    updateButtonStatus(true);
                } else {
                    updateBtStatus(BluetoothAdapter.STATE_OFF);
                    updateButtonStatus(false);
                }
                break;
            default:
                break;
        }
    }

    private void initView() {
        mClient = findViewById(R.id.client);
        mServer = findViewById(R.id.server);
        mClient.setOnClickListener(this);
        mServer.setOnClickListener(this);
        if (!mBtAdapter.isEnabled()) {
            updateButtonStatus(false);
        }
        mBtStatus = findViewById(R.id.bluetooth_status);
        mBtStatusImg = findViewById(R.id.bluetooth_status_img);
        mBtStatusImg.setOnClickListener(this);
    }

    private void updateButtonStatus(boolean enable) {
        mServer.setEnabled(enable);
        mClient.setEnabled(enable);
    }

    public void checkAndRequestPermission() {
        final List<String> requestPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(permission);
            }
        }
        // request permission
        if (requestPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, requestPermissions.toArray(new String[requestPermissions.size()]), REQUEST_PERMISSIONS);
        }
    }

    private void updateBtStatus(int status) {
        switch (status) {
            case BluetoothAdapter.STATE_OFF:
                mBtStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
                mBtStatus.setText(getResources().getString(R.string.bluetooth_disabled));
                mBtStatusImg.setImageResource(R.drawable.bluetooth_disable);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                mBtStatus.setTextColor(ContextCompat.getColor(this, R.color.red));
                mBtStatus.setText(getResources().getString(R.string.bluetooth_turning_on));
                mBtStatusImg.setImageResource(R.drawable.bluetooth_disable);
                break;
            case BluetoothAdapter.STATE_ON:
                mBtStatus.setTextColor(ContextCompat.getColor(this, R.color.blue));
                mBtStatus.setText(getResources().getString(R.string.bluetooth_enabled));
                mBtStatusImg.setImageResource(R.drawable.bluetooth_regular);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mBtStatus.setTextColor(ContextCompat.getColor(this, R.color.blue));
                mBtStatus.setText(getResources().getString(R.string.bluetooth_turning_off));
                mBtStatusImg.setImageResource(R.drawable.bluetooth_regular);
                break;

        }
    }

    private void enableBt() {
        if (mBtAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show();
        }
    }
}