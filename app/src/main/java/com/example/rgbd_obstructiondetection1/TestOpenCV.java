package com.example.rgbd_obstructiondetection1;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_16UC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;


public class TestOpenCV extends AppCompatActivity {

    private static final String TAG = "TestOpenCV";

    public static  final int IMAGE_REQUEST_CODE = 0;

    private Button btn1;
    private Button btn2;
    private ImageView orgPicture;
    private ImageView grayPicture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_open_c_v);
        Resources resources = this.getResources();
        btn1 = findViewById(R.id.choose_photo);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                Bitmap bp = BitmapFactory.decodeResource(resources, R.drawable.rgbtest);
                orgPicture.setImageBitmap(bp);
            }
        });
        btn2 = findViewById(R.id.change_photo);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Bitmap bp = BitmapFactory.decodeResource(resources, R.drawable.test);
//                Mat src = new Mat();
//                Mat dst = new Mat();
//                Utils.bitmapToMat(bp, src);
//                Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGRA2GRAY);
//                Utils.matToBitmap(dst, bp);
//                grayPicture.setImageBitmap(bp);
//                src.release();
//                dst.release();
                Log.d(TAG, "onClick: ");
                Bitmap depBitmap = BitmapFactory.decodeResource(resources, R.drawable.rgbtest);
                Log.d(TAG, "onClick: 1");
                Mat depMat = new Mat(depBitmap.getHeight(), depBitmap.getWidth(), CV_8UC4);
                Log.d(TAG, "onClick: 2");
                Utils.bitmapToMat(depBitmap, depMat);
                Log.d(TAG, "onClick: 3");
                Core.convertScaleAbs(depMat,depMat,0.3);
//                添加
//                depMat.convertTo(depMat,CV_8UC1);
                Log.d(TAG, "core");
                Imgproc.cvtColor(depMat, depMat, Imgproc.COLOR_BGRA2BGR);
                Log.d(TAG, "onClick: jet");
                Imgproc.applyColorMap(depMat, depMat, Imgproc.COLORMAP_JET);
                Log.d(TAG, "imgproc");
                Imgproc.cvtColor(depMat, depMat, Imgproc.COLOR_BGR2GRAY);
//                depMat.convertTo(depMat,CV_16UC1);
                Log.d(TAG, "mat to bitmap");
                Utils.matToBitmap(depMat, depBitmap);
                Log.d(TAG, "show result");
                grayPicture.setImageBitmap(depBitmap);
                depMat.release();
            }
        });
        orgPicture = findViewById(R.id.org);
        grayPicture = findViewById(R.id.gray);
    }
}