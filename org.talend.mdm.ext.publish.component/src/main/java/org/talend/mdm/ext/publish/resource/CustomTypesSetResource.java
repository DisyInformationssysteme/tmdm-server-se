/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement along with this program; if not, write to Talend SA 9 rue Pages
 * 92150 Suresnes, France
 */
package org.talend.mdm.ext.publish.resource;

import java.io.IOException;
import java.util.Objects;

import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.talend.mdm.ext.publish.util.DAOFactory;
import org.talend.mdm.ext.publish.util.DomainObjectsDAO;
import org.xml.sax.SAXException;

import com.amalto.core.util.Util;
import com.amalto.core.util.XtentisException;

public class CustomTypesSetResource extends BaseResource {

    private static Logger log = LogManager.getLogger(CustomTypesSetResource.class);

    private String customTypesSetName;

    private DomainObjectsDAO domainObjectsDAO = null;

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        this.customTypesSetName = getAttributeInUrl("customTypesSetName"); //$NON-NLS-1$
        this.domainObjectsDAO = DAOFactory.getUniqueInstance().getDomainObjectDAO();
        if (log.isDebugEnabled()) {
            log.debug("request params customTypesSetName=" + customTypesSetName);
        }
    }

    @Get
    public Representation getResourceRepresent(Variant variant) throws ResourceException {
        // Generate the right representation according to its media type.
        if (MediaType.TEXT_XML.equals(variant.getMediaType())) {
            DomRepresentation representation = null;
            try {
                String content = domainObjectsDAO.getResource(customTypesSetName);
                if (Objects.isNull(content)) {
                    throw new ResourceException(Status.NO_CONTENT.getStatusCode(),
                            "Failed to fetch resource by customTypesSetName=" + customTypesSetName);
                }
                representation = new DomRepresentation(MediaType.TEXT_XML, Util.parse(content));
                representation.getDocument().normalize();
            } catch (ParserConfigurationException e) {
                log.error(e.getLocalizedMessage(), e);
            } catch (IOException e) {
                log.error(e.getLocalizedMessage(), e);
            } catch (SAXException e) {
                log.error(e.getLocalizedMessage(), e);
            } catch (XtentisException e) {
                log.error(e.getLocalizedMessage(), e);
            }

            return representation;
        }
        return null;
    }
}