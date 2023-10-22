package cz.pyramidak.kalendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import cz.pyramidak.kalendar.databinding.AdapterOdberBinding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.pyramidak.kalendar.firebase.*

class OdberAdapter(val items: List<Odber>, val activity: MainActivity) : RecyclerView.Adapter<OdberAdapter.MyViewHolder>() {

    class MyViewHolder(v: View, val binding: AdapterOdberBinding) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = AdapterOdberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding.root, binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, index: Int) {
        val item = getItem(index)
        holder.binding.apply {
            rbOwn.isChecked = item.own
            tvName.text = item.name
            tvKey.text = item.key

            holder.itemView.setOnClickListener{
                //copy to clipboard
                //val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                //val clip = ClipData.newPlainText("Table calendar connection key", item.key)
                //clipboard.setPrimaryClip(clip)
                //activity.showMessage(activity.getString(R.string.keyInClipboard))
                val intent = Intent(activity, QRcodeActivity::class.java)
                intent.putExtra("CODE", item.key)
                activity.startActivity(intent)
            }

            holder.itemView.setOnLongClickListener{
                activity.run {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle(activity.getString(R.string.connectionDelete))
                    builder.setMessage(activity.getString(R.string.sureToDeleteConnection) + "\n" + item.name.toUpperCase() + "?")
                    builder.setPositiveButton(activity.getString(R.string.yes)){_, _ ->
                        if (item.own) { MyFirebase("kalendar/odbery").ref.child(item.key!!).removeValue() }
                        MyFirebase("kalendar/users").refWithUser.child("odbery").child(item.key!!).removeValue()
                    }
                    builder.setNegativeButton(activity.getString(R.string.no)){_, _ -> }
                    builder.show()
                }
                false
            }

            val eventListener = object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    rbConnected.isChecked = dataSnapshot.record<Extra>()?.connected ?:false
                }
                override fun onCancelled(error: DatabaseError) {
                    activity.showMessage("")
                }
            }
            val state = MyFirebase("kalendar/odbery/${item.key}/state")
            state.ref.addValueEventListener(eventListener as ValueEventListener)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(index: Int): Odber = items[index]
}