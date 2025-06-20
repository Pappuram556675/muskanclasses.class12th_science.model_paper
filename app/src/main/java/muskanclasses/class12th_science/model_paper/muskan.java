package muskanclasses.class12th_science.model_paper;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;


public class muskan extends Application {
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    private static String currentActivityName = "";
    private DatabaseReference messageRef;
    private long lastShownTimestamp = 0;

    private static final String ONESIGNAL_APP_ID = "6803df43-7a70-4fe7-85bc-770700dfdf7e";

    @Override
    public void onCreate() {
        super.onCreate();



        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        // Initialize with your OneSignal App ID
        OneSignal.initWithContext(this, "6803df43-7a70-4fe7-85bc-770700dfdf7e");
        // Use this method to prompt for push notifications.
        // We recommend removing this method after testing and instead use In-App Messages to prompt for notification permission.
        OneSignal.getNotifications().requestPermission(false, Continue.none());





        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    goOnline();
                }
                currentActivityName = activity.getClass().getSimpleName();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    goOffline();
                }
            }

            public void onActivityCreated(Activity activity, Bundle bundle) {}
            public void onActivityResumed(Activity activity) {}
            public void onActivityPaused(Activity activity) {}
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
            public void onActivityDestroyed(Activity activity) {}
        });

        listenForMessages();
    }

    private void goOnline() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("onlineUsers")
                    .child(user.getUid())
                    .setValue(System.currentTimeMillis());
        }
    }

    private void goOffline() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("onlineUsers")
                    .child(user.getUid())
                    .removeValue();
        }
    }

    public static String getCurrentActivityName() {
        return currentActivityName;
    }

    private void listenForMessages() {
        messageRef = FirebaseDatabase.getInstance()
                .getReference("groupChats")
                .child("group1")
                .child("messages");

        messageRef.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String senderName = snapshot.child("senderName").getValue(String.class);
                String message = snapshot.child("message").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                // ✅ Toast only if not in RealTimeChatActivity and new message
                if (senderName != null && message != null && timestamp != null &&
                        !currentActivityName.equals("RealTimeChatActivity") &&
                        timestamp > lastShownTimestamp) {

                    lastShownTimestamp = timestamp;

                    new Handler(getMainLooper()).post(() -> {
                        // Inflate custom toast layout
                        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                        View layout = inflater.inflate(R.layout.custom_toast, null);

                        TextView text = layout.findViewById(R.id.toast_text);
                        text.setText(senderName + ": " + message);

                        Toast toast = new Toast(getApplicationContext());
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.setView(layout);
                        toast.show();
                    });
                }
            }

            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
