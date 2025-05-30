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

    data class Combiner2(
        override val id: Int,
        override val signalPower: Double = -3.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Combiner2(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Combiner3(
        override val id: Int,
        override val signalPower: Double = -4.8,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Combiner3(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Combiner4(
        override val id: Int,
        override val signalPower: Double = -6.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Combiner4(id=$id, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter2(
        override val id: Int,
        override val signalPower: Double = -3.0,
        val endElementId1: Int,
        val endElementId2: Int,
        val cable1: Cable = Cable(),
        val cable2: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter2(id=$id, endElementId1=$endElementId1, endElementId2=$endElementId2, cable1=$cable1, cable2=$cable2)"
    }

    data class Splitter3(
        override val id: Int,
        override val signalPower: Double = -4.8,
        val endElementId1: Int,
        val endElementId2: Int,
        val endElementId3: Int,
        val cable1: Cable = Cable(),
        val cable2: Cable = Cable(),
        val cable3: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter3(id=$id, endElementId1=$endElementId1, endElementId2=$endElementId2, endElementId3=$endElementId3, cable1=$cable1, cable2=$cable2, cable3=$cable3)"
    }

    data class Splitter4(
        override val id: Int,
        override val signalPower: Double = -6.0,
        val endElementId1: Int,
        val endElementId2: Int,
        val endElementId3: Int,
        val endElementId4: Int,
        val cable1: Cable = Cable(),
        val cable2: Cable = Cable(),
        val cable3: Cable = Cable(),
        val cable4: Cable = Cable(),
    ) : Element() {
        override fun toString(): String =
            "Splitter4(id=$id, endElementId1=$endElementId1, endElementId2=$endElementId2, endElementId3=$endElementId3, endElementId4=$endElementId4, cable1=$cable1, cable2=$cable2, cable3=$cable3, cable4=$cable4)"
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
            is Combiner2 -> this.id
            is Combiner3 -> this.id
            is Combiner4 -> this.id
            is Repeater -> this.id
            is Splitter2 -> TODO()
            is Splitter3 -> TODO()
            is Splitter4 -> TODO()
        }
    }

    fun fetchEndElementId(): Int {
        return when (this) {
            is Antenna -> this.endElementId
            is Load -> this.endElementId
            is Combiner2 -> this.endElementId
            is Combiner3 -> this.endElementId
            is Combiner4 -> this.endElementId
            is Repeater -> this.endElementId
            is Splitter2 -> TODO()
            is Splitter3 -> TODO()
            is Splitter4 -> TODO()
        }
    }

    fun fetchCable(): Cable {
        return when (this) {
            is Antenna -> this.cable
            is Load -> this.cable
            is Combiner2 -> this.cable
            is Combiner3 -> this.cable
            is Combiner4 -> this.cable
            is Repeater -> this.cable
            is Splitter2 -> TODO()
            is Splitter3 -> TODO()
            is Splitter4 -> TODO()
        }
    }

    fun isCombiner(): Boolean = this is Combiner2 || this is Combiner3 || this is Combiner4

    fun isRepeater(): Boolean = this is Repeater

    fun isHalfShiftRender(): Boolean = this is Combiner2 || this is Combiner4
}

data class Cable(
    val length: Double = 0.0,
    val thickness: Int = 1,
    val lossPerMeter: Double = -0.5
)