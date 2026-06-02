package bin.mg.main.app

import android.graphics.drawable.Drawable
import bin.mg.main.model.FileItem

class ApkInfo {
    var packageName: String? = null
    var versionName: String? = null
    var versionCode: Long = 0
    var fileSize: Long = 0
    var isInstalled: Boolean = false
    var isSystemApp: Boolean = false
    var signatureV1: Boolean = false
    var signatureV2: Boolean = false
    var signatureV3: Boolean = false
    var targetSdk: Int = 0
    var minSdk: Int = 0
    var installedVersion: String? = null
    var dataDir: String? = null
    var sourceDir: String? = null
    var firstInstallTime: Long = 0
    var lastUpdateTime: Long = 0
    var uid: Int = 0
    var appIcon: Drawable? = null
    var appName: String? = null

    fun getPackageName(item: FileItem) {
        // Stub
    }
}