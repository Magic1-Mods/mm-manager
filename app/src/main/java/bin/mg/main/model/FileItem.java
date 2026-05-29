package bin.mg.main.model;

import java.io.File;

public class FileItem {
    public static final String PARENT_DIRECTORY = "...";
    
    private String name;
    private String path;
    private long size;
    private boolean isDirectory;
    private long lastModified;
    private FileType fileType;
    private boolean isVirtual;
    
    public FileItem(String name, String path, long size, boolean isDirectory, long lastModified) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.fileType = isDirectory ? FileType.FOLDER : FileType.fromFileName(name);
        this.isVirtual = false;
    }
    
    public static FileItem fromFile(File file) {
        return new FileItem(
            file.getName(),
            file.getAbsolutePath(),
            file.length(),
            file.isDirectory(),
            file.lastModified()
        );
    }
    
    public static FileItem createParentItem(String parentPath) {
        return new FileItem(PARENT_DIRECTORY, parentPath, 0, true, 0);
    }
    
    public static FileItem createVirtualParent() {
        FileItem item = new FileItem(PARENT_DIRECTORY, "", 0, true, 0);
        item.isVirtual = true;
        return item;
    }
    
    public boolean isParentDirectory() {
        return name.equals(PARENT_DIRECTORY);
    }
    
    public boolean isVirtual() {
        return isVirtual;
    }
    
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getLastModified() {
        return lastModified;
    }
    
    public FileType getFileType() {
        return fileType;
    }
    
    public String getExtension() {
        if (name == null || name.isEmpty()) return "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= name.length() - 1) return "";
        return name.substring(lastDot + 1);
    }
    
    public String getExtensionWithCompound() {
        if (name == null || name.isEmpty()) return "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= name.length() - 1) return "";
        
        String[] parts = name.split("\\.");
        if (parts.length <= 1) return "";
        
        StringBuilder ext = new StringBuilder(parts[parts.length - 1]);
        for (int i = parts.length - 2; i >= 1; i--) {
            String compound = parts[i] + "." + ext.toString();
            if (FileType.fromExtension(compound) != FileType.UNKNOWN) {
                return compound;
            }
            ext.insert(0, ".").insert(0, parts[i]);
        }
        
        return parts[parts.length - 1];
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setIsDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }
}
