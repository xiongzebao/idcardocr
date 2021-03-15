/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package win.smartown.android.library.certificateCamera.detection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;

import androidx.core.util.Pair;



import org.tensorflow.Graph;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MTCNN {
    private static final String MODEL_FILE = "file:///android_asset/mtcnn.pb";
    // Only return this many results.
    private static final int MAX_RESULTS = 100;
    private static final int BYTE_SIZE_OF_FLOAT = 4;

    // Config values.
    private String inputName;

    // Pre-allocated buffers.
    private FloatBuffer outputProbs;
    private FloatBuffer outputBoxes;
    private FloatBuffer outputLandmark;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     */
    public static  MTCNN create(
            final AssetManager assetManager) {
        final  MTCNN d = new  MTCNN();

        d.inferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);

        final Graph g = d.inferenceInterface.graph();

        d.inputName = "input";
        if (g.operation(d.inputName) == null)
            throw new RuntimeException("Failed to find input Node '" + d.inputName + "'");

        d.outputNames = new String[] {"prob", "landmarks", "box"};
        if (g.operation(d.outputNames[0]) == null)
            throw new RuntimeException("Failed to find output Node '" + d.outputNames[0] + "'");

        if (g.operation(d.outputNames[1]) == null)
            throw new RuntimeException("Failed to find output Node '" + d.outputNames[1] + "'");

        if (g.operation(d.outputNames[2]) == null)
            throw new RuntimeException("Failed to find output Node '" + d.outputNames[2] + "'");

        // Pre-allocate buffers.
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MAX_RESULTS * BYTE_SIZE_OF_FLOAT);
        byteBuffer.order(ByteOrder.nativeOrder());
        d.outputProbs = byteBuffer.asFloatBuffer();

        d.outputLandmark = ByteBuffer.allocateDirect(MAX_RESULTS * BYTE_SIZE_OF_FLOAT * 2 * 5)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        d.outputBoxes = ByteBuffer.allocateDirect(MAX_RESULTS * BYTE_SIZE_OF_FLOAT * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        return d;
    }

    public MTCNN() {}

    public  ScanResult detect( ScanResult result) {
       // Log.e("xiong","detect");
        Bitmap bitmap = result.bitmap;
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("detect");
        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        int w = bitmap.getWidth(), h = bitmap.getHeight();

        //Log.e("xiong","w:"+w+"h:"+h);
        int intValues[] = new int[w * h];
        float floatValues[] = new float[w * h * 3];

        bitmap.getPixels(intValues, 0, w, 0, 0, w, h);

        // BGR
        for (int i = 0; i < intValues.length; ++i) {
            int p = intValues[i];

            floatValues[i * 3 + 0] = p & 0xFF;
            floatValues[i * 3 + 1] = (p >> 8) & 0xFF;
            floatValues[i * 3 + 2] = (p >> 16) & 0xFF;
        }
        Trace.endSection(); // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");
        inferenceInterface.feed(inputName, floatValues, h, w, 3);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        inferenceInterface.run(outputNames, false);
        Trace.endSection();

        // Copy the output Tensor back into the output array.
        Trace.beginSection("fetch");
        inferenceInterface.fetch(outputNames[0], outputProbs);
        inferenceInterface.fetch(outputNames[1], outputLandmark);
        inferenceInterface.fetch(outputNames[2], outputBoxes);
        Trace.endSection();
        outputProbs.flip();
        outputLandmark.flip();
        outputBoxes.flip();

        // 成功：0，失败：1
        int isRight=0;

        // 身份证照片的状态，模糊：-1，正常：0，偏上：1，偏下：2，偏左：3，偏右：4，偏小：5，偏大;6
        int pictureStata=0;

        int len = outputProbs.remaining();
        int len2 = outputLandmark.remaining();
        Pair faces[] = new Pair[len];

        if (len2 > 9) {
            // 说明图片可以识别出人脸
            isRight = 0;
            for (int i = 0; i < len; i++) {
                float top = outputBoxes.get();
                float left = outputBoxes.get();
                float bottom = outputBoxes.get();
                float right = outputBoxes.get();

                //point [0:4]是五个关键点在水平方向上的坐标(列坐标)，point [5:9]是五个关键点在垂直方向上的坐标（行坐标）。
                float left_eye_y = outputLandmark.get();
                float right_eye_y = outputLandmark.get();
                float noise_y = outputLandmark.get();
                float left_mouse_y = outputLandmark.get();
                float right_mouse_y = outputLandmark.get();

                float left_eye_x = outputLandmark.get();
                float right_eye_x = outputLandmark.get();
                float noise_x = outputLandmark.get();
                float left_mouse_x = outputLandmark.get();
                float right_mouse_x = outputLandmark.get();

        /*        Log.e("xiong","w:"+w+"h:"+h);
                Log.e("xiong","top:"+top+"left:"+left);
                Log.e("xiong","bottom:"+bottom+"right:"+right);
                Log.e("xiong","noise_x:"+noise_x+"noise_y:"+noise_y);

                Log.e("xiong","left_eye_x:"+left_eye_x+"left_eye_y:"+left_eye_y);
                Log.e("xiong","right_eye_x:"+right_eye_x+"right_eye_y:"+right_eye_y);
                Log.e("xiong","left_mouse_x:"+left_mouse_x+"left_mouse_y:"+left_mouse_y);
                Log.e("xiong","right_mouse_x:"+right_mouse_x+"right_mouse_y:"+right_mouse_y);
*/

                float[][] landmark = {
                        {left_eye_x, left_eye_y},
                        {right_eye_x, right_eye_y},
                        {noise_x, noise_y},
                        {left_mouse_x, left_mouse_y},
                        {right_mouse_x, right_mouse_y}
                };

                // pictureStata 的状态判别
                // 身份证照片的状态，模糊：-1，正常：0，偏上：1，偏下：2，偏左：3，偏右：4，偏小：5，偏大;6
                if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 0;
                } else if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 1;
                } else if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 2;
                } else if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 3;
                } else if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 4;
                } else if (noise_x > w/2 && noise_y > h/2) {
                    pictureStata = 5;
                } else {
                    pictureStata = 6;
                }

                float prob = outputProbs.get();
                faces[i] = new Pair<>(
                        new RectF(left, top, right, bottom), landmark);
            }

        }
        else{
            // 说明图片无法识别出人脸
             isRight = 1;
             pictureStata = -1;
            };

        if (outputBoxes.hasRemaining())
            outputBoxes.position(outputBoxes.limit());

        if (outputLandmark.hasRemaining())
            outputLandmark.position(outputLandmark.limit());

        outputProbs.compact();
        outputLandmark.compact();
        outputBoxes.compact();
        Trace.endSection(); // "detect"

        result.isSucess = isRight;
        result.status = pictureStata;
        Log.e("xiong","isSuccess:"+result.isSucess+"status:"+result.status);

       // System.gc();
        return result;
    }

    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    public void close() {
        inferenceInterface.close();
    }
}
