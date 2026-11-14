package me.viknesh.justaqi.data


data class Place(val name: String, val lat: Double, val lon: Double)

object Places {
    val DELHI = Place("Delhi", 28.6139, 77.2090)

    val cities: List<Place> = listOf(
        DELHI,
        Place("Mumbai", 19.0760, 72.8777),
        Place("Bengaluru", 12.9716, 77.5946),
        Place("Chennai", 13.0827, 80.2707),
        Place("Kolkata", 22.5726, 88.3639),
        Place("Hyderabad", 17.3850, 78.4867),
        Place("Pune", 18.5204, 73.8567),
        Place("Ahmedabad", 23.0225, 72.5714),
        Place("Jaipur", 26.9124, 75.7873),
        Place("Lucknow", 26.8467, 80.9462),
        Place("Kanpur", 26.4499, 80.3319),
        Place("Patna", 25.5941, 85.1376),
        Place("Chandigarh", 30.7333, 76.7794),
        Place("Gurugram", 28.4595, 77.0266),
        Place("Noida", 28.5355, 77.3910),
        Place("Ghaziabad", 28.6692, 77.4538),
        Place("Bhopal", 23.2599, 77.4126),
        Place("Indore", 22.7196, 75.8577),
        Place("Nagpur", 21.1458, 79.0882),
        Place("Kochi", 9.9312, 76.2673),
        Place("Coimbatore", 11.0168, 76.9558),
        Place("Madurai", 9.9252, 78.1198),
        Place("Visakhapatnam", 17.6868, 83.2185),
        Place("Guwahati", 26.1445, 91.7362),
    )

    /** Closest listed city within [maxKm], or null. Used to label "Near Delhi". */
    fun nearest(lat: Double, lon: Double, maxKm: Double = 40.0): Place? =
        cities.minByOrNull { distanceKm(lat, lon, it.lat, it.lon) }
            ?.takeIf { distanceKm(lat, lon, it.lat, it.lon) <= maxKm }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        me.viknesh.justaqi.core.distanceKm(lat1, lon1, lat2, lon2)
}
