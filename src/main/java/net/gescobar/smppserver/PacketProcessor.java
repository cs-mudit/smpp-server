package net.gescobar.smppserver;

import net.gescobar.smppserver.packet.SmppRequest;

import com.cloudhopper.smpp.SmppSession;



public interface PacketProcessor {

	
	void processPacket(SmppRequest packet, ResponseSender responseSender);
	
}
