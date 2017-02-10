package com.xinnuo.apple.healthtesting;
/**
 * 父类封装一些方法
 * */

import android.app.Activity;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import static android.os.Environment.getRootDirectory;

/**
 * Created by Administrator on 2016/12/6 0006.
 */

public class ParentClass extends Activity {

    // 创建excel表.
    public void createExcel(File file ,WritableWorkbook wwb) {
        WritableSheet ws = null;
        try {
            if (!file.exists()) {
                wwb = Workbook.createWorkbook(file);

                ws = wwb.createSheet("sheet1", 0);

                // 在指定单元格插入数据
                Label lbl1 = new Label(0, 0, "卡号");
                Label bll2 = new Label(1, 0, "项目");
                Label bll3 = new Label(2, 0, "成绩");
                Label bll4 = new Label(3, 0, "录入时间");


                ws.addCell(lbl1);
                ws.addCell(bll2);
                ws.addCell(bll3);
                ws.addCell(bll4);


                // 从内存中写入文件中
                wwb.write();
                wwb.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToExcel(String cardNumber,String item,String grade,String date,File excelFile ,WritableWorkbook wwb) {

        try {
            Workbook oldWwb = Workbook.getWorkbook(excelFile);
            wwb = Workbook.createWorkbook(excelFile,
                    oldWwb);
            WritableSheet ws = wwb.getSheet(0);
            // 当前行数
            int row = ws.getRows();
            Label lbl1 = new Label(0, row, cardNumber);
            Label bll2 = new Label(1,row,item);
            Label bll3 = new Label(2,row,grade);
            Label bll4 = new Label(3,row, date);
            ws.addCell(lbl1);
            ws.addCell(bll2);
            ws.addCell(bll3);
            ws.addCell(bll4);

            // 从内存中写入文件中,只能刷一次.
            wwb.write();
            wwb.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 获取Excel文件夹
    public String getExcelDir() {
        String ret = null;
        // SD卡指定文件夹
        String sdcardPath = Environment.getExternalStorageDirectory()
                .toString();

        File dir = new File(sdcardPath + File.separator + "Excel"
                + File.separator + "健康测试");


        if (dir.exists()) {
            ret = dir.toString();
            Log.d("BAG", "dir保存路径存在");

        }

        else{
            dir.mkdirs();
            Log.d("BAG", "dir保存路径不存在,创建");
            ret = dir.toString();

        }

        Log.d("BAG", "保存路径 = "+ret);

        return ret;
    }



}
