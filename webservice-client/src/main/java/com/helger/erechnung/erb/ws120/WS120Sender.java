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
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.erechnung.erb.handler.SOAPAddWSSEHeaderHandler;
import com.phloc.commons.ValueEnforcer;
import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.charset.CCharset;
import com.phloc.commons.charset.CharsetManager;
import com.phloc.commons.random.VerySecureRandom;
import com.phloc.commons.xml.serialize.XMLWriter;
import com.phloc.commons.xml.serialize.XMLWriterSettings;
import com.phloc.web.https.DoNothingTrustManager;
import com.phloc.web.https.HostnameVerifierAlwaysTrue;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * A wrapper for invoking the Webservice 1.2 for ER>B - E-Rechnung an den Bund.
 * 
 * @author Philip Helger
 */
@NotThreadSafe
public class WS120Sender
{
  public static final String ENDPOINT_URL_PRODUCTION = "https://txm.portal.at/at.gv.bmf.erb/V1";
  public static final String ENDPOINT_URL_TEST = "https://txm.portal.at/at.gv.bmf.erb.test/V1";

  // Default encoding according to XSD
  public static final Charset DEFAULT_INVOICE_ENCODING = CCharset.CHARSET_UTF_8_OBJ;
  public static final boolean DEFAULT_DEBUG = false;
  public static final boolean DEFAULT_TEST_VERSION = false;
  public static final boolean DEFAULT_TRUST_ALL_CERTIFICATES = false;
  public static final boolean DEFAULT_TRUST_ALL_HOSTNAMES = false;

  // Logger to use
  private static final Logger s_aLogger = LoggerFactory.getLogger (WS120Sender.class);

  private final String m_sWebserviceUsername;
  private final String m_sWebservicePassword;
  private Charset m_aInvoiceEncoding = DEFAULT_INVOICE_ENCODING;
  private boolean m_bDebugMode = DEFAULT_DEBUG;
  private boolean m_bTestVersion = DEFAULT_TEST_VERSION;
  private boolean m_bTrustAllCertificates = DEFAULT_TRUST_ALL_CERTIFICATES;
  private boolean m_bTrustAllHostnames = DEFAULT_TRUST_ALL_HOSTNAMES;

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
   *         invoked, <code>false</code> if the Webservice of the production
   *         version should be invoked. Please note that to invoke the test
   *         version, the appropriate access right in the USP must be assigned.
   *         The default value is {@link #DEFAULT_TEST_VERSION}.
   */
  public boolean isTestVersion ()
  {
    return m_bTestVersion;
  }

  /**
   * Change whether the test Webservice or the production Webservice should be
   * invoked. The default value is {@link #DEFAULT_TEST_VERSION}.
   * 
   * @param bTestVersion
   *        <code>true</code> to invoke the test Webservice, <code>false</code>
   *        to invoke the production Webservice.
   * @return this
   */
  @Nonnull
  public WS120Sender setTestVersion (final boolean bTestVersion)
  {
    m_bTestVersion = bTestVersion;
    return this;
  }

  /**
   * @return <code>true</code> if the Webservice connection trusts all
   *         certificates and therefore does not check for certificate
   *         revocation etc. If this is enabled, the security of transmission
   *         cannot be guaranteed! The default value is
   *         {@link #DEFAULT_TRUST_ALL_CERTIFICATES}.
   */
  public boolean isTrustAllCertificates ()
  {
    return m_bTrustAllCertificates;
  }

  /**
   * Change whether the the Webservice connection trusts all certificates and
   * therefore does not check for certificate revocation etc. If this is
   * enabled, the security of transmission cannot be guaranteed! The default
   * value is {@link #DEFAULT_TRUST_ALL_CERTIFICATES}.<br>
   * Internally a special {@link SSLContext} with a
   * {@link DoNothingTrustManager} is created.
   * 
   * @param bTrustAllCertificates
   *        <code>true</code> to lower the security level and disable the
   *        certificate check, or <code>false</code> to enable the certificate
   *        check.
   * @return this
   */
  @Nonnull
  public WS120Sender setTrustAllCertificates (final boolean bTrustAllCertificates)
  {
    m_bTrustAllCertificates = bTrustAllCertificates;
    return this;
  }

  /**
   * @return <code>true</code> if the Webservice connection does not check the
   *         hostname as specified in the certificate of the receiver. For
   *         ER&gt; using the txm.portal.at service, this should always be
   *         <code>false</code>. If this is enabled, the security of
   *         transmission cannot be guaranteed! The default value is
   *         {@link #DEFAULT_TRUST_ALL_HOSTNAMES}. For ER&gt;B the hostname
   *         check should always be enabled.
   */
  public boolean isTrustAllHostnames ()
  {
    return m_bTrustAllHostnames;
  }

  /**
   * Change whether the the Webservice connection should check the hostname as
   * specified in the certificate of the receiver or not. If this is enabled,
   * the security of transmission cannot be guaranteed! The default value is
   * {@link #DEFAULT_TRUST_ALL_HOSTNAMES}. For ER&gt;B the hostname check should
   * always be enabled.<br>
   * Internally a special {@link HostnameVerifierAlwaysTrue} is installed.
   * 
   * @param bTrustAllHostnames
   *        <code>true</code> to lower the security level and trust all
   *        hostnames, or <code>false</code> to enable the certificate hostname
   *        check.
   * @return this
   */
  @Nonnull
  public WS120Sender setTrustAllHostnames (final boolean bTrustAllHostnames)
  {
    m_bTrustAllHostnames = bTrustAllHostnames;
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

  /**
   * This is the main sending routine. It can be invoked multiple times with
   * different invoices.
   * 
   * @param aOriginalInvoice
   *        The original invoice in an XML representation. May not be
   *        <code>null</code>.
   * @param aAttachments
   *        An optional list of attachments to this invoice. If the list is non-
   *        <code>null</code> it must contain only non-<code>null</code>
   *        elements.
   * @param aSettings
   *        The settings element as specified by the ER&gt;B Webservice 1.2.
   *        Within this settings element e.g. the test-flag can be set. May not
   *        be <code>null</code>.
   * @return A non-<code>null</code> upload status as returned by the ER&gt;B
   *         Webservice. In case of an internal error, a corresponding error
   *         structure is created.
   */
  @Nonnull
  public TypeUploadStatus deliverInvoice (@Nonnull final Node aOriginalInvoice,
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

      if (m_bTrustAllCertificates)
      {
        // Maybe required to trust txm.portal.at depending on the installed OS
        // root certificates.
        final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
        aSSLCtx.init (null, new TrustManager [] { new DoNothingTrustManager () }, VerySecureRandom.getInstance ());
        // Use JAXWS and runtime JAXWS property
        aBP.getRequestContext ().put (JAXWSProperties.SSL_SOCKET_FACTORY, aSSLCtx.getSocketFactory ());
        aBP.getRequestContext ().put ("com.sun.xml.internal.ws.transport.https.client.SSLSocketFactory",
                                      aSSLCtx.getSocketFactory ());
      }

      if (m_bTrustAllHostnames)
      {
        // Should not be required for txm.portal.at
        final HostnameVerifier aHostnameVerifier = new HostnameVerifierAlwaysTrue ();
        // Use JAXWS and runtime JAXWS property
        aBP.getRequestContext ().put (JAXWSProperties.HOSTNAME_VERIFIER, aHostnameVerifier);
        aBP.getRequestContext ().put ("com.sun.xml.internal.ws.transport.https.client.hostname.verifier",
                                      aHostnameVerifier);
      }

      // Ensure the WSSE headers are added using our handler
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
      s_aLogger.error ("Error uploading the document to ER>B Webservice 1.2!", ex);
      return _createError ("document", ex.getFaultInfo () != null ? ex.getFaultInfo ().getMessage () : ex.getMessage ());
    }
    catch (final WebServiceException ex)
    {
      s_aLogger.error ("Error transmitting the document to ER>B Webservice 1.2!", ex);
      return _createError ("webservice", ex.getMessage ());
    }
    catch (final Throwable t)
    {
      s_aLogger.error ("Generic error invoking ER>B Webservice 1.2", t);
      return _createError ("general", t.getMessage ());
    }
  }
}
