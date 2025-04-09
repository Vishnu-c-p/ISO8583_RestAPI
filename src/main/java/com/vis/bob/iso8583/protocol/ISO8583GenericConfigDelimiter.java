package com.vis.bob.iso8583.protocol;

import java.util.List;

import com.vis.bob.iso8583.exception.InvalidPayloadException;
import com.vis.bob.iso8583.exception.OutOfBoundsException;
import com.vis.bob.iso8583.exception.PayloadIncompleteException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.util.ISOUtils;

public class ISO8583GenericConfigDelimiter implements ISO8583Delimiter {

	@Override
	public String getName() {
		return "Generic, Based on Configuration";
	}

	@Override
	public String getDesc() {
		return "This delimiter is based on the configuration file to find out the length of each message. It " +
				"may has a poor performance, depending of the complexity and size of the messages.";
	}

	@Override
	public byte[] preparePayload(ISOMessage isoMessage, Iso8583Config isoConfig) {
		return isoMessage.getPayload();
	}

	@Override
	public boolean isPayloadComplete(List<Byte> bytes, Iso8583Config isoConfig) throws InvalidPayloadException {
		boolean isComplete = false;
		
		if (bytes.size() > 68 && bytes.size() <= isoConfig.getMaxBytes()) {
		
			try {
				new Bitmap(isoConfig, ISOUtils.listToArray(bytes), isoConfig.getMessageVOAtTree("1210"));
				
				isComplete = true;
			}
			catch (PayloadIncompleteException x) {
				isComplete = false;
			} 
			catch (NullPointerException n) {
				isComplete = false;
			}
			catch (Exception e) {
				if (bytes.size() >= isoConfig.getMaxBytes())
					isComplete = true;
				else 
					isComplete = false;
				e.printStackTrace();
			}
		}
		
		return isComplete;
	}

	@Override
	public byte[] clearPayload(byte[] data, Iso8583Config isoConfig) {
		return data;
	}

	@Override
	public int getMessageSize(List<Byte> bytes) throws OutOfBoundsException {
		return 0;
	}

	@Override
	public byte[] preparePayload(ISOMessage isoMessage, boolean stxEtx) {
		return isoMessage.getPayload();
	}

}
