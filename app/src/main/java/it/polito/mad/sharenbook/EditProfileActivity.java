package it.polito.mad.sharenbook;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import android.widget.EditText;

import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import it.polito.mad.sharenbook.model.UserProfile;


public class EditProfileActivity extends Activity {

    //context of the activity
    private Context context;

    // request codes to edit user photo
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int MULTIPLE_PERMISSIONS = 3;

    //views
    private TextView tv_userFullNameHeading_edit, tv_userNameHeading_edit, tv_userEmailHeading_edit, tv_userCityHeading_edit, tv_userBioHeading_edit;
    private EditText et_userFullName, et_userNickName, et_userCity, et_userBio, et_userEmail;
    private FloatingActionButton save_button;

    /* user photo views */
    private com.mikhaellopez.circularimageview.CircularImageView userPicture;

    private String userPhotoPath;

    private FloatingActionButton fab_editPhoto;

    private Uri photoPathUri;
    private OutputStream out;

    private StorageReference storageReference;


    //preferences

    private SharedPreferences editedProfile_copy;
    private SharedPreferences.Editor writeProfile_copy;

    //default profile values
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;

    private String fullname;
    private String username;
    private String email;
    private String city;
    private String bio;

    //permissions needed
    private String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    //Regex for input validation
    private Pattern RFC822_email_regex = Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");
    private Pattern fullname_regex = Pattern.compile("([A-Z]([a-z])*[\\s][A-Za-z]([a-z])*[\\s]?)([A-Za-z]([a-z])*[\\s]?)*");
    private Pattern city_regex = Pattern.compile("([A-Z][a-z]*)[\\s]?[,]?[\\s]?([A-Z][a-z]*)?[\\s]?");

    //User profile data
    private UserProfile user;
    private Uri pictureURI = null;

    //FIREBASE
    private DatabaseReference firebaseDB;
    /**
     * onCreate callback
     *
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_profile);

        hasPermissions();

        //modify default typography
        getViewsAndSetTypography();

        context = this.getApplicationContext();

        //retrieve the default values if the profile is not yet edited
        default_city = context.getResources().getString(R.string.default_city);
        default_bio = context.getResources().getString(R.string.default_bio);
        default_email = context.getResources().getString(R.string.default_email);
        default_fullname = context.getResources().getString(R.string.default_fullname);
        default_username = context.getResources().getString(R.string.default_username);
        default_picture_path = context.getResources().getString(R.string.default_picture_path);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        storageReference = storage.getReference();


        Bundle data = getIntent().getExtras();
        user = data.getParcelable(getString(R.string.user_profile_data_key));
        Log.d("USERID:",user.getUserID());

        firebaseDB = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user.getUserID());

        //call to methods that implements the final part of onCreate
        if ((savedInstanceState == null) || (savedInstanceState.isEmpty())) { //first time make copies and visualize stable profile

            onCreateWithBundleEmpty(user);

        } else {
            onCreateWithBundleNotEmpty();
        }
    }


    /**
     * onCreateWithBundleEmpty method
     */
    private void onCreateWithBundleEmpty(UserProfile user) {



        //make a copy for rollbacks
        editedProfile_copy = context.getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        //get view button
        save_button = (FloatingActionButton) findViewById(R.id.fab_save);

       // String fullname;

        if(user.getFullname() != null && !user.getFullname().equals(default_fullname)) {
            fullname = user.getFullname();
            et_userFullName.setText(fullname);
        }
        else {
            fullname = default_fullname;
            et_userFullName.setHint(fullname);

        }

        //get editViews
        writeProfile_copy.putString(getString(R.string.fullname_copy_key), fullname).commit();


        //String username;

        if(user.getUsername() != null && !user.getUsername().equals(default_username)) {
            username = user.getUsername();
            et_userNickName.setText(username);
        }
        else {
            username = default_username;
            et_userNickName.setHint(username);
        }

        writeProfile_copy.putString(getString(R.string.username_copy_key), username).commit();


       // String city;

        if(user.getCity() != null && !user.getCity().equals(default_city)) {
            city = user.getCity();
            et_userCity.setText(city);
        }

        else {
            city = default_city;
            et_userCity.setHint(city);
        }

        writeProfile_copy.putString(getString(R.string.city_copy_key), city).commit();


       // String bio;

        if(user.getBio() != null && !user.getBio().equals(default_bio)) {
            bio = user.getBio();
            et_userBio.setText(bio);
        }
        else {
            bio = default_bio;
            et_userBio.setHint(bio);
        }
        writeProfile_copy.putString(getString(R.string.bio_copy_key), bio).commit();

/*
        if (bio.equals(default_bio))
            et_userBio.setHint(bio);
        else {
            et_userBio.setText(bio);
            et_userBio.setTextColor(Color.GRAY);
        }
        */

       // String email;
        if( user.getEmail()!=null && !user.getEmail().equals(default_email)) {
            email = user.getEmail();
            et_userEmail.setText(email);
        }
        else {
            email = default_email;
            et_userEmail.setHint(email);
        }
        writeProfile_copy.putString(getString(R.string.email_copy_key), email).commit();


        /* Edit photo section */
        userPicture = (com.mikhaellopez.circularimageview.CircularImageView) findViewById(R.id.userPicture_edit);

        String choosenPicture;

        if(user.getPicture_uri()!=null)
            choosenPicture = user.getPicture_uri().toString();
        else
            choosenPicture = default_picture_path;

        writeProfile_copy.putString(getString(R.string.userPicture_copy_key), choosenPicture).commit();

        Log.d("Gallery:", choosenPicture);
        if (!choosenPicture.equals(default_picture_path))
            Glide.with(getApplicationContext()).load(choosenPicture).into(userPicture);

        fab_editPhoto = (FloatingActionButton) findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setBackgroundDrawable(AppCompatResources.getDrawable(EditProfileActivity.this,R.drawable.ic_check_black_24dp));

        fab_editPhoto.setOnClickListener(v -> {
            selectImage();
        });
    }


    /**
     * onCreateWithBundleNotEmpty method
     */
    private void onCreateWithBundleNotEmpty() {

        

        //make a copy for rollbacks
        editedProfile_copy = context.getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        //get view button
        save_button = (FloatingActionButton) findViewById(R.id.fab_save);

        //set hints for views
        et_userFullName.setHint(editedProfile_copy.getString(getString(R.string.fullname_copy_key), default_fullname));
        et_userNickName.setHint(editedProfile_copy.getString(getString(R.string.username_copy_key), default_username));
        et_userCity.setHint(editedProfile_copy.getString(getString(R.string.city_copy_key), default_city));
        String actualBio = editedProfile_copy.getString(getString(R.string.bio_copy_key), default_bio);

        if (actualBio.equals(default_bio))
            et_userBio.setHint(actualBio);
        else {
            et_userBio.setText(actualBio);
            et_userBio.setTextColor(Color.GRAY);
        }

        et_userEmail.setHint(editedProfile_copy.getString(getString(R.string.email_copy_key), default_email));

        /* Edit photo section */
        userPicture = (com.mikhaellopez.circularimageview.CircularImageView) findViewById(R.id.userPicture_edit);
        String choosenPicture = editedProfile_copy.getString(getString(R.string.userPicture_copy_key), default_picture_path);

        Log.d("Gallery:", choosenPicture);
        if (!choosenPicture.equals(default_picture_path))
            userPicture.setImageURI(Uri.parse(choosenPicture));

        fab_editPhoto = (FloatingActionButton) findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setOnClickListener(v -> {
            selectImage();
        });
    }


    //this method will upload the file
    private void uploadFile(Uri file) {
        //if there is a file to upload
        Uri filePath = file;

        if (filePath != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            StorageReference riversRef = storageReference.child("images/"+user.getUserID()+".jpg");
            riversRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                            Toast.makeText(getApplicationContext(), "Profile has been saved!", Toast.LENGTH_LONG).show();

                            Intent i = new Intent (getApplicationContext(), ShowProfileActivity.class);
                            i.putExtra(getString(R.string.user_profile_data_key),user);
                            startActivity(i);
                            finish();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploading " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
        }
    }



    /**
     * hasPermissions method
     */
    private void hasPermissions() {

        int result;

        List<String> listPermissionsNeeded = new ArrayList<String>();

        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(EditProfileActivity.this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.permission_already_granted), Toast.LENGTH_LONG).show();
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
        }
    }


    /**
     * onRequestPermissionsResult method
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    Toast.makeText(getApplicationContext(), getString(R.string.permissions_granted), Toast.LENGTH_LONG).show();
                } else {

                    final List<String> neededPermissions = new ArrayList<String>();
                    for (String permission : permissions) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(EditProfileActivity.this, permission)) {
                            neededPermissions.add(permission);
                        }
                    }

                    if (!neededPermissions.isEmpty()) {

                        AlertDialog.Builder newPermissionRequest = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
                        newPermissionRequest.setTitle(R.string.new_permission_request_title);
                        newPermissionRequest.setMessage(R.string.permissions_rationale);
                        newPermissionRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    ActivityCompat.requestPermissions(EditProfileActivity.this, neededPermissions.toArray(new String[neededPermissions.size()]), MULTIPLE_PERMISSIONS);
                                }
                        ).setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> {
                                    finish();
                                }
                        );

                        newPermissionRequest.show();
                    }

                }

                return;

            }
        }
    }


    /**
     * selectImage method
     */
    private void selectImage() {

        final CharSequence items[] = {getString(R.string.photo_dialog_item_camera), getString(R.string.photo_dialog_item_gallery), getString(android.R.string.cancel)};
        final AlertDialog.Builder select = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
        select.setTitle(getString(R.string.photo_dialog_title));


        select.setItems(items, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (items[i].equals(getString(R.string.photo_dialog_item_camera))) {

                    Intent selfie = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if (selfie.resolveActivity(getPackageManager()) != null) {
                        /*
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String imageFileName = "JPEG_" + timeStamp + "_userProfile.jpg";
                        String imagePath = null;
                        String galleryPath = null;

                        InputStream in = null;
                        //OutputStream out = null;



                            //CameraResolution();
                        //File outFile=new File(editProfile.this.getExternalFilesDir(null),imageFileName);
                        File outFile = new File(editProfile.this.getExternalFilesDir(null), "user_pictures");
                        //      in = getContentResolver().openInputStream(Uri.parse(galleryPath));

                        try {
                            out = new FileOutputStream(outFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        imagePath=outFile.getAbsolutePath();

                        photoPathUri = Uri.parse(imagePath);
                        Log.d("PATH:",photoPathUri.toString());

                        selfie.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,photoPathUri.toString());

                        */
                        startActivityForResult(selfie, REQUEST_CAMERA);

                    }

                } else if (items[i].equals(getString(R.string.photo_dialog_item_gallery))) {

                    Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    if (gallery.resolveActivity(getPackageManager()) != null) {

                        gallery.setType("image/*");
                        startActivityForResult(Intent.createChooser(gallery, getString(R.string.photo_dialog_select_gallery_method_title)), REQUEST_GALLERY);
                    }

                } else if (items[i].equals(getString(android.R.string.cancel))) {

                    dialogInterface.dismiss();
                }
            }
        });

        select.show();
    }


    /**
     * onActivityResult method
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) {

                if (data == null)
                    Log.d("DATA:", "NULL");

                saveCameraPhoto(data);


            } else if (requestCode == REQUEST_GALLERY) {

                saveGalleryPhoto(data);

            }
        }
    }

    /**
     * saveCameraPhoto method
     *
     * @param data
     */
    private void saveCameraPhoto(Intent data) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_userProfile.jpg";
        String imagePath = null;
        String galleryPath = null;

        InputStream in = null;
        OutputStream out = null;

        File outFile;

        Bundle resultImage = data.getExtras();
        Bitmap resultBMP = (Bitmap) resultImage.get("data");
        //CameraResolution(resultBMP);

        try {

            //save image in gallery
            //galleryPath = MediaStore.Images.Media.insertImage(getContentResolver(), resultBMP, imageFileName, "user profile image");

            outFile = new File(EditProfileActivity.this.getExternalFilesDir(null), imageFileName);

            //      in = getContentResolver().openInputStream(Uri.parse(galleryPath));

            out = new FileOutputStream(outFile);

            imagePath = outFile.getAbsolutePath();

            resultBMP.compress(Bitmap.CompressFormat.JPEG, 100, out);

            /*

            byte [] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0) {
                out.write(buf, 0, len);
            }
            */
            pictureURI = Uri.parse(imagePath);

            out.close();
            //in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }

        /*
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPathUri.toString(), options);
        final int REQUIRED_SIZE = 200;
        int scale = 1;
        while (options.outWidth / scale / 2 >= REQUIRED_SIZE
                && options.outHeight / scale / 2 >= REQUIRED_SIZE)
            scale *= 2;
        options.inSampleSize = scale;
        options.inJustDecodeBounds = false;
        resultBMP = BitmapFactory.decodeFile(photoPathUri.toString(), options);
        */
        userPicture.setImageBitmap(resultBMP);

        // userPicture.setImageURI(photoPathUri);
        //save uri path for persistence
        writeProfile_copy.putString(getString(R.string.userPicture_copy_key), imagePath).commit();
    }


    /**
     * saveGalleryPhoto method
     *
     * @param data
     */
    private void saveGalleryPhoto(Intent data) {

        String imagePath = null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_userProfile.jpg";
        File outFile;

        InputStream in = null;
        OutputStream out = null;
        try {


            outFile = new File(EditProfileActivity.this.getExternalFilesDir(null), imageFileName);

            in = getContentResolver().openInputStream(data.getData());

            out = new FileOutputStream(outFile);


            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            imagePath = outFile.getAbsolutePath();

            pictureURI = data.getData();    //picture URI to be saved on firebase

            out.close();
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        userPicture.setImageURI(Uri.parse(imagePath));

        writeProfile_copy.putString(getString(R.string.userPicture_copy_key), imagePath).commit();

    }


    /**
     * onResume method
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*               edit text             */
        setEditTextListeners(et_userFullName, R.string.fullname_key, R.string.fullname_copy_key);
        setEditTextListeners(et_userNickName, R.string.username_key, R.string.username_copy_key);
        setEditTextListeners(et_userCity, R.string.city_key, R.string.city_copy_key);
        setEditTextListeners(et_userBio, R.string.bio_key, R.string.bio_copy_key);
        setEditTextListeners(et_userEmail, R.string.email_key, R.string.email_copy_key);

        //save on click

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (!validateForm())
                    return;

                firebaseSaveProfile();

            }
        });

    }

    private void firebaseSaveProfile(){

        Map<String,Object> userData = new HashMap<>();
        if(et_userFullName.getText().length() != 0 && !et_userFullName.getText().equals(default_fullname)) {
            userData.put(getString(R.string.fullname_key), et_userFullName.getText().toString());
            user.setFullname(et_userFullName.getText().toString());
        }
        if(et_userNickName.getText().length() != 0 && !et_userNickName.getText().equals(default_username)) {
            userData.put(getString(R.string.username_key), et_userNickName.getText().toString());
            user.setUsername(et_userNickName.getText().toString());
        }
        if (et_userEmail.getText().length() != 0 && !et_userEmail.getText().equals(default_email)) {
            userData.put(getString(R.string.email_key), et_userEmail.getText().toString());
            user.setEmail(et_userEmail.getText().toString());
        }
        if(et_userCity.getText().length()!=0 && !et_userCity.getText().equals(default_city)) {
            userData.put(getString(R.string.city_key), et_userCity.getText().toString());
            user.setCity(et_userCity.getText().toString());
        }
        if (et_userBio.getText().length() != 0 && !et_userBio.getText().equals(default_bio)) {
            userData.put(getString(R.string.bio_key), et_userBio.getText().toString());
            user.setBio(et_userBio.getText().toString());
        }


        writeProfile_copy.clear().commit();


        firebaseDB.child(getString(R.string.profile_key)).updateChildren(userData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if(databaseError == null){

                    /**
                     * Check if profile picture has been changed or not
                     */
                    if(pictureURI != null) {

                        uploadFile(pictureURI);
                        user.setPicture_uri(pictureURI); //TODO modify this part -> URL not local URI!

                    } else {
                        /**
                         * The user has not changed the profile picture
                         */
                        user.setPicture_uri(user.getPicture_uri());
                        Toast.makeText(getApplicationContext(), "Profile has been saved!", Toast.LENGTH_LONG).show();

                        Intent i = new Intent (getApplicationContext(), ShowProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key),user);
                        startActivity(i);
                        finish();

                    }

                }

            }
        });


    }


    /**
     * validateForm method
     *
     * @return
     */
    private boolean validateForm() {
        boolean result = true;



        if (!(et_userFullName.getText().toString().equals(fullname)) && !(et_userFullName.getText().toString().isEmpty())) {
            if ((et_userFullName.getText().toString().length()) < 5) {
                et_userFullName.setError(getString(R.string.name_length_format_rationale));
                result = false;

            } else if (!fullname_regex.matcher(et_userFullName.getText().toString()).matches()) {
                et_userFullName.setError(getString(R.string.name_bad_format_rationale));
                result = false;
            } else {
                et_userFullName.setError(null);
            }

        }else if(et_userFullName.getText().toString().isEmpty()){
            et_userFullName.setError(getString(R.string.required_field));
            result = false;
        }

        if (!(et_userNickName.getText().toString().equals(username)) && !(et_userNickName.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userNickName.getText().toString())) < 2) {
                et_userNickName.setError(getString(R.string.username_bad_lenght_rationale));
                result = false;
            } else if (!(et_userNickName.getText().toString().startsWith("@"))) {
                et_userNickName.setError(getString(R.string.username_bad_start_rationale));
                result = false;
            } else {
                et_userNickName.setError(null);
            }

        }else if(et_userNickName.getText().toString().isEmpty()){
            et_userNickName.setError(getString(R.string.required_field));
            result = false;
        }


        if (!(et_userEmail.getText().toString().equals(email)) && !(et_userEmail.getText().toString().isEmpty())) { //the mail has been changed
            Log.d("Email:", et_userEmail.getText().toString());

            if (!RFC822_email_regex.matcher(et_userEmail.getText().toString()).matches()) {
                et_userEmail.setError(getString(R.string.bad_email));
                result = false;

            } else {
                et_userEmail.setError(null);
            }
        }else if(TextUtils.isEmpty(et_userEmail.getText().toString())){
            et_userEmail.setError(getString(R.string.required_field));
            result = false;
        }

        if (!(et_userCity.getText().toString().equals(city)) && !(et_userCity.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userCity.getText().toString())) < 2) {
                et_userCity.setError(getString(R.string.city_bad_lenght_rationale));
                result = false;
            } else if (!(city_regex.matcher(et_userCity.getText().toString()).matches())) {
                et_userCity.setError(getString(R.string.city_bad_format_rationale));
                result = false;
            } else {
                et_userCity.setError(null);
            }


        }else if(TextUtils.isEmpty(et_userCity.getText().toString())){
            et_userCity.setError(getString(R.string.required_field));
            result = false;
        }
        /*
        if(!(et_userBio.getText().toString().equals(editedProfile.getString(getString(R.string.bio_key),default_bio)))&&!(et_userBio.getText().toString().isEmpty())){

            if(!(bio_regex.matcher(et_userBio.getText().toString()).matches()) ){
                et_userBio.setError(getString(R.string.bio_bad_format_rationale));
                result = false;
            }
            else {
                et_userBio.setError(null);
            }


        }
        */
        if(TextUtils.isEmpty(et_userBio.getText().toString())){
            et_userBio.setError(getString(R.string.required_field));
            result = false;
        }

        return result;
    }


    /**
     * setEditTextListeners method
     *
     * @param et
     * @param key
     * @param keycopy
     */
    private void setEditTextListeners(final EditText et, final int key, final int keycopy) {

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (et.equals(et_userBio))
                    et.setTextColor(Color.DKGRAY);
                // et.setTypeface(robotoLight);

                writeProfile_copy.putString(getString(keycopy), et.getText().toString()).commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle save) {
        super.onSaveInstanceState(save);
        save.putString("Flag_changes", "true");
    }


    /**
     * onBackPressed method
     */
    @Override
    public void onBackPressed() {

        AlertDialog.Builder exitRequest = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
        exitRequest.setTitle(R.string.exit_request_title);
        exitRequest.setMessage(R.string.exit_rationale);
        exitRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    writeProfile_copy.clear().commit();
            //Intent i = new Intent (getApplicationContext(), ShowProfileActivity.class);
            //startActivity(i);
            finish();

                }
        ).setNegativeButton(android.R.string.cancel,
                (dialog, which) -> {
                    dialog.dismiss();
                }
        );

        exitRequest.show();
    }


    /**
     * getViewsAndSetTypography method
     */
    private void getViewsAndSetTypography() {

        //get views
        tv_userFullNameHeading_edit = (TextView) findViewById(R.id.tv_userFullNameHeading_edit);
        tv_userNameHeading_edit = (TextView) findViewById(R.id.tv_userNameHeading_edit);
        tv_userCityHeading_edit = (TextView) findViewById(R.id.tv_userCityHeading_edit);
        tv_userBioHeading_edit = (TextView) findViewById(R.id.tv_userBioHeading_edit);
        tv_userEmailHeading_edit = (TextView) findViewById(R.id.tv_userEmailHeading_edit);

        et_userFullName = (EditText) findViewById(R.id.et_fullNameContent_edit);
        et_userNickName = (EditText) findViewById(R.id.et_userNameContent_edit);
        et_userCity = (EditText) findViewById(R.id.et_userCityContent_edit);
        et_userBio = (EditText) findViewById(R.id.et_userBioContent_edit);
        et_userEmail = (EditText) findViewById(R.id.et_emailContent_edit);

        //retrieve fonts
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        /**
         * set views font and view text
         */

        //headings
        tv_userFullNameHeading_edit.setTypeface(robotoBold);
        tv_userNameHeading_edit.setTypeface(robotoBold);
        tv_userCityHeading_edit.setTypeface(robotoBold);
        tv_userBioHeading_edit.setTypeface(robotoBold);
        tv_userEmailHeading_edit.setTypeface(robotoBold);

        //edit texts
        et_userFullName.setTypeface(robotoLight);
        et_userNickName.setTypeface(robotoLight);
        et_userCity.setTypeface(robotoLight);
        et_userBio.setTypeface(robotoLight);
        et_userEmail.setTypeface(robotoLight);
    }

}