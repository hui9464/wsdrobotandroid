package com.wsd.wsdrobot.nlp;

import com.iflytek.aipsdk.nlp.INlpListener;
import com.iflytek.aipsdk.nlp.NlpHelper;

/**
 * Created by hui-MacBook on 2017/8/22.
 */

public class Nlp {
    private NlpHelper nlpHelper;

    private String params;
    private int code;
    private String nlpResult;

    private void init() {
        nlpHelper = new NlpHelper();
        params = "svc=nlp,url=36.7.172.16:5105,appid=pc20onli,username=unicom,org=currencyservice";
        params.trim();
    }

    public String result(String text) {
        init();
        if (null != nlpHelper) {
            nlpHelper.getResult(params, text, iNlpListener);
        }

        if (nlpResult != null) {
            return nlpResult;
        } else {
            return "没有结果";
        }
    }

    INlpListener iNlpListener = new INlpListener() {
        @Override
        public void onResult(final String s, final int i) {
            new Runnable() {
                @Override
                public void run() {
                    nlpResult = s;
                    code = i;
                }
            };
        }
    };


}
