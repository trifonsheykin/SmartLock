package com.smartlocks.trifonsheykin.smartlock.analytics

interface Event {

    val value: String

    enum class Screen(override val value: String) : Event {
        MAIN("screen_main")
    }

    enum class Click(override val value: String) : Event {
        ADD_KEY("click_add_key")
    }

    enum class Common(override val value: String) : Event {
        OPEN_DOOR("open_door")
    }
}