package win.smartown.android.library.certificateCamera.detection;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author xiongbin
 * @description:
 * @date : 2021/3/9 8:50
 */

public class ScanResult implements Parcelable {
    public static final  int SUCCESS=1;
    public static final  int FAILED=2;
    public int status; // 身份证照片的状态，模糊：-1，正常：0，偏上：1，偏下：2，偏左：3，偏右：4，偏小：5，偏大;6
    public int isSucess; // 成功：0，失败：1

    public String message;
    public Bitmap bitmap;

    protected ScanResult(Parcel in) {
        status = in.readInt();
        isSucess = in.readInt();
        message = in.readString();
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public ScanResult(int code, String message) {
        this.status = code;
        this.message = message;
    }

    public ScanResult() {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(status);
        dest.writeInt(isSucess);
        dest.writeString(message);
        dest.writeParcelable(bitmap, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ScanResult> CREATOR = new Creator<ScanResult>() {
        @Override
        public ScanResult createFromParcel(Parcel in) {
            return new ScanResult(in);
        }

        @Override
        public ScanResult[] newArray(int size) {
            return new ScanResult[size];
        }
    };
}
