package org.jetbrains.kotlinx.dataframe.geo.io

import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geojson.feature.FeatureJSON
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.geo.GeoDataFrame
import org.jetbrains.kotlinx.dataframe.geo.geotools.toGeoDataFrame
import org.jetbrains.kotlinx.dataframe.io.asURL
import java.net.URL


fun GeoDataFrame.Companion.readGeoJSON(path: String): GeoDataFrame<*> {
    return readGeoJSON(asURL(path))
}

fun GeoDataFrame.Companion.readGeoJSON(url: URL): GeoDataFrame<*> {
    return (FeatureJSON().readFeatureCollection(url.openStream()) as SimpleFeatureCollection).toGeoDataFrame()
}

fun DataFrame.Companion.readGeoJSON(path: String): GeoDataFrame<*> {
    return GeoDataFrame.readGeoJSON(path)
}

fun DataFrame.Companion.readGeoJSON(url: URL): GeoDataFrame<*> {
    return GeoDataFrame.readGeoJSON(url)
}

fun GeoDataFrame.Companion.readShapefile(path: String): GeoDataFrame<*> {
    return readShapefile(asURL(path))
}

fun GeoDataFrame.Companion.readShapefile(url: URL): GeoDataFrame<*> {
    return ShapefileDataStoreFactory().createDataStore(url).featureSource.features.toGeoDataFrame()
}

fun DataFrame.Companion.readShapefile(path: String): GeoDataFrame<*> {
    return GeoDataFrame.readShapefile(path)
}

fun DataFrame.Companion.readShapefile(url: URL): GeoDataFrame<*> {
    return GeoDataFrame.readShapefile(url)
}
