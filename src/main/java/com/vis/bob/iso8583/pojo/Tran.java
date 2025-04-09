package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class Tran {

	private String date;
	private String category;
	private String desc;
	private String type;
	private String amount;
}
