/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package FCU;

import Log4j.CustomLogger;
import common.executeApi;
import connection.DBConnection;
import dependency.ReadProperty2;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 *
 * @author t.deepak
 */
public class startFCUOperation {

    public static String SNO = "";
    public static String REQ_FROM = "";
    public static String CIF = "";
    public static String APPLICANT_TYPE = "";
    public static String ADDRESS_TYPE = "";
    public static String REQUEST = "";
    public static String WINAME = "";
    public static String RESPONSEDATA = "";
    public static String RESPONSE = "";
    public static String UTILITY_COMMENTS = "";
    public static String UTILITY_STATUS = "";
    public static String THRESHOLD = "";
    public static String SUBMIT_DATETIME = "";

    public static String Utility_comments = "";
    public static int newThreshold = 0;
    public static ReadProperty2 property;

    public static String FCU_TOKEN = "";
    public static int TOKEN_CNT = 0;

    public static org.apache.log4j.Logger mXMLLogger;
    public static org.apache.log4j.Logger mStatusLogger;
    public static org.apache.log4j.Logger mErrLogger;

    public static void startFCU() {
        CustomLogger.InitLog(FCU.constants.GEN_LOG_PATH);

        mStatusLogger = CustomLogger.mLOSStatusLogger;
        mXMLLogger = CustomLogger.mLOSXMLLogger;
        mErrLogger = CustomLogger.mLOSErrLogger;
        String retStr = "";

        try {
            property = ReadProperty2.getInstance(FI_Operations.constants.CONFIG_PATH);
            String utlityOperationQry = property.getvalue("6"
                    + "");
            List<List<String>> utilityRows = DBConnection.GetMulDatabaseConnection("Select", utlityOperationQry, mStatusLogger, mErrLogger);
            mStatusLogger.info("utilityRows ======>" + utilityRows);

            for (int ui = 0; ui < utilityRows.size(); ui++) {
                SNO = utilityRows.get(ui).get(0);
                REQ_FROM = utilityRows.get(ui).get(1);
                CIF = utilityRows.get(ui).get(2);
                APPLICANT_TYPE = utilityRows.get(ui).get(3);
                ADDRESS_TYPE = utilityRows.get(ui).get(4);
                REQUEST = utilityRows.get(ui).get(5);
                WINAME = utilityRows.get(ui).get(6);
                RESPONSEDATA = utilityRows.get(ui).get(7);
                RESPONSE = utilityRows.get(ui).get(8);
                UTILITY_COMMENTS = utilityRows.get(ui).get(9);
                UTILITY_STATUS = utilityRows.get(ui).get(10);
                THRESHOLD = utilityRows.get(ui).get(11);
                SUBMIT_DATETIME = utilityRows.get(ui).get(12);

                UTILITY_COMMENTS = "";
                UTILITY_STATUS = "P";
                newThreshold = Integer.parseInt(THRESHOLD);
                newThreshold++;
                loadFCUcase();
            }
        } catch (Exception ex) {
            UTILITY_COMMENTS = "Exception :: " + ex.getMessage().toString();
            mErrLogger.info("Exception startFI ===> " + ex.toString());
        }
        updateUtilityComments();
    }

    public static boolean getFCUToken() {
        boolean ret = false;
        String userName = "";
        String password = "";
        String grantType = "";
        FCU_TOKEN = "";
        try {
            userName = property.getvalue("FCU_Token_userName");
            password = property.getvalue("FCU_Token_password");
            grantType = property.getvalue("FCU_Token_grantType");

            String tokenJson = property.getvalue("FCU_Token_Json");

            tokenJson = tokenJson.replaceAll("##userName##", userName);
            tokenJson = tokenJson.replaceAll("##password##", password);
            tokenJson = tokenJson.replaceAll("##grantType##", grantType);

            String apiMeta = property.getvalue("FCU_Token_apiMeta");
            String response = executeApi.apiCall(tokenJson, apiMeta, "", mStatusLogger, mErrLogger);
            JSONObject respJson = new JSONObject(response);

            if (respJson.has("access_token")) {
                FCU_TOKEN = respJson.get("access_token").toString();
                mStatusLogger.info("Access token fetched succesfully");
                ret = true;
            } else if (respJson.has("error")) {
                mStatusLogger.info("Error in getting token ======>" + respJson.get("error_description").toString());
            }

        } catch (Exception ex) {
            UTILITY_COMMENTS = "Exception in  FCU get Token";
            mErrLogger.info("Exception in  FCU get Token ===> " + ex.toString());
        }
        return ret;
    }

    public static String createApplication() {
        String retStr = "fail";
        String qkey = "";
        String qryKey = "";
        String finalJson = "";
        try {

            finalJson = property.getvalue("FCU_createApp_Json");
            String apiMeta = property.getvalue("FCU_createApp_apiMeta");
            String attr[] = property.getvalue("attributes").split(",");
            for (int i = 0; i < attr.length; i++) {
                String key = attr[i];
                qkey = "Q_" + key;
                qryKey = "Qry_" + key;
                String temp = loadData(qkey, qryKey);
                String ToBeReplace = "(?i)##" + key + "##";
                finalJson = finalJson.replaceAll(key, temp);
            }
            String response = executeApi.apiCall(finalJson, apiMeta, FCU_TOKEN, mStatusLogger, mErrLogger);
            JSONObject respJson = new JSONObject(response);
            if (respJson.has("Status")) {

                if ("success".equalsIgnoreCase(respJson.get("Status").toString())) {

                    JSONArray msg = (JSONArray) respJson.get("Message");
                    String tempComments = "";
                    for (int k = 0; k < msg.length(); k++) {
                        tempComments = tempComments + msg.get(k).toString();
                        if (msg.get(k).toString().contains("ResponseInsertUpdateApplications")) {
                            retStr = "success";
                            UTILITY_COMMENTS = "FCU Application created successfully.";
                            UTILITY_STATUS = "C";
                        }
                    } // end of for loop.

                    if (!(retStr.equalsIgnoreCase("success"))) {
                        UTILITY_COMMENTS = "FCU Application failed due tov:: " + tempComments;
                    }
                }
            }

        } catch (Exception ex) {
            UTILITY_COMMENTS = "Exception :: " + ex.getMessage().toString();
            mErrLogger.info("Exception in  FCU  ===> " + ex.toString());
        }
        return retStr;
    }

    public static String uploadDocFCU() {
        String retStr = "fail";
        String qkey = "";
        String qryKey = "";
        String finalJson = "";
        try {

            finalJson = property.getvalue("FCU_uploadDoc_Json");
            String apiMeta = property.getvalue("FCU_uploadDoc_apiMeta");
            String attr[] = property.getvalue("attributes").split(",");
            String IP = 
            retStr = dependency.CustomRequest.downloadUploadDocument("192.168.12.27", "8080", "cubbpmdev", "1", "1",
                    "32522", "Aadhar Card", "Deepak", "C:\\Users\\t.deepak\\Documents\\NetBeansProjects\\BS_Utility\\config\\docCheck\\downloadDocs\\",
                    "-1907099659", "26700", "",
                    mXMLLogger, mXMLLogger, mXMLLogger);

        } catch (Exception ex) {
            UTILITY_COMMENTS = "Exception :: " + ex.getMessage().toString();
            mErrLogger.info("Exception addFIcase ===> " + ex.toString());
        }
        return retStr;
    }

    public static String loadData(String qkey, String qryKey) {
        String retStr = "";
        JSONObject attrJSON = null;
        mStatusLogger.info("Starting loadData with myKey ==> " + qkey + " and qryKey ==> " + qryKey);
        try {
            String qry = property.getvalue(qryKey);
            qry = qry.replaceAll("##myPid##", WINAME);
            qry = qry.replaceAll("##myApptype##", APPLICANT_TYPE);
            qry = qry.replaceAll("##myAddress##", APPLICANT_TYPE);
            List<List<String>> qryRows = DBConnection.GetMulDatabaseConnection("Select", qry, mStatusLogger, mErrLogger);
            mStatusLogger.info("loadData  qry result for  :: " + qryKey + " ::: " + qryRows);
        } catch (Exception ex) {
            UTILITY_COMMENTS = "Exception :: " + ex.getMessage().toString();
            mErrLogger.info("Exception addFIcase ===> " + ex.toString());
        }
        return retStr;
    }

    public static String updateUtilityComments() {
        String retStr = "";
        try {
            String updtQry = "update table LOS_CUB_UTILITY_FI set UTILITY_STATUS = '" + UTILITY_STATUS + "', UTILITY_COMMENTS = '"
                    + UTILITY_COMMENTS + "', RESPONSEDATA = '" + RESPONSEDATA + "', RESPONSE='" + RESPONSE + "'"
                    + ", THRESHOLD = '" + newThreshold + "' where SNO = " + SNO;
            List<List<String>> updateRs = DBConnection.GetMulDatabaseConnection("Update", updtQry, mStatusLogger, mErrLogger);
            mStatusLogger.info("utilityRows ======>" + updateRs);
        } catch (Exception ex) {
            mErrLogger.info("Exception updateUtilityComments ===> " + ex.toString());
        }
        return retStr;
    }

    public static String updateUtilitythreshold() {
        String retStr = "";
        try {
            String updtQry = "update table LOS_CUB_UTILITY_FI set THRESHOLD = '" + newThreshold + "' where SNO = " + SNO;
            List<List<String>> updateRs = DBConnection.GetMulDatabaseConnection("Update", updtQry, mStatusLogger, mErrLogger);
            mStatusLogger.info("utilityRows ======>" + updateRs);
        } catch (Exception ex) {
            mErrLogger.info("Exception updateUtilitythreshold ===> " + ex.toString());
        }
        return retStr;
    }
}
