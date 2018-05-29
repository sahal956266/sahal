package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.onesignal.OneSignal;

import java.util.Map;
import java.util.Set;

import it.polito.mad.sharenbook.adapters.ConversationAdapter;
import it.polito.mad.sharenbook.model.Conversation;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MyChatsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView navigationView;
    private BottomNavigationView navBar;
    private SharedPreferences userPreferences;
    private ListView chatsListView;
    private ConversationAdapter adapter;
    private DatabaseReference mychatsDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chats);
        setupNavigationTools();
        chatsListView = findViewById(R.id.my_chats_lv);

        adapter = new ConversationAdapter(MyChatsActivity.this);
        chatsListView.setAdapter(adapter);


        userPreferences = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        String username = userPreferences.getString(getString(R.string.username_copy_key), "void");

        if (!username.equals("void")){
            mychatsDB = FirebaseDatabase.getInstance().getReference("chats").child(username);


            /**take actual number of conversations **/

            mychatsDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long actualChats = dataSnapshot.getChildrenCount();

                    if(actualChats == 0)
                        noChatsDialog();

                  //  Log.d("Conversation:","actual chats size:"+actualChats);
                    adapter.setWithoutIncomingMessagesCounter(actualChats);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        mychatsDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
              //  Log.d("conversations added:", "->" + dataSnapshot.getKey() + "and" + s);
                /** Verificare getkey come una stringa concatenata di tutti gli utenti **/
                /** Altrimenti vedere se l'iterable ritornato da getChildren è sincrono
                 * e salvare tu**/

                dataSnapshot.getRef().orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                     //   Log.d("conversation add lmsg", dataSnapshot.toString());
                        setConversationAndApapter(dataSnapshot, true);


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
               // Log.d("conversations changed:", "->" + dataSnapshot.getKey() + "and" + s);
                dataSnapshot.getRef().orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                     //   Log.d("conversation chg lmsg", dataSnapshot.toString());
                        setConversationAndApapter(dataSnapshot, false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    }


    private void setConversationAndApapter(DataSnapshot dataSnapshot, boolean newConversation){
        /** Construct the message **/
        Message message = null;
        Map<String, Map<String,Object>> snapshot =( Map<String, Map<String,Object>>) dataSnapshot.getValue();
        Map<String,Object> messageMap;
        Set<String> key = snapshot.keySet();
        for(String s:key){
            messageMap = snapshot.get(s);
            message = new Message(messageMap.get("message").toString(),false,messageMap.get("user").toString(),
                    false,(long)messageMap.get("date_time"),MyChatsActivity.this);
            message.setViewed((boolean)messageMap.get("viewed"));
            if(message.isViewed())
                Log.d("conversation","added true");
            else
                Log.d("conversation","added false");
        }

        Conversation conversation = new Conversation(dataSnapshot.getKey(),message, 0,
                FirebaseStorage.getInstance().getReference().child("images/" + dataSnapshot.getKey() +".jpg"));

        if(newConversation) {
            adapter.addConversation(conversation);
            Log.d("chatpicture","new recipient->"+dataSnapshot.getKey()+" path->"+"images/" + dataSnapshot.getKey() +".jpg");
        }
        else {
            adapter.modifyConversation(conversation);
            Log.d("chatpicture","mod recipient->"+dataSnapshot.getKey()+" path->"+"images/" + dataSnapshot.getKey() +".jpg");
        }
    }

    private void noChatsDialog(){
        AlertDialog.Builder no_chats = new AlertDialog.Builder(MyChatsActivity.this); //give a context to Dialog
        no_chats.setTitle(R.string.no_chats_alert_title);
        no_chats.setMessage(R.string.no_chats_suggestion);
        no_chats.setPositiveButton(android.R.string.ok, (dialog, which) -> {

                    Intent i = new Intent (getApplicationContext(), ShowCaseActivity.class);
                    startActivity(i);


                }
        ).setNegativeButton(android.R.string.cancel,
                (dialog, which) -> {
                    dialog.dismiss();
                    
                }
        );

        no_chats.show();
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.mychats_title);
        }

        // Setup navigation drawer
        DrawerLayout drawer = findViewById(R.id.my_chats_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.my_chats_nav_view);
        navigationView.setNavigationItemSelectedListener(MyChatsActivity.this);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this,R.id.navigation_chat);
        navBar = findViewById(R.id.navigation);
    }


    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.drawer_navigation_profile){
            Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), NavigationDrawerManager.getUserParcelable(getApplicationContext()));
            startActivity(i);

        } else if (id == R.id.drawer_navigation_shareBook) {
            Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
            startActivity(i);

        } else if (id == R.id.drawer_navigation_myBook) {
            Intent i = new Intent(getApplicationContext(), MyBookActivity.class);
            startActivity(i);

        } else if (id == R.id.drawer_navigation_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> {
                        Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                        startActivity(i);
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        DrawerLayout drawer = findViewById(R.id.my_chats_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.my_chats_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        OneSignal.clearOneSignalNotifications();
        navBar.setSelectedItemId(R.id.navigation_chat);
    }
}

