/*
 * Copyright (C) 2006-2012 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package com.amalto.core.query;

import com.amalto.core.metadata.ComplexTypeMetadata;
import com.amalto.core.metadata.MetadataUtils;
import com.amalto.core.query.user.UserQueryBuilder;
import com.amalto.core.storage.StorageResults;
import com.amalto.core.storage.record.DataRecord;
import com.amalto.core.storage.record.DataRecordReader;
import com.amalto.core.storage.record.XmlStringDataRecordReader;

import java.util.LinkedList;
import java.util.List;

public class InheritanceTest extends StorageTestCase {

    private ComplexTypeMetadata a;

    private ComplexTypeMetadata b;

    private ComplexTypeMetadata c;

    private ComplexTypeMetadata d;

    private void populateData() {
        DataRecordReader<String> factory = new XmlStringDataRecordReader();
        List<DataRecord> allRecords = new LinkedList<DataRecord>();
        allRecords.add(factory.read(1, repository, b, "<B><id>1</id><textB>TextB</textB></B>"));
        allRecords.add(factory.read(1, repository, d, "<D><id>2</id><textB>TextBD</textB><textD>TextDD</textD></D>"));
        allRecords.add(factory.read(1, repository, a, "<A xmlns:tmdm=\"http://www.talend.com/mdm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><id>1</id><refB tmdm:type=\"B\">[1]</refB><textA>TextA</textA><nestedB xsi:type=\"Nested\"><text>Text</text></nestedB></A>"));
        allRecords.add(factory.read(1, repository, c, "<C xmlns:tmdm=\"http://www.talend.com/mdm\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><id>2</id><refB tmdm:type=\"D\">[2]</refB><textA>TextAC</textA><nestedB xsi:type=\"SubNested\"><text>Text</text><subText>SubText</subText></nestedB><textC>TextCC</textC></C>"));

        try {
            storage.begin();
            storage.update(allRecords);
            storage.commit();
        } finally {
            storage.end();
        }
    }

    @Override
    public void setUp() throws Exception {
        a = repository.getComplexType("A");
        b = repository.getComplexType("B");
        c = repository.getComplexType("C");
        d = repository.getComplexType("D");

        populateData();
        userSecurity.setActive(false); // Not testing security here
    }

    public void testTypeOrdering() throws Exception {
        List<ComplexTypeMetadata> sortedList = MetadataUtils.sortTypes(repository);
        String[] expectedOrder = {"EntityWithQuiteALongNameWithoutIncludingAnyUnderscore",
                "ProductFamily",
                "TypeA",
                "Country",
                "Address",
                "Person",
                "Supplier",
                "Product",
                "B", "D", "A", "C"};
        int i = 0;
        for (ComplexTypeMetadata sortedType : sortedList) {
            assertEquals(expectedOrder[i++], sortedType.getName());
        }
    }

    public void testSimpleQuery() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(d);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }

        qb = UserQueryBuilder.from(c);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testSimpleInheritanceQuery() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(b);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }

        qb = UserQueryBuilder.from(a);
        results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testQueryWithInstanceCheck() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(b)
                .isa(d);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
        } finally {
            results.close();
        }
    }

    public void testDefaultFKType() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(a);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
            for (DataRecord result : results) {
                Object value = result.get("refB");
                assertTrue(value instanceof DataRecord);
                if ("A".equals(result.getType().getName())) {
                    assertEquals("B", ((DataRecord) value).getType().getName());
                } else if ("C".equals(result.getType().getName())) {
                    assertEquals("D", ((DataRecord) value).getType().getName());
                }
            }
        } finally {
            results.close();
        }
    }

    public void testFKType() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(c);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                Object value = result.get("refB");
                assertTrue(value instanceof DataRecord);
                assertEquals("D", ((DataRecord) value).getType().getName());
            }
        } finally {
            results.close();
        }
    }

    public void testDefaultNestedType() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(a);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
            for (DataRecord result : results) {
                Object value = result.get("nestedB");
                assertTrue(value instanceof DataRecord);
                if ("A".equals(result.getType().getName())) {
                    assertEquals("Nested", ((DataRecord) value).getType().getName());
                } else if ("C".equals(result.getType().getName())) {
                    assertEquals("SubNested", ((DataRecord) value).getType().getName());
                }
            }
        } finally {
            results.close();
        }
    }

    public void testNestedType() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(c);
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                Object value = result.get("nestedB");
                assertTrue(value instanceof DataRecord);
                assertEquals("SubNested", ((DataRecord) value).getType().getName());
            }
        } finally {
            results.close();
        }
    }

    public void testJoin() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(c)
                .and(b)
                .select(c.getField("textC"))
                .select(b.getField("textB"))
                .join(c.getField("refB"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(1, results.getCount());
            for (DataRecord result : results) {
                assertEquals("TextBD", result.get("textB"));
                assertEquals("TextCC", result.get("textC"));
            }
        } finally {
            results.close();
        }
    }

    public void testJoinWithInheritance() throws Exception {
        UserQueryBuilder qb = UserQueryBuilder.from(a)
                .and(b)
                .select(a.getField("textA"))
                .select(b.getField("textB"))
                .join(a.getField("refB"));
        StorageResults results = storage.fetch(qb.getSelect());
        try {
            assertEquals(2, results.getCount());
            for (DataRecord result : results) {
                assertTrue("TextA".equals(result.get("textA")) || "TextAC".equals(result.get("textA")));
                assertTrue("TextB".equals(result.get("textB")) || "TextBD".equals(result.get("textB")));
            }
        } finally {
            results.close();
        }
    }
}
