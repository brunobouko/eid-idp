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

package be.fedict.eid.idp.sp.protocol.openid;

import be.fedict.eid.idp.common.OpenIDAXConstants;
import be.fedict.eid.idp.sp.protocol.openid.spi.AuthenticationRequestService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.html.HtmlResolver;
import org.openid4java.discovery.xri.XriResolver;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.server.RealmVerifierFactory;
import org.openid4java.util.HttpFetcherFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Generates and sends out a OpenID Authentication Request.
 * <p/>
 * <p/>
 * Configuration can be provided either by providing:
 * <ul>
 * <li><tt>AuthenticationRequestService</tt>: {@link AuthenticationRequestService}
 * to provide the IdP protocol entry point, SP response handling location,
 * optional SSL certificate to trust</li>
 * </ul>
 * or by provinding:
 * <ul>
 * <li><tt>SPDestination</tt> or <tt>SPDestinationPage</tt>: Service Provider
 * destination that will handle the returned SAML2 response. One of the 2
 * parameters needs to be specified.</li>
 * <li><tt>IdPDestination</tt>: SAML2 entry point of the eID IdP.</li>
 * <li><tt>TrustServer</tt>: optional boolean whether any SSL certificate is
 * regarded trusted.</li>
 * </ul>
 */
public class AuthenticationRequestServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private static final Log LOG = LogFactory
                .getLog(AuthenticationRequestServlet.class);

        private static final String AUTHN_REQUEST_SERVICE_PARAM =
                "AuthenticationRequestService";
        private static final String USER_IDENTIFIER_PARAM =
                "UserIdentifier";
        private static final String SP_DESTINATION_PARAM =
                "SPDestination";
        private static final String SP_DESTINATION_PAGE_PARAM =
                SP_DESTINATION_PARAM + "Page";
        private static final String TRUST_SERVER_PARAM = "TrustServer";

        public static final String CONSUMER_MANAGER_ATTRIBUTE =
                AuthenticationRequestServlet.class.getName() + ".ConsumerManager";

        private String userIdentifier;
        private String spDestination;
        private String spDestinationPage;

        private ServiceLocator<AuthenticationRequestService> authenticationRequestServiceLocator;


        private ConsumerManager consumerManager;

        private boolean trustServer;

        @Override
        public void init(ServletConfig config) throws ServletException {

                this.userIdentifier = config.getInitParameter(USER_IDENTIFIER_PARAM);
                this.spDestination = config.getInitParameter(SP_DESTINATION_PARAM);
                this.spDestinationPage = config.getInitParameter(SP_DESTINATION_PAGE_PARAM);
                this.authenticationRequestServiceLocator = new
                        ServiceLocator<AuthenticationRequestService>
                        (AUTHN_REQUEST_SERVICE_PARAM, config);

                // validate necessary configuration params
                if (null == this.userIdentifier
                        && !this.authenticationRequestServiceLocator.isConfigured()) {
                        throw new ServletException(
                                "need to provide either " + USER_IDENTIFIER_PARAM
                                        + " or " + AUTHN_REQUEST_SERVICE_PARAM +
                                        "(Class) init-params");
                }

                if (null == this.spDestination && null == this.spDestinationPage
                        && !this.authenticationRequestServiceLocator.isConfigured()) {
                        throw new ServletException(
                                "need to provide either " + SP_DESTINATION_PARAM
                                        + " or " + SP_DESTINATION_PAGE_PARAM +
                                        " or " + AUTHN_REQUEST_SERVICE_PARAM +
                                        "(Class) init-param");
                }

                // SSL configuration
                String trustServer = config.getInitParameter(TRUST_SERVER_PARAM);
                if (null != trustServer) {
                        this.trustServer = Boolean.parseBoolean(trustServer);
                }
                X509Certificate serverCertificate = null;
                if (this.authenticationRequestServiceLocator.isConfigured()) {
                        AuthenticationRequestService service =
                                this.authenticationRequestServiceLocator.locateService();
                        serverCertificate = service.getServerCertificate();
                }

                if (this.trustServer) {

                        LOG.warn("Trusting all SSL server certificates!");
                        try {
                                OpenIDSSLSocketFactory.installAllTrusted();
                        } catch (Exception e) {
                                throw new ServletException(
                                        "could not install OpenID SSL Socket Factory: "
                                                + e.getMessage(), e);
                        }
                } else if (null != serverCertificate) {

                        LOG.info("Trusting specified SSL certificate: " + serverCertificate);
                        try {
                                OpenIDSSLSocketFactory.install(serverCertificate);
                        } catch (Exception e) {
                                throw new ServletException(
                                        "could not install OpenID SSL Socket Factory: "
                                                + e.getMessage(), e);
                        }
                }

                ServletContext servletContext = config.getServletContext();
                this.consumerManager = (ConsumerManager) servletContext
                        .getAttribute(CONSUMER_MANAGER_ATTRIBUTE);

                if (null == this.consumerManager) {
                        try {
                                if (this.trustServer || null != serverCertificate) {

                                        TrustManager trustManager;
                                        if (this.trustServer) {
                                                trustManager = new OpenIDTrustManager();
                                        } else {
                                                trustManager = new OpenIDTrustManager(serverCertificate);
                                        }

                                        SSLContext sslContext = SSLContext.getInstance("SSL");
                                        TrustManager[] trustManagers = {trustManager};
                                        sslContext.init(null, trustManagers, null);
                                        HttpFetcherFactory httpFetcherFactory = new HttpFetcherFactory(
                                                sslContext,
                                                SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                                        YadisResolver yadisResolver = new YadisResolver(
                                                httpFetcherFactory);
                                        RealmVerifierFactory realmFactory = new RealmVerifierFactory(
                                                yadisResolver);
                                        HtmlResolver htmlResolver = new HtmlResolver(
                                                httpFetcherFactory);
                                        XriResolver xriResolver = Discovery.getXriResolver();
                                        Discovery discovery = new Discovery(htmlResolver,
                                                yadisResolver, xriResolver);
                                        this.consumerManager = new ConsumerManager(realmFactory,
                                                discovery, httpFetcherFactory);

                                } else {
                                        this.consumerManager = new ConsumerManager();
                                }
                        } catch (Exception e) {
                                throw new ServletException(
                                        "could not init OpenID ConsumerManager");
                        }
                        servletContext.setAttribute(CONSUMER_MANAGER_ATTRIBUTE,
                                this.consumerManager);
                }
        }

        public static ConsumerManager getConsumerManager(HttpServletRequest request) {
                HttpSession httpSession = request.getSession();
                ServletContext servletContext = httpSession.getServletContext();
                ConsumerManager consumerManager = (ConsumerManager) servletContext
                        .getAttribute(CONSUMER_MANAGER_ATTRIBUTE);
                if (null == consumerManager) {
                        throw new IllegalStateException(
                                "no ConsumerManager found in ServletContext");
                }
                return consumerManager;
        }

        @Override
        protected void doGet(HttpServletRequest request,
                             HttpServletResponse response) throws ServletException, IOException {

                String spDestination;
                String userIdentifier;

                AuthenticationRequestService service =
                        this.authenticationRequestServiceLocator.locateService();
                if (null != service) {
                        userIdentifier = service.getUserIdentifier();
                        spDestination = service.getSPDestination();
                } else {
                        userIdentifier = this.userIdentifier;
                        if (null != this.spDestination) {
                                spDestination = this.spDestination;
                        } else {
                                spDestination = request.getScheme() + "://"
                                        + request.getServerName() + ":"
                                        + request.getServerPort() + request.getContextPath()
                                        + this.spDestinationPage;
                        }
                }


                try {
                        LOG.debug("discovering the identity...");
                        LOG.debug("user identifier: " + userIdentifier);
                        List discoveries = this.consumerManager.discover(userIdentifier);
                        LOG.debug("associating with the IdP...");
                        DiscoveryInformation discovered = this.consumerManager
                                .associate(discoveries);
                        request.getSession().setAttribute("openid-disc", discovered);

                        LOG.debug("SP destination: " + spDestination);
                        AuthRequest authRequest = this.consumerManager.authenticate(
                                discovered, spDestination);

                        /*
                        * We also piggy-back an attribute fetch request.
                        */
                        FetchRequest fetchRequest = FetchRequest.createFetchRequest();

                        // required attributes
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_FIRST_NAME_PERSON_TYPE, true);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_LAST_NAME_PERSON_TYPE, true);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_NAME_PERSON_TYPE, true);

                        // optional attributes
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_GENDER_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_POSTAL_CODE_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_POSTAL_ADDRESS_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_CITY_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_NATIONALITY_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_PLACE_OF_BIRTH_TYPE, false);
                        fetchRequest.addAttribute(OpenIDAXConstants.AX_BIRTHDATE_TYPE, false);

                        authRequest.addExtension(fetchRequest);

                        LOG.debug("redirecting to producer with authn request...");
                        response.sendRedirect(authRequest.getDestinationUrl(true));
                } catch (OpenIDException e) {
                        throw new ServletException("OpenID error: " + e.getMessage(), e);
                }
        }
}