package ga.dhaan.firebaseextensions.generic.listener

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import ga.dhaan.firebaseextensions.generic.query.AbstractQuery

abstract class ListenerCollection(val updateTogether: Boolean = false) : AbstractQuery, ChildEventListener, ValueEventListener {

    private val childEventListeners = ArrayList<ChildEventListener>()
    private val changeListener = ArrayList<ValueEventListener>()


    override fun onDataChange(snap: DataSnapshot?) = changeListener.forEach { it.onDataChange(snap) }

    override fun onCancelled(error: DatabaseError) {
        childEventListeners.forEach { listener -> listener.onCancelled(error) }
        changeListener.forEach { listener -> listener.onCancelled(error) }
    }

    override fun onChildMoved(snap: DataSnapshot?, after: String?) {
        childEventListeners.forEach { listener-> listener.onChildMoved(snap, after) }
        if (updateTogether) onDataChange(snap)
    }

    override fun onChildChanged(snap: DataSnapshot?, after: String?) {
        childEventListeners.forEach { listener-> listener.onChildChanged(snap, after) }
        if (updateTogether) onDataChange(snap)
    }

    override fun onChildAdded(snap: DataSnapshot?, after: String?) {
        childEventListeners.forEach { listener-> listener.onChildAdded(snap, after) }
        if (updateTogether) onDataChange(snap)
    }

    override fun onChildRemoved(snap: DataSnapshot?) {
        childEventListeners.forEach { listener-> listener.onChildRemoved(snap) }
        if (updateTogether) onDataChange(snap)
    }

    override fun addChildEventListener(eventListener: ChildEventListener): ChildEventListener {
        if (eventListener!=this) childEventListeners.add(eventListener)
        iterateChilds(eventListener)
        return eventListener
    }

    abstract fun iterateChilds(eventListener: ChildEventListener)

    override fun addValueEventListener(eventListener: ValueEventListener): ValueEventListener {
        if (eventListener!=this) changeListener.add(eventListener)
        return eventListener
    }

    override fun removeEventListener(eventListener: ChildEventListener) {
        childEventListeners.remove(eventListener)
    }

    override fun removeEventListener(eventListener: ValueEventListener) {
        changeListener.remove(eventListener)
    }
}