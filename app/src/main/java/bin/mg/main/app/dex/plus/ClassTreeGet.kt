package bin.mg.main.app.dex.plus

import android.util.Log
import bin.mg.main.app.dex.plus.DexLog.TAG
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

object ClassTreeGet {

    @JvmStatic
    fun getAllClasses(dexPaths: Map<String, String>?): List<String> {
        if (dexPaths.isNullOrEmpty()) return emptyList()

        val classNames = Collections.synchronizedList(mutableListOf<String>())
        val threads = dexPaths.values.map { path ->
            thread {
                try {
                    val dexFile = File(path)
                    if (dexFile.exists()) {
                        val dex: DexFile = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault())
                        val tmp = dex.classes.map(ClassDef::getType)
                        classNames.addAll(tmp)
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "getAllClasses Error: ${e.message}")
                }
            }
        }

        threads.forEach { it.join() }
        return classNames.sorted()
    }
}