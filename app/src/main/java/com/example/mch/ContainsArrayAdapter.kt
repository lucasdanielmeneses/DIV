package com.example.mch

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * ArrayAdapter que filtra por "contiene" en cualquier parte del texto (no solo
 * al inicio) e ignora mayúsculas/minúsculas y acentos. Así evitamos errores de
 * tipeo al buscar marca o modelo.
 */
class ContainsArrayAdapter(
    context: Context,
    resource: Int,
    initialItems: List<String>
) : ArrayAdapter<String>(context, resource, initialItems.toMutableList()) {

    private var originalItems: List<String> = initialItems
    private var filteredItems: List<String> = initialItems

    override fun getCount(): Int = filteredItems.size
    override fun getItem(position: Int): String? = filteredItems.getOrNull(position)

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrBlank()) {
                    results.values = originalItems
                    results.count = originalItems.size
                } else {
                    val query = MchRepository.normalize(constraint.toString())
                    val matches = originalItems.filter {
                        MchRepository.normalize(it).contains(query)
                    }
                    results.values = matches
                    results.count = matches.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = (results?.values as? List<String>) ?: emptyList()
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    /** Reemplaza la lista completa de items (por ejemplo, al cambiar de marca). */
    fun updateItems(newItems: List<String>) {
        originalItems = newItems
        filteredItems = newItems
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }
}
