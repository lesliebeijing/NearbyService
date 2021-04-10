package com.lesliefang.nearbyservice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hms.nearby.Nearby;
import com.huawei.hms.nearby.discovery.BroadcastOption;
import com.huawei.hms.nearby.discovery.ConnectCallback;
import com.huawei.hms.nearby.discovery.ConnectInfo;
import com.huawei.hms.nearby.discovery.ConnectResult;
import com.huawei.hms.nearby.discovery.Policy;
import com.huawei.hms.nearby.discovery.ScanEndpointCallback;
import com.huawei.hms.nearby.discovery.ScanEndpointInfo;
import com.huawei.hms.nearby.discovery.ScanOption;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "NearbyService";
    private TextView btnSend, btnRecv;
    private String[] permissions = new String[]{Permission.ACCESS_FINE_LOCATION, Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE};
    private static final String mFileServiceId = "com.lesliefang.nearbyservice";
    private static final int REQ_CODE_PERMISSION_SEND = 1;
    private static final int REQ_CODE_PERMISSION_RECV = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSend = findViewById(R.id.sendBtn);
        btnRecv = findViewById(R.id.recvBtn);

        btnSend.setOnClickListener(v -> {
            AndPermission.with(this).runtime().permission(permissions)
                    .rationale(mRationale)
                    .onGranted(data -> startBroadcast())
                    .onDenied(data -> {
                        if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions)) {
                            // 用Dialog展示没有某权限，询问用户是否去设置中授权。
                            List<String> permissionNames = Permission.transformText(MainActivity.this, permissions);
                            String message = "需要这些权限，" + permissionNames + "，是否到系统设置中授权？";

                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setCancelable(false)
                                    .setTitle("提示")
                                    .setMessage(message)
                                    .setPositiveButton("确定", (dialog, which) ->
                                            AndPermission.with(MainActivity.this)
                                                    .runtime()
                                                    .setting()
                                                    .start(REQ_CODE_PERMISSION_SEND))
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .start();
        });

        btnRecv.setOnClickListener(v -> {
            AndPermission.with(this).runtime().permission(permissions)
                    .rationale(mRationale)
                    .onGranted(data -> startScan())
                    .onDenied(data -> {
                        if (AndPermission.hasAlwaysDeniedPermission(MainActivity.this, permissions)) {
                            // 用Dialog展示没有某权限，询问用户是否去设置中授权。
                            List<String> permissionNames = Permission.transformText(MainActivity.this, permissions);
                            String message = "需要这些权限，" + permissionNames + "，是否到系统设置中授权？";

                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setCancelable(false)
                                    .setTitle("提示")
                                    .setMessage(message)
                                    .setPositiveButton("确定", (dialog, which) ->
                                            AndPermission.with(MainActivity.this)
                                                    .runtime()
                                                    .setting()
                                                    .start(REQ_CODE_PERMISSION_RECV))
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .start();
        });
    }

    private Rationale<List<String>> mRationale = new Rationale<List<String>>() {

        @Override
        public void showRationale(Context context, List<String> permissions,
                                  RequestExecutor executor) {
            List<String> permissionNames = Permission.transformText(context, permissions);
            String message = "需要这些权限才能正常使用" + "\n" + permissionNames;

            new android.app.AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle("提示")
                    .setMessage(message)
                    .setPositiveButton("确定", (dialog, which) -> executor.execute())
                    .setNegativeButton("取消", (dialog, which) -> executor.cancel())
                    .show();
        }
    };

    private void startBroadcast() {
        // 获取设备信息
        String mEndpointName = android.os.Build.DEVICE;
        BroadcastOption.Builder advBuilder = new BroadcastOption.Builder();
        // 选择广播策略
        advBuilder.setPolicy(Policy.POLICY_P2P);
        // 启动广播
        Nearby.getDiscoveryEngine(getApplicationContext()).startBroadcasting(mEndpointName, mFileServiceId, mConnectCallback, advBuilder.build());
        Log.d(TAG, "Start Broadcasting.");
    }

    private ConnectCallback mConnectCallback = new ConnectCallback() {
        @Override
        public void onEstablish(String endpointId, ConnectInfo connectInfo) {
            Log.d(TAG, "onEstablish endpointId：" + endpointId
                    + "  mAuthToken:" + connectInfo.getAuthCode()
                    + " mEndpointName:" + connectInfo.getEndpointName()
                    + " mIsIncomingConnect:" + connectInfo.isRemoteConnect());
        }

        @Override
        public void onResult(String endpointId, ConnectResult connectResult) {
            Log.d(TAG, "onResult endpointId:" + endpointId + " mStatus:" + connectResult.getStatus());
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.d(TAG, "onDisconnected endpointId:" + endpointId);
        }
    };

    public void startScan() {
        ScanOption.Builder scanBuilder = new ScanOption.Builder();
        // 选择扫描策略
        scanBuilder.setPolicy(Policy.POLICY_P2P);
        // 启动扫描
        Nearby.getDiscoveryEngine(getApplicationContext()).startScan(mFileServiceId, mScanEndpointCallback, scanBuilder.build());
        Log.d(TAG, "Start Scan.");
    }

    private ScanEndpointCallback mScanEndpointCallback = new ScanEndpointCallback() {
        @Override
        public void onFound(String endpointId, ScanEndpointInfo scanEndpointInfo) {
            Log.d(TAG, "onFound endpointId:" + endpointId
                    + " mEndpointName:" + scanEndpointInfo.getName()
                    + " mServiceId:" + scanEndpointInfo.getServiceId());
        }

        @Override
        public void onLost(String endpointId) {
            Log.d(TAG, "onLost endpointId:" + endpointId);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQ_CODE_PERMISSION_SEND) {
            if (AndPermission.hasPermissions(this, permissions)) {
                // 有对应的权限
                startBroadcast();
            } else {
                // 没有对应的权限
                Toast.makeText(this, "权限不足，无法使用", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQ_CODE_PERMISSION_RECV) {
            if (AndPermission.hasPermissions(this, permissions)) {
                // 有对应的权限
                startScan();
            } else {
                // 没有对应的权限
                Toast.makeText(this, "权限不足，无法使用", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

