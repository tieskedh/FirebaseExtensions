package ga.dhaan.firebaseextensions.generic.gui

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import ga.dhaan.firebaseextensions.generic.query.AbstractQuery
import java.lang.IllegalArgumentException
import java.util.*


class FirebaseArray(private val mQuery: AbstractQuery) : ChildEventListener, ValueEventListener {
    interface OnChangedListener {
        enum class EventType {
            ADDED, CHANGED, REMOVED, MOVED
        }

        fun onChildChanged(type: EventType, index: Int, oldIndex: Int)

        fun onDataChanged(snap: DataSnapshot?)

        fun onCancelled(databaseError: DatabaseError)
    }

    private var mListener: OnChangedListener? = null
    private val mSnapshots = ArrayList<DataSnapshot>()

    init {
        mQuery.addChildEventListener(this)
        mQuery.addValueEventListener(this)
    }

    fun cleanup() {
        mQuery.removeEventListener(this as ValueEventListener)
        mQuery.removeEventListener(this as ChildEventListener)
    }

    val count:Int get() = mSnapshots.size

    fun getItem(index: Int)= mSnapshots[index]

    private fun String.toIndex(after:Int=0) : Int{
        val index = mSnapshots.map { mSnapshot->mSnapshot.key }.indexOf(this)
        if (index == -1) {
            throw IllegalArgumentException("Key not found")
        } else return index
    }

    override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
        val index = previousChildKey?.toIndex(1)?: 0
        mSnapshots.add(index, snapshot)
        notifyChangedListeners(OnChangedListener.EventType.ADDED, index)
    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
        val index = snapshot.key.toIndex()
        mSnapshots[index] = snapshot
        notifyChangedListeners(OnChangedListener.EventType.CHANGED, index)
    }

    override fun onChildRemoved(snapshot: DataSnapshot) {
        val index = snapshot.key.toIndex()
        mSnapshots.removeAt(index)
        notifyChangedListeners(OnChangedListener.EventType.REMOVED, index)
    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {
        val oldIndex = snapshot.key.toIndex()
        mSnapshots.removeAt(oldIndex)
        val newIndex = previousChildKey?.toIndex(1)?: 0
        mSnapshots.add(newIndex, snapshot)
        notifyChangedListeners(OnChangedListener.EventType.MOVED, newIndex, oldIndex)
    }

    override fun onDataChange(snap: DataSnapshot?) {
        mListener!!.onDataChanged(snap)
    }

    override fun onCancelled(error: DatabaseError) {
        notifyCancelledListeners(error)
    }

    fun setOnChangedListener(listener: OnChangedListener) {
        mListener = listener
    }

    @JvmOverloads protected fun notifyChangedListeners(type: OnChangedListener.EventType, index: Int, oldIndex: Int = -1) {
        mListener?.onChildChanged(type, index, oldIndex)
    }

    private fun notifyCancelledListeners(databaseError: DatabaseError) {
        mListener?.onCancelled(databaseError)
    }
}
