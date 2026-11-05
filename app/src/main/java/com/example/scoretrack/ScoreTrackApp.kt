package com.example.scoretrack

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder

// Uygulama genelinde TEK bir resim yükleyici.
// Eskiden her satır çizilirken yeni bir ImageLoader oluşturuluyordu, bu hem yavaştı hem önbelleği bozuyordu.
// Artık tüm `imageView.load(url)` çağrıları bunu kullanıyor (SVG logolar dahil).
class ScoreTrackApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
}
