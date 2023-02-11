package com.fxgear.nctmd3;

import static com.fxgear.nctmd3.FullscreenActivity.sMainActivity;
import static com.fxgear.nctmd3.GlobalDefine.GAUSSIAN_CDF;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class MDImageView extends ImageView {

    private Context mContext;
    private int mScreenWidth;
    private int mScreenHeight;
    private ArrayList<Bitmap> mSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mLeftSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mRightSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mMouthSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mOpenSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mUpSeqBitmapList = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> mDownSeqBitmapList = new ArrayList<Bitmap>();
    private Paint mIdlePaint = new Paint();
    private Paint mOtherPaint = new Paint();

    private float mSeqBitmapTotalIndex = 0.f;
    private int   mSeqBitmapRealIndex = 0;
    private float mLeftSeqBitmapTotalIndex = 0.f;
    private int   mLeftSeqBitmapRealIndex = 0;
    private float mRightSeqBitmapTotalIndex = 0.f;
    private int   mRightSeqBitmapRealIndex = 0;
    private float mMouthSeqBitmapTotalIndex = 0.f;
    private int   mMouthSeqBitmapRealIndex = 0;
    private float mOpenSeqBitmapTotalIndex = 0.f;
    private int   mOpenSeqBitmapRealIndex = 0;
    private float mUpSeqBitmapTotalIndex = 0.f;
    private int   mUpSeqBitmapRealIndex = 0;
    private float mDownSeqBitmapTotalIndex = 0.f;
    private int   mDownSeqBitmapRealIndex = 0;

    private Rect mSrcRect;
    private Rect mDstRect;

    private int mBitmapTopY = 0;
    private int mBitmapBottom3Y = 0;

    // RATIO CDF 500 - 10%
    //private int GAUSSIAN_HALF_RATIO_FRAME = 500;
    //private int GAUSSIAN_RAITO[] = new int[] {12, 26, 38, 52, 68, 84, 104, 128, 166, 200};
    private float mIncreaseGaussianX = 0.f;
    private float mIncreaseVelocityX = 0.f;

    private float mIncreaseVelocityX_Loop = 1.f; // using IDLE

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 0.f;

    // MODE
    enum ANI_MODE {
        IDLE,
        LEFT,
        RIGHT,
        MOUTH,
        OPEN,
        UP,
        DOWN
    }
    private ANI_MODE mAniMode = ANI_MODE.IDLE;

    // MP4 FRAME
    private int MIN_SEQ_SIZE = 60; // 120; // 150
    private int MAX_SEQ_SIZE = 341; // 341 = 400 - 59 // 681 = 800-119 // 251 = 400-149 // 139 = 368-229;
    private int MIN_LEFT_SEQ_SIZE = 27; // 50; // 230
    private int MAX_LEFT_SEQ_SIZE = 174; // 174 = 200-26 // 341 = 330-49
    private int MIN_RIGHT_SEQ_SIZE = 97; // 30; // 230
    private int MAX_RIGHT_SEQ_SIZE = 174; // 174 = 270-96 // 301 = 330-29
    private int MIN_MOUTH_SEQ_SIZE = 20; // 40; // 230
    private int MAX_MOUTH_SEQ_SIZE = 131; // 131 = 150-19 // 261 = 300-39
    private int MIN_OPEN_SEQ_SIZE = 17; // 90; // 230
    private int MAX_OPEN_SEQ_SIZE = 284; // 284 = 300-16 // 391 = 480-89
    private int MIN_UP_SEQ_SIZE = 137; // 30; // 90; // 230
    private int MAX_UP_SEQ_SIZE = 164; // 164 = 300-136 // 271=300-29 // 391 = 480-89
    private int MIN_DOWN_SEQ_SIZE = 27; // 90; // 230
    private int MAX_DOWN_SEQ_SIZE = 174; // 161 = 200-26 // 391 = 480-89
    private Object syncObj = new Object();
    private BitmapFactory.Options opt = new BitmapFactory.Options();

    private boolean mAssetBitmapInitial = false;
    private boolean mReturnWorked = false;
    private boolean mUpDownAvailable = false;

    private final float ALPHA_BLENDING_FRAME = 3.f;

    private final int MESSAGE_SEQ_INCREASE = 1;
    private final int MESSAGE_LEFT_SEQ_RETURN = 2;
    private final int MESSAGE_RIGHT_SEQ_RETURN = 3;
    private final int MESSAGE_MOUTH_SEQ_RETURN = 4;
    private final int MESSAGE_OPEN_SEQ_RETURN = 5;
    private final int MESSAGE_UP_SEQ_RETURN = 6;
    private final int MESSAGE_DOWN_SEQ_RETURN = 7;

    Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MESSAGE_SEQ_INCREASE:
                    synchronized (syncObj) {
                        //mIncreaseVelocityX += 0.01f;
                        //mSeqBitmapTotalIndex += (1.f * (1.0f + mIncreaseVelocityX));
                        mSeqBitmapTotalIndex += (mIncreaseVelocityX_Loop * mIncreaseVelocityX);
                        mSeqBitmapRealIndex = (int)(mSeqBitmapTotalIndex % MAX_SEQ_SIZE);
                    }
                    invalidate();

                    Log.e("NCTMD", "[c] MESSAGE_SEQ_INCREASE: " + mIncreaseVelocityX + ", " + mSeqBitmapTotalIndex);

                    if (mSeqBitmapTotalIndex >= MAX_SEQ_SIZE-1/*340.f*//*250.f*//*138.f*/) {
                        mIncreaseVelocityX_Loop = -1.f;
                    } else if (mSeqBitmapTotalIndex <= 0.f) {
                        mIncreaseVelocityX_Loop = 1.f;
                    }

                    if (mAniMode == ANI_MODE.IDLE) {
                        sendEmptyMessageDelayed(MESSAGE_SEQ_INCREASE, 30);
                    }
                    break;

                case MESSAGE_LEFT_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mLeftSeqBitmapTotalIndex > 0.f) {
                            mLeftSeqBitmapTotalIndex -= CalculateGaussianRatio(mLeftSeqBitmapTotalIndex);
                            if (mLeftSeqBitmapTotalIndex < 0.01f) mLeftSeqBitmapTotalIndex = 0.f;

                            mLeftSeqBitmapRealIndex = (int)(mLeftSeqBitmapTotalIndex % MAX_LEFT_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mLeftSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.LEFT) {
                        Log.e("NCTMD", "[c] MESSAGE_LEFT_SEQ_RETURN: " + mLeftSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_LEFT_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.RIGHT && mAniMode != ANI_MODE.MOUTH) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

                case MESSAGE_RIGHT_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mRightSeqBitmapTotalIndex > 0.f) {
                            mRightSeqBitmapTotalIndex -= CalculateGaussianRatio(mRightSeqBitmapTotalIndex);
                            if (mRightSeqBitmapTotalIndex < 0.01f) mRightSeqBitmapTotalIndex = 0.f;

                            mRightSeqBitmapRealIndex = (int)(mRightSeqBitmapTotalIndex % MAX_RIGHT_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mRightSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.RIGHT) {
                        Log.e("NCTMD", "[c] MESSAGE_RIGHT_SEQ_RETURN: " + mRightSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_RIGHT_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.MOUTH) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

                case MESSAGE_MOUTH_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mMouthSeqBitmapTotalIndex > 0.f) {
                            mMouthSeqBitmapTotalIndex -= CalculateGaussianRatio(mMouthSeqBitmapTotalIndex);
                            if (mMouthSeqBitmapTotalIndex < 0.01f) mMouthSeqBitmapTotalIndex = 0.f;

                            mMouthSeqBitmapRealIndex = (int)(mMouthSeqBitmapTotalIndex % MAX_MOUTH_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mMouthSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.MOUTH) {
                        Log.e("NCTMD", "[c] MESSAGE_MOUTH_SEQ_RETURN: " + mMouthSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_MOUTH_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.RIGHT) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

                case MESSAGE_OPEN_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mOpenSeqBitmapTotalIndex > 0.f) {
                            mOpenSeqBitmapTotalIndex -= CalculateGaussianRatio(mOpenSeqBitmapTotalIndex);
                            if (mOpenSeqBitmapTotalIndex < 0.01f) mOpenSeqBitmapTotalIndex = 0.f;

                            mOpenSeqBitmapRealIndex = (int)(mOpenSeqBitmapTotalIndex % MAX_OPEN_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mOpenSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.OPEN) {
                        Log.e("NCTMD", "[c] MESSAGE_OPEN_SEQ_RETURN: " + mOpenSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_OPEN_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.RIGHT) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

                case MESSAGE_UP_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mUpSeqBitmapTotalIndex > 0.f) {
                            mUpSeqBitmapTotalIndex -= CalculateGaussianRatio(mUpSeqBitmapTotalIndex);
                            if (mUpSeqBitmapTotalIndex < 0.01f) mUpSeqBitmapTotalIndex = 0.f;

                            mUpSeqBitmapRealIndex = (int)(mUpSeqBitmapTotalIndex % MAX_UP_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mUpSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.UP) {
                        Log.e("NCTMD", "[c] MESSAGE_UP_SEQ_RETURN: " + mUpSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_UP_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.RIGHT) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

                case MESSAGE_DOWN_SEQ_RETURN:
                    synchronized (syncObj) {
                        if (mDownSeqBitmapTotalIndex > 0.f) {
                            mDownSeqBitmapTotalIndex -= CalculateGaussianRatio(mDownSeqBitmapTotalIndex);
                            if (mDownSeqBitmapTotalIndex < 0.01f) mDownSeqBitmapTotalIndex = 0.f;

                            mDownSeqBitmapRealIndex = (int)(mDownSeqBitmapTotalIndex % MAX_DOWN_SEQ_SIZE);
                        }
                    }
                    invalidate();

                    if (mDownSeqBitmapTotalIndex > 0.f && mAniMode == ANI_MODE.DOWN) {
                        Log.e("NCTMD", "[c] MESSAGE_DOWN_SEQ_RETURN: " + mDownSeqBitmapTotalIndex);
                        sendEmptyMessage(MESSAGE_DOWN_SEQ_RETURN);

                    } else {
                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.RIGHT) {
                        ActionDownInitial(false);
                        mAniMode = ANI_MODE.IDLE;
                        mIncreaseVelocityX = 1.0f;
                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                        //}
                    }
                    break;

//                case MESSAGE_OPEN_SEQ_RETURN:
//                    synchronized (syncObj) {
//                        mOpenSeqBitmapTotalIndex += 1.0f;
//                        mOpenSeqBitmapRealIndex = (int)(mOpenSeqBitmapTotalIndex % MAX_OPEN_SEQ_SIZE);
//                    }
//                    invalidate();
//
//                    if (mOpenSeqBitmapTotalIndex < 390.f && mAniMode == ANI_MODE.OPEN) {
//                        Log.e("NCTMD", "[c] MESSAGE_OPEN_SEQ_RETURN: " + mOpenSeqBitmapTotalIndex);
//                        sendEmptyMessage(MESSAGE_OPEN_SEQ_RETURN);
//
//                    } else {
//                        //if (mAniMode != ANI_MODE.LEFT && mAniMode != ANI_MODE.RIGHT) {
//                        ActionDownInitial();
//                        mAniMode = ANI_MODE.IDLE;
//                        mIncreaseVelocityX = 1.0f;
//                        sendEmptyMessage(MESSAGE_SEQ_INCREASE);
//                        //}
//                    }
//                    break;
                default:
                    break;
            }
        }

        private float CalculateGaussianRatio(float totalIndex) {
            float result = 0.f;

            if (totalIndex > GAUSSIAN_CDF[GAUSSIAN_CDF.length-1]) {
                return 20.0f * 0.4f;
            }

            mIncreaseGaussianX++;
            double x = mIncreaseGaussianX * 0.01;
            double pdf = 20.0 * Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
            result = (float)pdf;

            Log.e("NCTMD", "[c] CalculateGaussianRatio: " + totalIndex + ", " + mIncreaseGaussianX + ", " + result);
            if (result < 0.001f) result = 0.001f;

            return result;
        }
    };

    public MDImageView(Context context) {
        super(context);
        Log.e("MDImageView", "[c] MDImageView #1");

        Initialized(context);
    }

    public MDImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.e("MDImageView", "[c] MDImageView #2");

        Initialized(context);
    }

    public MDImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.e("MDImageView", "[c] MDImageView #3");

        Initialized(context);
    }

    private void ScaleDetectorInitial() {

        mScaleDetector = new ScaleGestureDetector(mContext, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                float factor = scaleGestureDetector.getScaleFactor() - mScaleFactor;
                Log.e("NCTMD", "[c] onScale: " + mScaleFactor + ", " + scaleGestureDetector.getScaleFactor() + ", " + factor);

                // RETURN CHECK
                //if (factor < 0.f)
                {
                    updateMouthSeqBitmapIdxNInvalidate(factor);
                }

                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {

                if ((mAniMode == ANI_MODE.LEFT && mLeftSeqBitmapRealIndex > 30)
                 || (mAniMode == ANI_MODE.RIGHT && mRightSeqBitmapRealIndex > 30)
                 || (mAniMode == ANI_MODE.UP && mUpSeqBitmapRealIndex > 30)
                 || (mAniMode == ANI_MODE.DOWN && mDownSeqBitmapRealIndex > 30)) {
                    return false;

                } else {
                    ActionDownInitial(true);

                    //mAniMode = ANI_MODE.MOUTH; // in onScale

                    mScaleFactor = scaleGestureDetector.getScaleFactor();
                    Log.e("NCTMD", "[c] onScaleBegin: " + mScaleFactor);
                    return true;
                }

            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                Log.e("NCTMD", "[c] onScaleEnd: " + mAniMode);

                if (mAniMode == ANI_MODE.MOUTH) {

                    mMouthSeqBitmapTotalIndex = mMouthSeqBitmapRealIndex;
                    if (mMouthSeqBitmapTotalIndex > 0) {

                        ActionDownInitial(false);

                        mIncreaseGaussianX = GAUSSIAN_CDF.length - 1; // 274
                        for (int i = 0; i < GAUSSIAN_CDF.length; i++) {
                            if (mMouthSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length - 1] - GAUSSIAN_CDF[i])) {
                                if (i != 0)
                                    mMouthSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length - 1] - GAUSSIAN_CDF[i - 1]);
                                mIncreaseGaussianX = i;
                                break;
                            }
                        }
                        mHandler.sendEmptyMessage(MESSAGE_MOUTH_SEQ_RETURN);
                    }

                } else if (mAniMode == ANI_MODE.OPEN) {

                    mOpenSeqBitmapTotalIndex = mOpenSeqBitmapRealIndex;
                    if (mOpenSeqBitmapTotalIndex > 0) {

                        ActionDownInitial(false);

                        mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
                        for (int i=0; i<GAUSSIAN_CDF.length; i++) {
                            if (mOpenSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
                                if (i!=0) mOpenSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
                                mIncreaseGaussianX = i;
                                break;
                            }
                        }
                        mHandler.sendEmptyMessage(MESSAGE_OPEN_SEQ_RETURN);
                    }

                } else {

                    ActionDownInitial(false);

                    mAniMode = ANI_MODE.IDLE;
                    mIncreaseVelocityX = 1.0f;
                    mHandler.sendEmptyMessage(MESSAGE_SEQ_INCREASE);
                }
            }
        });
    }

    private void AssetBitmapInitial(Context context) {

        // 초기 onDraw 에서 동작
        if (mSeqBitmapList.size() == 0) {
            for (int i=MIN_SEQ_SIZE; i<MIN_SEQ_SIZE+MAX_SEQ_SIZE; i++) {
                String number = String.format("I%04d", i);
                mSeqBitmapList.add(assetsRead(number + ".jpg"));
                Log.e("NCTMD", "[c] Initialized() - seqBitmap: " + number);
            }
        }

        Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                mAssetBitmapInitial = false;

                if (mLeftSeqBitmapList.size() == 0) {
                    for (int i=MIN_LEFT_SEQ_SIZE; i<MIN_LEFT_SEQ_SIZE+MAX_LEFT_SEQ_SIZE; i++) {
                        String number = String.format("L%04d", i);
                        mLeftSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - LeftSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                if (mRightSeqBitmapList.size() == 0) {
                    for (int i=MIN_RIGHT_SEQ_SIZE; i<MIN_RIGHT_SEQ_SIZE+MAX_RIGHT_SEQ_SIZE; i++) {
                        String number = String.format("R%04d", i);
                        mRightSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - RightSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                if (mMouthSeqBitmapList.size() == 0) {
                    for (int i=MIN_MOUTH_SEQ_SIZE; i<MIN_MOUTH_SEQ_SIZE+MAX_MOUTH_SEQ_SIZE; i++) {
                        String number = String.format("M%04d", i);
                        mMouthSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - MouthSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                if (mOpenSeqBitmapList.size() == 0) {
                    for (int i=MIN_OPEN_SEQ_SIZE; i<MIN_OPEN_SEQ_SIZE+MAX_OPEN_SEQ_SIZE; i++) {
                        String number = String.format("O%04d", i);
                        mOpenSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - OpenSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                if (mUpSeqBitmapList.size() == 0) {
                    for (int i=MIN_UP_SEQ_SIZE; i<MIN_UP_SEQ_SIZE+MAX_UP_SEQ_SIZE; i++) {
                        String number = String.format("U%04d", i);
                        mUpSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - UpSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                if (mDownSeqBitmapList.size() == 0) {
                    for (int i=MIN_DOWN_SEQ_SIZE; i<MIN_DOWN_SEQ_SIZE+MAX_DOWN_SEQ_SIZE; i++) {
                        String number = String.format("D%04d", i);
                        mDownSeqBitmapList.add(assetsRead(number + ".jpg"));
                        Log.e("NCTMD", "[c] Initialized() - DownSeqBitmap: " + number);
                    }
                }

                Log.e("NCTMD", "[c] Initialized() - allocNativeHeap: " + Debug.getNativeHeapAllocatedSize());

                mAssetBitmapInitial = true;

                sMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sMainActivity.mTextView.setText("1. One Finger 좌-우 스크롤 해 보세요.\n2. One Finger 이마 부분을 아래-위 스크롤 해 보세요.\n3. Two Finger 줌-인 제스처 해보세요.");
                    }
                });
            }
        });
        thread.start();

    }

    private void Initialized(Context context) {

//        double x = 0.0;
//        float cdf = 0.0f;
//        for (int i=0; i<1000; i++) {
//            x += 0.01;
//            double pdf = 20.0 * Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
//            cdf += (float)pdf;
//            Log.e("", String.format("%ff", cdf));
//        }

        mContext = context;

        ScaleDetectorInitial();

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        if (screenWidth < screenHeight) {
            mScreenWidth = screenWidth;
            mScreenHeight = screenHeight;
        } else {
            mScreenWidth = screenHeight;
            mScreenHeight = screenWidth;
        }
        Log.i("NCTMD", "[c] Initialized() - w: " + mScreenWidth + ", h: " + mScreenHeight);

        AssetBitmapInitial(context);

        mIdlePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        mOtherPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        mSrcRect = new Rect(0,0, mSeqBitmapList.get(0).getWidth(), mSeqBitmapList.get(0).getHeight());
        int scaled_width = mScreenWidth;
        int scaled_height = (int)(((float)mScreenWidth/(float)mSeqBitmapList.get(0).getWidth())*(float)mSeqBitmapList.get(0).getHeight());
        int top = (mScreenHeight - scaled_height) / 2;
        mDstRect = new Rect(0, top, scaled_width, top + scaled_height);
        mBitmapTopY = top;
        mBitmapBottom3Y = top + scaled_height/3;

        Log.e("NCTMD", "[c] Initialized() - ltrb:" + top + ", " + scaled_height);

        // [IDLE] START LOOP
        mIncreaseVelocityX = 1.0f;
        mHandler.sendEmptyMessageDelayed(MESSAGE_SEQ_INCREASE, 1000); // increase loop
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        //mPaint.setAlpha((int)(255.f - (mIncreaseVelocityX/6.f)*255.f));
        synchronized (syncObj) {
            if (mAniMode == ANI_MODE.OPEN) {
//                if (mOpenSeqBitmapRealIndex > MAX_OPEN_SEQ_SIZE-30) {
//                    int openIndex = MAX_OPEN_SEQ_SIZE - mOpenSeqBitmapRealIndex;
//
//                    mIdlePaint.setAlpha((int)(255.f - (255.f*openIndex)/30.f));
//                    canvas.drawBitmap(mSeqBitmapList.get(mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
//                    mOtherPaint.setAlpha((int)((255.f*openIndex)/30.f));
//                    canvas.drawBitmap(mOpenSeqBitmapList.get(mOpenSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
//
//                } else {
//                    mOtherPaint.setAlpha(255);
//                    canvas.drawBitmap(mOpenSeqBitmapList.get(mOpenSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
//                }
                if (mOpenSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mOpenSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint); // x y 상관없음. up시 0로 세팅.
                    mOtherPaint.setAlpha((int)((255.f*mOpenSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mOpenSeqBitmapList.get(mOpenSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mOpenSeqBitmapList.get(mOpenSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else if (mAniMode == ANI_MODE.MOUTH) {
                if (mMouthSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mMouthSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
                    mOtherPaint.setAlpha((int)((255.f*mMouthSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mMouthSeqBitmapList.get(mMouthSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mMouthSeqBitmapList.get(mMouthSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else if (mAniMode == ANI_MODE.UP) {
                if (mUpSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mUpSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
                    mOtherPaint.setAlpha((int)((255.f*mUpSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mUpSeqBitmapList.get(mUpSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mUpSeqBitmapList.get(mUpSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else if (mAniMode == ANI_MODE.DOWN) {
                if (mDownSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mDownSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
                    mOtherPaint.setAlpha((int)((255.f*mDownSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mDownSeqBitmapList.get(mDownSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mDownSeqBitmapList.get(mDownSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else if (mAniMode == ANI_MODE.LEFT) {
                if (mLeftSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mLeftSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
                    mOtherPaint.setAlpha((int)((255.f*mLeftSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mLeftSeqBitmapList.get(mLeftSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mLeftSeqBitmapList.get(mLeftSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else if (mAniMode == ANI_MODE.RIGHT) {
                if (mRightSeqBitmapRealIndex <= (int)ALPHA_BLENDING_FRAME) {
                    mIdlePaint.setAlpha((int)(255.f - (255.f*mRightSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mSeqBitmapList.get((mPreRealX == 0) ? 0 : mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint);
                    mOtherPaint.setAlpha((int)((255.f*mRightSeqBitmapRealIndex)/ALPHA_BLENDING_FRAME));
                    canvas.drawBitmap(mRightSeqBitmapList.get(mRightSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);

                } else {
                    mOtherPaint.setAlpha(255);
                    canvas.drawBitmap(mRightSeqBitmapList.get(mRightSeqBitmapRealIndex), mSrcRect, mDstRect, mOtherPaint);
                }

            } else { // IDLE
                mIdlePaint.setAlpha(255);
                canvas.drawBitmap(mSeqBitmapList.get(mSeqBitmapRealIndex), mSrcRect, mDstRect, mIdlePaint); // [c]
            }
        }
    }

    private void ActionDownInitial(boolean isActionDown) {

        mReturnWorked = false;

        // SEQ
        mHandler.removeMessages(MESSAGE_SEQ_INCREASE);
        if (isActionDown) {
            mSeqBitmapTotalIndex = mSeqBitmapRealIndex;
        } else {
            mSeqBitmapTotalIndex = 0.f; // Retrun시 초기값 (Action Down시 Smoothing필요)
            mSeqBitmapRealIndex = 0;
        }
        mIncreaseVelocityX = 0.f;
        mIncreaseVelocityX_Loop = 1.f;

        // LEFT
        mHandler.removeMessages(MESSAGE_LEFT_SEQ_RETURN);
        mHandler.removeMessages(MESSAGE_RIGHT_SEQ_RETURN);
        mHandler.removeMessages(MESSAGE_MOUTH_SEQ_RETURN);
        mHandler.removeMessages(MESSAGE_OPEN_SEQ_RETURN);
        mHandler.removeMessages(MESSAGE_UP_SEQ_RETURN);
        mHandler.removeMessages(MESSAGE_DOWN_SEQ_RETURN);
        mIncreaseGaussianX = 0.f;
//        // OPEN NOT RETURN, CONTINUE...
//        mOpenSeqBitmapTotalIndex = 0.f;
//        mOpenSeqBitmapRealIndex = 0;
    }

    private VelocityTracker mVelocityTracker = null;
    private float mPreTotalX = 0.f;
    private float mPreRealX = 0.f;
    private float mPreRealY = 0.f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!mAssetBitmapInitial) {
            Log.e("NCTMD", "[c] onTouchEvent: return !AssetBitmapInitial");
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);

        mScaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("NCTMD", "[c] ACTION_DOWN: " + x + ", " + y);

                ActionDownInitial(true);

                if (mAniMode == ANI_MODE.OPEN || mAniMode == ANI_MODE.MOUTH) {
                    mAniMode = ANI_MODE.IDLE; // [c] test, OPEN or MOUTH 동작시 two finger 들어가면 IDLE로 돌아갔다가 감.
                }

                if (y > mBitmapTopY && y < mBitmapBottom3Y) {
                    Log.e("NCTMD", "[c] ACTION_DOWN: UpDownAvailable");
                    mUpDownAvailable = true;
                }

                // mPreTotalX = mSeqBitmapTotalIndex;
                mPreRealX = x;
                mPreRealY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.e("NCTMD", "[c] ACTION_UP: " + x + ", " + y + ", " + mReturnWorked + ", " + mAniMode);

                if (mAniMode == ANI_MODE.MOUTH || mAniMode == ANI_MODE.OPEN) {
                    return true;
                }

                float velocityX = 0.f;
                if (mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(100);

                    velocityX = mVelocityTracker.getXVelocity();
                    Log.e("NCTMD", "[c] VELOCITY X: " + velocityX);

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

//                if (velocityX > 600.f) {
//
//                    ActionDownInitial(false);
//                    mAniMode = ANI_MODE.OPEN;
//                    mOpenSeqBitmapTotalIndex = 0.f;
//                    mOpenSeqBitmapRealIndex = 0;
//                    mHandler.sendEmptyMessage(MESSAGE_OPEN_SEQ_RETURN); // not return, continue...
//
////                    if (mReturnWorked) {
////                        mReturnWorked = false;
////
////                        mHandler.removeMessages(MESSAGE_SEQ_RETURN);
////                        mIncreaseGaussianX = 0.f;
////                    }
////                    //updateSeqBitmapIdxNInvalidate(x, true);
////                    mSeqBitmapTotalIndex = mSeqBitmapRealIndex;
////
////                    //mIncreaseVelocityX = (velocityX - 600.f) / 500.f;
////                    mIncreaseVelocityX = 1.0f;
////                    mHandler.sendEmptyMessage(MESSAGE_SEQ_INCREASE); // increase loop
//
//                } else
                {
                    if (!mReturnWorked) {
                        //updateSeqBitmapIdxNInvalidate(x, true);

                        if (mUpDownAvailable) {
                            // UP RETURN
                            mUpSeqBitmapTotalIndex = mUpSeqBitmapRealIndex;
                            if (mUpSeqBitmapTotalIndex > 0) {

                                ActionDownInitial(false);

                                mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
                                for (int i=0; i<GAUSSIAN_CDF.length; i++) {
                                    if (mUpSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
                                        if (i!=0) mUpSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
                                        mIncreaseGaussianX = i;
                                        break;
                                    }
                                }
                                mHandler.sendEmptyMessage(MESSAGE_UP_SEQ_RETURN);
                            }

                            // DOWN RETURN
                            mDownSeqBitmapTotalIndex = mDownSeqBitmapRealIndex;
                            if (mDownSeqBitmapTotalIndex > 0) {

                                ActionDownInitial(false);

                                mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
                                for (int i=0; i<GAUSSIAN_CDF.length; i++) {
                                    if (mDownSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
                                        if (i!=0) mDownSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
                                        mIncreaseGaussianX = i;
                                        break;
                                    }
                                }
                                mHandler.sendEmptyMessage(MESSAGE_DOWN_SEQ_RETURN);
                            }

                        } else {
                            // LEFT RETURN
                            mLeftSeqBitmapTotalIndex = mLeftSeqBitmapRealIndex;
                            if (mLeftSeqBitmapTotalIndex > 0) {

                                ActionDownInitial(false);

                                mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
                                for (int i=0; i<GAUSSIAN_CDF.length; i++) {
                                    if (mLeftSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
                                        if (i!=0) mLeftSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
                                        mIncreaseGaussianX = i;
                                        break;
                                    }
                                }
                                mHandler.sendEmptyMessage(MESSAGE_LEFT_SEQ_RETURN);
                            }

                            // RIGHT RETURN
                            mRightSeqBitmapTotalIndex = mRightSeqBitmapRealIndex;
                            if (mRightSeqBitmapTotalIndex > 0) {

                                ActionDownInitial(false);

                                mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
                                for (int i=0; i<GAUSSIAN_CDF.length; i++) {
                                    if (mRightSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
                                        if (i!=0) mRightSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
                                        mIncreaseGaussianX = i;
                                        break;
                                    }
                                }
                                mHandler.sendEmptyMessage(MESSAGE_RIGHT_SEQ_RETURN);
                            }

                        } // if mUpDownAvailable

                    } else {
                        Log.e("NCTMD", "[c] ACTION_UP: mReturnWorked == true");
                    }
                }

                mUpDownAvailable = false;

                mPreRealX = 0.f;
                mPreRealY = 0.f;
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("NCTMD", "[c] ACTION_MOVE: " + x + ", " + y + ", " + mReturnWorked + ", " + mAniMode);

                if (mAniMode == ANI_MODE.MOUTH || mAniMode == ANI_MODE.OPEN) {
                    return true;
                }
//                if (mReturnWorked) {
//                    return true;
//                }

                if (mUpDownAvailable) {
                    // UP-DOWN RETURN CHECK
                    int disY = (int)((y - mPreRealY) * 0.6f);
                    if (disY > -160 && disY < 150) { // 180 // 330 // 24 // RIGHT && LEFT
                        updateUpDownSeqBitmapIdxNInvalidate(y, false);
                    }
                } else {
                    // LEFT-RIGHT RETURN CHECK
                    int disX = (int)((x - mPreRealX) * 0.3f);
                    if (disX > -173 && disX < 173) { // 180 // 330 // 24 // RIGHT && LEFT
                        updateLeftRightSeqBitmapIdxNInvalidate(x, false);
                    }
                }

//                else {
//                    mReturnWorked = true;
//
//                    updateSeqBitmapIdxNInvalidate(x, true);
//
//                    mIncreaseGaussianX = GAUSSIAN_CDF.length-1; // 274
//                    for (int i=0; i<GAUSSIAN_CDF.length; i++) {
//                        if (mSeqBitmapTotalIndex > /*(2.0f * 495.0f)*//*990.f*/(GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i])) {
//                            if (i!=0) mSeqBitmapTotalIndex = (GAUSSIAN_CDF[GAUSSIAN_CDF.length-1] - GAUSSIAN_CDF[i-1]);
//                            mIncreaseGaussianX = i;
//                            break;
//                        }
//                    }
//                    mHandler.sendEmptyMessage(MESSAGE_SEQ_RETURN);
//                }
                break;
        }

        return true; // super.onTouchEvent(event);
    }

    private void updateUpDownSeqBitmapIdxNInvalidate(float curY, boolean updateTotalIdx) {
        if (mPreRealY != 0) {
            int disY = (int)((curY - mPreRealY) * 0.6f);
            if (disY != 0) {
                synchronized (syncObj) {
                    // UP
                    float totalIdx = mUpSeqBitmapTotalIndex + (-1)*disY;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.UP;

                    if (updateTotalIdx) mUpSeqBitmapTotalIndex = totalIdx;
                    //mUpSeqBitmapRealIndex = (int)(totalIdx % MAX_UP_SEQ_SIZE);
                    mUpSeqBitmapRealIndex = ((int)totalIdx >= MAX_UP_SEQ_SIZE) ? MAX_UP_SEQ_SIZE-1 : (int)totalIdx;

                    // DOWN
                    totalIdx = mDownSeqBitmapTotalIndex + disY;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.DOWN;

                    if (updateTotalIdx) mDownSeqBitmapTotalIndex = totalIdx;
                    //mDownSeqBitmapRealIndex = (int)(totalIdx % MAX_DOWN_SEQ_SIZE);
                    mDownSeqBitmapRealIndex = ((int)totalIdx >= MAX_DOWN_SEQ_SIZE) ? MAX_DOWN_SEQ_SIZE-1 : (int)totalIdx;
                }
                invalidate();
            }
        } else {
            Log.e("NCTMD", "[c] Error ACTION_DOWN event not working!!!");
        }
    }

    private void updateLeftRightSeqBitmapIdxNInvalidate(float curX, boolean updateTotalIdx) {
        if (mPreRealX != 0) {
            int disX = (int)((curX - mPreRealX) * 0.3f);
            if (disX != 0) {
                synchronized (syncObj) {
                    // LEFT
                    float totalIdx = mLeftSeqBitmapTotalIndex + disX;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.LEFT;

                    if (updateTotalIdx) mLeftSeqBitmapTotalIndex = totalIdx;
                    //mLeftSeqBitmapRealIndex = (int)(totalIdx % MAX_LEFT_SEQ_SIZE);
                    mLeftSeqBitmapRealIndex = ((int)totalIdx >= MAX_LEFT_SEQ_SIZE) ? MAX_LEFT_SEQ_SIZE-1 : (int)totalIdx;

                    // RIGHT
                    totalIdx = mRightSeqBitmapTotalIndex + (-1)*disX;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.RIGHT;

                    if (updateTotalIdx) mRightSeqBitmapTotalIndex = totalIdx;
                    //mRightSeqBitmapRealIndex = (int)(totalIdx % MAX_RIGHT_SEQ_SIZE);
                    mRightSeqBitmapRealIndex = ((int)totalIdx >= MAX_RIGHT_SEQ_SIZE) ? MAX_RIGHT_SEQ_SIZE-1 : (int)totalIdx;
                }
                invalidate();
            }
        } else {
            Log.e("NCTMD", "[c] Error ACTION_DOWN event not working!!!");
        }
    }

    private void updateMouthSeqBitmapIdxNInvalidate(float factor) {
        if (mScaleFactor != 0) {
            //int disX = (int)((factor) * 10.f);
            //if (disX != 0) {
            //if (factor < 0.f)
            {
                synchronized (syncObj) {
                    // MOUTH
                    float totalIdx = mMouthSeqBitmapTotalIndex + (int)((-200.f)*factor); // disX;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.MOUTH;

                    //mMouthSeqBitmapRealIndex = (int)(totalIdx % MAX_MOUTH_SEQ_SIZE);
                    mMouthSeqBitmapRealIndex = ((int)totalIdx >= MAX_MOUTH_SEQ_SIZE) ? MAX_MOUTH_SEQ_SIZE-1 : (int)totalIdx;

                    // OPEN
                    totalIdx = mOpenSeqBitmapTotalIndex + (int)((30.f)*factor); // disX;
                    if (totalIdx < 0) totalIdx = 0;
                    else mAniMode = ANI_MODE.OPEN;

                    //mOpenSeqBitmapRealIndex = (int)(totalIdx % MAX_OPEN_SEQ_SIZE);
                    mOpenSeqBitmapRealIndex = ((int)totalIdx >= MAX_OPEN_SEQ_SIZE) ? MAX_OPEN_SEQ_SIZE-1 : (int)totalIdx;
                }
                invalidate();
            }
//        else {
//            Log.e("NCTMD", "[c] Error ACTION_DOWN event not working!!!");
//        }
        }
    }

    public Bitmap assetsRead(String file) {
        InputStream is;
        Bitmap bitmap = null;

        try {
            is = mContext.getAssets().open(file);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            opt.inSampleSize = 2;
            bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opt);
            // bitmap = Bitmap.createScaledBitmap(bitmap, 300, 340, true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
