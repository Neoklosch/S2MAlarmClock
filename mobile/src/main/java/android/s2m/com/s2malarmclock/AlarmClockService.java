package android.s2m.com.s2malarmclock;

import android.content.Intent;
import android.provider.AlarmClock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlarmClockService extends WearableListenerService {
    private static final String TAG = "AlarmClockService";

    private static final String ALARM_PATH = "/alarm-phone";
    private static final String HOURS_KEY = "hours";
    private static final String MINUTES_KEY = "minutes";
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        if(!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "DataLayerListenerService failed to connect to GoogleApiClient.");
                return;
            }
        }

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : events) {
            String path = event.getDataItem().getUri().getPath();
            if (ALARM_PATH.equals(path)) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    int hours = dataMap.getInt(HOURS_KEY);
                    int minutes = dataMap.getInt(MINUTES_KEY);
                    Intent setAlarmClockIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                    setAlarmClockIntent.putExtra(AlarmClock.EXTRA_HOUR, hours);
                    setAlarmClockIntent.putExtra(AlarmClock.EXTRA_MINUTES, minutes);
                    setAlarmClockIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "Phone Alarm from service");
                    setAlarmClockIntent.putExtra(AlarmClock.EXTRA_VIBRATE, true);
                    setAlarmClockIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                    setAlarmClockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(setAlarmClockIntent);
                }
            }
        }
    }
}
