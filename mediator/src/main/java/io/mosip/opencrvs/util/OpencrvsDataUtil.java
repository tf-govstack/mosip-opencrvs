package io.mosip.opencrvs.util;

import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.opencrvs.constant.LoggingConstants;
import io.mosip.opencrvs.dto.DecryptedEventDto;
import io.mosip.opencrvs.dto.ReceiveDto;
import io.mosip.opencrvs.error.ErrorCode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

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
    @Value("${opencrvs.data.dummy.phone}")
    private String dummyPhone;
    @Value("${opencrvs.data.dummy.emailSuffix}")
    private String dummyEmailSuffix;

    @Autowired
    private Environment env;

    public ReceiveDto buildIdJson(DecryptedEventDto opencrvsRequestBody){
        List<DecryptedEventDto.Event.Context.Entry> contextEntries;
        DecryptedEventDto.Event.Context.Entry.Resource patient = null;
        DecryptedEventDto.Event.Context.Entry.Resource task = null;
        try{
            contextEntries = opencrvsRequestBody.event.context.get(0).entry;
            for(DecryptedEventDto.Event.Context.Entry entry: contextEntries){
                if("Patient".equals(entry.resource.resourceType)){
                    patient = entry.resource;
                } else if("Task".equals(entry.resource.resourceType)) {
                    task = entry.resource;
                }
                if (patient!=null && task!=null) break;
            }
            if(patient == null || task == null){
                LOGGER.error(LoggingConstants.SESSION,LoggingConstants.ID,"ReceiveDto::build()","Error processing patient/task. Got null patient/task");
                throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE);
            }
        } catch(NullPointerException ne){
            LOGGER.error(LoggingConstants.FORMATTER_PREFIX,LoggingConstants.SESSION,LoggingConstants.ID,"ReceiveDto::build()", "Received null pointer exception", ne);
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE, ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE, ne);
        }

        ReceiveDto returner = new ReceiveDto();

        returner.setOpencrvsBRN(getOpencrvsBRNFromPatientBody(patient));

        String fullName = getFullNameFromPatientBody(patient);

        returner.setIdentityJson("{"+
            "\"introducerBiometrics\":\"null\"," +
            "\"identity\":{" +
                "\"IDSchemaVersion\":" + idschemaVersion + "," +
                "\"fullName\":" + fullName + "," +
                "\"dateOfBirth\":" + getDOBFromPatientBody(patient) + "," +
                "\"gender\":" + getGenderFromPatientBody(patient) +"," +
                "\"addressLine1\":" + dummyAddressLine1 + "," +
                "\"addressLine2\":" + dummyAddressLine2 + "," +
                "\"addressLine3\":" + dummyAddressLine3 + "," +
                "\"region\":" + dummyRegion + "," +
                "\"province\":" + dummyProvince + "," +
                "\"city\":" + dummyCity + "," +
                "\"zone\":" + dummyZone + "," +
                "\"postalCode\":" + dummyPostalCode + "," +
                //"\"phone\":" + getPhoneFromTaskBody(task) + "," +
                "\"phone\":" + dummyPhone + "," +
                "\"email\":" + getEmailFromPatientBody(patient) + "," +
                "\"proofOfIdentity\":" +
                "null" +
                //"{" +
                //    "\"refNumber\":null," +
                //    "\"format\":\"pdf\"," +
                //    "\"type\":\"Reference Identity Card\"," +
                //    "\"value\":\"Some_cert\"" +
                //"}" +
                "," +
                "\"individualBiometrics\":" +
                "null" +
                //"{" +
                //    "\"format\":\"cbeff\"," +
                //    "\"version\":1," +
                //    "\"value\":\"individualBiometrics_bio_CBEFF\"" +
                //"}" +
            "}" +
        "}");

        return returner;
    }

    public String getOpencrvsBRNFromPatientBody(DecryptedEventDto.Event.Context.Entry.Resource patient){
        for(DecryptedEventDto.Event.Context.Entry.Resource.Identifier identifier : patient.identifier){
            if("BIRTH_REGISTRATION_NUMBER".equals(identifier.type)){
                return identifier.value;
            }
        }
        return null;
    }

    public String getFullNameFromPatientBody(DecryptedEventDto.Event.Context.Entry.Resource patient){
        try{
            String ret = "";
            List<DecryptedEventDto.Event.Context.Entry.Resource.Name> names = patient.name;

            ret+="[";
            for (int i=0;i<names.size();i++){
                String givenName = String.join(" ", names.get(i).given).replaceAll("\"","");
                String familyName = String.join(" ", names.get(i).family).replaceAll("\"","");
                String langCode = names.get(i).use;

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
                if(i!=names.size()-1) ret+=",";
            }
            ret+="]";
            return ret;
        } catch(NullPointerException ne){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting Full Name from request ", ne);
        }
    }

    public String getGenderFromPatientBody(DecryptedEventDto.Event.Context.Entry.Resource patient){
        try{
            return "[{" +
                "\"language\":\""+ genderDefaultLangCode +"\"," +
                "\"value\":\""+ patient.gender + "\"" +
            "}]";
        } catch(NullPointerException ne){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting gender from request ", ne);
        }
    }

    public String getEmailFromPatientBody(DecryptedEventDto.Event.Context.Entry.Resource patient){
        //dummy implementation
        String staticEmail = env.getProperty("opencrvs.data.dummy.static.email");
        if(staticEmail!=null && !staticEmail.isEmpty()){
            return "\"" + staticEmail + "\"";
        }
        try{
            DecryptedEventDto.Event.Context.Entry.Resource.Name defaultName = patient.name.get(0);
            String givenName = String.join(".", defaultName.given).replaceAll("\\s","").replaceAll("\"","").toLowerCase();
            String familyName = String.join(".", defaultName.family).replaceAll("\\s","").replaceAll("\"","").toLowerCase();
            return "\"" + givenName + "." + familyName + dummyEmailSuffix.replaceAll("\"","") + "\"";
        } catch(NullPointerException ne){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting email id from request ", ne);
        }
    }

    public String getDOBFromPatientBody(DecryptedEventDto.Event.Context.Entry.Resource patient){
        try{
            return "\""+patient.birthDate.replaceAll("-","\\/")+"\"";
        } catch(NullPointerException ne){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting DOB from request", ne);
        }
    }

    public String getPhoneFromTaskBody(DecryptedEventDto.Event.Context.Entry.Resource task){
        try{
            for(DecryptedEventDto.Event.Context.Entry.Resource.Extension extension : task.extension){
                if (extension.url.toLowerCase().endsWith("contact-person-phone-number")){
                    return "\"" + extension.valueString + "\"";
                }
            }
            return null;
        } catch(NullPointerException ne){
            throw new BaseUncheckedException(ErrorCode.JSON_PROCESSING_EXCEPTION_CODE,ErrorCode.JSON_PROCESSING_EXCEPTION_MESSAGE+"while getting phone number from request ", ne);
        }
    }
}
