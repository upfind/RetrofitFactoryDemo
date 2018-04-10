package cn.upfinder.retrofitfactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.support.v4.util.Preconditions.checkNotNull;


/**
 * Created by ucm on 2018/3/19.
 */

public class RetrofitManager {

    private static Context context;

    public static void register(Context mContext) {
        context = mContext;
    }

    private RetrofitManager() {
    }


    public static final class Builder {
        private String baseUrl;

        //缓存相关默认  50M
        private int cacheSize = 1024 * 1024 * 50;
        private String cachePath;
        private boolean userCache = false;
        private int onlineCacheMaxStale = 60;
        private Interceptor cacheInterceptor;

        private OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        private Retrofit.Builder retrofitBuilder = new Retrofit.Builder();

        @SuppressLint("RestrictedApi")
        public Builder baseUrl(@NonNull String baseUrl) {
            checkNotNull(baseUrl);
            this.baseUrl = baseUrl;
            return this;
        }


        public Builder isUseCache(@NonNull boolean useCache) {
            this.userCache = useCache;
            return this;
        }

        @SuppressLint("RestrictedApi")
        public Builder setCacheSize(@NonNull int cacheSize) {
            checkNotNull(cacheSize);
            this.cacheSize = cacheSize;
            return this;
        }

        public Builder setOnlineCacheMaxStale(int maxStale) {
            this.onlineCacheMaxStale = maxStale;
            return this;
        }


        public Retrofit build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base Url required.");
            }

            if (context == null) {
                throw new IllegalStateException("在Application中初始化 retrofitManager");

            }

            if (userCache) {
                if (cachePath == null) {
                    cachePath = "upfinder";
                }

                File cacheFile = new File(context.getCacheDir(), cachePath);
                Cache cache = new Cache(cacheFile, cacheSize);
                httpClientBuilder.cache(cache);
                if (cacheInterceptor == null) {
                    initCacheInterceptor();
                }
                httpClientBuilder.addInterceptor(cacheInterceptor);
                httpClientBuilder.addNetworkInterceptor(cacheInterceptor);
            }

            initHttpClient();
            initRetrofit();
            return retrofitBuilder.build();
        }

        private void initCacheInterceptor() {
            cacheInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {

                    Request request = chain.request();//获取请求
                    //这里就是说判读我们的网络条件，要是有网络的话我么就直接获取网络上面的数据，要是没有网络的话我么就去缓存里面取数据
                    if (!RetrofitManager.isConnected()) { //无网络时

                        request = request.newBuilder()
                                .cacheControl(RetrofitManager.FORCE_CACHE)
                                .build();

                        Response response = chain.proceed(request);
                        return response.newBuilder()
                                .header("Cache-Control", "public, only-if-cached")
                                .removeHeader("Pragma")
                                .build();
                    } else { //有网络时
                        Response response = chain.proceed(request);
                        String chacheControl = request.cacheControl().toString();
                        Log.e("有网络-cache", "在线缓存在1分钟内可读取" + chacheControl);
                        return response.newBuilder()
                                .removeHeader("Pragma")
                                .removeHeader("Cache-Control")
                                .header("Cache-Control", "public, max-age=" + onlineCacheMaxStale)
                                .build();

                    }

                }
            };
        }


        private void initRetrofit() {
            retrofitBuilder
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(httpClientBuilder.build());
        }


        private void initHttpClient() {
            httpClientBuilder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS);

        }


    }


    public static boolean isConnected() {
        @SuppressLint("MissingPermission")
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        return info != null && info.isConnected();
    }


    public static CacheControl FORCE_CACHE = new CacheControl.Builder()
            .onlyIfCached()
            .maxStale(7, TimeUnit.DAYS) //七天
            .build();


}
