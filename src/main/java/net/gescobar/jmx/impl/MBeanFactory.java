package net.gescobar.jmx.impl;

import static net.gescobar.jmx.util.StringUtils.capitalize;
import static net.gescobar.jmx.util.StringUtils.decapitalize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import net.gescobar.jmx.Management;
import net.gescobar.jmx.ManagementException;
import net.gescobar.jmx.annotation.Description;
import net.gescobar.jmx.annotation.Impact;
import net.gescobar.jmx.annotation.ManagedAttribute;
import net.gescobar.jmx.annotation.ManagedOperation;


public final class MBeanFactory {
	
	/**
	 * Hide public constructor.
	 */
	private MBeanFactory() {}

	
	public static DynamicMBean createMBean(Object object) {
		
		if (object == null) {
			throw new IllegalArgumentException("No object specified.");
		}
     	
		Class<?> objectType = object.getClass();
		
		// retrieve description
		String description = "";
    	if (objectType.isAnnotationPresent(Description.class)) {
    	    description = objectType.getAnnotation(Description.class).value();
    	}
    	
    	// build attributes and operations
    	Method[] methods = objectType.getMethods();
    	MethodHandler methodHandler = new MBeanFactory().new MethodHandler( objectType );
    	for (Method method : methods) {
    		methodHandler.handleMethod(method);
    	}
    	
    	// build the MBeanInfo
    	MBeanInfo mBeanInfo = new MBeanInfo(objectType.getName(), description, methodHandler.getMBeanAttributes(), 
    			new MBeanConstructorInfo[0], methodHandler.getMBeanOperations(), new MBeanNotificationInfo[0]);
    	
    	// create the MBean
	    return new MBeanImpl(object, mBeanInfo);
	    
	}
    
    
    private class MethodHandler {
    	
    	/**
    	 * The class of the object.
    	 */
    	private Class<?> objectType;
    	
    	
    	private Collection<MBeanAttributeInfo> mBeanAttributes = new ArrayList<MBeanAttributeInfo>();
    	
    	
    	private Collection<MBeanOperationInfo> mBeanOperations = new ArrayList<MBeanOperationInfo>();
    	
    	
    	public MethodHandler(Class<?> objectType) {
    		this.objectType = objectType;
    	}
    	
    	
    	public void handleMethod(Method method) throws ManagementException {
    		
    		boolean hasManagedAttribute = method.isAnnotationPresent(ManagedAttribute.class);
    		boolean hasManagedOperation = method.isAnnotationPresent(ManagedOperation.class);
    		
    		if (hasManagedAttribute && hasManagedOperation) {
    			throw new ManagementException("Method " + method.getName() + " cannot have both ManagedAttribute and " +
    					"ManagedOperation annotations.");
    		}
    		
    		if (hasManagedAttribute) {
    			handleManagedAttribute(method);
    		} 
    		
    		if (hasManagedOperation) {
    			handleManagedOperation(method);
    		}
    		
    	}

    	
    	public MBeanAttributeInfo[] getMBeanAttributes() {
    		return mBeanAttributes.toArray( new MBeanAttributeInfo[0] );
    	}
    	
    	
    	public MBeanOperationInfo[] getMBeanOperations() {
    		return mBeanOperations.toArray( new MBeanOperationInfo[0] );
    	}
    	
    	private void handleManagedAttribute(Method method) {
    		
    		// validate if the method is a getter or setter
    		Method getterMethod = isGetterMethod(method) ? method : null;
    		Method setterMethod = isSetterMethod(method) ? method : null;
    		
    		if (getterMethod == null && setterMethod == null) {
    			// not a getter or setter
    			throw new ManagementException("Method " + method.getName() + " is annotated as ManagedAttribute " +
    					"but doesn't looks like a valid getter or setter.");
    		} 
    		
    		// retrieve the attribute name from the method name
    		String attributeName = method.getName().startsWith("is") ? 
    				decapitalize( method.getName().substring(2) ) : decapitalize( method.getName().substring(3) );
    		
    		// retrieve the attribute type from the setter argument type or the getter return type
    		Class<?> attributeType = setterMethod != null ? 
    				method.getParameterTypes()[0] : method.getReturnType();
    		
    		// find the missing method
    		getterMethod = getterMethod == null ? findGetterMethod(objectType, attributeName) : getterMethod;
    		setterMethod = setterMethod == null ? findSetterMethod(objectType, attributeName, attributeType) : setterMethod;
    		
    		boolean existsAttribute = existsAttribute(mBeanAttributes, attributeName, attributeType);
	    	if ( !existsAttribute ) {
	    		
	    		// add the MBeanAttribute to the collection
	    		MBeanAttributeInfo mBeanAttribute = buildMBeanAttribute(attributeName, attributeType, getterMethod, 
	    				setterMethod, method);
	    		if (mBeanAttribute != null) { // it can be null if it is neither readable or writable
	    			mBeanAttributes.add( mBeanAttribute );
	    		}
	    		
	    	} else {
	    		// both getter and setter are annotated ... throw exception
	    		throw new ManagementException("Both getter and setter are annotated for attribute " + 
	    				attributeName + ". Please remove one of the annotations.");
	    	}
    		
    	}
    	
    	
    	private boolean isGetterMethod(Method method) {
    		return (method.getName().startsWith("get") || method.getName().startsWith("is")) 
    				&& (!method.getReturnType().equals(Void.TYPE) && method.getParameterTypes().length == 0);
    	}
    	
    	
    	private boolean isSetterMethod(Method method) {
    		return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE) 
    				&& method.getParameterTypes().length == 1;
    	}
    	
    	
    	private Method findGetterMethod(Class<?> objectType, String attributeName) {
        	
        	try {
        		return objectType.getMethod( "get" + capitalize(attributeName) );
        	} catch (NoSuchMethodException e) {}
        	
        	try {
        		return objectType.getMethod( "is" + capitalize(attributeName) );
        	} catch (NoSuchMethodException e) {}
        	
        	return null;
        }
        
    	
        private Method findSetterMethod(Class<?> objectType, String name, Class<?> attributeType) {
        	
        	try { 
        		return objectType.getMethod( "set" + capitalize(name), attributeType );
        	} catch (NoSuchMethodException e) {
        		return null;
        	}
        	
        }
    	
    	
    	private boolean existsAttribute(Collection<MBeanAttributeInfo> mBeanAttributes, String attributeName, Class<?> attributeType) {
    		
    		for (MBeanAttributeInfo mBeanAttribute : mBeanAttributes) {
    			if (mBeanAttribute.getName().equals(attributeName) 
    					&& mBeanAttribute.getType().equals(attributeType.getName())) {
    				return true;
    			}
    		}
    		
    		return false;
    		
    	}
    	
    	
    	private MBeanAttributeInfo buildMBeanAttribute(String attributeName, 
    			Class<?> attributeType, Method getterMethod, Method setterMethod, Method annotatedMethod) {
        	
        	ManagedAttribute managedAttribute = annotatedMethod.getAnnotation(ManagedAttribute.class);
        	
        	// it's readable if the annotation has readable=true (which is the default) and the getter method exists
    		boolean readable = managedAttribute.readable() && getterMethod != null;
    		
    		// it's writable if the annotation has writable=true (which is the default) and the setter method exists.
    		boolean writable = managedAttribute.writable() && setterMethod != null;
    		
    		// it's IS if the getter method exists and starts with "is".
    		boolean isIs = getterMethod != null && getterMethod.getName().startsWith("is");

    		// only add the attribute if it is readable and writable
    		if (readable || writable) {
    			return new MBeanAttributeInfo(attributeName, attributeType.getName(), managedAttribute.description(), 
    					readable, writable, isIs);
    		}
    		
    		return null;
    		
        }
    	
       
    	private void handleManagedOperation(Method method) {
    		
    		// build the MBeanParameterInfo array from the parameters of the method
    		MBeanParameterInfo[] mBeanParameters = buildMBeanParameters( method.getParameterTypes(), 
    				method.getParameterAnnotations() );
    		
    		ManagedOperation managedOperation = method.getAnnotation(ManagedOperation.class);
			Impact impact = managedOperation.impact();
		
			mBeanOperations.add( new MBeanOperationInfo(method.getName(), managedOperation.description(), 
					mBeanParameters, method.getReturnType().getName(), impact.getCode()) );
		
    	}
    	
    	
    	private MBeanParameterInfo[] buildMBeanParameters(Class<?>[] paramsTypes, Annotation[][] paramsAnnotations) {
        	
        	MBeanParameterInfo[] mBeanParameters = new MBeanParameterInfo[paramsTypes.length];
        	
        	for (int i=0; i < paramsTypes.length; i++) {

    			MBeanParameterInfo parameterInfo = new MBeanParameterInfo("param" + i, paramsTypes[i].getName(), "");
    		    mBeanParameters[i] = parameterInfo;
    		    
    		}
        	
        	return mBeanParameters;
        	
        }
    	
    }
    
}