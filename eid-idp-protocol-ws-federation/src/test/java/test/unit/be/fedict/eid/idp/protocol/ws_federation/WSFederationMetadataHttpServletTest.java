/*
 * eID Identity Provider Project.
 * Copyright (C) 2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package test.unit.be.fedict.eid.idp.protocol.ws_federation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

import be.fedict.eid.idp.protocol.ws_federation.WSFederationMetadataHttpServlet;
import be.fedict.eid.idp.spi.IdentityProviderConfiguration;
import be.fedict.eid.idp.spi.IdentityProviderConfigurationFactory;

public class WSFederationMetadataHttpServletTest {

	private static final Log LOG = LogFactory
			.getLog(WSFederationMetadataHttpServletTest.class);

	private ServletTester servletTester;

	private String location;

	@BeforeClass
	public static void init() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Before
	public void setUp() throws Exception {
		KeyPair keyPair = generateKeyPair();
		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusMonths(1);
		X509Certificate certificate = generateSelfSignedCertificate(keyPair,
				"CN=Test", notBefore, notAfter);
		this.servletTester = new ServletTester();
		this.servletTester.setContextPath("/eid-idp");
		ServletHolder servletHolder = this.servletTester.addServlet(
				WSFederationMetadataHttpServlet.class,
				"/ws-federation-metadata");

		IdentityProviderConfiguration mockConfiguration = EasyMock
				.createMock(IdentityProviderConfiguration.class);
		EasyMock.expect(mockConfiguration.getIdentity()).andStubReturn(
				certificate);
		EasyMock.replay(mockConfiguration);
		this.servletTester.start();
		servletHolder
				.getServletHandler()
				.getServletContext()
				.setAttribute(
						IdentityProviderConfigurationFactory.IDENTITY_PROVIDER_CONFIGURATION_CONTEXT_ATTRIBUTE,
						mockConfiguration);

		this.location = this.servletTester.createSocketConnector(true)
				+ "/eid-idp/ws-federation-metadata";
	}

	@Test
	public void get() throws Exception {
		// setup
		LOG.debug("URL: " + this.location);
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(this.location);

		// operate
		int result = httpClient.executeMethod(getMethod);

		// verify
		assertEquals(HttpServletResponse.SC_OK, result);
		String responseBody = getMethod.getResponseBodyAsString();
		LOG.debug("Response body: " + responseBody);
		Header contentTypeHeader = getMethod.getResponseHeader("Content-Type");
		assertNotNull(contentTypeHeader);
		assertEquals("application/samlmetadata+xml", contentTypeHeader
				.getValue());
	}

	private KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = new SecureRandom();
		keyPairGenerator.initialize(new RSAKeyGenParameterSpec(1024,
				RSAKeyGenParameterSpec.F4), random);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return keyPair;
	}

	private SubjectKeyIdentifier createSubjectKeyId(PublicKey publicKey)
			throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(publicKey
				.getEncoded());
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(
				(ASN1Sequence) new ASN1InputStream(bais).readObject());
		return new SubjectKeyIdentifier(info);
	}

	private AuthorityKeyIdentifier createAuthorityKeyId(PublicKey publicKey)
			throws IOException {

		ByteArrayInputStream bais = new ByteArrayInputStream(publicKey
				.getEncoded());
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(
				(ASN1Sequence) new ASN1InputStream(bais).readObject());

		return new AuthorityKeyIdentifier(info);
	}

	private X509Certificate generateSelfSignedCertificate(KeyPair keyPair,
			String subjectDn, DateTime notBefore, DateTime notAfter)
			throws Exception {
		PublicKey subjectPublicKey = keyPair.getPublic();
		PrivateKey issuerPrivateKey = keyPair.getPrivate();
		String signatureAlgorithm = "SHA1WithRSAEncryption";
		X509V3CertificateGenerator certificateGenerator = new X509V3CertificateGenerator();
		certificateGenerator.reset();
		certificateGenerator.setPublicKey(subjectPublicKey);
		certificateGenerator.setSignatureAlgorithm(signatureAlgorithm);
		certificateGenerator.setNotBefore(notBefore.toDate());
		certificateGenerator.setNotAfter(notAfter.toDate());
		X509Principal issuerDN = new X509Principal(subjectDn);
		certificateGenerator.setIssuerDN(issuerDN);
		certificateGenerator.setSubjectDN(new X509Principal(subjectDn));
		certificateGenerator.setSerialNumber(new BigInteger(128,
				new SecureRandom()));

		certificateGenerator.addExtension(X509Extensions.SubjectKeyIdentifier,
				false, createSubjectKeyId(subjectPublicKey));
		PublicKey issuerPublicKey;
		issuerPublicKey = subjectPublicKey;
		certificateGenerator.addExtension(
				X509Extensions.AuthorityKeyIdentifier, false,
				createAuthorityKeyId(issuerPublicKey));

		certificateGenerator.addExtension(X509Extensions.BasicConstraints,
				false, new BasicConstraints(true));

		X509Certificate certificate;
		certificate = certificateGenerator.generate(issuerPrivateKey);

		/*
		 * Next certificate factory trick is needed to make sure that the
		 * certificate delivered to the caller is provided by the default
		 * security provider instead of BouncyCastle. If we don't do this trick
		 * we might run into trouble when trying to use the CertPath validator.
		 */
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(certificate
						.getEncoded()));
		return certificate;
	}
}