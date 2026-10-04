package tw.junba.transcriber;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * R4.1 process gate. This class intentionally has no references to Whisper,
 * Gemini, SecureKeyStore or MediaCodec. If this page opens, the secondary
 * Android process itself is healthy. The heavy Activity is loaded only after
 * the user presses the next-step button.
 */
public final class TranscriberActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DiagnosticApplication.writeStage(this, "GATE:onCreate entered");
        try {
            setContentView(buildUi());
            DiagnosticApplication.writeStage(this, "GATE:READY");
        } catch (Throwable t) {
            DiagnosticApplication.writeCrashReport(this, t, "TranscriberActivity gate onCreate");
            throw t;
        }
    }

    private ScrollView buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("R4.1｜第二層程序已正常啟動", 22, true), match());
        TextView info = text(
                "這一頁仍未載入 Whisper、Gemini、API Key 或音訊解碼器。\n\n" +
                "看到這一頁代表 :transcriber 獨立程序也正常。下一步才會啟動真正完整轉錄 Activity。",
                14, false);
        info.setPadding(0, dp(8), 0, dp(12));
        root.addView(info, match());

        status = text("第二層診斷：READY", 17, true);
        status.setTextColor(0xFF0B7A34);
        root.addView(status, match());

        Button next = button("下一步：載入完整轉錄功能");
        next.setOnClickListener(v -> openFull());
        root.addView(next, match());

        Button home = button("返回 R4.1 診斷首頁");
        home.setOnClickListener(v -> finish());
        root.addView(home, match());

        TextView hint = text(
                "若按『下一步』後閃退：回到 R4.1 首頁，按『重新讀取診斷』。" +
                "首頁會直接讀取實體 crash/stage 檔，不再依賴跨程序 SharedPreferences。",
                13, false);
        hint.setPadding(0, dp(12), 0, 0);
        root.addView(hint, match());
        return scroll;
    }

    private void openFull() {
        DiagnosticApplication.writeStage(this, "FULL_LAUNCH_REQUESTED");
        try {
            Intent intent = new Intent();
            intent.setClassName(getPackageName(), "tw.junba.transcriber.FullTranscriberActivity");
            startActivity(intent);
            status.setText("已送出完整功能啟動要求；若消失請回首頁看診斷");
        } catch (Throwable t) {
            DiagnosticApplication.writeCrashReport(this, t, "Gate start FullTranscriberActivity");
            status.setText("啟動完整功能失敗：" + t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
            status.setTextColor(0xFFB71C1C);
            Toast.makeText(this, "已記錄錯誤，請回診斷首頁", Toast.LENGTH_LONG).show();
        }
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
}
