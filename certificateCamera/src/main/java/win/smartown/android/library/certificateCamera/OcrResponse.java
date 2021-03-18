package win.smartown.android.library.certificateCamera;

import java.util.List;

/**
 * @author xiongbin
 * @description:
 * @date : 2021/3/18 10:34
 */


public class OcrResponse {

    /**
     * res : [{"name":"姓名","text":"陈世家"},{"name":"民族","text":"汉"},{"name":"出生年月","text":"1989-12-5"},{"name":"身份证号码","text":"420198912050478"},{"name":"身份证地址","text":"湖北省阳新县兴国镇张家居委会张家访组90"}]
     * result : 1
     * timeTake : 0.7033
     */

    private List<ResBean> res;
    private int result;
    private double timeTake;


    public static class ResBean {
        /**
         * name : 姓名
         * text : 陈世家
         */

        private String name;
        private String text;
    }
}
