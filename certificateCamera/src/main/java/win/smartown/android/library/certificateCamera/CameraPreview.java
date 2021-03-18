package win.smartown.android.library.certificateCamera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.util.List;

import win.smartown.android.library.certificateCamera.detection.MTCNN;

/**
 * Created by smartown on 2018/2/24 11:46.
 * <br>
 * Desc:
 * <br>
 * 相机预览封装
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static String TAG = CameraPreview.class.getName();

    private Camera camera;

    public CameraPreview(Context context) {
        super(context);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = CameraUtils.openCamera();
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                Camera.Parameters parameters = camera.getParameters();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //竖屏拍照时，需要设置旋转90度，否者看到的相机预览方向和界面方向不相同
                    camera.setDisplayOrientation(90);
                    parameters.setRotation(90);
                } else {
                    camera.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                Camera.Size bestSize = getBestSize(parameters.getSupportedPreviewSizes());
                if (bestSize != null) {
                    parameters.setPreviewSize(bestSize.width, bestSize.height);
                    parameters.setPictureSize(bestSize.width, bestSize.height);
                } else {
                    parameters.setPreviewSize(800, 400);
                    parameters.setPictureSize(800, 400);
                }
                camera.setParameters(parameters);
              //  camera.startPreview();
                startPreview(previewCallback);
                focus();
            } catch (Exception e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                try {
                    Camera.Parameters parameters = camera.getParameters();
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        //竖屏拍照时，需要设置旋转90度，否者看到的相机预览方向和界面方向不相同
                        camera.setDisplayOrientation(90);
                        parameters.setRotation(90);
                    } else {
                        camera.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera.setParameters(parameters);
                  //  camera.startPreview();
                    startPreview(previewCallback);
                    focus();
                } catch (Exception e1) {
                    e.printStackTrace();
                    camera = null;
                }
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //因为设置了固定屏幕方向，所以在实际使用中不会触发这个方法
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        //回收释放资源
        release();
    }

    /**
     * Android相机的预览尺寸都是4:3或者16:9，这里遍历所有支持的预览尺寸，得到16:9的最大尺寸，保证成像清晰度
     *
     * @param sizes
     * @return 最佳尺寸
     */
    private Camera.Size getBestSize(List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        for (Camera.Size size : sizes) {
            Log.e("xiong","w:"+size.width+"H:"+size.height);
            if ((float) size.width / (float) size.height == 16.0f / 9.0f) {
                if (bestSize == null) {
                    bestSize = size;
                } else {
                    if (size.width > bestSize.width) {
                        bestSize = size;
                    }
                }
            }
        }
        return bestSize;
    }

    /**
     * 释放资源
     */
    private void release() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.setPreviewCallbackWithBuffer(null);
            camera.release();
            camera = null;
        }
    }

    /**
     * 对焦，在CameraActivity中触摸对焦
     */
    public void focus() {
        if (camera != null) {
            camera.autoFocus(null);
        }
    }

    /**
     * 开关闪光灯
     *
     * @return 闪光灯是否开启
     */
    public boolean switchFlashLight() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                return true;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);

                return false;
            }
        }
        return false;
    }

    /**
     * 拍摄照片
     *
     * @param pictureCallback 在pictureCallback处理拍照回调
     */
    public void takePhoto(Camera.PictureCallback pictureCallback) {
        if (camera != null) {
            camera.takePicture(null, null, pictureCallback);
        }
    }

    public void startPreview() {
        if (camera != null) startPreview(previewCallback);
    }


    public static interface onFrame{
        void onBitmap(Bitmap bitmap);

    }
    onFrame onFrame ;


    long lastTime=0;
    long currentTime=0;

    public void setOnFrameListener(onFrame listener){
        onFrame = listener;
    }

    private class PreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {

            currentTime =  System.currentTimeMillis();
            // 在使用完 Buffer 之后记得回收复用。
         /*   if (null == data) {
                camera.addCallbackBuffer(new byte[getBufferSize()]);
            } else {
                camera.addCallbackBuffer(data);
            }
*/
            camera.addCallbackBuffer(data);
            if(currentTime-lastTime<300){
                return;
            }
            lastTime = currentTime;


            Camera.Size size = camera.getParameters().getPreviewSize();
            try{
                final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                if(image!=null){
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                    final Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                    if(onFrame!=null){
                        onFrame.onBitmap(bmp);
                    }
                    stream.close();

                  //  bmp.recycle();
                    System.gc();

                }
            }catch(Exception ex){
                Log.e("Sys","Error:"+ex.getMessage());
            }
        }
    }



     PreviewCallback previewCallback = new  PreviewCallback();


    private int  getBufferSize(){
       int height=  camera.getParameters().getPreviewSize().height;
       int width = camera.getParameters().getPreviewSize().width;
       return 3110400;
    }
    public void startPreview(Camera.PreviewCallback cb) {
        if (camera != null) {

           // byte[] buffer = new byte[getBufferSize()];
            camera.setPreviewCallback(cb);
      /*      camera.setPreviewCallbackWithBuffer(cb);
            camera.addCallbackBuffer(buffer);*/

            camera.startPreview();

            Log.d("xiong", "startPreview() called");
        }
    }





}
