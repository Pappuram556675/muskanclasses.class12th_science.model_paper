package muskanclasses.class12th_science.model_paper;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

public class AdsManager {

    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5497780479271547/6281108324";
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-5497780479271547/7482525261";


    public static InterstitialAd mInterstitialAd;
    public static RewardedAd rewardedAd;

    private static final String TAG = "AdsManager";







    public static void load_int_ads(Activity activity) {

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

        // एक basic event log करें
        Bundle bundle = new Bundle();
        bundle.putString("package", activity.getPackageName());

        mFirebaseAnalytics.logEvent("int_ads_load", bundle);


        if (mInterstitialAd == null) {
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                    FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

                    // एक basic event log करें
                    Bundle bundle = new Bundle();
                    bundle.putString("package", activity.getPackageName());

                    mFirebaseAnalytics.logEvent("int_ads_load_successfully", bundle);

                    Log.d("suman", "Ad loaded successfully");
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                    FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

                    // एक basic event log करें
                    Bundle bundle = new Bundle();
                    bundle.putString("package", activity.getPackageName());

                    mFirebaseAnalytics.logEvent("int_ads_load_failed", bundle);
                    Log.e("suman", "Ad failed to load: " + loadAdError.getMessage());
                    mInterstitialAd = null;
                }
            });
        }
    }

    public static void show_int_ads_with_callback(Activity activity){

        if (mInterstitialAd!=null){

            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {

                    mInterstitialAd = null;
                    load_int_ads(activity);

                    super.onAdDismissedFullScreenContent();
                }


            });

            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

            // एक basic event log करें
            Bundle bundle = new Bundle();
            bundle.putString("package", activity.getPackageName());

            mFirebaseAnalytics.logEvent("int_ads_show", bundle);

            mInterstitialAd.show(activity);

        } else {

            load_int_ads(activity);
        }

    }


    public static void loadRewardedAd(Activity activity) {
        AdRequest adRequest = new AdRequest.Builder().build();

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

        // एक basic event log करें
        Bundle bundle = new Bundle();
        bundle.putString("package", activity.getPackageName());

        mFirebaseAnalytics.logEvent("Reward_ads_load", bundle);

        RewardedAd.load(activity, REWARDED_AD_UNIT_ID,  // test ad unit ID
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d("suman", "Rewarded Ad loaded successfully");
                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

                        // एक basic event log करें
                        Bundle bundle = new Bundle();
                        bundle.putString("package", activity.getPackageName());

                        mFirebaseAnalytics.logEvent("Reward_ads_loaded_successfully", bundle);

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

                        // एक basic event log करें
                        Bundle bundle = new Bundle();
                        bundle.putString("package", activity.getPackageName());

                        mFirebaseAnalytics.logEvent("Reward_ads_load_failed", bundle);
                        Log.e("suman", "Rewarded Ad failed to load: " + loadAdError.getMessage());

                    }
                });
    }



    public static void showRewardedAd(Activity activity) {
        if (rewardedAd != null) {
            rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();

                   Log.d("suman", "User earned reward: " + rewardAmount + "Thanks ");

                   rewardedAd = null;

                    FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);

                    // एक basic event log करें
                    Bundle bundle = new Bundle();
                    bundle.putString("package", activity.getPackageName());

                    mFirebaseAnalytics.logEvent("Reward_ads_show", bundle);

                   loadRewardedAd(activity);
                }
            });
        } else {

            loadRewardedAd(activity); // try loading again
        }
    }

}
