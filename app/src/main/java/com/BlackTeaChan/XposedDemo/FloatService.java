package com.BlackTeaChan.XposedDemo;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static java.lang.Thread.sleep;

public class FloatService extends Service {
    //Log用的TAG
    private static final String TAG = "FloatService";
    //要引用的布局文件.
    ConstraintLayout toucherLayout;
    //布局参数.
    WindowManager.LayoutParams params;
    //实例化的WindowManager.
    WindowManager windowManager;
    //悬浮窗
    private LinearLayout float_layout;
    //悬浮窗-按钮1
    private Button float_btn1;

    //状态栏高度.（接下来会用到）
    int statusBarHeight = -1;
    //自身宽度
    int mWidth = 200;
    //自身高度
    int mHeight = 200;
    //屏幕宽度
    private static int mScreenWidth;
    //屏幕高度
    private static int mScreenHeight;
    //开关
    private static boolean isOpen = false;

    private OutputStream os;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"FloatService Created");
        createToucher();
    }

    @Override
    public void onDestroy() {
        //用imageButton检查悬浮窗还在不在，这里可以不要。优化悬浮窗时要用到。
        if (float_layout != null) {
            windowManager.removeView(toucherLayout);
        }
        super.onDestroy();
    }

    /**
     * 创建悬浮窗
     */
    private void createToucher() {
        //赋值WindowManager&LayoutParam.
        params = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){//6.0以上
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            params.type =  WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        //设置悬浮窗口长宽数据.
        //注意，这里的width和height均使用px而非dp.这里我偷了个懒
        //如果你想完全对应布局设置，需要先获取到机器的dpi
        //px与dp的换算为px = dp * (dpi / 160).

        DisplayMetrics metric = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels;  // 屏幕宽度（像素）
        mScreenHeight = metric.heightPixels;  // 屏幕高度（像素）
        float ppi = metric.density;  // 屏幕密度（0.75 / 1.0 / 1.5）
        int dpi = metric.densityDpi;  // 屏幕密度DPI（120 / 160 / 240）
        Log.d(TAG, "Base info:\nScreenWidth:" + mScreenWidth + "\nScreenHeight:" + mScreenHeight + "\nppi:" + ppi + "\ndpi:" + dpi);
        params.width = mWidth;
        params.height = mHeight;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        toucherLayout = (ConstraintLayout) inflater.inflate(R.layout.float_layout,null);
        //添加toucherlayout
        windowManager.addView(toucherLayout,params);

        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);

        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height","dimen","android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        //浮动窗口
        float_layout = toucherLayout.findViewById(R.id.float_layout);
        //按钮1
        float_btn1 = toucherLayout.findViewById(R.id.float_btn1);

        getRoot();

        //移动悬浮窗
        float_btn1.setOnTouchListener(new View.OnTouchListener() {
            boolean isMove = false;
            double lastX;
            double lastY;
            double tolerance = 10;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                double thisX = event.getX();
                double thisY = event.getY();
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "按下 - X:" + thisX + "\tY:" + thisY);
                        lastX = thisX;
                        lastY = thisY;
                        isMove = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "移动(针对屏幕) - X:" + event.getRawX() + "\tY:" + event.getRawY());
                        Log.d(TAG, "移动 - X:" + thisX + "\tY:" + thisY);
                        isMove = true;
                        params.x = (int) (event.getRawX() - lastX);
                        params.y = (int) (event.getRawY() - lastY - statusBarHeight);
                        windowManager.updateViewLayout(toucherLayout,params);
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "弹起 - ");
                        //允许限差
                        if( ((lastX - tolerance <= thisX && lastX + tolerance >= thisX)
                                &&(lastY - tolerance <= thisY && lastY + tolerance >= thisY)) || !isMove){
                            isOpen = !isOpen;
                            //autoTouch();
                            float_btn1.setText(isOpen ? "开" : "关");
                            float_btn1.setTextColor(isOpen ? 0xff000000 : 0xffdddddd);
                            Log.d(TAG, isOpen ? "开启" : "关闭");
                        }
                        isMove = false;
                        break;
                }
                return true;
            }
        });

    }

    /**
     * 自动点击屏幕
     */
    private void autoTouch(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isOpen){
                    try{
                        sleep(100);
                    } catch (Exception e){
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }).start();
    }

    /**
     * 执行ADB命令： input tap 125 340
     */
    private final void exec(String[] cmds) {
        try {
            if (os == null) {
                os = Runtime.getRuntime().exec("su").getOutputStream();
            }
            for(String cmd:cmds) {
                os.write(cmd.getBytes());
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * 执行指令
     * @param command
     */
    private void doShell(String command) {
        doShell(new String[]{command});
    }
    /**
     * 执行多条指令
     * @param commands
     * 如果已经root，但是用户选择拒绝授权,e.getMessage() = write failed: EPIPE (Broken pipe)
     * 如果没有root，,e.getMessage()= Error running exec(). Command: [su] Working Directory: null Environment: null
     */
    private boolean doShell(String[] commands) {
        Log.d("TEST","准备执行");
        try {
            Process process = Runtime.getRuntime().exec("su ");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            for(String command : commands){
                if(command != null){
                    os.writeBytes(command + "\n");
                }
            }
            os.writeBytes("exit\n");
            os.flush();
            Log.d(TAG,"执行成功："+os);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"执行出错："+e);
            return false;
        }
    }

    public void getRoot() {
        Log.d("TEST","测试ROOT权限");
        doShell("su");
    }


    /**
     * 模拟点击屏幕事件（废弃）
     * @param x X坐标
     * @param y Y坐标
     */
    /*private void touchScreen(double x, double y){
        Log.d(TAG, "Touch screen X:" + x + ",Y:" + y);
        String[] order = {"input", "tap", "" + x, "" + y};
        try {
            new ProcessBuilder(order).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/


}
