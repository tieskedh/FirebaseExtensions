package ga.dhaan.firebaseextensions.generic.query

import com.google.firebase.database.*
import java.util.*

class AbstractQueryImpl(val ref: Query) : AbstractQuery, ValueEventListener {
    private val changeListeners = ArrayList<ValueEventListener>()

    init { ref.addValueEventListener(this) }
    override fun onCancelled(error: DatabaseError) = changeListeners.forEach {it.onCancelled(error)}

    override fun onDataChange(snap: DataSnapshot?) = changeListeners.forEach{it.onDataChange(snap)}

    override fun addValueEventListener(eventListener: ValueEventListener): ValueEventListener {
        changeListeners.add(eventListener)
        return eventListener
    }

    override fun removeEventListener(eventListener: ValueEventListener) {
        changeListeners.remove(eventListener)
    }

    override fun addChildEventListener(eventListener: ChildEventListener)
            = ref.addChildEventListener(eventListener)!!

    override fun removeEventListener(eventListener: ChildEventListener)
            = ref.removeEventListener(eventListener)
}