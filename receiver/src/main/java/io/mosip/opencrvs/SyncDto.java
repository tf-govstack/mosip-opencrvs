package io.mosip.opencrvs;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;

import lombok.Data;

import org.json.JSONArray;

@Data
public class SyncDto implements Serializable {
  static final long serialVersionUID = -3922338139042373367L;
  String registrationId;
  String packetId ;
  String additionalInfoReqId ;
  String name;
  String email;
  String phone;
  String registrationType;
  String packetHashValue;
  BigInteger packetSize;
  String supervisorStatus;
  String supervisorComment;
  JSONArray optionalValues;
  String langCode;
  LocalDateTime createDateTime;
  LocalDateTime updateDateTime;
  LocalDateTime deletedDateTime;
  Boolean isActive;
  Boolean isDeleted;
}
