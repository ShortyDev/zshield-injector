package at.shorty.zshield.injector.visitors;

import org.objectweb.asm.*;

import static at.shorty.zshield.injector.Injector.PREFIX;

public class InitialHandlerVisitor extends ClassVisitor {

    public InitialHandlerVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter);
        System.out.println(PREFIX + "Preparing injection...");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("handle") && desc.contains("EncryptionResponse")) {
            System.out.println(PREFIX + "Injected new method");
            return new ReplaceEncryptionResponseMethod(
                    super.visitMethod(access, name, desc, signature, exceptions),
                    (Type.getArgumentsAndReturnSizes(desc) >> 2) - 1);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    static class ReplaceEncryptionResponseMethod extends MethodVisitor {
        private final MethodVisitor targetWriter;
        private final int newMaxLocals;

        ReplaceEncryptionResponseMethod(MethodVisitor writer, int newMaxL) {
            super(Opcodes.ASM5);
            targetWriter = writer;
            newMaxLocals = newMaxL;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetWriter.visitMaxs(0, newMaxLocals);
        }

        @Override
        public void visitCode() {
            targetWriter.visitAnnotation("Override", true);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitMethodInsn(Opcodes.INVOKESTATIC, "net/md_5/bungee/BungeeCord", "getInstance", "()Lnet/md_5/bungee/BungeeCord;", false);
            targetWriter.visitFieldInsn(Opcodes.GETFIELD, "net/md_5/bungee/BungeeCord", "gson", "Lcom/google/gson/Gson;");
            targetWriter.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            targetWriter.visitInsn(Opcodes.DUP);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 1);
            targetWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/md_5/bungee/protocol/packet/EncryptionResponse", "getSharedSecret", "()[B", false);
            targetWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
            targetWriter.visitLdcInsn(Type.getObjectType("net/md_5/bungee/connection/LoginResult"));
            targetWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/google/gson/Gson", "fromJson", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", false);
            targetWriter.visitTypeInsn(Opcodes.CHECKCAST, "net/md_5/bungee/connection/LoginResult");
            targetWriter.visitFieldInsn(Opcodes.PUTFIELD, "net/md_5/bungee/connection/InitialHandler", "loginProfile", "Lnet/md_5/bungee/connection/LoginResult;");
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitFieldInsn(Opcodes.GETFIELD, "net/md_5/bungee/connection/InitialHandler", "loginProfile", "Lnet/md_5/bungee/connection/LoginResult;");
            targetWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/md_5/bungee/connection/LoginResult", "getName", "()Ljava/lang/String;", false);
            targetWriter.visitMethodInsn(Opcodes.PUTFIELD, "net/md_5/bungee/connection/InitialHandler", "name", "Ljava/lang/String;", false);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitFieldInsn(Opcodes.GETFIELD, "net/md_5/bungee/connection/InitialHandler", "loginProfile", "Lnet/md_5/bungee/connection/LoginResult;");
            targetWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/md_5/bungee/connection/LoginResult", "getId", "()Ljava/lang/String;", false);
            targetWriter.visitMethodInsn(Opcodes.INVOKESTATIC, "net/md_5/bungee/Util", "getUUID", "(Ljava/lang/String;)Ljava/util/UUID;", false);
            targetWriter.visitFieldInsn(Opcodes.PUTFIELD, "net/md_5/bungee/connection/InitialHandler", "uniqueId", "Ljava/util/UUID;");
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/md_5/bungee/connection/InitialHandler", "finish", "()V", false);
            targetWriter.visitInsn(Opcodes.RETURN);
            visitEnd();
        }

        @Override
        public void visitLabel(Label label) {
            targetWriter.visitLabel(label);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetWriter.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetWriter.visitParameter("encryptionResponse", access);
        }
    }
}
