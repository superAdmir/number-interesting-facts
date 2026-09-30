package com.nip.numberinterestingfacts.facts.wiki;

import android.content.Context;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.nip.numberinterestingfacts.BuildConfig;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal HTTPS client for the MediaWiki Action API. Responses are parsed on Volley's network
 * thread (never on the UI thread) and cached in memory for the session. TLS uses the platform's
 * default certificate validation; cleartext is disabled app-wide.
 */
public final class WikiClient {
    /** Descriptive User-Agent with contact details, as the Wikimedia User-Agent policy requires. */
    static final String USER_AGENT = "NumberInterestingFacts/" + BuildConfig.VERSION_NAME
            + " (https://play.google.com/store/apps/details?id=com.nip.numberinterestingfacts;"
            + " superadmirapps@gmail.com) Volley/1.2.1";
    private static final int TIMEOUT_MS = 8000;

    public interface Parser<T> {
        @NonNull
        T parse(@NonNull String body) throws Exception;
    }

    public interface Callback<T> {
        void onResult(@NonNull T value);

        void onError(@NonNull Exception error);
    }

    private final RequestQueue queue;
    private final LruCache<String, Object> cache = new LruCache<>(48);

    public WikiClient(@NonNull Context context) {
        queue = Volley.newRequestQueue(context.getApplicationContext());
    }

    @SuppressWarnings("unchecked")
    public <T> void get(@NonNull String url, @NonNull Object tag, @NonNull Parser<T> parser,
                        @NonNull Callback<T> callback) {
        Object cached = cache.get(url);
        if (cached != null) {
            callback.onResult((T) cached);
            return;
        }
        ParsedRequest<T> request = new ParsedRequest<>(url, parser, value -> {
            cache.put(url, value);
            callback.onResult(value);
        }, error -> callback.onError(error));
        request.setTag(tag);
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 1, 1.5f));
        queue.add(request);
    }

    public void cancel(@NonNull Object tag) {
        queue.cancelAll(tag);
    }

    private static final class ParsedRequest<T> extends Request<T> {
        private final Parser<T> parser;
        private final Response.Listener<T> listener;

        ParsedRequest(String url, Parser<T> parser, Response.Listener<T> listener,
                      Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            this.parser = parser;
            this.listener = listener;
        }

        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", USER_AGENT);
            headers.put("Api-User-Agent", USER_AGENT);
            headers.put("Accept", "application/json");
            return headers;
        }

        @Override
        protected Response<T> parseNetworkResponse(NetworkResponse response) {
            try {
                String body = new String(response.data, StandardCharsets.UTF_8);
                return Response.success(parser.parse(body), HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }

        @Override
        protected void deliverResponse(T response) {
            listener.onResponse(response);
        }
    }
}
