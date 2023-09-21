package com.newgen.db.util;

import ISPack.*;
import ISPack.ISUtil.JPDBRecoverDocData;
import ISPack.ISUtil.JPISException;
import ISPack.ISUtil.JPISIsIndex;
import com.newgen.srvr.FunctionsWI;
import com.newgen.srvr.Log;
import com.newgen.srvr.XML.*;
import com.newgen.db.commons.WCUtilityHelper;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.newgen.niplj.fileformat.Tif6;

public class Common {

  private String mainCode = "";
  boolean keepRunning = true;
  public String processDefId;
  private static String str_inxml;
  private static String str_outxml;
  FunctionsWI objWF = new FunctionsWI();
  XMLParser xmlParser = new XMLParser();
  ArrayList components = new ArrayList();
  private BufferedWriter fw;
  DataInputStream dis = null;
  private String record = null;
  
  public void FireMailTrigger(String sCabinetName, String sSessionId,
                              String sParams, String sProcName) {
    String inXml = XMLGen.APProcedure(sCabinetName, sSessionId, "", sParams,
                                      sProcName);
    String outXml = objWF.execute(inXml);
  }

  public boolean moveToFolder(String sPath, String sDateFolderName,
                              String sBatchFolderName, String sBackUpFolderName,
                              File fFile, String sFile) {
    boolean bFileMoved = false;
    try {
      File fTemp = new File(sPath + System.getProperty("file.separator") +
                            sBackUpFolderName +
                            System.getProperty("file.separator") +
                            sDateFolderName);
      //System.out.println("fTemp is :"+fTemp);
      if (fTemp == null || !fTemp.isDirectory()) {
        fTemp.mkdir();
      }
      fTemp = new File(sPath + System.getProperty("file.separator") +
                       sBackUpFolderName + System.getProperty("file.separator") +
                       sDateFolderName + System.getProperty("file.separator") +
                       sBatchFolderName);
      if (fTemp == null || !fTemp.isDirectory()) {
        fTemp.mkdir();
      }
      fTemp = new File(sPath + System.getProperty("file.separator") +
                       sBackUpFolderName + System.getProperty("file.separator") +
                       sDateFolderName + System.getProperty("file.separator") +
                       sBatchFolderName + System.getProperty("file.separator") +
                       sFile);
      do {
        bFileMoved = fFile.renameTo(fTemp);
        if (!bFileMoved) {
          //commonlog.info("File "+sFile+" has not moved yet to  :"+fTemp);
        }
      }
      while (!bFileMoved);
      //commonlog.info("File "+sFile+" has been moved to  :"+fTemp);
      fTemp = null;
    }
    catch (Exception e) {
      //commonlog.info("Error in moving file "+sFile+" to the specified folder...."+e);
      return false;
    }

    return true;
  }

  public File[] getListingDirInPath(String sDirPath) {
    try {
      File folder = new File(sDirPath);
      File[] listOfFiles = folder.listFiles();
      return listOfFiles;
    }
    catch (Exception e) {
      return null;
    }

  }

  public void WriteLog(String filepath, String sMessage) {
    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(filepath +
          "\\Error.log", true));
      out.write(new java.util.Date() + "-" + sMessage + "\r\n");
      out.close();
    }
    catch (IOException e) {
    }
  }

  public String min2(String m) {
    return (m.length() == 1) ? ("0" + m) : m;
  }

  public void logUpload(String what) {
    try {
      fw.write(what + "\n");
      fw.flush();
    }
    catch (IOException io) {
      System.err.println(io.getMessage());
    }
  }

  public String IntroduceWorkItemWithoutDoc(String sProcessDefId,
                                            String sAttributes, Logger logger,
                                            String sActId, String sActName,
                                            String documentname,
                                            String sUploadDec[],String sValidReqd
                                            ) {
    String sDocuments = "";
    String sDoc = "";
    String processInstanceID = "";
    logger.debug("Function Executing : IntroduceWorkItemWithoutDoc");
    try {

      //if(! sUpload.equals("canNotUploaded"))
      //{
      sDocuments = "";
      sAttributes += "DSTUploadDecision" + ( (char) 21) + sUploadDec[0] +
          ( (char) 25);
      str_inxml = XMLGen.WFUploadWorkItem(WCUtilityHelper.cabinetName,
                                          WCUtilityHelper.sessionID,
                                          sProcessDefId, sValidReqd, sDocuments,
                                          sAttributes, sActId, sActName);
      logger.debug("WFUploadWorkItem input xml...." + str_inxml);
      str_outxml = objWF.execute(str_inxml);
      /////////////////////////////////////////////////////
      //Thread.sleep(1000);
      /////////////////////////////////////////////////////
      logger.debug("WFUploadWorkItem output xml is :" + str_outxml);
      xmlParser.setInputXML(str_outxml);
      mainCode = xmlParser.getValueOf("MainCode");
      if (mainCode.equalsIgnoreCase("-50146")) {
        str_inxml = XMLGen.WFUploadWorkItem(WCUtilityHelper.cabinetName,
                                            WCUtilityHelper.sessionID,
                                            sProcessDefId, sValidReqd, sDocuments,
                                            sAttributes, sActId, sActName);
        logger.debug("WFUploadWorkItem input xml...." + str_inxml);
        str_outxml = objWF.execute(str_inxml);
        xmlParser.setInputXML(str_outxml);
        mainCode = xmlParser.getValueOf("MainCode");
      }
      processInstanceID = xmlParser.getValueOf("processInstanceID");
      if (mainCode.equals("0") && sDoc.equals("Success")) {
        processInstanceID = xmlParser.getValueOf("processInstanceID");
        //ItemIndex=xmlParser.getValueOf("FolderIndex");
        //System.out.println("Case processed");
        return "success~" + processInstanceID;
      }
      else if (mainCode.equals("50")) {
      	
          processInstanceID = "WI-Already-Exists";
        }
      else if (!mainCode.equals("0")) {
        logger.debug("Error in creating workitem");
        sUploadDec[1] += "In Complete : Case is created for file ";
        sUploadDec[0] = "InComplete";
        return ("Exception: Error in creating workitem~");
      }
      System.out.println("Case processed");
      return "success~" + processInstanceID;
      //	}

    }
    catch (Exception e) {
      logger.debug("Exception occured while processing the File...." + e);
      return ("Exception:~" + e.toString());

    }
  }

  public String IntroduceWorkItem(String src, String dest, String sFileName,
                                  String sProcessDefId, String sAttributes,
                                  Logger logger, String sActId, String sActName,
                                  String documentname, String sUploadDec[],
                                  String sValidReqd, int delay) {
    short volIdShort;
    long lFileLength = 0;
    String sUpload = "";
    String strNoOfPages = "";
    String sDocuments = "";
    String sDoc = "", sFilePath_sm = "";
    String processInstanceID = "";
    logger.debug("Function Executing : IntroduceWorkItem");
    JPISIsIndex ISINDEX = new JPISIsIndex();
    JPDBRecoverDocData JPISDEC = new JPDBRecoverDocData();
    try {
      File fFile = new File(src);
      logger.debug("TIF File Path" + src);
      if (fFile.exists()) 
      {
        //Common.fileMoveToFolder(src, dest, sFileName, sFileName);
        fFile = new File(src);
        volIdShort = Short.parseShort(WCUtilityHelper.VolumeID);
        sFilePath_sm = src;
        strNoOfPages = String.valueOf(Tif6.getPageCount(sFilePath_sm));
        lFileLength = fFile.length();
        JPISDEC.m_cDocumentType = 'I';
        JPISDEC.m_nDocumentSize = (int) lFileLength;
        JPISDEC.m_sVolumeId = volIdShort;
        sDoc = AddDocument(WCUtilityHelper.jtsIP, (short) (WCUtilityHelper.jtsPort), WCUtilityHelper.cabinetName, volIdShort, sFilePath_sm, JPISDEC, "",
                           ISINDEX, logger);

        /////////////////////////////////////////////////////
        Thread.sleep(1000 * delay);
        /////////////////////////////////////////////////////
        //File temp=new File(sDoc);
        //System.out.println("DocIDX ="+DocIDX);
        if (!sDoc.equals("Success"))
        {
          sUploadDec[1] += "  In Complete :Document is not Added to SMS server";
          sUploadDec[0] = "InComplete";
          logger.debug("Error in adding document to server.");
          sUpload = "canNotUploaded";
          return ("Exception: Error in adding document to server.");
          //To set a atrribute for successful doc addition
        }
        else 
        {
          logger.debug("Document added successfully.");
          //To set a atrribute for Failure doc addition... for Error orphan Workstep
        }
      }
      else
      {
        logger.debug("Document does not exist for " + sFileName);
        sUploadDec[1] += "  In Complete : Document " + sFileName +
            " does not exist";
        sUploadDec[0] = "InComplete";
        sUpload = "canNotUploaded";
        //To set a atrribute for Failure doc addition... for Error orphan Workstep
      }
      if (!sUpload.equals("canNotUploaded")) {
        sAttributes += "DSTUploadDecision" + ( (char) 21) + sUploadDec[0] + ( (char) 25);
        sDocuments = documentname + ( (char) 21) + String.valueOf(ISINDEX.m_nDocIndex) + "#" + String.valueOf(ISINDEX.m_sVolumeId) + ( (char) 21) +
            strNoOfPages + ( (char) 21) + lFileLength + ( (char) 25);
        logger.debug(sValidReqd);
        str_inxml = XMLGen.WFUploadWorkItem(WCUtilityHelper.cabinetName, WCUtilityHelper.sessionID, sProcessDefId, sValidReqd, sDocuments, sAttributes, sActId,
                                            sActName);
        logger.debug("WFUploadWorkItem input xml...." + str_inxml);
        str_outxml = objWF.execute(str_inxml);
        /////////////////////////////////////////////////////
        //Thread.sleep(1000 * );
        /////////////////////////////////////////////////////
        logger.debug("WFUploadWorkItem output xml is :" + str_outxml);
        xmlParser.setInputXML(str_outxml);
        mainCode = xmlParser.getValueOf("MainCode");
        if (mainCode.equalsIgnoreCase("-50146")) {
          str_inxml = XMLGen.WFUploadWorkItem(WCUtilityHelper.cabinetName,
                                              WCUtilityHelper.sessionID,
                                              sProcessDefId, sValidReqd,
                                              sDocuments, sAttributes, sActId,
                                              sActName);
          logger.debug("WFUploadWorkItem input xml...." + str_inxml);
          str_outxml = objWF.execute(str_inxml);
          logger.debug("WFUploadWorkItem str_outxml xml...." + str_outxml);
          xmlParser.setInputXML(str_outxml);
          mainCode = xmlParser.getValueOf("MainCode");
        }
        processInstanceID = xmlParser.getValueOf("processInstanceID");
        if (mainCode.equals("0") && sDoc.equals("Success")) 
        {
          /*					processInstanceID=xmlParser.getValueOf("processInstanceID");
               ItemIndex=xmlParser.getValueOf("FolderIndex");

               str_inxml="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                      + "<NGOAddDocument_Input>\n"
                      + "<Option>NGOAddDocument</Option>\n"
           + "<CabinetName>" + WCUtilityHelper.cabinetName +"</CabinetName>\n"
           + "<UserDBId>" + WCUtilityHelper.sessionID + "</UserDBId>\n"
                      +"<GroupIndex>0</GroupIndex>\n"
           +"<ParentFolderIndex>"+ItemIndex+"</ParentFolderIndex>\n"
                      +"<AccessType></AccessType>\n"
           +"<DocumentName>"+documentname+"</DocumentName>\n"
                      +"<CreatedByAppName>TIF</CreatedByAppName>\n"
           +"<VolumeIndex>"+WCUtilityHelper.VolumeID+"</VolumeIndex>\n"
                      +"<FilePath>"+fFile.getAbsolutePath()+"</FilePath>"
                      +"<Comment>"+fFile.getName()+"</Comment>"
           +"<ISIndex>"+DocIDX+"#"+WCUtilityHelper.VolumeID+"#</ISIndex>"
           +"<NoOfPages>1</NoOfPages>"+"<DocumentType>N</DocumentType>"
           +"<DocumentSize>"+lFileLength+"</DocumentSize>"+"</NGOAddDocument_Input>";
               logger.debug("WFUploadWorkItem input xml...."+str_inxml);
               str_outxml = objWF.execute(str_inxml);
               xmlParser.setInputXML(str_outxml);
               mainCode=xmlParser.getValueOf("Status");
               logger.debug("add docs output xml is :"+str_outxml);
               if (mainCode.equals("0"))
               {
                //logger.debug("add docs output xml is :"+str_outxml);
               }
               else
               {
           logger.debug("Error in add documents to workitem"+str_outxml);
           sUploadDec[1]+="  In Complete : Document is not attached for file "+sFileName+" with WI "+processInstanceID;
                   sUploadDec[0]="InComplete";
                return  ("Exception: Error in add documents to workitem~");
               }
           */
        }
        else if (mainCode.equals("50")) 
        {
        	
          processInstanceID = "WI-Already-Exists";
        }
        else if (!mainCode.equals("0")) 
        {
          logger.debug("Error in creating workitem");
          sUploadDec[1] += "  In Complete : Case is created for file " +
              sFileName;
          sUploadDec[0] = "InComplete";
          return ("Exception: Error in creating workitem~");
        }
        System.out.println("Case processed");
        return "success~" + processInstanceID;
      }
      else 
      {
        logger.debug("File Not added to SMS....");
        return ("Exception:~DocNotFound");
      }

    }
    catch (Exception e) {
      logger.info("Exception occured while processing the File...." + e);
      return ("Exception:~" + e.toString());

    }
  }

  public synchronized String AddDocument(String sjtsIP, short jtsPort,
                                         String scabinetName, short VolId,
                                         String FilePath,
                                         JPDBRecoverDocData JPISDEC, String s3,
                                         JPISIsIndex jpisisindex, Logger logger)

  {
    try {
      CPISDocumentTxn.AddDocument_MT(null, sjtsIP, jtsPort, scabinetName, VolId,
                                     FilePath, JPISDEC, "", jpisisindex);
      return "Success";

    }
    catch (JPISException e) {
      logger.debug("JPISException occurred while processing file...." +
                         e.toString());
      mProcedureMailFire("Error Happened While Adding Docs"," SMS Not Running. Pls verify.",logger);
      return ("Exception:" + e.toString());
      //logger.debug("SMS Errror....");
      

    }
    catch (Exception e) {
      logger.info("Exception occured while processing document...." +
                         e.toString());
      return ("Exception:" + e.toString());

    }

  }

  //
  public void CreateTempFolder(String sPath) {
    File files = new File(sPath);
    //System.out.println("Final Storage "+sPath+" "+files.exists());
    if (!files.exists()) {
      files.mkdir();
    }
  }

  ////

  String mSelectQueryExecute(String strQuery, Logger logger) {
	  logger.debug("Ambuj" +strQuery);  
	strQuery = XMLGen.APSelectWithColumnNames(WCUtilityHelper.cabinetName,WCUtilityHelper.sessionID,strQuery);
    logger.debug(strQuery);
    strQuery = objWF.execute(strQuery);
    logger.debug(strQuery);
    xmlParser.setInputXML(strQuery);
    String sMainCode = xmlParser.getValueOf("MainCode");
    if (sMainCode.equals("0") &&
        Integer.parseInt(xmlParser.getValueOf("TotalRetrieved")) > 0) {
      return strQuery;
    }
    return "";
  }

  public String mProcedureMailFire(String sMailSub, String sMailContent,
                                   Logger logger) {
    String sParam = "select Count(*)as count from WFMAILQUEUEHISTORYTABLE with(nolock) where mailsubject='" +
        sMailSub + "' and datediff(hh,insertedtime,getdate())<2";
    sParam = XMLGen.APSelectWithColumnNames(WCUtilityHelper.cabinetName,
                                            WCUtilityHelper.sessionID, sParam);
    logger.debug("In Xml:" + sParam);
    sParam = objWF.execute(sParam);
    logger.debug("Out Xml: " + sParam);
    xmlParser.setInputXML(sParam);
    String sMainCode = xmlParser.getValueOf("MainCode");
    if (sMainCode.equals("0")) {
      xmlParser.setInputXML(sParam);
      sParam = xmlParser.getValueOf("count");
    }
    else {
      return "Error";
    }
    if (Integer.parseInt(sParam) < 1) {
      sParam = "'" + sMailSub + "','" + sMailContent + "'";
      String sInXML = XMLGen.APProcedure(WCUtilityHelper.cabinetName,
                                         WCUtilityHelper.sessionID, "", sParam,
                                         "NG_ME_SendMail");
      logger.debug(sInXML);
      sInXML = objWF.execute(sInXML);
      logger.debug(sInXML);
      xmlParser.setInputXML(sInXML);
      sInXML = xmlParser.getValueOf("MainCode");
      if (sInXML.equals("0")) {
        return "sucess fully inserted";
      }
      else {
        return "Error";
      }
    }
    return "already Existing Entry in MailQueuetable";

  }
  //added by chhavi
  public String mProcedureMailFire_UploadUtility(String sMailSub, String sMailContent,
          Logger logger) {
String sParam = "select Count(*)as count from WFMAILQUEUEHISTORYTABLE with(nolock) where mailsubject='" +
sMailSub + "' and datediff(hh,insertedtime,getdate())<2";
sParam = XMLGen.APSelectWithColumnNames(WCUtilityHelper.cabinetName,
                   WCUtilityHelper.sessionID, sParam);
logger.debug("In Xml:" + sParam);
sParam = objWF.execute(sParam);
logger.debug("Out Xml: " + sParam);
xmlParser.setInputXML(sParam);
String sMainCode = xmlParser.getValueOf("MainCode");
if (sMainCode.equals("0")) {
xmlParser.setInputXML(sParam);
sParam = xmlParser.getValueOf("count");
}
else {
return "Error";
}
if (Integer.parseInt(sParam) < 1) {
sParam = "'" + sMailSub + "','" + sMailContent + "'";
String sInXML = XMLGen.APProcedure(WCUtilityHelper.cabinetName,
                WCUtilityHelper.sessionID, "", sParam,
                "NG_ME_SendMail_uploadUtility");
logger.debug(sInXML);
sInXML = objWF.execute(sInXML);
logger.debug(sInXML);
System.out.println("chhavi output of mail procedure ---------"+sInXML);
xmlParser.setInputXML(sInXML);
sInXML = xmlParser.getValueOf("MainCode");

System.out.println("chhavi value of maincode ---------"+sInXML);
if (sInXML.equals("0")) {
return "sucess fully inserted";
}
else {
return "Error";
}
}
return "already Existing Entry in MailQueuetable";

}


  public String mProcedureMailFireWithoutChk(String sMailSub, String sMailContent, Logger logger) 
  {
		String sParam = "";
		sParam = "'" + sMailSub + "','" + sMailContent + "'";
		String sInXML = XMLGen.APProcedure(WCUtilityHelper.cabinetName,
		                WCUtilityHelper.sessionID, "", sParam,
		                "NG_ME_SendMail");
		logger.debug(sInXML);
		sInXML = objWF.execute(sInXML);
		logger.debug(sInXML);
		xmlParser.setInputXML(sInXML);
		sInXML = xmlParser.getValueOf("MainCode");
		if (sInXML.equals("0")) {
		return "sucess fully inserted";
		}
		else {
		return "Error";
		}
		
		//return "already Existing Entry in MailQueuetable";

}
  
  

  public void reconnectToWorkflow() {
    try {
      objWF.disconnectFromWorkFlow();
      objWF.disconnectFromServer();
    }
    catch (Exception ex) {
      //logger.debug(ex.toString());


    }
    try {
      if (objWF.connectToServer()) {
        String str = objWF.connectToWorkFlow("N");
        String temp[] = str.split("~");
        if (!temp[0].equals("0")) {
          Thread.sleep(30000);
          if (keepRunning) {
            reconnectToWorkflow();
          }
        }
      }
    }
    catch (Exception ex) {
      //logger.debug(ex.toString());
      //commonlog.info(ex.toString());
    }
  }

  public static Date adjustDateBy(Date d, int numberOfDays) {
    java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
    cal.setTime(d);
    cal.add(java.util.GregorianCalendar.DATE, numberOfDays);

    return cal.getTime();
  }

  public static String getDateString() {
    Calendar calendar = new GregorianCalendar();
    return "" + calendar.get(Calendar.DAY_OF_MONTH) + "-" +
        (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR);
  }

  public static boolean fileMoveToFolder(String src, String dest,
                                         String srcFileName,
                                         String destFileName) {
    java.util.Date date = new java.util.Date();
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh-mm-ss");
    String time = sdf.format(date);
    boolean retStatus = false;
    try {
      String folder = dest;
      File f = new File(folder);
      //System.out.println("-----"+f.exists());
      if (!f.exists()) {
        f.mkdir();
      }
      //.out.println(dest + File.separator + destFileName);
      //System.out.println(src + File.separator + srcFileName);

      File f2 = new File( (dest + File.separator + destFileName).trim());
      if (!srcFileName.equals("")) {
        srcFileName = File.separator + srcFileName;
      }
      File f1 = new File( (src + srcFileName).trim());
      //System.out.println(f2.exists());
      if (!f2.exists()) {
    	  retStatus=f1.renameTo(f2);
    	  if(!retStatus)
    	  retStatus=mMoveFileInCurrentDirectory(src,srcFileName,destFileName);
    	  return retStatus;
      }
      else {
        int pos = srcFileName.indexOf(".");
        String s1 = srcFileName.substring(0, pos);
        String s2 = srcFileName.substring(pos + 1, srcFileName.length());
        f1.renameTo(new File(dest + File.separator + s1 + "_(" + getDateString() +
                             "_" + time + ")." + s2));

      }
      retStatus = true;
    }
    catch (Exception e) {
      //System.out.println(e.toString());

      retStatus = false;
    }
    return retStatus;
  }

  public static boolean fileMoveToFolderWithoutSrc(String src, String dest,
      String srcFileName, String destFileName) {
    java.util.Date date = new java.util.Date();
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh-mm-ss");
    String time = sdf.format(date);
    boolean retStatus = false;
    try {
      String folder = dest;
      File f = new File(folder);
      //System.out.println("-----"+f.exists());
      if (!f.exists()) {
        f.mkdir();
      }
      //System.out.println(dest + File.separator + destFileName);
      //System.out.println(src);

      File f2 = new File( (dest + File.separator + destFileName).trim());
      File f1 = new File( (src.trim()));
      //System.out.println(f2.exists());
      if (!f2.exists()) {
    	  retStatus=f1.renameTo(f2);
    	  if(!retStatus)
    	  retStatus=mMoveFileInCurrentDirectory(src,srcFileName,destFileName);
    	  return retStatus;
        
      }
      else {
        int pos = srcFileName.indexOf(".");
        String s1 = srcFileName.substring(0, pos);
        String s2 = srcFileName.substring(pos + 1, srcFileName.length());
        f1.renameTo(new File(dest + File.separator + s1 + "_(" + getDateString() +"_" + time + ")." + s2));

      }
      retStatus = true;
    }
    catch (Exception e) {
      //System.out.println(e.toString());
      retStatus = false;
    }
    return retStatus;
  }

  public String getBatchString() {
    Calendar calendar = new GregorianCalendar();
    return "" + calendar.get(Calendar.YEAR) + "-" +
        (calendar.get(Calendar.MONTH) + 1) + "-" +
        calendar.get(Calendar.DAY_OF_MONTH);
  }

  public static boolean  mMoveFileInCurrentDirectory(String src,String srcFileName, String destFileName) 
  {
	    java.util.Date date = new java.util.Date();
	    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh-mm-ss");
	    String time = sdf.format(date);
	    String dest=System.getProperty("user.dir") +System.getProperty("file.separator") +"ErrorFiles";
	    boolean retStatus = false;
	    try {
	      String folder = dest;
	      File f = new File(folder);
	      //System.out.println("-----"+f.exists());
	      if (!f.exists()) {
	        f.mkdir();
	      }
	      dest=dest+System.getProperty("file.separator") +getDateString();
	      f = new File(dest);
	      //System.out.println("-----"+f.exists());
	      if (!f.exists()) {
	        f.mkdir();
	      }
	      //System.out.println(dest + File.separator + destFileName);
	      //System.out.println(src);

	      File f2 = new File( (dest + File.separator + destFileName).trim());
	      File f1 = new File( (src.trim()));
	      //System.out.println(f2.exists());
	      if (!f2.exists()) {
	        return f1.renameTo(f2);
	      }
	      else {
	        int pos = srcFileName.indexOf(".");
	        String s1 = srcFileName.substring(0, pos);
	        String s2 = srcFileName.substring(pos + 1, srcFileName.length());
	        f1.renameTo(new File(dest + File.separator + s1 + "_(" + getDateString() +"_" + time + ")." + s2));

	      }
	      retStatus = true;
	    }
	    catch (Exception e) 
	    {
	    	retStatus = false;
	    }
	   return retStatus;
  }

  
  
  
  public boolean mCheckForFolderDeleteTime(String sCompletePath,int noofdays,Logger logger ) {
	   
	  try
	  {
		File fTempFile=new File(sCompletePath);
	    long modified_time=fTempFile.lastModified();
	    Calendar calendar1 = Calendar.getInstance();
	    Calendar calendar2 = Calendar.getInstance();
	    calendar1.setTime(new Date());
	    calendar2.setTimeInMillis(modified_time);
	    long milliseconds1 = calendar1.getTimeInMillis();
	    long milliseconds2 = calendar2.getTimeInMillis();
	    long diff = milliseconds1 - milliseconds2;
	    //long diffHours = diff / (60 * 60 * 1000);
	    long diffDays = diff / (24 * 60 * 60 * 1000);
	    logger.debug("No of days difference between dir and current date :" + diffDays);
	    if (diffDays > noofdays) 
	    {
	    	logger.debug("Directory is ready for delte operation :" + diffDays);
	    	return fTempFile.delete();
	    	
	    }
	    else
	    return false;	    
	  }
	  catch(Exception e)
	  {
		  logger.info("Some exception: While folder delete method "+e);
		  return false;	 
	  }
	  }
}
