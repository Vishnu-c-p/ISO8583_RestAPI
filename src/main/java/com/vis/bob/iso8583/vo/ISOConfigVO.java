package com.vis.bob.iso8583.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.stereotype.Component;

import com.vis.bob.iso8583.constants.DelimiterEnum;
import com.vis.bob.iso8583.constants.EncodingEnum;

/**
 * Representation of a ISO8583 Config.
 */
@XmlRootElement(name="iso8583")
@XmlType(propOrder={"delimiter", "headerEncoding", "headerSize", "messageList"})
@Component
public class ISOConfigVO {
	
    private DelimiterEnum delimiter;
    private EncodingEnum headerEncoding;
    private Integer headerSize;
    
    private boolean TPDU;
    private boolean StxEtx;
    
    private final List<MessageVO> messageList = new ArrayList<MessageVO>();
    
    public ISOConfigVO() {
    }
    
	public ISOConfigVO(DelimiterEnum delimiter) {
		super();
		this.delimiter = delimiter;
	}

	@XmlAttribute(name="delimiter")
	public DelimiterEnum getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(DelimiterEnum delimiter) {
		this.delimiter = delimiter;
	}

    @XmlAttribute(name="headerEncoding")
    public EncodingEnum getHeaderEncoding() {
        return headerEncoding;
    }

    public void setHeaderEncoding(EncodingEnum headerEncoding) {
        this.headerEncoding = headerEncoding;
    }

    @XmlAttribute(name="headerSize")
    public Integer getHeaderSize() {
        return headerSize;
    }

    public void setHeaderSize(Integer headerSize) {
        this.headerSize = headerSize;
    }

    public boolean TPDU() {
        return TPDU;
    }

    public boolean StxEtx() {
        return StxEtx;
    }

	public void setTPDU(boolean TPDU) {
		this.TPDU=TPDU;		
	}

	public void setStxEtx(boolean StxEtx) {
		this.StxEtx=StxEtx;		
	}
	
	@XmlElement(name="message")
	public List<MessageVO> getMessageList() {
		return Collections.unmodifiableList(messageList);
	}

	public void addMessage(final MessageVO message){
		messageList.add(message);
	}

	public void addAllMessages(final List<MessageVO> messages){
		messageList.addAll(messages);
	}

	@Override
	public String toString() {
		return "iso8583 "+delimiter.toString();
	}
}
