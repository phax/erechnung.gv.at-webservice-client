/*
 * Copyright (C) 2014-2025 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
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
package com.helger.erechnung.erb.ws;

import java.util.Set;

import javax.xml.namespace.QName;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;

/**
 * A special SOAP handler that adds the WS Security headers for the
 * txm.portal.at machine as described on the e-Rechnung.gv.at web site.
 *
 * @see "https://www.erb.gv.at/erb?p=info_channel_ws&tab=ws20"
 * @author Philip Helger
 */
public class SOAPAddWSSEHeaderHandler implements SOAPHandler <SOAPMessageContext>
{
  /** The required XML namespace URI */
  public static final String WSSE_NSURI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  public static final String WSSE_PREFIX = "wsse";

  private final String m_sUSPWebserviceUsername;
  private final String m_sUSPWebservicePassword;

  public SOAPAddWSSEHeaderHandler (@Nonnull @Nonempty final String sUSPWebserviceUsername,
                                   @Nonnull @Nonempty final String sUSPWebservicePassword)
  {
    m_sUSPWebserviceUsername = ValueEnforcer.notEmpty (sUSPWebserviceUsername, "USP Webservice Username");
    m_sUSPWebservicePassword = ValueEnforcer.notEmpty (sUSPWebservicePassword, "USP Webservice Password");
  }

  /**
   * @return The USP web service user name as specified in the constructor.
   *         Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getUSPWebserviceUsername ()
  {
    return m_sUSPWebserviceUsername;
  }

  /**
   * @return The USP web service password as specified in the constructor.
   *         Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getUSPWebservicePassword ()
  {
    return m_sUSPWebservicePassword;
  }

  @Nullable
  @CodingStyleguideUnaware
  public Set <QName> getHeaders ()
  {
    return null;
  }

  @Override
  public boolean handleMessage (@Nonnull final SOAPMessageContext aContext)
  {
    // Handle only outbound messages (in contrast to inbound messages)
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
        final SOAPElement aSecurity = aHeader.addChildElement (new QName (WSSE_NSURI, "Security", WSSE_PREFIX));
        final SOAPElement aUsernameToken = aSecurity.addChildElement (new QName (WSSE_NSURI, "UsernameToken", WSSE_PREFIX));
        aUsernameToken.addChildElement (new QName (WSSE_NSURI, "Username", WSSE_PREFIX)).addTextNode (m_sUSPWebserviceUsername);
        aUsernameToken.addChildElement (new QName (WSSE_NSURI, "Password", WSSE_PREFIX)).addTextNode (m_sUSPWebservicePassword);
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("uspWebserviceUsername", m_sUSPWebserviceUsername)
                                       .appendPassword ("uspWebservicePassword")
                                       .getToString ();
  }
}
