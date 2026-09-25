package com.ridetrack.app.ui.components

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ridetrack.app.ui.theme.RtColors
import com.ridetrack.app.ui.theme.RtDimens
import com.ridetrack.app.ui.theme.RtType
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

data class GeoPoint(val latitude: Double, val longitude: Double)

private const val ROUTE_SOURCE = "route-source"
private const val ENDS_SOURCE = "ends-source"
private const val MARKER_SOURCE = "marker-source"

/**
 * Dark raster basemap (OpenStreetMap data, CARTO tiles). Tiles need a connection; the
 * route itself is local data and always draws, even offline.
 */
private val DARK_STYLE = """
{
  "version": 8,
  "sources": {
    "basemap": {
      "type": "raster",
      "tiles": [
        "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        "https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        "https://c.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "maxzoom": 19,
      "attribution": "© OpenStreetMap contributors © CARTO"
    }
  },
  "layers": [
    { "id": "background", "type": "background", "paint": { "background-color": "#0B0B0D" } },
    { "id": "basemap", "type": "raster", "source": "basemap" }
  ]
}
""".trimIndent()

private fun hex(c: androidx.compose.ui.graphics.Color): String {
    val argb = android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    return String.format("#%06X", 0xFFFFFF and argb)
}

/**
 * MapLibre route map. [route] should already be filtered to valid fixes; [marker] is the
 * current scrub/replay position.
 */
@Composable
fun RouteMap(
    route: List<GeoPoint>,
    modifier: Modifier = Modifier,
    marker: GeoPoint? = null,
    interactive: Boolean = false,
    followMarker: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.apply {
                isCompassEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = true
                setAllGesturesEnabled(interactive)
            }
            m.setStyle(Style.Builder().fromJson(DARK_STYLE)) { s ->
                s.addSource(GeoJsonSource(ROUTE_SOURCE))
                s.addSource(GeoJsonSource(ENDS_SOURCE))
                s.addSource(GeoJsonSource(MARKER_SOURCE))
                s.addLayer(
                    LineLayer("route-casing", ROUTE_SOURCE).withProperties(
                        PropertyFactory.lineColor("#000000"),
                        PropertyFactory.lineWidth(7f),
                        PropertyFactory.lineOpacity(0.6f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
                s.addLayer(
                    LineLayer("route", ROUTE_SOURCE).withProperties(
                        PropertyFactory.lineColor(hex(RtColors.Primary)),
                        PropertyFactory.lineWidth(4f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
                s.addLayer(
                    CircleLayer("ends", ENDS_SOURCE).withProperties(
                        PropertyFactory.circleRadius(5f),
                        PropertyFactory.circleColor(hex(RtColors.TextPrimary)),
                        PropertyFactory.circleStrokeColor("#000000"),
                        PropertyFactory.circleStrokeWidth(2f),
                    ),
                )
                s.addLayer(
                    CircleLayer("marker", MARKER_SOURCE).withProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleColor(hex(RtColors.GForce)),
                        PropertyFactory.circleStrokeColor("#000000"),
                        PropertyFactory.circleStrokeWidth(3f),
                    ),
                )
                style = s
            }
        }
    }

    LaunchedEffect(style, route) {
        val s = style ?: return@LaunchedEffect
        val m = map ?: return@LaunchedEffect
        val points = route.map { Point.fromLngLat(it.longitude, it.latitude) }
        s.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(
            if (points.size >= 2) FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(points)))
            else FeatureCollection.fromFeatures(emptyList()),
        )
        s.getSourceAs<GeoJsonSource>(ENDS_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(listOfNotNull(points.firstOrNull(), points.lastOrNull()).map { Feature.fromGeometry(it) }),
        )
        mapView.post { fitCamera(m, route) }
    }

    LaunchedEffect(style, marker) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(MARKER_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(
                listOfNotNull(marker?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) }),
            ),
        )
        if (followMarker && marker != null) {
            map?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(marker.latitude, marker.longitude)))
        }
    }

    Box(
        modifier
            .clip(RoundedCornerShape(RtDimens.cardRadius))
            .background(RtColors.Surface)
            .semantics { contentDescription = "Route map" },
    ) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (route.size < 2) {
            Text(
                "No GPS route recorded",
                style = RtType.caption,
                color = RtColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(RtColors.Background.copy(alpha = 0.7f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

private fun fitCamera(map: MapLibreMap, route: List<GeoPoint>) {
    when {
        route.isEmpty() -> Unit
        route.size == 1 || route.all { it == route.first() } -> {
            val p = route.first()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 15.0))
        }
        else -> {
            val bounds = LatLngBounds.Builder().apply { route.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
            runCatching { map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64)) }
        }
    }
}
