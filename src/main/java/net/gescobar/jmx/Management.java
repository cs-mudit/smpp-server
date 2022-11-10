package net.gescobar.jmx;

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.gescobar.jmx.impl.MBeanFactory;


/**
 * <p>Provides methods to register and unregister objects as JMX MBeans.</p>
 * 
 * @author German Escobar
 */
public final class Management {
	
	/**
	 * Hide public constructor.
	 */
	private Management() {}

	
    public static void register(Object object, String name) throws InstanceAlreadyExistsException, ManagementException {
    	
    	if (object == null) {
    		throw new IllegalArgumentException("No object specified.");
    	}
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    		
    	DynamicMBean mBean = MBeanFactory.createMBean(object);
		
    	try { 
    		mBeanServer.registerMBean( mBean, new ObjectName(name) );
    	} catch (InstanceAlreadyExistsException e) {
    		throw e;
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }

    public static void unregister(String name) throws ManagementException {
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    	
    	try {
    		mBeanServer.unregisterMBean( new ObjectName(name) );
    	} catch (InstanceNotFoundException e) {
    		
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }
    
    public boolean isRegistered(String name) throws ManagementException {
    	
    	if (name == null || "".equals(name)) {
    		throw new IllegalArgumentException("No name specified.");
    	}
    	
    	MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    	if (mBeanServer == null) {
    		throw new ManagementException("No MBeanServer found.");
    	}
    	
    	try {
    		return mBeanServer.isRegistered( new ObjectName(name) );
    	} catch (Exception e) {
    		throw new ManagementException(e);
    	}
    	
    }
    
}
