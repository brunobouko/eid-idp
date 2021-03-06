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

package be.fedict.eid.idp.sp.protocol.saml2.artifact;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HTTP Proxy Selector used by the HTTP-Artifact WS Client
 * {@link ArtifactServiceClient} for running behind a proxy (both HTTP as HTTPS)
 * 
 * @author Wim Vandenhaute
 */
public class ArtifactProxySelector extends ProxySelector {

	private static final Log LOG = LogFactory
			.getLog(ArtifactProxySelector.class);

	private final ProxySelector defaultProxySelector;

	private final Map<String, Proxy> proxies;

	/**
	 * Main constructor
	 * 
	 * @param proxySelector
	 *            proxy selector to use
	 */
	public ArtifactProxySelector(ProxySelector proxySelector) {
		this.defaultProxySelector = proxySelector;
		this.proxies = new HashMap<String, Proxy>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Proxy> select(URI uri) {
		LOG.debug("select: " + uri);
		String hostname = uri.getHost();
		Proxy proxy = this.proxies.get(hostname);
		if (null != proxy) {
			LOG.debug("using proxy: " + proxy);
			return Collections.singletonList(proxy);
		}
		return this.defaultProxySelector.select(uri);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		this.defaultProxySelector.connectFailed(uri, sa, ioe);
	}

	/**
	 * Sets the proxy for a certain location URL. If the proxyHost is null, we
	 * go DIRECT.
	 * 
	 * @param location
	 *            location
	 * @param proxyHost
	 *            proxy hostname
	 * @param proxyPort
	 *            proxy port
	 */
	public void setProxy(String location, String proxyHost, int proxyPort) {
		String hostname;
		try {
			hostname = new URL(location).getHost();
		} catch (MalformedURLException e) {
			throw new RuntimeException("URL error: " + e.getMessage(), e);
		}
		if (null == proxyHost) {
			LOG.debug("removing proxy for: " + hostname);
			this.proxies.remove(hostname);
		} else {
			LOG.debug("setting proxy for: " + hostname);
			this.proxies.put(hostname, new Proxy(Proxy.Type.HTTP,
					new InetSocketAddress(proxyHost, proxyPort)));
		}
	}
}
