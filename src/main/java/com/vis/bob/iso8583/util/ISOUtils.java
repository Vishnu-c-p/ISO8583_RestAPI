package com.vis.bob.iso8583.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Node;

import com.vis.bob.iso8583.exception.OutOfBoundsException;
import com.vis.bob.iso8583.pojo.Balances;

import jdk.internal.org.jline.utils.Log;

public class ISOUtils {

	public static String hexToBin(String hex){
		String bin = "";
		String binFragment = "";
		int iHex;
		hex = hex.trim();
		hex = hex.replaceFirst("0x", "");

		for(int i = 0; i < hex.length(); i++){
			iHex = Integer.parseInt(""+hex.charAt(i),16);
			binFragment = Integer.toBinaryString(iHex);

			while(binFragment.length() < 4){
				binFragment = "0" + binFragment;
			}
			bin += binFragment;
		}
		return bin;
	}

	public static String binToHex(String bin) {
		String result = "";
		int decimal;
		for (int i = 4; (i <= 64 && i <= bin.length()); i = i + 4) {
			decimal = Integer.parseInt(bin.substring((i - 4), i), 2);
			result = result.concat(Integer.toString(decimal, 16)).toUpperCase();
		}

		return result;
	}

	public static String binToHexLog(String bin) {
		String result = "";
		String endReuslt = "";
		int decimal;
		for (int i = 4; (i <= 128 && i <= bin.length()); i = i + 4) {
			decimal = Integer.parseInt(bin.substring((i - 4), i), 2);
			result = result.concat(Integer.toString(decimal, 16)).toUpperCase();
		}

		for (int i = 2; i <= result.length(); i = i + 2)
			endReuslt += result.substring(i-2,i)+ " ";

		return endReuslt.trim();
	}

	public static byte[] subArray(byte[] data, int start, int end) throws OutOfBoundsException {
		if ((end - start) <= 0) throw new OutOfBoundsException();
		if (data.length < end || data.length < start || data.length < (end - start)) throw new OutOfBoundsException();

		byte[] result = new byte[end - start];

		for (int i = start; i < end; i++)
			result[i - start] = data[i];

		return result;
	}

	public static byte[] mergeArray(byte[] arr1, byte[] arr2) {
		byte[] result = new byte[arr1.length + arr2.length];

		for (int i = 0; i < arr1.length; i++)
			result[i] = arr1[i];

		for (int i = 0; i < arr2.length; i++)
			result[i + arr1.length] = arr2[i];

		return result;
	}

	public static String getAttr(Node node, String name, String defaultValue) {
		String result = defaultValue;
		if (node != null && node.getAttributes() != null && node.getAttributes().getNamedItem(name) != null)
			result = node.getAttributes().getNamedItem(name).getNodeValue();

		return result;
	}

	public static byte[] listToArray(List<Byte> bytes) {
		byte[] data = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++)
			data[i] = bytes.get(i).byteValue();
		return data;
	}

	public static String pad(String param, String padStr, int padSize, int padType) {
		String paddedStr = "";

		if (param.length() == padSize)
			paddedStr = param;
		else if (param.length() > padSize)
			paddedStr = param.substring(0,padSize);
		else {
			for (int i=0; i < padSize-param.length(); i++) 
				paddedStr += padStr;

			if (padType == 0) 
				paddedStr = paddedStr + param;
			else
				paddedStr = param + paddedStr;

		}

		return paddedStr;
	}

	public static String binToAscii(String binValue) {
		if (binValue.length() % 2 != 0) {
			binValue = "0" + binValue;
		}
		String string = new String();
		int length = binValue.length();
		for (int i = 0; i <= length - 1; i += 8) {
			int byteValue = Integer.parseInt(binValue.substring(i, i + 8), 2);
			string = string + (char) byteValue;
		}
		return string;
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			builder.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
			builder.append(Character.forDigit((bytes[i] & 0xF), 16));
		}
		return builder.toString().toUpperCase();
	}

	public static String hexToAsccii(String hex) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hex.length(); i+=2) {
			String str = hex.substring(i, i+2);
			output.append((char)Integer.parseInt(str, 16));
		}
		return output.toString();
	}

	public static String ascciiToHex(String asccii) {
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < asccii.length(); i++) {
			String hexVal = Integer.toHexString(asccii.charAt(i));
			if (hexVal.length() < 2)
				hexVal = "0" + hexVal;
			hex.append(hexVal.toUpperCase());
			hex.append(" ");
		}
		return hex.toString();
	}

	public static String getCbsCurrencyVal(String str) {
		String amnt = ""+Long.parseLong(str);
		int lenOrg = amnt.length();
		int len = lenOrg;
		String sign = "";
		if (amnt.startsWith("-")) {
			len = len-1;
			sign = "-";
			amnt = amnt.substring(1);
		}
		if (len >= 3)
			amnt = sign+amnt.substring(0,len-2) + "." + amnt.substring(len-2);
		else if (len == 2)
			amnt = sign+"0." + amnt;
		else if (len == 1)
			amnt = sign+"0.0" + amnt;

		return amnt;
	}

	public static String getCbsRrn() {
		Random random = new Random();
		char[] digits = new char[12];
		digits[0] = (char)(random.nextInt(9) + '1');
		for (int i = 1; i < 12; i++) {
			digits[i] = (char)(random.nextInt(10) + '0');
		}
		return new String(digits);
	}

	public static Balances getBalances(String balanceStr) {
		Balances balances = new Balances();

		balances.setLedgerBalance(getCbsCurrencyVal(balanceStr.substring(0,17)));
		balances.setAvailableBalance(getCbsCurrencyVal(balanceStr.substring(17,34)));
		balances.setFloatBalance(getCbsCurrencyVal(balanceStr.substring(34,51)));
		balances.setFfdBalance(getCbsCurrencyVal(balanceStr.substring(51,68)));
		balances.setUserDefinedBalance(getCbsCurrencyVal(balanceStr.substring(68,85)));
		balances.setBalanceCurrency(balanceStr.substring(85,88));

		return balances;
	}	
	
	public static String readConfigFile(String file) {
		
		String xmlText = "";
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    xmlText = (sb.toString());
		} 
		catch (Exception x) {
			Log.error("Reading config file : {} failed, msg: {}", file, x.getMessage());
		}
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (Exception x) {
					Log.error("Reading config file : {} failed, msg: {}", file, x.getMessage());
				}
			}
		}
		
		return xmlText;
	}
}
