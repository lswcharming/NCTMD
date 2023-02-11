package com.fxgear.nctmd3;

import android.annotation.SuppressLint;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.TextView;

import com.fxgear.nctmd3.databinding.ActivityFullscreenBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            if (Build.VERSION.SDK_INT >= 30) {
                mContentView.getWindowInsetsController().hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                // Note that some of these constants are new as of API 16 (Jelly Bean)
                // and API 19 (KitKat). It is safe to use them, as they are inlined
                // at compile-time and do nothing on earlier devices.
                mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        }
    };
    private View mControlsView;
//    private final Runnable mShowPart2Runnable = new Runnable() { // [c] test
//        @Override
//        public void run() {
//            // Delayed display of UI elements
//            ActionBar actionBar = getSupportActionBar();
//            if (actionBar != null) {
//                actionBar.show();
//            }
//            mControlsView.setVisibility(View.VISIBLE);
//        }
//    };
//    private boolean mVisible;
//    private final Runnable mHideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            hide();
//        }
//    };
//    /**
//     * Touch listener to use for in-layout UI controls to delay hiding the
//     * system UI. This is to prevent the jarring behavior of controls going away
//     * while interacting with activity UI.
//     */
//    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            switch (motionEvent.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    if (AUTO_HIDE) {
//                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
//                    }
//                    break;
//                case MotionEvent.ACTION_UP:
//                    view.performClick();
//                    break;
//                default:
//                    break;
//            }
//            return false;
//        }
//    };

    public static FullscreenActivity sMainActivity;

    private ActivityFullscreenBinding binding;
    public TextView mTextView;

    private ArrayList<Bitmap> seqBitmapList = new ArrayList<Bitmap>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); // [c] test
        super.onCreate(savedInstanceState);

        sMainActivity = this;

        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        mVisible = true; // [c] test
//        mControlsView = binding.fullscreenContentControls;
//        mContentView = binding.fullscreenContent;
        mContentView = binding.getRoot();

// 전체화면 [c] test [[
//        // Delayed removal of status and navigation bar
//        if (Build.VERSION.SDK_INT >= 30) {
//            mContentView.getWindowInsetsController().hide(
//                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
// ]]

//        // Set up the user interaction to manually show or hide the system UI. // [c] test
//        mContentView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggle();
//            }
//        });
//
//        // Upon interacting with UI controls, delay any scheduled hide()
//        // operations to prevent the jarring behavior of controls going away
//        // while interacting with the UI.
//        binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);

        mTextView = (TextView) binding.fullscreenContent;
    }

    Timer timer = null;
    int curSeqIndex = 0;
    @Override
    protected void onResume() {
        super.onResume();

        // https://www.iloveimg.com/ko/convert-to-jpg/png-to-jpg

//        if (seqBitmapList.size() == 0) {
//            for (int i=0; i<898; i++) {
//                String number = String.format("%05d", i+1);
//                seqBitmapList.add(assetsRead(number + ".png"));
//                Log.e("NCTMD", "[c] onResume() - seqBitmap: " + number);
//            }
//        }

//        if (timer == null) {
//            timer = new Timer();
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    synchronized (this) {
//                        if (curSeqIndex >= 898) curSeqIndex = 0;
//                        binding.imageContent.setImageBitmap(seqBitmapList.get(curSeqIndex));
//                        curSeqIndex++;
//                    }
//                }
//            }, 0, 15);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        if (timer != null) {
//            timer.cancel();
//            timer = null;
//
//            curSeqIndex = 0;
//        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been // [c] test
        // created, to briefly hint to the user that UI controls
        // are available.
//        delayedHide(100); // [c] test
    }

//    private void toggle() { // [c] test
//        if (mVisible) {
//            hide();
//        } else {
//            show();
//        }
//    }
//
//    private void hide() {
//        // Hide UI first
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.hide();
//        }
//        mControlsView.setVisibility(View.GONE);
//        mVisible = false;
//
//        // Schedule a runnable to remove the status and navigation bar after a delay
//        mHideHandler.removeCallbacks(mShowPart2Runnable);
//        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
//    }
//
//    private void show() {
//        // Show the system bar
//        if (Build.VERSION.SDK_INT >= 30) {
//            mContentView.getWindowInsetsController().show(
//                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//        } else {
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//        }
//        mVisible = true;
//
//        // Schedule a runnable to display UI elements after a delay
//        mHideHandler.removeCallbacks(mHidePart2Runnable);
//        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
//    }
//
//    /**
//     * Schedules a call to hide() in delay milliseconds, canceling any
//     * previously scheduled calls.
//     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    public Bitmap assetsRead(String file) {
        InputStream is;
        Bitmap bitmap = null;

        try {
            is = getAssets().open(file);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            // bitmap = Bitmap.createScaledBitmap(bitmap, 300, 340, true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }
}