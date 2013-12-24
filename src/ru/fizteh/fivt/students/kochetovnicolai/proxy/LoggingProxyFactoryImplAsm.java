package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import ru.fizteh.fivt.proxy.LoggingProxyFactory;

import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

public class LoggingProxyFactoryImplAsm implements LoggingProxyFactory {

    public void doNothing() {
        ConsoleLoggerInvocationHandler.doNothing();
    }

    public void writeLog(String className, String methodName, Writer writer,
                         Object[] args, Object returnValue, Throwable throwable, boolean returned) {
        ConsoleLoggerInvocationHandler.writeLog(className, methodName, writer, args, returnValue, throwable, returned);
    }

    private static ClassWriter newClassWriter() {
        int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        return new ClassWriter(flags);
    }

    private static byte[] myInterface(Class<?> interFace, Writer writer, Object object) {

        ClassWriter cw = newClassWriter();

        final Type type = Type.getType(object.getClass().getName().replace(".", "") + "Proxy");
        Type writerType = Type.getType(writer.getClass());
        Type objectType = Type.getType(object.getClass());
        Type thisType = Type.getType(LoggingProxyFactoryImplAsm.class);

        //System.out.println(Type.getType(interFace).getInternalName());
        //System.out.println(object.getClass().getSimpleName());

        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, type.getInternalName(), null,
                "java/lang/Object", new String[] {Type.getType(interFace).getInternalName()});

        cw.visitField(Opcodes.ACC_PRIVATE,
                "writer", Type.getDescriptor(writer.getClass()), null, null)
                .visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE,
                "object", Type.getDescriptor(object.getClass()), null, null)
                .visitEnd();

        /*cw.visitField(Opcodes.ACC_PRIVATE,
                "nullObject", Type.getDescriptor(Object.class), null, null)
                .visitEnd();      */

        cw.visitField(Opcodes.ACC_PRIVATE,
                "returned", Type.getDescriptor(Object.class), null, null)
                .visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE,
                "throwable", Type.getDescriptor(Throwable.class), null, null)
                .visitEnd();

        /*cw.visitField(Opcodes.ACC_PRIVATE,
                "nullThrowable", Type.getDescriptor(Throwable.class), null, null)
                .visitEnd(); */

        cw.visitField(Opcodes.ACC_PRIVATE,
                "factory", Type.getDescriptor(LoggingProxyFactoryImplAsm.class), null, null)
                .visitEnd();

        {
            Method mt = new Method("<init>", Type.VOID_TYPE, new Type[]{writerType, objectType, thisType});

            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor(), null, null);
            GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor());
            //MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            //GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, "<init>", "()V");
            ga.visitCode();

            ga.loadArg(2);
            Method doNothing = new Method("doNothing", "()V"); //Type.VOID_TYPE, new Type[]{});
            ga.invokeVirtual(Type.getType(LoggingProxyFactoryImplAsm.class), doNothing);

            /*
            Type printStreamType = Type.getType(PrintStream.class);
            ga.getStatic(Type.getType(System.class), "out", printStreamType);
            ga.push("constructor");
            ga.invokeVirtual(printStreamType, new Method("println", "(Ljava/lang/String;)V"));
            //*/
            ga.loadThis();
            ga.invokeConstructor(
                    Type.getType("java/lang/Object"),
                    new Method("<init>", "()V")
            );
            ga.loadThis();
            ga.dup();
            ga.dup();
            ga.loadArg(0);
            ga.putField(type, "writer", writerType);
            ga.loadArg(1);
            ga.putField(type, "object", objectType);
            ga.loadArg(2);
            ga.putField(type, "factory", thisType);
            ga.returnValue();
            ga.endMethod();
        }

        java.lang.reflect.Method[] methods = interFace.getMethods();
        for (java.lang.reflect.Method method : methods) {
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            Class<?>[] argTypes = method.getParameterTypes();
            Type[] types1 = new Type[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                types1[i] = Type.getType(argTypes[i]);
                //System.out.println(types1[i]);
            }

            //System.out.println(method.getName());
            Method mt = new Method(method.getName(), Type.getType(method.getReturnType()), types1);

            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor(), null, null);
            GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor());
            ga.visitCode();
            /*
            Type printStreamType = Type.getType(PrintStream.class);
            ga.getStatic(Type.getType(System.class), "out", printStreamType);
            ga.push("Hello, World!");
            ga.invokeVirtual(printStreamType, new Method("println", "(Ljava/lang/String;)V"));
            */
            Label tryLabel = ga.newLabel();
            Label catchLabel = ga.newLabel();
            Label finallyLabel = ga.newLabel();
            //ga.putStatic();
            Method writeLog = new Method("writeLog", Type.VOID_TYPE, new Type[] {
               Type.getType(String.class),
               Type.getType(String.class),
               Type.getType(Writer.class),
               Type.getType(Object[].class),
               Type.getType(Object.class),
               Type.getType(Throwable.class),
               Type.BOOLEAN_TYPE
            });

            ga.mark(tryLabel);

            ga.loadThis();
            ga.getField(type, "object", objectType);
            for (int i = 0; i < types1.length; i++) {
                ga.loadArg(i);
            }

            /*  className
                methodName
                writer
                [array with arguments]
                this
                argument 0
                ...
                argument length - 1
            */
            //method.setAccessible(true);

            ga.invokeInterface(Type.getType(interFace), mt);
            //ga.invokeVirtual(objectType, mt);
            ga.goTo(finallyLabel);




            ga.mark(catchLabel);

            ga.catchException(tryLabel, catchLabel, Type.getType(Throwable.class));
            ga.loadThis();
            ga.swap();
            ga.putField(type, "throwable", Type.getType(Throwable.class));

            ga.loadThis();
            ga.getField(type, "factory", thisType);
            ga.push(object.getClass().getName());
            ga.push(mt.getName());
            ga.loadThis();
            ga.getField(type, "writer", writerType);

            /*  className
                methodName
                writer    */

            ga.push(types1.length);
            ga.newArray(Type.getType(Object.class));
            for (int i = 0; i < types1.length; i++) {
                ga.dup();
                ga.push(i);
                ga.loadArg(i);
                if (types1[i].getDescriptor().length() == 1) {
                    ga.box(types1[i]);
                }
                ga.arrayStore(Type.getType(Object.class));
            }
            //ga.loadThis();
            //ga.getField(type, "nullObject", Type.getType(Object.class));
            ga.push((Type) null);
            ga.loadThis();
            ga.getField(type, "throwable", Type.getType(Throwable.class));
            ga.push(false);

            ga.invokeVirtual(thisType, writeLog);
            //ga.invokeStatic(Type.getType(ConsoleLoggerInvocationHandler.class), writeLog);   <---IllegalAccessError
            ga.loadThis();
            ga.getField(type, "throwable", Type.getType(Throwable.class));
            ga.throwException();


            /************************************/
            boolean returned = !method.getReturnType().getName().equals("void");

            ga.mark(finallyLabel);
            if (returned) {
                ga.loadThis();
                ga.swap();
                //Label isNull = ga.newLabel();
                //ga.ifNull(isNull);
                Type returnType = Type.getType(method.getReturnType());
                if (returnType.getDescriptor().length() == 1) {
                    ga.box(returnType);
                }
                //ga.mark(isNull);
                ga.putField(type, "returned", Type.getType(Object.class));
            }

            ga.loadThis();
            ga.getField(type, "factory", thisType);
            ga.push(object.getClass().getName());
            ga.push(mt.getName());
            ga.loadThis();
            ga.getField(type, "writer", writerType);


            ga.push(types1.length);
            ga.newArray(Type.getType(Object.class));
            for (int i = 0; i < types1.length; i++) {
                ga.dup();
                ga.push(i);
                ga.loadArg(i);
                if (types1[i].getDescriptor().length() == 1) {
                    ga.box(types1[i]);
                }
                ga.arrayStore(Type.getType(Object.class));
            }
            ga.loadThis();
            ga.getField(type, "returned", Type.getType(Object.class));
            //ga.loadThis();
            //ga.getField(type, "nullThrowable", Type.getType(Throwable.class));
            ga.push((Type) null);
            ga.push(returned);
            ga.invokeVirtual(thisType, writeLog);

            if (returned) {
                ga.loadThis();
                Type returnType = Type.getType(method.getReturnType());
                ga.getField(type, "returned", Type.getType(Object.class));
                //Label isNull = ga.newLabel();
                //ga.ifNull(isNull);
                if (returnType.getDescriptor().length() == 1) {
                    ga.unbox(returnType);
                }
                if (returnType.getDescriptor().charAt(0) == '[') {
                    //ga.cast(Type.getType(Object.class), Type.getType(method.getReturnType()));
                    ga.unbox(returnType);
                }
                if (returnType.equals(Type.getType(String.class))) {
                    ga.unbox(returnType);
                }
                if (returnType.equals(Type.getType(Class.class))) {
                    ga.unbox(returnType);
                }
                //ga.mark(isNull);
            }
            //ga.cast(Type.getType(Object.class), Type.getType(method.getReturnType()));
            //ga.unbox(Type.getType(method.getReturnType()));
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

    @Override
    public Object wrap(Writer writer, Object implementation, Class<?> interfaceClass) {
        if (writer == null) {
            throw new IllegalArgumentException("writer shouldn't be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("implementation shouldn't be null");
        }
        if (interfaceClass == null) {
            throw new IllegalArgumentException("interfaceClass shouldn't be null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("interfaceClass is not interface");
        }
        if (!interfaceClass.isAssignableFrom(implementation.getClass())) {
            throw new IllegalArgumentException(implementation + " doesn't implements " + interfaceClass);
        }
        try {
            return loadClass(myInterface(interfaceClass, writer, implementation)).getConstructor(
                    writer.getClass(), implementation.getClass(), this.getClass()
            ).newInstance(writer, implementation, this);
        } catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}

