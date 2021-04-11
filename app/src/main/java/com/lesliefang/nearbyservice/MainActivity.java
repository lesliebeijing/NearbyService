package com.lesliefang.nearbyservice;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.obsez.android.lib.filechooser.ChooserDialog;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RequestExecutor;
import com.yanzhenjie.permission.runtime.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int FILE_SELECT_CODE = 0;
    private TextView btnSend, btnRecv;
    private String[] permissions = new String[]{Permission.ACCESS_FINE_LOCATION, Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE, Permission.CAMERA};
    private static final int REQ_CODE_PERMISSION_SEND = 100;
    private static final int REQ_CODE_PERMISSION_RECV = 101;
    private NearbyAgent nearbyAgent;

    // 从分享来的文件
    private List<File> shareFiles = new ArrayList<>();
    TextView tvShareFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");
        tvShareFiles = findViewById(R.id.tv_sharefiles);
        btnSend = findViewById(R.id.sendBtn);
        btnRecv = findViewById(R.id.recvBtn);

        nearbyAgent = new NearbyAgent(this);

        btnSend.setOnClickListener(v -> {
            AndPermission.with(this).runtime().permission(permissions)
                    .rationale(mRationale)
                    .onGranted(data -> {
                        if (!shareFiles.isEmpty()) {
                            nearbyAgent.sendFiles(shareFiles);
                            shareFiles.clear();
                            tvShareFiles.setText("");
                        } else {
                            showFileChooser();
                        }
                    })
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
                    .onGranted(data -> nearbyAgent.receiveFile())
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

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            String filepath = FileUtil.getPathFromContentUri(this, uri);
            File file = new File(filepath);
            if (file.exists()) {
                shareFiles.clear();
                shareFiles.add(file);
                tvShareFiles.setText("分享文件：" + uri.getPath());
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {
            shareFiles.clear();
            List<Uri> uriList = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            String sFile = "";
            for (int i = 0; i < uriList.size(); i++) {
                String filepath = FileUtil.getPathFromContentUri(this, uriList.get(i));
                File file = new File(filepath);
                if (file.exists()) {
                    shareFiles.add(file);
                    sFile += uriList.get(i).getPath() + "\n";
                }
            }
            tvShareFiles.setText("分享文件：\n" + sFile);
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    nearbyAgent.sendFile(new File(uri.getPath()));
                }
                break;
            case NearbyAgent.REQUEST_CODE_SCAN_ONE:
                nearbyAgent.onScanResult(data);
                break;
            case REQ_CODE_PERMISSION_SEND:
                if (AndPermission.hasPermissions(this, permissions)) {
                    // 有对应的权限
                    if (!shareFiles.isEmpty()) {
                        nearbyAgent.sendFiles(shareFiles);
                        shareFiles.clear();
                        tvShareFiles.setText("");
                    } else {
                        showFileChooser();
                    }
                } else {
                    // 没有对应的权限
                    Toast.makeText(this, "权限不足，无法使用", Toast.LENGTH_LONG).show();
                }
                break;
            case REQ_CODE_PERMISSION_RECV:
                if (AndPermission.hasPermissions(this, permissions)) {
                    // 有对应的权限
                    nearbyAgent.receiveFile();
                } else {
                    // 没有对应的权限
                    Toast.makeText(this, "权限不足，无法使用", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showFileChooser() {
        new ChooserDialog(MainActivity.this)
                .enableMultiple(false)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        //call nearby agent
                        nearbyAgent.sendFile(pathFile);
                    }
                })
                // to handle the back key pressed or clicked outside the dialog:
                .withOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.cancel(); // MUST have
                    }
                })
                .build()
                .show();
    }
}

