package com.wsd.wsdrobot;

import android.app.Application;
import android.util.Log;

import com.iflytek.aipsdk.authorize.Authorize;
import com.iflytek.aipsdk.authorize.IAuthorizeLoginListener;
import com.iflytek.aipsdk.common.SpeechUtility;
import com.iflytek.crashcollect.CrashCollector;
import com.iflytek.util.Logs;

/**
 * Created by hui-MacBook on 2017/8/22.
 */

public class App extends Application {
    private final String BASE_PATH = "/sdcard/test";
    //是否授权成功
    public static boolean isAuthor = false;

    @Override
    public void onCreate() {
        super.onCreate();
        //ca_path不传则走默认证书
        String params = null;
        //params = "ca_path=ca.jet,res=0";
        //params = "ca_path=/sdcard/ca.crt,res=1";
        SpeechUtility.createUtility(App.this, params);
        initCrashCollect();
        Logs.setSaveFlag(true, BASE_PATH);
        Logs.setPerfFlag(true);
        Authorize authorize = new Authorize();
        authorize.login("sn=c,appid=djrobot0,url=36.7.172.16:5105", null, new IAuthorizeLoginListener() {
            @Override
            public void onLoginResult(int code) {
                if (code == 0) {
                    Log.v("TAG", "授权成功");
                    isAuthor = true;
                } else {
                    Log.v("TAG", "授权识别");
                    isAuthor = false;
                }
            }
        });
    }

    private void initCrashCollect() {
        //日志开关
        CrashCollector.setDebugable(true);
        //初始化崩溃sdk
        CrashCollector.init(this, "55ee4f8b");
        CrashCollector.setWorkDir(BASE_PATH);
    }
}
