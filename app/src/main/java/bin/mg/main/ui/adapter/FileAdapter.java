package bin.mg.main.ui.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import bin.mg.main.R;
import bin.mg.main.handlers.FileClickHandler;
import bin.mg.main.handlers.FileClickHandlerRegistry;
import bin.mg.main.model.FileItem;
import bin.mg.main.ui.view.ViewHolder;
import bin.mg.main.utils.file.FileSystemHelper;

public class FileAdapter extends RecyclerView.Adapter<ViewHolder> {

    private List<FileItem> items = new ArrayList<>();
    private Context context;
    private SimpleDateFormat dateFormat;
    private Handler mainHandler;
    private FileSystemHelper fileHelper;
    private FileClickHandlerRegistry handlerRegistry;
    private int themeColor;
    private boolean isDarkMode;
    
    private OnItemClickListener clickListener;
    private boolean isImageThumbnailsEnabled = true;

    public interface OnItemClickListener {
        void onItemClick(View view, int position, FileItem item);
    }

    public FileAdapter(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.fileHelper = FileSystemHelper.getInstance(context);
        this.handlerRegistry = FileClickHandlerRegistry.getInstance();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setImageThumbnailsEnabled(boolean enabled) {
        this.isImageThumbnailsEnabled = enabled;
    }

    public void setHandlerRegistry(FileClickHandlerRegistry registry) {
        this.handlerRegistry = registry;
    }

    public void setThemeColor(int color, boolean isDarkMode) {
        this.themeColor = color;
        this.isDarkMode = isDarkMode;
        notifyDataSetChanged();
    }

    public int getThemeColor() {
        return themeColor;
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);

        return new ViewHolder(view, context, fileHelper, mainHandler, dateFormat, isImageThumbnailsEnabled, themeColor, isDarkMode);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setThemeColor(themeColor);
        FileItem item = items.get(position);
        holder.bind(item, isDarkMode);

        holder.itemView.setOnClickListener(v -> {
            FileClickHandler handler = handlerRegistry.getHandler(item);

            if (handler != null) {
                handler.onClick(context, item, v);
            } else if (clickListener != null) {
                clickListener.onItemClick(v, position, item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            FileClickHandler handler = handlerRegistry.getHandler(item);

            if (handler != null) {
                handler.onLongClick(context, item, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<FileItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }
    
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        try {
            Glide.with(holder.itemView.getContext()).clear(holder.icon);
            holder.icon.setImageDrawable(null);
            holder.iconContainer.setBackground(null);
        } catch (Exception ignored) {}
    }

    public List<FileItem> getItems() {
        return new ArrayList<>(items);
    }
}