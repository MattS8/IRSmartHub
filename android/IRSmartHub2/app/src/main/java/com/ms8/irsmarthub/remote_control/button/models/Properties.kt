package com.ms8.irsmarthub.remote_control.button.models

import android.util.ArrayMap

data class Properties(
    var bgStyle: BgStyle = BgStyle.BG_CIRCLE,
    var bgUrl: String = "",
    var image: String = "",
    var marginBottom: Int = 16,
    var marginTop: Int = 16,
    var marginStart: Int = 16,
    var marginEnd: Int = 16,
    var text: String = "",
    var bgTint: String = ""
    ) {

    fun toFirebaseObject() : Map<String, Any?> {
        val firebaseObject = ArrayMap<String, Any?>()
            .apply {
                put("bgStyle", bgStyle.ordinal)
            }
        if (bgUrl != "")
            firebaseObject["bgUrl"] = bgUrl
        if (image != "")
            firebaseObject["image"] = image
        if (marginBottom != 16)
            firebaseObject["marginBottom"] = marginBottom
        if (marginTop != 16)
            firebaseObject["marginTop"] = marginTop
        if (marginStart != 16)
            firebaseObject["marginStart"] = marginStart
        if (marginEnd != 16)
            firebaseObject["marginEnd"] = marginEnd
        if (text != "")
            firebaseObject["text"] = text
        if (bgTint != "")
            firebaseObject["bgTint"] = bgTint

        return firebaseObject
    }

    companion object {
        fun fromFirebaseObject(propertiesMap: Map<String, Any?>) : Properties {
            return Properties()
                .apply {
                    if (propertiesMap.containsKey("bgUrl"))
                        bgUrl = propertiesMap["bgUrl"] as String
                    if (propertiesMap.containsKey("image"))
                        image = propertiesMap["image"] as String
                    if (propertiesMap.containsKey("marginEnd"))
                        marginEnd = (propertiesMap["marginEnd"] as Number).toInt()
                    if (propertiesMap.containsKey("marginBottom"))
                        marginBottom = (propertiesMap["marginBottom"] as Number).toInt()
                    if (propertiesMap.containsKey("marginTop"))
                        marginTop = (propertiesMap["marginTop"] as Number).toInt()
                    if (propertiesMap.containsKey("marginStart"))
                        marginStart = (propertiesMap["marginStart"] as Number).toInt()
                    if (propertiesMap.containsKey("bgStyle"))
                        bgStyle = BgStyle.values().associateBy(BgStyle::ordinal)[(propertiesMap["bgStyle"] as Number).toInt()]
                            ?: BgStyle.BG_NONE
                    if (propertiesMap.containsKey("text"))
                        text = propertiesMap["text"] as String
                    if (propertiesMap.containsKey("bgTint"))
                        bgTint = propertiesMap["bgTint"] as String
                }
        }

        enum class BgStyle {
            BG_INVISIBLE,
            BG_CIRCLE,
            BG_ROUND_RECT,
            BG_ROUND_RECT_TOP,
            BG_ROUND_RECT_BOTTOM,
            BG_CUSTOM_IMAGE,
            BG_RADIAL_TOP,
            BG_RADIAL_END,
            BG_RADIAL_BOTTOM,
            BG_RADIAL_START,
            BG_RADIAL_CENTER,
            BG_NONE
        }

        const val IMG_ADD           = "_IMG_ADD_"
        const val IMG_SUBTRACT      = "_IMG_SUBTRACT_"
        const val IMG_RADIAL_LEFT   = "_IMG_RADIAL_LEFT"
        const val IMG_RADIAL_UP     = "_IMG_RADIAL_UP"
        const val IMG_RADIAL_DOWN   = "_IMG_RADIAL_DOWN"
        const val IMG_RADIAL_RIGHT  = "_IMG_RADIAL_RIGHT"
    }
}