/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.charset.CharsetManager;
import com.helger.commons.url.URLHelper;
import com.helger.commons.ws.WSClientConfig;
import com.helger.commons.ws.WSHelper;
import com.helger.commons.xml.serialize.write.XMLWriter;
import com.helger.commons.xml.serialize.write.XMLWriterSettings;
import com.helger.erechnung.erb.ws.AbstractWSSender;
import com.helger.erechnung.erb.ws.SOAPAddWSSEHeaderHandler;

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

/**
 * A wrapper for invoking the Webservice 1.2 for ER&gt;B - E-Rechnung an den
 * Bund. The technical details can be found at
 *
 * @see "https://www.erb.gv.at/erb?p=info_channel_ws&tab=ws12"
 * @author Philip Helger
 */
@NotThreadSafe
public class WS120Sender extends AbstractWSSender <WS120Sender>
{
  public static final URL ENDPOINT_URL_PRODUCTION = URLHelper.getAsURL ("https://txm.portal.at/at.gv.bmf.erb/V1");
  public static final URL ENDPOINT_URL_TEST = URLHelper.getAsURL ("https://txm.portal.at/at.gv.bmf.erb.test/V1");

  // Logger to use
  private static final Logger s_aLogger = LoggerFactory.getLogger (WS120Sender.class);

  public WS120Sender (@Nonnull @Nonempty final String sWebserviceUsername,
                      @Nonnull @Nonempty final String sWebservicePassword)
  {
    super (sWebserviceUsername, sWebservicePassword);
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
   *        <code>null</code>. It may be in any of the formats supported by
   *        ER&gt;B (ebInterface 3.0, 3.02, 4.0, 4.1 or UBL 2.0, 2.1).
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
    WSHelper.enableSoapLogging (isDebugMode ());

    // Convert XML node to a String
    final XMLWriterSettings aXWS = new XMLWriterSettings ().setCharset (getInvoiceEncoding ());
    final String sInvoiceString = XMLWriter.getNodeAsString (aOriginalInvoice, aXWS);
    if (sInvoiceString == null)
    {
      s_aLogger.error ("Failed to serialize the specified XML document to a String!");
      return _createError ("document", "Failed to serialize the specified XML document to a String!");
    }

    // Prepare document
    final DocumentType aDocument = new DocumentType ();
    aDocument.setValue (CharsetManager.getAsBytes (sInvoiceString, getInvoiceEncoding ()));
    aDocument.setEncoding (getInvoiceEncoding ().name ());

    try
    {
      final WSClientConfig aWSClientConfig = new WSClientConfig (isTestVersion () ? ENDPOINT_URL_TEST
                                                                                  : ENDPOINT_URL_PRODUCTION);

      if (isTrustAllCertificates ())
      {
        // Maybe required to trust txm.portal.at depending on the installed OS
        // root certificates.
        aWSClientConfig.setSSLSocketFactoryTrustAll ();
      }

      if (isTrustAllHostnames ())
        aWSClientConfig.setHostnameVerifierTrustAll ();

      // Ensure the WSSE headers are added using our handler
      aWSClientConfig.addHandler (new SOAPAddWSSEHeaderHandler (getWebserviceUsername (), getWebservicePassword ()));

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
      s_aLogger.error ("Error uploading the document to ER>B Webservice 1.2!", ex);
      return _createError ("document",
                           ex.getFaultInfo () != null ? ex.getFaultInfo ().getMessage () : ex.getMessage ());
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
