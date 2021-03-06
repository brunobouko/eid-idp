/*
 * eID Identity Provider Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.eid.idp.model;

public enum KeyStoreType {

	PKCS12("PKCS#12", "PKCS12"), PKCS11("PKCS#11", "PKCS11"), JKS("JKS", "JKS");

	private final String description;

	private final String javaKeyStoreType;

	/**
	 * @param description
	 *            human readable description of the key store type.
	 * @param javaKeyStoreType
	 *            java key store type required for KeyStore.getInstance.
	 */
	private KeyStoreType(String description, String javaKeyStoreType) {
		this.description = description;
		this.javaKeyStoreType = javaKeyStoreType;
	}

	@Override
	public String toString() {
		return this.description;
	}

	public String getJavaKeyStoreType() {
		return this.javaKeyStoreType;
	}
}
