package it.polito.mad.sharenbook;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.UserInterface;

public class TabbedShowProfileActivity extends AppCompatActivity
        implements MaterialSearchBar.OnSearchActionListener{

    /* Views */
    private BottomNavigationView navBar;
    private FloatingActionButton goEdit_button;
    private CircularImageView userPicture;
    private MaterialSearchBar searchBar;
    private TabLayout tabLayout;
    private TextView tv_userFullName, tv_userNickName;

    /* User data*/
    private UserProfile user;
    private String profile_picture_signature;
    private String default_picture_timestamp;

    /* TabView vars*/
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    /* Other vars */
    private static final int EDIT_RETURN_VALUE = 1;
    private String searchState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed_show_profile);

        default_picture_timestamp = App.getContext().getResources().getString(R.string.default_picture_path);

        getViews();

        /* Take user data from the bundle */
        Bundle data;
        if (savedInstanceState == null) //ShowProfile is started by SplashActivity
            data = getIntent().getExtras();
        else                            //otherwise landscape -> portrait or viceversa
            data = savedInstanceState;

        user = data.getParcelable(getString(R.string.user_profile_data_key));

        /*
         * set Profile data info
         */
        setProfileData();


        //Adapter for viewPager
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        tabLayout.setTabTextColors(getResources().getColor(R.color.secondaryText), getResources().getColor(R.color.white));

        /*
         * goEdit_Button
         */
        goEdit_button.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), user);
            startActivityForResult(i, EDIT_RETURN_VALUE);
        });

        searchBar.setOnSearchActionListener(TabbedShowProfileActivity.this);

        // Setup navbar
        UserInterface.setupNavigationBar(this, 0, true);

        searchStatusCheck(data);

    }

    /** get references to UI elements */
    private void getViews(){
        tv_userFullName = findViewById(R.id.tv_userFullName);
        tv_userNickName = findViewById(R.id.tv_userNickName);
        goEdit_button = findViewById(R.id.fab_edit);
        navBar = findViewById(R.id.navigation);
        userPicture = findViewById(R.id.userPicture);
        searchBar =  findViewById(R.id.searchBar);
        mViewPager = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabs);
    }

    /** Set profile data information */
    private void setProfileData(){
        UserInterface.TextViewFontResize(user.getFullname().length(), getWindowManager(), tv_userFullName);
        tv_userFullName.setText(user.getFullname());
        String tv_username = getString(R.string.tv_username, user.getUsername());
        tv_userNickName.setText(tv_username);

        //Set profile picture
        profile_picture_signature = user.getPicture_timestamp();

        if (!profile_picture_signature.equals(default_picture_timestamp)) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/"+user.getUsername()+".jpg");
            UserInterface.showGlideImage(getApplicationContext(), storageRef, userPicture,  Long.valueOf(profile_picture_signature));

            userPicture.setOnClickListener(v -> {
                Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                i.putExtra("PictureSignature", profile_picture_signature);
                i.putExtra("username", user.getUsername());
                startActivity(i);
            });
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private ShowUserInfo fragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("userData", user);
                    fragment = new ShowUserInfo();
                    fragment.setArguments(bundle);
                    return fragment;
                case 1:
                    //TODO take to review fragment (da fare in kotlin)
                    bundle = new Bundle();
                    bundle.putParcelable("userData", user);
                    fragment = new ShowUserInfo();
                    fragment.setArguments(bundle);
                    return fragment;
                default:
                    return null;
            }

        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private void searchStatusCheck(Bundle data){
        if(data.getString("searchState")!=null) {
            searchState = data.getString("searchState");
            if (searchState.equals("enabled")) {
                navBar.setVisibility(View.GONE);
                goEdit_button.setVisibility(View.GONE);
            } else {
                navBar.setVisibility(View.VISIBLE);
                goEdit_button.setVisibility(View.VISIBLE);
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState); //the activity is going to be destroyed I need to save user
        outState.putParcelable(getString(R.string.user_profile_data_key), user);
        outState.putString("searchState",searchState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchState = savedInstanceState.getString("searchState");
    }


    /**
     * onActivityResult callback
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        String default_picture_path = "void";

        if (requestCode == EDIT_RETURN_VALUE) {

            if (resultCode == RESULT_OK) {

                Bundle userData = data.getExtras();
                user = userData.getParcelable(getString(R.string.user_profile_data_key));

                profile_picture_signature = user.getPicture_timestamp();


                if (!profile_picture_signature.equals(default_picture_path)) {

                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/"+user.getUsername()+".jpg");

                    UserInterface.showGlideImage(getApplicationContext(), storageRef, userPicture,  Long.valueOf(profile_picture_signature));

                    userPicture.setOnClickListener(v -> {
                        Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                        i.putExtra("PictureSignature", profile_picture_signature);
                        i.putExtra("username", user.getUsername());
                        startActivity(i);
                    });

                }


                /*
                 * set texts
                 */
                UserInterface.TextViewFontResize(user.getFullname().length(), getWindowManager(), tv_userFullName);
                tv_userFullName.setText(user.getFullname());
                String tv_username = getString(R.string.tv_username, user.getUsername());
                tv_userNickName.setText(tv_username);

                //Update the User Info Fragment
                mSectionsPagerAdapter.notifyDataSetChanged();

            }
        }
    }


    @Override
    public void onSearchStateChanged(boolean enabled) {
        searchState = enabled ? "enabled" : "disabled";
        Log.d("debug", "Search " + searchState);
        if( searchState.equals("enabled")) {
            navBar.setVisibility(View.GONE);
            goEdit_button.setVisibility(View.GONE);
        }
        else {
            navBar.setVisibility(View.VISIBLE);
            goEdit_button.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode){
            case MaterialSearchBar.BUTTON_NAVIGATION:
                //drawer.openDrawer(Gravity.START);
                break;
            case MaterialSearchBar.BUTTON_SPEECH:
                break;
            case MaterialSearchBar.BUTTON_BACK:
                searchBar.disableSearch();
                break;
        }

    }

    //send intent to SearchActivity
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        startSearchActivity(searchInputText);
    }

    /**
     * Starts the search activity with the appropriate bundle
     */
    private void startSearchActivity(CharSequence searchInputText){
        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
        if(searchInputText!=null)
            i.putExtra("searchInputText",searchInputText);
        startActivity(i);
    }

}
