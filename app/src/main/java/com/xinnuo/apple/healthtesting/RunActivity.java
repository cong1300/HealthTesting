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
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.xinnuo.apple.healthtesting.read.ParsedNdefRecord;

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

import jxl.write.WritableWorkbook;

import static android.content.ContentValues.TAG;

public class RunActivity extends ParentClass implements View.OnClickListener{
    private TextView minuteTv, secondTv, longmillTv;
    private Button resetBtn, startBtn,stateBtn, backOffBtn,saveBtn;
    private ListView listView;
    //    private TimeAdapter adapter;
    private ArrayList<Map<String,Object>> list = new ArrayList<>();
    private boolean isPaused = false;
    private String timeUsed;
    private long timeUsedInsec;
    private boolean leftBtnFlag = false;// 判断是复位还是计次,ture为计次
    private boolean rightBtnFlag = true;// 判断是开始还是暂停,true为开始

    private static final int TICK_WHAT = 2;
    private static final int TIME_TO_SEND = 100;
    private int order = 0;
    private String cardNumber = "";
    private LinearLayout btnLayout;
    protected RelativeLayout underBtnLayout;
    private File excelFile;
    private String excelPath;
    private WritableWorkbook wwb;
    private static final DateFormat TIME_FORMAT = SimpleDateFormat
            .getDateTimeInstance();
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    //    private TextView promt;
    private AlertDialog mDialog;

    private String hexIdStr;
    private String decIdStr = "";
    private String oldCardId = "0123";
    private ArrayList<Map<String,Object>> newList = new ArrayList<>();
    private int orderNumber = 0;
    private boolean cardNumberInsert = false;
    private String item = null;
     private Handler uiHandle = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case TICK_WHAT:
                    if (!isPaused) {
                        addTimeUsed();
                        updateClockUI();
                    }
                    uiHandle.sendEmptyMessageDelayed(TICK_WHAT, TIME_TO_SEND);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_run);
        initView();
        setListener();
        Intent intent = getIntent();
        item = intent.getStringExtra("item");
        excelPath = getExcelDir()+ File.separator+"健康测试.xls";
        excelFile = new File(excelPath);
        createExcel(excelFile,wwb);

        resolveIntent(getIntent());

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
           // text_cardNumber.setText("设备不支持NFC！");
            return;
        }
        if (!mAdapter.isEnabled()) {
            //text_cardNumber.setText("请在系统设置中先启用NFC功能！");
            return;
        }

        if (mAdapter != null) {
            //隐式启动
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
        isPaused = false;

    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            //隐式启动
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
        isPaused = true;

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
    // ListView 中某项被选中后的逻辑
//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        Log.v("MyListView4-click", (String) mData.get(position).get("title"));
//    }
//
//    // listview中点击按键弹出对话框
//    public void showInfo(final int position) {
//        new AlertDialog.Builder(this).setTitle("我的提示").setMessage("确定要删除吗？")
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        mData.remove(position);
//                        // 通过程序我们知道删除了，但是怎么刷新ListView呢？
//                        // 只需要重新设置一下adapter
//                        setListAdapter(adapter);
//                    }
//                }).show();
//    }


    //每次刷卡插入数据
    private void makeNewList(String decId){
        Log.d(TAG, "makeNewList: newId = "+decIdStr);
        Log.d(TAG, "makeNewList: oldId = "+oldCardId);

        if (list.size() == 0)
        {
            Log.d(TAG, "makeNewList: 请添加数据");
            Toast.makeText(getApplicationContext(), "请添加数据!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (orderNumber == list.size())
        {
            Log.d(TAG, "makeNewList: 没那么多数据");
            Toast.makeText(getApplicationContext(), "没那么多数据!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cardNumberInsert){
            Log.d(TAG, "makeNewList:还没点击开始刷卡");
            Toast.makeText(getApplicationContext(), "还没点击开始刷卡!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (oldCardId.equals(decId)){
            Log.d(TAG, "makeNewList: 已经打过卡了");
            Toast.makeText(getApplicationContext(), "已经打过卡了!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        for (Map a : list)
        {
            if (a.get("cardNumber").toString().equals(decId))
            {
                Toast.makeText(getApplicationContext(), "已经打过卡了!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        }
        oldCardId = decId;
        Log.d(TAG, "**************之前***************");

        Log.d(TAG, "makeNewList: "+newList);
        Log.d(TAG, "oldList: "+list);

        Map<String,Object> aMap = new HashMap<>();

        aMap = list.get(orderNumber);
        aMap.put("cardNumber",decId);
        newList.set(orderNumber,aMap);
        SimpleAdapter qrAdapter = new SimpleAdapter(this,newList,R.layout.list_time,
                new  String[]{"order","time","cardNumber"},new int[]{R.id.order_textView,R.id.time_textView,R.id.card_textView});




        listView.setAdapter(qrAdapter);
        orderNumber++;
        Log.d(TAG, "**************之后***************");

        Log.d(TAG, "makeNewList: "+newList);
        Log.d(TAG, "oldList: "+list);

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

        makeNewList(decIdStr);


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
//            timeView.setText("\n获取到hexId = "+hexIdStr+"\n获取到decId = "+decIdStr);
            ParsedNdefRecord record = records.get(i);
//            promt.append(record.getViewText());
//            text_cardNumber.setText(decIdStr);
//            promt.append(result);

        }
    }

    //获取系统隐式启动的
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }
    private void initView() {
        minuteTv = (TextView) findViewById(R.id.minute);
        secondTv = (TextView) findViewById(R.id.second);
        longmillTv = (TextView) findViewById(R.id.longmill);
        listView = (ListView) findViewById(R.id.listview);
        resetBtn = (Button) findViewById(R.id.reset);
        startBtn = (Button) findViewById(R.id.start_and_stop);
        stateBtn = (Button)findViewById(R.id.state_btn);
        backOffBtn = (Button)findViewById(R.id.backOff_btn);
        saveBtn = (Button)findViewById(R.id.ok_btn);
        btnLayout = (LinearLayout)findViewById(R.id.btn_layout);
        underBtnLayout = (RelativeLayout)findViewById(R.id.underBtn_layout);
        underBtnLayout.setVisibility(View.GONE);

        if (leftBtnFlag) {
            resetBtn.setText("计次");
        } else {
            resetBtn.setText("复位");
        }
        if (rightBtnFlag) {
            startBtn.setText("启动");
        } else {
            startBtn.setText("暂停");
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (newList.size() == 0)
                {
                    Log.d(TAG, "onItemClick: 还不可以点");
                    return;
                }
                Log.d("点击事件","第"+position+"行");
                showInfo(position);

            }
        });

    }
    // listview中点击按键弹出对话框
    public void showInfo(final int position) {
         new AlertDialog.Builder(this).setTitle("提示").setMessage("确定要删除吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       
                        newList.remove(position);
                        // 通过程序我们知道删除了，但是怎么刷新ListView呢？
                        // 只需要重新设置一下adapter
                        listFresh();

                     }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {// 消极

            @Override
            public void onClick(DialogInterface dialog,
                                int which) {
 //                Toast.makeText(MainActivity.this, "一点也不老实", 0)
//                        .show();
            }
        }).show();
    }
    private void listFresh(){
        SimpleAdapter qrAdapter = new SimpleAdapter(this,newList,R.layout.list_time,
                new  String[]{"order","time","cardNumber"},new int[]{R.id.order_textView,R.id.time_textView,R.id.card_textView});

        listView.setAdapter(qrAdapter);
    }
    private void setListener() {
        resetBtn.setOnClickListener(this);
        startBtn.setOnClickListener(this);
        stateBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        backOffBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reset:
                startTimer(State.HIDE);

                if (rightBtnFlag) {
                    uiHandle.removeMessages(TICK_WHAT);
                    orderNumber = 0;
                    timeUsedInsec = 0;
                    order = 0;
                    minuteTv.setText("00");
                    secondTv.setText("00");
                    longmillTv.setText("0");
                    list.removeAll(list);
                     listView.setAdapter(null);

                } else {
//                     adapter.notifyDataSetChanged();
                    order++;
                    Map<String ,Object> aData = new HashMap<>();
                    aData.put("time",timeUsed);
                    aData.put("order",order+"");
                    aData.put("cardNumber",cardNumber);
                    list.add(aData);
                    SimpleAdapter qrAdapter = new SimpleAdapter(this,list,R.layout.list_time,
                            new  String[]{"order","time","cardNumber"},new int[]{R.id.order_textView,R.id.time_textView,R.id.card_textView});

                    listView.setAdapter(qrAdapter);
                }
                break;
            case R.id.start_and_stop:
                rightBtnFlag = !rightBtnFlag;
                if (rightBtnFlag) {
                    startBtn.setText("启动");
                    resetBtn.setText("复位");
                    leftBtnFlag = false;
                    isPaused = true;
                    uiHandle.removeMessages(TICK_WHAT);
                    startTimer(State.SHOW);
                } else {

                    startBtn.setText("暂停");
                    resetBtn.setClickable(true);
                    startTime();
                    startTimer(State.HIDE);
                    resetBtn.setText("计次");
                    leftBtnFlag = true;
                    isPaused = false;

                }
                break;
            case R.id.state_btn:
                // 弹出一个提示窗口 是否开始打 1。是（把下列方法里执行）2。否

                startCard();

                break;
            case R.id.ok_btn:
                // 弹出一个提示窗口 是否保存，保存：state =1 ，取消
                saveData();
                //保存相当于复位 ，在提示窗口里（方法里复位）
                break;
            case R.id.backOff_btn:
                //List 重新加载到 ListView  , newList 清空。【刷卡按钮】【上方按钮区域】显示
                backOffAction();
                break;
                //List 重新加载到 ListView ，newList 清空 其他不变



            default:
                break;
        }
    }
    /*
【启动】
1.计时ing。下方按钮区域隐藏
2.暂停ing。下方按钮区域显示
【复位】
1.下方按钮区域隐藏

【开始刷卡】
1.上方按钮区域隐藏，【开始刷卡按钮】隐藏 .newList -> List
【保存】
1。是。数据清空复位，把上方按钮区域显示出来，下方按钮区域隐藏
2。否。
【撤销】
1.重载旧List 。
2.否。
【返回】
1.newList清空
2.list重新加载到listView
3.【上方按钮区域】显示 【开始刷卡按钮】显示
     */

    /**
     * 启动计时，隐藏下方按钮区域
     */
    private void startTimer(State s){

        cardNumberInsert = false;

        if (s == State.SHOW){
            underBtnLayout.setVisibility(View.VISIBLE);
        }else {
            underBtnLayout.setVisibility(View.GONE);
        }
    }
    enum State{//隐藏后者显示
        SHOW,HIDE
    }

    /**
     * 复位 下方按钮区域隐藏
     */
    private void resetAll(){
        cardNumberInsert = false;
        underBtnLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 开始刷开 上方按钮区隐藏。【开始刷卡按钮】隐藏
     */
    private void startCard(){

        if (order == 0){
            Toast.makeText(getApplicationContext(), "请计时",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this).setTitle("提示").setMessage("确定要开始刷卡吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         btnLayout.setVisibility(View.GONE);
                        stateBtn.setVisibility(View.GONE);
                         newList.addAll(list);

                         cardNumberInsert = true;

                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {// 消极

            @Override
            public void onClick(DialogInterface dialog,
                                int which) {

            }
        }).show();

    }
    /**
     【返回】
     1.newList清空
     2.list重新加载到listView
     3.【上方按钮区域】显示 【开始刷卡按钮】显示
     */
    private void backOffAction(){
        Log.d(TAG, "backOffAction: 返回时，list = "+list);
        list.removeAll(list);

        for (Map<String,Object> a :newList){
            a.put("carNumber","");
            list.add(a);
        }


        cardNumberInsert = false;
        newList.removeAll(newList);
        SimpleAdapter qrAdapter = new SimpleAdapter(this,list,R.layout.list_time,
                new  String[]{"order","time","cardNumber"},new int[]{R.id.order_textView,R.id.time_textView,R.id.card_textView});

        listView.setAdapter(qrAdapter);

        btnLayout.setVisibility(View.VISIBLE);
        stateBtn.setVisibility(View.VISIBLE);
    }


    /**
     * 保存 。
     * 1先把数据 for循环到excel ，
     * 2然后再清空listView list。
     * 3视图还原。[1.【上方按钮区域】显示 2.【开始刷卡按钮】显示，【下方按钮区域】隐藏
     */
    private void saveData(){

        if (orderNumber == 0){
            Log.d(TAG, "saveData: 请刷卡");
            Toast.makeText(getApplicationContext(), "请刷卡",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (orderNumber < newList.size()){
            Log.d(TAG, "saveData: 请删除多余数据");
            Toast.makeText(getApplicationContext(), "请删除多余数据",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this).setTitle("提示").setMessage("确定要保存数据吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                         //1保存
                        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy.MM.dd");
                        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
                        String date = formatter.format(curDate);
                        Iterator<Map<String,Object>> iter = newList.iterator();


                         while (iter.hasNext()){
                             Map<String,Object> aMap = iter.next();

                             String cardId = aMap.get("cardNumber").toString();
                             String time = aMap.get("time").toString();
                             writeToExcel(cardId,item,time,date,excelFile,wwb);



                         }



                        //2清空
                        list.removeAll(list);
                        listView.setAdapter(null);
                        //3视图还原
                        stateBtn.setVisibility(View.VISIBLE);
                        underBtnLayout.setVisibility(View.GONE);
                        btnLayout.setVisibility(View.VISIBLE);
                        cardNumberInsert = false;
                        orderNumber = 0;

                        uiHandle.removeMessages(TICK_WHAT);
                        timeUsedInsec = 0;
                        order = 0;
                        minuteTv.setText("00");
                        secondTv.setText("00");
                        longmillTv.setText("0");
                        list.removeAll(list);
                        newList.removeAll(newList);
                        listView.setAdapter(null);
                        oldCardId = "0123";


                        Toast.makeText(getApplicationContext(), "保存完成",
                                Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {// 消极

            @Override
            public void onClick(DialogInterface dialog,
                                int which) {

            }
        }).show();



     }


    private void startTime() {
        uiHandle.sendEmptyMessageDelayed(TICK_WHAT, TIME_TO_SEND);
    }

    /**
     * 更新时间的显示
     */
    private void updateClockUI() {
        minuteTv.setText(getMin());
        secondTv.setText(getSec());
        longmillTv.setText(getLongMill());
    }

    public void addTimeUsed() {
        timeUsedInsec += 100 ;
        timeUsed = this.getMin() + ":" + this.getSec()+":"+this.getLongMill();
    }

    public String getMin() {
        long min = (timeUsedInsec) / 60000;
        return min < 10 ? "0" + min : min+"";
    }

    public String getSec() {
        long sec = (timeUsedInsec / 1000) % 60;
        return sec < 10 ? "0" + sec : sec+"";
    }

    public String getLongMill() {
        long longmill = (timeUsedInsec/100) % 10;
        return longmill + "";
    }


}
