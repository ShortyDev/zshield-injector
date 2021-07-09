package at.shorty.zshield.injector.visitors;

import org.objectweb.asm.*;

import static at.shorty.zshield.injector.Injector.PREFIX;

public class EncryptionResponseVisitor extends ClassVisitor {

    public EncryptionResponseVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter);
        System.out.println(PREFIX + "Preparing number replacements to increment special packet limits...");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (!name.equals("hashCode")) {
            if (name.equals("expectedMinLength")) {
                return new MinReplacer(
                        super.visitMethod(access, name, desc, signature, exceptions), (Type.getArgumentsAndReturnSizes(desc) >> 2) - 1);
            } else {
                return new IntReplacer(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions));
            }
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    static class IntReplacer extends MethodVisitor {

        public IntReplacer(int i, MethodVisitor methodVisitor) {
            super(i, methodVisitor);
        }

        @Override
        public void visitIntInsn(int i, int i1) {
            super.visitIntInsn(i, 2048);
        }
    }

    static class MinReplacer extends MethodVisitor {
        private final MethodVisitor targetWriter;
        private final int newMaxLocals;

        MinReplacer(MethodVisitor writer, int newMaxL) {
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
            targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 1);
            targetWriter.visitVarInsn(Opcodes.ALOAD, 2);
            targetWriter.visitVarInsn(Opcodes.ILOAD, 3);
            targetWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/md_5/bungee/protocol/packet/EncryptionResponse", "expectedMaxLength", "(Lio/netty/buffer/ByteBuf;Lnet/md_5/bungee/protocol/ProtocolConstants$Direction;I)I", false);
            targetWriter.visitIntInsn(Opcodes.SIPUSH, 2000);
            targetWriter.visitInsn(Opcodes.ISUB);
            targetWriter.visitInsn(Opcodes.IRETURN);
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
    }
}
