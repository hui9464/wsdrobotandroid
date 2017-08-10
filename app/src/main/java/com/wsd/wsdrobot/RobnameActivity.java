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
import android.view.View;
import android.widget.Button;

import com.baidu.speech.VoiceRecognitionService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;


public class RobnameActivity extends AppCompatActivity implements MeteorCallback, RecognitionListener {

    private Meteor mMeteor;
    private int myRecognitonStatus = enumRecognitionResult.STATUS_None.value; //语音识别结果
    private SpeechRecognizer speechRecognizer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.robname);
        initMeteor();
        initSpeechRecognizer();
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


    private void startVoiceRecognizer(){
        speechRecognizer.cancel();
        Intent intent = new Intent();
        speechRecognizer.startListening(intent);
    }

    private  void initWidgetListen(){
        Button button = (Button) findViewById(R.id.StartListenBtn);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                startVoiceRecognizer();
            }
        });
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


}
