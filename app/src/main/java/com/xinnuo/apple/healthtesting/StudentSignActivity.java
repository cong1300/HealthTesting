package com.xinnuo.apple.healthtesting;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.xinnuo.apple.healthtesting.StudentSignWriteClass;
import com.xinnuo.apple.healthtesting.read.ParsedNdefRecord;

import org.w3c.dom.Text;

import jxl.write.WritableWorkbook;

import static android.content.ContentValues.TAG;

public class StudentSignActivity extends StudentSignWriteClass {
    private TextView studentCountText;
    private ListView studentListView;
    private TextView cardNumberTextView;
    private String date;         // 日期
    private String excelPath;
    private File excelFile;
    private WritableWorkbook wwb;
    private AlertDialog mDialog;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private String hexIdStr;
    private String decIdStr = "";

    private static final DateFormat TIME_FORMAT = SimpleDateFormat
            .getDateTimeInstance();
    ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String,     Object>>();/*在数组中存放数据*/
    private int order = 0;
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_sign);
        excelPath = getExcelDir()+ File.separator+"学生签到.xls";
        excelFile = new File(excelPath);
        createExcel(excelFile,wwb);
        resolveIntent(getIntent());
        binding();

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null)
                .create();
        // 获取默认的NFC控制器
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        //拦截系统级的NFC扫描，例如扫描蓝牙
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNdefPushMessage = new NdefMessage(new NdefRecord[] { newTextRecord("",
                Locale.ENGLISH, true) });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter == null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }

            showMessage(R.string.error, R.string.no_nfc);
            cardNumberTextView.setText("设备不支持NFC！");
            return;
        }
        if (!mAdapter.isEnabled()) {
            cardNumberTextView.setText("请在系统设置中先启用NFC功能！");
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
    //16进制字符串转换为String
    private String hexString = "0123456789ABCDEF";
    public String decode(String bytes) {
        if (bytes.length() != 30) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                bytes.length() / 2);
        // 将每2位16进制整数组装成一个字节
        for (int i = 0; i < bytes.length(); i += 2)
            baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
                    .indexOf(bytes.charAt(i + 1))));
        return new String(baos.toByteArray());
    }

    // 字符序列转换为16进制字符串
    private static String bytesToHexString(byte[] src, boolean isPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (isPrefix == true) {
            stringBuilder.append("0x");
        }
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.toUpperCase(Character.forDigit(
                    (src[i] >>> 4) & 0x0F, 16));
            buffer[1] = Character.toUpperCase(Character.forDigit(src[i] & 0x0F,
                    16));
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
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

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(
                                Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        builder.create().show();
        return;
    }

    //初步判断是什么类型NFC卡
    private void resolveIntent(Intent intent) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        date = formatter.format(curDate);


        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent
                    .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Parcelable tag = intent
                        .getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] payload = dumpTagData(tag).getBytes();
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,
                        empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
                msgs = new NdefMessage[] { msg };
            }
            // Setup the views
            buildTagViews(msgs);
        }
    }

    //一般公家卡，扫描的信息
    private String dumpTagData(Parcelable p) {
        StringBuilder sb = new StringBuilder();
        Tag tag = (Tag) p;
        byte[] id = tag.getId();
        sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
        sb.append("Tag ID (dec): ").append(getDec(id)).append("\n");
        sb.append("ID (reversed): ").append(getReversed(id)).append("\n");
        hexIdStr = getHex(id);
        decIdStr = getDec(id)+"";


        Log.d("de he id =", "dumpTagData: hex ="+hexIdStr+"dex="+decIdStr);
        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                MifareClassic mifareTag = MifareClassic.get(tag);
                String type = "Unknown";
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                sb.append("Mifare Classic type: ");
                sb.append(type);
                sb.append('\n');

                sb.append("Mifare size: ");
                sb.append(mifareTag.getSize() + " bytes");
                sb.append('\n');

                sb.append("Mifare sectors: ");
                sb.append(mifareTag.getSectorCount());
                sb.append('\n');

                sb.append("Mifare blocks: ");
                sb.append(mifareTag.getBlockCount());
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }

        return sb.toString();
    }

    private String getHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long getReversed(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    //显示NFC扫描的数据
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }
        // Parse the first message in the list
        // Build views for all of the sub records
        Date now = new Date();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();
        for (int i = 0; i < size; i++) {
            TextView timeView = new TextView(this);
            timeView.setText(TIME_FORMAT.format(now));
            String result = "\n获取到hexId = "+hexIdStr+"\n获取到decId = "+decIdStr;
            Log.d("result", "buildTagViews: "+result);
            cardNumberTextView.setText(decIdStr);
            //写入excl
            writeTo(decIdStr);
        }
    }

    //获取系统隐式启动的
    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent        Log.d(TAG, \"onNewIntent: nfc刷新了\");\n: nfc刷新了");
        setIntent(intent);
        resolveIntent(intent);
    }
    protected void binding()
    {
        cardNumberTextView = (TextView)findViewById(R.id.studentSign_TextView);
        studentListView = (ListView)findViewById(R.id.studentList_listView);
        studentCountText = (TextView)findViewById(R.id.studentCount_textView);
        HashMap<String,Object> aMap = new HashMap<String, Object>();
        aMap.put("item0","序号");
        aMap.put("item1","卡号");
        aMap.put("item2","日期");

        listItem.add(aMap);
    }

    private void writeTo(String decId) {
        order++;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        date = formatter.format(curDate);

    HashMap<String,Object> aMap = new HashMap<String, Object>();
        aMap.put("item0",order+"");
        aMap.put("item1",decId);
        aMap.put("item2",date);

        Log.d(TAG, "writeTo: contains"+listItem.contains(aMap));

        for (Map a : listItem)
        {
            if (a.get("item1").toString().equals(aMap.get("item1").toString()))
            {
                Toast.makeText(getApplicationContext(), "已经打过卡了!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        }


            listItem.add(aMap);


        SimpleAdapter qrAdapter = new SimpleAdapter(this,listItem,R.layout.item_studentlistlayout,
                new  String[]{"item0","item1","item2"},new int[]{R.id.orderNumber_textView,R.id.cardNumber_textView,R.id.time_textView});


        studentListView.setAdapter(qrAdapter);
        studentCountText.setText("签到人数："+order);
        Log.d(TAG, "writeTo: listItem = "+listItem);


        writeToExcel(decId, date, excelFile, wwb);


    }


}
