package com.vidrierapp.domain.location

import com.vidrierapp.domain.model.GeoPoint
import java.text.Normalizer

data class Neighborhood(val name: String, val center: GeoPoint)

object NeighborhoodCatalog {

    val all: List<Neighborhood> = listOf(
        Neighborhood("Mataderos", GeoPoint(-34.6615, -58.5070)),
        Neighborhood("Haedo", GeoPoint(-34.6430, -58.5926)),
        Neighborhood("Liniers", GeoPoint(-34.6445, -58.5200)),
        Neighborhood("Villa Luro", GeoPoint(-34.6370, -58.5030)),
        Neighborhood("Ciudadela", GeoPoint(-34.6350, -58.5400)),
        Neighborhood("Ramos Mejía", GeoPoint(-34.6400, -58.5650)),
        Neighborhood("Morón", GeoPoint(-34.6534, -58.6198)),
        Neighborhood("Floresta", GeoPoint(-34.6310, -58.4830)),
        Neighborhood("Flores", GeoPoint(-34.6280, -58.4630)),
        Neighborhood("Caballito", GeoPoint(-34.6186, -58.4417))
    )

    fun search(query: String): List<Neighborhood> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return emptyList()
        return all.filter { normalize(it.name).contains(normalized) }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
}
