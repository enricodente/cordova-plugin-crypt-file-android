package com.crypt.cordova;

import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.webkit.WebViewAssetLoader;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPluginPathHandler;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.LOG;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptResource extends CordovaPlugin {

    private static final String TAG = "DecryptResource";
    private static final String DEFAULT_PORT = "8080";
    private static final String DEFAULT_INDEX_FILE = "index.html";
    private static final String ASSET_PREFIX = "www/";

    private static final String CRYPT_KEY = "";
    private static final String CRYPT_IV = "";

    private static final String[] CRYPT_FILES = {
        ".htm",
        ".html",
        ".js",
        ".css",
    };

    private String URL_PREFIX;
    private CordovaPluginPathHandler pathHandler;

    public void changingPort(CordovaPreferences preferences) {
        String port = preferences.getString("cryptoPort", DEFAULT_PORT);
        URL_PREFIX = "http://localhost:" + port + "/";
    }

    @Override
    protected void pluginInitialize() {
        changingPort(preferences);
    }

    @Override
    public CordovaPluginPathHandler getPathHandler() {
        if (pathHandler == null) {
            pathHandler = new CordovaPluginPathHandler(new WebViewAssetLoader.PathHandler() {
                @Override
                public WebResourceResponse handle(String path) {
                    try {
                        return decryptAssetToWebResponse(normalizeAssetPath(path));
                    } catch (IOException ex) {
                        LOG.e(TAG, "Unable to decrypt asset from WebViewAssetLoader", ex);
                        return null;
                    }
                }
            });
        }
        return pathHandler;
    }

    @Override
    public Uri remapUri(Uri uri) {
        changingPort(preferences);
        String launchUri = uri.toString();
        if (launchUri.startsWith(URL_PREFIX)) {
            return this.toPluginUri(uri);
        }
        return uri;
    }

    @Override
    public CordovaResourceApi.OpenForReadResult handleOpenForRead(Uri uri) throws IOException {
        Uri originalUri = fromPluginUri(uri);
        String relativePath = toRelativeAssetPath(originalUri.toString().split("\\?")[0]);
        String normalizedPath = normalizeAssetPath(relativePath);

        if (!isCryptFile(normalizedPath)) {
            String fileUri = toFileUri(originalUri.toString().split("\\?")[0]);
            return this.webView.getResourceApi().openForRead(Uri.parse(fileUri), true);
        }

        byte[] decryptedBytes = decryptAsset(normalizedPath);
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(decryptedBytes);
        return new CordovaResourceApi.OpenForReadResult(
            uri,
            byteInputStream,
            getMimeType(normalizedPath),
            decryptedBytes.length,
            null
        );
    }

    private WebResourceResponse decryptAssetToWebResponse(String relativePath) throws IOException {
        if (!isCryptFile(relativePath)) {
            return null;
        }

        byte[] decryptedBytes = decryptAsset(relativePath);
        return new WebResourceResponse(
            getMimeType(relativePath),
            StandardCharsets.UTF_8.name(),
            new ByteArrayInputStream(decryptedBytes)
        );
    }

    private byte[] decryptAsset(String relativePath) throws IOException {
        String assetPath = ASSET_PREFIX + relativePath;
        String encryptedPayload = readAssetAsString(assetPath);
        return decryptBase64Payload(encryptedPayload, assetPath);
    }

    private String readAssetAsString(String assetPath) throws IOException {
        try (InputStream inputStream = this.webView.getContext().getAssets().open(assetPath, AssetManager.ACCESS_STREAMING);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    private byte[] decryptBase64Payload(String payload, String assetPath) throws IOException {
        try {
            byte[] bytes = Base64.decode(payload, Base64.DEFAULT);
            SecretKey secretKey = new SecretKeySpec(CRYPT_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(CRYPT_IV.getBytes(StandardCharsets.UTF_8)));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(cipher.doFinal(bytes));
            LOG.d(TAG, "decrypt: " + assetPath);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IOException("Unable to decrypt asset: " + assetPath, ex);
        }
    }

    private String normalizeAssetPath(String path) {
        if (path == null || path.length() == 0 || "/".equals(path)) {
            return DEFAULT_INDEX_FILE;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path + DEFAULT_INDEX_FILE;
        }
        return path;
    }

    private String toRelativeAssetPath(String uri) {
        if (uri.startsWith(URL_PREFIX)) {
            uri = uri.replace(URL_PREFIX, "");
        }
        return uri;
    }

    private String toFileUri(String uri) {
        String relativePath = normalizeAssetPath(toRelativeAssetPath(uri));
        return "file:///android_asset/www/" + relativePath;
    }

    private boolean isCryptFile(String path) {
        for (String extension : CRYPT_FILES) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private String getMimeType(String path) {
        String mimeType = "text/html";
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null && !extension.isEmpty()) {
            if (path.endsWith(".js") || path.endsWith(".mjs")) {
                mimeType = "application/javascript";
            } else {
                String resolvedType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (resolvedType != null) {
                    mimeType = resolvedType;
                }
            }
        }
        return mimeType;
    }
}
