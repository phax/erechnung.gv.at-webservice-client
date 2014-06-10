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
package com.helger.erechnung.erb.ws120;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.erechnung.erb.handler.SOAPAddWSSEHeaderHandler;
import com.phloc.commons.ValueEnforcer;
import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.charset.CCharset;
import com.phloc.commons.charset.CharsetManager;
import com.phloc.commons.xml.serialize.XMLWriter;
import com.phloc.commons.xml.serialize.XMLWriterSettings;

/**
 * A wrapper for invoking the Webservice 1.2 for ER>B - E-Rechnung an den Bund.
 * 
 * @author Philip Helger
 */
public class WS120Sender
{
  public static final String ENDPOINT_URL_PRODUCTION = "https://txm.portal.at/at.gv.bmf.erb/V1";
  public static final String ENDPOINT_URL_TEST = "https://txm.portal.at/at.gv.bmf.erb.test/V1";

  // Default encoding according to XSD
  public static final Charset DEFAULT_INVOICE_ENCODING = CCharset.CHARSET_UTF_8_OBJ;
  public static final boolean DEFAULT_DEBUG = false;
  public static final boolean DEFAULT_TEST_FLAG = false;
  public static final boolean DEFAULT_TEST_VERSION = false;

  // Logger to use
  private static final Logger s_aLogger = LoggerFactory.getLogger (WS120Sender.class);

  private final String m_sWebserviceUsername;
  private final String m_sWebservicePassword;
  private Charset m_aInvoiceEncoding = DEFAULT_INVOICE_ENCODING;
  private boolean m_bDebugMode = DEFAULT_DEBUG;
  private boolean m_bTestVersion = DEFAULT_TEST_VERSION;

  public WS120Sender (@Nonnull @Nonempty final String sWebserviceUsername,
                      @Nonnull @Nonempty final String sWebservicePassword)
  {
    m_sWebserviceUsername = ValueEnforcer.notEmpty (sWebserviceUsername, "Webservice Username");
    m_sWebservicePassword = ValueEnforcer.notEmpty (sWebservicePassword, "Webservice Password");
  }

  /**
   * @return The web service user name as specified in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getWebserviceUsername ()
  {
    return m_sWebserviceUsername;
  }

  /**
   * @return The web service password as specified in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getWebservicePassword ()
  {
    return m_sWebservicePassword;
  }

  /**
   * @return The encoding of the XML invoice to be used. The default value is
   *         {@link #DEFAULT_INVOICE_ENCODING}.
   */
  @Nonnull
  public Charset getInvoiceEncoding ()
  {
    return m_aInvoiceEncoding;
  }

  /**
   * Set the encoding of the original XML invoice to be used. The default value
   * is {@link #DEFAULT_INVOICE_ENCODING}.
   * 
   * @param aInvoiceEncoding
   *        The new encoding to be used. May not be <code>null</code>.
   * @return this
   */
  @Nonnull
  public WS120Sender setInvoiceEncoding (@Nonnull final Charset aInvoiceEncoding)
  {
    ValueEnforcer.notNull (aInvoiceEncoding, "InvoiceEncoding");
    m_aInvoiceEncoding = aInvoiceEncoding;
    return this;
  }

  /**
   * @return <code>true</code> if the debug mode is enabled, <code>false</code>
   *         if not. The default value is {@link #DEFAULT_DEBUG}.
   */
  public boolean isDebugMode ()
  {
    return m_bDebugMode;
  }

  /**
   * Change the setting of the debug mode. Enabling the debug mode means, that
   * the exchanged Webservice messages are logged to stdout.
   * 
   * @param bDebugMode
   *        The new value of the debug flag. <code>true</code> to enable debug
   *        mode, <code>false</code> to disable it.
   * @return this
   */
  @Nonnull
  public WS120Sender setDebugMode (final boolean bDebugMode)
  {
    m_bDebugMode = bDebugMode;
    return this;
  }

  /**
   * @return <code>true</code> if the Webservice of the test version should be
   *         invoked, false if the Webservice of the production version should
   *         be invoked. Please note that to invoke the test version, the
   *         appropriate access right in the USP must be assigned.
   */
  public boolean isTestVersion ()
  {
    return m_bTestVersion;
  }

  @Nonnull
  public WS120Sender setTestVersion (final boolean bTestVersion)
  {
    m_bTestVersion = bTestVersion;
    return this;
  }

  @Nonnull
  private static TypeUploadStatus _createError (@Nonnull final String sField, @Nonnull final String sMessage)
  {
    final TypeUploadStatus ret = new TypeUploadStatus ();
    final TypeError aError = new TypeError ();
    final TypeErrorDetails aDetails = new TypeErrorDetails ();
    final TypeErrorDetail aDetail = new TypeErrorDetail ();
    aDetail.setField (sField);
    aDetail.setMessage (sMessage);
    aDetails.getErrorDetail ().add (aDetail);
    aError.setErrorDetails (aDetails);
    ret.setError (aError);
    return ret;
  }

  @Nonnull
  public TypeUploadStatus send (@Nonnull final Document aOriginalInvoice,
                                @Nullable final List <AttachmentType> aAttachments,
                                @Nonnull final SettingsType aSettings)
  {
    ValueEnforcer.notNull (aOriginalInvoice, "OriginalInvoice");
    ValueEnforcer.notNull (aSettings, "Settings");

    // Some debug output
    System.setProperty ("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", Boolean.toString (m_bDebugMode));
    System.setProperty ("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump",
                        Boolean.toString (m_bDebugMode));

    // Convert XML node to a String
    final XMLWriterSettings aXWS = new XMLWriterSettings ().setCharset (m_aInvoiceEncoding);
    final String sInvoiceString = XMLWriter.getNodeAsString (aOriginalInvoice, aXWS);
    if (sInvoiceString == null)
    {
      s_aLogger.error ("Failed to serialize the specified XML document to a String!");
      return _createError ("document", "Failed to serialize the specified XML document to a String!");
    }

    // Prepare document
    final DocumentType aDocument = new DocumentType ();
    aDocument.setValue (CharsetManager.getAsBytes (sInvoiceString, m_aInvoiceEncoding));
    aDocument.setEncoding (m_aInvoiceEncoding.name ());

    try
    {
      // Invoke WS
      final WSDocumentUploadService aService = new WSDocumentUploadService ();
      final Wsupload aPort = aService.getWSDocumentUploadPort ();
      final BindingProvider aBP = (BindingProvider) aPort;

      // Determine where to send the WS request to
      aBP.getRequestContext ().put (BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                    m_bTestVersion ? ENDPOINT_URL_TEST : ENDPOINT_URL_PRODUCTION);

      // Ensure the WSSE headers are added
      @SuppressWarnings ("rawtypes")
      final List <Handler> aHandlers = new ArrayList <Handler> ();
      aHandlers.add (new SOAPAddWSSEHeaderHandler (m_sWebserviceUsername, m_sWebservicePassword));
      aBP.getBinding ().setHandlerChain (aHandlers);

      // Main sending
      final TypeUploadStatus aResult = aPort.uploadDocument (aDocument, aAttachments, aSettings);
      return aResult;
    }
    catch (final UploadException ex)
    {
      s_aLogger.error ("Error uploading the document!", ex);
      return _createError ("document", ex.getFaultInfo () != null ? ex.getFaultInfo ().getMessage () : ex.getMessage ());
    }
    catch (final WebServiceException ex)
    {
      s_aLogger.error ("Error transmitting the document!", ex);
      return _createError ("webservice", ex.getMessage ());
    }
  }
}
