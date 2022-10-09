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

import java.io.File;

import org.restlet.data.MediaType;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.amalto.core.util.Util;

/**
 * Resource which has only one representation.
 *
 */
public class BarFileResource extends BaseResource {

    private String barFileHome;

    private String barFileName;

    public BarFileResource() {
        this.barFileHome = Util.getBarHomeDir();
        this.barFileName = getAttributeInUrl("barFileName").replace("$$", ".") + ".bar"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Get
    public Representation getResourceRepresent(Variant variant) throws ResourceException {

        String filePath = barFileHome + File.separator + barFileName;
        if (new File(filePath).exists()) {
            // Generate the right representation according to its media type.
            FileRepresentation representation = null;
            representation = new FileRepresentation(filePath, MediaType.APPLICATION_OCTET_STREAM);
            return representation;
        } else {
            return null;
        }
    }
}