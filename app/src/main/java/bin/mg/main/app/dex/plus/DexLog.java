package bin.mg.main.app.dex.plus;

import android.util.Log;

public class DexLog {

    public static final String TAG = "ANTIK";

    public static void log(Object args) {

        Log.i(TAG, String.valueOf(args));

    }
}
