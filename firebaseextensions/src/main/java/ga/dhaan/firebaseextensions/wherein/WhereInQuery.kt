package ga.dhaan.firebaseextensions.wherein

import com.google.firebase.database.*
import ga.dhaan.firebaseextensions.generic.listener.ListenerCollection
import ga.dhaan.firebaseextensions.generic.query.AbstractQuery
import java.util.*

class WhereInQuery(
        private val rootRef: DatabaseReference,
        selection: AbstractQuery
) : ListenerCollection(true) {

    override fun iterateChilds(eventListener: ChildEventListener) {
        var prefKey : String?= null
        for (key in keys) {
            eventListener.onChildAdded(getModel(key).snap, prefKey)
            prefKey = key
        }
    }

    private var models = HashMap<String, Model>()
    private var keys = LinkedList<String>()

    init {
        loadThese(selection)
    }

    private fun loadThese(ref: AbstractQuery) {
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snap: DataSnapshot, after: String?) {
                val position = getPosAfter(after)
                val oldKey = keys[position]
                val newKey = snap.key
                if ( oldKey != newKey) {
                    keys[position] = newKey
                    getModel(oldKey).stop()
                    val newModel = getModel(newKey)
                    newModel.after = after
                    newModel.start()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                this@WhereInQuery.onCancelled(error)
            }

            override fun onChildMoved(snap: DataSnapshot, after: String?) {
                keys.remove(snap.key)
                keys.add(getPosAfter(after), snap.key)
                val model = getModel(snap.key)
                model.after = after
                this@WhereInQuery.onChildMoved(model.snap, after)
            }

            override fun onChildAdded(snap: DataSnapshot, after: String?) {
                keys.add(getPosAfter(after), snap.key)
                val model = getModel(snap.key)
                model.firstAction = { snap-> this@WhereInQuery.onChildAdded(snap, after)}
                model.after = after
                model.start()
            }

            override fun onChildRemoved(snap: DataSnapshot) {
                keys.remove(snap.key)
                val model = getModel(snap.key)
                model.stop()
                if (model.snap!=null) {
                    this@WhereInQuery.onChildRemoved(snap)
                }
            }
        })
    }

    private fun  getModel(key: String): Model {
        val model: Model
        if (models.containsKey(key)) {
            model = models[key]!!
        } else {
            model = Model(rootRef.child(key))
            models[key] = model
        }
        return model
    }

    private fun getPosAfter(after: String?) = keys.indexOf(after)+1

    inner class Model(val ref: DatabaseReference) {
        var after: String? = null
        var snap: DataSnapshot? = null
            private set

        private var listening: Boolean = false
        val listener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) = error("error "+error.message)

            override fun onDataChange(snap: DataSnapshot) {
                this@Model.snap = snap
                firstAction(snap)
                firstAction = dataChange
            }
        }

        fun start() {
            if (!listening) {
                listening = true
                this@Model.snap = null
                ref.addValueEventListener(listener)
                ref.onDisconnect().cancel()
            }
        }

        fun stop() {
            if (listening) {
                ref.removeEventListener(listener)
                listening = false
            }
        }

        var firstAction: (snap: DataSnapshot)->Unit = { _->}
        val dataChange: (snap: DataSnapshot)->Unit = { snap-> this@WhereInQuery.onChildChanged(snap, after)}
    }
}