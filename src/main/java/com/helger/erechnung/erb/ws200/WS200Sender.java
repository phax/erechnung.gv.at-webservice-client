/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
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
package com.helger.erechnung.erb.ws200;

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
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.commons.url.URLHelper;
import com.helger.erechnung.erb.ws.AbstractWSSender;
import com.helger.erechnung.erb.ws.SOAPAddWSSEHeaderHandler;
import com.helger.wsclient.WSClientConfig;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.xml.serialize.write.XMLWriterSettings;

import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliverInvoiceFaultInvoice;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryEmbeddedAttachmentType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryErrorDetailType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryErrorType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryInvoiceType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryResponseType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliverySettingsType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.WSInvoiceDeliveryPort;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.WSInvoiceDeliveryService;

/**
 * A wrapper for invoking the Webservice 2.0 for e-Rechnung.gv.at. The technical
 * details can be found at
 *
 * @see "https://www.erb.gv.at/erb?p=info_channel_ws&tab=ws20"
 * @author Philip Helger
 */
@NotThreadSafe
public class WS200Sender extends AbstractWSSender <WS200Sender>
{
  public static final URL ENDPOINT_URL_PRODUCTION = URLHelper.getAsURL ("https://txm.portal.at/at.gv.bmf.erb/V2");
  public static final URL ENDPOINT_URL_TEST = URLHelper.getAsURL ("https://txm.portal.at/at.gv.bmf.erb.test/V2");

  // Logger to use
  private static final Logger LOGGER = LoggerFactory.getLogger (WS200Sender.class);

  public WS200Sender (@Nonnull @Nonempty final String sWebserviceUsername,
                      @Nonnull @Nonempty final String sWebservicePassword)
  {
    super (sWebserviceUsername, sWebservicePassword);
  }

  @Nonnull
  private static DeliveryResponseType _createError (@Nonnull final String sField, @Nonnull final String sMessage)
  {
    return _createError (sField, CollectionHelper.newList (sMessage));
  }

  @Nonnull
  private static DeliveryResponseType _createError (@Nonnull final String sField,
                                                    @Nonnull final List <String> aMessages)
  {
    final DeliveryResponseType ret = new DeliveryResponseType ();
    final DeliveryErrorType aError = new DeliveryErrorType ();
    for (final String sMessage : aMessages)
    {
      final DeliveryErrorDetailType aDetail = new DeliveryErrorDetailType ();
      aDetail.setField (sField);
      aDetail.setMessage (sMessage);
      aError.getErrorDetail ().add (aDetail);
    }
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
  public DeliveryResponseType deliverInvoice (@Nonnull final Node aOriginalInvoice,
                                              @Nullable final List <DeliveryEmbeddedAttachmentType> aAttachments,
                                              @Nonnull final DeliverySettingsType aSettings)
  {
    ValueEnforcer.notNull (aOriginalInvoice, "OriginalInvoice");
    ValueEnforcer.notNull (aSettings, "Settings");

    // Some debug output
    SystemProperties.setPropertyValue ("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", isDebugMode ());
    SystemProperties.setPropertyValue ("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump",
                                       isDebugMode ());

    // Convert XML node to a String
    final XMLWriterSettings aXWS = new XMLWriterSettings ().setCharset (getInvoiceEncoding ());
    final String sInvoiceString = XMLWriter.getNodeAsString (aOriginalInvoice, aXWS);
    if (sInvoiceString == null)
    {
      LOGGER.error ("Failed to serialize the specified XML document to a String!");
      return _createError ("document", "Failed to serialize the specified XML document to a String!");
    }

    // Prepare document
    final DeliveryType aDelivery = new DeliveryType ();

    // Main invoice
    final DeliveryInvoiceType aInvoice = new DeliveryInvoiceType ();
    aInvoice.setValue (sInvoiceString.getBytes (getInvoiceEncoding ()));
    aInvoice.setEncoding (getInvoiceEncoding ().name ());
    aDelivery.setInvoice (aInvoice);

    // Embedded attachments
    aDelivery.setEmbeddedAttachment (aAttachments);

    // ER>B does not support external attachments!

    // Settings
    aDelivery.setSettings (aSettings);

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
      aWSClientConfig.handlers ()
                     .add (new SOAPAddWSSEHeaderHandler (getWebserviceUsername (), getWebservicePassword ()));

      // Invoke WS
      final WSInvoiceDeliveryService aService = new WSInvoiceDeliveryService ();
      final WSInvoiceDeliveryPort aPort = aService.getWSInvoiceDeliveryPort ();
      aWSClientConfig.applyWSSettingsToBindingProvider ((BindingProvider) aPort);

      // Main sending
      final DeliveryResponseType aResult = aPort.deliverInvoice (aDelivery);
      return aResult;
    }
    catch (final DeliverInvoiceFaultInvoice ex)
    {
      LOGGER.error ("Error uploading the document to ER>B Webservice 2.0!", ex);
      return _createError ("document",
                           ex.getFaultInfo () != null ? ex.getFaultInfo ().getMessage ()
                                                      : CollectionHelper.newList (ex.getMessage ()));
    }
    catch (final WebServiceException ex)
    {
      LOGGER.error ("Error transmitting the document to ER>B Webservice 2.0!", ex);
      return _createError ("webservice", ex.getMessage ());
    }
    catch (final Throwable t)
    {
      LOGGER.error ("Generic error invoking ER>B Webservice 2.0", t);
      return _createError ("general", t.getMessage ());
    }
  }
}
