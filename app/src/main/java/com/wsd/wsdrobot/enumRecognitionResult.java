package com.wsd.wsdrobot;

/**
 * Created by dengke on 2017/7/21.
 */

public enum enumRecognitionResult {
    STATUS_None(0),STATUS_Ready(1),STATUS_Speaking(2),STATUS_Recognition(3),STATUS_Successful(4),STATUS_RecognitionFailed(5);
    public int value;
    private enumRecognitionResult(int value){
      this.value = value;
    };
}
