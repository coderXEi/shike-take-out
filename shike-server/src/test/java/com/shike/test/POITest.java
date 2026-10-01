package com.shike.test;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;

@SpringBootTest
public class POITest {

    /**
     * 通过POI创建excel文件并写入文件内容
     */
    @Test
    public  void write() throws IOException {
        XSSFWorkbook excel =  new XSSFWorkbook();

        // 在excel文件 创建一个sheet表  手动打开时默认一个，但代码层面没有
        XSSFSheet sheet1 = excel.createSheet("sheet1");
        // 在sheet页中创建行对象
        XSSFRow row = sheet1.createRow(0);
        // 创建单元格 并写入内容
        row.createCell(1).setCellValue("姓名 ");
        row.createCell(3).setCellValue("城市");

        // 创建新行
        XSSFRow row1 = sheet1.createRow(1);
        row1.createCell(2).setCellValue("小王");
        row1.createCell(3).setCellValue("江苏");


        // 通过输出流写出到硬盘
        FileOutputStream out = new FileOutputStream("D:\\info.xlsx");

        excel.write(out);
        out.close();
        excel.close();
    }


    // 读取

    @Test
    public void read() throws IOException {
        FileInputStream fis =   new FileInputStream(new File("D:\\info.xlsx"));
        XSSFWorkbook excel =  new XSSFWorkbook(fis);

        // 读取 excel sheet页  可以根据下标 或名字
        XSSFSheet sheet =  excel.getSheetAt(0); // 第一个sheet页
        int lastRowNum = sheet.getLastRowNum();// 获取最后一行的行号

        for(int i = 0 ; i <= lastRowNum ; i++){
            // 获得某一行
            XSSFRow row = sheet.getRow(i);
            // 获得单元格对象
            String stringCellValue = row.getCell(1).getStringCellValue();
            String stringCellValue1 = row.getCell(3).getStringCellValue();

            System.out.println(stringCellValue);

            System.out.println(stringCellValue1);
        }

        excel.close();
        fis.close();

    }


}
