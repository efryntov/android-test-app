/*
Licensed under Creative Commons Zero (CC0).
https://creativecommons.org/publicdomain/zero/1.0/
*/

package ca.psiphon.tunneledwebview;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ca.psiphon.PsiphonTunnel;

//----------------------------------------------------------------------------------------------
// TunneledWebView
//
// This sample app demonstrates tunneling a WebView through the
// Psiphon Library. This app's main activity shows a log of
// events and a WebView that is loaded once Psiphon is connected.
//
// The flow is as follows:
//
// - The Psiphon tunnel is started in onResume(). PsiphonTunnel.start()
//   is an asynchronous call that returns immediately.
//
// - Once Psiphon has selected a local HTTP proxy listening port, the
//   onListeningHttpProxyPort() callback is called. This app records the
//   port to use for tunneling traffic.
//
// - Once Psiphon has established a tunnel, the onConnected() callback
//   is called. This app now loads the WebView, after setting its proxy
//   to point to Psiphon's local HTTP proxy.
//
// To adapt this sample into your own app:
//
// - Embed a Psiphon config file in app/src/main/res/raw/psiphon_config.
//
// - Add the Psiphon Library AAR module as a dependency (see this app's
//   project settings; to build this sample project, you need to drop
//   psi-0.0.10.aar into app/libs).
//
// - Use app/src/main/java/ca/psiphon/PsiphonTunnel.java, which provides
//   a higher-level wrapper around the Psiphon Library module. This file
//   shows how to use PsiphonTunnel and PsiphonTunnel.TunneledApp.
//
//----------------------------------------------------------------------------------------------

public class MainActivity extends AppCompatActivity
        implements PsiphonTunnel.HostService {

    private ListView mListView;
    private WebView mWebView;

    private ArrayAdapter<String> mLogMessages;
    private AtomicInteger mLocalHttpProxyPort;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((TunneledWebViewApplication)getApplication()).setPsiphonCallbackProxy(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TunneledWebViewApplication) getApplication()).setPsiphonCallbackProxy(this);

        setContentView(R.layout.activity_main);

        mListView = (ListView)findViewById(R.id.listView);
        mWebView = (WebView)findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        mLogMessages = new ArrayAdapter<>(
                this, R.layout.log_message, R.id.logMessageTextView);

        mListView.setAdapter(mLogMessages);

        mLocalHttpProxyPort = new AtomicInteger(0);

        Button recreateButton = findViewById(R.id.recreateButton);

        recreateButton.setOnClickListener(view -> {
            ((TunneledWebViewApplication) getApplication()).restartPsiphon();
            recreate();
        });
    }


    private void setHttpProxyPort(int port) {

        // NOTE: here we record the Psiphon proxy port for subsequent
        // use in tunneling app traffic. In this sample app, we will
        // use WebViewProxySettings.setLocalProxy to tunnel a WebView
        // through Psiphon. By default, the local proxy port is selected
        // dynamically, so it's important to record and use the correct
        // port number.

        mLocalHttpProxyPort.set(port);
    }

    private void loadWebView() {

        // NOTE: functions called via PsiphonTunnel.TunneledApp may be
        // called on background threads. It's important to ensure that
        // these threads are not blocked and that UI functions are not
        // called directly from these threads. Here we use runOnUiThread
        // to handle this.

        runOnUiThread(() -> {
            WebViewProxySettings.setLocalProxy(
                    MainActivity.this, mLocalHttpProxyPort.get());
            mWebView.loadUrl("https://freegeoip.app/");
        });
    }

    private void logMessage(final String message) {
        runOnUiThread(() -> {
            mLogMessages.add(message);
            mListView.setSelection(mLogMessages.getCount() - 1);
        });
    }

    @Override
    public String getAppName() {
        return null;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public String getPsiphonConfig() {
        return null;
    }

    @Override
    public void onDiagnosticMessage(String message) {
        android.util.Log.i(getString(R.string.app_name), message);
        logMessage(message);
    }

    @Override
    public void onAvailableEgressRegions(List<String> regions) {
        for (String region : regions) {
            logMessage("available egress region: " + region);
        }
    }

    @Override
    public void onListeningSocksProxyPort(int port) {
        logMessage("local SOCKS proxy listening on port: " + port);
    }

    @Override
    public void onListeningHttpProxyPort(int port) {
        logMessage("local HTTP proxy listening on port: " + port);
        setHttpProxyPort(port);
    }

    @Override
    public void onConnecting() {
        logMessage("connecting...");
    }

    @Override
    public void onConnected() {
        logMessage("connected");
        loadWebView();
    }
    @Override
    public void onBytesTransferred(long sent, long received) {
        logMessage("bytes sent: " + Long.toString(sent));
        logMessage("bytes received: " + Long.toString(received));
    }

    @Override
    public void onClientRegion(String region) {
        logMessage("client region: " + region);
    }
}
