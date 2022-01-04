package me.onils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class Agent {
    private static boolean isStaffModuleClass(ClassNode node) {
        for (MethodNode method : node.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) continue;

            if (!method.desc.equals("(L" + node.name + ";)V")) continue;

            boolean hasReloadChunks = Arrays.stream(method.instructions.toArray())
                    .filter(MethodInsnNode.class::isInstance)
                    .map(MethodInsnNode.class::cast)
                    .map(it -> it.name)
                    .anyMatch("bridge$reloadChunks"::equals);
            if (hasReloadChunks) return true;
        }
        return false;
    }

    public static void premain(String option, Instrumentation inst){
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if(classfileBuffer == null || classfileBuffer.length == 0)
                    return new byte[0];

                ClassReader cr = new ClassReader(classfileBuffer);
                ClassNode cn = new ClassNode();
                cr.accept(cn, ClassReader.SKIP_FRAMES);

                if (isStaffModuleClass(cn)) {
                    for(MethodNode method : cn.methods){
                        if(method.desc.equals("()Z")){
                            InsnList inject = new InsnList();
                            inject.add(new InsnNode(Opcodes.ICONST_1));
                            inject.add(new InsnNode(Opcodes.IRETURN));
                            method.instructions.insert(inject);

                            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                            cn.accept(cw);
                            return cw.toByteArray();
                        }
                    }
                }

                return classfileBuffer;
            }
        });
    }
}