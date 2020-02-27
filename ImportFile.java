package com.vendorPortal.importFile;

import com.vendorPortal.common.Utility;
import com.vendorPortal.importFile.readFile.ReadFile;
import com.vendorPortal.importFile.readFile.impl.CSVImpl;
import com.vendorPortal.importFile.readFile.impl.TXTImpl;
import com.vendorPortal.importFile.readFile.impl.XLSImpl;
import com.vendorPortal.importFile.readFile.impl.XLSXImpl;
import java.io.File;
import java.io.FileInputStream;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class ImportFile
{
  public static XmlObject validateFile(String ServiceCode, String fileBase64, XmlObject structure, String mainTagNameStruct, String nameColumnOrderStruct, String typeFile, String delimiter, int is_header, int validation_row, int Validation_column, String totalAccountNumber, String totalAmounts, String numberOfRows, String penaltyDate)
  {
    Map<Integer, ArrayList<String>> rowsDataMap = null;
    Map<Integer, ArrayList<String>> rowsMapStructure = null;
    int errorCode = 0;
    String errorMessage = "";
    boolean isValidate = true;
    XmlObject objToReturn = null;
    File fileResponse = null;
    ArrayList<String> concatRowsIsDuplicate = null;
    String concatDuplicateCol = null;
    int totalActNbrLeft = 0;// for BLCB
    int totalActNbrRight = 0;// for BLCB
    Double totalAmount = Double.valueOf(0.0D);//THNS
    String cycleDate = "";//UNRA or THAS
    String normalDate = "";//UNRA
    String excepDate = "";//UNRA or THAS
    Integer countRows = Integer.valueOf(0);//THNS
    try
    {
      fileBase64 = fileBase64.replaceAll("\\r|\\n", "");
      //byte[] bFile = Base64.getDecoder().decode(fileBase64.getBytes("UTF-8"));
      byte[] bFile = org.apache.commons.codec.binary.Base64.decodeBase64(fileBase64.getBytes("UTF-8"));
      String path = new File(".").getCanonicalPath();
      boolean dirStatus = new File(path + "/vendorPortal/repository/import").mkdirs();
      System.out.println("Import directory path status ==" + dirStatus);
      File fileConvert = new File(path + "/vendorPortal/repository/import/convertFile_" + System.currentTimeMillis() + "." + typeFile);
      fileResponse = new File(path + "/vendorPortal/repository/import/response_" + System.currentTimeMillis() + "." + typeFile);
      ImportFile validation = new ImportFile();
      String typeFileUpper = typeFile.toUpperCase();
      rowsMapStructure = validation.getRowsFromXMLObject(structure, mainTagNameStruct, nameColumnOrderStruct);
      rowsDataMap = validation.getRowsFromFile(ServiceCode, rowsMapStructure, bFile, fileConvert, typeFileUpper, delimiter, validation_row, Validation_column);
      DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
      Document document = documentBuilder.newDocument();
      Document documentError = documentBuilder.newDocument();
      
      Element root = document.createElement("PostOfflineDataRequest");
      root.setAttribute("xmlns", "http://www.omt.com/schemas/PostOfflineDataRequest");
      document.appendChild(root);
      System.out.println("Import rowsDataMap ==" + rowsDataMap);
      if (rowsDataMap.equals(null))
      {
        errorCode = 11;
        errorMessage = "JAVA CallOut ERROR : No Data found in file ;";
        isValidate = false;
      }
      else
      {
        Element success = document.createElement("Success");
        success.appendChild(document.createTextNode("true"));
        root.appendChild(success);
        

        Element errorElem = document.createElement("error");
        root.appendChild(errorElem);
        
        Element errorCodeElem = document.createElement("errorCode");
        errorCodeElem.appendChild(document.createTextNode(""));
        errorElem.appendChild(errorCodeElem);
        
        Element errorMessageElem = document.createElement("errorMessage");
        errorMessageElem.appendChild(document.createTextNode(""));
        errorElem.appendChild(errorMessageElem);
        
        Element providerErrorCode = document.createElement("providerErrorCode");
        providerErrorCode.appendChild(document.createTextNode(""));
        errorElem.appendChild(providerErrorCode);
        
        Element providerErrorMessage = document.createElement("providerErrorMessage");
        providerErrorMessage.appendChild(document.createTextNode(""));
        errorElem.appendChild(providerErrorMessage);

        Element serviceCode = document.createElement("ServiceCode");
        serviceCode.appendChild(document.createTextNode(ServiceCode));
        root.appendChild(serviceCode);

        concatRowsIsDuplicate = new ArrayList();
      }
      if ("UNRA".equals(ServiceCode.toUpperCase()))
      {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyyy");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd");
        
        Date valDateCycle = sdf.parse(penaltyDate.split(delimiter)[0]);
        cycleDate = output.format(valDateCycle);
        System.out.println("Import Validation cycleDate =" + cycleDate);
        
        Date valDateOfNormal = sdf.parse(penaltyDate.split(delimiter)[1]);
        normalDate = output.format(valDateOfNormal);
        System.out.println("Import Validation normalDate =" + normalDate);
        
        Date valDateOfExcep = sdf.parse(penaltyDate.split(delimiter)[2]);
        excepDate = output.format(valDateOfExcep);
        System.out.println("Import Validation excepDate =" + excepDate);
      }
      else if ("OEAS".equals(ServiceCode.toUpperCase()))
      {
          SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
          SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd");

          Date valFromDate = sdf.parse(penaltyDate.split(delimiter)[0]);
          cycleDate = output.format(valFromDate);
          System.out.println("Import Validation OEAS valFromDate =" + cycleDate);
          
          Date valToDate = sdf.parse(penaltyDate.split(delimiter)[1]);
          excepDate = output.format(valToDate);
          System.out.println("Import Validation OEAS valToDate =" + excepDate);
      }
      int totalRows = rowsDataMap.size();
      System.out.println("Import totalRows ==" + totalRows);
      if (isValidate) {
        if (((ArrayList)rowsDataMap.get(Integer.valueOf(0))).size() >= rowsMapStructure.size())
        {
          for (int i = 0; i < totalRows; i++)
          {
            Element offlineInfo = null;
            Element offlineInfo1 = null;
            if ((i != 0) || (is_header == 0))
            {
              offlineInfo = document.createElement("OfflineInfo");
              root.appendChild(offlineInfo);
              if ("OEAS".equals(ServiceCode.toUpperCase())){
                  offlineInfo1 = document.createElement("OfflineInfo");
                  root.appendChild(offlineInfo1);
              }
            }
            ArrayList<String> row = (ArrayList)rowsDataMap.get(Integer.valueOf(i));
            concatDuplicateCol = "";
            int lenthOfRow = rowsMapStructure.size();
            if (row.size() >= lenthOfRow) {
              for (int j = 0; j < lenthOfRow; j++)
              {
                String cell = ((String)row.get(j)).trim();
                if (Boolean.parseBoolean(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(6)).trim())) {
                  if (is_header == 1)
                  {
                    if (i != 0) {
                      if (concatDuplicateCol.equals("")) {
                        concatDuplicateCol = cell.trim();
                      } else {
                        concatDuplicateCol = concatDuplicateCol + "_" + cell.trim();
                      }
                    }
                  }
                  else if (concatDuplicateCol.equals("")) {
                    concatDuplicateCol = cell.trim();
                  } else {
                    concatDuplicateCol = concatDuplicateCol + "_" + cell.trim();
                  }
                }
                if ((i == 0) && (is_header == 1))
                {
                  for (int k = 0; k < rowsMapStructure.size(); k++) {
                    if (cell.equals(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(k))).get(i)).trim()))
                    {
                      if (Integer.parseInt(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(k))).get(2)).trim()) == j) {
                        break;
                      }
                      errorCode = 1;
                      errorMessage = "JAVA CallOut ERROR : order ; column Name = " + cell;
                      isValidate = false; break;
                    }
                  }
                  if (!isValidate) {
                    break;
                  }
                  if ((!cell.equals(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(i)).trim())) && (isValidate))
                  {
                    errorCode = 2;
                    errorMessage = "JAVA CallOut ERROR : Column Name = " + cell;
                    isValidate = false;
                    break;
                  }
                }
                else
                {
                  Element columnNameElem = document.createElement(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(5)).trim());
                  Element columnNameElem1 = document.createElement(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(5)).trim());
                  if (Boolean.parseBoolean(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(4)).trim())) {
                    if (cell.isEmpty())
                    {
                      errorCode = 3;
                      errorMessage = "JAVA CallOut ERROR : Mandatory ; Column Name = " + ((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(0)).trim() + "; row " + (i + validation_row + is_header);
                      isValidate = false;
                      break;
                    }
                  }
                  String typeOfColumn = ((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(1)).trim();
                  try
                  {
                    Double valDouble;
                    SimpleDateFormat output;
                    switch (typeOfColumn.toUpperCase())
                    {
                    case "INTEGER": 
                    case "INT": 
                    case "NUMBER": 
                    case "DECIMAL": 
                    case "DOUBLE": 
                        if(("").equals(cell) || null == cell)
                        {
                            cell = "0";
                      }
                      valDouble = Double.valueOf(Double.parseDouble(cell));
                      break;
                    case "DATE": 
                    case "DATETIME": 
                      Date valDate = null;
                      if ((typeFileUpper.equals("XLSX")) || (typeFileUpper.equals("XLS")))
                      {
                          output  = new SimpleDateFormat(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(11)).trim());
                          valDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(cell.trim());
                          cell    = output.format(valDate);
                      }
                      else
                      {
                        SimpleDateFormat sdf = new SimpleDateFormat(((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(11)).trim());
                        output  = new SimpleDateFormat("yyyy-MM-dd");
                        valDate = sdf.parse(cell.trim());
                        cell    = output.format(valDate);
                      }
                      break;
                    case "BOOLEAN": 
                      String newCell = cell.toUpperCase().trim();
                      if ((!newCell.equals("FALSE")) && (!newCell.equals("TRUE"))) {
                        throw new Exception("some error message");
                      }
                      break;
                    default: 
                        String valString = new String(cell+"");
                    }
                  }
                  catch (Exception ex)
                  {
                    String columnName = ((String)((ArrayList)rowsMapStructure.get(Integer.valueOf(j))).get(0)).trim();
                    errorCode = 4;
                    errorMessage = "JAVA CallOut ERROR : Column Format = " + typeOfColumn + "; Column Name =" + columnName + "; row " + (i + validation_row + is_header) + " and value =" + cell;
                    if ((i == 0) && (is_header == 0)) {
                      errorMessage = "JAVA CallOut ERROR : order ; column Name =" + columnName + " or the Format " + typeOfColumn + " is incorrect ; row " + (i + validation_row + is_header) + " and value =" + cell;
                    }
                    isValidate = false;
                    break;
                  }
                  if ((i != 0) || (is_header == 0))
                  {
                    columnNameElem.appendChild(document.createTextNode(cell));
                    offlineInfo.appendChild(columnNameElem);
                    if ("OEAS".equals(ServiceCode.toUpperCase())){
                        columnNameElem1.appendChild(document.createTextNode(cell));
                        offlineInfo1.appendChild(columnNameElem1);
                    }
                  }
                  if ("BLCB".equals(ServiceCode.toUpperCase()))
                  {
                    if (j == 0)
                    {
                      String valString = new String(cell);
                      int valStringSize = valString.length();
                      if(valStringSize < 18)
                      {
                          int cntZeroAdd = 18 - valStringSize ;
                          for(int m = 0  ;m < cntZeroAdd  ; m++)
                          {
                               valString = "0" +valString;
                           }
                      }
                      totalActNbrLeft += Integer.parseInt(valString.substring(0, 3));
                      totalActNbrRight += Integer.parseInt(valString.substring(valString.length() - 4, valString.length()));
                    }
                    else if (j == 2)
                    {
                      totalAmount = Double.valueOf(totalAmount.doubleValue() + Double.parseDouble(cell));
                    }
                    if (j == 3) 
                    {
                      System.out.println("Import Validation for i == " + i + " totalAmount =" + totalAmount + " totalActNbrRight =" + totalActNbrRight + " totalActNbrLeft =" + totalActNbrLeft);
                    }
                  }
                  else if("THAS".equals(ServiceCode.toUpperCase()))
                  {
                      if (j == 3)
                      {
                        totalAmount = Double.valueOf(totalAmount.doubleValue() + Double.parseDouble(cell));
                      }
                  }
                }
              }
            }
           else
            {
                errorCode = 10;
                errorMessage = "JAVA CallOut ERROR : Number of Column Incorect row = " + i;
                isValidate = false;
            }
            if ("UNRA".equals(ServiceCode.toUpperCase()))
            {
              Element fromCycleDateElem = document.createElement("startDate");
              fromCycleDateElem.appendChild(document.createTextNode(cycleDate));
              offlineInfo.appendChild(fromCycleDateElem);
              
              Element tillDateOfNormalElem = document.createElement("endDate");
              tillDateOfNormalElem.appendChild(document.createTextNode(excepDate));
              offlineInfo.appendChild(tillDateOfNormalElem);
              
              Element tillDateOfExceptionalElem = document.createElement("normalCycleEndDate");
              tillDateOfExceptionalElem.appendChild(document.createTextNode(normalDate));
              offlineInfo.appendChild(tillDateOfExceptionalElem);
            }
            else if ("OEAS".equals(ServiceCode.toUpperCase()))
            {
              Element accountType = document.createElement("accountType");
              accountType.appendChild(document.createTextNode("EABS"));
              offlineInfo.appendChild(accountType);
              
              Element accountType1 = document.createElement("accountType");
              accountType1.appendChild(document.createTextNode("EABI"));
              offlineInfo1.appendChild(accountType1);
              
              Element fromCycleDateElem = document.createElement("startDate");
              fromCycleDateElem.appendChild(document.createTextNode(cycleDate));
              offlineInfo.appendChild(fromCycleDateElem);
              
              Element fromCycleDateElem1 = document.createElement("startDate");
              fromCycleDateElem1.appendChild(document.createTextNode(cycleDate));
              offlineInfo1.appendChild(fromCycleDateElem1);
              
              Element tillDateOfNormalElem = document.createElement("endDate");
              tillDateOfNormalElem.appendChild(document.createTextNode(excepDate));
              offlineInfo.appendChild(tillDateOfNormalElem);
              
              Element tillDateOfNormalElem1 = document.createElement("endDate");
              tillDateOfNormalElem1.appendChild(document.createTextNode(excepDate));
              offlineInfo1.appendChild(tillDateOfNormalElem1);
            }
            if ((null != concatDuplicateCol) && (!concatDuplicateCol.equals("")) && (!concatDuplicateCol.equals(null))) 
            {
              if (concatRowsIsDuplicate.contains(concatDuplicateCol))
              {
                errorCode = 5;
                errorMessage = "JAVA CallOut ERROR : Duplicate row value in row =" + (i + validation_row + is_header);
                isValidate = false;
              }
              else
              {
                concatRowsIsDuplicate.add(concatDuplicateCol);
              }
            }
            if (!isValidate) {
              break;
            }
          }
        }
        else
        {
          errorCode = 10;
          errorMessage = "JAVA CallOut ERROR : Number of Column Incorect ;";
          isValidate = false;
        }
      }
      if ((isValidate) && ("BLCB".equals(ServiceCode.toUpperCase())))
      {
        System.out.println("Import Validation totalActNbrRight =" + totalActNbrRight);
        System.out.println("Import Validation totalActNbrLeft =" + totalActNbrLeft);
        BigInteger totalActNbrLeftBI = new BigInteger(totalActNbrLeft+"");
        BigInteger totalActNbrRightBI = new BigInteger(totalActNbrRight+"");
        totalActNbrLeftBI = totalActNbrLeftBI.multiply(new BigInteger(10000+""));
        BigInteger bgTotalActNbr = totalActNbrLeftBI.add(totalActNbrRightBI);
        BigInteger bgTotalAccountNumber = new BigInteger(totalAccountNumber);
        System.out.println("Import Validation bgTotalActNbr =" + bgTotalActNbr);
        System.out.println("Import Validation bgTotalAccountNumber from control file =" + bgTotalAccountNumber);
        if (bgTotalActNbr.compareTo(bgTotalAccountNumber) != 0)
        {
          errorCode = 6;
          errorMessage = "total Acount Number is incorrect ; ";
          isValidate = false;
        }
        System.out.println("Import Validation totalAmount =" + totalAmount);
        System.out.println("Import Validation totalAmount from control file =" + totalAmounts);
        BigInteger bgTotalAmount = new BigInteger((int)Math.round(totalAmount.doubleValue()) + "");
        BigInteger bgTotalAmounts = new BigInteger((int)Math.round(Double.parseDouble(totalAmounts)) + "");
        if (bgTotalAmount.compareTo(bgTotalAmounts) != 0)
        {
          errorCode = 7;
          errorMessage = errorMessage + "total Amounts  is incorrect ; ";
          isValidate = false;
        }
        System.out.println("Import Validation totalRows =" + totalRows);
        System.out.println("Import Validation numberOfRows =" + numberOfRows);
        if (totalRows != Integer.parseInt(numberOfRows))
        {
          errorCode = 8;
          errorMessage = errorMessage + "Number of rows is incorrect ; ";
          isValidate = false;
        }
      }
      else if ((isValidate) && ("THAS".equals(ServiceCode.toUpperCase())))
      {
        System.out.println("Import Validation totalAmount =" + totalAmount);
        System.out.println("Import Validation totalAmount from control file =" + totalAmounts);
        BigInteger bgTotalAmount = new BigInteger((int)Math.round(totalAmount.doubleValue()) + "");
        BigInteger bgTotalAmounts = new BigInteger((int)Math.round(Double.parseDouble(totalAmounts)) + "");
        if (bgTotalAmount.compareTo(bgTotalAmounts) != 0)
        {
          errorCode = 7;
          errorMessage = errorMessage + "total Amounts  is incorrect ; ";
          isValidate = false;
        }
        System.out.println("Import Validation totalRows =" + totalRows);
        System.out.println("Import Validation numberOfRows =" + numberOfRows);
        if (totalRows != Integer.parseInt(numberOfRows))
        {
          errorCode = 8;
          errorMessage = errorMessage + "Number of rows is incorrect ; ";
          isValidate = false;
        }
      }
      if (!isValidate)
      {
        Element rootError = documentError.createElement("PostOfflineDataRequest");
        rootError.setAttribute("xmlns", "http://www.omt.com/schemas/PostOfflineDataRequest");
        documentError.appendChild(rootError);
        

        Element successError = documentError.createElement("Success");
        successError.appendChild(documentError.createTextNode("false"));
        rootError.appendChild(successError);
        

        Element errorElemError = documentError.createElement("error");
        rootError.appendChild(errorElemError);
        
        Element errorCodeElemError = documentError.createElement("errorCode");
        errorCodeElemError.appendChild(documentError.createTextNode(errorCode + ""));
        errorElemError.appendChild(errorCodeElemError);
        
        Element errorMessageElemError = documentError.createElement("errorMessage");
        errorMessageElemError.appendChild(documentError.createTextNode(errorMessage));
        errorElemError.appendChild(errorMessageElemError);
        
        Element providerErrorCodeError = documentError.createElement("providerErrorCode");
        providerErrorCodeError.appendChild(documentError.createTextNode(""));
        errorElemError.appendChild(providerErrorCodeError);
        
        Element providerErrorMessageError = documentError.createElement("providerErrorMessage");
        providerErrorMessageError.appendChild(documentError.createTextNode(""));
        errorElemError.appendChild(providerErrorMessageError);
      }
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource domSource = null;
      if (isValidate) {
        domSource = new DOMSource(document);
      } else {
        domSource = new DOMSource(documentError);
      }
      StreamResult streamResult = new StreamResult(fileResponse);
      transformer.transform(domSource, streamResult);
      objToReturn = XmlObject.Factory.parse(fileResponse);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.out.println("Error Import Validation validateFile =" + ex.getMessage());
    }
    finally
    {
      if (fileResponse.exists()) {
        fileResponse.delete();
      }
    }
    return objToReturn;
  }
  
  public Map<Integer, ArrayList<String>> getRowsFromFile(String serviceCode, Map<Integer, ArrayList<String>> rowsMapStructure, byte[] bFile, File fileConvert, String typeFile, String delimiter, int validation_row, int validation_column)
  {
    ReadFile readFile = null;
    File file = null;
    Map<Integer, ArrayList<String>> rowsMap = null;
    try
    {
      System.out.println("Import Validation getRowsFromFile typeFile=" + typeFile);
      
        if(("PMIS").equals(serviceCode))
        {
            File fileUCS = Utility.convertByteToFile(bFile, fileConvert);
            String path = new File(".").getCanonicalPath();
            FileInputStream fis = new FileInputStream(fileUCS);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-16LE");
            Reader in = new BufferedReader(isr);
            file = new File(path + "/vendorPortal/repository/import/convertFileUCS_" + System.currentTimeMillis() + "." + typeFile);
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            Writer out = new BufferedWriter(osw);
            int ch;
            while ((ch = in.read()) > -1) {
                out.write(ch);
            }

            if (fileUCS.exists()) {
              fileUCS.delete();
            }
            out.close();
            in.close();
        }
        else
        {
            file = Utility.convertByteToFile(bFile, fileConvert);
        }
            
      switch (typeFile.toUpperCase())
      {
      case "XLSX": 
        readFile = new XLSXImpl();
        break;
      case "XLS": 
        readFile = new XLSImpl();
        break;
      case "CSV": 
        readFile = new CSVImpl();
        break;
      case "TXT": 
        readFile = new TXTImpl();
        break;
      default: 
        return null;
      }
      System.out.println("Import Validation getRowsFromFile getAbsolutePath=" + file.getAbsolutePath());
      System.out.println("Import Validation getRowsFromFile delimiter=" + delimiter);
      System.out.println("Import Validation getRowsFromFile validation_row=" + validation_row);
      System.out.println("Import Validation getRowsFromFile validation_column=" + validation_column);
      rowsMap = readFile.getRowsFromFile(serviceCode, rowsMapStructure, file, delimiter, validation_row, validation_column);
      if (file.exists()) {
        file.delete();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.out.println("Error Import Validation getRowsFromFile =" + ex.getMessage());
    }
    return rowsMap;
  }
  
  public Map<Integer, ArrayList<String>> getRowsFromXMLObject(XmlObject structure, String mainTagName, String nameColumnOrder)
  {
    Map<Integer, ArrayList<String>> rowsMapStructure = null;
    ArrayList<String> rowDataList = null;
    int order = 0;
    try
    {
      rowsMapStructure = new HashMap();
      rowDataList = new ArrayList();
      Node node = structure.getDomNode();
      NodeList lst = node.getChildNodes().item(0).getChildNodes();
      for (int i = 0; i < lst.getLength(); i++)
      {
        rowDataList = new ArrayList();
        if ((lst.item(i).getNodeName().split(":")[1].equals(mainTagName)) && (!"#text".equals(lst.item(i).getNodeName().split(":")[1])))
        {
          NodeList lstVal = lst.item(i).getChildNodes();
          for (int j = 0; j < lstVal.getLength(); j++) {
            if ((!"#text".equals(lstVal.item(j).getNodeName().split(":")[1])) && (lstVal.item(j).getChildNodes().getLength() != 0)) {
              if (nameColumnOrder.equals(lstVal.item(j).getNodeName().split(":")[1]))
              {
                order = Integer.parseInt(lstVal.item(j).getChildNodes().item(0).getNodeValue()) - 1;
                rowDataList.add(order + "");
              }
              else if (("IS_INBOUND".equals(lstVal.item(j).getNodeName().split(":")[1])) || ("IS_MANDATORY".equals(lstVal.item(j).getNodeName().split(":")[1])) || ("IS_DUPLICATE".equals(lstVal.item(j).getNodeName().split(":")[1])))
              {
                int bool = Integer.parseInt(lstVal.item(j).getChildNodes().item(0).getNodeValue());
                if (bool == 1) {
                  rowDataList.add("true");
                } else {
                  rowDataList.add("false");
                }
              }
              else
              {
                rowDataList.add(lstVal.item(j).getChildNodes().item(0).getNodeValue());
              }
            }
          }
          rowsMapStructure.put(Integer.valueOf(order), rowDataList);
        }
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      System.out.println("Error Import Validation getRowsFromXMLObject =" + ex.getMessage());
    }
    return rowsMapStructure;
  }
}
