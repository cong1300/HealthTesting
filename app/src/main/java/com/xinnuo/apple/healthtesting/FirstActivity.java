package com.xinnuo.apple.healthtesting;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.nio.charset.Charset;
import java.util.Locale;

public class FirstActivity extends AppCompatActivity {

    private Button healthTestBtn;
    private Button studentSignBtn;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        bindingView();

        // 获取默认的NFC控制器
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        //拦截系统级的NFC扫描，例如扫描蓝牙
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNdefPushMessage = new NdefMessage(new NdefRecord[] { newTextRecord("",
                Locale.ENGLISH, true) });
      }
    private NdefRecord newTextRecord(String text, Locale locale,
                                     boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(
                Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset
                .forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
                textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                new byte[0], data);
    }
    /**
     * 绑定空间
     */
    private void bindingView(){
        healthTestBtn = (Button)findViewById(R.id.healthBtn);
        studentSignBtn = (Button)findViewById(R.id.stuSignBtn);
    }

    /**
     * 按钮绑定事件
     */

    public void click(View view){
        //获得控件id
        int id = view.getId();
        switch (id){
            case R.id.healthBtn:
                Intent intent = new Intent(FirstActivity.this,MainActivity.class);
                startActivity(intent);
                break;
            case R.id.stuSignBtn:
                Intent intent2 = new Intent(FirstActivity.this,StudentSignActivity.class);
                startActivity(intent2);
                break;
            default:
                break;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter == null) {
            if (!mAdapter.isEnabled()) {
//                showWirelessSettingsDialog();
            }

            showMessage(R.string.error, R.string.no_nfc);
//            promt.setText("设备不支持NFC！");
            return;
        }
        if (!mAdapter.isEnabled()) {
//            promt.setText("请在系统设置中先启用NFC功能！");
            return;
        }

        if (mAdapter != null) {
            //隐式启动
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            //隐式启动
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
    }
    //获取系统隐式启动的
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }
    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }
}
