// ============================================================================
//
// Copyright (C) 2006-2019 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package com.amalto.core.servlet;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

public class MultipartFileUploadTest {

    @Test
    public void testUploadFile() throws Exception {
        //load file to MultipartFile
        File file = getFile("com/amalto/core/servlet/myUploadFile.log");
        FileItem fileItem = new DiskFileItem("formFieldName", Files.probeContentType(file.toPath()), false, file.getName(),
                (int) file.length(), file.getParentFile());
        IOUtils.copy(new FileInputStream(file), fileItem.getOutputStream());

        MultipartFile cMultiFile = new CommonsMultipartFile(fileItem);
        assertEquals("myUploadFile.log", cMultiFile.getOriginalFilename());

        // convert MultipartFile to File object
        CommonsMultipartFile cFile = (CommonsMultipartFile) cMultiFile;
        // cFile.getStoreLocation() has changed in new version, so using other API as test purpose
        DiskFileItem fileItem2 = (DiskFileItem) cFile.getFileItem();
        long fileSize = fileItem2.getSize();
        assertNotNull(fileSize);
    }

    private File getFile(String filename) {
        URL url = getClass().getClassLoader().getResource(filename);
        assertNotNull(url);
        return new File(url.getFile());
    }
}