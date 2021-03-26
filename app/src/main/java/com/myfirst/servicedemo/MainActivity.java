package com.myfirst.servicedemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final String BROADCAST_ACTION = "com.daria.example.servicebackbroadcast";
    public static final String MAX_NUMBER_KEY = "MAX_NUMBER_KEY";
    public static final String PENDING_INTENT_KEY = "PENDING_INTENT_KEY";

    public static final int REQUEST_CODE = 4003;

    private NumberPicker picker;
    private TextView tvLog;

    private Intent serviceIntent;
    private ServiceConnection serviceConnection;
    private MyService myService;
    private boolean bound = false;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        picker = findViewById(R.id.np_number);
        picker.setMinValue(1);
        picker.setMaxValue(100);
        picker.setValue(50);

        tvLog = findViewById(R.id.tv_log);
        tvLog.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MyService.NUMBER_KEY)) {
            int number = intent.getIntExtra(MyService.NUMBER_KEY, 0);
            tvLog.append("Random number: " + number + "\n");
        }

        serviceIntent = new Intent(this, MyService.class);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MainActivity.this.onServiceConnected(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                MainActivity.this.onServiceDisconnected();
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int number = intent.getIntExtra(MyService.NUMBER_KEY, 0);
                tvLog.append("Random number: " + number + "\n");
            }
        };
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void onServiceConnected(IBinder service) {
        tvLog.append("BOUND to service\n");
        myService = ((MyService.MyBinder) service).getService();
        myService.maxNumber = picker.getValue();
        picker.setOnValueChangedListener((picker, oldVal, newVal) -> myService.maxNumber = newVal);
        bound = true;
    }

    private void onServiceDisconnected() {
        tvLog.append("UNBOUND from service\n");
        myService = null;
        picker.setOnValueChangedListener(null);
        bound = false;
    }

    public void onStartService(View view) {
        PendingIntent pendingIntent = createPendingResult(REQUEST_CODE, new Intent(), 0);

        serviceIntent.putExtra(MAX_NUMBER_KEY, picker.getValue());
        serviceIntent.putExtra(PENDING_INTENT_KEY, pendingIntent);
        startService(serviceIntent);
    }

    public void onStopService(View view) {
        stopService(serviceIntent);
    }

    public void onBindService(View view) {
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void onUnbindService(View view) {
        if (!bound) return;
        unbindService(serviceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            switch (resultCode) {
                case MyService.START_KEY:
                    tvLog.append("START service task\n");
                    break;
                case MyService.STOP_KEY:
                    tvLog.append("STOP service task\n");
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        if (bound) {
            onUnbindService(null);
        }
        super.onDestroy();
    }
}