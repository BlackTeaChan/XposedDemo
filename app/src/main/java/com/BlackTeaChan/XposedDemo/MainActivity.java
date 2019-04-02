package com.BlackTeaChan.XposedDemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static Boolean XPOSED_ACTIVATION = false;
    EditText et_custom_text;
    Switch switch_time_watermark;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_custom_text = findViewById(R.id.et_custom_watermark);
        switch_time_watermark = findViewById(R.id.switch_time_watermark);
        switch_time_watermark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("TEST","switch:"+ (isChecked?"true":"false"));
            }
        });
        //开启悬浮窗
        //openFloat();
        //加载模块
        loadAPP(false);
    }

    private void loadAPP(Boolean state) {
        //验证模块是否激活
        XPOSED_ACTIVATION = state;
        if(!XPOSED_ACTIVATION){
            Toast.makeText(MainActivity.this,getResources().getText(R.string.module_not_activated),Toast.LENGTH_LONG).show();
        }
    }

    public void openActivity(View v){
        Intent intent = new Intent();
        switch (v.getId()){
            case R.id.btn_goto_cm:
                break;
            case R.id.btn_goto_fn:
                intent.setClass(this,com.BlackTeaChan.XposedDemo.FlashNotice.class);
                break;
            default:break;
        }
        startActivity(intent);
    }

    public void openFloat(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(MainActivity.this, FloatService.class);
                startService(intent);
//                finish();
            } else {
                //若没有权限，提示获取.
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                Toast.makeText(MainActivity.this,"需要取得权限以使用悬浮窗",Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        } else {
            //SDK在23以下，不用管.
            Intent intent = new Intent(MainActivity.this, FloatService.class);
            startService(intent);
            finish();
        }
    }

}
