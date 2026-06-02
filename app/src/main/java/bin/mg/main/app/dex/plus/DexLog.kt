package bin.mg.main.app.dex.plus

import android.util.Log

object DexLog {
    const val TAG = "MM-DEXEDITOR-PLUS"

    @JvmStatic
    fun log(args: Any?) {
        Log.i(TAG, args.toString())
    }
}