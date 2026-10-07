package com.limelight;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.log.StreamLogStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StreamLogFilesActivity extends BaseActivity {
    private static final int REQUEST_EXPORT_LOG = 7001;

    private LinearLayout logList;
    private TextView emptyView;
    private File pendingExportFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_logs);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        logList = findViewById(R.id.stream_log_list);
        emptyView = findViewById(R.id.stream_log_empty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        logList.removeAllViews();
        List<File> files = StreamLogStore.list(this);
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
        for (File file : files) {
            logList.addView(createLogRow(file));
        }
    }

    private View createLogRow(File file) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(13), dp(16), dp(13));
        row.setBackgroundResource(R.drawable.bg_update_dialog_card_selector);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        row.setLayoutParams(params);
        row.setFocusable(true);
        row.setClickable(true);

        TextView title = new TextView(this);
        title.setText(file.getName());
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setSingleLine(true);
        row.addView(title);

        TextView detail = new TextView(this);
        detail.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(file.lastModified())) + "  ·  " + Formatter.formatShortFileSize(this, file.length()));
        detail.setTextColor(0xB3FFFFFF);
        detail.setTextSize(11);
        detail.setPadding(0, dp(5), 0, 0);
        row.addView(detail);

        row.setOnClickListener(v -> showActions(file));
        return row;
    }

    private void showActions(File file) {
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(new String[]{getString(R.string.axi_ui_ver), getString(R.string.axi_ui_exportar), getString(R.string.axi_ui_eliminar)}, (dialog, which) -> {
                    if (which == 0) {
                        showPreview(file);
                    }
                    else if (which == 1) {
                        exportFile(file);
                    }
                    else {
                        confirmDelete(file);
                    }
                })
                .show();
    }

    private void showPreview(File file) {
        String content = StreamLogStore.readPreview(this, file, 24000);
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage(content.isEmpty() ? getString(R.string.axi_ui_el_registro_esta_vacio) : content)
                .setPositiveButton(getString(R.string.settings_panel_close), null)
                .setNeutralButton(getString(R.string.axi_ui_exportar), (dialog, which) -> exportFile(file))
                .show();
    }

    private void exportFile(File file) {
        pendingExportFile = file;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(StreamLogStore.hasExitTrace(this, file) ? "application/zip" : "text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, StreamLogStore.getSuggestedExportName(this, file));
        startActivityForResult(intent, REQUEST_EXPORT_LOG);
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.axi_ui_eliminar_registro))
                .setMessage(getString(R.string.axi_ui_eliminar_este_registro_de_streaming))
                .setNegativeButton(getString(R.string.game_menu_cancel), null)
                .setPositiveButton(getString(R.string.axi_ui_eliminar), (dialog, which) -> {
                    if (StreamLogStore.delete(this, file)) {
                        refreshList();
                    }
                    else {
                        Toast.makeText(this, getString(R.string.axi_ui_no_se_pudo_eliminar_el_registro), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT_LOG) {
            return;
        }
        if (resultCode != RESULT_OK || data == null || pendingExportFile == null) {
            pendingExportFile = null;
            return;
        }
        Uri destination = data.getData();
        boolean exported = destination != null && StreamLogStore.export(this, pendingExportFile, destination);
        pendingExportFile = null;
        Toast.makeText(this, exported ? getString(R.string.axi_ui_registro_exportado) : getString(R.string.axi_ui_no_se_pudo_exportar_el_registro), Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
