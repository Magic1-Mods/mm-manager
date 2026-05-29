package bin.mg.main.handlers;

import android.content.Context;
import android.view.View;
import bin.mg.main.model.FileItem;
import bin.mg.main.model.FileType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileClickHandlerRegistry {
    
    private static final FileClickHandlerRegistry INSTANCE = new FileClickHandlerRegistry();
    private final Map<String, FileClickHandler> handlers = new HashMap<>();
    private FileClickHandler defaultHandler;
    
    private FileClickHandlerRegistry() {}
    
    public static FileClickHandlerRegistry getInstance() {
        return INSTANCE;
    }
    
    public void registerHandler(String extension, FileClickHandler handler) {
        handlers.put(extension.toLowerCase(), handler);
    }
    
    public void registerHandler(FileType type, FileClickHandler handler) {
        handlers.put(type.name().toLowerCase(), handler);
    }
    
    public FileClickHandler getHandler(FileItem item) {
        if (item == null) return defaultHandler;
        
        FileClickHandler handler = handlers.get(item.getExtension().toLowerCase());
        if (handler != null) return handler;
        
        FileType type = item.getFileType();
        handler = handlers.get(type.name().toLowerCase());
        if (handler != null) return handler;
        
        return defaultHandler;
    }
    
    public FileClickHandler getHandler(FileItem item, FileClickHandler fallback) {
        FileClickHandler handler = getHandler(item);
        return handler != null ? handler : fallback;
    }
    
    public void setDefaultHandler(FileClickHandler handler) {
        this.defaultHandler = handler;
    }
    
    public void clearHandlers() {
        handlers.clear();
    }
}
