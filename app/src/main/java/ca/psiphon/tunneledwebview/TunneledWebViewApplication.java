package ca.psiphon.tunneledwebview;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.security.auth.login.LoginException;

import ca.psiphon.PsiphonTunnel;

public class TunneledWebViewApplication extends Application implements PsiphonTunnel.HostService {
    private static final String TAG = "TunneledWebView";
    private PsiphonTunnel psiphonTunnel;
    private PsiphonTunnel.HostService psiphonCallbackProxy;
    @Override
    public void onCreate() {
        super.onCreate();
        psiphonTunnel = PsiphonTunnel.newPsiphonTunnel(this);
        try {
            psiphonTunnel.startTunneling("");
        } catch (PsiphonTunnel.Exception e) {
            Log.e(TAG, "Psiphon failed to start: " + e);
        }
    }

    @Override
    public String getAppName() {
        return TAG;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        psiphonTunnel.stop();
    }

    @Override
    public void onDiagnosticMessage(String message) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onDiagnosticMessage(message);
        }
    }

    @Override
    public void onAvailableEgressRegions(List<String> regions) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onAvailableEgressRegions(regions);
        }
    }

    @Override
    public void onListeningSocksProxyPort(int port) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onListeningSocksProxyPort(port);
        }
    }

    @Override
    public void onListeningHttpProxyPort(int port) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onListeningHttpProxyPort(port);
        }
    }

    @Override
    public void onConnecting() {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onConnecting();
        }
    }

    @Override
    public void onConnected() {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onConnected();
        }
    }
    @Override
    public void onBytesTransferred(long sent, long received) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onBytesTransferred(sent, received);
        }
    }

    @Override
    public void onClientRegion(String region) {
        if (psiphonCallbackProxy != null) {
            psiphonCallbackProxy.onClientRegion(region);
        }
    }

    @Override
    public String getPsiphonConfig() {
        try {
            JSONObject config = new JSONObject(
                    readInputStreamToString(
                            getResources().openRawResource(R.raw.psiphon_config)));

            return config.toString();

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to get Psiphon config: " + e.getMessage());
        }
        return "";
    }

    private static String readInputStreamToString(InputStream inputStream) throws IOException {
        return new String(readInputStreamToBytes(inputStream), "UTF-8");
    }

    private static byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int readCount;
        byte[] buffer = new byte[16384];
        while ((readCount = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, readCount);
        }
        outputStream.flush();
        inputStream.close();
        return outputStream.toByteArray();
    }

    public void restartPsiphon() {
        try {
            psiphonTunnel.restartPsiphon();
        } catch (PsiphonTunnel.Exception e) {
            Log.e(TAG, "Psiphon failed to restart: " + e);
        }
    }

    public void setPsiphonCallbackProxy(PsiphonTunnel.HostService psiphonCallbackProxy) {
        this.psiphonCallbackProxy = psiphonCallbackProxy;
    }
}
