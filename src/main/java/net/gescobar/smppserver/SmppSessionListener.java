package net.gescobar.smppserver;

/**
 * Implemented to listen the creation and destroy of sessions.
 * 
 * @author German Escobar
 */
public interface SmppSessionListener {

	
	void created(SmppSession session);
	
	void destroyed(SmppSession session);
	
}
