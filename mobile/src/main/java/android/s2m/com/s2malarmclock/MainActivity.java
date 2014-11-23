package android.s2m.com.s2malarmclock;

import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.AlarmClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public class MainActivity extends ActionBarActivity implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static final String ALARM_PATH = "/alarm-watch";
    private static final String HOURS_KEY = "hours";
    private static final String MINUTES_KEY = "minutes";

    private TimePicker mTimePicker;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private Button mSetAlarmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimePicker = (TimePicker) findViewById(R.id.mainActivityTimePicker);
        mTimePicker.setIs24HourView(true);
        mSetAlarmButton = (Button) findViewById(R.id.mainActivitySetAlarmButton);
        mSetAlarmButton.setOnClickListener(this);
        mSetAlarmButton.setVisibility(View.GONE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mainActivitySetAlarmButton:
                setAlarm(mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
                syncAlarm(mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
                break;
            default:
                break;
        }
    }

    private void setAlarm(int hours, int minutes) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hours);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Wear Synced Alarm");
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        startActivity(intent);
    }

    public void syncAlarm(int hours, int minutes) {
        new StartWearableActivityTask().execute(hours, minutes);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mResolvingError = false;
        mSetAlarmButton.setVisibility(View.VISIBLE);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mSetAlarmButton.setVisibility(View.GONE);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DataEvent event : events) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        // TODO: mach was
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {
                        // TODO: mach was
                    }
                }
            }
        });
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

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
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = false;
            mSetAlarmButton.setVisibility(View.GONE);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    private class StartWearableActivityTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node, args[0], args[1]);
            }
            return null;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node, int hours, int minutes) {
        AlarmClockDataGenerator alarmClockDataGenerator = new AlarmClockDataGenerator(hours, minutes);
        alarmClockDataGenerator.run();

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
                            }
                        }
                    });
        }
    }
}
