package com.vis.bob.iso8583.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.listoperations.FieldListMerge;
import com.vis.bob.iso8583.helper.listoperations.StructurePriorityMerge;
import com.vis.bob.iso8583.helper.listoperations.ValuePriorityMerge;
import com.vis.bob.iso8583.protocol.ISOMessage;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;
import com.vis.bob.iso8583.xml.XMLUtils;

@Component
public class PayloadMessageConfig {

	private MessageVO messageVO;

	private ArrayList<PayloadField> fieldList;

	private int numLines;

	private ISOMessage isoMessage;

	public PayloadMessageConfig() {
	}

	public MessageVO getMessageVO() {
		return this.messageVO;
	}
	
	public void setMessageVO(Iso8583Config isoConfig, String messageType) {
		setMessageVO(isoConfig.getMessageVOAtTree(messageType));
	}

	public void setMessageVO(MessageVO messageVO) {
		if (messageVO != null) {
			this.messageVO = messageVO.getInstanceCopy();
			this.messageVO.setFieldList(new ArrayList<FieldVO>());
			numLines = 0;
			fieldList = new ArrayList<PayloadField>();

			for (FieldVO fieldVO : messageVO.getFieldList()) {
				PayloadField newPayloadField = new PayloadField(fieldVO, null, "");
				fieldList.add(newPayloadField);
				setSubFieldsVO(fieldVO.getFieldList(), newPayloadField, "");
			}
		}
	}

	protected void setSubFieldsVO(final List<FieldVO> fields, final PayloadField parentPlayloadField, String superFieldBitNum){
		for (final FieldVO fieldVO : fields) {
			PayloadField innerFieldPayload = parentPlayloadField.addSubline(fieldVO, parentPlayloadField.getFieldVO(), superFieldBitNum +"["+ parentPlayloadField.getFieldVO().getBitNum() +"]");
			setSubFieldsVO(fieldVO.getFieldList(), innerFieldPayload, superFieldBitNum +"["+ parentPlayloadField.getFieldVO().getBitNum() +"]");
		}
	}

	public void setHeaderValue(Iso8583Config isoConfig, String headerValue) {

		this.messageVO.setHeaderEncoding(isoConfig.getHeaderEncoding());
		this.messageVO.setHeaderSize(isoConfig.getHeaderSize());

		if (headerValue != null) {
			if (headerValue.length() > isoConfig.getHeaderSize()) {
				this.messageVO.setHeader(headerValue.substring(0, isoConfig.getHeaderSize()));
			}
			else {
				this.messageVO.setHeader(headerValue);
			}
		}
	}

	public void setTPDUValue(String tpdu) {

		if (tpdu != null) {
			if (tpdu.length() != 10) {
				this.messageVO.setTPDUValue(this.messageVO.getTPDUValue());
			}
			else {
				this.messageVO.setTPDUValue(tpdu);
			}
		}
	}

	private static NodeList convertToDOMNodes(final String xml) throws ParserConfigurationException, SAXException, IOException {
		final Document document = XMLUtils.convertXMLToDOM(xml);
		return document.getDocumentElement().getChildNodes();
	}

	public MessageVO buildMessageStructureFromXML(Iso8583Config isoConfig,  String xml) throws ParseException{
		return buildMessagesFromXML(isoConfig, xml, new ValuePriorityMerge());
	}

	public MessageVO updateMessageValuesFromXML(Iso8583Config isoConfig,  String xml) throws ParseException{
		return buildMessagesFromXML(isoConfig, xml, new StructurePriorityMerge());
	}

	public MessageVO buildMessagesFromXML(Iso8583Config isoConfig, String xml, FieldListMerge fieldListMege) throws ParseException{
		try{
			MessageVO newMessageVO = null;
			final NodeList nodeList = convertToDOMNodes(xml);
			for (int i = 0; i < nodeList.getLength(); i++) {
				final Node node = nodeList.item(i);
				if ("message".equalsIgnoreCase(node.getNodeName())) {
					newMessageVO = isoConfig.getMessageVOAtTree(ISOUtils.getAttr(node, "type", "")).getInstanceCopy();

					String headerValue = ISOUtils.getAttr(node, "header", "");
					if (headerValue != null) {
						if (headerValue.length() > isoConfig.getHeaderSize()) {
							newMessageVO.setHeader(headerValue.substring(0, isoConfig.getHeaderSize()));
						}
						else {
							newMessageVO.setHeader(headerValue);
						}
					}

					newMessageVO.setHeaderEncoding(isoConfig.getHeaderEncoding());
					newMessageVO.setHeaderSize(isoConfig.getHeaderSize());

					String tpduValue = ISOUtils.getAttr(node, "tpdu", "");
					if (tpduValue != null) {
						if (tpduValue.length() > 10) {
							newMessageVO.setTPDUValue(tpduValue.substring(0, 10));
						}
						else {
							newMessageVO.setTPDUValue(tpduValue);
						}
					}

					final ArrayList<FieldVO> messageFieldList = newMessageVO.getFieldList();
					final ArrayList<FieldVO> xmlFieldList = getFieldsFromXML(node.getChildNodes(), messageFieldList);
					final List<FieldVO> newFieldList = fieldListMege.merge(messageFieldList, xmlFieldList);
					newMessageVO.setFieldList((ArrayList<FieldVO>)newFieldList);
					break;
				}
			}
			return newMessageVO;
		}
		catch(SAXException | IOException | ParserConfigurationException e){
			throw new ParseException(e.getMessage());
		}
	}

	public int getNumLines() {
		return numLines;
	}

	private ArrayList<FieldVO> getFieldsFromXML(NodeList fieldNodeList, ArrayList<FieldVO> treeFieldList) throws ParseException {
		ArrayList<FieldVO> fieldList = new ArrayList<FieldVO>();

		int bitNum = 0;
		FieldVO fieldFound;
		FieldVO newFieldVO;
		Node node;

		for (int j = 0; j < fieldNodeList.getLength(); j++) {
			node = fieldNodeList.item(j);

			if ("bit".equalsIgnoreCase(node.getNodeName())) {
				bitNum = Integer.parseInt(ISOUtils.getAttr(node, "num", ""));
				fieldFound = null;

				if (treeFieldList == null || treeFieldList.size() == 0)
					throw new ParseException("It was not possible to parse the XML. At the XML it appears to have children, but there is not children at the ISO structure.");

				for (FieldVO field : treeFieldList) {
					if (field.getBitNum().intValue() == bitNum) {
						fieldFound = field;
						break;
					}
				}

				if (fieldFound == null){
					throw new ParseException("It was not possible to parse the XML. The bit number was not found at the ISO structure.");
				}

				newFieldVO = fieldFound.getInstanceCopy();

				newFieldVO.setPresent(true);
				newFieldVO.setTlvType(ISOUtils.getAttr(node, "tag", ""));
				newFieldVO.setTlvLength(ISOUtils.getAttr(node, "length", ""));
				newFieldVO.setValue(ISOUtils.getAttr(node, "value", ""));

				if (node.getChildNodes().getLength() > 0){
					newFieldVO.setFieldList(getFieldsFromXML(node.getChildNodes(), newFieldVO.getFieldList()));
				}

				fieldList.add(newFieldVO);
			}
		}

		return fieldList;
	}

	public void updateFromMessageVO(Iso8583Config isoConfig) throws ParseException {
		isoMessage = new ISOMessage(isoConfig, messageVO);
	}

	public void updateFromMessageVO(Iso8583Config isoConfig, MessageVO messageVO) throws ParseException {
		isoMessage = new ISOMessage(isoConfig, messageVO);
	}

	public void updateFromPayload(Iso8583Config isoConfig, byte[] bytes) throws ParseException {
		try {
			isoMessage = new ISOMessage(isoConfig, bytes, messageVO);
			setMessageVO(isoMessage.getMessageVO());
		}
		catch (ParseException x) {
			x.printStackTrace();
		}
	}

	public ISOMessage getIsoMessage() {
		return isoMessage;
	}

	public void setReadOnly() {
		setFieldListsReadOnly(fieldList);
	}

	public static void setFieldListsReadOnly(final List<PayloadField> fieldList){
		fieldList.forEach(field->{
			setFieldListsReadOnly(field.subfieldList);
		});
	}
	
	public void setMessage(PayloadMessageConfig payloadMessageConfig, Iso8583Config isoConfig, String msgType) {
		int totalMessages = isoConfig.getConfigTreeNode().size();
		MessageVO treeNode;
		MessageVO messageVO = null;
		MessageVO selectedMessageVO = null;
		for (int messageIndex = 0; messageIndex < totalMessages; messageIndex++) {
			treeNode = isoConfig.getConfigTreeNode().get(messageIndex);
			if (treeNode instanceof MessageVO) {
				messageVO = treeNode;
				
				if (msgType != null && messageVO.getType().equals(msgType)) selectedMessageVO = messageVO;
			}
		}
		payloadMessageConfig.setMessageVO(selectedMessageVO); 
	}

	private class PayloadField {

		boolean isSubfield;
		private FieldVO fieldVO;
		private FieldVO superFieldVO;

		private ArrayList<PayloadField> subfieldList;

		private PayloadField(FieldVO fieldVO, FieldVO superfieldVO, String superFieldBitNum) {

			this.isSubfield = (superfieldVO != null);
			this.superFieldVO = superfieldVO;
			this.fieldVO = fieldVO.getInstanceCopy();
			this.fieldVO.setFieldList(new ArrayList<FieldVO>());

			if (isSubfield) {
				superFieldVO.getFieldList().add(this.fieldVO);
			}
			else {
				messageVO.getFieldList().add(this.fieldVO);
			}

			numLines++;
		}

		private PayloadField addSubline(FieldVO fieldVO, FieldVO superfieldVO, String superFieldBitNum) {
			PayloadField newPayloadField = new PayloadField(fieldVO, superfieldVO, superFieldBitNum);
			subfieldList.add(newPayloadField);
			return newPayloadField;
		}

		private FieldVO getFieldVO() {
			return fieldVO;
		}
	}
}