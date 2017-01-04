package com.cocomeng.videoencryptedplayer;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private EncryptedVideo encryptedVideo;
    private static final int RC_STORAGE = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        encryptedVideo = new EncryptedVideo(this);
        getPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        encryptedVideo.onDestroy();
    }

    @AfterPermissionGranted(RC_STORAGE)
    public void getPermissions() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "already obtain r/w permission", Toast.LENGTH_LONG).show();
            encryptedVideo.initServer("/sdcard/encrypted.mp4");
        } else {
            EasyPermissions.requestPermissions(this, "apply permission failed", RC_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_STORAGE) {
            Toast.makeText(this, "apply permission success", Toast.LENGTH_LONG).show();
            encryptedVideo.initServer("/sdcard/encrypted.mp4");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == RC_STORAGE) {
            Toast.makeText(this, "apply permission failed", Toast.LENGTH_LONG).show();
            EasyPermissions.somePermissionPermanentlyDenied("apply permission failed", perms);
        }
    }
}
