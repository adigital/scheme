package com.vegatel.scheme.model

const val REPEATER_ID = 0

sealed class Element {

    abstract val id: Int
    abstract val signalPower: Double

    abstract override fun toString(): String

    data class Antenna(
        override val id: Int,
        override val signalPower: Double = 6.0,
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
            "Combiner2(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Combiner3(
        override val id: Int,
        override val signalPower: Double = -4.8,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Combiner3(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Combiner4(
        override val id: Int,
        override val signalPower: Double = -6.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Combiner4(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter2(
        override val id: Int,
        override val signalPower: Double = -3.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter2(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter3(
        override val id: Int,
        override val signalPower: Double = -4.8,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter3(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Splitter4(
        override val id: Int,
        override val signalPower: Double = -6.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Splitter4(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Repeater(
        override val id: Int = REPEATER_ID,
        override val signalPower: Double = 30.0,
        val maxOutputPower: Double = 20.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Repeater(id=$id, signalPower=$signalPower, maxOutputPower=$maxOutputPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Booster(
        override val id: Int,
        val maxOutputPower: Double,      // предельная выходная мощность, дБм
        val maxGain: Double,             // максимальное возможное усиление бустера, дБ (константа модели)
        override val signalPower: Double, // текущее установленное усиление, дБ
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Booster(id=$id, maxOutputPower=$maxOutputPower, maxGain=$maxGain, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    data class Coupler(
        override val id: Int,
        val attenuation1: Double,
        val attenuation2: Double,
        override val signalPower: Double = 0.0,
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Coupler(id=$id, attenuation1=$attenuation1, attenuation2=$attenuation2, endElementId=$endElementId, cable=$cable)"
    }

    data class Attenuator(
        override val id: Int,
        override val signalPower: Double = 0.0, // ослабление сигнала (0..-20 дБ)
        val endElementId: Int,
        val cable: Cable = Cable()
    ) : Element() {
        override fun toString(): String =
            "Attenuator(id=$id, signalPower=$signalPower, endElementId=$endElementId, cable=$cable)"
    }

    fun fetchTopElementId(): Int {
        return when (this) {
            is Antenna -> this.id
            is Load -> this.id
            is Combiner2 -> this.id
            is Combiner3 -> this.id
            is Combiner4 -> this.id
            is Repeater -> this.id
            is Booster -> this.id
            is Splitter2 -> this.id
            is Splitter3 -> this.id
            is Splitter4 -> this.id
            is Coupler -> this.id
            is Attenuator -> this.id
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
            is Booster -> this.endElementId
            is Splitter2 -> this.endElementId
            is Splitter3 -> this.endElementId
            is Splitter4 -> this.endElementId
            is Coupler -> this.endElementId
            is Attenuator -> this.endElementId
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
            is Booster -> this.cable
            is Splitter2 -> this.cable
            is Splitter3 -> this.cable
            is Splitter4 -> this.cable
            is Coupler -> this.cable
            is Attenuator -> this.cable
        }
    }

    fun isCombiner(): Boolean = this is Combiner2 || this is Combiner3 || this is Combiner4

    fun isSplitterOrCoupler(): Boolean =
        this is Splitter2 || this is Splitter3 || this is Splitter4 || this is Coupler

    fun isHalfShiftRender(): Boolean =
        this is Combiner2 || this is Combiner4 || this is Splitter2 || this is Splitter4 || this is Coupler
}

data class Cable(
    val length: Double = 0.0,
    val type: CableType = CableType.CF_HALF,
    val isTwoCorners: Boolean = true,
    val isSideThenDown: Boolean = true,
    val isStraightLine: Boolean = false
)