package io.mosip.opencrvs.dto;

import lombok.Data;

@Data
public class ReceiveDto {
	String idValue;
	String centerId;
	String machineId;
	String identityJson;
	String proofOfAddress;
	String proofOfIdentity;
	String proofOfRelationship;
	String proofOfDateOfBirth;
  String opencrvsId;

	public static ReceiveDto build(String opencrvsRequestBody, String rid, String centerId, String machineId){
		ReceiveDto returner = new ReceiveDto();
		// TODO
		returner.idValue = rid;
		returner.centerId = centerId;
		returner.machineId = machineId;

		returner.identityJson = "{\"introducerBiometrics\":\"null\",\"identity\":{\"proofOfAddress\":{\"refNumber\":null,\"format\":\"pdf\",\"type\":\"Rental contract\",\"value\":\"POA_Passport_11344053174764080361\"},\"gender\":[{\"language\":\"fra\",\"value\":\"Femelle\"},{\"language\":\"ara\",\"value\":\"أنثى\"}],\"city\":[{\"language\":\"fra\",\"value\":\"KNT\"},{\"language\":\"ara\",\"value\":\"KNT\"},{\"language\":\"eng\",\"value\":\"?=$࿒ṭmy dòu﷖\"}],\"postalCode\":\"10114\",\"fullName\":[{\"language\":\"fra\",\"value\":\"mxtzozhksnmxyrbnjwfaocfseimfgu xzpteuglndbofqicwcacpcfcqozkof zjduxtapaarntmvwdfmblgsenkmjyo\"},{\"language\":\"ara\",\"value\":\"مكستزُزهكسنمكسيربنجوفَُكفسِِمفگُ كسزپتُِگلندبُفقِكوكَكپكفكقُزكُف زجدُكستَپََرنتمڤودفمبلگسِنكمجيُ\"},{\"language\":\"eng\",\"value\":\"mxtzozhksnmxyrbnjwfaocfseimfgu xzpteuglndbofqicwcacpcfcqozkof zjduxtapaarntmvwdfmblgsenkmjyo\"}],\"dateOfBirth\":\"2003/10/05\",\"proofOfIdentity\":{\"refNumber\":null,\"format\":\"pdf\",\"type\":\"Reference Identity Card\",\"value\":\"POI_Passport_11344053174764080361\"},\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1,\"value\":\"individualBiometrics_bio_CBEFF\"},\"IDSchemaVersion\":0.2,\"province\":[{\"language\":\"fra\",\"value\":\"KTA\"},{\"language\":\"ara\",\"value\":\"KTA\"},{\"language\":\"eng\",\"value\":\"Kénitra\"}],\"zone\":[{\"language\":\"fra\",\"value\":\"BNMR\"},{\"language\":\"ara\",\"value\":\"BNMR\"},{\"language\":\"eng\",\"value\":\"Ben Mansour\"}],\"phone\":\"9671086201\",\"addressLine1\":[{\"language\":\"fra\",\"value\":\"#201, 74 Street, 5 block, lane #1\"},{\"language\":\"ara\",\"value\":\"#٢٠١، ٧٤ سترِِت، ٥ بلُكك، لَنِ #١\"},{\"language\":\"eng\",\"value\":\"#201, 74 Street, 5 block, lane #1\"}],\"addressLine2\":[{\"language\":\"fra\",\"value\":\"#135, 45 Street, 7 block, lane #2\"},{\"language\":\"ara\",\"value\":\"#١٣٥، ٤٥ سترِِت، ٧ بلُكك، لَنِ #٢\"},{\"language\":\"eng\",\"value\":\"#135, 45 Street, 7 block, lane #2\"}],\"proofOfRelationship\":{\"refNumber\":null,\"format\":\"pdf\",\"type\":\"Passport\",\"value\":\"POR_Passport_11344053174764080361\"},\"residenceStatus\":[{\"language\":\"fra\",\"value\":\"Étrangère\"},{\"language\":\"ara\",\"value\":\"أجنبي\"}],\"addressLine3\":[{\"language\":\"fra\",\"value\":\"#506, 30 Street, 4 block, lane #3\"},{\"language\":\"ara\",\"value\":\"#٥٠٦، ٣٠ سترِِت، ٤ بلُكك، لَنِ #٣\"},{\"language\":\"eng\",\"value\":\"#506, 30 Street, 4 block, lane #3\"}],\"region\":[{\"language\":\"fra\",\"value\":\"RSK\"},{\"language\":\"ara\",\"value\":\"RSK\"},{\"language\":\"eng\",\"value\":\"?=\"}],\"email\":\"mxtzozhksnmxyrbnjwfaocfseimf.zjduxtapaarntmvwdfmblgsenkmj.128@mailinator.com\"}}";

		returner.opencrvsId = "20210120001";
		return returner;
	}
}
