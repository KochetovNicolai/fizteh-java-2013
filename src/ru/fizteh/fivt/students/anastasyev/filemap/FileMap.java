package ru.fizteh.fivt.students.anastasyev.filemap;

import ru.fizteh.fivt.storage.structured.Storeable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileMap {
    private File fileMap;
    private FileMapTable table;
    private FileMapTableProvider provider;
    private HashMap<String, Storeable> elementHashMap = new HashMap<String, Storeable>();
    private int ndirectory;
    private int nfile;

    public FileMap(String dbDir, int directory, int file, FileMapTable myTable, FileMapTableProvider myProvider)
            throws IOException, ParseException {
        fileMap = new File(dbDir);
        ndirectory = directory;
        nfile = file;
        table = myTable;
        provider = myProvider;
        try {
            openFileMapWithCheck();
        } catch (FileNotFoundException e) {
            throw new IOException("File " + nfile + ".dat not found", e);
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void openFileMapWithCheck() throws IOException, ParseException {
        if (!fileMap.exists()) {
            return;
        }
        try (RandomAccessFile input = new RandomAccessFile(fileMap.toString(), "r")) {
            if (input.length() == 0) {
                return;
            }
            while (input.getFilePointer() < input.length()) {
                readWithCheck(input);
            }
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(nfile + ".dat - File not found");
        } catch (IOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void readWithCheck(RandomAccessFile input) throws IOException, ParseException {
        int keyLength;
        int valueLength;
        try {
            keyLength = input.readInt();
            valueLength = input.readInt();
        } catch (IOException e) {
            throw new IOException("Error in key/value reading", e);
        }
        if (keyLength <= 0 || valueLength <= 0) {
            throw new IOException(nfile + ".dat has incorrect format");
        }
        try {
            byte[] keyBytes = new byte[keyLength];
            byte[] valueBytes = new byte[valueLength];
            input.read(keyBytes);
            input.read(valueBytes);
            if (keyBytes.length != keyLength || valueBytes.length != valueLength) {
                throw new IOException("Error in read strings in " + nfile + ".dat");
            }
            String key = new String(keyBytes);
            int hashcode = Math.abs(key.hashCode());
            if (hashcode % 16 != ndirectory || hashcode / 16 % 16 != nfile) {
                throw new IOException(ndirectory + ".dir" + File.separator + nfile + ".dat has wrong key: " + key);
            }
            String value = new String(valueBytes);
            elementHashMap.put(key, provider.deserialize(table, value));
        } catch (OutOfMemoryError e) {
            throw new IOException(nfile + ".dat has incorrect format", e);
        }
    }

    private void write(RandomAccessFile output, String key, Storeable value) throws IOException {
        output.writeInt(key.getBytes("UTF-8").length);
        output.writeInt(provider.serialize(table, value).getBytes("UTF-8").length);
        output.write(key.getBytes("UTF-8"));
        output.write(provider.serialize(table, value).getBytes("UTF-8"));
    }

    public void save() throws IOException {
        if (!fileMap.getParentFile().exists()) {
            if (!fileMap.getParentFile().mkdir()) {
                throw new IOException("Can't create " + ndirectory + ".dir");
            }
        }
        if (!fileMap.exists()) {
            if (!fileMap.createNewFile()) {
                throw new IOException("Can't create " + nfile + ".dat");
            }
        }
        try (RandomAccessFile output = new RandomAccessFile(fileMap.toString(), "rw")) {
            output.setLength(0);
            Set<Map.Entry<String, Storeable>> hashMapSet = elementHashMap.entrySet();
            for (Map.Entry<String, Storeable> element : hashMapSet) {
                write(output, element.getKey(), element.getValue());
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Can't find file to commit", e);
        } catch (Exception e) {
            throw new IOException("Can't commit FileMap", e);
        }
    }

    public Storeable put(String newKey, Storeable newValue) {
        return elementHashMap.put(newKey, newValue);
    }

    public Storeable get(String key) {
        return elementHashMap.get(key);
    }

    public Storeable remove(String key) {
        return elementHashMap.remove(key);
    }

    public boolean isEmpty() {
        return elementHashMap.isEmpty();
    }

    public void delete() throws IOException {
        if (!fileMap.getParentFile().exists()) {
            return;
        }
        if (fileMap.exists() && !fileMap.delete()) {
            throw new IOException("Can't remove empty fileMap");
        }
        if (fileMap.getParentFile().listFiles().length == 0) {
            if (!fileMap.getParentFile().delete()) {
                throw new IOException("Can't remove empty fileMaps directory");
            }
        }
    }

    public int size() {
        return elementHashMap.size();
    }
}
