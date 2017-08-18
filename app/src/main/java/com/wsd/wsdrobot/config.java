package com.wsd.wsdrobot;

/**
 * Created by dengke on 2017/7/19.
 */

public class config {
    private static String ip = "192.168.0.172:3000";
    public final static String meteorSocketUrl = "ws://" + ip + "/websocket";
    public final static String webUrl = "http://" + ip + "/voiceListen";
}
