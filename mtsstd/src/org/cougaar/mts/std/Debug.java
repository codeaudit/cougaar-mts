package org.cougaar.core.mts;

public interface Debug
{
    public static final boolean DEBUG_TRANSPORT = 
	Boolean.getBoolean("org.cougaar.message.transport.debug");
    public static final boolean DEBUG_FLUSH = 
	Boolean.getBoolean("org.cougaar.message.transport.debug.flush");
    public static final boolean DEBUG_MULTICAST = 
	Boolean.getBoolean("org.cougaar.message.transport.debug.multicast");
}
