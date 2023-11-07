package co.uk.mommyheather.finitegas.coremod;

import static org.objectweb.asm.Opcodes.ASM4;

import java.util.HashMap;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;


public class FiniteGasTransformer implements IClassTransformer {

    private static final HashMap<String, BiFunction<Integer, ClassVisitor, ClassVisitor>> TRANSFORMERS = new HashMap<>();

    static {
        TRANSFORMERS.put("net.bdew.generators.modules.gasInput.TileGasInput$gasHandler$", GasHandlerTransformer::new);
        TRANSFORMERS.put("net.bdew.generators.modules.gasInput.TileGasInput$gasHandler$$anonfun$receiveGas$1$$anonfun$apply$1", AppliedGasInputTransformer::new);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (name.startsWith("net.bdew.generators.modules.gasInput")) System.out.println(name);
        if (TRANSFORMERS.containsKey(name)) {
            System.out.println("Transforming class : " + name);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            reader.accept(TRANSFORMERS.get(name).apply(ASM4, writer), 0);

            return writer.toByteArray();
        }
        return bytes;
    }


    /*
     * Transformer - store the doTransfer boolean.
     * Easy way to carry this between different classes.
     */

     public static class GasHandlerTransformer extends ClassVisitor{

        public GasHandlerTransformer(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            System.out.println(name);
            if (!name.equals("receiveGas")) return super.visitMethod(access, name, desc, signature, exceptions);
            return new ReceiveGasMethodVisitor(ASM4, super.visitMethod(access, name, desc, signature, exceptions));
        }

        private static class ReceiveGasMethodVisitor extends MethodVisitor {

            private boolean written = false;

            public ReceiveGasMethodVisitor(int api, MethodVisitor mv) {
                super(api, mv);
                System.out.println("Transforming method receiveGas");

            }

            //We're safe to insert the extra instructions right after the fist line number in the target method.
            @Override
            public void visitLineNumber(int line, Label start) {
                super.visitLineNumber(line, start);
                if (!written) {
                    super.visitVarInsn(Opcodes.ILOAD, 3);

                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "co/uk/mommyheather/finitegas/util/ReceiveGasBoolStore",
                    "setBool", "(Z)V", false);

                    written = true;
                }
            }

        }
    }


    /*
     * Transformer - load the boolean stored using the above transformer, instead of a new truthy boolean.
     * This fixes the gas dupe.
     */

    public static class AppliedGasInputTransformer extends ClassVisitor{

        public AppliedGasInputTransformer(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            System.out.println(name);
            return new ReceiveGasMethodVisitor(ASM4, super.visitMethod(access, name, desc, signature, exceptions));
        }

        private static class ReceiveGasMethodVisitor extends MethodVisitor {

            private boolean visited = false;
            

            public ReceiveGasMethodVisitor(int api, MethodVisitor mv) {
                super(api, mv);
                System.out.println("Transforming method apply");
            }

            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.ICONST_1) {
                    System.out.println("BOOLEAN TRUE");
                    if (!visited) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "co/uk/mommyheather/finitegas/util/ReceiveGasBoolStore",
                        "getBool", "()Z", false);
                        visited = true;
                        return;
                    }
                }
                super.visitInsn(opcode);
            }
        }
    }

}
