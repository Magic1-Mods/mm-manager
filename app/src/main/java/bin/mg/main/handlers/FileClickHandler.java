package bin.mg.main.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import bin.mg.main.model.FileItem;
import java.io.File;

public interface FileClickHandler {
    
    void onClick(Context context, FileItem item, View anchor);
    
    void onLongClick(Context context, FileItem item, View anchor);
}
