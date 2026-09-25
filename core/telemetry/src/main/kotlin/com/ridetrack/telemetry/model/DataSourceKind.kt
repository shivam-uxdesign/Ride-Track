package com.ridetrack.telemetry.model

/**
 * Where telemetry came from. New sources (e.g. Bluetooth OBD, external GPS) are added
 * here when they are actually implemented; demo data is always tagged [DEMO].
 */
enum class DataSourceKind { PHONE, DEMO }
