package nl.plaatsoft.warquest3;

import android.content.Context;
import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.Base64;

// Fetch data async task
public class FetchDataTask extends AsyncTask<Void, Void, String> {
    public static interface OnLoadListener {
        public abstract void onLoad(String data);
    }

    private final Context context;
    private final String url;
    private final boolean loadFomCache;
    private final boolean saveToCache;
    private final OnLoadListener onLoadListener;

    private FetchDataTask(Context context, String url, boolean loadFomCache, boolean saveToCache, OnLoadListener onLoadListener) {
        this.context = context;
        this.url = url;
        this.loadFomCache = loadFomCache;
        this.saveToCache = saveToCache;
        this.onLoadListener = onLoadListener;
    }

    public static void fetchData(Context context, String url, boolean loadFomCache, boolean saveToCache, OnLoadListener onLoadListener) {
        FetchDataTask fetchDataTask = new FetchDataTask(context, url, loadFomCache, saveToCache, onLoadListener);
        fetchDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String doInBackground(Void... voids) {
        try {
            File file = new File(context.getCacheDir(), new String(Base64.getUrlEncoder().encode(url.getBytes())));
            if (loadFomCache && file.exists()) {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.lineSeparator());
                }
                bufferedReader.close();
                return stringBuilder.toString();
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.lineSeparator());
                }
                bufferedReader.close();
                String data = stringBuilder.toString();
                if (saveToCache) {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(data);
                    fileWriter.close();
                }
                return data;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void onPostExecute(String data) {
        if (!isCancelled()) {
            onLoadListener.onLoad(data);
        }
    }
}
