package org.cougaar.core.mts;

/**
 * Standard message transport cutpoint constants. */
public interface MessageTransportCutpoints
{
    public static final int ServiceProxy = 0;
    public static final int SendQueue = 1;
    public static final int Router = 2;
    public static final int DestinationQueue = 3;
    public static final int DestinationLink = 4;
    public static final int ReceiveQueue = 5;
    public static final int ReceiveLink = 6;
    public static final int RemoteProxy = 7;
    public static final int RemoteImpl = 8;
    public static final int RmiClientOutputStream = 9;
    public static final int RmiClientInputStream = 10;
    public static final int RmiServerOutputStream = 11;
    public static final int RmiServerInputStream = 12;
}
