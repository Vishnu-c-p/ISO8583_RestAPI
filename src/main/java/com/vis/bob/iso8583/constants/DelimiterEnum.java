package com.vis.bob.iso8583.constants;

import com.vis.bob.iso8583.protocol.ISO8583Delimiter;
import com.vis.bob.iso8583.protocol.ISO8583GenericConfigDelimiter;

public enum DelimiterEnum {

	GENERIC_CONFIG_DELIMITER("GENERIC_CONFIG_DELIMITER", new ISO8583GenericConfigDelimiter());
	
	private ISO8583Delimiter isoDelimiter;
	private String value;
	
	DelimiterEnum(String value, ISO8583Delimiter isoDelimiter) {
		this.value = value;
		this.isoDelimiter = isoDelimiter;
	}
	
	public static DelimiterEnum getDelimiter(String value) {
		if ("GENERIC_CONFIG_DELIMITER".equals(value))
			return DelimiterEnum.GENERIC_CONFIG_DELIMITER;
		
		return DelimiterEnum.GENERIC_CONFIG_DELIMITER;
	}
	
	public String toString() {
		return isoDelimiter.getName();
	}
	
	public ISO8583Delimiter getDelimiter() {
		return isoDelimiter;
	}
	
	public String getValue() {
		return value;
	}
}
