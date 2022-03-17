package io.mosip.opencrvs.dto;

import lombok.Data;

@Data
public class ReceiveDto {
	String rid;
	String identityJson;
	String proofOfIdentity;
	String opencrvsId;
//	String proofOfAddress;
//	String proofOfRelationship;
//	String proofOfDateOfBirth;
}
