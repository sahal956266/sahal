package it.polito.mad.sharenbook.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.database.*
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.adapters.BorrowRequestAdapter
import it.polito.mad.sharenbook.model.Book
import it.polito.mad.sharenbook.model.BorrowRequest

class BorrowRequestsFragment : Fragment() {

    internal lateinit var requestAdapter : BorrowRequestAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var requestsRef : DatabaseReference
    private lateinit var booksDb : DatabaseReference
    private lateinit var username : String
    private lateinit var childEventListener : ChildEventListener

    companion object {

        fun newInstance(): BorrowRequestsFragment {
            return BorrowRequestsFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAdapter = BorrowRequestAdapter(activity!!.supportFragmentManager)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        username = arguments!!.getString(getString(R.string.username_key))

        val rootView: View = inflater.inflate(R.layout.fragment_show_user_borrow_requests, container, false)
        linearLayoutManager = LinearLayoutManager(App.getContext())

        val recyclerView = rootView.findViewById<RecyclerView>(R.id.rv_borrow_requests) as RecyclerView
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = requestAdapter

        requestsRef = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("borrowRequests")
        booksDb = FirebaseDatabase.getInstance().getReference(getString(R.string.books_key))

        return rootView
    }

    override fun onResume() {
        super.onResume()

        requestAdapter.clearRequests()
        loadRequests()
        requestAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        requestsRef.removeEventListener(childEventListener)
    }



    fun loadRequests(){

        childEventListener = object : ChildEventListener {

            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

                val bookId = dataSnapshot.key

                booksDb.child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                        val book = dataSnapshot.getValue(Book::class.java)

                        val req = BorrowRequest(null, bookId, book!!.title, book.authorsAsString, book.getCreationTimeAsString(App.getContext()), 0, book.photosName[0], book.owner_username)

                        requestAdapter.addRequest(req)
                        requestAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                        Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show()

                    }
                })

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

        requestsRef.addChildEventListener(childEventListener)

    }
}