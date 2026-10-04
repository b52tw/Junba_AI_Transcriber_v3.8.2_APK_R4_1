package tw.junba.transcriber;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Minimal R4.1 launcher. It reads physical diagnostic files first so crashes
 * from another Android process are visible immediately.
 */
public final class MainActivity extends Activity {
    private EditText crashBox;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DiagnosticApplication.writeStage(this, "LAUNCHER:onCreate");
        setContentView(buildUi());
        refreshDiagnostics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDiagnostics();
    }

    private ScrollView buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("峻爸 AI Transcriber v3.8.2｜R4.1 Diagnostic Launcher", 21, true), match());

        TextView intro = text(
                "R4.1 會分三層測試：①安全首頁 → ②獨立程序 Gate → ③完整轉錄功能。\n\n" +
                "診斷資料改用實體檔案跨程序讀取，不再只靠 SharedPreferences。", 14, false);
        intro.setPadding(0, dp(8), 0, dp(12));
        root.addView(intro, match());

        status = text("R4.1 安全啟動層：正常", 17, true);
        status.setTextColor(0xFF0B7A34);
        root.addView(status, match());

        TextView device = text(
                "裝置：" + Build.MANUFACTURER + " " + Build.MODEL +
                "\nAndroid：" + Build.VERSION.RELEASE + "（SDK " + Build.VERSION.SDK_INT + ")" +
                "\nABI：" + String.join(", ", Build.SUPPORTED_ABIS),
                13, false);
        device.setPadding(0, dp(6), 0, dp(12));
        root.addView(device, match());

        Button open = button("步驟 2：開啟獨立程序 Gate");
        open.setOnClickListener(v -> openGate());
        root.addView(open, match());

        Button refresh = button("重新讀取診斷");
        refresh.setOnClickListener(v -> refreshDiagnostics());
        root.addView(refresh, match());

        Button copy = button("複製診斷內容");
        copy.setOnClickListener(v -> copyReport());
        root.addView(copy, match());

        Button clear = button("清除診斷紀錄");
        clear.setOnClickListener(v -> clearReport());
        root.addView(clear, match());

        TextView label = text("最近診斷內容（stage + crash）", 16, true);
        label.setPadding(0, dp(14), 0, dp(4));
        root.addView(label, match());

        crashBox = new EditText(this);
        crashBox.setGravity(Gravity.TOP | Gravity.START);
        crashBox.setTextSize(12);
        crashBox.setMinLines(18);
        crashBox.setHorizontallyScrolling(false);
        crashBox.setVerticalScrollBarEnabled(true);
        crashBox.setTextIsSelectable(true);
        crashBox.setFocusable(false);
        crashBox.setHint("目前沒有診斷內容。\n\n先按『步驟 2』。若 Gate 正常，再在 Gate 頁按『下一步：載入完整轉錄功能』。若閃退，回此頁按『重新讀取診斷』。");
        root.addView(crashBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(470)));

        TextView footer = text(
                "判讀：\n" +
                "A. Gate 都打不開 → 第二程序啟動層問題。\n" +
                "B. Gate 可開、完整功能一按就消失 → FullTranscriberActivity/相依載入問題。\n" +
                "C. 完整功能可開 → 再依序測 Gemini、Whisper、存檔。", 13, false);
        footer.setPadding(0, dp(12), 0, 0);
        root.addView(footer, match());
        return scroll;
    }

    private void openGate() {
        DiagnosticApplication.writeStage(this, "LAUNCHER:gate launch requested");
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), "tw.junba.transcriber.TranscriberActivity");
            startActivity(intent);
        } catch (Throwable t) {
            DiagnosticApplication.writeCrashReport(this, t, "Launcher start TranscriberActivity gate");
            refreshDiagnostics();
        }
    }

    private void refreshDiagnostics() {
        if (crashBox == null || status == null) return;
        String stage = DiagnosticApplication.readDiagnosticFile(this, DiagnosticApplication.STAGE_FILE);
        String crash = DiagnosticApplication.readDiagnosticFile(this, DiagnosticApplication.CRASH_FILE);

        if (crash.isEmpty()) {
            SharedPreferences p = getSharedPreferences(DiagnosticApplication.PREFS, MODE_PRIVATE);
            crash = p.getString(DiagnosticApplication.KEY_LAST_CRASH, "");
            if (crash == null) crash = "";
        }

        StringBuilder out = new StringBuilder();
        if (!stage.isEmpty()) out.append("=== LAST STAGE ===\n").append(stage).append("\n\n");
        if (!crash.isEmpty()) out.append("=== LAST CRASH ===\n").append(crash).append('\n');

        if (out.length() == 0) {
            crashBox.setText("");
            status.setText("R4.1 安全啟動層：正常｜目前沒有 crash/stage 記錄");
            status.setTextColor(0xFF0B7A34);
        } else {
            crashBox.setText(out.toString().trim());
            crashBox.setSelection(0);
            if (!crash.isEmpty()) {
                status.setText("R4.1 已讀到完整功能錯誤紀錄");
                status.setTextColor(0xFFB71C1C);
            } else {
                status.setText("R4.1 已讀到最近執行階段（尚無 Java crash stack）");
                status.setTextColor(0xFF1565C0);
            }
        }
    }

    private void copyReport() {
        String s = crashBox == null ? "" : crashBox.getText().toString();
        if (s.trim().isEmpty()) {
            toast("目前沒有診斷內容可複製");
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Junba R4.1 diagnostic", s));
            toast("診斷內容已複製");
        }
    }

    private void clearReport() {
        DiagnosticApplication.clearDiagnostics(this);
        DiagnosticApplication.writeStage(this, "LAUNCHER:diagnostics cleared");
        refreshDiagnostics();
        toast("已清除舊診斷並建立新的 stage 記錄");
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(0xFF17202A);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setMinHeight(dp(48));
        return b;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
