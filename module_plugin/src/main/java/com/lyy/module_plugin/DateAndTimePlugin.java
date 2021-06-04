package com.lyy.module_plugin;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.tools.r8.w.E;
import com.android.utils.FileUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author: user
 * @date: 2021/6/3
 * @email:1083573260@qq.com
 * @description $ com.lyy.module_plugin.DateAndTimePlugin
 */
public class DateAndTimePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        appExtension.registerTransform(new CustomTransform());
    }

    public class CustomTransform extends Transform {
        @Override
        public String getName() {
            return "Team122";
        }
        @Override
        public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
            super.transform(transformInvocation);
            try {
                transform(transformInvocation.getContext(), transformInvocation.getInputs(), transformInvocation.getOutputProvider(), transformInvocation.isIncremental());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("-----------transform >>>>>>>>> 结束---wwww--------" + e.getMessage().toString());
            }

        }

        public void transform(Context context, Collection<TransformInput> inputs, TransformOutputProvider outputProvider,
                              boolean isIncremental) throws IOException, TransformException, InterruptedException {

            if (!isIncremental) {
                outputProvider.deleteAll();
            }

            inputs.forEach(input -> {
                /**遍历目录*/
                input.getDirectoryInputs().forEach(directoryInput -> {
                    directoryInput.getChangedFiles().forEach(new BiConsumer<File, Status>() {
                        @Override
                        public void accept(File file, Status status) {
                        }
                    });

                    /**当前这个 Transform 输出目录*/
                    File dest = outputProvider.getContentLocation(directoryInput.getName(),
                            directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
                    File dir = directoryInput.getFile();
                    if (dir.exists()) {
                        HashMap<String, File> modifyMap = new HashMap<>();
                        /**遍历以某一扩展名结尾的文件*/
                        File[] files = dir.listFiles(new Demo06FilterImpl());
                        //递归遍历文件.class
                        traverseFolder(context,dir,modifyMap,files);
                        try {
                            FileUtils.copyDirectory(directoryInput.getFile(), dest);
                            modifyMap.entrySet().forEach(entry -> {
                                File target = new File(dest.getAbsoluteFile() + entry.getKey());
                                if (target.exists()) {
                                    target.delete();
                                }
                                try {
                                    FileUtils.copyFile(entry.getValue(), target);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                entry.getValue().delete();
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("> dest.absolutePath: ===modified==" + "==111" + e.getMessage().toString());
                        }
                    }
                });
                // /**遍历 jar*/
                input.getJarInputs().forEach(jarInput -> {
                    String destName = jarInput.getFile().getName();
                    /**截取文件路径的 md5 值重命名输出文件,因为可能同名,会覆盖*/
                    String hexName = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath()).substring(0, 8);
                    /** 获取 jar 名字*/
                    if (destName.endsWith(".jar")) {
                        destName = destName.substring(0, destName.length() - 4);
                    }
                    /** 获得输出文件*/
                    File dests = outputProvider.getContentLocation(destName + "_" + hexName,
                            jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    File modifiedJar = modifyJar(jarInput.getFile(), context.getTemporaryDir(), true);
                    if (modifiedJar == null) {
                        modifiedJar = jarInput.getFile();
                    }
                    try {
                        FileUtils.copyFile(modifiedJar, dests);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });
            });
            System.out.println("-----------transform >>>>>>>>> 结束-----------" + getName());
        }

        public class Demo06FilterImpl implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory())
                    return true;
                return pathname.toString().endsWith(".class");
            }
        }

        public void traverseFolder( Context context,File dir, HashMap<String, File> modifyMap,File[] files ) {
            for (int i = 0; i < files.length; i++) {
                File classFile = files[i];
                if (!classFile.isDirectory()) {
                    System.out.println(files.length + "> ===modified=====lyy==我是文件==" + classFile.getAbsolutePath());
                    File modified = modifyClassFile(dir, classFile, context.getTemporaryDir());
                    if (modified != null) {
                        System.out.println("> dest.absolutePath: ===modified==" + modified.getAbsolutePath());
                        /**key 为包名 + 类名，如：/cn/sensorsdata/autotrack/android/app/MainActivity.class*/
                        String ke = classFile.getAbsolutePath().replace(dir.getAbsolutePath(), "");
                        modifyMap.put(ke, modified);
                    }
                } else {
                    File[] files1 = classFile.listFiles(new Demo06FilterImpl());
                    System.out.println(files.length + "> ===modified=====lyy=我是文件夹===" + classFile.getAbsolutePath());
                    traverseFolder(context,dir,modifyMap,files1);
                }
            }
        }

        File modifyClassFile(File dir, File classFile, File tempDir) {
            File modified = null;
            try {
                String className = path2ClassName(classFile.getAbsolutePath().replace(dir.getAbsolutePath() + File.separator, ""));
                byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile));
                byte[] modifiedClassBytes = modifyClass(sourceClassBytes);
                if (modifiedClassBytes != null) {
                    modified = new File(tempDir, className.replace('.', ' ') + ".class");
                    if (modified.exists()) {
                        modified.delete();
                    }
                    modified.createNewFile();
                    new FileOutputStream(modified).write(modifiedClassBytes);
                }
            } catch (Exception e) {
                e.printStackTrace();
                modified = classFile;
            }
            return modified;
        }


        File modifyJar(File jarFile, File tempDir, boolean nameHex) {
            if (!isModifyEnable()) {
                return null;
            }
            try {


                /**
                 * 读取原 jar
                 */
                JarFile file = new JarFile(jarFile, false);

                /**
                 * 设置输出到的 jar
                 */
                String hexName = "";
                if (nameHex) {
                    hexName = DigestUtils.md5Hex(jarFile.getAbsolutePath()).substring(0, 8);
                }
                File outputJar = new File(tempDir, hexName + jarFile.getName());
                JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar));
                Enumeration enumeration = file.entries();
                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    InputStream inputStream = null;
                    try {
                        inputStream = file.getInputStream(jarEntry);
                    } catch (Exception e) {
                        return null;
                    }
                    String entryName = jarEntry.getName();
                    if (entryName.endsWith(".DSA") || entryName.endsWith(".SF")) {
                        //ignore
                    } else {
                        JarEntry jarEntry2 = new JarEntry(entryName);
                        jarOutputStream.putNextEntry(jarEntry2);

                        byte[] modifiedClassBytes = null;
                        byte[] sourceClassBytes = IOUtils.toByteArray(inputStream);
                        if (entryName.endsWith(".class")) {
                            modifiedClassBytes = sourceClassBytes;
                        }
                        if (modifiedClassBytes == null) {
                            modifiedClassBytes = sourceClassBytes;
                        }
                        jarOutputStream.write(modifiedClassBytes);
                        jarOutputStream.closeEntry();
                    }
                }
                jarOutputStream.close();
                file.close();
                return outputJar;
            } catch (Exception E) {
                E.printStackTrace();
            }
            return null;
        }


        String path2ClassName(String pathName) {
            return pathName.replace(File.separator, ".").replace(".class", "");
        }

        /**
         * 需要处理的数据类型，有两种枚举类型
         * CLASSES 代表处理的 java 的 class 文件，RESOURCES 代表要处理 java 的资源
         *
         * @return
         */
        @Override
        public Set<QualifiedContent.ContentType> getInputTypes() {
            return TransformManager.CONTENT_CLASS;
        }

        /**
         * 指 Transform 要操作内容的范围，官方文档 Scope 有 7 种类型：
         * 1. EXTERNAL_LIBRARIES        只有外部库
         * 2. PROJECT                   只有项目内容
         * 3. PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
         * 4. PROVIDED_ONLY             只提供本地或远程依赖项
         * 5. SUB_PROJECTS              只有子项目。
         * 6. SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
         * 7. TESTED_CODE               由当前变量(包括依赖项)测试的代码
         *
         * @return
         */
        @Override
        public Set<? super QualifiedContent.Scope> getScopes() {
            return TransformManager.SCOPE_FULL_PROJECT;
        }

        @Override
        public boolean isIncremental() {
            return true;
        }
    }

    /**
     * 字节码修改是否开启，默认开启
     *
     * @return
     */
    boolean isModifyEnable() {
        return true;
    }

    /**
     * 修改class文件
     *
     * @param srcClass 源class
     * @return 目标class
     * @throws IOException
     */
    byte[] modifyClass(byte[] srcClass) throws IOException {
        System.out.println("-----------transform >>>>>>>>> 结束-----------");
        return ASMTest.redefineInjectTestClass(srcClass);
    }

    ;
}
