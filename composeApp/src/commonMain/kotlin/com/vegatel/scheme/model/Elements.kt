package com.vegatel.scheme.model

sealed class Element {

    abstract val id: Int

    abstract override fun toString(): String

    data class Antenna(
        override val id: Int,
        val signalPower: Double = 35.0,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Antenna(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Load(
        override val id: Int,
        val signalPower: Double = 0.0,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Load(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter2(
        override val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter2(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter3(
        override val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val topElementId3: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter3(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, topElementId3=$topElementId3, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter4(
        override val id: Int,
        val topElementId1: Int,
        val topElementId2: Int,
        val topElementId3: Int,
        val topElementId4: Int,
        val endElementId: Int,
        val cable: Cable
    ) : Element() {
        override fun toString(): String =
            "Splitter4(id=$id, topElementId1=$topElementId1, topElementId2=$topElementId2, topElementId3=$topElementId3, topElementId4=$topElementId4, endElementId=$endElementId, cable=$cable)"
    }

    data class Repeater(
        override val id: Int,
        val topElementId: Int
    ) : Element() {
        override fun toString(): String =
            "Repeater(id=$id, topElementId=$topElementId)"
    }

    fun fetchEndElementId(): Int {
        return when (this) {
            is Antenna -> this.endElementId
            is Load -> this.endElementId
            is Splitter2 -> this.endElementId
            is Splitter3 -> this.endElementId
            is Splitter4 -> this.endElementId
            is Repeater -> this.topElementId
        }
    }

    fun fetchCable(): Cable {
        return when (this) {
            is Antenna -> this.cable
            is Load -> this.cable
            is Splitter2 -> this.cable
            is Splitter3 -> this.cable
            is Splitter4 -> this.cable
            else -> Cable()
        }
    }
}

data class Cable(
    val length: Double = 10.0,
    val thickness: Int = 1,
    val lossPerMeter: Double = 0.5
)