// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package com.amalto.core.query;

import com.amalto.core.query.user.OrderBy;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.storage.FullTextResultsWriter;
import com.amalto.core.storage.Storage;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.hibernate.HibernateStorage;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.DataRecordWriter;
import com.amalto.core.storage.record.XmlStringDataRecordReader;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import static com.amalto.core.query.user.UserQueryBuilder.*;

public class StorageFullTextTest extends StorageTestCase {

    private void populateData() {
        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        List<DataRecord> allRecords = new LinkedList<DataRecord>();
        allRecords.add(factory.read("MDM", 1, product, "<Product>\n" +
                "    <Id>1</Id>\n" +
                "    <Name>Product name</Name>\n" +
                "    <ShortDescription>Short description word</ShortDescription>\n" +
                "    <LongDescription>Long description</LongDescription>\n" +
                "    <Price>10</Price>\n" +
                "    <Features>\n" +
                "        <Sizes>\n" +
                "            <Size>Small</Size>\n" +
                "            <Size>Medium</Size>\n" +
                "            <Size>Large</Size>\n" +
                "        </Sizes>\n" +
                "        <Colors>\n" +
                "            <Color>Blue</Color>\n" +
                "            <Color>Red</Color>\n" +
                "        </Colors>\n" +
                "    </Features>\n" +
                "    <Status>Pending</Status>\n" +
                "    <Supplier>[1]</Supplier>\n" +
                "</Product>"));
        allRecords.add(factory.read("MDM", 1, product, "<Product>\n" +
                "    <Id>2</Id>\n" +
                "    <Name>Renault car</Name>\n" +
                "    <ShortDescription>A car</ShortDescription>\n" +
                "    <LongDescription>Long description 2</LongDescription>\n" +
                "    <Price>10</Price>\n" +
                "    <Features>\n" +
                "        <Sizes>\n" +
                "            <Size>Large</Size>\n" +
                "        </Sizes>\n" +
                "        <Colors>\n" +
                "            <Color>Blue 2</Color>\n" +
                "            <Color>Blue 1</Color>\n" +
                "            <Color>Klein blue2</Color>\n" +
                "        </Colors>\n" +
                "    </Features>\n" +
                "    <Status>Pending</Status>\n" +
                "    <Supplier>[2]</Supplier>\n" +
                "    <Supplier>[1]</Supplier>\n" +
                "</Product>"));
        allRecords.add(factory.read("MDM", 1, supplier, "<Supplier>\n" +
                "    <Id>1</Id>\n" +
                "    <SupplierName>Renault</SupplierName>\n" +
                "    <Contact>" +
                "        <Name>Jean Voiture</Name>\n" +
                "        <Phone>33123456789</Phone>\n" +
                "        <Email>test@test.org</Email>\n" +
                "    </Contact>\n" +
                "</Supplier>"));
        allRecords.add(factory.read("MDM", 1, supplier, "<Supplier>\n" +
                "    <Id>2</Id>\n" +
                "    <SupplierName>Starbucks Talend</SupplierName>\n" +
                "    <Contact>" +
                "        <Name>Jean Cafe</Name>\n" +
                "        <Phone>33234567890</Phone>\n" +
                "        <Email>test@testfactory.org</Email>\n" +
                "    </Contact>\n" +
                "</Supplier>"));
        allRecords.add(factory.read("MDM", 1, supplier, "<Supplier>\n" +
                "    <Id>3</Id>\n" +
                "    <SupplierName>Talend</SupplierName>\n" +
                "    <Contact>" +
                "        <Name>Jean Paul</Name>\n" +
                "        <Phone>33234567890</Phone>\n" +
                "        <Email>test@talend.com</Email>\n" +
                "    </Contact>\n" +
                "</Supplier>"));
        try {
            storage.begin();
            storage.update(allRecords);
            storage.commit();
        } catch (Exception e) {
            storage.rollback();
            try {
                tearDown();
            } catch (Exception e1) {
                // Ignored
            }
            throw new RuntimeException(e);
        } finally {
            storage.end();
        }
    }

    @Override
    public void tearDown() throws Exception {
        storage.begin();
        {
            UserQueryBuilder qb = from(product);
            storage.delete(qb.getSelect());

            qb = from(supplier);
            storage.delete(qb.getSelect());
        }
        storage.commit();
        storage.end();
    }

    @Override
    public void setUp() throws Exception {
        populateData();
    }

    public void testSimpleSearch() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            for (DataRecord result : results) {
                System.out.println("result = " + result);
            }
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchOrderBy() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .where(fullText("Talend"))
                .orderBy(supplier.getField("Id"), OrderBy.Direction.ASC);

        try {
            storage.fetch(qb.getSelect());
            fail("Expected exception since there's no support for order by clause in full text searches.");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testMultipleTypesSearch() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .and(product)
                .where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleSearchWithCondition() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .where(fullText("Renault"))
                .where(eq(supplier.getField("Contact/Name"), "Jean Voiture"));

        // TODO Cannot filter full text search results.
        try {
            storage.fetch(qb.getSelect());
            fail("Expected an exception: not implemented when test was written.");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testSimpleSearchWithProjection() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .select(supplier.getField("Contact/Name"))
                .where(fullText("Renault"));

        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals(1, result.getSetFields().size());
                assertNotNull(result.get("Name"));
            }
        } finally {
            results.close();
        }
    }

    public void testMultipleTypesSearchWithCondition() throws Exception {
        UserQueryBuilder qb = from(supplier)
                .and(product)
                .where(fullText("Renault"))
                .where(eq(supplier.getField("Contact/Name"), "Jean Voiture"));

        try {
            storage.fetch(qb.getSelect());
            fail("Expected an exception: can not add a where condition when searching 1+ types.");
        } catch (Exception e) {
            // Expected
        }
    }

    public void testFullTextSuggestion() throws Exception {
        try {
            storage.getFullTextSuggestion("Ren", Storage.FullTextSuggestion.START, 3);
            fail("Expected due to Lucene version being used.");
        } catch (Exception e) {
            // Expected.
        }
    }

    public void testFullTextAlternative() throws Exception {
        try {
            storage.getFullTextSuggestion("strabuks", Storage.FullTextSuggestion.ALTERNATE, 3);
            fail("Expected due to Lucene version being used");
        } catch (Exception e) {
            // Expected
        }

    }

    public void testCollectionCondition() throws Exception {
        UserQueryBuilder qb = from(product)
                .select(product.getField("Id"))
                .where(eq(product.getField("Features/Sizes/Size"), "XL"));

        try {
            storage.fetch(qb.getSelect());
            fail("Cannot search on collections until use of Hibernate 4");
        } catch (Exception e) {
            // Excepted
        }
    }

    public void testFullTextResultsFormat() throws Exception {
        UserQueryBuilder qb = from(product)
                .where(fullText("Renault"));

        StorageResults results = null;
        try {
            results = storage.fetch(qb.getSelect());
            assertEquals(1, results.getCount());

            DataRecordWriter writer = new FullTextResultsWriter("Renault");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (DataRecord result : results) {
                writer.write(result, output);
            }
            assertTrue(output.toString().contains("<b>Renault car</b>"));
        } finally {
            if (results != null) {
                results.close();
            }
        }
    }

    public void testNoFullText() throws Exception {
        Storage storage = new HibernateStorage("noFullText");
        try {
            storage.init("RDBMS-1-NO-FT");
            storage.prepare(repository, false, false);
            UserQueryBuilder qb = from(product).where(fullText("Test"));

            try {
                storage.fetch(qb.getSelect());
                fail("Full text is not enabled");
            } catch (Exception e) {
                assertEquals("Storage 'noFullText' is not configured to support full text queries.", e.getCause().getMessage());
            }
        } finally {
            storage.close();
        }
    }
}
