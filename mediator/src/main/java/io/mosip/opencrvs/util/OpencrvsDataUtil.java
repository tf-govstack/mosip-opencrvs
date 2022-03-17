package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.ReceiveDto;
import io.mosip.opencrvs.error.ErrorCode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class OpencrvsDataUtil {
    private static Logger LOGGER = LogUtil.getLogger(OpencrvsDataUtil.class);

    @Value("${IDSchema.Version}")
    private String idschemaVersion;
    @Value("${opencrvs.data.gender.default.lang.code}")
    private String genderDefaultLangCode;
    @Value("${opencrvs.data.lang.code.mapping}")
    private String langCodeMapping;
    @Value("${opencrvs.data.dummy.address.line1}")
    private String dummyAddressLine1;
    @Value("${opencrvs.data.dummy.address.line2}")
    private String dummyAddressLine2;
    @Value("${opencrvs.data.dummy.address.line3}")
    private String dummyAddressLine3;
    @Value("${opencrvs.data.dummy.region}")
    private String dummyRegion;
    @Value("${opencrvs.data.dummy.province}")
    private String dummyProvince;
    @Value("${opencrvs.data.dummy.city}")
    private String dummyCity;
    @Value("${opencrvs.data.dummy.zone}")
    private String dummyZone;
    @Value("${opencrvs.data.dummy.postal.code}")
    private String dummyPostalCode;

    public ReceiveDto buildIdJson(String opencrvsRequestBody){
        JSONObject opencrvsJSONRequest;
        JSONArray contextEntries;
        JSONObject patient;
        JSONObject task;
        try{
            opencrvsJSONRequest = new JSONObject(opencrvsRequestBody);
            contextEntries = opencrvsJSONRequest.getJSONObject("event").getJSONArray("context").getJSONObject(0).getJSONArray("entry");
            patient = (JSONObject)returnOutputOfArrayIfInputIsValue(contextEntries,"resource.resourceType","Patient","resource");
            task = (JSONObject)returnOutputOfArrayIfInputIsValue(contextEntries,"resource.resourceType","Task","resource");
            if(patient == null){
                LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"ReceiveDto::build()","Error processing patient. Got null patient");
                throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
            }
            if(task == null){
                LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"ReceiveDto::build()","Error processing task. Got null task");
                throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
            }
        } catch(JSONException je){
            LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"ReceiveDto::build()", ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
        }

        ReceiveDto returner = new ReceiveDto();

        //returner.setIdValue(rid);
        //returner.setCenterId(centerId);
        //returner.setMachineId(machineId);

        returner.setOpencrvsId(getOpencrvsIdFromPatientBody(patient));

        String fullName = getFullNameFromPatientBody(patient);

        returner.setIdentityJson("{"+
            "\"introducerBiometrics\":\"null\"," +
            "\"identity\":{" +
                "\"IDSchemaVersion\":" + idschemaVersion + "," +
                "\"fullName\":" + fullName + "," +
                "\"dateOfBirth\":" + getDOBFromPatientBody(patient) + "," +
                //"\"2003/10/05\""
                "\"gender\":" + getGenderFromPatientBody(patient) +"," +
                "\"addressLine1\":" + dummyAddressLine1 + "," +
                "\"addressLine2\":" + dummyAddressLine2 + "," +
                "\"addressLine3\":" + dummyAddressLine3 + "," +
                "\"region\":" + dummyRegion + "," +
                "\"province\":" + dummyProvince + "," +
                "\"city\":" + dummyCity + "," +
                "\"zone\":" + dummyZone + "," +
                "\"postalCode\":" + dummyPostalCode + "," +
                "\"phone\":" + getPhoneFromTaskBody(task) + "," +
                "\"email\":\"" + getEmailFromPatientBody(patient) +
                //"," +
                //"\"proofOfIdentity\":{" +
                //    "\"refNumber\":null," +
                //    "\"format\":\"pdf\"," +
                //    "\"type\":\"Reference Identity Card\"," +
                //    "\"value\":\"Some_cert\"" +
                //"}" +
                //"," +
                //"\"individualBiometrics\":{" +
                //    "\"format\":\"cbeff\"," +
                //    "\"version\":1," +
                //    "\"value\":\"individualBiometrics_bio_CBEFF\"" +
                //"}" +
            "}" +
        "}");

        System.out.println("Hello 1" + returner.getIdentityJson());

        return returner;
    }
    public String getTxnIdFromBody(String requestBody){
        try{
            return new JSONObject(requestBody).getString("id");
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting txn_id ",je);
        }
    }
    public String getOpencrvsIdFromPatientBody(JSONObject patient){
        try{
            return (String)returnOutputOfArrayIfInputIsValue(patient.getJSONArray("identifier"),"type","BIRTH_REGISTRATION_NUMBER","value");
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting opencrvs id ",je);
        }
    }

    public String getFullNameFromPatientBody(JSONObject patient){
        try{
            String ret = "";
            JSONArray names = patient.getJSONArray("name");

            ret+="[";
            for (int i=0;i<names.length();i++){
                String givenName = names.getJSONObject(i).getJSONArray("given").join(" ");
                String familyName = names.getJSONObject(i).getJSONArray("family").join(" ");
                String langCode = names.getJSONObject(i).getString("use");

                boolean isSet = false;
                for(String langMap: langCodeMapping.split(",")){
                    for (String str : langMap.split(":")[1].split("\\|")){
                        if (str.equalsIgnoreCase(langCode)){
                            isSet=true;
                            langCode=langMap.split(":")[0];
                            break;
                        }
                    }
                    if(isSet) break;
                }
                if(!isSet) langCode = "eng";

                ret+="{" +
                    "\"language\":\""+langCode+"\"," +
                    "\"value\":\""+ givenName + " " + familyName + "\"" +
                "}";
                if(i!=names.length()) ret+=",";
            }
            ret+="]";
            return ret;
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting Full Name from request ",je);
        }
    }

    public String getGenderFromPatientBody(JSONObject patient){
        try{
            return "[{" +
                "\"language\":\""+ genderDefaultLangCode +"\"," +
                "\"value\":\""+ patient.getString("gender") + "\"" +
            "}]";
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting gender from request ",je);
        }
    }

    //dummy
    public String getEmailFromPatientBody(JSONObject patient){
        try{
            JSONObject defaultName = patient.getJSONArray("name").getJSONObject(0);
            String givenName = defaultName.getJSONArray("given").join(".").toLowerCase();
            String familyName = defaultName.getJSONArray("family").join(".").toLowerCase();
            return "\"" + givenName + "." + familyName + ".123@mailinator.com\"";
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting email id from request ",je);
        }
    }

    public String getDOBFromPatientBody(JSONObject patient){
        try{
            return patient.getString("birthDate").replaceAll("-","\\/");
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting DOB from request",je);
        }
    }

    public String getPhoneFromTaskBody(JSONObject task){
        try{
            return (String)returnOutputOfArrayIfInputEndsWithValue(task.getJSONArray("extension"),"url","contact-person-phone-number","valueString");
        } catch(JSONException je){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting email id from request ",je);
        }
    }

    /* [{ "input": "inputValue", "output": "return"},...] */
    public Object returnOutputOfArrayIfInputIsValue(JSONArray arr, String input, String inputValue, String output) throws JSONException{
        for(int i=0;i<arr.length();i++){
            JSONObject json = arr.getJSONObject(i);
            if(inputValue.equalsIgnoreCase((String)getJSONNested(json,input))){
                return getJSONNested(json,output);
            }
        }
        return null;
    }

    public Object returnOutputOfArrayIfInputEndsWithValue(JSONArray arr, String input, String inputValue, String output) throws JSONException{
        for(int i=0;i<arr.length();i++){
            JSONObject json = arr.getJSONObject(i);
            if(inputValue.toLowerCase().endsWith(((String)getJSONNested(json,input)).toLowerCase())){
                return getJSONNested(json,output);
            }
        }
        return null;
    }

    public Object getJSONNested(JSONObject json, String input) throws JSONException{
        Object ret=json;
        for(String str: input.split("\\.")){
            ret=((JSONObject)ret).get(str);
        }
        return ret;
    }
}
