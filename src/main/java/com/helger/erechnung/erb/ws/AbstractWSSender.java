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
package com.helger.erechnung.erb.ws;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.charset.CCharset;
import com.helger.commons.lang.GenericReflection;
import com.helger.commons.string.ToStringGenerator;

/**
 * Abstract base class for for the ER>B - E-Rechnung an den Bund - Webservice
 * wrapper.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The real implementation type
 */
@NotThreadSafe
public abstract class AbstractWSSender <IMPLTYPE extends AbstractWSSender <IMPLTYPE>>
{
  // Default encoding according to XSD
  public static final Charset DEFAULT_INVOICE_ENCODING = CCharset.CHARSET_UTF_8_OBJ;
  public static final boolean DEFAULT_DEBUG = false;
  public static final boolean DEFAULT_TEST_VERSION = false;
  public static final boolean DEFAULT_TRUST_ALL_CERTIFICATES = false;
  public static final boolean DEFAULT_TRUST_ALL_HOSTNAMES = false;

  private final String m_sWebserviceUsername;
  private final String m_sWebservicePassword;
  private Charset m_aInvoiceEncoding = DEFAULT_INVOICE_ENCODING;
  private boolean m_bDebugMode = DEFAULT_DEBUG;
  private boolean m_bTestVersion = DEFAULT_TEST_VERSION;
  private boolean m_bTrustAllCertificates = DEFAULT_TRUST_ALL_CERTIFICATES;
  private boolean m_bTrustAllHostnames = DEFAULT_TRUST_ALL_HOSTNAMES;

  public AbstractWSSender (@Nonnull @Nonempty final String sWebserviceUsername,
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

  @Nonnull
  private IMPLTYPE _thisAsT ()
  {
    return GenericReflection.<Object, IMPLTYPE> uncheckedCast (this);
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
  public IMPLTYPE setInvoiceEncoding (@Nonnull final Charset aInvoiceEncoding)
  {
    ValueEnforcer.notNull (aInvoiceEncoding, "InvoiceEncoding");
    m_aInvoiceEncoding = aInvoiceEncoding;
    return _thisAsT ();
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
  public IMPLTYPE setDebugMode (final boolean bDebugMode)
  {
    m_bDebugMode = bDebugMode;
    return _thisAsT ();
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
  public IMPLTYPE setTestVersion (final boolean bTestVersion)
  {
    m_bTestVersion = bTestVersion;
    return _thisAsT ();
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
  public IMPLTYPE setTrustAllCertificates (final boolean bTrustAllCertificates)
  {
    m_bTrustAllCertificates = bTrustAllCertificates;
    return _thisAsT ();
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
  public IMPLTYPE setTrustAllHostnames (final boolean bTrustAllHostnames)
  {
    m_bTrustAllHostnames = bTrustAllHostnames;
    return _thisAsT ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("webserviceUsername", m_sWebserviceUsername)
                                       .appendPassword ("webservicePassword")
                                       .append ("invoiceEncoding", m_aInvoiceEncoding)
                                       .append ("debugMode", m_bDebugMode)
                                       .append ("testVersion", m_bTestVersion)
                                       .append ("trustAllCertificates", m_bTrustAllCertificates)
                                       .append ("trustAllHostnames", m_bTrustAllHostnames)
                                       .toString ();
  }
}
