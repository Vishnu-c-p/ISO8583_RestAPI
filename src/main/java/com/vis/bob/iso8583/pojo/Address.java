package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class Address {

	private String addressLine1;
	private String addressLine2;
	private String addressLine3;
	private String city;
	private String state;
	private String pincode;
	private String country;
}
