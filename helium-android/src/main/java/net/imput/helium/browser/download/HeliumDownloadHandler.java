package net.imput.helium.browser.download;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;
import androidx.annotation.NonNull;

/**
 * HeliumDownloadHandler: Intercepts file downloads from WebViews and routes them
 * to the Android system DownloadManager with proper authentication cookies and headers.
 */
public final class HeliumDownloadHandler implements DownloadListener {

    private final Context mContext;

    public HeliumDownloadHandler(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);

            // Forward user session cookies for authenticated downloads
            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            request.setDescription("Downloading file via Helium Browser");
            request.setTitle(fileName);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(mContext, "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
