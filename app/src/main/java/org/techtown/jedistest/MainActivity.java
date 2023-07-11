package org.techtown.jedistest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Context context;
    private TextView logText;
    private EditText hostInput;
    private EditText portInput;
    private EditText channelInput;
    private Button openButton;
    private Button closeButton;
    private Button restartButton;
    private Button clearLogButton;
    private Button reconnectButton;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initButton();
        initReceiver();
    }


    // 화면 초기화
    private void initView() {
        BasicInfo.debug(TAG, "initView() called.");

        context = this;
        serviceIntent = new Intent(context, RedisService.class);

        portInput = findViewById(R.id.portInput);
        hostInput = findViewById(R.id.hostInput);
        channelInput = findViewById(R.id.channelInput);
        openButton = findViewById(R.id.openButton);
        closeButton = findViewById(R.id.closeButton);
        restartButton = findViewById(R.id.restartButton);
        clearLogButton = findViewById(R.id.clearLogButton);
        reconnectButton = findViewById(R.id.reconnectButton);

        logText = findViewById(R.id.logText);
        logText.setMovementMethod(new ScrollingMovementMethod());
    }


    // 버튼 초기화
    private void initButton() {
        BasicInfo.debug(TAG, "initView() called.");

        openButton.setOnClickListener(view -> connectRedis());
        closeButton.setOnClickListener(view -> disConnectRedis());
        restartButton.setOnClickListener(view -> restart());
        clearLogButton.setOnClickListener(view -> logText.setText(""));
        reconnectButton.setOnClickListener(view -> reconnectRedis());
    }


    // 서비스에서 받은 메시지 전달받는 리시버 초기화
    private void initReceiver() {
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(redisReceiver, new IntentFilter("ServiceFilter"));
    }


    // 서비스로부터 메시지 수신 받는 곳
    private final BroadcastReceiver redisReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            BasicInfo.debug(TAG, "onReceive() called. command : " + message);

            // 로그 화면에 출력
            logText.append("\n" + message);
            scrollBottom(logText);
        }

    };


    // 맨 아래로 스크롤
    private void scrollBottom(TextView textView) {
        int lineTop =  textView.getLayout().getLineTop(textView.getLineCount());
        int scrollY = lineTop - textView.getHeight();
        textView.scrollTo(0, Math.max(scrollY, 0));
    }


    // 레디스 연결 서비스 실행
    private void connectRedis() {
        BasicInfo.debug(TAG, "connectRedis() called.");

        // 인풋의 값으로 포트 호스트 채널을 지정
        final int port = Integer.parseInt(portInput.getText().toString());
        final String host = hostInput.getText().toString();
        final String channel = channelInput.getText().toString();

//        BasicInfo.debug(TAG, "REDIS 채널 [" + channel + "] 연결\n" + host + ":" + port);

        // 레디스 서비스 실행
        serviceIntent.putExtra("command", "connect");
        serviceIntent.putExtra("port", port);
        serviceIntent.putExtra("host", host);
        serviceIntent.putExtra("channel", channel);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) this.startService(serviceIntent);
        else this.startForegroundService(serviceIntent);
    }


    // 레디스 연결 끊기
    private void disConnectRedis() {
        BasicInfo.debug(TAG, "disConnectRedis() called.");

        this.stopService(serviceIntent);
    }


    // 레디스 재연결
    private void reconnectRedis() {
        BasicInfo.debug(TAG, "reconnectRedis() called.");
        // 레디스 서비스 실행
        serviceIntent.putExtra("command", "reconnect");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) this.startService(serviceIntent);
        else this.startForegroundService(serviceIntent);
    }


    // 재시작 상황 만들기 (연결 수 확인용)
    private void restart() {
        BasicInfo.debug(TAG, "restart() called.");

        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

}