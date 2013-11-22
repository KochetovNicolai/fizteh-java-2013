package ru.fizteh.fivt.students.kochetovnicolai.fileMap;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TableRecord implements Storeable {

    public static final HashMap<String, Class<?>> SUPPORTED_TYPES;
    public static final HashMap<Class<?>, String> SUPPORTED_CLASSES;
    static {
        SUPPORTED_TYPES = new HashMap<>();
        SUPPORTED_TYPES.put("int", Integer.class);
        SUPPORTED_TYPES.put("long", Long.class);
        SUPPORTED_TYPES.put("byte", Byte.class);
        SUPPORTED_TYPES.put("float", Float.class);
        SUPPORTED_TYPES.put("double", Double.class);
        SUPPORTED_TYPES.put("boolean", Boolean.class);
        SUPPORTED_TYPES.put("String", String.class);

        SUPPORTED_CLASSES = new HashMap<>();
        SUPPORTED_CLASSES.put(Integer.class, "int");
        SUPPORTED_CLASSES.put(Long.class, "long");
        SUPPORTED_CLASSES.put(Byte.class, "byte");
        SUPPORTED_CLASSES.put(Float.class, "float");
        SUPPORTED_CLASSES.put(Double.class, "double");
        SUPPORTED_CLASSES.put(Boolean.class, "boolean");
        SUPPORTED_CLASSES.put(String.class, "String");
    }

    protected List<Class<?>> types;
    protected List<Object> values;

    TableRecord(List<Class<?>> types) throws IllegalArgumentException {
        if (types == null || types.size() == 0) {
            throw new IllegalArgumentException("invalid object list");
        }
        values = new ArrayList<>(types.size());
        this.types = types;
        for (Class<?> ignored : types) {
            values.add(null);
        }
    }

    public int size() {
        return types.size();
    }

    public int hashCode() {
       return types.hashCode() ^ values.hashCode();
    }

    public boolean equals(Object other) {
       if (other.getClass() != this.getClass()) {
           return false;
       }
       TableRecord record = (TableRecord) other;
       return types.equals(record.types) && values.equals(record.values);
    }

    protected void checkIndex(int columnIndex) throws IndexOutOfBoundsException {
        if (columnIndex < 0 || columnIndex >= values.size()) {
            throw new IndexOutOfBoundsException("index expected between 0 and " + values.size() + ", but "
                    + columnIndex + "was received");
        }
    }

    protected void checkColumnType(int columnIndex, Class<?> columnType) {
        checkIndex(columnIndex);
        if (columnType != null && !columnType.equals(types.get(columnIndex))) {
            throw new ColumnFormatException("incorrect type at position " + columnIndex
                    + ": expected" + types.get(columnIndex) + ", but " + columnType + "was received");
        }
    }

    public void setColumnFromStringAt(int columnIndex, String value) throws IndexOutOfBoundsException {
        checkIndex(columnIndex);
        Class columnType = types.get(columnIndex);
        if (columnType.equals(Integer.class)) {
            setColumnAt(columnIndex, Integer.parseInt(value));
        } else if (columnType.equals(Long.class)) {
            setColumnAt(columnIndex, Long.parseLong(value));
        } else if (columnType.equals(Byte.class)) {
            setColumnAt(columnIndex, Byte.parseByte(value));
        } else if (columnType.equals(Float.class)) {
            setColumnAt(columnIndex, Float.parseFloat(value));
        } else if (columnType.equals(Double.class)) {
            setColumnAt(columnIndex, Double.parseDouble(value));
        } else if (columnType.equals(Boolean.class)) {
            setColumnAt(columnIndex, Boolean.parseBoolean(value));
        } else if (columnType.equals(String.class)) {
            setColumnAt(columnIndex, value);
        } else {
            throw new IllegalArgumentException("unsupported object type");
        }
    }

    static public void checkTypesList(List<Class<?>> columnTypes) {
        if (columnTypes == null) {
            throw new IllegalArgumentException("types list shouldn't be null");
        }
        for (int i = 0; i < columnTypes.size(); i++) {
            if (!SUPPORTED_TYPES.containsValue(columnTypes.get(i))) {
                throw new IllegalArgumentException(columnTypes.get(i) + ": invalid type at position " + i);
            }
        }
    }

    public static void checkStoreableTypes(Storeable storeable, List<Class<?>> columnTypes)
            throws IndexOutOfBoundsException, ColumnFormatException {
        if (storeable == null) {
            throw new IllegalArgumentException("storeable shouldn't be null");
        }
        checkTypesList(columnTypes);
        for (int i = 0; i < columnTypes.size(); i++) {
            try {
                getColumnFromTypeAt(i, columnTypes.get(i), storeable);
            } catch (IndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException("invalid storeable size: expected " + columnTypes.size()
                        + ", but was " + i);
            }
        }
        try {
            storeable.getColumnAt(columnTypes.size());
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        throw new IndexOutOfBoundsException("invalid storeable size: more, than " + columnTypes.size());
    }

    public static Object getColumnFromTypeAt(int columnIndex, Class<?> columnType, Storeable storable)
            throws IndexOutOfBoundsException, ColumnFormatException {
        if (columnType.equals(Integer.class)) {
            return storable.getIntAt(columnIndex);
        } else if (columnType.equals(Long.class)) {
            return storable.getLongAt(columnIndex);
        } else if (columnType.equals(Byte.class)) {
            return storable.getByteAt(columnIndex);
        } else if (columnType.equals(Float.class)) {
            return storable.getFloatAt(columnIndex);
        } else if (columnType.equals(Double.class)) {
            return storable.getDoubleAt(columnIndex);
        } else if (columnType.equals(Boolean.class)) {
            return storable.getBooleanAt(columnIndex);
        } else if (columnType.equals(String.class)) {
            return storable.getStringAt(columnIndex);
        } else {
            throw new IllegalArgumentException("unsupported object type");
        }
    }

    public void setColumnAt(int columnIndex, Object value) throws ColumnFormatException, IndexOutOfBoundsException {
        if (value == null) {
            checkIndex(columnIndex);
            values.set(columnIndex, null);
        } else {
        checkColumnType(columnIndex, value.getClass());
        }
        values.set(columnIndex, value);
    }

    protected Object getObjectAt(int columnIndex, Class<?> columnType) {
        checkColumnType(columnIndex, columnType);
        return values.get(columnIndex);
    }

    public Object getColumnAt(int columnIndex) throws IndexOutOfBoundsException {
        checkIndex(columnIndex);
        return values.get(columnIndex);
    }

    public Integer getIntAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Integer) getObjectAt(columnIndex, Integer.class);
    }

    public Long getLongAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Long) getObjectAt(columnIndex, Long.class);
    }

    public Byte getByteAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Byte) getObjectAt(columnIndex, Byte.class);
    }

    public Float getFloatAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Float) getObjectAt(columnIndex, Float.class);
    }

    public Double getDoubleAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Double) getObjectAt(columnIndex, Double.class);
    }

    public Boolean getBooleanAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (Boolean) getObjectAt(columnIndex, Boolean.class);
    }

    public String getStringAt(int columnIndex) throws ColumnFormatException, IndexOutOfBoundsException {
        return (String) getObjectAt(columnIndex, String.class);
    }

    List<Class<?>> getTypes() {
        return types;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        boolean isFirst = true;
        for (Object value : values) {
            if (!isFirst) {
                sb.append(",");
            } else {
                isFirst = false;
            }
            if (value != null) {
                sb.append(value);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
