package com.worship.nityamandir.data

import android.content.Context
import org.json.JSONObject

data class ShrineOption(val id:String,val en:String,val hi:String,val path:String,val kind:String,val thumbnail:String)
data class ShrineCategory(val id:String,val en:String,val hi:String,val options:List<ShrineOption>)
data class ShrineSelection(val values:Map<String,String> = emptyMap()) {
    operator fun get(category:String)=values[category] ?: "original"
    fun with(category:String,id:String)=copy(values=values+(category to id))
    // Comma-separated IDs preserve the existing string-valued account wire format.
    val flowerIds get()=this["flowers"].split(',').filter {it.isNotBlank()}.distinct().ifEmpty {listOf("original")}
    fun toggleFlower(id:String):ShrineSelection {
        val ids=flowerIds
        val next=if(id in ids) ids-id else ids+id
        return if(next.isEmpty()) this else with("flowers",next.joinToString(","))
    }
    val original get()=values.values.all {it=="original"}
    // Measured altar surfaces in the prepared portrait backgrounds, in image-width units.
    val altarOffset get()=when(this["shrine"]) {"marble" -> -.040f; "ivory" -> -.080f; "carved" -> -.055f; else -> 0f}
    val deityCount get()=if(this["idols"] in listOf("original","ganesh_hanuman")) 2 else 1
    fun deityNames(hindi:Boolean):List<String> = when(this["idols"]) {
        "ganesh_hanuman" -> if(hindi) listOf("गणेश जी","हनुमान जी") else listOf("Ganesha","Hanuman")
        "shiva" -> listOf(if(hindi) "शिव जी" else "Shiva")
        "lakshmi" -> listOf(if(hindi) "लक्ष्मी जी" else "Lakshmi")
        "durga" -> listOf(if(hindi) "दुर्गा माँ" else "Durga")
        "ram_darbar" -> listOf(if(hindi) "राम दरबार" else "Ram Darbar")
        else -> if(hindi) listOf("गणेश जी","लक्ष्मी जी") else listOf("Ganesha","Lakshmi")
    }
}
class ShrineCatalog(context:Context) {
    val categories:List<ShrineCategory>
    init {
        val array=JSONObject(context.assets.open("shrine/catalog.json").bufferedReader().use {it.readText()}).getJSONArray("categories")
        categories=(0 until array.length()).map {i -> val c=array.getJSONObject(i);val options=c.getJSONArray("options")
            ShrineCategory(c.getString("id"),c.getString("label_en"),c.getString("label_hi"),(0 until options.length()).map {j -> val o=options.getJSONObject(j)
                ShrineOption(o.getString("id"),o.getString("label_en"),o.getString("label_hi"),o.getString("path"),o.getString("kind"),o.getString("thumbnail"))
            })
        }
    }
    fun option(category:String,selection:ShrineSelection):ShrineOption {
        val c=categories.first {it.id==category}
        return c.options.firstOrNull {it.id==selection[category]} ?: c.options.first()
    }
    fun flowerPaths(selection:ShrineSelection):List<String> {
        val options=categories.first {it.id=="flowers"}.options
        return selection.flowerIds.flatMap {id ->
            if(id=="original") listOf("shrine/flowers/sunflower.glb","shrine/flowers/peony.glb")
            else options.firstOrNull {it.id==id}?.let {listOf(it.path)} ?: emptyList()
        }.distinct()
    }
    fun normalize(selection:ShrineSelection)=ShrineSelection(categories.associate {category ->
        category.id to if(category.id=="flowers") {
            selection.flowerIds.filter {id -> category.options.any {it.id==id}}.ifEmpty {listOf("original")}.joinToString(",")
        } else option(category.id,selection).id
    })
}
