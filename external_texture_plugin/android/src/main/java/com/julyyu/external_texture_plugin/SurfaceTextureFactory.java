package com.julyyu.external_texture_plugin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.TextureRegistry;

/**
 * @author julyyu
 * @date 2020-06-02.
 * description：
 */
public class SurfaceTextureFactory {


    private static HashMap<String, TextureEntity> surfaceMap = new HashMap<>();
//    private PluginRegistry.Registrar registrar;
//    private Context mContext;
//    private Activity mActivity;

//    public SurfaceTextureFactory(Context context, Activity activity, PluginRegistry.Registrar registrar) {
//        this.registrar = registrar;
//        this.mContext = context;
//        this.mActivity = activity;
//    }


    public static void loadImage(Context context,
                                 Activity activity,
                                 MethodCall call,
                                 MethodChannel.Result result,
                                 PluginRegistry.Registrar registrar) {
        String url = call.argument("url");
        if (TextUtils.isEmpty(url)) {
            Map<String, Object> maps = new HashMap<>();
            result.error("error", "url is null", maps);
            return;
        }
        TextureEntity textureEntity = surfaceMap.get(url);

        Log.i("ExternalTexturePlugin", " surfaceMap size " + surfaceMap.size());
        if (textureEntity != null) {
            Log.i("ExternalTexturePlugin", " surfaceTextureEntry != null");
            Map<String, Object> reply = new HashMap<>();
            reply.put("textureId", textureEntity.getTextureEntry().id());
            reply.put("width", textureEntity.getWidth());
            reply.put("height", textureEntity.getHeight());
            result.success(reply);
        } else {
            Log.i("ExternalTexturePlugin", " surfaceTextureEntry == null");
            Map<String, Object> reply = new HashMap<>();
            glideLoad(context, activity, reply, result, registrar, url);
        }
    }


    private static void glideLoad(
            Context context,
            final Activity activity,
            final Map<String, Object> maps,
            final MethodChannel.Result result,
            final PluginRegistry.Registrar registrar,
            final String url) {
        final TextureRegistry.SurfaceTextureEntry surfaceTextureEntry = registrar.textures().createSurfaceTexture();
        Glide.with(context).asBitmap().load(url).listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                surfaceTextureEntry.release();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.error("error", "onLoadFailed", maps);
                        }
                    });
                }
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                try {
                    int bitmapWidth = resource.getWidth();
                    int bitmapHeight = resource.getHeight();
                    Rect rect = new Rect(0, 0, bitmapWidth, bitmapHeight);
                    SurfaceTexture surfaceTexture = surfaceTextureEntry.surfaceTexture();
                    surfaceTexture.setDefaultBufferSize(bitmapWidth, bitmapHeight);
                    Surface surface = new Surface(surfaceTextureEntry.surfaceTexture());
                    Canvas canvas = surface.lockCanvas(rect);
                    canvas.drawBitmap(resource, null, rect, null);
                    surface.unlockCanvasAndPost(canvas);
                    maps.put("textureId", surfaceTextureEntry.id());
                    maps.put("width", bitmapWidth);
                    maps.put("height", bitmapHeight);
                    surfaceMap.put(url, new TextureEntity(bitmapWidth, bitmapHeight, surfaceTextureEntry));
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                result.success(maps);
                            }
                        });
                    }
                } catch (final Exception e) {
                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                result.error("error", e.getMessage(), maps);
                            }
                        });
                    }
                }

                return false;
            }
        }).submit();
    }


}