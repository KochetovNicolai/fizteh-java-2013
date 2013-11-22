package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import org.junit.*;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;
import ru.fizteh.fivt.storage.structured.TableProvider;
import java.text.ParseException;

import java.io.IOException;
import java.util.ArrayList;

@RunWith(Theories.class)
public class TestTable {

    DistributedTableProviderFactory factory;
    TableProvider provider;
    Table table;
    protected String validTableName = "default";
    protected String validString = "<row><col>justSimpleValidKeyOrValue</col></row>";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createWorkingDirectoryAndTable() throws IOException {
        factory = new DistributedTableProviderFactory();
        provider = factory.create(folder.getRoot().getPath());
        ArrayList<Class<?>> type = new ArrayList<>();
        type.add(String.class);
        table = provider.createTable(validTableName, type);
    }

    @After
    public void removeWorkingDirectoryAndProvider() {
        factory = null;
        provider = null;
        table = null;
    }

    @DataPoints
    public static String[] argumentsWithBadSymbols = new String [] {
            null,
            "",
            " ",
            "   ",
            "\t",
            "\t \t",
            "\n\t    ",
            " \t \t ",
    };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Theory
    public void putKeyWithBadSymbolShouldFail(String key) throws ParseException {
        thrown.expect(IllegalArgumentException.class);
        table.put(key, provider.deserialize(table, validString));
    }

    @Theory
    public void getWithBadSymbolShouldFail(String key) {
        thrown.expect(IllegalArgumentException.class);
        table.get(key);
    }

    @Theory
    public void removeWithBadSymbolShouldFail(String key) {
        thrown.expect(IllegalArgumentException.class);
        table.remove(key);
    }

    @Test
    public void basicTableMethodsShouldWork() throws ParseException, IOException {
        Storeable value = provider.deserialize(table, "<row><col>value</col></row>");
        Storeable string = provider.deserialize(table, validString);
        Assert.assertTrue("empty table size should be null", table.size() == 0);
        Assert.assertTrue("remove from empty table should be null", table.remove(validString) == null);
        Assert.assertTrue("rollback from empty table should return 0", table.rollback() == 0);
        Assert.assertTrue("put new key should be null", table.put(validString, string) == null);
        Assert.assertTrue("table size should equals 1", table.size() == 1);
        Assert.assertTrue("commit should return 1", table.commit() == 1);
        Assert.assertTrue("put new key should be null", table.put("key", value) == null);
        Assert.assertEquals("remove should return old value", table.remove(validString), string);
        Assert.assertEquals("rollback should return 2", table.rollback(), 2);
        Assert.assertEquals("key should exists after rollback", table.get(validString), string);
        Assert.assertEquals("key shouldn't exists after rollback", table.get("key"), null);
    }

    @Test
    public void commitAndRollbackShouldWork() throws ParseException, IOException {
        Storeable value = provider.deserialize(table, "<row><col>value</col></row>");
        Storeable value1 = provider.deserialize(table, "<row><col>value1</col></row>");
        Storeable value2 = provider.deserialize(table, "<row><col>value2</col></row>");
        Storeable value3 = provider.deserialize(table, "<row><col>value3</col></row>");
        Assert.assertTrue("put new key should be null", table.put("key", value) == null);
        Assert.assertEquals("remove should return value", table.remove("key"), value);
        Assert.assertEquals("commit should return 0", table.commit(), 0);

        Assert.assertTrue("put new key should be null", table.put("key", value) == null);
        Assert.assertEquals("commit should return 1", table.commit(), 1);

        Assert.assertEquals("remove should return value", table.remove("key"), value);
        Assert.assertTrue("put new key should be null", table.put("key", value) == null);
        Assert.assertEquals("commit should return 0", table.commit(), 0);

        Assert.assertTrue("put new key should be null", table.put("key1", value1) == null);
        Assert.assertTrue("put new key should be null", table.put("key2", value2) == null);
        Assert.assertEquals("commit should return 2", table.commit(), 2);
        Assert.assertEquals("remove should return value1", table.remove("key1"), value1);
        Assert.assertEquals("put should return value1", table.put("key2", value), value2);
        Assert.assertTrue("put new key should be null", table.put("key3", value3) == null);
        Assert.assertEquals("rollback should return 3", table.rollback(), 3);
    }

    @Test(timeout = 90000)
    public void putAndCommitMore() throws IOException {
        ArrayList<Class<?>> types = new ArrayList<>();
        types.add(Integer.class);
        types.add(String.class);
        Table table = provider.createTable("jastanothertable", types);
        for (int i = 0; i < 1000; i++) {
            Storeable storeable = new TableRecord(types);
            storeable.setColumnAt(0, i);
            storeable.setColumnAt(1, Integer.toString(i));
            table.put(Integer.toString(i), storeable);
            table.commit();
        }
    }

    @Test
    public void tableShouldBeConsistent() throws ParseException, IOException {
        Storeable value4 = provider.deserialize(table, "<row><col>value4</col></row>");
        Storeable value1 = provider.deserialize(table, "<row><col>value1</col></row>");
        Storeable value2 = provider.deserialize(table, "<row><col>value2</col></row>");
        Storeable value3 = provider.deserialize(table, "<row><col></col></row>");
        Storeable value5 = provider.deserialize(table, "<row><col>value5</col></row>");
        Assert.assertTrue("put new key should be null", table.put("key1", value1) == null);
        Assert.assertTrue("put new key should be null", table.put("key2", value2) == null);
        Assert.assertTrue("put new key should be null", table.put("key3", value3) == null);
        Assert.assertTrue("commit should return 3", table.commit() == 3);
        Assert.assertTrue("put new key should be null", table.put("key5", value5) == null);
        Assert.assertTrue("put new key should be null", table.put("key4", value4) == null);

        table = null;
        provider = null;
        factory = null;

        factory = new DistributedTableProviderFactory();
        provider = factory.create(folder.getRoot().getPath());
        ArrayList<Class<?>> type = new ArrayList<>();
        type.add(String.class);
        table = provider.getTable(validTableName);

        Assert.assertEquals("table size should be equals 3", table.size(), 3);
        Assert.assertEquals("key should exists in file", table.get("key1"), value1);
        Assert.assertEquals("key should exists in file", table.get("key2"), value2);
        Assert.assertEquals("key should exists in file", table.get("key3"), value3);
        Assert.assertEquals("key should not exists in file", table.get("key4"), null);
        Assert.assertEquals("key should not exists in file", table.get("key5"), null);
    }
}
