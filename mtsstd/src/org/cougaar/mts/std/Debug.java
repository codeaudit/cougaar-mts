package org.cougaar.core.mts;

public interface Debug
{
    public static final boolean DEBUG_TRANSPORT = 
	Boolean.getBoolean("org.cougaar.message.transport.debug");
}
