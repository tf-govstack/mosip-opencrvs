package io.mosip.opencrvs;

class ReceiveDto {
	String idValue;
	// enum idType{
  //   UIN, VID
  // }
	// enum requestType{
  // 	NEW,
  // 	UPDATE,
  // 	RES_UPDATE,
  // 	CORRECTION,
  // 	ACTIVATED,
  // 	DEACTIVATED,
  // 	LOST,
  // 	RES_REPRINT
  // }
	String centerId;
	String machineId;
	String identityJson;
	String proofOfAddress;
	String proofOfIdentity;
	String proofOfRelationship;
	String proofOfDateOfBirth;
  String opencrvsId;
}
