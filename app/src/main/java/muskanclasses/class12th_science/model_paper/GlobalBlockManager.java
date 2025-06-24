package muskanclasses.class12th_science.model_paper;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the global list of blocked users by listening to Firebase Realtime Database.
 */
public class GlobalBlockManager {

    private static final String TAG = "GlobalBlockManager";
    private DatabaseReference globalBlockedUsersRef;
    private Set<String> blockedUserIds;
    private ValueEventListener globalBlockedUsersListener;
    private OnBlockedUsersChangedListener listener;

    /**
     * Interface for listeners to be notified when the blocked users list changes.
     */
    public interface OnBlockedUsersChangedListener {
        void onBlockedUsersChanged(Set<String> newBlockedUserIds);
        void onBlockedUsersLoadFailed(String errorMessage);
        void onBlockedUsersLoadedInitial(); // To signal initial load completion
    }

    public GlobalBlockManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.globalBlockedUsersRef = database.getReference("globalBlockedUsers");
        this.blockedUserIds = new HashSet<>();
    }

    /**
     * Sets the listener for blocked user list changes.
     * @param listener The listener to set.
     */
    public void setOnBlockedUsersChangedListener(OnBlockedUsersChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Starts listening for changes in the global blocked users list.
     * Call this in the `onCreate` or `onResume` of the Activity/Fragment.
     */
    public void startListening() {
        if (globalBlockedUsersRef == null) {
            Log.e(TAG, "globalBlockedUsersRef is null, cannot start listening.");
            if (listener != null) {
                listener.onBlockedUsersLoadFailed("Database reference not initialized.");
            }
            return;
        }

        // Remove any existing listener to prevent duplicates if startListening is called multiple times
        if (globalBlockedUsersListener != null) {
            globalBlockedUsersRef.removeEventListener(globalBlockedUsersListener);
        }

        globalBlockedUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> newBlockedUsers = new HashSet<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    // Only add UIDs whose value is 'true'
                    if (childSnapshot.exists() && Boolean.TRUE.equals(childSnapshot.getValue(Boolean.class))) {
                        newBlockedUsers.add(childSnapshot.getKey());
                    }
                }

                // Check if the list has actually changed before updating and notifying
                if (!blockedUserIds.equals(newBlockedUsers)) {
                    blockedUserIds.clear();
                    blockedUserIds.addAll(newBlockedUsers);
                    Log.d(TAG, "Blocked users list updated. Count: " + blockedUserIds.size());
                    if (listener != null) {
                        listener.onBlockedUsersChanged(Collections.unmodifiableSet(blockedUserIds));
                    }
                } else {
                    Log.d(TAG, "Blocked users list unchanged. No notification sent.");
                }

                // Signal initial load completion if listener is set
                if (listener != null) {
                    listener.onBlockedUsersLoadedInitial();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load/listen for global blocked users: " + error.getMessage(), error.toException());
                if (listener != null) {
                    listener.onBlockedUsersLoadFailed(error.getMessage());
                }
            }
        };
        globalBlockedUsersRef.addValueEventListener(globalBlockedUsersListener);
        Log.d(TAG, "Started listening for global blocked users.");
    }

    /**
     * Stops listening for changes in the global blocked users list.
     * Call this in the `onDestroy` of the Activity/Fragment to prevent memory leaks.
     */
    public void stopListening() {
        if (globalBlockedUsersRef != null && globalBlockedUsersListener != null) {
            globalBlockedUsersRef.removeEventListener(globalBlockedUsersListener);
            globalBlockedUsersListener = null; // Clear the listener reference
            Log.d(TAG, "Stopped listening for global blocked users.");
        }
    }

    /**
     * Checks if a specific user UID is currently in the blocked list.
     * @param uid The UID of the user to check.
     * @return true if the user is blocked, false otherwise.
     */
    public boolean isUserBlocked(String uid) {

        return blockedUserIds.contains(uid);
    }

    /**
     * Returns an unmodifiable set of currently blocked user IDs.
     * @return A Set containing the UIDs of blocked users.
     */
    public Set<String> getBlockedUserIds() {

        return Collections.unmodifiableSet(blockedUserIds);
    }
}