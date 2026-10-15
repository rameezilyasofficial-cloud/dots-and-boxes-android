package com.fixtocode.dotsandboxes;

import android.app.Activity;
import android.content.Context;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.metadata.MetaData;

public class UnityAdsBridge {
    private static boolean isAdLoaded = false;
    private static String activePlacementId = "Interstitial_Android";

    public interface AdCompleteCallback {
        void onAdFinished();
        void onAdFailed();
    }

    public static void initialize(final Context context, final String gameId, final boolean testMode) {
        try {
            MetaData privacyMeta = new MetaData(context);
            privacyMeta.set("user.nonbehavioral", true);
            privacyMeta.commit();

            MetaData ageGateMeta = new MetaData(context);
            ageGateMeta.set("player.age_gate", true);
            ageGateMeta.commit();

            UnityAds.initialize(context.getApplicationContext(), gameId, testMode, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    preloadAd("Interstitial_Android");
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                    isAdLoaded = false;
                }
            });
        } catch (Exception e) {
            isAdLoaded = false;
        }
    }

    public static void preloadAd(final String placementId) {
        try {
            UnityAds.load(placementId, new IUnityAdsLoadListener() {
                @Override
                public void onUnityAdsAdLoaded(String loadedPlacementId) {
                    activePlacementId = loadedPlacementId;
                    isAdLoaded = true;
                }

                @Override
                public void onUnityAdsFailedToLoad(String failedPlacementId, UnityAds.UnityAdsLoadError error, String message) {
                    if ("Interstitial_Android".equals(failedPlacementId)) {
                        preloadAd("video");
                    } else {
                        isAdLoaded = false;
                    }
                }
            });
        } catch (Exception e) {
            isAdLoaded = false;
        }
    }

    public static boolean isReady() {
        return isAdLoaded;
    }

    public static void showInterstitial(final Activity activity, final AdCompleteCallback callback) {
        if (!isAdLoaded) {
            preloadAd("Interstitial_Android");
            callback.onAdFailed();
            return;
        }
        isAdLoaded = false;
        try {
            UnityAds.show(activity, activePlacementId, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
                @Override
                public void onUnityAdsShowStart(String placementId) {}

                @Override
                public void onUnityAdsShowClick(String placementId) {}

                @Override
                public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                    preloadAd("Interstitial_Android");
                    callback.onAdFinished();
                }

                @Override
                public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                    preloadAd("Interstitial_Android");
                    callback.onAdFailed();
                }
            });
        } catch (Exception e) {
            callback.onAdFailed();
        }
    }
}