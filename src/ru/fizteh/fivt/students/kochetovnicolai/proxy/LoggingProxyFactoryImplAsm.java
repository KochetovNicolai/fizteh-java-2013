package ru.fizteh.fivt.students.kochetovnicolai.proxy;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import ru.fizteh.fivt.proxy.LoggingProxyFactory;

import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

public class LoggingProxyFactoryImplAsm implements LoggingProxyFactory {

    private static ClassWriter newClassWriter() {
        int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        return new ClassWriter(flags);
    }

    private static byte[] myInterface(Class<?> interFace, Writer writer, Object object) {

        ClassWriter cw = newClassWriter();

        final Type type = Type.getType(object.getClass().getName().replace(".", "") + "Proxy");
        Type writerType = Type.getType(writer.getClass());
        Type objectType = Type.getType(object.getClass());

        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, type.getInternalName(), null,
                "java/lang/Object", new String[]{Type.getType(interFace).getInternalName()});

        cw.visitField(Opcodes.ACC_PRIVATE,
                "writer", Type.getDescriptor(writer.getClass()), null, null)
                .visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE,
                "object", Type.getDescriptor(object.getClass()), null, null)
                .visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE,
                "returned", Type.getDescriptor(Object.class), null, null)
                .visitEnd();

        cw.visitField(Opcodes.ACC_PRIVATE,
                "throwable", Type.getDescriptor(Throwable.class), null, null)
                .visitEnd();

        {
            Method mt = new Method("<init>", Type.VOID_TYPE, new Type[]{writerType, objectType});

            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor(), null, null);
            GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor());
            ga.visitCode();

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
            ga.returnValue();
            ga.endMethod();
        }

        java.lang.reflect.Method[] methods = interFace.getMethods();
        java.lang.reflect.Method[] objectMethods = Object.class.getMethods();

        for (java.lang.reflect.Method method : methods) {
            boolean hasMethod = false;
            for (java.lang.reflect.Method objectMethod : objectMethods) {
                if (objectMethod.getName().equals(method.getName())) {
                    hasMethod = true;
                }
            }
            try {
                if (hasMethod
                        && !object.getClass().getMethod(method.getName(), method.getParameterTypes())
                        .getDeclaringClass().equals(object.getClass())) {
                    continue;
                }
            } catch (NoSuchMethodException e) {
                continue;
            }
            Class<?>[] argTypes = method.getParameterTypes();
            Type[] types1 = new Type[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                types1[i] = Type.getType(argTypes[i]);
            }

            Method mt = new Method(method.getName(), Type.getType(method.getReturnType()), types1);

            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor(), null, null);
            GeneratorAdapter ga = new GeneratorAdapter(mv, Opcodes.ACC_PUBLIC, mt.getName(), mt.getDescriptor());


            ga.visitCode();


            Label tryLabel = ga.newLabel();
            Label catchLabel = ga.newLabel();
            Label finallyLabel = ga.newLabel();

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

            ga.invokeInterface(Type.getType(interFace), mt);
            ga.goTo(finallyLabel);


            ga.mark(catchLabel);

            ga.catchException(tryLabel, catchLabel, Type.getType(Throwable.class));
            //ga.loadThis();
            //ga.swap();
            //ga.putField(type, "throwable", Type.getType(Throwable.class));
            int throwable = ga.newLocal(Type.getType(Throwable.class));
            ga.storeLocal(throwable);

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
            ga.push((Type) null);
            //ga.loadThis();
            //ga.getField(type, "throwable", Type.getType(Throwable.class));
            ga.loadLocal(throwable);
            ga.push(false);

            ga.invokeStatic(Type.getType(ConsoleLoggerInvocationHandler.class), writeLog);  // <---IllegalAccessError
            //ga.loadThis();
            //ga.getField(type, "throwable", Type.getType(Throwable.class));
            ga.loadLocal(throwable);
            ga.throwException();


            /************************************/
            boolean isReturn = !method.getReturnType().getName().equals("void");
            Type returnType = Type.getType(method.getReturnType());
            int returned = ga.newLocal(Type.getType(Object.class));

            ga.mark(finallyLabel);
            if (isReturn) {
                //ga.loadThis();
                //ga.swap();

                if (returnType.getDescriptor().length() == 1) {
                    ga.box(returnType);
                }
                //ga.putField(type, "returned", Type.getType(Object.class));
                ga.storeLocal(returned);
            } else {
                ga.push((Type) null);
                ga.storeLocal(returned);
            }

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

            //ga.loadThis();
            //ga.getField(type, "returned", Type.getType(Object.class));
            ga.loadLocal(returned);
            ga.push((Type) null);
            ga.push(isReturn);


            ga.invokeStatic(Type.getType(ConsoleLoggerInvocationHandler.class), writeLog);

            if (isReturn) {
                //ga.loadThis();
                //ga.getField(type, "returned", Type.getType(Object.class));
                ga.loadLocal(returned);
                if (returnType.getDescriptor().length() == 1
                        || returnType.getDescriptor().charAt(0) == '['
                        || returnType.equals(Type.getType(String.class))
                        || returnType.equals(Type.getType(Class.class))) {
                    ga.unbox(returnType);
                }
            }
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
                    writer.getClass(), implementation.getClass()
            ).newInstance(writer, implementation);
        } catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}

