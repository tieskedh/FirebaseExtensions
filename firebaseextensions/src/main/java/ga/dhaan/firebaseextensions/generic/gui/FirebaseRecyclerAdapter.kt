package ga.dhaan.firebaseextensions.generic.gui

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import ga.dhaan.firebaseextensions.generic.query.AbstractQuery
import java.lang.reflect.InvocationTargetException

/**
 * This class is a generic way of backing an RecyclerView with a Firebase location.
 * It handles all of the child events at the given Firebase location. It marshals received data into the given
 * class type.
 *
 *
 * To use this class in your app, subclass it passing in all required parameters and implement the
 * populateViewHolder method.
 *
 *
 * <pre>
 * private static class ChatMessageViewHolder extends RecyclerView.ViewHolder {
 * TextView messageText;
 * TextView nameText;

 * public ChatMessageViewHolder(View itemView) {
 * super(itemView);
 * nameText = (TextView)itemView.findViewById(android.R.id.text1);
 * messageText = (TextView) itemView.findViewById(android.R.id.text2);
 * }
 * }

 * FirebaseRecyclerAdapter<ChatMessage></ChatMessage>, ChatMessageViewHolder> adapter;
 * DatabaseReference ref = FirebaseDatabase.getInstance().getReference();

 * RecyclerView recycler = (RecyclerView) findViewById(R.id.messages_recycler);
 * recycler.setHasFixedSize(true);
 * recycler.setLayoutManager(new LinearLayoutManager(this));

 * adapter = new FirebaseRecyclerAdapter<ChatMessage></ChatMessage>, ChatMessageViewHolder>(
 * ChatMessage.class, android.R.layout.two_line_list_item, ChatMessageViewHolder.class, ref) {
 * public void populateViewHolder(ChatMessageViewHolder chatMessageViewHolder,
 * ChatMessage chatMessage,
 * int position) {
 * chatMessageViewHolder.nameText.setText(chatMessage.getName());
 * chatMessageViewHolder.messageText.setText(chatMessage.getMessage());
 * }
 * };
 * recycler.setAdapter(mAdapter);
</pre> *

 * @param <T>  The Java class that maps to the type of objects stored in the Firebase location.
 * *
 * @param <VH> The ViewHolder class that contains the Views in the layout that is shown for each object.
</VH></T> */
abstract class FirebaseRecyclerAdapter<T, VH : RecyclerView.ViewHolder>
(
        private val mModelClass: Class<T>,
        protected  var mModelLayout: @param:LayoutRes Int,
        protected var mViewHolderClass: Class<VH>,
        private val mSnapshots: FirebaseArray
) : RecyclerView.Adapter<VH>() {

    init {

        mSnapshots.setOnChangedListener(object : FirebaseArray.OnChangedListener {
            override fun onChildChanged(type: FirebaseArray.OnChangedListener.EventType, index: Int, oldIndex: Int) {
                when (type) {
                    FirebaseArray.OnChangedListener.EventType.ADDED -> notifyItemInserted(index)
                    FirebaseArray.OnChangedListener.EventType.CHANGED -> notifyItemChanged(index)
                    FirebaseArray.OnChangedListener.EventType.REMOVED -> notifyItemRemoved(index)
                    FirebaseArray.OnChangedListener.EventType.MOVED -> notifyItemMoved(oldIndex, index)
                    else -> throw IllegalStateException("Incomplete case statement")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                this@FirebaseRecyclerAdapter.onCancelled(databaseError)
            }

            override fun onDataChanged(snap: DataSnapshot?) {
                this@FirebaseRecyclerAdapter.onDataChanged(snap)
            }
        })
    }

    /**
     * @param modelClass      Firebase will marshall the data at a location into
     * *                        an instance of a class that you provide
     * *
     * @param modelLayout     This is the layout used to represent a single item in the list.
     * *                        You will be responsible for populating an instance of the corresponding
     * *                        view with the data from an instance of modelClass.
     * *
     * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
     * *
     * @param ref             The Firebase location to watch for data changes. Can also be a slice of a location,
     * *                        using some combination of `limit()`, `startAt()`, and `endAt()`.
     */
    constructor(modelClass: Class<T>, modelLayout: Int, viewHolderClass: Class<VH>, ref: AbstractQuery)
            : this(modelClass, modelLayout, viewHolderClass, FirebaseArray(ref))

    open fun cleanup() = mSnapshots.cleanup()

    override fun getItemCount(): Int = mSnapshots.count

    fun getItem(position: Int): T = parseSnapshot(mSnapshots.getItem(position))

    /**
     * This method parses the DataSnapshot into the requested type. You can override it in subclasses
     * to do custom parsing.

     * @param snapshot the DataSnapshot to extract the model from
     * *
     * @return the model extracted from the DataSnapshot
     */
    protected fun parseSnapshot(snapshot: DataSnapshot): T = snapshot.getValue(mModelClass)

    fun getRef(position: Int): DatabaseReference = mSnapshots.getItem(position).ref

    // http://stackoverflow.com/questions/5100071/whats-the-purpose-of-item-ids-in-android-listview-adapter
    override fun getItemId(position: Int): Long = mSnapshots.getItem(position).key.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        try {
            val constructor = mViewHolderClass.getConstructor(View::class.java)
            return constructor.newInstance(view)
        } catch (e: Exception) {
            when (e) {
                is NoSuchMethodException,
                is InvocationTargetException,
                is InstantiationException,
                is IllegalAccessException -> throw RuntimeException(e)

                else -> throw e
            }
        }

    }

    override fun onBindViewHolder(viewHolder: VH, position: Int)
            = populateViewHolder(viewHolder, getItem(position), position)

    override fun getItemViewType(position: Int) = mModelLayout

    /**
     * This method will be triggered each time updates from the database have been completely processed.
     * So the first time this method is called, the initial data has been loaded - including the case
     * when no data at all is available. Each next time the method is called, a complete update (potentially
     * consisting of updates to multiple child items) has been completed.
     *
     *
     * You would typically override this method to hide a loading indicator (after the initial load) or
     * to complete a batch update to a UI element.
     */
    protected open fun onDataChanged(snap:DataSnapshot?) {}

    /**
     * This method will be triggered in the event that this listener either failed at the server,
     * or is removed as a result of the security and Firebase Database rules.

     * @param error A description of the error that occurred
     */
    protected fun onCancelled(error: DatabaseError) = Log.w(TAG, error.toException())

    /**
     * Each time the data at the given Firebase location changes,
     * this method will be called for each item that needs to be displayed.
     * The first two arguments correspond to the mLayout and mModelClass given to the constructor of
     * this class. The third argument is the item's position in the list.
     *
     *
     * Your implementation should populate the view using the data contained in the model.

     * @param viewHolder The view to populate
     * *
     * @param model      The object containing the data used to populate the view
     * *
     * @param position   The position in the list of the view being populated
     */
    protected abstract fun populateViewHolder(viewHolder: VH, model: T, position: Int)

    companion object {
        private val TAG = FirebaseRecyclerAdapter::class.java.simpleName
    }
}
