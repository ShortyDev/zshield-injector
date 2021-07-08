package at.shorty.zshield.injector;

import at.shorty.zshield.injector.visitors.EncryptionResponseVisitor;
import at.shorty.zshield.injector.visitors.InitialHandlerVisitor;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class Injector {

    public static final String PREFIX = "[Z-SHIELD INJECTOR] ";
    static byte[] initialHandlerRewrite;
    static byte[] encryptionResponseRewrite;

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        System.out.println("  ____   ___ _  _ ___ ___ _    ___    ___ _  _    _ ___ ___ _____ ___  ___ \n" +
                " |_  /__/ __| || |_ _| __| |  |   \\  |_ _| \\| |_ | | __/ __|_   _/ _ \\| _ \\\n" +
                "  / /___\\__ \\ __ || || _|| |__| |) |  | || .` | || | _| (__  | || (_) |   /\n" +
                " /___|  |___/_||_|___|___|____|___/  |___|_|\\_|\\__/|___\\___| |_| \\___/|_|_\\\n" +
                " Make your BungeeCord compatible with Z-Shield!");
        System.out.println(" Developed by Shorty with help JByteMod and Stackoverflow - dependencies: commons-io, ASM");
        System.out.println(PREFIX + "Starting...");
        if (!new File("BungeeCord.jar").exists()) {
            System.out.println(PREFIX + "Please make sure BungeeCord.jar exists in the running directory.");
            System.exit(1);
        }
        JarFile jarFile = new JarFile("BungeeCord.jar");
        File output = new File("BungeeCord-out.jar");
        if (output.exists())
            output.delete();
        Map<String, byte[]> fileMap = new HashMap<>();
        Map<String, ClassNode> classMap = new HashMap<>();
        
        System.out.println(PREFIX + "Loading classes...");

        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                final byte[] bytes = IOUtils.toByteArray(inputStream);
                if (!jarEntry.getName().endsWith(".class")) {
                    fileMap.put(jarEntry.getName(), bytes);
                    continue;
                }

                final ClassNode classNode = new ClassNode();
                try {
                    final ClassReader classReader = new ClassReader(bytes);
                    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
                    classMap.put(classNode.name, classNode);
                } catch (Exception exception) {
                    fileMap.put(jarEntry.getName(), bytes);
                }
            }
        }
        System.out.println(PREFIX + "Loaded classes");
        handle(classMap);
        if (initialHandlerRewrite != null) {
            ClassNode classNode = new ClassNode();
            new ClassReader(initialHandlerRewrite).accept(classNode, 0);
            classMap.put("net/md_5/bungee/connection/InitialHandler", classNode);
            System.out.println(PREFIX + "Successfully replaced old InitialHandler class");
        }
        if (encryptionResponseRewrite != null) {
            ClassNode classNode = new ClassNode();
            new ClassReader(encryptionResponseRewrite).accept(classNode, 0);
            classMap.put("net/md_5/bungee/protocol/packet/EncryptionResponse", classNode);
            System.out.println(PREFIX + "Successfully replaced old EncryptionResponse class");
        }
        System.out.println(PREFIX + String.format("Writing to %s...", output.getName()));
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(output))) {
            for (ClassNode classNode : classMap.values().stream().distinct().collect(Collectors.toList())) {
                final JarEntry jarEntry = new JarEntry(classNode.name + ".class");
                final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

                try {
                    jarOutputStream.putNextEntry(jarEntry);
                } catch (Exception e) {
                    continue;
                }
                classNode.accept(classWriter);
                jarOutputStream.write(classWriter.toByteArray());
                jarOutputStream.closeEntry();
            }
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
                jarOutputStream.write(entry.getValue());
                jarOutputStream.closeEntry();
            }
        }
        System.out.println(PREFIX + "Done, took " + (System.currentTimeMillis() - start) + "ms"); // todo increment max array size
    }

    public static void handle(Map<String, ClassNode> classMap) {
        classMap.values().stream()
                .filter(classNode -> classNode.methods.size() > 0)
                .distinct()
                .filter(classNode -> classNode.name.contains("InitialHandler") || classNode.name.contains("EncryptionResponse"))
                .forEach(classNode -> {
                    ClassWriter classWriter = new ClassWriter(0);
                    if (classNode.name.endsWith("InitialHandler")) {
                        System.out.println(PREFIX + "Found InitialHandler class");
                        classNode.accept(new InitialHandlerVisitor(classWriter));
                        initialHandlerRewrite = classWriter.toByteArray();
                        return;
                    }
                    if (!classNode.name.contains("Encryption")) {
                        return;
                    }
                    System.out.println(PREFIX + "Found EncryptionResponse class");
                    classNode.accept(new EncryptionResponseVisitor(classWriter));
                    encryptionResponseRewrite = classWriter.toByteArray();
                    System.out.println(PREFIX + "Replaced numbers");
                });
    }
}
