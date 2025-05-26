package com.vegatel.scheme.model

const val REPEATER_ID = 0

sealed class Element {

    abstract val id: Int
    abstract val signalPower: Double

    abstract override fun toString(): String

    data class Antenna(
        override val id: Int,
        override val signalPower: Double = 35.0,
        val endElementId: Int = -1,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Antenna(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Load(
        override val id: Int,
        override val signalPower: Double = 0.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Load(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter2(
        override val id: Int,
        override val signalPower: Double = -3.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter2(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter3(
        override val id: Int,
        override val signalPower: Double = -4.8,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter3(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter4(
        override val id: Int,
        override val signalPower: Double = -6.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter4(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Repeater(
        override val id: Int = REPEATER_ID,
        override val signalPower: Double = 50.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String = "Repeater(id=$id)"
    }

    fun fetchTopElementId(): Int {
        return when (this) {
            is Antenna -> this.id
            is Load -> this.id
            is Splitter2 -> this.id
            is Splitter3 -> this.id
            is Splitter4 -> this.id
            is Repeater -> this.id
        }
    }

    fun fetchEndElementId(): Int {
        return when (this) {
            is Antenna -> this.endElementId
            is Load -> this.endElementId
            is Splitter2 -> this.endElementId
            is Splitter3 -> this.endElementId
            is Splitter4 -> this.endElementId
            is Repeater -> this.endElementId
        }
    }

    fun fetchCable(): Cable {
        return when (this) {
            is Antenna -> this.cable
            is Load -> this.cable
            is Splitter2 -> this.cable
            is Splitter3 -> this.cable
            is Splitter4 -> this.cable
            is Repeater -> this.cable
        }
    }

    fun isSplitter(): Boolean = this is Splitter2 || this is Splitter3 || this is Splitter4

    fun isRepeater(): Boolean = this is Repeater

    fun isHalfShiftRender(): Boolean = this is Splitter2 || this is Splitter4
}

data class Cable(
    val length: Double = 0.0,
    val thickness: Int = 1,
    val lossPerMeter: Double = -0.5
)