package android.s2m.com.s2malarmclock;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AlarmClockWatchFace extends Activity {
    private TextView mTextViewTime;
    private TextView mTextViewAlarm;
    private TextView mTextViewBattery;

    private final static IntentFilter INTENT_FILTER;
    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    }

    private final String TIME_FORMAT_DISPLAYED = "kk:mm";

    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTextViewTime.setText(new SimpleDateFormat(TIME_FORMAT_DISPLAYED).format(Calendar.getInstance().getTime()));
        }
    };

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTextViewBattery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_clock_watch_face);
        final WatchViewStub watchViewStub = (WatchViewStub) findViewById(R.id.activity_alarm_clock_watch_face_view_stub);
        watchViewStub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextViewTime = (TextView) stub.findViewById(R.id.activity_alarm_clock_watch_face_time);
                mTextViewAlarm = (TextView) stub.findViewById(R.id.activity_alarm_clock_watch_face_alarm);
                mTextViewBattery = (TextView) stub.findViewById(R.id.activity_alarm_clock_watch_face_battery);

                mTimeInfoReceiver.onReceive(AlarmClockWatchFace.this, registerReceiver(null, INTENT_FILTER));
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
                registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                AlarmManager alarmManager = (AlarmManager) stub.getContext().getSystemService(Context.ALARM_SERVICE);
//                String nextAlarm = Settings.System.getString(getContentResolver(), alarmManager.getNextAlarmClock().toString());
                mTextViewAlarm.setText("ab");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTimeInfoReceiver);
        unregisterReceiver(mBatteryInfoReceiver);
    }
}
