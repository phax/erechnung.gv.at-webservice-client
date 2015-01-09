/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
import org.xml.sax.SAXException;

import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryEmbeddedAttachmentType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliveryResponseType;
import at.gv.brz.eproc.erb.ws.invoicedelivery._201306.DeliverySettingsType;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.xml.serialize.DOMReader;

/**
 * Unit test class for class {@link WS200Sender}.
 *
 * @author Philip Helger
 */
public final class WS200SenderTest
{
  private static final String USP_WS_USERNAME = "xx";
  private static final String USP_WS_PASSWORD = "yy";

  /**
   * Basic test case. It is ignored by default, since no test username and
   * password are present. After setting {@link #USP_WS_USERNAME} and
   * {@link #USP_WS_PASSWORD} constants in this class, this test can be
   * "un-ignored".
   *
   * @throws SAXException
   *         in case XML reading fails
   */
  @Test
  @Ignore
  public void testDeliverInvoice1 () throws SAXException
  {
    final Node aXMLDocument = DOMReader.readXMLDOM (new ClassPathResource ("test-invoices/ebi40.xml"));
    assertNotNull ("Failed to read example invoice", aXMLDocument);

    final WS200Sender aSender = new WS200Sender (USP_WS_USERNAME, USP_WS_PASSWORD);
    aSender.setDebugMode (true);
    aSender.setTestVersion (true);

    // No attachments
    final List <DeliveryEmbeddedAttachmentType> aAttachments = null;

    final DeliverySettingsType aSettings = new DeliverySettingsType ();
    // Perform only technical validation
    aSettings.setTest (Boolean.TRUE);

    // Deliver it
    final DeliveryResponseType aResult = aSender.deliverInvoice (aXMLDocument, aAttachments, aSettings);
    assertNotNull (aResult.toString (), aResult.getSuccess ());
  }
}
