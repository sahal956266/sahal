package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.onesignal.OneSignal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import it.polito.mad.sharenbook.adapters.MessageAdapter;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.UserInterface;


public class ChatActivity extends AppCompatActivity {

    ListView messageView;
    ImageView sendButton;
    EditText messageArea;
    DatabaseReference chatToOthersReference, chatFromOthersReference;
    public static String recipientUsername;
    ImageView iv_profile;
    TextView tv_username;

    private boolean lastMessageNotFromCounterpart = false;
    private boolean activityWasOnPause =false, isOnPause = false;
    private String username, userID;

    public static boolean chatOpened = false;
    private boolean openedFromNotification;
    private boolean firstTimeNotViewed = true;

    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference serverTimeRef;

    private ChildEventListener childEventListener;
    private ValueEventListener readServerTime;

    /** adapter setting **/

    private MessageAdapter messageAdapter;

    private long unixTime;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        intent.putExtra("openedFromNotification", false);
        setIntent(intent);
        this.recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageView = findViewById(R.id.chat_list_view);
        sendButton = findViewById(R.id.sendButton);
        messageArea = findViewById(R.id.messageArea);
        iv_profile = findViewById(R.id.iv_profile);
        tv_username = findViewById(R.id.tv_username);

        recipientUsername = getIntent().getStringExtra("recipientUsername");
        //recipientUID = getIntent().getStringExtra("recipientUID");
        /*SharedPreferences userPreferences = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        userPreferences.edit().putString(recipientUsername,recipientUID).commit();*/
        openedFromNotification = getIntent().getBooleanExtra("openedFromNotification", false);

        tv_username.setText(recipientUsername);

        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        userID = firebaseUser.getUid();

        //show recipient profile pic
        StorageReference profilePicRef = FirebaseStorage.getInstance().getReference().child("images/" + recipientUsername +".jpg");
        profilePicRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                UserInterface.showGlideImage(getApplicationContext(),profilePicRef, iv_profile, 0 );
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // File not found
            }
        });


        /** create and set adapter **/
        messageAdapter = new MessageAdapter(ChatActivity.this,profilePicRef);
        messageView.setAdapter(messageAdapter);

        chatToOthersReference = FirebaseDatabase.getInstance().getReference("chats").child(username).child(recipientUsername);
        chatFromOthersReference = FirebaseDatabase.getInstance().getReference("chats").child(recipientUsername).child(username);

        serverTimeRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://sharenbook-debug.firebaseio.com/server_timestamp");

        readServerTime = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                unixTime = (Long) dataSnapshot.getValue() + 70000;
                System.out.println("current time: "+ unixTime);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        serverTimeRef.addListenerForSingleValueEvent(readServerTime);
        serverTimeRef.setValue(ServerValue.TIMESTAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                String messageBody = map.get("message").toString();
                String userName = map.get("user").toString();
                Boolean viewed = (Boolean) map.get("viewed");

                long date = 0;
                if(map.get("date_time")!=null){
                    date = (long)map.get("date_time");
                }
                Message message;
                //MESSAGE ADD -> MESSAGE string message, int type, string username
                if(userName.equals(username)){

                    message = new Message(messageBody,true, userName, lastMessageNotFromCounterpart, date, ChatActivity.this);
                    messageAdapter.addMessage(message);
                    messageView.setSelection(messageView.getCount() - 1);
                    lastMessageNotFromCounterpart = false;

                }
                else{

                    if(!viewed){

                        if((date < unixTime && firstTimeNotViewed) || activityWasOnPause || (openedFromNotification && firstTimeNotViewed)){
                            message = new Message(null, true, null, lastMessageNotFromCounterpart, 0, ChatActivity.this);
                            messageAdapter.addMessage(message);
                            firstTimeNotViewed = false;
                            activityWasOnPause = false;
                        }

                        map.put("viewed", true);
                        dataSnapshot.getRef().updateChildren(map);

                    }

                    message = new Message(messageBody,false,userName, lastMessageNotFromCounterpart, date, ChatActivity.this);

                    lastMessageNotFromCounterpart = true;

                    messageAdapter.addMessage(message);
                    messageView.setSelection(messageView.getCount() - 1);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
        };
        chatToOthersReference.addChildEventListener(childEventListener);

        sendButton.setOnClickListener(v -> {
            String messageText = messageArea.getText().toString();

            if(!messageText.equals("")){
                Map<String,Object> map = new HashMap<>();
                map.put("message", messageText);
                map.put("user", username);
                map.put("date_time", ServerValue.TIMESTAMP);
                map.put("viewed", true);
                sendNotification(recipientUsername, username);
                chatToOthersReference.push().setValue(map);
                Map<String,Object> map2 = new HashMap<>();
                map2.put("message", messageText);
                map2.put("user", username);
                map2.put("date_time", ServerValue.TIMESTAMP);
                map2.put("viewed", false);
                chatFromOthersReference.push().setValue(map2);
                messageArea.setText("");
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        chatOpened = true;

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatOpened = false;
    }



    public void sendNotification(String destination, String sender){
        AsyncTask.execute(() -> {
            int SDK_INT = Build.VERSION.SDK_INT;
            if(SDK_INT > 8){
                Log.d("notification", "sending to " + destination);
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                try{
                    String jsonResponse;

                    URL url = new URL("https://onesignal.com/api/v1/notifications");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Authorization", "Basic ZTc3MjExODEtYmM4Yy00YjU5LWFjNWEtM2VlNGNmYTA0OWU1");
                    conn.setRequestMethod("POST");

                    String strJsonBody = "{"
                            + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                            + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + destination + "\"}],"

                            + "\"data\": {\"notificationType\": \"message\", \"senderName\": \"" + sender + "\", \"senderUid\": \"" + userID + "\"},"
                            + "\"contents\": {\"en\": \"" + sender + " sent you a message!\", " +
                                             "\"it\": \"" + sender + " ti ha inviato un messaggio!\"},"
                            + "\"headings\": {\"en\": \"New message!\", \"it\": \"Nuovo messaggio!\"}"
                            + "}";

                    System.out.println("strJsonBody:" + strJsonBody);

                    byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                    conn.setFixedLengthStreamingMode(sendBytes.length);

                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(sendBytes);

                    int httpResponse = conn.getResponseCode();
                    System.out.println("httpResponse: " + httpResponse);

                    if (httpResponse >= HttpURLConnection.HTTP_OK
                            && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                        Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    } else {
                        Scanner scanner = new Scanner(conn.getErrorStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    }
                    System.out.println("jsonResponse:\n" + jsonResponse);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(openedFromNotification){
            /*Intent i = new Intent (getApplicationContext(), MyChatsActivity.class);
            startActivity(i);*/
        }
        chatToOthersReference.removeEventListener(childEventListener);
        finish();
    }

    @Override
    protected void onPause() {
        chatToOthersReference.removeEventListener(childEventListener);
        isOnPause = true;
        activityWasOnPause = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        if(isOnPause) {
            serverTimeRef.addListenerForSingleValueEvent(readServerTime);
            serverTimeRef.setValue(ServerValue.TIMESTAMP);
            messageAdapter.clearMessages();
            isOnPause = false;
            chatToOthersReference.addChildEventListener(childEventListener);
        }
        super.onResume();
    }
}
