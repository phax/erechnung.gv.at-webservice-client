/*
 * Copyright (C) 2014-2024 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Node;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.config.ConfigFactory;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.read.DOMReader;

import at.gv.brz.eproc.erb.ws.documentupload._20121205.AttachmentType;
import at.gv.brz.eproc.erb.ws.documentupload._20121205.SettingsType;
import at.gv.brz.schema.eproc.invoice_uploadstatus_1_0.TypeUploadStatus;

/**
 * Unit test class for class {@link WS120Sender}.
 *
 * @author Philip Helger
 */
public final class WS120SenderTest
{
  // See src/test/resources/application.properties
  private static final String USP_WS_USERNAME = ConfigFactory.getDefaultConfig ().getAsString ("ws.username");
  private static final String USP_WS_PASSWORD = ConfigFactory.getDefaultConfig ().getAsString ("ws.password");

  /**
   * Basic test case. It is ignored by default, since no test username and
   * password are present. After setting the properties in the
   * "application.properties" file, this test can be "un-ignored".
   */
  @Test
  @Ignore
  public void testDeliverInvoiceViaDOMNode ()
  {
    final Node aXMLDocument = DOMReader.readXMLDOM (new ClassPathResource ("test-invoices/ebi60.xml"));
    assertNotNull ("Failed to read example invoice", aXMLDocument);

    final WS120Sender aSender = new WS120Sender (USP_WS_USERNAME, USP_WS_PASSWORD);
    aSender.setDebugMode (true);

    // Send to test system?
    if (false)
      aSender.setURL (WS120Sender.ENDPOINT_URL_TEST);

    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("eb", "http://www.ebinterface.at/schema/6p0/");
    aNSCtx.addMapping ("ds", "http://www.w3.org/2000/09/xmldsig#");
    aSender.setNamespaceContext (aNSCtx);

    // No attachments
    final List <AttachmentType> aAttachments = null;

    final SettingsType aSettings = new SettingsType ();
    // Perform only technical validation
    aSettings.setTest (Boolean.TRUE);

    // Deliver it
    final TypeUploadStatus aResult = aSender.deliverInvoice (aXMLDocument, aAttachments, aSettings);
    assertNotNull (aResult.toString (), aResult.getSuccess ());
  }

  /**
   * Basic test case. It is ignored by default, since no test username and
   * password are present. After setting the properties in the
   * "application.properties" file, this test can be "un-ignored".
   */
  @Test
  @Ignore
  public void testDeliverInvoiceViaByteArray ()
  {
    final byte [] aXMLBytes = StreamHelper.getAllBytes (new ClassPathResource ("test-invoices/ebi60.xml"));
    assertNotNull ("Failed to read example invoice", aXMLBytes);

    final WS120Sender aSender = new WS120Sender (USP_WS_USERNAME, USP_WS_PASSWORD);
    aSender.setDebugMode (true);

    // Send to test system?
    if (false)
      aSender.setURL (WS120Sender.ENDPOINT_URL_TEST);

    // No attachments
    final List <AttachmentType> aAttachments = null;

    final SettingsType aSettings = new SettingsType ();
    // Perform only technical validation
    aSettings.setTest (Boolean.TRUE);

    // Deliver it
    final TypeUploadStatus aResult = aSender.deliverInvoice (aXMLBytes, aAttachments, aSettings);
    assertNotNull (aResult.toString (), aResult.getSuccess ());
  }
}
