package muskanclasses.class12th_science.model_paper;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdsManager {

    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";


    private static InterstitialAd mInterstitialAd;
    private static RewardedAd mRewardedAd;

    private static final String TAG = "AdsManager";





    public static void load_int_ads(Activity activity) {
        if (mInterstitialAd == null) {
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    Toast.makeText(activity, "load", Toast.LENGTH_SHORT).show();
                    Log.d("suman", "Ad loaded successfully");
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Toast.makeText(activity, "faild", Toast.LENGTH_SHORT).show();
                    Log.e("suman", "Ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
        }
    }

}
