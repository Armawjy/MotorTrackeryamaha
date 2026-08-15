package com.example.motortrackeryamaha.data

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithPoints(
    @Embedded val trip: Trip,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val points: List<TripPoint>
)
