package ga.dhaan.firebaseextensions.generic.query

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.ValueEventListener

interface AbstractQuery{
    fun addChildEventListener(eventListener: ChildEventListener): ChildEventListener
    fun addValueEventListener(eventListener: ValueEventListener) : ValueEventListener
    fun removeEventListener(eventListener: ValueEventListener)
    fun removeEventListener(eventListener: ChildEventListener)
}