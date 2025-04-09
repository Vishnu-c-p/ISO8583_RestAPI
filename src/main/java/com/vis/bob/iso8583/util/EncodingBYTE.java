package com.vis.bob.iso8583.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncodingBYTE implements Encoding {

    @Override
    public String convert(byte[] bytesToConvert) {
        try {
            StringBuilder strResult = new StringBuilder();
    		
    		for(int i=0;i<bytesToConvert.length;i++) {
    			/*
    			int y = ((int) bytesToConvert[i]);
    			String x=Integer.toString(y);
    			*/
    			String x="";
    			try {
    	    		x = Integer.toHexString((int) bytesToConvert[i]);
    	    		x=x.replace("ffffff", "");
    	    		
    				if(x.length() < 2)
	    				x="0".concat(x);
    				else
    					if(x.length() > 2);
    						x=x.substring(0,2);
    			}
    			catch(Exception ex) {
    				log.error("TPDU invalid char "+x);
    			}
    			strResult.append(x);
    		}
    		return strResult.toString();
            
        } 
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] convert(String strToConvert) {
        try {
            log.debug("converting ["+strToConvert+"] to byte[]");
        	int l=strToConvert.length();
            byte[] resultBytes = new byte[l];
    		for(int i=0;i<l;i++) {
    			log.debug(strToConvert.substring(i,(i+1)));
    			resultBytes[i] = (byte) Integer.parseInt(strToConvert.substring(i,(i+1)));
    			
    		}

            return resultBytes;
            
        } 
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String convertBitmap(byte[] binaryBitmap) {
        /*
         * TODO: implementar para byte
         * */
    	log.debug("convertBitmap(byte[] binaryBitmap) needs to be implemented");
        return "";
    }

    @Override
    public byte[] convertBitmap(String binaryBitmap) {
    	/*
         * TODO: implementar para byte
         * */
    	log.debug("convertBitmap(String binaryBitmap) needs to be implemented");
        return null;
    	//return ISOUtils.binToHex(binaryBitmap).getBytes();
    }

    @Override
    public int getMinBitmapSize() {
        return 16;
    }

    @Override
    public int getEncondedByteLength(final int asciiLength) {
        return asciiLength;
    }
}
