package muskanclasses.class12th_science.model_paper;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.*;

public class SplashActivity extends AppCompatActivity {

    Button btnOpenApp, btnUpdateApp;
    ProgressBar progressBar;

    // 🔁 अपने app का real versionCode डालें (या BuildConfig.VERSION_CODE से लें)
    int currentVersionCode = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.app_color));

        setContentView(R.layout.activity_splash);







        RelativeLayout mainLayout = findViewById(R.id.splash_layout); // R.id.main को XML में set करें

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, v.getPaddingTop(), systemBars.right, systemBars.bottom);
            return insets;
        });









        btnOpenApp = findViewById(R.id.btn_open_app);
        btnUpdateApp = findViewById(R.id.btn_update_app);

        btnOpenApp.setVisibility(View.GONE);
        btnUpdateApp.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progress_loader);

        // ✅ Show Progress Dialog


        String packageName = getPackageName();
        String safeKey = packageName.replace(".", "_");

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("app_versions")
                .child(safeKey)
                .child("latest_version_code");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {


                progressBar.setVisibility(View.GONE);



                if (snapshot.exists()) {
                    long latestVersion = snapshot.getValue(Long.class);
                    if (latestVersion <= currentVersionCode) {
                        btnOpenApp.setVisibility(View.VISIBLE);
                        Log.d("suman", String.valueOf(latestVersion));
                    } else {
                        btnUpdateApp.setVisibility(View.VISIBLE);
                        Log.d("suman", String.valueOf(latestVersion));
                    }
                } else {
                    btnOpenApp.setVisibility(View.VISIBLE); // default
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnOpenApp.setVisibility(View.VISIBLE);

            }
        });

        btnOpenApp.setOnClickListener(v -> {


            SharedPreferences sharedPreferences = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
            String name = sharedPreferences.getString("name", "null");
            String email = sharedPreferences.getString("email", "null");
            if (sharedPreferences.getString("email", "null").equals("null")){

                Intent intent = new Intent(getApplicationContext(), LoginnActivity.class);
                startActivity(intent);
                finish();

            } else {

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();

            }



        });

        btnUpdateApp.setOnClickListener(v -> {
            final String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + appPackageName)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        });
    }
}