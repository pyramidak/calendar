package cz.pyramidak.kalendar.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class TextFirebase {

    //Jak filtrovat data
//        val found = ref.orderByChild("name").equalTo("Ivo").limitToFirst(1).addValueEventListener(object: ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val record = snapshot.recordsZaznam()[0]
//            }
//            override fun onCancelled(error: DatabaseError) {}
//        })

    val user: FirebaseUser? =  Firebase.auth.currentUser
    val database = Firebase.database("https://stolni-kalendar-default-rtdb.europe-west1.firebasedatabase.app").reference

    fun isUserSigned(): Boolean {
        return (user != null)
    }

    fun write(record: Odber, records: List<Odber>) {

        //Vloží celou kolekci pod jediný KEY_ID
        database.child("graf").push().setValue(records)

        //Nejrychlejší uložení nového záznamu pod nový KEY_ID
        database.child("graf").push().setValue(record)

        //Podrobné přidání i do více tabulek najednou
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val key = database.child("graf").push().key
                if (key == null) {
                    Log.w("PUSH", "Couldn't get push key for posts")
                } else {
//                    val pushValues = record.toMap()
//                    val childUpdates = hashMapOf<String, Any>("/graf/${key}" to pushValues,
//                                                             "/user/${key}" to pushValues)
//                    database.updateChildren(childUpdates)
                    Log.w("PUSH", "Data added.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("PUSH", "Fail to add data $error")
            }
        })
    }

    fun readItem(key: String) {
        database.child("graf").child(key).get().addOnSuccessListener {
            it.getValue(Odber::class.java)?.name
            Log.i("firebase", "Got value ${it.getValue(Odber::class.java)?.name}")
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }

    fun readTable(table: String) {
        database.child("graf").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (one in dataSnapshot.children) {
                    val record = one.getValue(Odber::class.java)
                    readItem(one.key!!)
                    Log.d("GET", "Value is: ${record?.name}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("GET", "Failed to read value.", error.toException())
            }
        })
    }

    fun deleteTable(table: String) {
        database.child(table).removeValue()
    }

    fun deleteItem(item: String) {
        database.child(item).removeValue()
    }


}