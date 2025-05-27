package muskanclasses.class12th_science.model_paper;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RealTimeChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView onlineUsersText;
    private RecyclerView recyclerView;
    private EditText input;
    private ImageButton sendBtn;

    private DatabaseReference messagesRef;
    private DatabaseReference onlineUsersRef;
    private FirebaseUser currentUser;

    private List<Message> messagesList = new ArrayList<>();
    private MessageAdapter adapter;

    private ChildEventListener messagesListener;
    private ChildEventListener onlineUsersListener;

    private ProgressDialog progressDialog; // ✅ Progress dialog field

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

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        // Firebase database references
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("groupChats").child("group1").child("messages");
        onlineUsersRef = database.getReference("onlineUsers");

        setupRecyclerView();
        goOnline();

        // ✅ Show loading dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading messages...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        listenForMessages();
        listenOnlineUsers();

        sendBtn.setOnClickListener(v -> {
            String msgText = input.getText().toString().trim();
            if (!msgText.isEmpty()) {
                sendMessage(msgText);
                input.setText("");
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messagesList, currentUser.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void sendMessage(String messageText) {
        String messageId = messagesRef.push().getKey(); // unique key
        if (messageId == null) return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("senderName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonymous");
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());

        messagesRef.child(messageId)
                .setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Message sent
                })
                .addOnFailureListener(e -> Log.e("SendMessage", "Failed to send message", e));
    }

    private void listenForMessages() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists()) {
                    String senderId = snapshot.child("senderId").getValue(String.class);
                    String senderName = snapshot.child("senderName").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);
                    Long timestampMillis = snapshot.child("timestamp").getValue(Long.class);

                    if (senderId != null && message != null && timestampMillis != null) {
                        messagesList.add(new Message(senderId, senderName, message, timestampMillis));
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messagesList.size() - 1);
                    }

                    // ✅ Dismiss progress dialog after first message load
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("MessagesListener", "Listen failed.", error.toException());
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss(); // ✅ Dismiss on failure
                }
            }
        };

        messagesRef.orderByChild("timestamp").addChildEventListener(messagesListener);
    }

    private void goOnline() {
        Map<String, Object> userStatus = new HashMap<>();
        userStatus.put("timestamp", System.currentTimeMillis());

        onlineUsersRef.child(currentUser.getUid())
                .setValue(userStatus)
                .addOnSuccessListener(aVoid -> Log.d("OnlineStatus", "User is online"))
                .addOnFailureListener(e -> Log.w("OnlineStatus", "Error going online", e));
    }

    private void goOffline() {
        onlineUsersRef.child(currentUser.getUid())
                .removeValue()
                .addOnSuccessListener(aVoid -> Log.d("OnlineStatus", "User is offline"))
                .addOnFailureListener(e -> Log.w("OnlineStatus", "Error going offline", e));
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

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        goOffline();

        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (onlineUsersListener != null) {
            onlineUsersRef.removeEventListener(onlineUsersListener);
        }
    }

    // Message model
    public static class Message {
        private String senderId;
        private String senderName;
        private String message;
        private long timestamp;

        public Message(String senderId, String senderName, String message, long timestamp) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getSenderId() {
            return senderId;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    // Adapter for messages
    public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        private List<Message> messages;
        private String currentUserId;

        public MessageAdapter(List<Message> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @Override
        public int getItemViewType(int position) {
            Message message = messages.get(position);
            return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_SENT) {
                View view = inflater.inflate(R.layout.item_message_sent, parent, false);
                return new SentViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_message_received, parent, false);
                return new ReceivedViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = messages.get(position);

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(message.getTimestamp());

            if (holder instanceof SentViewHolder) {
                SentViewHolder h = (SentViewHolder) holder;
                h.textMessage.setText(message.getMessage());
                h.senderName.setText(message.getSenderName() != null ? message.getSenderName() : "You");
                h.textTime.setText(formattedTime);
            } else {
                ReceivedViewHolder h = (ReceivedViewHolder) holder;
                h.textMessage.setText(message.getMessage());
                h.senderName.setText(message.getSenderName());
                h.textTime.setText(formattedTime);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class SentViewHolder extends RecyclerView.ViewHolder {
            TextView senderName, textMessage, textTime;
            SentViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.senderName);
                textMessage = itemView.findViewById(R.id.textMessage);
                textTime = itemView.findViewById(R.id.textTime);
            }
        }

        class ReceivedViewHolder extends RecyclerView.ViewHolder {
            TextView senderName, textMessage, textTime;
            ReceivedViewHolder(@NonNull View itemView) {
                super(itemView);
                senderName = itemView.findViewById(R.id.senderName);
                textMessage = itemView.findViewById(R.id.textMessage);
                textTime = itemView.findViewById(R.id.textTime);
            }
        }
    }
}
