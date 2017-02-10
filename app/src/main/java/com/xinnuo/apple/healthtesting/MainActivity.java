package com.xinnuo.apple.healthtesting;
/**
 * 健康测试主界面 选择相应功能进行跳转
 * */
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

public class MainActivity extends AppCompatActivity {
    private Button height_and_weight;          //身高体重
    private Button vital_capacity;             //肺活量
    private Button long_jump;                  //立定跳远
    private Button reach;                      //坐位体前屈
    private Button abdominal_curl;           //仰卧起坐
    private Button pull_ups;                  //引体向上
    private Button eight_hundred_meters;   //800米
    private Button one_thousand_meters;    //1000米
    private Button fifty_meters;            //50米
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("健康测试");
        binding();
        clickJump();
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
     * 绑定控件
     * */
    protected void binding()
    {
        height_and_weight = (Button) findViewById(R.id.height_and_weight);
        vital_capacity = (Button) findViewById(R.id.vital_capacity);
        long_jump = (Button) findViewById(R.id.long_jump);
        reach = (Button) findViewById(R.id.reach);
        abdominal_curl = (Button) findViewById(R.id.abdominal_curl);
        pull_ups = (Button) findViewById(R.id.pull_ups);
        eight_hundred_meters = (Button) findViewById(R.id.eight_hundred_meters);
        one_thousand_meters = (Button) findViewById(R.id.one_thousand_meters);
        fifty_meters = (Button) findViewById(R.id.fifty_meters);
    }

    protected void clickJump()
    {
        //身高体重
        height_and_weight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,HeightAndWeightActivity.class);
                startActivity(intent);
            }
        });
        //肺活量
        vital_capacity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "肺活量";
                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //立定跳远
        long_jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "立定跳远(cm)";
                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //坐位体前屈
        reach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "坐位体前屈(cm)";
                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //仰卧起坐
        abdominal_curl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "仰卧起坐";
                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //引体向上
        pull_ups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "引体向上";
                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //800米
        eight_hundred_meters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "800米跑";
//                Intent intent = new Intent(MainActivity.this,GeneralPageActivity.class);
                Intent intent = new Intent(MainActivity.this,RunActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);

            }
        });
        //1000米
        one_thousand_meters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "1000米";
                Intent intent = new Intent(MainActivity.this,RunActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
        //50米
        fifty_meters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String item = "50米(秒)";
                Intent intent = new Intent(MainActivity.this,RunActivity.class);
                intent.putExtra("item",item);
                startActivity(intent);
            }
        });
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
