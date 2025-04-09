package com.vis.bob.iso8583.helper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList; 

import com.vis.bob.iso8583.constants.DelimiterEnum;
import com.vis.bob.iso8583.constants.EncodingEnum;
import com.vis.bob.iso8583.constants.TypeEnum;
import com.vis.bob.iso8583.constants.TypeLengthEnum;
import com.vis.bob.iso8583.exception.OutOfBoundsException;
import com.vis.bob.iso8583.protocol.ISO8583Delimiter;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;
import com.vis.bob.iso8583.xml.XMLUtils;

import groovy.util.Eval;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Iso8583Config {
	
	private static final String XML_FIELD_NODENAME = "field";
	private ArrayList<MessageVO> configTreeNode;
	private String xmlText = "";
	
    private DelimiterEnum isoDelimiter;
    private EncodingEnum headerEncoding;
    private Integer headerSize;
    
    private boolean TPDU;
    private boolean StxEtx;
	private String xmlFilePath = null;
	
	@Getter
	@Setter
	private int maxBytes;
	@Getter
	@Setter
	private boolean dirtyPayload = false;
		
	public Iso8583Config(String fileName) {
		this();
		openFile(fileName);
		parseXmlToConfig();
	}
	
	public Iso8583Config() {
        isoDelimiter = DelimiterEnum.getDelimiter("");
        headerEncoding = EncodingEnum.getEncoding("");
        headerSize = 0;
        TPDU=false;
        StxEtx=false;
       configTreeNode = new ArrayList<MessageVO>();
	}
	
	public ArrayList<MessageVO> getConfigTreeNode() {
		return configTreeNode;
	}
	
	public MessageVO addType() {
		MessageVO parseVO = new MessageVO("0000", EncodingEnum.UTF8);
		configTreeNode.add(parseVO);
		return parseVO;
	}

	public FieldVO addField( Object node) {
		FieldVO newNode = null;
		
		if (isAMessageNode(node) || isAFieldNode(node)) {
			newNode = new FieldVO( "NewField", "", 2, TypeEnum.ALPHANUMERIC, TypeLengthEnum.FIXED, 1, EncodingEnum.UTF8, "");
		}
		return newNode;
	}
	
	private boolean isAFieldNode(final Object node) {
		return (node instanceof FieldVO);
	}

	private boolean isAMessageNode(final Object node) {
		return node instanceof MessageVO;
	}
	
	public void parseXmlToConfig() {
		try {
			if (!xmlText.trim().equals("")) {
				configTreeNode.removeAll(configTreeNode);
				
				Document document = XMLUtils.convertXMLToDOM(xmlText);
				
				MessageVO lastParseNode;
				
                setDelimiterEnum(DelimiterEnum.getDelimiter(document.getDocumentElement().getAttribute("delimiter")));
                setHeaderEncoding(EncodingEnum.getEncoding(document.getDocumentElement().getAttribute("headerEncoding")));
                
                try {
                    setHeaderSize(Integer.parseInt(document.getDocumentElement().getAttribute("headerSize")));
                }
                catch (Exception x) {
                    setHeaderSize(0);
                }
                
                try {
                    setTPDU(Boolean.parseBoolean(document.getDocumentElement().getAttribute("tpdu")));
                    
                }
                catch (Exception x) {
                	setTPDU(false);
                }
				
                try {
                    setStxEtx(Boolean.parseBoolean(document.getDocumentElement().getAttribute("stxetx")));
                    
                }
                catch (Exception x) {
                	log.error("(error) on setting stxetx");
                	setStxEtx(false);
                }
                
				NodeList nodeList = document.getDocumentElement().getChildNodes();
				Node node;
				
				for (int i = 0; i < nodeList.getLength(); i++) {
					node = nodeList.item(i);
					
					if ("message".equalsIgnoreCase(node.getNodeName())) {
						lastParseNode = new MessageVO();
						lastParseNode.setType(ISOUtils.getAttr(node, "type", "0000"));;
						lastParseNode.setBitmatEncoding(EncodingEnum.getEncoding(ISOUtils.getAttr(node, "bitmap-encoding", "")));
						
						ArrayList<FieldVO> fieldList = new ArrayList<FieldVO>();
						NodeList fielNodedList = node.getChildNodes();
						for (int j = 0; j < fielNodedList.getLength(); j++) {
							Node domNode = fielNodedList.item(j);
							
							if (XML_FIELD_NODENAME.equalsIgnoreCase(domNode.getNodeName())) {
								FieldVO fieldVo = new FieldVO();
								
								fieldVo.setName(ISOUtils.getAttr(domNode, "name", ""));

								fieldVo.setBitNum(Integer.parseInt(ISOUtils.getAttr(domNode, "bitnum", "0")));
								fieldVo.setType(TypeEnum.getType(ISOUtils.getAttr(domNode, "type", "")));
								fieldVo.setTypeLength(TypeLengthEnum.getTypeLength(ISOUtils.getAttr(domNode, "length-type", "")));
								fieldVo.setLength(Integer.parseInt(ISOUtils.getAttr(domNode, "length", "0")));
								fieldVo.setEncoding(EncodingEnum.getEncoding(ISOUtils.getAttr(domNode, "encoding", "")));
								fieldVo.setDynaCondition(ISOUtils.getAttr(domNode, "condition", ""));
								
								fieldList.add(fieldVo);
							}
						}
						
						lastParseNode.setFieldList(fieldList);						
						configTreeNode.add(lastParseNode);
					}
				}
			}
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public MessageVO getMessageVOAtTree(String type) {
		MessageVO newMessageVO = null;
		
		for (int i =0; i < configTreeNode.size(); i++) {
			if (configTreeNode.get(i).getType().equals(type)) {
				newMessageVO = configTreeNode.get(i);
				newMessageVO.setHeaderEncoding(getHeaderEncoding());
                newMessageVO.setHeaderSize(getHeaderSize());
				break;
			}
		}
		return newMessageVO;
	}

	public void openFile(String file) {
		
		this.xmlFilePath = file;
		
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
			this.xmlFilePath = null;
			x.printStackTrace();
		}
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (Exception x) {
					x.printStackTrace();
				}
			}
		}
	}
	
	public String validateCondition(FieldVO fieldVO) {
		String resultMessage = "";
		
		try {
			String condition = "Object[] BIT = new Object[255];\n";
			condition = condition + fieldVO.getDynaCondition();
			Eval.me(condition);
			
			if (condition.indexOf("BIT[" + fieldVO.getBitNum() + "]") > -1)
				throw new Exception("You cannot look for the same bit value.");
		}
		catch (Exception x) {
			resultMessage = x.getMessage();
		}
		
		return resultMessage;
	}
	
	public String getXmlFilePath() {
		return xmlFilePath;
	}
	
	public void setXmlFilePath(String xmlFilePath) {
		this.xmlFilePath = xmlFilePath;
	}
	
	public DelimiterEnum getDelimiterEnum() {
		return isoDelimiter;
	}
    
    public ISO8583Delimiter getDelimiter() {
		return isoDelimiter.getDelimiter();
	}

    public EncodingEnum getHeaderEncoding() {
        return headerEncoding;
    }

    public Integer getHeaderSize() {
        return headerSize;
    }

	public boolean getTPDU() {
		return TPDU;
	}

	public void setDelimiterEnum(DelimiterEnum isoDelimiter) {
		this.isoDelimiter = isoDelimiter;
	}
    
    public void setHeaderEncoding(EncodingEnum headerEncoding) {
        this.headerEncoding = headerEncoding;
    }

    public void setHeaderSize(Integer headerSize) {
        this.headerSize = headerSize;
    }

	public void setTPDU(boolean TPDU) {
		this.TPDU=TPDU;
	}
	
	public void setStxEtx(boolean StxEtx) {
		this.StxEtx=StxEtx;
	}

	public MessageVO findMessageVOByPayload(byte[] payload) {
		MessageVO result = null;
		try {
			int messageTypeSize = 4;
			int calculatedHeaderSize = 0;
            
            String messageType = headerEncoding.convert(ISOUtils.subArray(payload, calculatedHeaderSize, (calculatedHeaderSize + messageTypeSize)));
            for (int i = 0; i < configTreeNode.size(); i++) {
            	result = configTreeNode.get(i);
                
				if (result.getType().equals(messageType))
					break;
				else
					result = null;
			}
		}
		catch (OutOfBoundsException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public boolean getStxEtx() {
		return StxEtx;
	}

	public void setXmlText(String xmlText) {
		this.xmlText = xmlText;
	}
	
}
