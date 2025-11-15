package Triangle; 

import java.io.*;

public class LLVMPathFinder {

    /**
     * Busca un ejecutable (llc.exe, clang.exe) en el PATH del sistema.
     * @param name nombre del exe, ej: "llc.exe"
     * @return ruta completa o null si no lo encuentra
     */
    public static String find(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;

        String[] paths = pathEnv.split(";");
        for (String dir : paths) {
            File exe = new File(dir, name);
            if (exe.exists() && exe.isFile()) {
                return exe.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Devuelve la ruta de llc.exe o lanza excepción si no está en PATH
     */
    public static String getLLC() throws Exception {
        String llc = find("llc.exe");
        if (llc == null) throw new Exception("llc.exe no encontrado en PATH. Por favor agregue LLVM al PATH o configure la ruta manualmente.");
        return llc;
    }

    /**
     * Devuelve la ruta de clang.exe o lanza excepción si no está en PATH
     */
    public static String getClang() throws Exception {
        String clang = find("clang.exe");
        if (clang == null) throw new Exception("clang.exe no encontrado en PATH. Por favor agregue LLVM al PATH o configure la ruta manualmente.");
        return clang;
    }
}