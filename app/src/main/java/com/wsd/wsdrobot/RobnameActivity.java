package com.wsd.wsdrobot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;

import com.baidu.speech.VoiceRecognitionService;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.SynthesizerTool;
import com.baidu.tts.client.TtsMode;
import com.wsd.wsdrobot.nlp.Nlp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import im.delight.android.ddp.Meteor;
import im.delight.android.ddp.MeteorCallback;
import im.delight.android.ddp.db.memory.InMemoryDatabase;


public class RobnameActivity extends Activity implements MeteorCallback, RecognitionListener, SpeechSynthesizerListener, View.OnClickListener {

    private static final String TAG = "pdh";
    private static final String MTAG = "meteor";
    private Meteor mMeteor;

    private int myRecognitonStatus = enumRecognitionResult.STATUS_None.value; //语音识别结果
    // 语音识别
    private SpeechRecognizer speechRecognizer;
    //语音合成客户端
    private SpeechSynthesizer speechSynthesizer;
    private String mSampleDirPath;
    private static final String SAMPLE_DIR_NAME = "baiduTTS";
    private static final String SPEECH_FEMALE_MODEL_NAME = "bd_etts_speech_female.dat";
    private static final String SPEECH_MALE_MODEL_NAME = "bd_etts_speech_male.dat";
    private static final String TEXT_MODEL_NAME = "bd_etts_text.dat";
    private static final String LICENSE_FILE_NAME = "temp_license";
    private static final String ENGLISH_SPEECH_FEMALE_MODEL_NAME = "bd_etts_speech_female_en.dat";
    private static final String ENGLISH_SPEECH_MALE_MODEL_NAME = "bd_etts_speech_male_en.dat";
    private static final String ENGLISH_TEXT_MODEL_NAME = "bd_etts_text_en.dat";

    private static final int PRINT = 0;
    private static final int UI_CHANGE_INPUT_TEXT_SELECTION = 1;
    private static final int UI_CHANGE_SYNTHES_TEXT_SELECTION = 2;

    private EditText editText;
    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.robname);
        initMeteor();
        initSpeechRecognizer();
        initWidgetListen();
        initialEnv();
        initialTts();

    }

    private void initMeteor() {
        Meteor.setLoggingEnabled(true);
        mMeteor = new Meteor(this, config.meteorSocketUrl, new InMemoryDatabase());
        mMeteor.addCallback(this);
        mMeteor.connect();
    }

    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
        speechRecognizer.setRecognitionListener(this);
    }

    private void startVoiceRecognizer() {
        //speechRecognizer.cancel();
        Intent intent = new Intent();
        speechRecognizer.startListening(intent);
    }

    private void initWidgetListen() {

        editText = (EditText) findViewById(R.id.resText);
//
        findViewById(R.id.StartListenBtn).setOnClickListener(this);
        findViewById(R.id.speak).setOnClickListener(this);

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //开始收听
            case R.id.StartListenBtn:
//                startVoiceRecognizer();

                editText.setText(new Nlp().result(editText.getText().toString()));
                break;
            //开始说话
            case R.id.speak:
                speak("");
//                Collection c = mMeteor.getDatabase().getCollection("listenStatus");
//                Log.d(TAG, "listenStatus集合: " + c);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        speechRecognizer.destroy();
        speechSynthesizer.release();
        super.onDestroy();
    }

    /**
     * Mereor
     */
    @Override
    public void onConnect(boolean signedInAutomatically) {
        Log.d(MTAG, "onConnect: Connected");
        Log.d(MTAG, "Is logged in :" + mMeteor.isLoggedIn());
        Log.d(MTAG, "User ID: " + mMeteor.getUserId());

        String sub_listenStatus_id = mMeteor.subscribe("listenStatus", new Object[]{1});
        String sub_listenVolumes_id = mMeteor.subscribe("listenVolumes", new Object[]{1});
        String sub_btnStatus_id = mMeteor.subscribe("btnStatus", new Object[]{1});
        Log.d(MTAG, "sub: " + sub_btnStatus_id);
    }

    @Override
    public void onDisconnect() {
        System.out.println("Disconnected");
        Log.d(MTAG, "onDisconnect: Disconnected");
    }

    @Override
    public void onException(Exception e) {
        System.out.println("Exception");
        Log.d(MTAG, "onDisconnect: Exception");
        if (e != null) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDataAdded(String collectionName, String documentID, String newValuesJson) {
        Log.d(MTAG, "Data added to <" + collectionName + "> in document <" + documentID + ">");
        Log.d(MTAG, "    Added: " + newValuesJson);
    }

    @Override
    public void onDataChanged(String collectionName, String documentID, String updatedValuesJson, String removedValuesJson) {
        Log.d(MTAG, "Data changed in <" + collectionName + "> in document <" + documentID + ">");
        Log.d(MTAG, "    Updated: " + updatedValuesJson);
        Log.d(MTAG, "    Removed: " + removedValuesJson);

        /**
         * 管理web下的按钮
         */
        if(collectionName.equals("btnStatus")) {
            switch (documentID) {
                case "xLHzabvj9Dm7JL7Wf":
                    Log.d(TAG, "onDataChanged: 开始识别");
                    initSpeechRecognizer();
                    startVoiceRecognizer();
                    break;
                case "oqPKHFiYFmhd5raz4":
                    Log.d(TAG, "onDataChanged: 暂停识别");
                    speechRecognizer.destroy();
                    break;
                case "J5t4RMBwx2hRqKvXR" :
                    Log.d(TAG, "onDataChanged: 开始说话");
                    speak("");
                    break;
                case "ZnRB2NLqoA4ZkQBPQ":
                    Log.d(TAG, "onDataChanged: 暂停说话");
                    pause();
                    break;
            };

        };
    }

    @Override
    public void onDataRemoved(String collectionName, String documentID) {
        Log.d(MTAG, "Data removed from <" + collectionName + "> in document <" + documentID + ">");
    }


    /**
     * 百度语音识别
     */
    @Override
    public void onReadyForSpeech(Bundle params) {
        myRecognitonStatus = enumRecognitionResult.STATUS_Ready.value;
        Log.d(TAG, "onReadyForSpeech: 准备就绪，可以开始说话");
    }

    @Override
    public void onBeginningOfSpeech() {
        myRecognitonStatus = enumRecognitionResult.STATUS_Speaking.value;
        Log.d(TAG, "onBeginningOfSpeech: 检测到用户已经开始说话");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        System.out.println("onRmsChanged " + rmsdB);
        Log.d(TAG, "音量: " + rmsdB);
        mMeteor.call("listenVolume.updateVolume", new Object[]{"vol", rmsdB});
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        //录音文本保存
    }


    @Override
    public void onEndOfSpeech() {
        myRecognitonStatus = enumRecognitionResult.STATUS_Recognition.value;
        System.out.println("检测到用户已经停止说话");
        Log.d(TAG, "onEndOfSpeech: 检测到用户已经停止说话");
    }

    @Override
    public void onError(int error) {
        myRecognitonStatus = enumRecognitionResult.STATUS_None.value;
        StringBuilder sb = new StringBuilder();
        // 根据报错类型重启语音识别服务
        if(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH){
            speak("不好意思，没有听清楚你说什么，请再说一遍。");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: 新的识别");
                    startVoiceRecognizer();
                }
            }, 4000);
        }

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
        Log.d(TAG, "识别失败: " + sb.toString());
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
            mMeteor.call("listenStatus.updateSpeakStatus", new Object[]{corpus_no, res_type, nbest.get(0), err_no});
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 等待识别结果后再启动识别
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "run: 新的识别");
                startVoiceRecognizer();
            };
        }, 2000);

//        editText.setText(nbest.get(0));
        speak(nbest.get(0));
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
    private void initialEnv() {
        if (mSampleDirPath == null) {
            String sdcardPath = Environment.getExternalStorageDirectory().toString();
            mSampleDirPath = sdcardPath + "/" + SAMPLE_DIR_NAME;
        }
        makeDir(mSampleDirPath);
        copyFromAssetsToSdcard(false, SPEECH_FEMALE_MODEL_NAME, mSampleDirPath + "/" + SPEECH_FEMALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, SPEECH_MALE_MODEL_NAME, mSampleDirPath + "/" + SPEECH_MALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, TEXT_MODEL_NAME, mSampleDirPath + "/" + TEXT_MODEL_NAME);
        copyFromAssetsToSdcard(false, LICENSE_FILE_NAME, mSampleDirPath + "/" + LICENSE_FILE_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_SPEECH_MALE_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_SPEECH_MALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, "english/" + ENGLISH_TEXT_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_TEXT_MODEL_NAME);
    }

    private void makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 将sample工程需要的资源文件拷贝到SD卡中使用（授权文件为临时授权文件，请注册正式授权）
     *
     * @param isCover 是否覆盖已存在的目标文件
     * @param source
     * @param dest
     */
    private void copyFromAssetsToSdcard(boolean isCover, String source, String dest) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = getResources().getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initialTts() {
        this.speechSynthesizer = SpeechSynthesizer.getInstance();
        this.speechSynthesizer.setContext(this);
        this.speechSynthesizer.setSpeechSynthesizerListener(this);
        // 文本模型文件路径 (离线引擎使用)
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, mSampleDirPath + "/"
                + TEXT_MODEL_NAME);
        // 声学模型文件路径 (离线引擎使用)
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, mSampleDirPath + "/"
                + SPEECH_FEMALE_MODEL_NAME);
        // 本地授权文件路径,如未设置将使用默认路径.设置临时授权文件路径，LICENCE_FILE_NAME请替换成临时授权文件的实际路径，仅在使用临时license文件时需要进行设置，如果在[应用管理]中开通了正式离线授权，不需要设置该参数，建议将该行代码删除（离线引擎）
        // 如果合成结果出现临时授权文件将要到期的提示，说明使用了临时授权文件，请删除临时授权即可。
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_LICENCE_FILE, mSampleDirPath + "/"
                + LICENSE_FILE_NAME);
        // 请替换为语音开发者平台上注册应用得到的App ID (离线授权)
        this.speechSynthesizer.setAppId("10018798"/*这里只是为了让Demo运行使用的APPID,请替换成自己的id。*/);
        // 请替换为语音开发者平台注册应用得到的apikey和secretkey (在线授权)
        this.speechSynthesizer.setApiKey("btRQQDLbWOgkUPegmGUej119",
                "bIe96UAAi8MT6BfqQPljYPpTWZzcVWOL"/*这里只是为了让Demo正常运行使用APIKey,请替换成自己的APIKey*/);
        // 发音人（在线引擎），可用参数为0,1,2,3。。。（服务器端会动态增加，各值含义参考文档，以文档说明为准。0--普通女声，1--普通男声，2--特别男声，3--情感男声。。。）
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "3");
        // 音量0-9
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "7");
        // 设置Mix模式的合成策略
        this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 授权检测接口(只是通过AuthInfo进行检验授权是否成功。)
        // AuthInfo接口用于测试开发者是否成功申请了在线或者离线授权，如果测试授权成功了，可以删除AuthInfo部分的代码（该接口首次验证时比较耗时），不会影响正常使用（合成使用时SDK内部会自动验证授权）
        AuthInfo authInfo = this.speechSynthesizer.auth(TtsMode.MIX);

        if (authInfo.isSuccess()) {
            Log.d(TAG, "auth success");
        } else {
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            Log.d(TAG, "auth failed errorMsg=" + errorMsg);
        }

        // 初始化tts
        speechSynthesizer.initTts(TtsMode.MIX);
        // 加载离线英文资源（提供离线英文合成功能）
        int result =
                speechSynthesizer.loadEnglishModel(mSampleDirPath + "/" + ENGLISH_TEXT_MODEL_NAME, mSampleDirPath
                        + "/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        Log.d(TAG, "loadEnglishModel result=" + result);

        //打印引擎信息和model基本信息
        printEngineInfo();
    }

    private void speak(String text) {
        //text = this.editText.getText().toString();
        //需要合成的文本text的长度不能超过1024个GBK字节。
        if (TextUtils.isEmpty(text)) {
            text = "你好，请问有什么帮到你？";
            mMeteor.call("listenStatus.updateSpeakStatus", new Object[]{"robotHi", 3, text, "机器人问候语"});
        }
        int result = this.speechSynthesizer.speak(text);
        if (result < 0) {
            Log.d(TAG, "error,please look up error code in doc or URL:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    private void pause() {
        this.speechSynthesizer.pause();
    }

    /**
     * 打印引擎so库版本号及基本信息和model文件的基本信息
     */
    private void printEngineInfo() {
        Log.d(TAG, "EngineVersioin=" + SynthesizerTool.getEngineVersion());
        Log.d(TAG, "EngineInfo=" + SynthesizerTool.getEngineInfo());
        String textModelInfo = SynthesizerTool.getModelInfo(mSampleDirPath + "/" + TEXT_MODEL_NAME);
        Log.d(TAG, "textModelInfo=" + textModelInfo);
        String speechModelInfo = SynthesizerTool.getModelInfo(mSampleDirPath + "/" + SPEECH_FEMALE_MODEL_NAME);
        Log.d(TAG, "speechModelInfo=" + speechModelInfo);
    }

    @Override
    public void onSynthesizeStart(String s) {

    }

    @Override
    public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {

    }

    @Override
    public void onSynthesizeFinish(String s) {

    }

    @Override
    public void onSpeechStart(String s) {

    }

    @Override
    public void onSpeechProgressChanged(String s, int i) {

    }

    @Override
    public void onSpeechFinish(String s) {

    }

    @Override
    public void onError(String s, SpeechError speechError) {

    }
}
