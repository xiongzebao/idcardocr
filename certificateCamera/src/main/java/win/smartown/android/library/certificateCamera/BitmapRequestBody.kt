package win.smartown.android.library.certificateCamera

import android.graphics.Bitmap
import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.nio.ByteBuffer

class BitmapRequestBody(
        private val bitmap: Bitmap,
        private val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): RequestBody(){

    override fun contentType(): MediaType? {
        return MediaType.parse(when (format) {
            Bitmap.CompressFormat.WEBP -> "image/webp"
            Bitmap.CompressFormat.PNG -> "image/png"
            else -> "image/jpeg"
        })
    }

    override fun writeTo(sink: BufferedSink) {
         var  b = bitmap.compress(format, 0, sink.outputStream());
  /*      val bytes: Int = bitmap.getByteCount()

        val buf: ByteBuffer = ByteBuffer.allocate(bytes)
        bitmap.copyPixelsToBuffer(buf)

        val byteArray: ByteArray = buf.array()

        sink.write(byteArray)*/
        Log.e("xiong", "writeTo:" + b);
    }

}
