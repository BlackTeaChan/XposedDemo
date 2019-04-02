package com.BlackTeaChan.XposedDemo;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.XModuleResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by BlackTeaChan on 18-04-16.
 */
public class Hook implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    //全局Context
    private static Context applicationContext;

    private static final String TAG = "Hook";
    private static String MODULE_PATH = null;
    //开启
    private static final boolean ON = true;
    //关闭
    private static final boolean OFF = false;
    //系统状态栏时钟
    //static final int SYSTEMUI_CLOCK_ID = 2131689890;//MIUI9
    static final int SYSTEMUI_CLOCK_ID = 2131361955;//MIUI10
    //闪光通知冷却
    private static final int FLASH_CD = 3000;
    //是否可以闪光通知
    private static boolean canFlash = true;
    //黑名单
    private static String[] flash_blackList = {"com.miui.contentcatcher"};
    /**
     * 钩子
     * @param loadPackageParam
     * @throws Throwable
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        //黑名单排除
        if(!loadPackageParam.packageName.equals(flash_blackList[0])) {
            //拦截Notification
            Class clazz = loadPackageParam.classLoader.loadClass("android.app.NotificationManager");
            XposedHelpers.findAndHookMethod(clazz, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String packageName = loadPackageParam.packageName;
                    XposedBridge.log("当前包：" + packageName);
                    XposedBridge.log("id:" + param.args[1]);
                    Notification a = (Notification) param.args[2];
                    XposedBridge.log("param:" + param.toString());
                    XposedBridge.log("tickerText:" + a.tickerText);
                    XposedBridge.log("title:" + a.extras.get("android.title"));
                    XposedBridge.log("text:" + a.extras.get("android.text"));
                    if (false) {
                        final int frequency = 3;
                        final long startTime = 35;
                        final long endTime = 70;
                        if (canFlash) {
                            XposedBridge.log("闪光通知");
                            setCantFlash();
                            doFlash(frequency, startTime, endTime);
                        }
                        setCanFlash();
                    }
                }
            });
        }
        //进入MIUI相机
        if (loadPackageParam.packageName.equals("com.android.camera")) {
            XposedBridge.log("进入相机");
            //开启双摄水印开关
            Class class1 = loadPackageParam.classLoader.loadClass("com.android.camera.hardware.CameraHardwareProxy");
            XposedHelpers.findAndHookMethod(class1, "setDualCameraWatermark", Camera.Parameters.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("开启双摄水印");
                    param.args[1] = "on";
                }
            });
            Class class2 = loadPackageParam.classLoader.loadClass("com.android.camera.Device");
            //设置设备支持双摄水印
            XposedHelpers.findAndHookMethod(class2, "supportPictureWaterMark", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("设置支持图片水印");
                    param.setResult(true);
                }
            });
            XposedHelpers.findAndHookMethod(class2, "isEffectWatermarkFilted", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("设置支持图片水印(isEffectWatermarkFilted)");
                    param.setResult(true);
                }
            });
            //设置支持图片水印
            XposedHelpers.findAndHookMethod(class2, "pictureWatermarkDefaultValue", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            Class class3 = loadPackageParam.classLoader.loadClass("com.android.gallery3d.ui.StringTexture");
            //修改时间水印
            XposedHelpers.findAndHookMethod(class3, "newInstance", String.class, TextPaint.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("param String："+param.args[0]);
                    XposedBridge.log("param TextPaint："+param.args[1]);
                    XposedBridge.log("param int："+param.args[2]);
//                    XposedBridge.log("param float："+param.args[3]);
//                    XposedBridge.log("param boolean："+param.args[4]);
//                    XposedBridge.log("param int："+param.args[5]);
                    param.args[0] = "米粉群339467051";
                }
            });
            Class class8 = loadPackageParam.classLoader.loadClass("com.android.camera.module.CameraModule");
            XposedHelpers.findAndHookMethod(class8, "writeOrientationToExif", byte[].class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("writeOrientationToExif - param.args[1]:" + param.args[1].toString());
                    Bitmap bitmap = Bytes2Bitmap((byte[]) param.args[0]);
                    saveImage(bitmap, Environment.getExternalStorageState());
                }
            });
            Class class7 = loadPackageParam.classLoader.loadClass("com.android.camera.storage.ImageSaver");
//            Class classPictureInfo = loadPackageParam.classLoader.loadClass("com.android.camera.PictureInfo");
            Class classPictureInfo = XposedHelpers.findClass("com.android.camera.PictureInfo",loadPackageParam.classLoader);
            Class classExifInterface = XposedHelpers.findClass("com.android.gallery3d.exif.ExifInterface",loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(class7, "addImage",
                int.class,
                byte[].class,
                String.class,
                String.class,
                long.class,
                Uri.class,
                Location.class,
                int.class,
                int.class,
                classExifInterface,
                int.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                String.class,
                classPictureInfo,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if(OFF) {
                            XposedBridge.log("进入addImage()");
                            //传进来的图片byte[]
                            byte[] bytes = (byte[]) param.args[1];
                            ExifInterface exif = (ExifInterface) param.args[9];
                            XposedBridge.log("参数-旋转角度：" + param.args[9]);
                            XposedBridge.log("参数-旋转角度：" + exif.TAG_MODEL);
                            long startTime = System.currentTimeMillis();
                            //byte[]转bitmap
                            Bitmap bitmap = Bytes2Bitmap((byte[]) param.args[1]);
                            Log.d(TAG, "MIUICamera - Byte[] to Bitmap：" + (System.currentTimeMillis() - startTime));

                            startTime = System.currentTimeMillis();
                            //添加水印
                            bitmap = drawTextToBitmap(bitmap, "米粉吹X群339467051", 0, 540, 150, 0);
                            Log.d(TAG, "MIUICamera - draw text to Bitmap：" + (System.currentTimeMillis() - startTime));

                            startTime = System.currentTimeMillis();
                            //回调
                            param.args[1] = Bitmap2Bytes(bitmap);
                            Log.d(TAG, "MIUICamera - Bitmap to Byte[]：" + (System.currentTimeMillis() - startTime));
                        }
                    }
                });
            Class class4 = loadPackageParam.classLoader.loadClass("com.android.camera.storage.Storage");
            XposedHelpers.findAndHookMethod(class4, "writeJpeg", String.class, byte[].class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = applicationContext;
                    if(OFF) {
                        Log.d(TAG,"照片保存到：" + param.args[0].toString());
                        long startTime = System.currentTimeMillis();
                        //bytes转bitmap
                        Bitmap bitmap = Bytes2Bitmap((byte[]) param.args[1]);
                        Log.d(TAG,"Step1：" + (System.currentTimeMillis() - startTime));

                        startTime = System.currentTimeMillis();
                        //添加水印
                        bitmap = drawTextToBitmap(bitmap, "米粉吹X群339467051", 0, 540, 150, 0);
                        Log.d(TAG,"Step2：" + (System.currentTimeMillis() - startTime));

                        startTime = System.currentTimeMillis();
                        //回调
                        if (bitmap != null) {
//                            saveImage(bitmap, param.args[0].toString());
                            param.args[1] = Bitmap2Bytes(bitmap);
//                            param.args[1] = getBytesByBitmap(bitmap);
                        }
                        Log.d(TAG,"Step3：" + (System.currentTimeMillis() - startTime));

                    }
                }
            });
            Class class5 = loadPackageParam.classLoader.loadClass("com.android.camera.Util");
            XposedHelpers.findAndHookMethod(class5, "getTimeWatermark", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("时间水印："+param.getResult());
                    param.setResult("339467051");
                    XposedBridge.log("修改时间水印");
                }
            });
            Class class6 = loadPackageParam.classLoader.loadClass("com.android.camera.hardware.CameraHardwareProxy");
            XposedHelpers.findAndHookMethod(class6, "getTimeWatermark", android.hardware.Camera.Parameters.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("xiaomi-time-watermark："+param.getResult());
                }
            });
        }

        //激活模块
        if (loadPackageParam.packageName.equals("com.BlackTeaChan.XposedDemo")) {
            Class com_BlackTeaChan_XposedDemo = loadPackageParam.classLoader.loadClass("com.BlackTeaChan.XposedDemo.MainActivity");
            XposedHelpers.findAndHookMethod(com_BlackTeaChan_XposedDemo, "loadAPP", Boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });
        }

        //状态栏时间设置
        if(loadPackageParam.packageName.equals("com.android.systemui")){
            XposedBridge.log("钩住SystemUI");
            Class com_android_systemui = loadPackageParam.classLoader.loadClass("com.android.systemui.statusbar.policy.Clock");
            XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("获取SystemUI:" + param.args[0].toString());
//                    param.args[0] = "TestText" + param.args[0].toString();
                }
            });
            XposedHelpers.findAndHookMethod("com.android.systemui.statusbar.policy.Clock", loadPackageParam.classLoader, "updateClock", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    TextView tv = (TextView) param.thisObject;
                    String text = tv.getText().toString();
                    XposedBridge.log("TextView id=>"+tv.getId()+",text=>"+tv.getText().toString());
                    if(tv.getId()==SYSTEMUI_CLOCK_ID){
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM日dd日");
                        String str_date = sdf.format(date).toString();
                        tv.setText(str_date + "\n" + text);
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,7);
                        tv.setSingleLine(false);
                        tv.setGravity(Gravity.CENTER);

                        XposedBridge.log("修改状态栏时间=>"+tv.getText().toString());
                    }
                }
            });
        }

        try  {
            Class<?>  ContextClass  =  XposedHelpers.findClass("android.content.ContextWrapper",  loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(ContextClass,  "getApplicationContext",  new  XC_MethodHook()  {
                @Override
                protected  void  afterHookedMethod(MethodHookParam  param)  throws  Throwable  {
                    super.afterHookedMethod(param);
                    if  (applicationContext  !=  null)
                        return;
                    applicationContext  =  (Context)  param.getResult();
                    XposedBridge.log("Get Context success");
                }
            });
        }  catch  (Throwable e)  {
            XposedBridge.log("Get Context error:" + e.getMessage());
        }

    }

    /**
     * 资源钩子
     * @param resourcesParam
     * @throws Throwable
     */
    @Override
    public void handleInitPackageResources(final XC_InitPackageResources.InitPackageResourcesParam resourcesParam) throws Throwable {
        //生成资源实例
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resourcesParam.res);
        if(resourcesParam.packageName.equals("com.BlackTeaChan.XposedDemo")){
            XposedBridge.log("开始替换布局");
            resourcesParam.res.setReplacement("com.BlackTeaChan.XposedDemo", "layout", "flash_notice", modRes.fwd(R.layout.activity_main_test));
        }
        if (resourcesParam.packageName.equals("com.android.systemui")) {
            resourcesParam.res.hookLayout("com.android.systemui", "layout", "status_bar", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    XposedBridge.log("进入status_bar，调用方法："+liparam.resNames.id);
                    FrameLayout simpBar = (FrameLayout) liparam.view;
                    if (simpBar != null && false) {
                        LinearLayout linearLayout = (LinearLayout) liparam.view.findViewById(liparam.res.getIdentifier("status_bar_contents","id","com.android.systemui"));
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(linearLayout.getLayoutParams());
                        layoutParams.gravity = Gravity.CENTER;
                        linearLayout.setLayoutParams(layoutParams);
                        linearLayout.setGravity(Gravity.CENTER);

                        FrameLayout frameLayout = (FrameLayout) linearLayout.getChildAt(0);
                        layoutParams = new FrameLayout.LayoutParams(frameLayout.getLayoutParams());
                        layoutParams.gravity = Gravity.CENTER;
                        layoutParams.width = 100;
//                        linearLayout.setBackgroundColor(Color.RED);

                        TextView clock = (TextView) liparam.view.findViewById(liparam.res.getIdentifier("clock","id","com.android.systemui"));
                        clock.setGravity(Gravity.CENTER);
//                        clock.setBackgroundColor(Color.RED);

                    }
                }
            });
        }

        //测试资源钩子
        if(resourcesParam.packageName.equals("com.BlackTeaChan.XposedDemo")){
            resourcesParam.res.hookLayout("com.BlackTeaChan.XposedDemo", "layout", "activity_main", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
                    TextView tv = (TextView)layoutInflatedParam.view.findViewById(layoutInflatedParam.res.getIdentifier("tv_active","id","com.BlackTeaChan.XposedDemo"));
                    tv.setText("Resource hook is available-0.0.43");
                    tv.setTextColor(Color.GREEN);
                }
            });
        }
    }

    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    /**
     * bytes转图片文件
     * @param bytes
     */
    private void bytesToImageFile(byte[] bytes, String fileName) {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void bytesToImageFile(byte[] bytes){
        String date = new SimpleDateFormat("TEST-yyyy-MM-dd-HH:mm:ss").format(new Date());
        bytesToImageFile(bytes,date);
    }

    /**
     * bytes转bitmap
     * @param b
     * @return
     */
    Bitmap Bytes2Bitmap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    /**
     * bitmap转bytes
     * @param bm
     * @return
     */
    byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * bitmap转bytes
     * @param bitmap
     * @return
     */
    public byte[] getBytesByBitmap(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();
        ByteBuffer buf = ByteBuffer.allocate(bytes);
        bitmap.copyPixelsToBuffer(buf);
        byte[] byteArray = buf.array();
        return byteArray;
    }

    /**
     * 绘制文本到bitmap上
     * @param bitmap 指定的图
     * @param mText 文本
     * @param mX x轴坐标
     * @param mY y轴坐标
     * @param mTextSize 文本大小
     * @param angle 角度
     * @return
     */
    public Bitmap drawTextToBitmap(Bitmap bitmap, String mText, int mX, int mY, int mTextSize, float angle) {
        try {
            android.graphics.Bitmap.Config bitmapConfig =   bitmap.getConfig();
            // set default bitmap config if none
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }
            // resource bitmaps are imutable,
            // so we need to convert it to mutable one
            bitmap = bitmap.copy(bitmapConfig, true);

            Matrix matrix = new Matrix();
            matrix.setRotate(angle);
            //旋转
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

            Canvas canvas = new Canvas(bitmap);
            // new antialised Paint
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            // text color - #3D3D3D
            paint.setColor(Color.rgb(110,110, 110));
            // text size in pixels
            paint.setTextSize(mTextSize);
            // text shadow
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

            // draw text to the Canvas center
            Rect bounds = new Rect();
            paint.getTextBounds(mText, 0, mText.length(), bounds);
            int x = (bitmap.getWidth() - bounds.width())/6;
            int y = (bitmap.getHeight() + bounds.height())/5;

            canvas.drawText(mText, mX, mY, paint);

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 保存bitmap到本地
     * @param mBitmap
     * @return
     */
    public void saveImage(Bitmap mBitmap, String mImagePath) {
        long time = System.currentTimeMillis();
        File appDir = new File(Environment.getExternalStorageDirectory(),"DCIM/Camera");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        File file = new File(appDir, Math.random()*5+"-chc.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "保存到本地耗时：" + (System.currentTimeMillis() - time));
    }

    /**
     * 设置不可闪光通知
     */
    public void setCantFlash(){
        canFlash = false;
    }
    /**
     * 设置可以闪光
     */
    public void setCanFlash(){
        new Thread(){
            public void run(){
                try{
                    Thread.sleep(FLASH_CD);
                    canFlash = true;
                }catch(Exception e){
                }
            }
        }.start();
    }
    /**
     * 开启闪光灯频闪
     * @param frequency 次数
     * @param startTime 开启时间(毫秒)
     * @param endTime 关闭时间(毫秒)
     */
    public void doFlash(int frequency, long startTime, long endTime){
        final int p1 = frequency;
        final long p2 = startTime;
        final long p3 = endTime;
        new Thread(){
            @Override
            public void run() {
                try {
                    Camera camera;
                    camera = Camera.open();
                    camera.startPreview();
                    Camera.Parameters parameters = camera.getParameters();
                    for(int i=0;i<=p1;i++){
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(parameters);
                        Thread.sleep(p2);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        Thread.sleep(p3);
                    }
                    camera.release();
                    camera = null;
                }catch (Exception e){
                    XposedBridge.log("闪光灯出错："+e.getMessage());
                }
            }
        }.start();
    }
}