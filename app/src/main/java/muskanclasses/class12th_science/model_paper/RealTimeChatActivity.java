package muskanclasses.class12th_science.model_paper;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RealTimeChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView onlineUsersText;
    private RecyclerView recyclerView;
    private EditText input;
    private ImageButton sendBtn, imagePickerBtn;
    private ProgressBar loadMoreProgressBar; // Declare ProgressBar for pagination

    private DatabaseReference messagesRef;
    private DatabaseReference onlineUsersRef;
    private FirebaseUser currentUser;

    private List<Message> messagesList = new ArrayList<>();
    private MessageAdapter adapter;

    private ChildEventListener messagesListener;
    private ChildEventListener onlineUsersListener;

    private ProgressDialog progressDialog; // For initial load

    private static final int PICK_IMAGE_REQUEST = 1;
    public static boolean isInForeground = false;

    // --- Pagination related variables ---
    private static final int MESSAGES_PER_LOAD = 15;
    private long lastTimestamp = -1;
    private boolean isLoading = false;
    private boolean allMessagesLoaded = false;

    // --- Delay related variables ---
    private Handler handler = new Handler();
    private Runnable loadMoreRunnable;
    FirebaseAnalytics mFirebaseAnalytics;

    // Custom Message class (no change needed here)
    public static class Message {
        private String senderId;
        private String senderName;
        private String message;
        private String imageUrl;
        private long timestamp;
        private String status;
        private Uri localImageUri;
        private String tempId;
        private String firebaseKey;

        public Message() {}

        public Message(String senderId, String senderName, String message, long timestamp) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.timestamp = timestamp;
            this.status = "sent";
            this.imageUrl = null;
            this.localImageUri = null;
            this.tempId = null;
            this.firebaseKey = null;
        }

        public Message(String senderId, String senderName, String message, Uri localImageUri, long timestamp, String status, String tempId) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.localImageUri = localImageUri;
            this.timestamp = timestamp;
            this.status = status;
            this.imageUrl = null;
            this.tempId = tempId;
            this.firebaseKey = null;
        }

        public String getSenderId() { return senderId; }
        public String getSenderName() { return senderName; }
        public String getMessage() { return message; }
        public String getImageUrl() { return imageUrl; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public Uri getLocalImageUri() { return localImageUri; }
        public String getTempId() { return tempId; }
        public String getFirebaseKey() { return firebaseKey; }

        public void setStatus(String status) { this.status = status; }
        public void setMessage(String message) { this.message = message; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public void setLocalImageUri(Uri localImageUri) { this.localImageUri = localImageUri; }
        public void setTempId(String tempId) { this.tempId = tempId; }
        public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.app_color));

        setContentView(R.layout.activity_real_time_chat);

        toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ConstraintLayout mainLayout = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return insets;
        });
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(view -> finish());

        onlineUsersText = findViewById(R.id.onlineUsersText);
        recyclerView = findViewById(R.id.recyclerView);
        input = findViewById(R.id.input);
        sendBtn = findViewById(R.id.sendBtn);
        imagePickerBtn = findViewById(R.id.imagePickerBtn);
        loadMoreProgressBar = findViewById(R.id.loadMoreProgressBar); // Initialize ProgressBar

        Bundle bundle = new Bundle();
        bundle.putString("package_name", getPackageName());
        mFirebaseAnalytics.logEvent("open_chat", bundle);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("groupChats").child("group1").child("messages");
        onlineUsersRef = database.getReference("onlineUsers");

        setupRecyclerView();

        // ProgressDialog for the initial load
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading messages...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        loadInitialMessages(); // This will dismiss the progressDialog when done
        listenOnlineUsers();

        sendBtn.setOnClickListener(v -> {
            String msgText = input.getText().toString().trim();
            if (!msgText.isEmpty()) {
                sendMessage(msgText);
                input.setText("");
            }
        });
        imagePickerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            String messageText = input.getText().toString().trim();
            input.setText("");

            if (currentUser == null || currentUser.getUid() == null) {
                Toast.makeText(this, "User ID not available for image upload.", Toast.LENGTH_SHORT).show();
                return;
            }

            String tempId = UUID.randomUUID().toString();
            RealTimeChatActivity.Message pendingImageMessage = new RealTimeChatActivity.Message(
                    currentUser.getUid(),
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous",
                    messageText,
                    imageUri,
                    System.currentTimeMillis(),
                    "pending_upload",
                    tempId
            );
            messagesList.add(pendingImageMessage);
            adapter.notifyItemInserted(messagesList.size() - 1);
            recyclerView.scrollToPosition(messagesList.size() - 1);

            uploadImageToBunny(imageUri, pendingImageMessage, messageText, tempId);
        }
    }

    private void setupRecyclerView() {
        String uid = (currentUser != null && currentUser.getUid() != null) ?
                currentUser.getUid() : "unknown_user";

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messagesList, uid, this);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int firstVisibleItemPosition = lm.findFirstVisibleItemPosition();

                if (dy < 0 && firstVisibleItemPosition <= 5) {
                    Log.d("ChatPagination", "User scrolled near top. Attempting delayed load.");
                    startDelayedLoadMoreMessages();
                }
            }
        });
    }

    private void loadInitialMessages() {
        isLoading = true;
        allMessagesLoaded = false;

        messagesRef.orderByChild("timestamp").limitToLast(MESSAGES_PER_LOAD)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Message> initialMessages = new ArrayList<>();
                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            Message msg = snapshot.getValue(Message.class);
                            if (isValidMessage(msg, snapshot.getKey())) {
                                msg.setLocalImageUri(null);
                                msg.setTempId(null);
                                msg.setFirebaseKey(snapshot.getKey());
                                initialMessages.add(msg);
                            }
                        }

                        Collections.sort(initialMessages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                        messagesList.clear();
                        messagesList.addAll(initialMessages);
                        adapter.notifyDataSetChanged();

                        if (!messagesList.isEmpty()) {
                            recyclerView.scrollToPosition(messagesList.size() - 1);
                            lastTimestamp = messagesList.get(0).getTimestamp();
                            Log.d("ChatPagination", "Initial messages loaded. Count: " + initialMessages.size() + ", Oldest timestamp: " + lastTimestamp);
                        } else {
                            Log.d("ChatPagination", "No initial messages found.");
                        }

                        if (initialMessages.size() < MESSAGES_PER_LOAD) {
                            allMessagesLoaded = true;
                            Log.d("ChatPagination", "All available history loaded during initial fetch.");
                        }

                    } else {
                        Log.e("ChatPagination", "Failed to load initial messages", task.getException());
                        Toast.makeText(RealTimeChatActivity.this, "Failed to load initial messages.", Toast.LENGTH_SHORT).show();
                    }

                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss(); // Dismiss initial load dialog
                        }
                        isLoading = false;
                        listenForMessages();
                    });
                });
    }

    private void startDelayedLoadMoreMessages() {
        if (isLoading || allMessagesLoaded) {
            Log.d("ChatPagination", "Already loading or all messages loaded. Aborting new delayed load.");
            return;
        }

        isLoading = true;
        Toast.makeText(this, "Loading more messages in 1 second...", Toast.LENGTH_SHORT).show();
        Log.d("ChatPagination", "Scheduling next message load with 1s delay.");

        // Show the ProgressBar when scheduling a new load
        runOnUiThread(() -> loadMoreProgressBar.setVisibility(View.VISIBLE));


        if (loadMoreRunnable != null) {
            handler.removeCallbacks(loadMoreRunnable);
            Log.d("ChatPagination", "Cancelled previous delayed load runnable.");
        }

        loadMoreRunnable = () -> {
            Log.d("ChatPagination", "Executing delayed message load after 1 second.");
            performLoadMoreMessages();
        };

        handler.postDelayed(loadMoreRunnable, 1000); // 1 second delay
    }

    private void performLoadMoreMessages() {
        Log.d("ChatPagination", "Attempting to load " + MESSAGES_PER_LOAD + " messages older than timestamp: " + lastTimestamp);

        messagesRef.orderByChild("timestamp")
                .endBefore(lastTimestamp)
                .limitToLast(MESSAGES_PER_LOAD)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Message> olderMessages = new ArrayList<>();
                        for (DataSnapshot snapshot : task.getResult().getChildren()) {
                            Message msg = snapshot.getValue(Message.class);
                            if (isValidMessage(msg, snapshot.getKey())) {
                                msg.setLocalImageUri(null);
                                msg.setTempId(null);
                                msg.setFirebaseKey(snapshot.getKey());
                                olderMessages.add(msg);
                            }
                        }

                        Collections.sort(olderMessages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                        if (!olderMessages.isEmpty()) {
                            messagesList.addAll(0, olderMessages);
                            adapter.notifyItemRangeInserted(0, olderMessages.size());
                            // Keep scroll position stable
                            recyclerView.scrollToPosition(olderMessages.size());

                            lastTimestamp = olderMessages.get(0).getTimestamp();
                            Log.d("ChatPagination", "Loaded " + olderMessages.size() + " older messages. New oldest timestamp: " + lastTimestamp);
                        } else {
                            allMessagesLoaded = true;
                            Toast.makeText(this, "No more old messages.", Toast.LENGTH_SHORT).show();
                            Log.d("ChatPagination", "No more old messages found after query.");
                        }

                        if (olderMessages.size() < MESSAGES_PER_LOAD) {
                            allMessagesLoaded = true;
                            Toast.makeText(this, "All available history loaded.", Toast.LENGTH_SHORT).show();
                            Log.d("ChatPagination", "Less than " + MESSAGES_PER_LOAD + " messages loaded, assuming all history is loaded.");
                        }

                    } else {
                        Log.e("ChatPagination", "Failed to load more messages", task.getException());
                        Toast.makeText(RealTimeChatActivity.this, "Failed to load more messages.", Toast.LENGTH_SHORT).show();
                    }

                    // --- IMPORTANT: Hide the ProgressBar here ---
                    runOnUiThread(() -> {
                        loadMoreProgressBar.setVisibility(View.GONE); // Hide ProgressBar when load is complete
                        isLoading = false;
                    });
                });
    }

    // Helper method to validate messages (no change needed here)
    private boolean isValidMessage(Message msg, String key) {
        if (msg == null || TextUtils.isEmpty(msg.getSenderId()) || (msg.getMessage() == null && msg.getImageUrl() == null) || msg.getTimestamp() == 0) {
            Log.w("ChatValidation", "Skipping invalid message from Firebase: " + (key != null ? key : "unknown key"));
            return false;
        }
        return true;
    }


    private void listenForMessages() {
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
            Log.d("RealTimeListener", "Removed old messagesListener.");
        }

        Query newMessagesQuery;
        if (!messagesList.isEmpty()) {
            long latestTimestampInList = messagesList.get(messagesList.size() - 1).getTimestamp();
            newMessagesQuery = messagesRef.orderByChild("timestamp").startAt(latestTimestampInList + 1);
            Log.d("RealTimeListener", "Listening for new messages after timestamp: " + (latestTimestampInList + 1));
        } else {
            newMessagesQuery = messagesRef.orderByChild("timestamp");
            Log.d("RealTimeListener", "Listening for all new messages (list is empty).");
        }


        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    Message newMessage = snapshot.getValue(Message.class);
                    if (!isValidMessage(newMessage, snapshot.getKey())) {
                        return;
                    }
                    newMessage.setFirebaseKey(snapshot.getKey());
                    boolean messageHandled = false;

                    if (currentUser != null && !TextUtils.isEmpty(currentUser.getUid()) && newMessage.getSenderId().equals(currentUser.getUid())) {
                        String firebaseTempId = (String) snapshot.child("tempId").getValue();
                        if (!TextUtils.isEmpty(firebaseTempId)) {
                            for (int i = 0; i < messagesList.size(); i++) {
                                Message existingMessage = messagesList.get(i);
                                if (existingMessage != null && "pending_upload".equals(existingMessage.getStatus()) &&
                                        existingMessage.getTempId() != null && existingMessage.getTempId().equals(firebaseTempId)) {
                                    existingMessage.setStatus("sent");
                                    existingMessage.setImageUrl(newMessage.getImageUrl());
                                    existingMessage.setMessage(newMessage.getMessage());
                                    existingMessage.setLocalImageUri(null);
                                    existingMessage.setTempId(null);
                                    existingMessage.setFirebaseKey(newMessage.getFirebaseKey());
                                    adapter.notifyItemChanged(i);
                                    messageHandled = true;
                                    Log.d("RealTimeListener", "Updated pending image message for tempId: " + firebaseTempId);
                                    break;
                                }
                            }
                        }
                    }

                    if (!messageHandled) {
                        boolean alreadyExistsInList = false;
                        for (Message msg : messagesList) {
                            if (msg != null && msg.getFirebaseKey() != null && msg.getFirebaseKey().equals(newMessage.getFirebaseKey())) {
                                alreadyExistsInList = true;
                                break;
                            }
                            if (msg != null && msg.getTimestamp() == newMessage.getTimestamp() &&
                                    msg.getSenderId().equals(newMessage.getSenderId()) &&
                                    ((msg.getMessage() != null && msg.getMessage().equals(newMessage.getMessage())) ||
                                            (msg.getImageUrl() != null && msg.getImageUrl().equals(newMessage.getImageUrl())))) {
                                alreadyExistsInList = true;
                                break;
                            }
                        }

                        if (!alreadyExistsInList) {
                            newMessage.setLocalImageUri(null);
                            newMessage.setTempId(null);
                            messagesList.add(newMessage);
                            adapter.notifyItemInserted(messagesList.size() - 1);
                            recyclerView.scrollToPosition(messagesList.size() - 1);
                            Log.d("RealTimeListener", "New real-time message added: " + newMessage.getMessage());
                        } else {
                            Log.d("RealTimeListener", "Skipping duplicate message from onChildAdded. Key: " + snapshot.getKey());
                        }
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message changedMessage = snapshot.getValue(Message.class);
                if (!isValidMessage(changedMessage, snapshot.getKey())) {
                    return;
                }
                changedMessage.setFirebaseKey(snapshot.getKey());

                for (int i = 0; i < messagesList.size(); i++) {
                    Message existingMessage = messagesList.get(i);
                    if (existingMessage != null && existingMessage.getFirebaseKey() != null && existingMessage.getFirebaseKey().equals(changedMessage.getFirebaseKey())) {
                        changedMessage.setLocalImageUri(null);
                        changedMessage.setTempId(null);
                        messagesList.set(i, changedMessage);
                        adapter.notifyItemChanged(i);
                        Log.d("RealTimeListener", "Message changed: " + changedMessage.getMessage());
                        break;
                    }
                }
            }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String removedKey = snapshot.getKey();
                int indexToRemove = -1;
                for (int i = 0; i < messagesList.size(); i++) {
                    Message existingMessage = messagesList.get(i);
                    if (existingMessage != null && existingMessage.getFirebaseKey() != null && existingMessage.getFirebaseKey().equals(removedKey)) {
                        indexToRemove = i;
                        break;
                    }
                    if (existingMessage != null && existingMessage.getTimestamp() == snapshot.child("timestamp").getValue(Long.class) &&
                            existingMessage.getSenderId().equals(snapshot.child("senderId").getValue(String.class))) {
                        indexToRemove = i;
                        break;
                    }
                }

                if (indexToRemove != -1) {
                    messagesList.remove(indexToRemove);
                    adapter.notifyItemRemoved(indexToRemove);
                    Log.d("RealTimeListener", "Message removed. Key: " + removedKey);
                } else {
                    Log.w("RealTimeListener", "Attempted to remove message not found in list. Key: " + removedKey);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealTimeListener", "Message listener cancelled: " + error.getMessage());
                Toast.makeText(RealTimeChatActivity.this, "Chat connection lost: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
        newMessagesQuery.addChildEventListener(messagesListener);
    }


    private void listenOnlineUsers() {
        onlineUsersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                updateOnlineUsersCount(snapshot.getRef().getParent());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                updateOnlineUsersCount(snapshot.getRef().getParent());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                updateOnlineUsersCount(snapshot.getRef().getParent());
            }

            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealTimeChat", "Online users listener cancelled: " + error.getMessage());
            }
        };
        onlineUsersRef.addChildEventListener(onlineUsersListener);
    }

    private void updateOnlineUsersCount(DatabaseReference onlineUsersRef) {
        onlineUsersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                long count = task.getResult().getChildrenCount();
                onlineUsersText.setText("Online Users: " + count);
            } else {
                Log.e("RealTimeChat", "Failed to update online users count", task.getException());
            }
        });
    }

    private void sendMessage(String messageText) {
        if (currentUser == null || currentUser.getUid() == null) {
            Toast.makeText(this, "User not logged in. Cannot send message.", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Log.e("SendMessage", "Failed to get Firebase push key for message.");
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous");
        message.put("message", messageText);
        message.put("imageUrl", null);
        message.put("timestamp", System.currentTimeMillis());

        messagesRef.child(messageId)
                .setValue(message)
                .addOnFailureListener(e -> Log.e("SendMessage", "Failed to send message to Firebase", e));
    }


    private void sendMessageToFirebase(String imageUrl, String captionText, String tempId) {
        if (currentUser == null || currentUser.getUid() == null) {
            Toast.makeText(this, "User not logged in. Cannot send image message.", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) {
            Log.e("SendMessage", "Failed to get Firebase push key for image message.");
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous");
        message.put("message", captionText);
        message.put("imageUrl", imageUrl);
        message.put("timestamp", System.currentTimeMillis());
        message.put("tempId", tempId);
        messagesRef.child(messageId)
                .setValue(message)
                .addOnFailureListener(e -> Log.e("SendMessage", "Failed to send image message to Firebase", e));
    }


    @Override
    protected void onStart() {
        super.onStart();
        isInForeground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isInForeground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
            Log.d("Lifecycle", "messagesListener removed.");
        }
        if (onlineUsersListener != null) {
            onlineUsersRef.removeEventListener(onlineUsersListener);
            Log.d("Lifecycle", "onlineUsersListener removed.");
        }
        if (loadMoreRunnable != null) {
            handler.removeCallbacks(loadMoreRunnable);
            Log.d("Lifecycle", "Pending loadMoreRunnable removed.");
        }
    }

    public void onMessageLongClicked(Message message) {
        if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
            final CharSequence[] options;
            if (TextUtils.isEmpty(message.getMessage())) {
                options = new CharSequence[]{"Delete Message"};
            } else {
                options = new CharSequence[]{"Copy Text", "Delete Message"};
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Message Options");
            builder.setItems(options, (dialog, item) -> {
                if (options[item].equals("Copy Text")) {
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("message", message.getMessage());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No text to copy.", Toast.LENGTH_SHORT).show();
                    }
                } else if (options[item].equals("Delete Message")) {
                    showDeleteConfirmationDialog(message);
                }
            });
            builder.show();
        } else {
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("message", message.getMessage());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "You can only delete your own messages.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteConfirmationDialog(Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(Message message) {
        if (message.getFirebaseKey() == null) {
            Toast.makeText(this, "Cannot delete message: Missing Firebase key.", Toast.LENGTH_SHORT).show();
            Log.e("DeleteMessage", "Attempted to delete message with null Firebase key.");
            return;
        }

        messagesRef.child(message.getFirebaseKey()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show();
                    if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                        deleteImageFromBunny(message.getImageUrl());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("DeleteMessage", "Firebase message deletion failed: " + e.getMessage(), e);
                });
    }


    private void deleteImageFromBunny(String imageUrl) {
        String storageZone = "muskan-classes";
        String apiKey = "d4008284-1e76-4d3e-aee31a61ab7c-a87f-48e5";

        if (!imageUrl.startsWith("https://muskanclasses.b-cdn.net/")) {
            Log.w("DeleteBunny", "Image URL is not from expected CDN: " + imageUrl);
            return;
        }

        String pathToDelete = imageUrl.substring("https://muskanclasses.b-cdn.net/".length());
        String deleteUrl = "https://storage.bunnycdn.com/" + storageZone + "/" + pathToDelete;
        new Thread(() -> {
            try {
                URL url = new URL(deleteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("AccessKey", apiKey);
                conn.setDoOutput(false);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    Log.d("DeleteBunny", "Image deleted from BunnyCDN: " + pathToDelete);
                } else {
                    String errorMsg = "Failed to delete image from BunnyCDN: " + responseCode + ", Message: " + conn.getResponseMessage();
                    Log.e("DeleteBunny", errorMsg);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to delete image from CDN: " + responseCode, Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("DeleteBunny", "Error deleting image from BunnyCDN: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error deleting image from CDN: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    public void onImageClicked(String imageUrl) {
        showFullScreenImage(imageUrl);
    }

    private void showFullScreenImage(String imageUrl) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_full_screen_image);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        PhotoView fullScreenImageView = dialog.findViewById(R.id.fullScreenImageView);
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this).load(imageUrl).into(fullScreenImageView);
        } else {
            fullScreenImageView.setImageResource(R.drawable.dummy_gray_image);
            Toast.makeText(this, "Image not available.", Toast.LENGTH_SHORT).show();
        }

        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    private void uploadImageToBunny(Uri imageUri, Message pendingMessage, String captionText, String tempId) {
        new Thread(() -> {
            int messagePosition = messagesList.indexOf(pendingMessage);
            if (messagePosition == -1) {
                Log.e("BunnyUpload", "Pending message not found in list for update. Aborting.");
                runOnUiThread(() -> Toast.makeText(this,
                        "Image upload failed to update status locally.", Toast.LENGTH_SHORT).show());
                return;
            }

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    throw new Exception("Could not open input stream for image URI.");
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] fileBytes = buffer.toByteArray();
                inputStream.close();

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(fileBytes);
                String fileHash = bytesToHex(hashBytes);
                String extension = "";
                String mimeType = getContentResolver().getType(imageUri);
                if (mimeType != null && mimeType.contains("/")) {
                    extension = "." + mimeType.substring(mimeType.lastIndexOf("/") + 1);
                    if (extension.equalsIgnoreCase(".jpeg")) {
                        extension = ".jpg";
                    } else if (extension.equalsIgnoreCase(".webp")) {
                        extension = ".webp";
                    }
                }
                if (extension.isEmpty()) {
                    extension = ".jpg";
                }

                String uniqueFileName = fileHash + "_" + System.currentTimeMillis() + extension;
                String storageZone = "muskan-classes";
                String apiKey = "d4008284-1e76-4d3e-aee31a61ab7c-a87f-48e5";
                String uploadPath = "uploads/12th-science/";
                String uploadUrl = "https://storage.bunnycdn.com/" + storageZone + "/" + uploadPath + uniqueFileName;
                String publicUrl = "https://muskanclasses.b-cdn.net/" + uploadPath + uniqueFileName;

                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("AccessKey", apiKey);
                conn.setDoOutput(true);
                conn.getOutputStream().write(fileBytes);

                int responseCode = conn.getResponseCode();
                if (responseCode == 201 || responseCode == 200) {
                    runOnUiThread(() -> {
                        sendMessageToFirebase(publicUrl, captionText, tempId);
                        Toast.makeText(this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String errorMsg = "Upload failed: " + responseCode + ", Message: " + conn.getResponseMessage();
                    Log.e("BunnyUpload", errorMsg);
                    runOnUiThread(() -> {
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        pendingMessage.setStatus("failed");
                        pendingMessage.setMessage("Image upload failed (" + responseCode + ")");
                        pendingMessage.setImageUrl(null);
                        pendingMessage.setLocalImageUri(null);
                        pendingMessage.setTempId(null);
                        adapter.notifyItemChanged(messagePosition);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("BunnyUpload", "Image upload failed due to exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    pendingMessage.setStatus("failed");
                    pendingMessage.setMessage("Image upload failed: " + e.getClass().getSimpleName());
                    pendingMessage.setImageUrl(null);
                    pendingMessage.setLocalImageUri(null);
                    pendingMessage.setTempId(null);
                    adapter.notifyItemChanged(messagePosition);
                });
            }
        }).start();
    }


    public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_SENT_TEXT = 1;
        private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
        private static final int VIEW_TYPE_SENT_IMAGE = 3;
        private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
        private static final int VIEW_TYPE_PENDING_IMAGE = 5;

        private List<Message> messages;
        private String currentUserId;
        private RealTimeChatActivity activityContext;

        public MessageAdapter(List<Message> messages, String currentUserId, RealTimeChatActivity activityContext) {
            this.messages = messages;
            this.currentUserId = currentUserId;
            this.activityContext = activityContext;
        }

        @Override
        public int getItemViewType(int position) {
            Message message = messages.get(position);
            boolean isCurrentUser = message.getSenderId().equals(currentUserId);

            if ("pending_upload".equals(message.getStatus()) && message.getLocalImageUri() != null) {
                return VIEW_TYPE_PENDING_IMAGE;
            } else if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                return isCurrentUser ?
                        VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
            } else {
                return isCurrentUser ?
                        VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_SENT_TEXT:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                    return new SentMessageViewHolder(view);
                case VIEW_TYPE_RECEIVED_TEXT:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
                    return new ReceivedMessageViewHolder(view);
                case VIEW_TYPE_SENT_IMAGE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                    return new SentMessageViewHolder(view);
                case VIEW_TYPE_RECEIVED_IMAGE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
                    return new ReceivedMessageViewHolder(view);
                case VIEW_TYPE_PENDING_IMAGE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                    return new SentMessageViewHolder(view);
                default:
                    throw new IllegalArgumentException("Unknown view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = messages.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(message.getTimestamp()));

            if (holder instanceof SentMessageViewHolder) {
                SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
                sentHolder.senderName.setText("You");
                sentHolder.textTime.setText(formattedTime);

                if ("pending_upload".equals(message.getStatus())) {
                    sentHolder.imageUploadProgressBar.setVisibility(View.VISIBLE);
                    sentHolder.textMessage.setVisibility(View.VISIBLE);
                    sentHolder.imageView.setVisibility(View.VISIBLE);
                    sentHolder.textMessage.setText("Sending: " + message.getMessage());
                    sentHolder.textMessage.setTextColor(Color.GRAY);
                    sentHolder.textMessage.setMovementMethod(null);
                    sentHolder.imageView.setImageURI(message.getLocalImageUri());

                    sentHolder.textMessage.setOnLongClickListener(null);
                    sentHolder.imageView.setOnClickListener(null);
                    sentHolder.itemView.setOnLongClickListener(null);
                } else if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    sentHolder.imageUploadProgressBar.setVisibility(View.GONE);
                    sentHolder.textMessage.setVisibility(View.GONE);
                    sentHolder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(sentHolder.imageView.getContext())
                            .load(message.getImageUrl())
                            .placeholder(R.drawable.dummy_gray_image)
                            .error(R.drawable.dummy_gray_image)
                            .into(sentHolder.imageView);
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        sentHolder.textMessage.setVisibility(View.VISIBLE);
                        sentHolder.textMessage.setText(message.getMessage());
                        sentHolder.textMessage.setTextColor(Color.WHITE);
                        Linkify.addLinks(sentHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                        sentHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        sentHolder.textMessage.setVisibility(View.GONE);
                    }

                    sentHolder.imageView.setOnClickListener(v -> activityContext.onImageClicked(message.getImageUrl()));
                    sentHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });
                } else { // Sent text message
                    sentHolder.imageUploadProgressBar.setVisibility(View.GONE);
                    sentHolder.imageView.setVisibility(View.GONE);
                    sentHolder.textMessage.setVisibility(View.VISIBLE);
                    sentHolder.textMessage.setText(message.getMessage());
                    sentHolder.textMessage.setTextColor(Color.WHITE);
                    Linkify.addLinks(sentHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                    sentHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());

                    sentHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });
                }
            } else if (holder instanceof ReceivedMessageViewHolder) {
                ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
                receivedHolder.senderName.setText(message.getSenderName());
                receivedHolder.textTime.setText(formattedTime);

                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    receivedHolder.textMessage.setVisibility(View.GONE);
                    receivedHolder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(receivedHolder.imageView.getContext())
                            .load(message.getImageUrl())
                            .placeholder(R.drawable.dummy_gray_image)
                            .error(R.drawable.dummy_gray_image)
                            .into(receivedHolder.imageView);
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        receivedHolder.textMessage.setVisibility(View.VISIBLE);
                        receivedHolder.textMessage.setText(message.getMessage());
                        receivedHolder.textMessage.setTextColor(Color.BLACK);
                        Linkify.addLinks(receivedHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                        receivedHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        receivedHolder.textMessage.setVisibility(View.GONE);
                    }

                    receivedHolder.imageView.setOnClickListener(v -> activityContext.onImageClicked(message.getImageUrl()));
                    receivedHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });
                } else {
                    receivedHolder.imageView.setVisibility(View.GONE);
                    receivedHolder.textMessage.setVisibility(View.VISIBLE);
                    receivedHolder.textMessage.setText(message.getMessage());
                    receivedHolder.textMessage.setTextColor(Color.BLACK);
                    Linkify.addLinks(receivedHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                    receivedHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());

                    receivedHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }


        public class SentMessageViewHolder extends RecyclerView.ViewHolder {
            TextView senderName, textMessage, textTime;
            ImageView imageView;
            ProgressBar imageUploadProgressBar;

            public SentMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.senderName);
                textMessage = itemView.findViewById(R.id.textMessage);
                textTime = itemView.findViewById(R.id.textTime);
                imageView = itemView.findViewById(R.id.imageView);
                imageUploadProgressBar = itemView.findViewById(R.id.imageUploadProgressBar);
            }
        }


        public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
            TextView senderName, textMessage, textTime;
            ImageView imageView;

            public ReceivedMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.senderName);
                textMessage = itemView.findViewById(R.id.textMessage);
                textTime = itemView.findViewById(R.id.textTime);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}