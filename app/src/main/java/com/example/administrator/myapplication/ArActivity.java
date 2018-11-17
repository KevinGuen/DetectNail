
package com.example.administrator.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ArActivity extends AppCompatActivity implements View.OnTouchListener {


    static{

        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");

    }
    String shopName, nailTipname;
    int Captureflag=0;
    private static final String TAG = "Opencv";
    Preview preview;
    int [] resultPoint = new int[20];
    Camera camera;
    Context ctx;
    int CAMERA_FACING_stat = Camera.CameraInfo.CAMERA_FACING_BACK;
    private final static int PERMISSIONS_REQUEST_CODE = 100;
    private AppCompatActivity mActivity;
    public static void doRestart(Context c) {

        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
    }


    public void startCamera() {

        if ( preview == null ) {


            FrameLayout frameLayout = (FrameLayout)findViewById(R.id.layout);
            double ratio  = (double)frameLayout.getHeight() / (double)frameLayout.getWidth();

            preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView), ratio);


            preview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ((FrameLayout) findViewById(R.id.layout)).addView(preview);

            //프리뷰 화면 눌렀을 때  사진을 찍음
        }

        preview.setCamera(null);
        if (camera != null) {
            camera.release();
            camera = null;
        }

        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {

                // Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK
                int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
                camera = Camera.open(CAMERA_FACING);
                // camera orientation
                camera.setDisplayOrientation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                // get Camera parameters
                Camera.Parameters params = camera.getParameters();
                // picture image orientation
                params.setRotation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                camera.startPreview();

            } catch (RuntimeException ex) {
                Toast.makeText(ctx, "camera_not_found " + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                Log.d(TAG, "camera_not_found " + ex.getMessage().toString());
            }
        }

        preview.setCamera(camera);

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        mActivity = this;
        setContentView(R.layout.activity_ar);
        FrameLayout testView = (FrameLayout) findViewById(R.id.introView);


        testView.setOnTouchListener(this);


        Response.Listener<String> responseListener = new Response.Listener<String>(){

            @Override
            public void onResponse(String response){

                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    int Size = jsonResponse.getInt("size");


                    if (Size > 0) {
                        for (int i = 1; i <= Size; i++) {

                            String Key = "Image" + String.valueOf(i);
                            String tempGet = jsonResponse.getString(Key);
                            byte[] decodedString = Base64.decode(tempGet, Base64.DEFAULT);
                            Bitmap tmpbit = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            tmpbit = Bitmap.createScaledBitmap( tmpbit,312,140,false);
                            addImageView(tmpbit, i);

                        }
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        DownloadTips downloadtips = new DownloadTips( "gkhadan", responseListener);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(downloadtips);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //API 23 이상이면 런타임 퍼미션 처리 필요

                int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
                int hasWriteExternalStoragePermission =
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (hasCameraPermission == PackageManager.PERMISSION_GRANTED
                        && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED) {
                    ;//이미 퍼미션을 가지고 있음
                } else {
                    //퍼미션 요청
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_CODE);
                }
            } else {
                ;
            }
        }
        else {
            Toast.makeText(this, "Camera not supported", Toast.LENGTH_LONG).show();
        }



    }
    public void addImageView(Bitmap bitmap, int idNum){
        ImageView.OnClickListener TipsClickListener = new View.OnClickListener(){
            public void onClick(View v){
                int i = v.getId();
                changeTips(i);
            }
        };
        ImageView iv = new ImageView(this);
        LinearLayout horizontalScrollview = (LinearLayout)findViewById(R.id.horizonView);
        LinearLayout.LayoutParams Iparms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iv.setLayoutParams(Iparms);
        iv.setImageBitmap(bitmap);
        iv.setId(idNum);
        iv.setOnClickListener(TipsClickListener);
        horizontalScrollview.addView(iv);

    }
    public void changeTips(int idNum){

        Response.Listener<String> responseListener = new Response.Listener<String>(){

            @Override
            public void onResponse(String response){
                Bitmap bitmap[] = new Bitmap[5];
                try {
                    JSONObject jsonResponse = new JSONObject(response);

                    for (int i = 1; i <= 5; i++) {

                        String Key = "Image" + String.valueOf(i);
                        String tempGet = jsonResponse.getString(Key);
                        byte[] decodedString = Base64.decode(tempGet, Base64.DEFAULT);
                        Bitmap tmpbit = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        bitmap[i-1] = tmpbit;
                    }

                    ImageView finger1 = (ImageView) findViewById(R.id.finger);
                    ImageView finger2 = (ImageView) findViewById(R.id.finger2);
                    ImageView finger3 = (ImageView) findViewById(R.id.finger3);
                    ImageView finger4 = (ImageView) findViewById(R.id.finger4);
                    ImageView finger5 = (ImageView) findViewById(R.id.finger5);


                    finger1.setImageBitmap(bitmap[0]);
                    finger2.setImageBitmap(bitmap[1]);
                    finger3.setImageBitmap(bitmap[2]);
                    finger4.setImageBitmap(bitmap[3]);
                    finger5.setImageBitmap(bitmap[4]);


                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        DownloadTip downloadTip = new DownloadTip("gkhadan", idNum, responseListener);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(downloadTip);




    }



    public void CaptureBt(View v) throws Exception {

        if(Captureflag == 0) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {

                    if (success) {

                        Button cptBt = (Button) findViewById(R.id.cmnbt);
                        cptBt.setVisibility(View.GONE);
                        Button saveBt = (Button) findViewById(R.id.saveBt);
                        saveBt.setVisibility(View.VISIBLE);
                        camera.takePicture(shutterCallback, null, jpegCallback);

                    }
                    else
                        Toast.makeText(getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
                }
            });
        }
        }

    public void Changeview(View v) {

        ImageView resultView = (ImageView) findViewById(R.id.imageView2);
        ImageView finger = (ImageView) findViewById(R.id.finger);
        ImageView finger2 = (ImageView) findViewById(R.id.finger2);
        ImageView finger3 = (ImageView) findViewById(R.id.finger3);
        ImageView finger4 = (ImageView) findViewById(R.id.finger4);
        ImageView finger5 = (ImageView) findViewById(R.id.finger5);
        Button cptBt = (Button)findViewById(R.id.cmnbt);
        Button saveBt = (Button)findViewById(R.id.saveBt);


        if (v.getId() == R.id.refresh) {
            resultPoint = null;
            resultView.setVisibility(View.GONE);
            finger.setVisibility(View.GONE);
            finger2.setVisibility(View.GONE);
            finger3.setVisibility(View.GONE);
            finger4.setVisibility(View.GONE);
            finger5.setVisibility(View.GONE);
            cptBt.setVisibility(View.VISIBLE);
            saveBt.setVisibility(View.GONE);

        }
        }



    @Override
    protected void onResume() {
        super.onResume();

        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Surface will be destroyed when we return, so stop the preview.
        if(camera != null) {
            // Call stopPreview() to stop updating the preview surface
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;

        }

        ((FrameLayout) findViewById(R.id.layout)).removeView(preview);
        preview = null;

    }

    private void resetCam() {
        startCamera();
    }



    public Bitmap rotateImage(Bitmap src, float degree) {

        // Matrix 객체 생성
        Matrix matrix = new Matrix();
        // 회전 각도 셋팅
        matrix.postRotate(degree);
        // 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }


    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Mat img_output;
            Mat img_input;

            final ImageView imgback2 = (ImageView) findViewById(R.id.imageView2);
            // new SaveImageTask().execute(data);
            Bitmap tempback = BitmapFactory.decodeByteArray(data, 0, data.length);

            Bitmap tempback2 = rotateImage(tempback, 90);

            img_output = new Mat();

            img_input = new Mat (tempback2.getWidth(), tempback2.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(tempback2, img_input);

            resultPoint = Test(img_input.getNativeObjAddr(), img_output.getNativeObjAddr());
            if(resultPoint.length == 1){

                ImageView resultView = (ImageView) findViewById(R.id.imageView2);
                ImageView finger = (ImageView) findViewById(R.id.finger);
                ImageView finger2 = (ImageView) findViewById(R.id.finger2);
                ImageView finger3 = (ImageView) findViewById(R.id.finger3);
                ImageView finger4 = (ImageView) findViewById(R.id.finger4);
                ImageView finger5 = (ImageView) findViewById(R.id.finger5);
                Button cptBt = (Button)findViewById(R.id.cmnbt);
                Button saveBt = (Button)findViewById(R.id.saveBt);

                resultPoint = null;
                resultView.setVisibility(View.GONE);
                finger.setVisibility(View.GONE);
                finger2.setVisibility(View.GONE);
                finger3.setVisibility(View.GONE);
                finger4.setVisibility(View.GONE);
                finger5.setVisibility(View.GONE);
                cptBt.setVisibility(View.VISIBLE);
                saveBt.setVisibility(View.GONE);
                startCamera();
                return;

            }
         /*   Toast.makeText(getApplicationContext(), ""+ tempback2.getWidth() + "  " + tempback2.getHeight(), Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(), ""+ imgback2.getWidth() + "  " + imgback2.getHeight(), Toast.LENGTH_LONG).show();
*/
            ImageView finger = (ImageView) findViewById(R.id.finger);
            ImageView finger2 = (ImageView) findViewById(R.id.finger2);
            ImageView finger3 = (ImageView) findViewById(R.id.finger3);
            ImageView finger4 = (ImageView) findViewById(R.id.finger4);
            ImageView finger5 = (ImageView) findViewById(R.id.finger5);

            if (resultPoint.length == 10) {

       /*         if (resultPoint[10] > 0 && resultPoint[10] < 200) {
                    finger.setScaleX((float) (0.2));
                    finger.setScaleY((float) (0.2));

                    finger2.setScaleX((float) (0.2));
                    finger2.setScaleY((float) (0.2));

                    finger3.setScaleX((float) (0.2));
                    finger3.setScaleY((float) (0.2));

                    finger4.setScaleX((float) (0.2));
                    finger4.setScaleY((float) (0.2));

                    finger5.setScaleX((float) (0.2));
                    finger5.setScaleY((float) (0.2));
                } else if (resultPoint[10] >= 200 && resultPoint[10] < 280) {
                    finger.setScaleX((float) (0.2));
                    finger.setScaleY((float) (0.2));

                    finger2.setScaleX((float) (0.2));
                    finger2.setScaleY((float) (0.2));

                    finger3.setScaleX((float) (0.2));
                    finger3.setScaleY((float) (0.2));

                    finger4.setScaleX((float) (0.2));
                    finger4.setScaleY((float) (0.2));

                    finger5.setScaleX((float) (0.2));
                    finger5.setScaleY((float) (0.2));
                } else if (resultPoint[10] >= 280 && resultPoint[10] < 350) {
                    finger.setScaleX((float) (0.2));
                    finger.setScaleY((float) (0.2));

                    finger2.setScaleX((float) (0.2));
                    finger2.setScaleY((float) (0.2));

                    finger3.setScaleX((float) (0.2));
                    finger3.setScaleY((float) (0.2));

                    finger4.setScaleX((float) (0.2));
                    finger4.setScaleY((float) (0.2));

                    finger5.setScaleX((float) (0.2));
                    finger5.setScaleY((float) (0.2));
                }
                else {
                    finger.setScaleX((float) (0.2));
                    finger.setScaleY((float) (0.2));

                    finger2.setScaleX((float) (0.2));
                    finger2.setScaleY((float) (0.2));

                    finger3.setScaleX((float) (0.2));
                    finger3.setScaleY((float) (0.2));

                    finger4.setScaleX((float) (0.2));
                    finger4.setScaleY((float) (0.2));

                    finger5.setScaleX((float) (0.2));
                    finger5.setScaleY((float) (0.2));
                }*/

                finger5.setRotation(340);
                finger2.setRotation(10);
                finger.setRotation(35);

                Bitmap bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_output, bitmapOutput);


                imgback2.setImageBitmap(bitmapOutput);

                double x = imgback2.getWidth();
                double y = imgback2.getHeight();

                double scaleX = bitmapOutput.getWidth();
                double scaleY = bitmapOutput.getHeight();
                double Xscale = x/scaleX;
                double Yscale = y/scaleY;

              /*  if(resultPoint[0] >  resultPoint[8]) {
                    finger.setX((float) (resultPoint[2] * Xscale));
                    finger2.setX((float) (resultPoint[4] * Xscale));
                    finger3.setX((float) (resultPoint[6] * Xscale));
                    finger4.setX((float) (resultPoint[8] * Xscale));
                    finger5.setX((float) (resultPoint[0] * Xscale));

                    finger.setY((float) (resultPoint[3] * Yscale));
                    finger2.setY((float) (resultPoint[5] * Yscale));
                    finger3.setY((float) (resultPoint[7] * Yscale));
                    finger4.setY((float) (resultPoint[9] * Yscale));
                    finger5.setY((float) (resultPoint[1] * Yscale));
                }

                else{

                    finger.setX((float) (resultPoint[0] * Xscale));
                    finger2.setX((float) (resultPoint[2] * Xscale) );
                    finger3.setX((float) (resultPoint[4] * Xscale) );
                    finger4.setX((float) (resultPoint[6] * Xscale) );
                    finger5.setX((float) (resultPoint[8] * Xscale) );

                    finger.setY((float) (resultPoint[1] * Yscale));
                    finger2.setY((float) (resultPoint[3] * Yscale) );
                    finger3.setY((float) (resultPoint[5] * Yscale) );
                    finger4.setY((float) (resultPoint[7] * Yscale) );
                    finger5.setY((float) (resultPoint[9] * Yscale) );
                }*/

                finger.setX((float) (resultPoint[8] * Xscale ) - 45 );
                finger2.setX((float) (resultPoint[6] * Xscale) - 25);
                finger3.setX((float) (resultPoint[4] * Xscale)- 20);
                finger4.setX((float) (resultPoint[2] * Xscale) - 20);
                finger5.setX((float) (resultPoint[0] * Xscale) );

                finger.setY((float) (resultPoint[9] * Yscale) - 32);
                finger2.setY((float) (resultPoint[7] * Yscale) - 25);
                finger3.setY((float) (resultPoint[5] * Yscale)- 20 );
                finger4.setY((float) (resultPoint[3] * Yscale) - 20);
                finger5.setY((float) (resultPoint[1] * Yscale) );

                imgback2.setVisibility(View.VISIBLE);

                finger.setVisibility(View.VISIBLE);
                finger2.setVisibility(View.VISIBLE);
                finger3.setVisibility(View.VISIBLE);
                finger4.setVisibility(View.VISIBLE);
                finger5.setVisibility(View.VISIBLE);




            }

            else {

                Bitmap bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_output, bitmapOutput);
                imgback2.setImageBitmap(bitmapOutput);
                imgback2.setVisibility(View.VISIBLE);

                String Arraysize = String.valueOf(resultPoint.length);
                Toast.makeText(getApplicationContext(), Arraysize, Toast.LENGTH_SHORT).show();
            }

            startCamera();
            Log.d(TAG, "onPictureTaken - jpeg");

        }
    };

    public void saveFrame(View v){



        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(ArActivity.this);
        alert_confirm.setMessage("이미지를 저장 하시겠습니가?").setCancelable(false).setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        FrameLayout testFrame = (FrameLayout) findViewById(R.id.captureView);

                        testFrame.buildDrawingCache();

                        Bitmap screenshot = testFrame.getDrawingCache();

                        screenshot(screenshot);

                        Toast.makeText(getApplicationContext(),"저장되었습니다.", Toast.LENGTH_LONG).show();

                        ImageView imageView = (ImageView)findViewById(R.id.imageView2);
                        ImageView finger = (ImageView)findViewById(R.id.finger);
                        ImageView finger2 = (ImageView)findViewById(R.id.finger2);
                        ImageView finger3 = (ImageView)findViewById(R.id.finger3);
                        ImageView finger4 = (ImageView)findViewById(R.id.finger4);
                        ImageView finger5 = (ImageView)findViewById(R.id.finger5);

                        Button cptBt = (Button) findViewById(R.id.cmnbt);
                        cptBt.setVisibility(View.VISIBLE);
                        Button saveBt = (Button) findViewById(R.id.saveBt);
                        saveBt.setVisibility(View.GONE);
                        imageView.setVisibility(View.GONE);
                        finger.setVisibility(View.GONE);
                        finger2.setVisibility(View.GONE);
                        finger3.setVisibility(View.GONE);
                        finger4.setVisibility(View.GONE);
                        finger5.setVisibility(View.GONE);

                        // 'YES'
                    }
                }).setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 'No'
                        dialog.dismiss();

                        return;
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();

    }

    private void screenshot(Bitmap bitmap) {
        String wallpaper_url = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + "Toefing" + File.separator;

        String file_name = System.currentTimeMillis() + ".jpg";

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            final File file_path;

            try {
                file_path = new File(wallpaper_url);
                if (!file_path.isDirectory()) {
                    file_path.mkdirs();
                }

                File ImageFile = new File(file_path , file_name);


                FileOutputStream out = new FileOutputStream(ImageFile);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                out.flush();
                out.close();

                ArActivity.this.sendBroadcast(new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(ImageFile)) );


            } catch (FileNotFoundException exception) {
                Log.e("FileNotFoundException", exception.getMessage());
            } catch (IOException exception) {
                Log.e("IOException", exception.getMessage());
            }


        }

    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction();
        int id = v.getId();

        if (action == MotionEvent.ACTION_DOWN) {
            if (id == R.id.imageView2) {
                String checkmapX = String.valueOf(event.getX());
                String checkmapY = String.valueOf(event.getY());
                Toast.makeText(getApplicationContext(), "getX : " + checkmapX + "getY : " + checkmapY, Toast.LENGTH_SHORT).show();

            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (id == R.id.imageView2) {
                Log.d("TAG", "OnTouch : ACTION_UP");
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (id == R.id.imageView2) {
                Log.d("TAG", "OnTouch : ACTION_MOVE");
            }
        }
        return true;
    }

    /**
     *
     * @param activity
     * @param cameraId  Camera.CameraInfo.CAMERA_FACING_FRONT, Camera.CameraInfo.CAMERA_FACING_BACK
     * @param camera
     *
     * Camera Orientation
     * reference by https://developer.android.com/reference/android/hardware/Camera.html
     */
    public static int setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grandResults) {

        if ( requestCode == PERMISSIONS_REQUEST_CODE && grandResults.length > 0) {

            int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                    && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED ){

                //이미 퍼미션을 가지고 있음
                doRestart(this);
            }
            else{
                checkPermissions();
            }
        }

    }


    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {

        int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasWriteExternalStoragePermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
        boolean writeExternalStorageRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

/*
        if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED && writeExternalStorageRationale))
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && !cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED && !writeExternalStorageRationale))
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");

        else if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                || hasWriteExternalStoragePermission== PackageManager.PERMISSION_GRANTED ) {
            doRestart(this);
            }
*/
        }



    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //퍼미션 요청
                ActivityCompat.requestPermissions(ArActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }



    public native int[] Test(long imageinput, long imgoutput);

   /* @Override
    public void onClick(View v) {

        final ImageView finger = (ImageView)findViewById(R.id.finger);
        ImageView finger2 = (ImageView)findViewById(R.id.finger2);
        ImageView finger3 = (ImageView)findViewById(R.id.finger3);
        ImageView finger4 = (ImageView)findViewById(R.id.finger4);
        ImageView finger5 = (ImageView)findViewById(R.id.finger5);

        int idNumg = v.getId();

        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                try {

                    JSONObject jsonResponse = new JSONObject(response);

                    String TipImg = "Tip";

                    byte[] decodedString = Base64.decode(TipImg, Base64.DEFAULT);
                    Bitmap tmpbit = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    finger.setImageBitmap(tmpbit);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }*/
}