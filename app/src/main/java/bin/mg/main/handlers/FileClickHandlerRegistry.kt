package bin.mg.main.handlers

import bin.mg.main.model.FileItem
import bin.mg.main.model.FileType

class FileClickHandlerRegistry private constructor() {

    private val handlers = HashMap<String, FileClickHandler>()
    private var defaultHandler: FileClickHandler? = null

    fun registerHandler(extension: String, handler: FileClickHandler) {
        handlers[extension.lowercase()] = handler
    }

    fun registerHandler(type: FileType, handler: FileClickHandler) {
        handlers[type.name.lowercase()] = handler
    }

    fun getHandler(item: FileItem?): FileClickHandler? {
        if (item == null) return defaultHandler

        val handler = handlers[item.getExtension().lowercase()]
        if (handler != null) return handler

        val type = item.fileType
        val typeHandler = handlers[type.name.lowercase()]
        if (typeHandler != null) return typeHandler

        return defaultHandler
    }

    fun getHandler(item: FileItem, fallback: FileClickHandler): FileClickHandler {
        return getHandler(item) ?: fallback
    }

    fun setDefaultHandler(handler: FileClickHandler) {
        defaultHandler = handler
    }

    fun clearHandlers() {
        handlers.clear()
    }

    companion object {
        @JvmStatic
        val instance: FileClickHandlerRegistry by lazy { FileClickHandlerRegistry() }
    }
}
