package win.smartown.android.library.certificateCamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.tensorflow.Session;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import win.smartown.android.library.certificateCamera.detection.MTCNN;
import win.smartown.android.library.certificateCamera.detection.ScanResult;

/**
 * Created by smartown on 2018/2/24 11:46.
 * <br>
 * Desc:
 * <br>
 * 拍照界面
 */
public class CameraActivityNew extends Activity implements View.OnClickListener {

    /**
     * 拍摄类型-身份证正面
     */
    public final static int TYPE_IDCARD_FRONT = 1;
    /**
     * 拍摄类型-身份证反面
     */
    public final static int TYPE_IDCARD_BACK = 2;
    /**
     * 拍摄类型-竖版营业执照
     */
    public final static int TYPE_COMPANY_PORTRAIT = 3;
    /**
     * 拍摄类型-横版营业执照
     */
    public final static int TYPE_COMPANY_LANDSCAPE = 4;

    public final static int REQUEST_CODE = 0X13;
    public final static int RESULT_CODE = 0X14;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ScanResult result = (ScanResult) msg.obj;

          //  模糊：-1，正常：0，偏上：1，偏下：2，偏左：3，偏右：4，偏小：5，偏大:6

            String tip;
            switch (result.status){
                case -1:tip="正常"   ;break;
                case 0:tip="偏上"; break;
                case 1:tip="偏下";break;
                case 2:tip="偏左";break;
                case 3:tip="偏右";break;
                case 4:tip="偏小";break;
                case 5:tip="偏大";break;
                default:tip="未知状态";break;
            }
            tv_tip.setText(tip);
            if (result.isSucess==0){
                uploadImage(result.bitmap);
              //  closeDetect();
                //threadPoolExecutor.shutdownNow();
            }

        }
    };



    public static void openCertificateCamera(Activity activity, int type) {
        Intent intent = new Intent(activity, CameraActivityNew.class);
        intent.putExtra("type", type);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * @return 结果文件路径
     */
    public static String getResult(Intent data) {
        if (data != null) {
            return data.getStringExtra("result");
        }
        return "";
    }

    private CameraPreview cameraPreview;
    private ImageView cropView;
    private View containerInnerView;
    private Bitmap temp;
    private TextView tv_tip;

    private ExecutorService threadPoolExecutor  = Executors.newSingleThreadExecutor();
    MTCNN mtcnn ;
    int type;

    boolean isUp=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mtcnn = MTCNN.create(getAssets());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera_new);
        cameraPreview = (CameraPreview) findViewById(R.id.camera_surface);
        tv_tip = findViewById(R.id.tv_tip);
        //获取屏幕最小边，设置为cameraPreview较窄的一边
        float screenMinSize = Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        //根据screenMinSize，计算出cameraPreview的较宽的一边，长宽比为标准的16:9
        float maxSize = screenMinSize / 9.0f * 16.0f;
        RelativeLayout.LayoutParams layoutParams;
        if (type == TYPE_COMPANY_PORTRAIT) {
            layoutParams = new RelativeLayout.LayoutParams((int) screenMinSize, (int) maxSize);
        } else {
            layoutParams = new RelativeLayout.LayoutParams((int) maxSize, (int) screenMinSize);
        }
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        cameraPreview.setLayoutParams(layoutParams);
        containerInnerView = findViewById(R.id.camera_crop_container_inner);
        cropView = (ImageView) findViewById(R.id.camera_crop);
        cropView.setImageResource(R.mipmap.camera_idcard_front);
        cameraPreview.setOnClickListener(this);
        cameraPreview.setOnFrameListener(new CameraPreview.onFrame() {
            @Override
            public void onBitmap(final Bitmap bitmap) {
                temp = Bitmap.createBitmap(bitmap);
                Runnable runnable  = new Runnable() {
                    @Override
                    public void run() {
                        ScanResult scanResult = new ScanResult();
                        scanResult.bitmap = temp;
                        ScanResult  result =mtcnn.detect(scanResult);
                       Message msg =  handler.obtainMessage();
                       msg.obj= result;
                       handler.sendMessage(msg);
                    }
                };
                threadPoolExecutor.execute(runnable);
            }
        });
        setScanFrameSize(400,200);
    }

    OkHttpUpUtil okHttpUpUtil = new OkHttpUpUtil();


    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ocr/";
    private void uploadImage(final Bitmap bitmap){
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!new File(dir).exists()){
                            new File(dir).mkdir();
                        }
                        final File file = new File(dir + "photo" +System.currentTimeMillis()+ ".jpg");
                        file.createNewFile();
                        Log.e("xiong",file.getAbsolutePath());
                        FileOutputStream out = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
                        out.flush();
                        out.close();
                        final String uploadurl = "http://192.168.17.58:8383/ocr";
                        okHttpUpUtil.uploadImage(uploadurl,file.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e("xiong",e.getMessage());
            e.printStackTrace();
        }

    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }


    private void closeDetect(){
        mtcnn.close();
        threadPoolExecutor.shutdownNow();
    }
    public void setScanFrameSize(int width,int height){


      /*  float screenMinSize = Math.min(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

        float height = (int) (screenMinSize * 0.75);//75
        float width = (int) (height * 60.0f / 47.0f);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams((int) width, ViewGroup.LayoutParams.MATCH_PARENT);*/
        LinearLayout.LayoutParams cropParams = new LinearLayout.LayoutParams( dip2px(this,width),dip2px(this,height)  );
      //  containerInnerView.setLayoutParams(containerParams);
        cropView.setLayoutParams(cropParams);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.camera_surface) {
            cameraPreview.focus();
        } else if (id == R.id.camera_close) {
            finish();
        } else if (id == R.id.camera_take) {
            //takePhoto();
        } else if (id == R.id.camera_flash) {
            boolean isFlashOn = cameraPreview.switchFlashLight();
           // flashImageView.setImageResource(isFlashOn ? R.mipmap.camera_flash_on : R.mipmap.camera_flash_off);
        } else if (id == R.id.camera_result_ok) {
            goBack();
        } else if (id == R.id.camera_result_cancel) {
          //  optionView.setVisibility(View.VISIBLE);
            cameraPreview.setEnabled(true);
          //  resultView.setVisibility(View.GONE);
            cameraPreview.startPreview();
        }
    }






   /* private void takePhoto() {
      //  optionView.setVisibility(View.GONE);
        cameraPreview.setEnabled(false);

        cameraPreview.takePhoto(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {
                camera.stopPreview();
                //子线程处理图片，防止ANR
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            File originalFile = getOriginalFile();
                            FileOutputStream originalFileOutputStream = new FileOutputStream(originalFile);
                            originalFileOutputStream.write(data);
                            originalFileOutputStream.close();

                            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getPath());

                            //计算裁剪位置
                            float left, top, right, bottom;
                            if (type == TYPE_COMPANY_PORTRAIT) {
                                left = (float) cropView.getLeft() / (float) cameraPreview.getWidth();
                                top = ((float) containerView.getTop() - (float) cameraPreview.getTop()) / (float) cameraPreview.getHeight();
                                right = (float) cropView.getRight() / (float) cameraPreview.getWidth();
                                bottom = (float) containerView.getBottom() / (float) cameraPreview.getHeight();
                            } else {
                                left = ((float) containerView.getLeft() - (float) cameraPreview.getLeft()) / (float) cameraPreview.getWidth();
                                top = (float) cropView.getTop() / (float) cameraPreview.getHeight();
                                right = (float) containerView.getRight() / (float) cameraPreview.getWidth();
                                bottom = (float) cropView.getBottom() / (float) cameraPreview.getHeight();
                            }
                            //裁剪及保存到文件
                            Bitmap cropBitmap = Bitmap.createBitmap(bitmap,
                                    (int) (left * (float) bitmap.getWidth()),
                                    (int) (top * (float) bitmap.getHeight()),
                                    (int) ((right - left) * (float) bitmap.getWidth()),
                                    (int) ((bottom - top) * (float) bitmap.getHeight()));

                            final File cropFile = getCropFile();
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cropFile));
                            cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            bos.flush();
                            bos.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                  //  resultView.setVisibility(View.VISIBLE);
                                }
                            });
                            return;
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               // optionView.setVisibility(View.VISIBLE);
                                cameraPreview.setEnabled(true);
                            }
                        });
                    }
                }).start();

            }
        });
    }*/

    /**
     * @return 拍摄图片原始文件
     */
    private File getOriginalFile() {
        switch (type) {
            case TYPE_IDCARD_FRONT:
                return new File(getExternalCacheDir(), "idCardFront.jpg");
            case TYPE_IDCARD_BACK:
                return new File(getExternalCacheDir(), "idCardBack.jpg");
            case TYPE_COMPANY_PORTRAIT:
            case TYPE_COMPANY_LANDSCAPE:
                return new File(getExternalCacheDir(), "companyInfo.jpg");
        }
        return new File(getExternalCacheDir(), "picture.jpg");
    }

    /**
     * @return 拍摄图片裁剪文件
     */
    private File getCropFile() {
        switch (type) {
            case TYPE_IDCARD_FRONT:
                return new File(getExternalCacheDir(), "idCardFrontCrop.jpg");
            case TYPE_IDCARD_BACK:
                return new File(getExternalCacheDir(), "idCardBackCrop.jpg");
            case TYPE_COMPANY_PORTRAIT:
            case TYPE_COMPANY_LANDSCAPE:
                return new File(getExternalCacheDir(), "companyInfoCrop.jpg");
        }
        return new File(getExternalCacheDir(), "pictureCrop.jpg");
    }

    /**
     * 点击对勾，使用拍照结果，返回对应图片路径
     */
    private void goBack() {
        Intent intent = new Intent();
        intent.putExtra("result", getCropFile().getPath());
        setResult(RESULT_CODE, intent);
        finish();
    }

}
