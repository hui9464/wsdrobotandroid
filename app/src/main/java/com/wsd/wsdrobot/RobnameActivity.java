package com.wsd.wsdrobot;

import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import im.delight.android.ddp.ResultListener;
import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.db.memory.InMemoryDatabase;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.baidu.speech.VoiceRecognitionService;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;


public class RobnameActivity extends AppCompatActivity implements MeteorCallback, RecognitionListener, SpeechSynthesizerListener {

    private static final String TAG = "pdh";
    private Meteor mMeteor;
    private int myRecognitonStatus = enumRecognitionResult.STATUS_None.value; //语音识别结果
    // 语音识别
    private SpeechRecognizer speechRecognizer;
    //语音合成客户端
    private SpeechSynthesizer speechSynthesizer;


    private EditText editText;
    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.robname);
        initMeteor();
        initSpeechRecognizer();
        startTTS();
        initWidgetListen();
    }

    private void initMeteor(){
        Meteor.setLoggingEnabled(true);
        mMeteor = new Meteor(this,config.meteorSocketUrl,new InMemoryDatabase());
        mMeteor.addCallback(this);
        mMeteor.connect();
    }

    private void initSpeechRecognizer(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this,new ComponentName(this,VoiceRecognitionService.class));
        speechRecognizer.setRecognitionListener(this);
    }

    // 初始化语音合成客户端并启动
    private void startTTS() {
//        //获取assets路径
//        InputStream assetsFile = getClass().getClassLoader().getResourceAsStream("assets/bd_etts_speech_female.dat");
//        //getResource("/assets/bd_etts_speech_female.dat");
//        Log.d("地址", "file: " + assetsFile);

        // 获取语音合成对象实例
        speechSynthesizer = SpeechSynthesizer.getInstance();
        // 设置context
        speechSynthesizer.setContext(this);
        // 设置语音合成状态监听器
        speechSynthesizer.setSpeechSynthesizerListener(this);
        // 设置在线语音合成授权，需要填入从百度语音官网申请的api_key和secret_key
        speechSynthesizer.setApiKey("btRQQDLbWOgkUPegmGUej119", "bIe96UAAi8MT6BfqQPljYPpTWZzcVWOL");
        // 设置离线语音合成授权，需要填入从百度语音官网申请的app_id
        speechSynthesizer.setAppId("10018798");
        // 设置语音合成文本模型文件
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, "file:///android_asset/bd_etts_text.dat");
        // 设置语音合成声音模型文件
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, "file:///android_asset/bd_etts_speech_female.dat");
        // 设置语音合成声音授权文件
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_LICENCE_FILE, "file:///android_asset/temp_license");
        // 获取语音合成授权信息
        AuthInfo authInfo = speechSynthesizer.auth(TtsMode.MIX);
        // 判断授权信息是否正确，如果正确则初始化语音合成器并开始语音合成，如果失败则做错误处理
        if (authInfo.isSuccess()) {
            speechSynthesizer.initTts(TtsMode.MIX);
            speechSynthesizer.speak("百度语音合成示例程序正在运行");
        } else {
            // 授权失败
            Log.d(TAG, "startTTS: 授权失败");
        }

    }

    private void startVoiceRecognizer(){
        speechRecognizer.cancel();
        Intent intent = new Intent();
        speechRecognizer.startListening(intent);
    }

    private  void initWidgetListen(){

        editText = (EditText) findViewById(R.id.resText);

        Button startListenBtn = (Button) findViewById(R.id.StartListenBtn);
        startListenBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                startVoiceRecognizer();
            }
        });

        Button startSpeak = (Button) findViewById(R.id.StartListenBtn);
        startSpeak.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                speak();
            }
        });

        webView = (WebView) findViewById(R.id.webView);
        //WebView加载web资源
        webView.loadUrl(config.webUrl);
//        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
//        webView.setWebViewClient(new WebViewClient(){
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                // TODO Auto-generated method stub
//                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
//                view.loadUrl(url);
//                return true;
//            }
//        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
    }

    private void speak() {
        String text = this.editText.getText().toString();
        Log.d(TAG, "speak: " + text);
        //需要合成的文本text的长度不能超过1024个GBK字节。
        if (TextUtils.isEmpty(editText.getText())) {
            editText.setText(text);
        }
        int result = this.speechSynthesizer.speak(text);
        if (result < 0) {

        }
    }

    @Override
    protected void onDestroy(){
        speechRecognizer.destroy();
        super.onDestroy();
    }


    @Override
    public void onConnect(boolean signedInAutomatically) {
        System.out.println("Connected");
        System.out.println("Is logged in :" + mMeteor.isLoggedIn());
        System.out.println("User ID: " + mMeteor.getUserId());

        String sub_listenStatus_id = mMeteor.subscribe("listenStatus");
    }

    @Override
    public void onDisconnect() {
        System.out.println("Disconnected");
    }

    @Override
    public void onException(Exception e) {
        System.out.println("Exception");
        if(e!=null){
            e.printStackTrace();
        }
    }

    @Override
    public void onDataAdded(String collectionName, String documentID, String newValuesJson) {
        System.out.println("Data added to <"+collectionName+"> in document <"+documentID+">");
        System.out.println("    Added: "+newValuesJson);
    }

    @Override
    public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {
        System.out.println("Data changed in <"+collectionName+"> in document <"+documentID+">");
        System.out.println("    Updated: "+updatedValuesJson);
        System.out.println("    Removed: "+removedValuesJson);
    }



    //语音识别相关
    @Override
    public void onDataRemoved(String collectionName, String documentID) {
        System.out.println("Data removed from <"+collectionName+"> in document <"+documentID+">");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        myRecognitonStatus = enumRecognitionResult.STATUS_Ready.value;
        System.out.println("准备就绪，可以开始说话");
    }

    @Override
    public void onBeginningOfSpeech() {
        myRecognitonStatus = enumRecognitionResult.STATUS_Speaking.value;
        System.out.println("检测到用户已经开始说话");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        System.out.println("onRmsChanged " + rmsdB);
        Log.d(TAG, "音量: " + rmsdB);
        mMeteor.call("listenVolume.updateVolume", new Object[] {"vol", rmsdB});
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }


    @Override
    public void onEndOfSpeech() {
        myRecognitonStatus = enumRecognitionResult.STATUS_Recognition.value;
        System.out.println("检测到用户已经停止说话");
    }

    @Override
    public void onError(int error) {
        myRecognitonStatus = enumRecognitionResult.STATUS_None.value;
        StringBuilder sb = new StringBuilder();
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");

                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
        sb.append(":" + error);
        System.out.println("识别失败：" + sb.toString());
        myRecognitonStatus = enumRecognitionResult.STATUS_RecognitionFailed.value;
    }

    @Override
    public void onResults(Bundle results) {

        myRecognitonStatus = enumRecognitionResult.STATUS_Successful.value;

        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        System.out.println("识别成功：" + Arrays.toString(nbest.toArray(new String[nbest.size()])));

        String json_res = results.getString("origin_result");
        try {
            System.out.println("origin_result=\n" + new JSONObject(json_res).toString(4));
        } catch (Exception e) {
            System.out.println("origin_result=[warning: bad json]\n" + json_res);
        }


        /**
         * 解析结果
         * pdh
         */
        try {
            JSONObject jsonObj = new JSONObject(results.getString("origin_result"));
            String corpus_no = jsonObj.getJSONObject("result").getString("corpus_no");
            String err_no = jsonObj.getJSONObject("result").getString("err_no");
            String idx = jsonObj.getJSONObject("result").getString("idx");
            Integer res_type = Integer.valueOf(jsonObj.getJSONObject("result").getString("res_type"));
            String sn = jsonObj.getJSONObject("result").getString("sn");
            //ddp 调用后台方法
            //'listenStatus.updateSpeakStatus'(msgid, status, sayMessage, errorMsg)
            mMeteor.call("listenStatus.updateSpeakStatus", new Object[] {corpus_no, res_type, nbest.get(0), err_no});
        } catch (JSONException e) {
            e.printStackTrace();
        }

        editText.setText(nbest.get(0));
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            System.out.println("~临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    /**
     * 百度语音合成
     */
    public void onError(String arg0, SpeechError arg1) {
        // 监听到出错，在此添加相关操作
    }

    public void onSpeechFinish(String arg0) {
        // 监听到播放结束，在此添加相关操作
    }

    public void onSpeechProgressChanged(String arg0, int arg1) {
        // 监听到播放进度有变化，在此添加相关操作
    }

    public void onSpeechStart(String arg0) {
        // 监听到合成并播放开始，在此添加相关操作
    }

    public void onSynthesizeDataArrived(String arg0, byte[] arg1, int arg2) {
        // 监听到有合成数据到达，在此添加相关操作
    }

    public void onSynthesizeFinish(String arg0) {
        // 监听到合成结束，在此添加相关操作
    }

    public void onSynthesizeStart(String arg0) {
        // 监听到合成开始，在此添加相关操作
    }

}
