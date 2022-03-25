//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.05.13 at 03:22:53 PM IST 
//

package io.mosip.print.constant;


import java.io.Serializable;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "SingleTypeType")
@XmlEnum
public enum BiometricType implements Serializable {

	@XmlEnumValue("Scent")
	SCENT("Scent"), 
	@XmlEnumValue("Dna")
	DNA("DNA"),
	@XmlEnumValue("Dna")
	EAR("Ear "),
	@XmlEnumValue("Face")
	FACE("Face"),
	@XmlEnumValue("Finger")
	FINGER("Finger"),
	@XmlEnumValue("Foot")
	FOOT("Foot"),
	@XmlEnumValue("Vein")
	VEIN("Vein"),
	@XmlEnumValue("HandGeometry")
	HAND_GEOMETRY("HandGeometry"),
	@XmlEnumValue("Iris")
	IRIS("Iris"),
	@XmlEnumValue("Retina")
	RETINA("Retina"),
	@XmlEnumValue("Voice")
	VOICE("Voice"),
	@XmlEnumValue("Gait")
	GAIT("Gait"),
	@XmlEnumValue("Keystroke")
	KEYSTROKE("Keystroke"),
	@XmlEnumValue("LipMovement")
	LIP_MOVEMENT("LipMovement"),
	@XmlEnumValue("SignatureSign")
	SIGNATURE_SIGN("SignatureSign");

	private final String value;

	BiometricType(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static BiometricType fromValue(String v) {
		for (BiometricType c : BiometricType.values()) {
			if (c.value.equalsIgnoreCase(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
