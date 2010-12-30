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

package be.fedict.eid.idp.admin.webapp.bean;

import be.fedict.eid.idp.admin.webapp.Identity;
import be.fedict.eid.idp.model.IdentityConfig;
import be.fedict.eid.idp.model.IdentityService;
import be.fedict.eid.idp.model.KeyStoreType;
import be.fedict.eid.idp.model.exception.KeyStoreLoadException;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.model.SelectItem;
import java.util.LinkedList;
import java.util.List;

@Stateful
@Name("idpIdentity")
@LocalBinding(jndiBinding = "fedict/eid/idp/admin/webapp/IdentityBean")
public class IdentityBean implements Identity {

    private static final String ADD_IDENTITY_LABEL = "Add...";

    @Logger
    private Log log;

    @EJB
    private IdentityService identityService;

    @In
    FacesMessages facesMessages;

    @In(value = "idp.identity.name", required = false, scope = ScopeType.SESSION)
    @Out(value = "idp.identity.name", required = false, scope = ScopeType.SESSION)
    private String name;

    private boolean nameReadOnly = true;

    private List<String> identityNames;
    private IdentityConfig identityConfig;


    @Override
    @Create
    public void create() {

        // Identity Config
        if (null != this.name) {
            this.identityConfig = this.identityService.findIdentityConfig(this.name);
            if (null == this.identityConfig) {
                this.identityConfig = new IdentityConfig(this.name);
            }
        } else {
            this.identityConfig = this.identityService.getIdentityConfig();
        }
        this.name = this.identityConfig.getName();
        this.identityNames = this.identityService.getIdentities();

        this.nameReadOnly = !this.name.isEmpty();
    }

    @Override
    @Remove
    @Destroy
    public void destroy() {
    }

    @Override
    public String save() {
        this.log.debug("save");

        try {
            // Identity Config
            this.identityService.setIdentity(this.identityConfig);

            if (this.identityConfig.isActive()) {
                this.identityService.reloadIdentity();
            }
            this.identityNames = this.identityService.getIdentities();
            return "success";
        } catch (KeyStoreLoadException e) {
            this.facesMessages.add(StatusMessage.Severity.ERROR,
                    "Failed to load keystore", e.getMessage());
            return null;
        }
    }

    @Override
    public String activate() {
        this.log.debug("activate: " + this.name);

        try {
            this.identityConfig.setActive(true);
            this.identityService.setActiveIdentity(this.name);
            return "success";
        } catch (KeyStoreLoadException e) {
            this.facesMessages.add(StatusMessage.Severity.ERROR,
                    "Failed to load keystore", e.getMessage());
            return null;
        }
    }

    @Override
    public String remove() {
        this.log.debug("remove: " + this.name);

        // disallow removing currently active
        if (this.identityConfig.isActive()) {
            this.facesMessages.add(StatusMessage.Severity.ERROR,
                    "Identity is currently active, cannot remove.");
            return null;
        }

        // disallow removing only 1 identity configured
        if (this.identityNames.size() == 1) {
            this.facesMessages.add(StatusMessage.Severity.ERROR,
                    "Cannot remove last identity.");
            return null;
        }

        // remove
        this.identityService.removeIdentityConfig(this.name);

        // load default config and list of identities
        this.identityConfig = this.identityService.getIdentityConfig();
        this.name = this.identityConfig.getName();
        this.identityNames = this.identityService.getIdentities();
        return "success";
    }

    @Override
    public List<SelectItem> getIdentityNames() {

        List<SelectItem> selectItems = new LinkedList<SelectItem>();
        selectItems.add(new SelectItem(ADD_IDENTITY_LABEL));
        for (String identityName : identityNames) {
            selectItems.add(new SelectItem(identityName));
        }
        return selectItems;
    }

    @Override
    public String getIdentityLabel() {
        return "Identity" + (this.identityConfig.isActive() ? " (Active)" : "");
    }

    @Override
    public String getName() {

        return this.name;
    }

    @Override
    public void setName(String name) {

        if (name.equals(ADD_IDENTITY_LABEL)) {
            this.identityConfig = new IdentityConfig("");
            this.nameReadOnly = false;
            this.name = this.identityConfig.getName();
        } else {
            IdentityConfig identityConfig = this.identityService.findIdentityConfig(name);
            if (null != identityConfig) {
                this.identityConfig = identityConfig;
                this.name = this.identityConfig.getName();
            } else {
                this.name = name;
                this.identityConfig.setName(name);
            }
        }
    }

    @Override
    public Boolean isNameReadOnly() {
        return this.nameReadOnly;
    }

    @Override
    public String getKeyStoreType() {
        return identityConfig.getKeyStoreType().name();
    }

    @Override
    public void setKeyStoreType(String keyStoreType) {
        this.identityConfig.setKeyStoreType(KeyStoreType.valueOf(keyStoreType));
    }

    @Override
    public String getKeyStorePath() {
        return this.identityConfig.getKeyStorePath();
    }

    @Override
    public void setKeyStorePath(String keyStorePath) {
        this.identityConfig.setKeyStorePath(keyStorePath);
    }

    @Override
    public String getKeyStorePassword() {
        return this.identityConfig.getKeyStorePassword();
    }

    @Override
    public void setKeyStorePassword(String keyStorePassword) {
        this.identityConfig.setKeyStorePassword(keyStorePassword);
    }

    @Override
    public String getKeyEntryPassword() {
        return this.identityConfig.getKeyEntryPassword();
    }

    @Override
    public void setKeyEntryPassword(String keyEntryPassword) {
        this.identityConfig.setKeyEntryPassword(keyEntryPassword);
    }

    @Override
    public String getKeyEntryAlias() {
        return this.identityConfig.getKeyEntryAlias();
    }

    @Override
    public void setKeyEntryAlias(String keyEntryAlias) {
        this.identityConfig.setKeyEntryAlias(keyEntryAlias);
    }
}
