/*
 * Copyright (C) 2014-2022 Philip Helger (www.helger.com)
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
package com.helger.erechnung.erb.ws120;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.exception.InitializationException;
import com.helger.erechnung.erb.ws.AbstractWSSender;
import com.helger.erechnung.erb.ws.SOAPAddWSSEHeaderHandler;
import com.helger.wsclient.WSClientConfig;
import com.helger.wsclient.WSHelper;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

import at.gv.brz.eproc.erb.ws.documentupload._20121205.AttachmentType;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.DocumentType;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.SettingsType;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.UploadException;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.WSDocumentUploadService;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.Wsupload;
import at.gv.brz.schema.eproc.invoice_uploadstatus_1_0.TypeError;
import at.gv.brz.schema.eproc.invoice_uploadstatus_1_0.TypeErrorDetail;
import at.gv.brz.schema.eproc.invoice_uploadstatus_1_0.TypeErrorDetails;
import at.gv.brz.schema.eproc.invoice_uploadstatus_1_0.TypeUploadStatus;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

/**
 * A wrapper for invoking the Webservice 1.2 for e-Rechnung.gv.at. The technical
 * details can be found at
 *
 * @see "https://www.erb.gv.at/erb?p=info_channel_ws&tab=ws12"
 * @author Philip Helger
 */
@NotThreadSafe
public class WS120Sender extends AbstractWSSender <WS120Sender>
{
  public static final URL ENDPOINT_URL_PRODUCTION;
  public static final URL ENDPOINT_URL_TEST;

  static
  {
    try
    {
      ENDPOINT_URL_PRODUCTION = new URL ("https://txm.portal.at/at.gv.bmf.erb/V1");
      ENDPOINT_URL_TEST = new URL ("https://txm.portal.at/at.gv.bmf.erb.test/V1");
    }
    catch (final MalformedURLException ex)
    {
      throw new InitializationException ("Failed to init URL", ex);
    }
  }

  // Logger to use
  private static final Logger LOGGER = LoggerFactory.getLogger (WS120Sender.class);

  private URL m_aURL = ENDPOINT_URL_PRODUCTION;

  public WS120Sender (@Nonnull @Nonempty final String sWebserviceUsername, @Nonnull @Nonempty final String sWebservicePassword)
  {
    super (sWebserviceUsername, sWebservicePassword);
  }

  @Nonnull
  public final URL getURL ()
  {
    return m_aURL;
  }

  @Nonnull
  public final WS120Sender setURL (@Nonnull final URL aURL)
  {
    ValueEnforcer.notNull (aURL, "URL");
    m_aURL = aURL;
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
    aDetails.addErrorDetail (aDetail);
    aError.setErrorDetails (aDetails);
    ret.setError (aError);
    return ret;
  }

  /**
   * Protected method to be overridden.
   *
   * @param aWSClientConfig
   *        Client config to be modified. May not be <code>null</code>.
   */
  @OverrideOnDemand
  protected void modifyWSClientConfig (@Nonnull final WSClientConfig aWSClientConfig)
  {}

  /**
   * This is the main sending routine. It can be invoked multiple times with
   * different invoices.
   *
   * @param aOriginalInvoice
   *        The original invoice in an XML representation. May not be
   *        <code>null</code>. It may be in any of the formats supported by
   *        ER&gt;B (ebInterface 4.x, 5.x or UBL 2.x).
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

    // Convert XML node to a byte array
    final XMLWriterSettings aXWS = new XMLWriterSettings ().setCharset (getInvoiceEncoding ()).setNamespaceContext (getNamespaceContext ());
    final byte [] aInvoiceBytes = XMLWriter.getNodeAsBytes (aOriginalInvoice, aXWS);
    if (aInvoiceBytes == null)
    {
      LOGGER.error ("Failed to serialize the specified XML document");
      return _createError ("document", "Failed to serialize the specified XML document");
    }

    if (false)
      LOGGER.info ("Created XML:\n" + new String (aInvoiceBytes, getInvoiceEncoding ()));

    return deliverInvoice (aInvoiceBytes, aAttachments, aSettings);
  }

  /**
   * This is the main sending routine. It can be invoked multiple times with
   * different invoices.
   *
   * @param aInvoiceBytes
   *        The byte array representation of the XML invoice to be send. May not
   *        be <code>null</code>. It may be in any of the formats supported by
   *        ER&gt;B (ebInterface 4.x, 5.x or UBL 2.x).
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
  public TypeUploadStatus deliverInvoice (@Nonnull final byte [] aInvoiceBytes,
                                          @Nullable final List <AttachmentType> aAttachments,
                                          @Nonnull final SettingsType aSettings)
  {
    ValueEnforcer.notNull (aInvoiceBytes, "InvoiceBytes");
    ValueEnforcer.notNull (aSettings, "Settings");

    // Some debug output
    WSHelper.enableSoapLogging (isDebugMode ());

    // Prepare document
    final DocumentType aDocument = new DocumentType ();
    aDocument.setValue (aInvoiceBytes);
    aDocument.setEncoding (getInvoiceEncoding ().name ());

    try
    {
      final WSClientConfig aWSClientConfig = new WSClientConfig (m_aURL);

      if (isTrustAllCertificates ())
      {
        // Maybe required to trust txm.portal.at depending on the installed OS
        // root certificates.
        aWSClientConfig.setSSLSocketFactoryTrustAll ();
      }

      if (isTrustAllHostnames ())
        aWSClientConfig.setHostnameVerifierTrustAll ();

      // Ensure the WSSE headers are added using our handler
      aWSClientConfig.handlers ().add (new SOAPAddWSSEHeaderHandler (getWebserviceUsername (), getWebservicePassword ()));

      // Customizing callback
      modifyWSClientConfig (aWSClientConfig);

      // Invoke WS
      final WSDocumentUploadService aService = new WSDocumentUploadService ();
      final Wsupload aPort = aService.getWSDocumentUploadPort ();
      aWSClientConfig.applyWSSettingsToBindingProvider ((BindingProvider) aPort);

      // Main sending
      final TypeUploadStatus aResult = aPort.uploadDocument (aDocument, aAttachments, aSettings);
      return aResult;
    }
    catch (final UploadException ex)
    {
      LOGGER.error ("Error uploading the document to ER>B Webservice 1.2!", ex);
      return _createError ("document", ex.getFaultInfo () != null ? ex.getFaultInfo ().getMessage () : ex.getMessage ());
    }
    catch (final WebServiceException ex)
    {
      LOGGER.error ("Error transmitting the document to ER>B Webservice 1.2!", ex);
      return _createError ("webservice", ex.getMessage ());
    }
    catch (final Throwable t)
    {
      LOGGER.error ("Generic error invoking ER>B Webservice 1.2", t);
      return _createError ("general", t.getMessage ());
    }
  }
}
