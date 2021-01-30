package com.example.rgbd_obstructiondetection1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.intel.realsense.librealsense.Align;
import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DecimationFilter;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameReleaser;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.HoleFillingFilter;
import com.intel.realsense.librealsense.Option;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.Pointcloud;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.SpatialFilter;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.TemporalFilter;
import com.intel.realsense.librealsense.ThresholdFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;

import static org.opencv.core.CvType.CV_8UC4;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "librs process example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
//    private GLRsSurfaceView mGLSurfaceViewOrg;
    //    private GLRsSurfaceView mGLSurfaceViewProcessed;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;

    //filters
    private Align mAlign;
    private Colorizer mColorizerOrg;
    private Colorizer mColorizerProcessed;
    private DecimationFilter mDecimationFilter;
    private HoleFillingFilter mHoleFillingFilter;
    private Pointcloud mPointcloud;
    private TemporalFilter mTemporalFilter;
    private ThresholdFilter mThresholdFilter;
    private SpatialFilter mSpatialFilter;

    private ImageView rgbView;
    private ImageView grayDepthView;
//    private Button btn;

    private RsContext mRsContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);

        grayDepthView = findViewById(R.id.grayDepthView);
        grayDepthView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        rgbView = findViewById(R.id.RGBView);
        rgbView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

//        跳转到一个活动测试openCV内容能否正常使用
//        btn = findViewById(R.id.testOpenCV);
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this,TestOpenCV.class);
//                startActivity(intent);
//            }
//        });


//        mGLSurfaceViewOrg = findViewById(R.id.glSurfaceViewOrg);
//        mGLSurfaceViewOrg.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

//        mGLSurfaceViewProcessed = findViewById(R.id.glSurfaceViewProcessed);
//        mGLSurfaceViewProcessed.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                | View.SYSTEM_UI_FLAG_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        mPermissionsGranted = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mGLSurfaceViewOrg.close();
//        mGLSurfaceViewProcessed.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }
        mPermissionsGranted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
//                免除OpenCV Manager的安装。
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
//                权限申请成功,执行init()方法
        if (mPermissionsGranted)
            init();
        else
            Log.e(TAG, "missing permissions");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (mRsContext != null)
            mRsContext.close();
        stop();
        mPipeline.close();
    }

    private void init() {
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();

        //init filters
        mAlign = new Align(StreamType.COLOR);
        mColorizerOrg = new Colorizer();
//        mColorizerProcessed = new Colorizer();
        mDecimationFilter = new DecimationFilter();
        mHoleFillingFilter = new HoleFillingFilter();
        mPointcloud = new Pointcloud();
        mTemporalFilter = new TemporalFilter();
        mThresholdFilter = new ThresholdFilter();
        mSpatialFilter = new SpatialFilter();

        //config filters
        mThresholdFilter.setValue(Option.MIN_DISTANCE, 0.1f);
        mThresholdFilter.setValue(Option.MAX_DISTANCE, 0.8f);

        mDecimationFilter.setValue(Option.FILTER_MAGNITUDE, 8);

        try (DeviceList dl = mRsContext.queryDevices()) {
            if (dl.getDeviceCount() > 0) {
                showConnectLabel(false);
                start();
            }
        }
    }

    private void showConnectLabel(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);
                mBackGroundText.setText("");
            }
        });
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            showConnectLabel(false);
        }

        @Override
        public void onDeviceDetach() {
            showConnectLabel(true);
            stop();
        }
    };

    Runnable mStreaming = new Runnable() {
        @Override
        public void run() {
            try {
                try (FrameReleaser fr = new FrameReleaser()) {
                    FrameSet frames = mPipeline.waitForFrames(1000).releaseWith(fr);
                    FrameSet orgSet = frames.applyFilter(mColorizerOrg).releaseWith(fr);
//                    FrameSet processedSet = frames.applyFilter(mDecimationFilter).releaseWith(fr).
//                            applyFilter(mHoleFillingFilter).releaseWith(fr).
//                            applyFilter(mTemporalFilter).releaseWith(fr).
//                            applyFilter(mSpatialFilter).releaseWith(fr).
//                            applyFilter(mThresholdFilter).releaseWith(fr).
//                            applyFilter(mColorizerProcessed).releaseWith(fr).
//                            applyFilter(mAlign).releaseWith(fr);
//                    FrameSet processedSet = frames.applyFilter(mAlign).releaseWith(fr).
//                            applyFilter(mThresholdFilter).releaseWith(fr).
//                            applyFilter(mSpatialFilter).releaseWith(fr).
//                            applyFilter(mHoleFillingFilter).releaseWith(fr).
//                            applyFilter(mColorizerProcessed).releaseWith(fr);
//                    使用try来autoclose会有频闪现象
//                    try (Frame org = orgSet.first(StreamType.DEPTH, StreamFormat.RGB8).releaseWith(fr)) {
//                        try (Frame processed = processedSet.first(StreamType.DEPTH, StreamFormat.RGB8).releaseWith(fr)) {
//                    mGLSurfaceViewOrg.upload(org);
//                            mGLSurfaceViewProcessed.setVisibility(View.GONE);
                    startTime = System.currentTimeMillis();
//                    imageView显示rgb图像
                    Frame org = orgSet.first(StreamType.COLOR,StreamFormat.RGB8).releaseWith(fr);
                    byte[] rgbData = new byte[640 * 480 * 3];
                    org.getData(rgbData);
                    int[] rgbIntData = convertByteToColor(rgbData);
                    Bitmap rgbBitmap = Bitmap.createBitmap(rgbIntData,640,480,Bitmap.Config.ARGB_8888);
                    rgbView.setImageBitmap(rgbBitmap);
//                    获取单通道深度图
                    Frame processed = orgSet.first(StreamType.DEPTH, StreamFormat.RGB8).releaseWith(fr);
                    byte[] proData = new byte[640 * 480 * 3];
                    processed.getData(proData);
                    int[] intData = convertByteToColor(proData);
                    Bitmap depBitmap = Bitmap.createBitmap(intData, 640, 480, Bitmap.Config.ARGB_8888);
                    Mat depMat = new Mat(depBitmap.getHeight(), depBitmap.getWidth(), CV_8UC4);
                    Utils.bitmapToMat(depBitmap, depMat);
                    Core.convertScaleAbs(depMat, depMat, 0.3);
                    Imgproc.cvtColor(depMat, depMat, Imgproc.COLOR_BGRA2BGR);
                    Imgproc.applyColorMap(depMat, depMat, Imgproc.COLORMAP_JET);
                    Imgproc.cvtColor(depMat, depMat, Imgproc.COLOR_RGB2GRAY);
//                     depMat.convertTo(depMat,CV_16UC1);
                    Utils.matToBitmap(depMat, depBitmap);
//                     输出到ui界面
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            grayDepthView.setImageBitmap(depBitmap);
                        }
                    });
//                            mGLSurfaceViewProcessed.upload(processed);
                }
                showResultOnUI();
                mHandler.post(mStreaming);
            } catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private long endTime = 0;
    private long startTime = 0;
    float total_fps = 0;
    int fps_count = 0;

    protected void showResultOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                endTime = System.currentTimeMillis();
                long dur = endTime - startTime;
                float fps = (float) (1000.0 / dur);
                total_fps = (total_fps == 0) ? fps : (total_fps + fps);
                fps_count++;
                mBackGroundText.setText(String.format(Locale.CHINESE,
                        "FPS: %.3f\nAVG_FPS: %.3f", fps, (float) total_fps / fps_count));
            }
        });
    }

    private void configAndStart() throws Exception {
        try (Config config = new Config()) {
            config.enableStream(StreamType.DEPTH, 640, 480);
            config.enableStream(StreamType.COLOR, 640, 480);
            // try statement needed here to release resources allocated by the Pipeline:start() method
            try (PipelineProfile pp = mPipeline.start(config)) {
            }
        }
    }

    private synchronized void start() {
        if (mIsStreaming)
            return;
        try {
            Log.d(TAG, "try start streaming");
//            mGLSurfaceViewOrg.clear();
//            mGLSurfaceViewProcessed.clear();
            configAndStart();
            mIsStreaming = true;
            mHandler.post(mStreaming);
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    private synchronized void stop() {
        if (!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();
            Log.d(TAG, "streaming stopped successfully");
//            mGLSurfaceViewOrg.clear();
//            mGLSurfaceViewProcessed.clear();
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
            mPipeline = null;
            mColorizerOrg.close();
//            mColorizerProcessed.close();
        }
    }

    public static int convertByteToInt(byte data) {
        int heightBit = (int) ((data >> 4) & 0x0F);
        int lowBit = (int) (0x0F & data);
        return heightBit * 16 + lowBit;
    }

    public static int[] convertByteToColor(byte[] data) {
        int size = data.length;
        if (size == 0) {
            return null;
        }

        int arg = 0;
        if (size % 3 != 0) {
            arg = 1;
        }

        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        int[] color = new int[size / 3 + arg];
        int red, green, blue;
        int colorLen = color.length;
        if (arg == 0) {
            for (int i = 0; i < colorLen; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }
        } else {
            for (int i = 0; i < colorLen - 1; ++i) {
                red = convertByteToInt(data[i * 3]);
                green = convertByteToInt(data[i * 3 + 1]);
                blue = convertByteToInt(data[i * 3 + 2]);
                color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
            }

            color[colorLen - 1] = 0xFF000000;
        }

        return color;
    }
}