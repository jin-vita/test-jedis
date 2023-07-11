package org.techtown.jedistest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;


public class RedisService extends Service {
    private static final String TAG = "RedisService";

    int port;
    String host;
    String channel;
    Jedis jedis;
    JedisPubSub pubSub;
    boolean isFirst = true;

    @Override
    public void onCreate() {
        super.onCreate();

        initNotificationChannel();
        initPubSub();
    }


    // 포그라운드 서비스를 위해 노티 표시
    private void initNotificationChannel() {
        String channelId = "org.techtown.smartcartrfidreader";
        String channelName = "MomsRedisAgent";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            int NOTIFICATION_ID = (int) (System.currentTimeMillis() % 10000);

            Notification notification = new Notification.Builder(this, channelId).build();
            startForeground(NOTIFICATION_ID, notification);
        }

    }


    // 메시지 전달받기
    private void initPubSub() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                BasicInfo.debug(TAG, "REDIS - onMessage(channel:" + channel + ", message:" + message + ")");

                // MainActivity 에서 메세지 찍기
                sendToActivity(message);
            }

        };

    }


    private void sendToActivity(String message) {
        BasicInfo.debug(TAG, "sendMessage() called.");

        Intent intent = new Intent("ServiceFilter");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        BasicInfo.debug(TAG, "onDestroy() called.");

        // 서비스 종료시 연결 끊기
        disconnectRedis();
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BasicInfo.debug(TAG, "onStartCommand() called.");

        // 명령 전달
        handleCommand(intent);

        return super.onStartCommand(intent, flags, startId);
    }


    protected void handleCommand(Intent intent) {
        String command = null;

        try {command = intent.getStringExtra("command");
        } catch (Exception ignored) {}

        BasicInfo.debug(TAG, "handleCommand() called. command : " + command);

        if (command == null) return;

        switch (command) {
            case "connect":
                port = intent.getIntExtra("port", 0);
                host = intent.getStringExtra("host");
                channel = intent.getStringExtra("channel");

                // 서비스 실행 후 최초에만 바로 연결
                if (jedis == null || isFirst) connectRedis();
                // 아니면 무조건 끊고 연결
                // (같은 채널일때는 끊지 않고 기존 연결 유지하고싶지만,
                // 그렇다면 레디스 서버의 서비스 재시작 후 통신이 안된다)
                else reconnectRedis();

                break;

            case "reconnect":
                reconnectRedis();
                break;
        }

    }


    // 레디스 연결
    private void connectRedis() {
        BasicInfo.debug(TAG, "handleCommand() called. " + host + ":" + port + ", " + channel);

        isFirst = false;

        new Thread(() -> {
            // 연결 관리 해시맵에 채널 정보가 있으면 객체 가져오기
            if (BasicInfo.redisMap.containsKey(channel))
                jedis = BasicInfo.redisMap.get(channel);
            else {
                // 정보가 없으면 새로 연결 후 해시맵에 값 저장
                jedis = new Jedis(host, port);
                BasicInfo.redisMap.put(channel, jedis);
            }

            assert jedis != null;
            // 해당 채널로 구독하기
            try {jedis.subscribe(pubSub, channel);
            } catch (Exception ignore) {}

        }).start();
    }


    // 레디스 연결 끊기
    private void disconnectRedis() {
        BasicInfo.debug(TAG, "disconnectRedis() called.");

        BasicInfo.redisMap.clear();
        if (jedis == null) return;
        jedis.disconnect();
        BasicInfo.error(TAG, "jedis.disconnect() called.");
    }


    // 레디스 재연결
    private void reconnectRedis() {
        BasicInfo.debug(TAG, "reconnectRedis() called.");

        disconnectRedis();
        if (jedis == null) { sendToActivity("기존 연결이 없습니다."); return; }

        // 연결이 없을때만 연결
        while (!isFirst) if (!jedis.isConnected()) { connectRedis(); break; }
    }

}