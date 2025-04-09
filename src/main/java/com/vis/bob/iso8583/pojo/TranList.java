package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class TranList {

	private int count;
	private List<Tran> transaction;
}
