package ux.justsadnyx.droiddeck;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FilesFragment extends Fragment {

    private File currentDir;
    private FileAdapter adapter;
    private TextView pathView;
    private final ActivityResultLauncher<Intent> allFilesAccess =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> refresh());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        RecyclerView list = v.findViewById(R.id.f_list);
        pathView = v.findViewById(R.id.f_path);
        ImageButton upBtn = v.findViewById(R.id.f_up);
        ExtendedFloatingActionButton newFolder = v.findViewById(R.id.f_new_folder);

        adapter = new FileAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        upBtn.setOnClickListener(btn -> {
            File parent = currentDir == null ? null : currentDir.getParentFile();
            navigate(parent == null ? Environment.getExternalStorageDirectory() : parent);
        });

        newFolder.setOnClickListener(btn -> promptNewFolder());

        if (!hasAllFilesAccess()) {
            requestAllFilesAccess();
            currentDir = requireContext().getExternalFilesDir(null);
            if (currentDir == null) currentDir = requireContext().getFilesDir();
        } else {
            currentDir = Environment.getExternalStorageDirectory();
        }
        navigate(currentDir);
    }

    private boolean hasAllFilesAccess() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName()));
                allFilesAccess.launch(intent);
                Toast.makeText(requireContext(), "Grant \"All files access\" to browse your storage", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                try {
                    allFilesAccess.launch(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Exception ignored) {}
            }
        }
    }

    private void promptNewFolder() {
        EditText input = new EditText(requireContext());
        input.setHint("Folder name");
        new AlertDialog.Builder(requireContext())
                .setTitle("New folder")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty() || name.contains("/")) return;
                    File dir = new File(currentDir, name);
                    boolean ok = dir.mkdirs();
                    Toast.makeText(requireContext(), ok ? "Created" : "Could not create folder", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigate(File dir) {
        if (dir == null || !dir.exists()) return;
        currentDir = dir;
        pathView.setText(dir.getAbsolutePath());
        List<File> items = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            items.addAll(fileList);
        }
        adapter.setItems(items);
    }

    private void refresh() {
        navigate(hasAllFilesAccess() && Environment.getExternalStorageDirectory().equals(currentDir)
                ? Environment.getExternalStorageDirectory()
                : currentDir);
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            String mime = mimeFor(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Open " + file.getName()));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app can open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeFor(file));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share " + file.getName()));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not share file", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + file.getName() + "?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    boolean ok = deleteRecursive(file);
                    Toast.makeText(requireContext(), ok ? "Deleted" : "Delete failed", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        return f.delete();
    }

    private String mimeFor(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        String ext = name.substring(dot + 1).toLowerCase(java.util.Locale.US);
        String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime == null ? "application/octet-stream" : mime;
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.Holder> {

        private final List<File> items = new ArrayList<>();

        void setItems(List<File> files) {
            items.clear();
            items.addAll(files);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            File file = items.get(position);
            h.name.setText(file.getName());
            if (file.isDirectory()) {
                File[] kids = file.listFiles();
                h.meta.setText((kids == null ? 0 : kids.length) + " items");
                h.icon.setImageResource(R.drawable.ic_folder);
            } else {
                h.meta.setText(Util.humanSize(file.length()) + " · "
                        + android.text.format.DateFormat.getDateFormat(requireContext()).format(new Date(file.lastModified())));
                h.icon.setImageResource(R.drawable.ic_file);
            }
            h.itemView.setOnClickListener(btn -> {
                if (file.isDirectory()) navigate(file);
                else openFile(file);
            });
            h.share.setOnClickListener(btn -> shareFile(file));
            h.delete.setOnClickListener(btn -> confirmDelete(file));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView meta;
            final ImageButton share;
            final ImageButton delete;

            Holder(View v) {
                super(v);
                icon = v.findViewById(R.id.if_icon);
                name = v.findViewById(R.id.if_name);
                meta = v.findViewById(R.id.if_meta);
                share = v.findViewById(R.id.if_share);
                delete = v.findViewById(R.id.if_delete);
            }
        }
    }
}
