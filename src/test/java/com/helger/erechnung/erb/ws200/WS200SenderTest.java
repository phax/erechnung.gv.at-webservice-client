/**
 * Copyright (C) 2014-2020 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.read.DOMReader;

import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryEmbeddedAttachmentType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryResponseType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliverySettingsType;

/**
 * Unit test class for class {@link WS200Sender}.
 *
 * @author Philip Helger
 */
public final class WS200SenderTest
{
  private static final String USP_WS_USERNAME = "xx";
  private static final String USP_WS_PASSWORD = "yy";

  @Test
  public void testURLs ()
  {
    assertNotNull (WS200Sender.ENDPOINT_URL_PRODUCTION);
    assertNotNull (WS200Sender.ENDPOINT_URL_TEST);
  }

  /**
   * Basic test case. It is ignored by default, since no test username and
   * password are present. After setting {@code USP_WS_USERNAME} and
   * {@code USP_WS_PASSWORD} constants in this class, this test can be
   * "un-ignored".
   */
  @Test
  @Ignore
  public void testDeliverInvoiceViaDOMNode ()
  {
    final Node aXMLDocument = DOMReader.readXMLDOM (new ClassPathResource ("test-invoices/ebi40.xml"));
    assertNotNull ("Failed to read example invoice", aXMLDocument);

    final WS200Sender aSender = new WS200Sender (USP_WS_USERNAME, USP_WS_PASSWORD);
    aSender.setDebugMode (true);
    aSender.setTestVersion (true);
    // Send to test system?
    if (false)
      aSender.setURL (WS200Sender.ENDPOINT_URL_TEST);

    // Namespace mapping is required for ebInterface 4.x
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("eb", "http://www.ebinterface.at/schema/4p0/");
    aNSCtx.addMapping ("dsig", "http://www.w3.org/2000/09/xmldsig#");
    aNSCtx.addMapping ("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    aSender.setNamespaceContext (aNSCtx);

    // No attachments
    final List <DeliveryEmbeddedAttachmentType> aAttachments = null;

    final DeliverySettingsType aSettings = new DeliverySettingsType ();
    // Perform only technical validation
    aSettings.setTest (Boolean.TRUE);

    // Deliver it
    final DeliveryResponseType aResult = aSender.deliverInvoice (aXMLDocument, aAttachments, aSettings);
    assertNotNull (aResult.toString (), aResult.getSuccess ());
  }

  /**
   * Basic test case. It is ignored by default, since no test username and
   * password are present. After setting {@code USP_WS_USERNAME} and
   * {@code USP_WS_PASSWORD} constants in this class, this test can be
   * "un-ignored".
   */
  @Test
  @Ignore
  public void testDeliverInvoiceViaByteArray ()
  {
    final byte [] aXMLBytes = StreamHelper.getAllBytes (new ClassPathResource ("test-invoices/ebi40.xml"));
    assertNotNull ("Failed to read example invoice", aXMLBytes);

    final WS200Sender aSender = new WS200Sender (USP_WS_USERNAME, USP_WS_PASSWORD);
    aSender.setDebugMode (true);
    aSender.setTestVersion (true);
    // Send to test system
    if (false)
      aSender.setURL (WS200Sender.ENDPOINT_URL_TEST);

    // No attachments
    final List <DeliveryEmbeddedAttachmentType> aAttachments = null;

    final DeliverySettingsType aSettings = new DeliverySettingsType ();
    // Perform only technical validation
    aSettings.setTest (Boolean.TRUE);

    // Deliver it
    final DeliveryResponseType aResult = aSender.deliverInvoice (aXMLBytes, aAttachments, aSettings);
    assertNotNull (aResult.toString (), aResult.getSuccess ());
  }
}
