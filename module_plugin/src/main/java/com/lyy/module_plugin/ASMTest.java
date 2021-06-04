package com.lyy.module_plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author: user
 * @date: 2021/6/3
 * @email:
 * @description $
 * <p>
 * <p>
 * <p>
 * "I"        = int
 * "B"        = byte
 * "C"        = char
 * "D"        = double
 * "F"        = float
 * "J"        = long
 * "S"        = short
 * "Z"        = boolean
 * "V"        = void
 * "[...;"    = 数组
 * "[[...;"   = 二维数组
 * "[[[...;"  = 三维数组
 * "L....;"   = 引用类型
 */
public class ASMTest {
    public static void redefineInjectTestClass() {
        /**
         * 1、准备待分析的class
         * javac  InjectTest.java 生成 calss
         */
        //     FileInputStream fis = new FileInputStream("xxxxx/test/java/InjectTest.class");
        try {
            FileInputStream fis = new FileInputStream("D://myWork//CustomPlugins//module_plugin//src//main//java//test//InjectTest.class");

            // 1. 创建 ClassReader 读入 .class 文件到内存中
            ClassReader reader = new ClassReader(fis);
            // 2.写出器 COMPUTE_FRAMES 自动计算所有的内容，后续操作更简单
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            // 3. 创建自定义的 ClassVisitor 对象
            ClassVisitor change = new ClassAdapterVisitor(cw);
            // 4. 将 ClassVisitor 对象传入 ClassReader 中
            reader.accept(change, ClassReader.EXPAND_FRAMES);

            System.out.println("Success!");
            // 获取修改后的 class 文件对应的字节数组
            byte[] code = cw.toByteArray();
            try {
                // 将二进制流写到本地磁盘上
                FileOutputStream fos = new FileOutputStream("C://Users//user//Desktop//InjectTest.class");
                fos.write(code);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failure!");
        }

    }

    public static byte[] redefineInjectTestClass(byte[] fis) {
        /**
         * 1、准备待分析的class
         */
        //     FileInputStream fis = new FileInputStream("xxxxx/test/java/InjectTest.class");
        try {
            // FileInputStream fis = new FileInputStream("D://myWork//MvpDemo//app//src//main//java//com//example//mvpdemo//ui//activity//InjectTest.class");
            System.out.println("方法:===================" );
            // 1. 创建 ClassReader 读入 .class 文件到内存中
            ClassReader reader = new ClassReader(fis);
            // 2.写出器 COMPUTE_FRAMES 自动计算所有的内容，后续操作更简单
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            // 3. 创建自定义的 ClassVisitor 对象
            ClassVisitor change = new ClassAdapterVisitor(cw);
            // 4. 将 ClassVisitor 对象传入 ClassReader 中
            reader.accept(change, ClassReader.EXPAND_FRAMES);

//            System.out.println("Success!");
//            // 获取修改后的 class 文件对应的字节数组
            byte[] code = cw.toByteArray();
            return code;
//            try {
//                // 将二进制流写到本地磁盘上
//                FileOutputStream fos = new FileOutputStream("C://Users//user//Desktop//InjectTest.class");
//                //  FileOutputStream fos = new FileOutputStream(" D:/myWork/MvpDemo/app/src/test/java/InjectTest.class");
//                fos.write(code);
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failure!");
        }
        return null;
    }


    public static class ClassAdapterVisitor extends ClassVisitor {
        public ClassAdapterVisitor(ClassVisitor cv) {
            super(Opcodes.ASM7, cv);

        }

        //  该方法是当扫描类时第一个拜访的方法，主要用于类声明使用
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            System.out.println("方法:" + name + " 签名:===" + access);
        }

        //该方法是当扫描器扫描到类注解声明时进行调用。
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return super.visitAnnotation(descriptor, visible);
        }

        //该方法是当扫描器扫描到类中字段时进行调用。
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            System.out.println("字段:" + name + " 签名:===" + access);
            return super.visitField(access, name, descriptor, signature, value);
        }

        //该方法是当扫描器扫描到类的方法时进行调用
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                         String[] exceptions) {
            System.out.println("qqqqqqqq==" + name + "===qqqqqq==" + desc);
           if (name.equals("lyyAndroidText")) {
                System.out.println("我进入了==" + name + "===qqqqqq==" + desc);
                MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                        exceptions);
                return new MethodAdapterVisitor(api, mv, access, name, desc);
          }
            return super.visitMethod(access, name, desc, signature,
                    exceptions);
        }
    }

    /**
     * AdviceAdapter: 子类
     * 对methodVisitor进行了扩展， 能让我们更加轻松的进行方法分析
     */
    public static class MethodAdapterVisitor extends AdviceAdapter {
        private Boolean inject = true;

        protected MethodAdapterVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor);
        }

        /**
         * 分析方法上面的注解
         * 在这里干嘛？？？
         * <p>
         * 判断当前这个方法是不是使用了injecttime，如果使用了，我们就需要对这个方法插桩
         * 没使用，就不管了。
         *
         * @param descriptor
         * @param visible
         * @return
         */
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            System.out.println("方法:" + descriptor + " ===333333=");
            if (Type.getDescriptor(ASMTest.class).equals(descriptor)) {
                System.out.println(descriptor);
                inject = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        private int start;

        @Override
        protected void onMethodEnter() {
            super.onMethodEnter();
            System.out.println("方法:" + inject + " ====");
            if (inject) {
                //执行完了怎么办？记录到本地变量中
                invokeStatic(Type.getType("Ljava/lang/System;"),
                        new Method("currentTimeMillis", "()J"));
                start = newLocal(Type.LONG_TYPE);
                //创建本地 LONG类型变量
                //记录 方法执行结果给创建的本地变量
                storeLocal(start);
            }


        //  mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/lyy/customplugins/SDK", "invoke", "()V", false);
        }

        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode);
            if (inject) {
                invokeStatic(Type.getType("Ljava/lang/System;"),
                        new Method("currentTimeMillis", "()J"));
                int end = newLocal(Type.LONG_TYPE);
                storeLocal(end);
                getStatic(Type.getType("Ljava/lang/System;"), "out", Type.getType("Ljava/io" +
                        "/PrintStream;"));
                //分配内存 并dup压入栈顶让下面的INVOKESPECIAL 知道执行谁的构造方法创建StringBuilder
                newInstance(Type.getType("Ljava/lang/StringBuilder;"));
                dup();
                invokeConstructor(Type.getType("Ljava/lang/StringBuilder;"), new Method("<init>", "()V"));
                visitLdcInsn("execute:");
                invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                //减法
                loadLocal(end);
                loadLocal(start);
                math(SUB, Type.LONG_TYPE);
                invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("append", "(J)Ljava/lang/StringBuilder;"));
                invokeVirtual(Type.getType("Ljava/lang/StringBuilder;"), new Method("toString", "()Ljava/lang/String;"));
                invokeVirtual(Type.getType("Ljava/io/PrintStream;"), new Method("println", "(Ljava/lang/String;)V"));
            }
        }
    }
}
