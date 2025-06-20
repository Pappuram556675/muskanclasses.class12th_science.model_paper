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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    private DatabaseReference messagesRef;
    private DatabaseReference onlineUsersRef;
    private FirebaseUser currentUser;

    private List<Message> messagesList = new ArrayList<>();
    private MessageAdapter adapter;

    private ChildEventListener messagesListener;
    private ChildEventListener onlineUsersListener;

    private ProgressDialog progressDialog;
    private static final int PICK_IMAGE_REQUEST = 1;

    public static boolean isInForeground = false;

    // Custom Message class
    public static class Message {
        private String senderId;
        private String senderName;
        private String message; // Now primarily for text/caption
        private String imageUrl; // For the URL of the image
        private long timestamp;
        private String status; // e.g., "sent", "pending_upload", "failed"
        private Uri localImageUri; // For displaying locally before upload
        private String tempId; // For identifying pending messages locally
        private String firebaseKey; // To store the Firebase push key for deletion

        // Default constructor for Firebase (important for DataSnapshot.getValue(Message.class))
        public Message() {
            // Default constructor required for calls to DataSnapshot.getValue(Message.class)
        }

        // Constructor for regular text messages
        public Message(String senderId, String senderName, String message, long timestamp) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
            this.status = "sent";
            this.imageUrl = null; // No image for text-only message
            this.localImageUri = null;
            this.tempId = null; // No temp ID for sent messages
            this.firebaseKey = null; // Will be set after pushed to Firebase
        }

        // Constructor for pending image uploads (with potential caption)
        public Message(String senderId, String senderName, String message, Uri localImageUri, long timestamp, String status, String tempId) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message; // This is the caption for the pending image
            this.localImageUri = localImageUri;
            this.timestamp = timestamp;
            this.status = status;
            this.imageUrl = null; // Image URL is not yet available
            this.tempId = tempId; // Assign temp ID
            this.firebaseKey = null; // Will be set after pushed to Firebase
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


        // Setters to update message status/URL after upload
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
        setContentView(R.layout.activity_real_time_chat);

        toolbar = findViewById(R.id.toolbar);
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

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading messages...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        loadInitialMessages();
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
            String messageText = input.getText().toString().trim(); // Get the text from input
            input.setText(""); // Clear the input field immediately

            if (currentUser == null || currentUser.getUid() == null) {
                Toast.makeText(this, "User ID not available for image upload.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate a unique temporary ID for this pending message
            String tempId = UUID.randomUUID().toString();

            // Immediately add a pending message to the list with the caption
            RealTimeChatActivity.Message pendingImageMessage = new RealTimeChatActivity.Message(
                    currentUser.getUid(),
                    currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous",
                    messageText, // This is the caption
                    imageUri,
                    System.currentTimeMillis(),
                    "pending_upload",
                    tempId // Pass the temporary ID
            );
            messagesList.add(pendingImageMessage);
            adapter.notifyItemInserted(messagesList.size() - 1);
            recyclerView.scrollToPosition(messagesList.size() - 1);

            // Pass the image URI, the pending message object, AND the text caption and tempId
            uploadImageToBunny(imageUri, pendingImageMessage, messageText, tempId);
        }
    }

    private void setupRecyclerView() {
        // Ensure currentUserId is not null before passing to adapter
        String uid = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "unknown_user";
        // Pass RealTimeChatActivity.this to the adapter so it can call methods in this activity
        adapter = new MessageAdapter(messagesList, uid, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadInitialMessages() {
        messagesRef.orderByChild("timestamp").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                messagesList.clear();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    Message msg = snapshot.getValue(Message.class);
                    // Crucial: Only add message if it's considered valid
                    if (msg != null && !TextUtils.isEmpty(msg.getSenderId()) && (msg.getMessage() != null || msg.getImageUrl() != null) && msg.getTimestamp() != 0) {
                        // Ensure localImageUri and tempId are null for loaded messages from DB
                        msg.setLocalImageUri(null);
                        msg.setTempId(null);
                        // Set the Firebase key for deletion later
                        msg.setFirebaseKey(snapshot.getKey());
                        messagesList.add(msg);
                    } else {
                        Log.w("RealTimeChat", "Skipping invalid message from Firebase (loadInitialMessages): " + (snapshot.getKey() != null ? snapshot.getKey() : "unknown key"));
                    }
                }
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messagesList.size() - 1);
            } else {
                Log.e("RealTimeChat", "Failed to load initial messages", task.getException());
            }

            runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                listenForMessages();
            });
        });
    }

    private void listenForMessages() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    Message newMessage = snapshot.getValue(Message.class);

                    // Crucial: Validate incoming message before processing
                    if (newMessage == null || TextUtils.isEmpty(newMessage.getSenderId()) || (newMessage.getMessage() == null && newMessage.getImageUrl() == null) || newMessage.getTimestamp() == 0) {
                        Log.w("RealTimeChat", "Skipping invalid new message from Firebase (onChildAdded): " + (snapshot.getKey() != null ? snapshot.getKey() : "unknown key"));
                        return; // Skip this message if it's not valid
                    }
                    newMessage.setFirebaseKey(snapshot.getKey()); // Set the Firebase key for deletion

                    boolean messageHandled = false;

                    // If it's our own message AND it has a tempId (meaning it's an uploaded message coming back from Firebase)
                    if (currentUser != null && !TextUtils.isEmpty(currentUser.getUid()) && newMessage.getSenderId().equals(currentUser.getUid())) {
                        String firebaseTempId = (String) snapshot.child("tempId").getValue(); // Get tempId from Firebase
                        if (!TextUtils.isEmpty(firebaseTempId)) { // If Firebase message contains a tempId
                            for (int i = 0; i < messagesList.size(); i++) {
                                Message existingMessage = messagesList.get(i);
                                // Ensure existingMessage is valid for comparison
                                if (existingMessage != null && !TextUtils.isEmpty(existingMessage.getSenderId()) &&
                                        existingMessage.getStatus() != null && existingMessage.getStatus().equals("pending_upload") &&
                                        existingMessage.getLocalImageUri() != null &&
                                        existingMessage.getTempId() != null && existingMessage.getTempId().equals(firebaseTempId)) {

                                    // This is our pending message that Firebase just confirmed!
                                    existingMessage.setStatus("sent"); // Update status to sent
                                    existingMessage.setImageUrl(newMessage.getImageUrl()); // Set the actual public URL
                                    existingMessage.setMessage(newMessage.getMessage()); // Set the caption from Firebase
                                    existingMessage.setLocalImageUri(null); // Clear local URI
                                    existingMessage.setTempId(null); // Clear tempId
                                    existingMessage.setFirebaseKey(newMessage.getFirebaseKey()); // Set the actual Firebase key
                                    adapter.notifyItemChanged(i); // Notify adapter for this specific item
                                    messageHandled = true;
                                    Log.d("RealTimeChat", "Updated pending image message with URL and text (matched by tempId): " + newMessage.getImageUrl() + ", " + newMessage.getMessage());
                                    break;
                                }
                            }
                        }
                    }

                    // If the message was not our own pending message being updated, add it as a new one
                    if (!messageHandled) {
                        // Prevent adding duplicates if it was already loaded or is another type of duplicate
                        boolean alreadyExistsInList = false;
                        for (Message msg : messagesList) {
                            // Ensure 'msg' is valid for comparison
                            if (msg != null && !TextUtils.isEmpty(msg.getSenderId())) {
                                // Check for existing message based on sender, timestamp, AND content/image URL
                                // Using imageUrl and timestamp for uniqueness for already "sent" messages
                                if (msg.getSenderId().equals(newMessage.getSenderId()) && msg.getTimestamp() == newMessage.getTimestamp() &&
                                        ((msg.getMessage() != null && msg.getMessage().equals(newMessage.getMessage())) ||
                                                (msg.getImageUrl() != null && msg.getImageUrl().equals(newMessage.getImageUrl())))) {
                                    alreadyExistsInList = true;
                                    break;
                                }
                            }
                        }

                        if (!alreadyExistsInList) {
                            // Clear local props for truly new incoming messages
                            newMessage.setLocalImageUri(null);
                            newMessage.setTempId(null);
                            messagesList.add(newMessage);
                            adapter.notifyItemInserted(messagesList.size() - 1);
                            recyclerView.scrollToPosition(messagesList.size() - 1);

                            // Optional: Show a toast notification for new incoming messages if activity is not in foreground
                            // if (!isInForeground) {
                            //     String displayContent = (newMessage.getMessage() != null && !newMessage.getMessage().isEmpty()) ? newMessage.getMessage() : "Image";
                            //     Toast.makeText(getApplicationContext(), newMessage.getSenderName() + ": " + displayContent, Toast.LENGTH_SHORT).show();
                            // }
                        }
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                // If a message's content or status changes in Firebase after initial load
                Message changedMessage = snapshot.getValue(Message.class);
                // Validate changed message
                if (changedMessage != null && !TextUtils.isEmpty(changedMessage.getSenderId()) && changedMessage.getTimestamp() != 0) {
                    changedMessage.setFirebaseKey(snapshot.getKey()); // Set the Firebase key for deletion
                    for (int i = 0; i < messagesList.size(); i++) {
                        Message existingMessage = messagesList.get(i);
                        // Ensure existing message is valid for comparison
                        if (existingMessage != null && !TextUtils.isEmpty(existingMessage.getSenderId())) {
                            // Assuming timestamp + senderId is a good unique identifier
                            if (existingMessage.getTimestamp() == changedMessage.getTimestamp() &&
                                    existingMessage.getSenderId().equals(changedMessage.getSenderId())) {
                                // Ensure local props are cleared for the updated message from DB
                                changedMessage.setLocalImageUri(null);
                                changedMessage.setTempId(null);
                                messagesList.set(i, changedMessage); // Replace the old message with the updated one
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }
            }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // If a message is deleted from Firebase
                Message removedMessage = snapshot.getValue(Message.class);
                if (removedMessage == null) return; // Basic null check

                // Find the message in our list using Firebase key or timestamp+senderId
                int indexToRemove = -1;
                String removedKey = snapshot.getKey();
                for (int i = 0; i < messagesList.size(); i++) {
                    Message existingMessage = messagesList.get(i);
                    // Match by FirebaseKey first, as it's definitive
                    if (existingMessage != null && existingMessage.getFirebaseKey() != null && existingMessage.getFirebaseKey().equals(removedKey)) {
                        indexToRemove = i;
                        break;
                    }
                    // Fallback to timestamp+senderId if firebaseKey not set (e.g., older messages or during initial load)
                    if (existingMessage != null && existingMessage.getTimestamp() == removedMessage.getTimestamp() &&
                            existingMessage.getSenderId().equals(removedMessage.getSenderId()) &&
                            ((existingMessage.getMessage() != null && existingMessage.getMessage().equals(removedMessage.getMessage())) ||
                                    (existingMessage.getImageUrl() != null && existingMessage.getImageUrl().equals(removedMessage.getImageUrl())))) {
                        indexToRemove = i;
                        break;
                    }
                }

                if (indexToRemove != -1) {
                    messagesList.remove(indexToRemove);
                    adapter.notifyItemRemoved(indexToRemove);
                    Log.d("RealTimeChat", "Message removed from UI: " + removedKey);
                } else {
                    Log.d("RealTimeChat", "Message not found in list for removal: " + removedKey);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RealTimeChat", "Firebase listen cancelled: " + error.getMessage());
            }
        };

        messagesRef.orderByChild("timestamp").addChildEventListener(messagesListener);
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
                Log.w("OnlineUsers", "Listen failed.", error.toException());
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
                Log.w("OnlineUsers", "Failed to get online users count");
            }
        });
    }

    private void sendMessage(String messageText) {
        if (currentUser == null || currentUser.getUid() == null) {
            Toast.makeText(this, "User ID not available to send message.", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous");
        message.put("message", messageText); // This is a text-only message
        message.put("imageUrl", null); // No image URL for text-only message
        message.put("timestamp", System.currentTimeMillis());
        // Do NOT put tempId for text-only messages

        messagesRef.child(messageId)
                .setValue(message)
                .addOnFailureListener(e -> Log.e("SendMessage", "Failed to send message", e));
    }

    // This method sends the uploaded image's URL and caption to Firebase
    private void sendMessageToFirebase(String imageUrl, String captionText, String tempId) {
        if (currentUser == null || currentUser.getUid() == null) {
            Toast.makeText(this, "User ID not available to send image message.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a new Firebase key. This will be the actual key for the message in the DB.
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous");
        message.put("message", captionText); // The caption for the image
        message.put("imageUrl", imageUrl); // The public URL of the uploaded image
        message.put("timestamp", System.currentTimeMillis());
        message.put("tempId", tempId); // Include tempId for matching with pending local message

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
        }
        if (onlineUsersListener != null) {
            onlineUsersRef.removeEventListener(onlineUsersListener);
        }
    }

    // New method to handle message long clicks for copying text or deleting
    public void onMessageLongClicked(Message message) {
        // Only allow deletion for messages sent by the current user
        if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
            // Options: Copy, Delete
            final CharSequence[] options;
            if (TextUtils.isEmpty(message.getMessage())) {
                // If it's only an image, only offer delete
                options = new CharSequence[]{"Delete Message"}; // Only delete option
            } else {
                // If it has text, offer copy and delete
                options = new CharSequence[]{"Copy Text", "Delete Message"};
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Message Options");
            builder.setItems(options, (dialog, item) -> {
                // Check which option was selected based on its text
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
            // For messages not sent by current user, just offer copy (if text exists)
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("message", message.getMessage());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                // No text to copy and not your message to delete
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

    // **NEW METHOD: deleteMessage**
    private void deleteMessage(Message message) {
        if (message.getFirebaseKey() == null) {
            Toast.makeText(this, "Cannot delete message: Missing Firebase key.", Toast.LENGTH_SHORT).show();
            Log.e("DeleteMessage", "Attempted to delete message with null Firebase key.");
            return;
        }

        // First, attempt to delete from Firebase Database
        messagesRef.child(message.getFirebaseKey()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DeleteMessage", "Message deleted from Firebase: " + message.getFirebaseKey());
                    Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show();

                    // If it was an image message, attempt to delete from Bunny.net
                    if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                        deleteImageFromBunny(message.getImageUrl());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("DeleteMessage", "Failed to delete message from Firebase: " + message.getFirebaseKey(), e);
                });
    }

    // **NEW METHOD: deleteImageFromBunny**
    private void deleteImageFromBunny(String imageUrl) {
        // Extract the file path from the full public URL
        // Example: "https://muskanclasses.b-cdn.net/uploads/12th-science/image_hash.jpg"
        // Needs to become: "uploads/12th-science/image_hash.jpg"
        String storageZone = "muskan-classes"; // Your storage zone name
        String apiKey = "d4008284-1e76-4d3e-aee31a61ab7c-a87f-48e5"; // Your BunnyCDN API Key

        if (!imageUrl.startsWith("https://muskanclasses.b-cdn.net/")) {
            Log.w("DeleteBunny", "Image URL is not from expected CDN: " + imageUrl);
            return; // Or handle this case as an error
        }

        String pathToDelete = imageUrl.substring("https://muskanclasses.b-cdn.net/".length());
        String deleteUrl = "https://storage.bunnycdn.com/" + storageZone + "/" + pathToDelete;

        new Thread(() -> {
            try {
                URL url = new URL(deleteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("AccessKey", apiKey);
                conn.setDoOutput(false); // No body for DELETE request

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 204) { // 200 OK or 204 No Content
                    Log.d("DeleteBunny", "Image deleted from BunnyCDN: " + pathToDelete);
                    runOnUiThread(() -> Toast.makeText(this, "Image also deleted from CDN.", Toast.LENGTH_SHORT).show());
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


    // Added a method to handle image clicks for full screen view
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
            fullScreenImageView.setImageResource(R.drawable.dummy_gray_image); // More appropriate placeholder
            Toast.makeText(this, "Image not available.", Toast.LENGTH_SHORT).show();
        }

        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Utility method to convert byte array to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Now accepts the text caption as a parameter, and tempId
    private void uploadImageToBunny(Uri imageUri, Message pendingMessage, String captionText, String tempId) {
        new Thread(() -> {
            int messagePosition = messagesList.indexOf(pendingMessage);
            if (messagePosition == -1) {
                Log.e("BunnyUpload", "Pending message not found in list for update. Aborting.");
                runOnUiThread(() -> Toast.makeText(this, "Image upload failed to update status locally.", Toast.LENGTH_SHORT).show());
                return;
            }

            try {
                // Read file bytes into a ByteArrayOutputStream to calculate hash
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

                // Calculate SHA-256 hash of the image bytes
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(fileBytes);
                String fileHash = bytesToHex(hashBytes);

                // Get file extension from the original URI
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
                    extension = ".jpg"; // Default to jpg
                }

                // Create a unique file name using hash and timestamp
                String uniqueFileName = fileHash + "_" + System.currentTimeMillis() + extension;


                // 🔑 CONFIG (Replace with your actual BunnyCDN details)
                String storageZone = "muskan-classes";
                String apiKey = "d4008284-1e76-4d3e-aee31a61ab7c-a87f-48e5";
                String uploadPath = "uploads/12th-science/"; // Path within your storage zone
                String uploadUrl = "https://storage.bunnycdn.com/" + storageZone + "/" + uploadPath + uniqueFileName;
                String publicUrl = "https://muskanclasses.b-cdn.net/" + uploadPath + uniqueFileName; // Your public CDN URL


                // 🌐 Upload via HttpURLConnection
                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("AccessKey", apiKey);
                conn.setDoOutput(true);
                conn.getOutputStream().write(fileBytes);

                int responseCode = conn.getResponseCode();

                if (responseCode == 201 || responseCode == 200) {
                    runOnUiThread(() -> {
                        // The Firebase listener will handle updating the UI for this message.
                        // Send the message to Firebase with both the image URL, the caption, and the tempId.
                        sendMessageToFirebase(publicUrl, captionText, tempId);
                        Toast.makeText(this, "Image uploaded!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String errorMsg = "Upload failed: " + responseCode + ", Message: " + conn.getResponseMessage();
                    Log.e("BunnyUpload", errorMsg);
                    runOnUiThread(() -> {
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        // Update status to failed and notify adapter for the pending message
                        pendingMessage.setStatus("failed");
                        pendingMessage.setMessage("Image upload failed (" + responseCode + ")");
                        pendingMessage.setImageUrl(null); // Ensure no partial URL is kept
                        pendingMessage.setLocalImageUri(null); // Clear local URI as it's failed
                        pendingMessage.setTempId(null); // Clear tempId
                        adapter.notifyItemChanged(messagePosition);
                    });
                }
                conn.disconnect();

            } catch (Exception e) { // Catch general Exception for network or file issues
                Log.e("BunnyUpload", "Image upload failed due to exception: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Update status to failed and notify adapter for the pending message
                    pendingMessage.setStatus("failed");
                    pendingMessage.setMessage("Image upload failed: " + e.getClass().getSimpleName()); // Show simple error
                    pendingMessage.setImageUrl(null);
                    pendingMessage.setLocalImageUri(null);
                    pendingMessage.setTempId(null);
                    adapter.notifyItemChanged(messagePosition);
                });
            }
        }).start(); // Start the new thread
    }


    // --- MessageAdapter Class (Nested for brevity, but can be a separate file) ---

    public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_SENT_TEXT = 1;
        private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
        private static final int VIEW_TYPE_SENT_IMAGE = 3;
        private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;
        private static final int VIEW_TYPE_PENDING_IMAGE = 5;

        private List<Message> messages;
        private String currentUserId;
        private RealTimeChatActivity activityContext; // Reference to the activity for callbacks

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
                return isCurrentUser ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
            } else {
                return isCurrentUser ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
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
                    return new SentMessageViewHolder(view); // Using the same layout for sent image
                case VIEW_TYPE_RECEIVED_IMAGE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
                    return new ReceivedMessageViewHolder(view); // Using the same layout for received image
                case VIEW_TYPE_PENDING_IMAGE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                    return new SentMessageViewHolder(view); // Using sent layout with special handling
                default:
                    throw new IllegalArgumentException("Unknown view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = messages.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(message.getTimestamp()));

            // Apply Linkify to message content for both sent and received text messages
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                Linkify.addLinks(new TextView(holder.itemView.getContext()), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            }


            if (holder instanceof SentMessageViewHolder) {
                SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
                sentHolder.senderName.setText("You");
                sentHolder.textTime.setText(formattedTime);

                // Handle status for pending uploads
                if ("pending_upload".equals(message.getStatus())) {
                    sentHolder.imageUploadProgressBar.setVisibility(View.VISIBLE);
                    sentHolder.textMessage.setVisibility(View.VISIBLE); // Show text view for caption
                    sentHolder.imageView.setVisibility(View.VISIBLE); // Keep image view visible
                    sentHolder.textMessage.setText("Sending: " + message.getMessage()); // Show sending status with caption
                    sentHolder.textMessage.setTextColor(Color.GRAY); // Indicate pending
                    sentHolder.textMessage.setMovementMethod(null); // Disable links for pending
                    sentHolder.imageView.setImageURI(message.getLocalImageUri()); // Load from local URI

                    // No long click or click listener for pending messages
                    sentHolder.textMessage.setOnLongClickListener(null);
                    sentHolder.imageView.setOnClickListener(null);
                    sentHolder.itemView.setOnLongClickListener(null); // Disable long press for pending

                } else if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    // Sent Image Message
                    sentHolder.imageUploadProgressBar.setVisibility(View.GONE);
                    sentHolder.textMessage.setVisibility(View.GONE); // Hide text message area for images
                    sentHolder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(sentHolder.imageView.getContext())
                            .load(message.getImageUrl())
                            .placeholder(R.drawable.dummy_gray_image) // Placeholder while loading
                            .error(R.drawable.dummy_gray_image) // Error placeholder
                            .into(sentHolder.imageView);

                    // If there's a caption, show it below the image or in the same TextView
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        sentHolder.textMessage.setVisibility(View.VISIBLE);
                        sentHolder.textMessage.setText(message.getMessage());
                        sentHolder.textMessage.setTextColor(Color.WHITE); // Normal text color
                        Linkify.addLinks(sentHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                        sentHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance()); // Enable link clicking
                    } else {
                        sentHolder.textMessage.setVisibility(View.GONE);
                    }

                    // Click listener for full-screen image view
                    sentHolder.imageView.setOnClickListener(v -> activityContext.onImageClicked(message.getImageUrl()));

                    // Set long click listener for the message bubble itself
                    sentHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });

                } else {
                    // Sent Text Message
                    sentHolder.imageUploadProgressBar.setVisibility(View.GONE);
                    sentHolder.imageView.setVisibility(View.GONE);
                    sentHolder.textMessage.setVisibility(View.VISIBLE);
                    sentHolder.textMessage.setText(message.getMessage());
                    sentHolder.textMessage.setTextColor(Color.WHITE); // Normal text color
                    Linkify.addLinks(sentHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                    sentHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance()); // Enable link clicking

                    // Set long click listener for the message bubble itself
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
                    // Received Image Message
                    receivedHolder.textMessage.setVisibility(View.GONE); // Hide text message area for images
                    receivedHolder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(receivedHolder.imageView.getContext())
                            .load(message.getImageUrl())
                            .placeholder(R.drawable.dummy_gray_image) // Placeholder while loading
                            .error(R.drawable.dummy_gray_image) // Error placeholder
                            .into(receivedHolder.imageView);

                    // If there's a caption, show it below the image or in the same TextView
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        receivedHolder.textMessage.setVisibility(View.VISIBLE);
                        receivedHolder.textMessage.setText(message.getMessage());
                        receivedHolder.textMessage.setTextColor(Color.BLACK); // Normal text color
                        Linkify.addLinks(receivedHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                        receivedHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance()); // Enable link clicking
                    } else {
                        receivedHolder.textMessage.setVisibility(View.GONE);
                    }

                    // Click listener for full-screen image view
                    receivedHolder.imageView.setOnClickListener(v -> activityContext.onImageClicked(message.getImageUrl()));

                    // Set long click listener for the message bubble itself
                    receivedHolder.itemView.setOnLongClickListener(v -> {
                        activityContext.onMessageLongClicked(message);
                        return true;
                    });

                } else {
                    // Received Text Message
                    receivedHolder.imageView.setVisibility(View.GONE);
                    receivedHolder.textMessage.setVisibility(View.VISIBLE);
                    receivedHolder.textMessage.setText(message.getMessage());
                    receivedHolder.textMessage.setTextColor(Color.BLACK); // Normal text color
                    Linkify.addLinks(receivedHolder.textMessage, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                    receivedHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance()); // Enable link clicking

                    // Set long click listener for the message bubble itself
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

        // --- ViewHolder Classes for MessageAdapter ---

        // ViewHolder for Sent Messages (Text, Image, or Pending Image)
        public class SentMessageViewHolder extends RecyclerView.ViewHolder {
            TextView senderName, textMessage, textTime;
            ImageView imageView;
            ProgressBar imageUploadProgressBar; // Only visible for pending uploads

            public SentMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.senderName);
                textMessage = itemView.findViewById(R.id.textMessage);
                textTime = itemView.findViewById(R.id.textTime);
                imageView = itemView.findViewById(R.id.imageView);
                imageUploadProgressBar = itemView.findViewById(R.id.imageUploadProgressBar);
            }
        }

        // ViewHolder for Received Messages (Text or Image)
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