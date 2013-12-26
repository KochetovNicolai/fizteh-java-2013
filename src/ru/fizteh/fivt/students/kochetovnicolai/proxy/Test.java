package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.PrintStream;
import java.io.StringWriter;

public class Test {
    public interface Executable {
        Object execute(Object[] args);
        int foo(int a);
        void throwNoArg();
        String toString();
    }

    private static ClassWriter newClassWriter() {
        int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        return new ClassWriter(flags);
    }

    private static byte[] runnableWithHelloWorld() {
        ClassWriter cw = newClassWriter();
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "HelloWorldRunnable", null,
                "java/lang/Object", new String[] {"java/lang/Runnable"});

        {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, "<init>", "()V");
        ga.visitCode();
        ga.loadThis();
        ga.invokeConstructor(
                Type.getType("java/lang/Object"),
                new Method("<init>", "()V")
        );
        ga.returnValue();
        ga.endMethod();
        }
        {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, "run", "()V");
        ga.visitCode();
        ga.loadThis();
            Type printStreamType = Type.getType(PrintStream.class);
            ga.getStatic(Type.getType(System.class), "out", printStreamType);
            ga.push("Hello, World!");
            ga.invokeVirtual(printStreamType, new Method("println", "(Ljava/lang/String;)V"));
            ga.returnValue();
        ga.returnValue();
        ga.endMethod();
        }

        return cw.toByteArray();
    }


    private static Class<?> loadClass(byte[] bytes) {
        class LocalClassLoader extends ClassLoader {
            public Class<?> defineClass(byte[] bytes) {
                return super.defineClass(null, bytes, 0, bytes.length);
            }
        }
        return new LocalClassLoader().defineClass(bytes);
    }

    public static void main(String[] args) throws Exception {

        Executable executable = new Executable() {
            @Override
            public Object execute(Object[] args) {
                System.out.println("test test test");
                if (args == null) {

                    throw new IllegalStateException("ups!");
                }

                return null;
            }
            @Override
            public int foo(int a) {
                System.out.println("film film film");
                return 0;
            }
            @Override
            public void throwNoArg() {
                //try {
                    throw new IllegalStateException("throw no arg");

                //} catch (IllegalStateException e) {
                    //System.out.println("catch");
                //}

            }
        };

        /*Class<?> clazz = loadClass(runnableWithHelloWorld());
        System.out.println(clazz);
        Runnable runnable = (Runnable) clazz.newInstance();
        runnable.run();
        */
        StringWriter writer = new StringWriter();
        Executable proxy = (Executable) (new LoggingProxyFactoryImplAsm()).wrap(writer, executable, Executable.class);
        //proxy.execute(null);

        java.lang.reflect.Method[] methods = Executable.class.getMethods();
        for (java.lang.reflect.Method method : methods) {
            System.out.println(method.getName() + method.getDeclaringClass());
        }
        try {
            proxy.execute(new Object[0]);
        } catch (IllegalStateException e) {
            System.out.println("I hate empty try/catch blocks!");
        }
        proxy.foo(42);
        proxy.toString();
        proxy.hashCode();
        try {
            proxy.throwNoArg();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(writer);
        System.out.println(executable.getClass().getMethod("toString").getDeclaringClass()
                .equals(Object.class));
    }
}
