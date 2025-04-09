package com.vis.bob.iso8583.client;

import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;

public abstract class CallbackAction {

	public abstract void dataReceived(Iso8583Config isoConfig, SocketPayload payload) throws ParseException;

	public void keepalive() { }
}
