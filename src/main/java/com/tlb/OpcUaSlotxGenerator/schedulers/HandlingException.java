package com.tlb.OpcUaSlotxGenerator.schedulers;

public class HandlingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public HandlingException() { super("Unspecified error when processing message"); }
	public HandlingException(String message) { super(message); }
	public HandlingException(String message, Throwable cause) { super(message, cause); }
	public HandlingException(Throwable cause) { super(cause); }
	public HandlingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{ super(message, cause, enableSuppression, writableStackTrace); }
};
