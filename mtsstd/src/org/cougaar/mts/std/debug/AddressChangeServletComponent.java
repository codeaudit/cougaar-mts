package org.cougaar.mts.std.debug;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.BaseServlet;
import org.cougaar.mts.base.BoundComponent;
import org.cougaar.mts.base.MessageTransportRegistryService;

/**
 * This Component tests the {@link MessageTransportRegistryService} by invoking
 * its ipAddressChanged method.
 */
public class AddressChangeServletComponent
        extends BoundComponent {
    @Override
   public void start() {
        super.start();
        ServiceBroker sb = getServiceBroker();
        new AddressChangeServlet(sb);
    }

    private static class AddressChangeServlet
            extends BaseServlet {
        /**
       * 
       */
      private static final long serialVersionUID = 1L;
      MessageTransportRegistryService svc;

        AddressChangeServlet(ServiceBroker sb) {
            super(sb);
            svc = sb.getService(this, MessageTransportRegistryService.class, null);
        }

        @Override
      protected String getPath() {
            return "/mts/address-changed-test";
        }

        @Override
      protected String getTitle() {
            return "Address Change Test";
        }

        @Override
      protected void printPage(HttpServletRequest request, PrintWriter out) {
            svc.ipAddressChanged();
            out.println("Link Protocols have been reset");
        }

    }

}
