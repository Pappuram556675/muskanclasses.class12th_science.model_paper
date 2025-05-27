package muskanclasses.class12th_science.model_paper;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ads);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d("suman", "Ads_activity");

        MobileAds.initialize(this, initializationStatus -> {
            Log.d("suman", "Mobile Ads initialized");
        });
        //AdsManager.load_int_ads(MainActivity.this);
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(AdsActivity.this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                Toast.makeText(AdsActivity.this, "load", Toast.LENGTH_SHORT).show();
                Log.d("suman", "Ad loaded successfully");
                super.onAdLoaded(interstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Toast.makeText(AdsActivity.this, loadAdError.toString(), Toast.LENGTH_SHORT).show();
                Log.e("suman", "Ad failed to load: " + loadAdError.getMessage());
                super.onAdFailedToLoad(loadAdError);
            }
        });
    }
}