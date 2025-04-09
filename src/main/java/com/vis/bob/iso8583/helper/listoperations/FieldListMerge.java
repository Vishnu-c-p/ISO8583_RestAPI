package com.vis.bob.iso8583.helper.listoperations;

import java.util.List;

import com.vis.bob.iso8583.vo.FieldVO;

public interface FieldListMerge {
	public List<FieldVO> merge(final List<FieldVO> A, final List<FieldVO> B);
}
