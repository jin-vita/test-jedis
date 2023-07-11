package org.techtown.jedistest;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import redis.clients.jedis.Jedis;

public class BasicInfo {
    /**
     * DEBUG 로그를 찍을 것인지 여부 확인
     */
    public static boolean isDebug = true;

    /**
     * ERROR 로그를 찍을 것인지 여부 확인
     */
    public static boolean isError = true;

    /**
     * DEBUG 로그 찍기
     *
     * @param tag 위치
     * @param msg 내용
     */
    public static void debug(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    /**
     * ERROR 로그 찍기
     *
     * @param tag 위치
     * @param msg 내용
     */
    public static void error(String tag, String msg) {
        if (isError) {
            Log.e(tag, msg);
        }
    }

    public static void error(String tag, String msg, Exception ex) {
        if (isError) {
            Log.e(tag, msg, ex);
        }
    }

    /**
     * REDIS 연결 관리
     */
    public static HashMap<String, Jedis> redisMap = new HashMap<>();

    /**
     * Show Toast message during 1 second
     *
     * @param context 위치
     * @param msg 내용
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show Toast message during 1 second
     *
     * @param context 위치
     * @param msg 내용
     */
    public static void showToast(Context context, int msg) {
        Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show();
    }

}
