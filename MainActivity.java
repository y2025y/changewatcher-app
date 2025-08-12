
package com.example.changewatcherapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button startButton, stopButton;
    private Handler handler = new Handler();
    private Runnable checkTask;
    private boolean isMonitoring = false;
    private List<String> lastIds = new ArrayList<>();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        startButton.setOnClickListener(v -> startMonitoring());
        stopButton.setOnClickListener(v -> stopMonitoring());
    }

    private void startMonitoring() {
        String url = urlInput.getText().toString();
        if (url.isEmpty()) {
            Toast.makeText(this, "URL을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        isMonitoring = true;
        webView.loadUrl(url);
        checkTask = new Runnable() {
            @Override
            public void run() {
                if (isMonitoring) {
                    webView.loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
                    handler.postDelayed(this, 30000); // 30초 주기
                }
            }
        };
        handler.post(checkTask);
    }

    private void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacks(checkTask);
        Toast.makeText(this, "감시 중단", Toast.LENGTH_SHORT).show();
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        public void processHTML(String html) {
            List<String> currentIds = extractIds(html);
            if (lastIds.isEmpty()) {
                lastIds = new ArrayList<>(currentIds);
                showAlert("감시 시작 - 기존 10개 알림");
            } else if (!lastIds.equals(currentIds)) {
                lastIds = new ArrayList<>(currentIds);
                showAlert("변화 감지");
            }
        }
    }

    private List<String> extractIds(String html) {
        List<String> ids = new ArrayList<>();
        String marker = "showSideView(this";
        int idx = html.indexOf(marker);
        while (idx != -1 && ids.size() < 10) {
            int start = html.indexOf("'", idx + marker.length());
            int end = html.indexOf("'", start + 1);
            if (start != -1 && end != -1) {
                String id = html.substring(start + 1, end);
                ids.add(id);
            }
            idx = html.indexOf(marker, end);
        }
        return ids;
    }

    private void showAlert(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}
