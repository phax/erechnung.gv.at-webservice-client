/**
 * Copyright (C) 2014 Philip Helger
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

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.erechnung.erb.handler.SOAPAddWSSEHeaderHandler;
import com.phloc.commons.charset.CCharset;
import com.phloc.commons.charset.CharsetManager;

/**
 * Example for invoking the Webservice 1.2 for ER>B - E-Rechnung an den Bund.
 * 
 * @author Philip Helger
 */
public final class WS120UploadDemo
{
  // Settings start here

  // Assume to use UTF-8
  private static final Charset ENCODING = CCharset.CHARSET_UTF_8_OBJ;
  private static final String USP_WS_USERNAME = "xxx";
  private static final String USP_WS_PASSWORD = "yyy";
  private static final boolean DEBUG_OUTPUT = true;

  // Logger to use
  private static final Logger s_aLogger = LoggerFactory.getLogger (WS120UploadDemo.class);

  public static void main (final String [] args) throws Exception
  {
    // Code starts here
    s_aLogger.info ("Starting the engines");

    // Some debug output
    System.setProperty ("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", Boolean.toString (DEBUG_OUTPUT));
    System.setProperty ("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump",
                        Boolean.toString (DEBUG_OUTPUT));

    final String sDemoEbInterface = "<?xml version=\"1.0\" encoding=\"" +
                                    ENCODING.name () +
                                    "\"?>\n" +
                                    "<eb:Invoice xmlns:eb=\"http://www.ebinterface.at/schema/4p0/\" xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
                                    "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                    "  xsi:schemaLocation=\"http://www.ebinterface.at/schema/4p0/ http://www.ebinterface.at/schema/4p0/Invoice.xsd\"\n" +
                                    "  eb:GeneratingSystem=\"none\" eb:DocumentType=\"Invoice\" eb:InvoiceCurrency=\"EUR\">\n" +
                                    "  <eb:InvoiceNumber>993433000298</eb:InvoiceNumber>\n" +
                                    "  <eb:InvoiceDate>2005-01-03</eb:InvoiceDate>\n" +
                                    "  <eb:Delivery>\n" +
                                    "    <eb:Date>2013-02-28</eb:Date>\n" +
                                    "  </eb:Delivery>\n" +
                                    "  <eb:Biller>\n" +
                                    "    <eb:VATIdentificationNumber>ATU51507409</eb:VATIdentificationNumber>\n" +
                                    "    <eb:InvoiceRecipientsBillerID>0011025781</eb:InvoiceRecipientsBillerID>\n" +
                                    "    <eb:Address>\n" +
                                    "      <eb:Name>Schrauben Willi</eb:Name>\n" +
                                    "      <eb:Street>Lassallestraße 5</eb:Street>\n" +
                                    "      <eb:Town>Wien</eb:Town>\n" +
                                    "      <eb:ZIP>1020</eb:ZIP>\n" +
                                    "      <eb:Country>Österreich</eb:Country>\n" +
                                    "      <eb:Email>demo@x-erechnung-webservice120.at</eb:Email>\n" +
                                    "    </eb:Address>\n" +
                                    "  </eb:Biller>\n" +
                                    "  <eb:InvoiceRecipient>\n" +
                                    "    <eb:VATIdentificationNumber>ATU12345678</eb:VATIdentificationNumber>\n" +
                                    "    <eb:BillersInvoiceRecipientID>BIR4567</eb:BillersInvoiceRecipientID>\n" +
                                    "    <eb:OrderReference>\n" +
                                    "      <eb:OrderID>501:4599999999</eb:OrderID>\n" +
                                    "      <eb:ReferenceDate>2009-09-01</eb:ReferenceDate>\n" +
                                    "    </eb:OrderReference>\n" +
                                    "    <eb:Address>\n" +
                                    "      <eb:Name>Mustermann GmbH</eb:Name>\n" +
                                    "      <eb:Street>Hauptstraße 10</eb:Street>\n" +
                                    "      <eb:Town>Graz</eb:Town>\n" +
                                    "      <eb:ZIP>8010</eb:ZIP>\n" +
                                    "      <eb:Country>Austria</eb:Country>\n" +
                                    "    </eb:Address>\n" +
                                    "  </eb:InvoiceRecipient>\n" +
                                    "  <eb:Details>\n" +
                                    "    <eb:ItemList>\n" +
                                    "      <eb:ListLineItem>\n" +
                                    "        <eb:Description>Schraubenzieher</eb:Description>\n" +
                                    "        <eb:Quantity eb:Unit=\"IntegerType\">100.</eb:Quantity>\n" +
                                    "        <eb:UnitPrice>10.20</eb:UnitPrice>\n" +
                                    "        <eb:TaxRate>20.</eb:TaxRate>\n" +
                                    "        <eb:InvoiceRecipientsOrderReference>\n" +
                                    "          <eb:OrderID>any</eb:OrderID>\n" +
                                    "          <eb:OrderPositionNumber>1</eb:OrderPositionNumber>\n" +
                                    "        </eb:InvoiceRecipientsOrderReference>\n" +
                                    "        <eb:LineItemAmount>1020.00</eb:LineItemAmount>        \n" +
                                    "      </eb:ListLineItem>\n" +
                                    "    </eb:ItemList>\n" +
                                    "  </eb:Details>\n" +
                                    "  <eb:Tax>\n" +
                                    "    <eb:VAT>\n" +
                                    "      <eb:Item>\n" +
                                    "        <eb:TaxedAmount>1020.00</eb:TaxedAmount>\n" +
                                    "        <eb:TaxRate>20.00</eb:TaxRate>\n" +
                                    "        <eb:Amount>204.00</eb:Amount>\n" +
                                    "      </eb:Item>\n" +
                                    "    </eb:VAT>\n" +
                                    "  </eb:Tax>\n" +
                                    "  <eb:TotalGrossAmount>1224</eb:TotalGrossAmount>\n" +
                                    "  <eb:PaymentMethod xsi:type=\"eb:DirectDebitType\" />\n" +
                                    "</eb:Invoice>\n";

    // Prepare document
    final DocumentType aDocument = new DocumentType ();
    aDocument.setValue (CharsetManager.getAsBytes (sDemoEbInterface, ENCODING));
    aDocument.setEncoding (ENCODING.name ());
    // No attachments
    final List <AttachmentType> aAttachments = null;
    // Settings
    final SettingsType aSettings = new SettingsType ();
    // Set test flag
    aSettings.setTest (Boolean.TRUE);

    // Invoke WS
    final WSDocumentUploadService aService = new WSDocumentUploadService ();
    final Wsupload aPort = aService.getWSDocumentUploadPort ();
    final BindingProvider aBP = (BindingProvider) aPort;

    // Determine where to send the WS request to
    aBP.getRequestContext ().put (BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://txm.portal.at/at.gv.bmf.erb/V1");

    // Ensure the WSSE headers are added
    @SuppressWarnings ("rawtypes")
    final List <Handler> aHandlers = new ArrayList <Handler> ();
    aHandlers.add (new SOAPAddWSSEHeaderHandler (USP_WS_USERNAME, USP_WS_PASSWORD));
    aBP.getBinding ().setHandlerChain (aHandlers);

    final TypeUploadStatus aResult = aPort.uploadDocument (aDocument, aAttachments, aSettings);
    s_aLogger.info ("Return code: " + aResult);
    s_aLogger.info ("Done");
  }
}
