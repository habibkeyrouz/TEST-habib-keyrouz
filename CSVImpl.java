package com.vendorPortal.importFile.readFile.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.vendorPortal.importFile.readFile.ReadFile;



/**
 * 
 * @author HabibKeyrouz
 *
 */
public class CSVImpl  implements ReadFile
{

    @SuppressWarnings("resource")
    public Map<Integer, ArrayList<String>> getRowsFromFile(String serviceCode, Map<Integer, ArrayList<String>> structure , File csvFile,String delimiter, int validation_row,int Validation_column)
    throws Exception 
    {
        Map<Integer, ArrayList<String>> rowsDataMap = null;
        ArrayList<String>               rowDataList  = null;
        BufferedReader br         = null;
        String         line       = "";
        String         cvsSplitBy = ",";
        int            order      = 0;
        try
        {
            rowsDataMap = new HashMap<Integer, ArrayList<String>>();
            br = new BufferedReader(new FileReader(csvFile));
            int lineNbr      = 0;
            while ((line = br.readLine()) != null) 
            {
                int colNbr      = 0;
                if(lineNbr >= validation_row)
                {
                    rowDataList = new ArrayList<String>();
                    String[] country = line.split(cvsSplitBy);
                    for(int i = 0 ; i <country.length ;i++)
                    {
                        if(colNbr >= Validation_column)
                        {
                            System.out.println("Country "+i+" [code= " + country[i]+"]");
                            rowDataList.add(country[i]);    
                        }
                        colNbr++;            
                    }
                    rowsDataMap.put(order, rowDataList);
                    order++;
                }
                lineNbr++;
            }
        }
        catch(Exception ex)
        {
                ex.getStackTrace();
                ex.getMessage();
        }
        return rowsDataMap;
    }

}
