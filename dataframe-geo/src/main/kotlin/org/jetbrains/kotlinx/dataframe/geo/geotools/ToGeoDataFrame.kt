package org.jetbrains.kotlinx.dataframe.geo.geotools

import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.api.feature.type.GeometryDescriptor
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.data.simple.SimpleFeatureCollection
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.Infer
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.geo.GeoDataFrame
import org.jetbrains.kotlinx.dataframe.geo.GeoFrame
import org.locationtech.jts.geom.Geometry

fun SimpleFeatureCollection.toGeoDataFrame(): GeoDataFrame<*> {

    require(schema is SimpleFeatureType) {
        "GeoTools: SimpleFeatureType expected but was: ${schema::class.simpleName}"
    }
    val attributeDescriptors = (schema as SimpleFeatureType).attributeDescriptors

    val dataAttributes = attributeDescriptors?.filter { it !is GeometryDescriptor }?.map { it!! } ?: emptyList()
    val geometryAttribute = attributeDescriptors?.find { it is GeometryDescriptor }
        ?: throw IllegalArgumentException("No geometry attribute")

    // In GeoJSON the crs attribute is optional
    val crs: CoordinateReferenceSystem? = (geometryAttribute as GeometryDescriptor).coordinateReferenceSystem

    val data = dataAttributes.associate { it.localName to ArrayList<Any?>() }
    val geometries = ArrayList<Geometry>()

    features().use {
        while (it.hasNext()) {
            val feature = it.next()
            require(feature is SimpleFeature) {
                "GeoTools: SimpleFeature expected but was: ${feature::class.simpleName}"
            }
            val featureGeometry = feature.getAttribute(geometryAttribute.name)

            require(featureGeometry is Geometry) {
                "Not a geometry: [${geometryAttribute.name}] = ${featureGeometry?.javaClass?.simpleName} (feature id: ${feature.id})"
            }
            // TODO require(featureGeometry.isValid) { "Invalid geometry, feature id: ${feature.id}" }

            for (dataAttribute in dataAttributes) {
                data[dataAttribute.localName]?.add(feature.getAttribute(dataAttribute.name))
            }
            geometries.add(featureGeometry)
        }
    }

    val geometryColumn = DataColumn.create("geometry", geometries, Infer.Type)

    return GeoDataFrame((data.toDataFrame() + geometryColumn) as DataFrame<GeoFrame>, crs)
}
