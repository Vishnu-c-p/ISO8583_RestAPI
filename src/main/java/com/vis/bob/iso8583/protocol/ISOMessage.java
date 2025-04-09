package com.vis.bob.iso8583.protocol;

import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ISOMessage {

	private int messageSize;
	private byte[] payload;

	private Bitmap bitmap;
	
	public ISOMessage(Iso8583Config isoConfig, MessageVO messageVO) throws ParseException {
		this(isoConfig, null, messageVO);
	}
	
	private byte[] bytesTPDU(String TPDU) {
		String strByteTPDU;
		byte x;
		byte[] resultBytes = new byte[5];
		
		for(int i=0;i<5;i++) {
			strByteTPDU="";
			try {
				strByteTPDU=TPDU.substring(i*2,(i*2+2));
				x=(byte) Integer.parseInt(strByteTPDU,16);
			}
			catch(Exception xx) {
				log.error("Error parsing TPDU byte ["+strByteTPDU+"]. Setting to 0. [i="+i+"]. "+xx.getMessage());
				x=0;
			}
			resultBytes[i] = x; 
		}
		
		return resultBytes;
	}
	
	public ISOMessage(Iso8583Config isoConfig ,byte[] payload, MessageVO messageVO) throws ParseException {
		
		if (payload != null)
			bitmap = new Bitmap(isoConfig, payload, messageVO);
		else
			bitmap = new Bitmap(isoConfig, messageVO);
		
        StringBuilder strMessage = new StringBuilder();
        this.payload = new byte[0];

        if (bitmap.getMessageVO().getHeader() != null) {
        	if (!bitmap.getMessageVO().getHeader().isEmpty()) {        	
        		strMessage.append(bitmap.getMessageVO().getHeader());
        		this.payload = ISOUtils.mergeArray(this.payload, bitmap.getMessageVO().getHeaderEncoding().convert(bitmap.getMessageVO().getHeader()));
        	}
        }
        if (!bitmap.getMessageVO().getTPDUValue().isEmpty()) {
            strMessage.append(bitmap.getMessageVO().getTPDUValue());
            this.payload = ISOUtils.mergeArray(this.payload, bytesTPDU(bitmap.getMessageVO().getTPDUValue()));
        }
        
		strMessage.append(messageVO.getType());
		strMessage.append(bitmap.getPayloadBitmap());
        
		this.payload = ISOUtils.mergeArray(this.payload, messageVO.getHeaderEncoding().convert(messageVO.getType()));
		this.payload = ISOUtils.mergeArray(this.payload, bitmap.getPayloadBitmap());
		
		log.debug("Retrieving Data from Bitmap -");
		for (int i = 0; i <= bitmap.getSize(); i++) {
			if (bitmap.getBit(i) != null) {
				try {
					log.debug("		Bit ["+i+"]	: {"+bitmap.getBit(i).getEncoding().convert(bitmap.getBit(i).getPayloadValue())+"}");
					
					this.payload = ISOUtils.mergeArray(this.payload, bitmap.getBit(i).getPayloadValue());
					strMessage.append(bitmap.getBit(i).getPayloadValue());
				}
				catch(Exception exx) {
					log.error("Bit ["+i+"]: error. "+exx.getMessage());
				}
			}
		}
		this.messageSize = strMessage.length();
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	public FieldVO getBit(int bit) {
		return bitmap.getBit(bit);
	}
	
	public int getMessageSize() {
		return messageSize;
	}
	
	public String getMessageSize(int numChars) {
		String result = String.valueOf(messageSize);
		while (result.length() < numChars)
			result = "0" + result;
		return result;
	}
	
	public MessageVO getMessageVO() {
		return bitmap.getMessageVO();
	}
}
