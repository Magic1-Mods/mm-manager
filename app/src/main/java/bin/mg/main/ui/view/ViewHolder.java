package bin.mg.main.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import bin.mg.main.model.FileItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import bin.mg.main.R;
import bin.mg.main.model.FileType;
import bin.mg.main.utils.file.FileSystemHelper;
import bin.mg.main.utils.icon.IconManager;

public class ViewHolder extends RecyclerView.ViewHolder {

    public FrameLayout iconContainer;
    public ImageView icon;
    public TextView name, size, date;
    private int themeColor;
    private boolean isDarkMode;
    private static final int DEFAULT_FOLDER_COLOR = 0xFF212121;
    
    public void setThemeColor(int color) {
        this.themeColor = color;
    }
    
    private FileSystemHelper fileHelper;
    private Handler mainHandler;
    private Context context;
    private SimpleDateFormat dateFormat;
    private boolean isImageThumbnailsEnabled;

    public ViewHolder(View itemView,
                      Context context,
                      FileSystemHelper fileHelper,
                      Handler handler,
                      SimpleDateFormat format,
                      boolean thumbnailsEnabled, int themeColor, boolean isDarkMode) {

        super(itemView);

        this.context = context;
        this.fileHelper = fileHelper;
        this.mainHandler = handler;
        this.dateFormat = format;
        this.isImageThumbnailsEnabled = thumbnailsEnabled;
        this.themeColor = themeColor;
        this.isDarkMode = isDarkMode;
        
        iconContainer = itemView.findViewById(R.id.file_icon_container);
        icon = itemView.findViewById(R.id.file_icon);
        name = itemView.findViewById(R.id.file_name);
        size = itemView.findViewById(R.id.file_size);
        date = itemView.findViewById(R.id.file_date);
    }

    public void bind(FileItem item, boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
        iconContainer.setBackground(null);
        icon.setVisibility(View.VISIBLE);
        icon.setColorFilter(null);
        name.setText(item.getName());

        int textPrimary = isDarkMode ? 0xFFFFFFFF : 0xFF212121;
        int textSecondary = isDarkMode ? 0xFFB0B0B0 : 0xFF757575;
        name.setTextColor(textPrimary);
        size.setTextColor(textSecondary);
        date.setTextColor(textSecondary);

        FileType fileType = item.getFileType();

        if (item.isParentDirectory()) {
            setFolderUI(fileType, true);
        } else if (item.isDirectory()) {
            setFolderUI(FileType.FOLDER, false);
            date.setText(formatDate(item.getLastModified()));
        } else {
            bindFile(item, fileType);
        }
    }

    private void setFolderUI(FileType type, boolean isParent) {
        int bgColor = (themeColor != 0) ? themeColor : DEFAULT_FOLDER_COLOR;
        
        GradientDrawable bg = (GradientDrawable)
                ContextCompat.getDrawable(context, R.drawable.bg_file).mutate();

        bg.setColor(bgColor);

        iconContainer.setBackground(bg);
        icon.setImageResource(R.drawable.ic_folder);
        size.setText("Folder");

        if (isParent) date.setText("");
    }

    private void bindFile(FileItem item, FileType fileType) {
        boolean fullIcon = (fileType == FileType.APK) ||
                (fileType == FileType.IMAGE && isImageThumbnailsEnabled);

        if (fullIcon) {
            icon.setVisibility(View.GONE);
        } else {
            icon.setVisibility(View.VISIBLE);

            GradientDrawable bg = (GradientDrawable)
                    ContextCompat.getDrawable(context, R.drawable.bg_file).mutate();

            bg.setColor(ContextCompat.getColor(context, IconManager.getIconTint(fileType)));

            iconContainer.setBackground(bg);
            icon.setImageResource(IconManager.getIconResource(fileType));
        }

        size.setText(formatSize(item.getSize()));
        date.setText(formatDate(item.getLastModified()));

        loadThumbnail(item, fileType);
    }

    private void loadThumbnail(FileItem item, FileType type) {
        if (type == FileType.APK) {
            loadApkIcon(item.getPath());
        } else if (type == FileType.IMAGE && isImageThumbnailsEnabled) {
            loadImageThumbnail(item.getPath());
        }
    }

    private void loadApkIcon(String path) {
        fileHelper.execute(() -> {
            Bitmap bitmap = fileHelper.getApkIcon(context, path);

            if (bitmap != null) {
                mainHandler.post(() -> {
                    Glide.with(context)
                            .load(bitmap)
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable res, Transition<? super Drawable> t) {
                                    iconContainer.setBackground(res);
                                }

                                @Override
                                public void onLoadCleared(Drawable placeholder) {
                                    iconContainer.setBackground(null);
                                }
                            });
                });
            }
        });
    }

    private void loadImageThumbnail(String path) {
        File file = new File(path);
        if (!file.exists()) return;
        Glide.with(context)
        .load(file)
        .override(96, 96)
        .centerCrop()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.ic_file)
        .error(R.drawable.ic_file)
        .into(icon);
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatDate(long ts) {
        return ts <= 0 ? "" : dateFormat.format(new Date(ts));
    }
}
