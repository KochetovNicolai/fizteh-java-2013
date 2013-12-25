package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

class ConsoleLoggerInvocationHandler implements InvocationHandler {
    private Writer writer;
    private Object implementation;

    ConsoleLoggerInvocationHandler(Writer writer, Object implementation) {
        this.writer = writer;
        this.implementation = implementation;
    }

    private static void addObjectInArray(JSONArray jsonArray, Object anArray,
                                         IdentityHashMap<Object, Object> addedElements) {
        if (anArray != null && Iterable.class.isAssignableFrom(anArray.getClass())) {
            if (addedElements.containsKey(anArray)) {
                jsonArray.put("cyclic");
            } else {
                jsonArray.put(resolveIterable((Iterable) anArray, addedElements));
            }
        } else if (anArray != null && anArray.getClass().isArray()) {
            if (addedElements.containsKey(anArray)) {
                jsonArray.put("cyclic");
            } else {
                try {
                    jsonArray.put(createJSONArray((Object[]) anArray, addedElements));
                } catch (ClassCastException e) {
                    jsonArray.put(anArray.toString());
                }
            }
        } else {
            jsonArray.put(anArray);
        }
    }

    private static JSONArray createJSONArray(Object[] array, IdentityHashMap<Object, Object> addedElements) {
        addedElements.put(array, null);
        JSONArray jsonArray = new JSONArray();
        for (Object anArray : array) {
            addObjectInArray(jsonArray, anArray, addedElements);
        }
        addedElements.remove(array);
        return jsonArray;
    }

    private static JSONArray resolveIterable(Iterable array, IdentityHashMap<Object, Object> addedElements) {
        addedElements.put(array, null);
        JSONArray jsonArray = new JSONArray();
        for (Object anArray : array) {
            addObjectInArray(jsonArray, anArray, addedElements);
        }
        addedElements.remove(array);
        return jsonArray;
    }

    private static void writeJSONObject(JSONObject object, Writer writer) {

        try {
            writer.write(object.toString() + System.lineSeparator());
        } catch (IOException e) {
            return;
        }
    }

    public static void writeLog(String className, String methodName, Writer writer,
                                Object[] args, Object returnValue, Throwable throwable, boolean returned) {
        boolean failed = false;
        JSONObject log = null;
        try {
            log = new JSONObject();
            log.put("timestamp", System.currentTimeMillis());
            log.put("class", className);
            log.put("method", methodName);
            if (args == null || args.length == 0) {
                log.put("arguments", new Object[0]);
            } else {
                log.put("arguments", createJSONArray(args, new IdentityHashMap<>()));
            }
        } catch (Throwable e) {
            failed = true;
        }
        if (throwable != null) {
            if (!failed) {
                try {
                    log.put("thrown", throwable.toString());
                    writeJSONObject(log, writer);
                } catch (Throwable t) {
                    failed = true;
                }
            }
            return;
        }
        if (!failed) {
            try {
                if (!returned) {
                    writeJSONObject(log, writer);
                    return;
                }
                Object writingValue = null;
                if (returnValue != null) {
                    if (returnValue.getClass().isArray()) {
                        try {
                            writingValue = createJSONArray((Object[]) returnValue, new IdentityHashMap<>());
                        } catch (ClassCastException e) {
                            writingValue = returnValue.toString();
                        }
                    } else if (Iterable.class.isAssignableFrom(returnValue.getClass())) {
                        writingValue = resolveIterable((Iterable) returnValue, new IdentityHashMap<>());
                    } else {
                        writingValue = returnValue;
                    }
                    log.put("returnValue", writingValue);
                } else {
                    log.put("returnValue", JSONObject.NULL);
                }
                writeJSONObject(log, writer);
            } catch (Throwable e) {
                failed = true;
            }
        }
    }

    public static void doNothing() {

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                return method.invoke(implementation, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
        Object returnValue = null;
        Throwable throwable = null;
        try {
            returnValue = method.invoke(implementation, args);
        } catch (InvocationTargetException e) {
            throwable = e.getTargetException();
        }
        boolean returned = !method.getReturnType().getName().equals("void");
        writeLog(implementation.getClass().getName(), method.getName(), writer, args, returnValue, throwable, returned);
        if (throwable == null) {
            return returnValue;
        } else {
            throw throwable;
        }
        /*
        boolean failed = false;
        JSONObject log = null;
        try {
            log = new JSONObject();
            log.put("timestamp", System.currentTimeMillis());
            log.put("class", implementation.getClass().getName());
            log.put("method", method.getName());
            if (args == null || args.length == 0) {
                log.put("arguments", new Object[0]);
            } else {
                log.put("arguments", createJSONArray(args, new IdentityHashMap<>()));
            }
        } catch (Throwable e) {
            failed = true;
        }
        Object returnValue;
        Object writingValue = null;
        try {
            returnValue = method.invoke(implementation, args);
        } catch (InvocationTargetException e) {
            Throwable exception = e.getTargetException();
            if (!failed) {
                try {
                    log.put("thrown", exception.toString());
                    writeJSONObject(log, Void.class, writer);
                } catch (Throwable t) {
                    failed = true;
                }
            }
            throw e.getTargetException();
        }
        if (!failed) {
            try {
                if (returnValue != null) {
                    if (returnValue.getClass().isArray()) {
                        writingValue = createJSONArray((Object[]) returnValue, new IdentityHashMap<>());
                    } else if (Iterable.class.isAssignableFrom(returnValue.getClass())) {
                        writingValue = resolveIterable((Iterable) returnValue, new IdentityHashMap<>());
                    } else {
                        writingValue = returnValue;
                    }
                } else {
                    Class type = method.getReturnType();
                    if (type.getName().equals("void")) {
                        writingValue = Void.class;
                    }
                }
                writeJSONObject(log, writingValue, writer);
            } catch (Throwable e) {
                failed = true;
            }
        }
        return returnValue;
        */
    }
}
