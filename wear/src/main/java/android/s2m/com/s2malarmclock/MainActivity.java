package android.s2m.com.s2malarmclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String ALARM_PATH = "/alarm-phone";
    private static final String HOURS_KEY = "hours";
    private static final String MINUTES_KEY = "minutes";
    private static final String LOG_TAG = "MainActivity";
    private WearableListView mWearableListView;
    String[] mElements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mElements = getResources().getStringArray(R.array.listview_times);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.activity_main_watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mWearableListView = (WearableListView) stub.findViewById(R.id.mainActivityListView);
                mWearableListView.setAdapter(new TimeAdapter(MainActivity.this, mElements));
                mWearableListView.setClickListener(new WearableListView.ClickListener() {
                    @Override
                    public void onClick(WearableListView.ViewHolder viewHolder) {
                        Integer position = (Integer) viewHolder.itemView.getTag();
                        String time = mElements[position];
                        int hours = Integer.parseInt(time.substring(0, 2));
                        int minutes = Integer.parseInt(time.substring(3, 5));
                        setAlarm(hours, minutes);
                        AlarmClockDataGenerator alarmClockDataGenerator = new AlarmClockDataGenerator(hours, minutes);
                        alarmClockDataGenerator.run();
                    }

                    @Override
                    public void onTopEmptyRegionClick() {

                    }
                });
            }
        });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Nothing to do here
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Nothing to do here
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Nothing to do here
    }

    @Override
    public void onPeerConnected(Node node) {
        // Nothing to do here
    }

    @Override
    public void onPeerDisconnected(Node node) {
        // Nothing to do here
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Nothing to do here
    }

    private void setAlarm(int hours, int minutes) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Wear Synced Alarm");
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        startActivity(intent);
    }

    private class AlarmClockDataGenerator implements Runnable {
        private int mHours;
        private int mMinutes;

        public AlarmClockDataGenerator(int hours, int minutes) {
            mHours = hours;
            mMinutes = minutes;
        }

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(ALARM_PATH);
            putDataMapRequest.getDataMap().putInt(HOURS_KEY, mHours);
            putDataMapRequest.getDataMap().putInt(MINUTES_KEY, mMinutes);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(LOG_TAG, "ERROR: failed to putDataItem, status code: " + dataItemResult.getStatus().getStatusCode());
                            } else {
                                Intent intent = new Intent(MainActivity.this, ConfirmationActivity.class);
                                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Wecker gestellt");
                                startActivity(intent);
                            }
                        }
                    });
        }
    }

    private class TimeAdapter extends WearableListView.Adapter {
        private final LayoutInflater mInflater;
        private final String[] mTimes;

        private TimeAdapter(Context context, String[] times) {
            mInflater = LayoutInflater.from(context);
            mTimes = times;
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(mInflater.inflate(R.layout.time_list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
            TextView view = (TextView) viewHolder.itemView.findViewById(R.id.time_text);
            view.setText(mTimes[position]);
            viewHolder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mTimes.length;
        }
    }
}
