package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

public class TestLoggingProxyFactoryAsmImpl {
    public interface Executable {
        Object execute(Object[] args);
        void foo();
    }

    public interface VoidFunc {
        void func();
    }

    @Test
    public void voidShouldNotReturnValue() {
        VoidFunc voidFunc = new VoidFunc() {
            @Override
            public void func() {
            }
        };
        Writer writer = new StringWriter();
        VoidFunc proxy = (VoidFunc) (new LoggingProxyFactoryImplAsm()).wrap(writer, voidFunc, VoidFunc.class);
        proxy.func();
        //System.out.println(writer);
        JSONObject object = new JSONObject(writer.toString());
        String expected = "{"
                + "\"arguments\":[],"
                + "\"class\":\"" + voidFunc.getClass().getName() + "\","
                + "\"method\":\"func\""
                + "}";
        JSONObject expectedJSON = new JSONObject(expected);
        //System.out.println(expectedJSON);
        //System.out.println(object);
        expectedJSON.put("timestamp", object.get("timestamp"));
        //System.out.println(expectedJSON);
        Assert.assertEquals("invalid json object", expectedJSON.toString(), object.toString());
    }

    @Test
    public void testFormatShouldBeCorrect() {
        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                Object[] array = new Object[6];
                array[0] = 42;
                array[1] = 3.1415;
                array[2] = "abracadabra";
                array[3] = true;
                array[4] = null;
                array[5] = array;
                return array;
            }
            @Override
            public void foo() {}
        };
        ArrayList<Object> arg = new ArrayList<>();
        arg.add("123");
        arg.add("456");
        arg.add(arg);
        Object[] args = new Object[2];
        args[0] = args;
        args[1] = arg;
        Writer writer = new StringWriter();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        proxy.execute(args);
        //System.out.println(writer);
        JSONObject object = new JSONObject(writer.toString());
        String expected = "{"
                + "\"arguments\":[[\"cyclic\",[\"123\",\"456\", cyclic]]],"
                + "\"returnValue\":[42,3.1415,\"abracadabra\",true,null, cyclic],"
                + "\"class\":\"" + executable.getClass().getName() + "\","
                + "\"method\":\"execute\""
                + "}";
        JSONObject expectedJSON = new JSONObject(expected);
        //System.out.println(expectedJSON);
        //System.out.println(object);
        expectedJSON.put("timestamp", object.get("timestamp"));
        //System.out.println(expectedJSON);
        Assert.assertEquals("invalid json object", expectedJSON.toString(), object.toString());
    }

    @Test
    public void invalidWriterShouldWork() throws IOException {
        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                return null;
            }
            @Override
            public void foo() {}
        };
        StringWriter writer = new StringWriter();
        writer.close();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        proxy.execute(null);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void proxyShouldThrowsExceptions() throws IOException {
        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                return args[1];
            }
            @Override
            public void foo() {}
        };
        StringWriter writer = new StringWriter();
        writer.close();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        proxy.execute(new Object[1]);
    }

    @Test
    public void logShouldContainsExceptions() throws IOException {
        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                return args[1];
            }
            @Override
            public void foo() {}
        };
        StringWriter writer = new StringWriter();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        try {
            proxy.execute(new Object[1]);
        } catch (IndexOutOfBoundsException e) {
            JSONObject object = new JSONObject(writer.toString());
            String expected = "{"
                    + "\"thrown\":\"java.lang.ArrayIndexOutOfBoundsException: 1\","
                    + "\"arguments\":[[null]],"
                    + "\"class\":\"" + executable.getClass().getName() + "\","
                    + "\"method\":\"execute\""
                    + "}";
            JSONObject expectedJSON = new JSONObject(expected);
            //System.out.println(expectedJSON);
            //System.out.println(object);
            expectedJSON.put("timestamp", object.get("timestamp"));
            //System.out.println(expectedJSON);
            Assert.assertEquals("invalid json object", expectedJSON.toString(), object.toString());
            return;
        }
        Assert.fail("expected IndexOutOfBoundsException");
    }

    @Test
    public void emptyArgumentsShouldBeRight() {
        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                return null;
            }
            @Override
            public void foo() {}
        };
        Writer writer = new StringWriter();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        proxy.foo();
        //System.out.println(writer);
        JSONObject object = new JSONObject(writer.toString());
        String expected = "{"
                + "\"arguments\":[],"
                + "\"class\":\"" + executable.getClass().getName() + "\","
                + "\"method\":\"foo\""
                + "}";
        JSONObject expectedJSON = new JSONObject(expected);
        //System.out.println(expectedJSON);
        //System.out.println(object);
        expectedJSON.put("timestamp", object.get("timestamp"));
        //System.out.println(expectedJSON);
        Assert.assertEquals("invalid json object", expectedJSON.toString(), object.toString());
    }
}
