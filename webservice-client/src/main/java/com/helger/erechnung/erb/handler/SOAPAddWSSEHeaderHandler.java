/**
 * Copyright (C) 2013-2014 Philip Helger
 * ph[at]phloc[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.erechnung.erb.handler;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.phloc.commons.ValueEnforcer;
import com.phloc.commons.annotations.Nonempty;

/**
 * A special SOAP handler that adds the WS Security headers as described in
 * https://www.erb.gv.at/erb?p=info_channel_ws&tab=ws20
 * 
 * @author Philip Helger
 */
public class SOAPAddWSSEHeaderHandler implements SOAPHandler <SOAPMessageContext>
{
  public static final String WSSE_NSURI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

  private final String m_sUSPWebserviceUsername;
  private final String m_sUSPWebservicePassword;

  public SOAPAddWSSEHeaderHandler (@Nonnull @Nonempty final String sUSPWebserviceUsername,
                                   @Nonnull @Nonempty final String sUSPWebservicePassword)
  {
    m_sUSPWebserviceUsername = ValueEnforcer.notEmpty (sUSPWebserviceUsername, "USP Webservice Username");
    m_sUSPWebservicePassword = ValueEnforcer.notEmpty (sUSPWebservicePassword, "USP Webservice Password");
  }

  @Nullable
  public Set <QName> getHeaders ()
  {
    return null;
  }

  @Override
  public boolean handleMessage (@Nonnull final SOAPMessageContext aContext)
  {
    final boolean bOutbound = ((Boolean) aContext.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue ();
    if (bOutbound)
    {
      try
      {
        final SOAPMessage aMsg = aContext.getMessage ();

        // Ensure a header is present
        SOAPHeader aHeader = aMsg.getSOAPHeader ();
        if (aHeader == null)
          aHeader = aMsg.getSOAPPart ().getEnvelope ().addHeader ();

        // Add the WSSE stuff
        final SOAPElement aSecurity = aHeader.addChildElement (new QName (WSSE_NSURI, "Security"));
        final SOAPElement aUsernameToken = aSecurity.addChildElement (new QName (WSSE_NSURI, "UsernameToken"));
        aUsernameToken.addChildElement (new QName (WSSE_NSURI, "Username")).addTextNode (m_sUSPWebserviceUsername);
        aUsernameToken.addChildElement (new QName (WSSE_NSURI, "Password")).addTextNode (m_sUSPWebservicePassword);
      }
      catch (final Exception ex)
      {
        throw new RuntimeException (ex);
      }
    }
    return true;
  }

  public void close (final MessageContext aContext)
  {}

  public boolean handleFault (final SOAPMessageContext aContext)
  {
    return true;
  }
}
