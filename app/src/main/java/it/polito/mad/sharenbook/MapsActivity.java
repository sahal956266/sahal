package it.polito.mad.sharenbook;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.model.SearchResults;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.CustomInfoWindowAdapter;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, MaterialSearchBar.OnSearchActionListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private ArrayList<Book> searchResult;

    private MaterialSearchBar sba_searchbar;

    //fab to display map
    FloatingActionButton search_fab_list;

    // Algolia instant search
    Searcher searcher;
    InstantSearch helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        if(!extras.isEmpty()) {
            searchResult = extras.getParcelableArrayList("SearchResults");
        }

        //Algolia's InstantSearch setup
        searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
        helper = new InstantSearch(searcher);

        searcher.registerResultListener((results, isLoadingMore) -> {

            if (results.nbHits > 0) {

                if(searchResult == null){
                    searchResult = new ArrayList<>();
                }

                searchResult.clear();
                searchResult.addAll(parseResults(results.hits));
                showSearchResults();
                sba_searchbar.disableSearch();

            } else {
                Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            }
        });

        searcher.registerErrorListener((query, error) -> {

            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            Log.d("error", "Unable to retrieve search result from Algolia");
        });

        setListButton();

        setSeachBar();
    }


    /**
     * Fire the list view mode
     */
    private void setListButton() {

        search_fab_list = findViewById(R.id.search_fab_list);

        search_fab_list.setOnClickListener((v) -> {
            Intent listSearch = new Intent(getApplicationContext(), SearchActivity.class);
            if(!searchResult.isEmpty()){
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("SearchResults", searchResult);
                listSearch.putExtras(bundle);
            }
            startActivity(listSearch);
            finish();
        });
    }


    private void setSeachBar(){
        sba_searchbar = findViewById(R.id.sba_searchbar);

        sba_searchbar.setOnSearchActionListener(MapsActivity.this);
        sba_searchbar.enableSearch();
    }


    /**
     * JSON Parser: from JSON to Book
     *
     * @param jsonObject : json representation of the book stored in algolia's "books" index
     * @return : the Book object
     */
    public Book BookJsonParser(JSONObject jsonObject) {

        String bookId = jsonObject.optString("bookId");
        String owner_uid = jsonObject.optString("owner_uid");
        String isbn = jsonObject.optString("isbn");
        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        //authors
        ArrayList<String> authors = new ArrayList<>();

        try {

            Object a = jsonObject.get("authors");

            if (a instanceof String) {

                String author = (String) a;
                author = author.replace("[", "");
                author = author.replace("]", "");
                authors.add(author);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("authors");
                for (int i = 0; i < jsonCategories.length(); i++)
                    authors.add(jsonCategories.optString(i));
            }

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        String publisher = jsonObject.optString("publisher");
        String publishedDate = jsonObject.optString("publishedDate");
        String description = jsonObject.optString("description");
        int pageCount = jsonObject.optInt("pageCount");

        //categories
        ArrayList<String> categories = new ArrayList<>();

        try {

            Object c = jsonObject.get("categories");

            if (c instanceof String) {

                String category = (String) c;
                category = category.replace("[", "");
                category = category.replace("]", "");
                categories.add(category);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("categories");
                for (int i = 0; i < jsonCategories.length(); i++)
                    categories.add(jsonCategories.optString(i));
            }

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        String language = jsonObject.optString("language");
        String thumbnail = jsonObject.optString("thumbnail");
        int numPhotos = jsonObject.optInt("numPhotos");

        String bookConditions = jsonObject.optString("bookConditions");

        //tags
        ArrayList<String> tags = new ArrayList<>();

        try {

            Object t = jsonObject.get("tags");

            if (t instanceof String) {

                String tag = (String) t;
                tag = tag.replace("[", "");
                tag = tag.replace("]", "");
                tags.add(tag);

            } else {

                JSONArray jsonTags = jsonObject.getJSONArray("tags");
                for (int i = 0; i < jsonTags.length(); i++)
                    tags.add(jsonTags.optString(i));
            }

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        long creationTime = jsonObject.optLong("creationTime");
        String locationLat = jsonObject.optString("location_lat");
        String locationLong = jsonObject.optString("location_long");

        return new Book(bookId, owner_uid, isbn, title, subtitle, authors, publisher, publishedDate, description,
                pageCount, categories, language, thumbnail, numPhotos, bookConditions, tags, creationTime, locationLat, locationLong);
    }


    /**
     * Parser for the result of the search that returns an ArrayList of books that matched the query
     *
     * @param hits : algolia's search hits
     * @return : the colleciton of books
     */
    public ArrayList<Book> parseResults(JSONArray hits) {

        ArrayList<Book> books = new ArrayList<>();

        for (int i = 0; i < hits.length(); i++) {

            try {

                JSONObject hit = hits.getJSONObject(i);

                Iterator<String> keyList = hit.keys();

                //first key (bookData): book object
                String bookData = keyList.next();
                Book b = BookJsonParser(hit.getJSONObject(bookData));
                //b.setBookId(bookData); //save also the FireBase unique ID, is used to retrieve the photos from firebase storage
                books.add(b);

                //second key: objectId == bookId
                //third key: _highlightResult

            } catch (JSONException e) {
                Log.d("debug", "unable to retrieve search result from json hits");
                e.printStackTrace();
            }
        }

        return books;
    }


    private void showSearchResults(){
        mMap.clear(); //remove previous markers
        setAnnouncementMarkers();
    }


    /**
     * Material Search Bar onSearchStateChanged
     */
    @Override
    public void onSearchStateChanged(boolean enabled) {
        String s = enabled ? "enabled" : "disabled";
        Log.d("debug", "search " + s);
    }


    /**
     * Material Search Bar onSearchConfirmed
     */
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        if (searchInputText != null) {
            helper.search(searchInputText.toString());
        }
    }

    /**
     * Material Search Bar onButtonClicked
     */
    @Override
    public void onButtonClicked(int buttonCode) {

        switch (buttonCode) {

            case MaterialSearchBar.BUTTON_NAVIGATION:
                //drawer.openDrawer(Gravity.START); //open the drawer
                break;

            case MaterialSearchBar.BUTTON_SPEECH:
                break;

            case MaterialSearchBar.BUTTON_BACK:
                sba_searchbar.disableSearch();
                break;
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            if(searchResult != null)
                setAnnouncementMarkers();

        } else {
            Toast.makeText(getApplicationContext(), "Niente", Toast.LENGTH_SHORT).show();
            //TODO add ask for permissions here or something similar
        }

    }

    public void setAnnouncementMarkers(){
        for(Book b : searchResult){
            LatLng loc = new LatLng(Double.parseDouble(b.getLocationLat()), Double.parseDouble(b.getLocationLong()));
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .snippet(b.getDescription())
                    .title(b.getTitle())
                    .snippet("Press here for more details..."));

            m.setTag(b); //associate book object to this marker

            //Set Custom InfoWindow Adapter
            CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(this);
            mMap.setInfoWindowAdapter(adapter);
        }

        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);


    }


    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        // Retrieve the data from the marker.
        Book book = (Book) marker.getTag();

        Intent showBook = new Intent(getApplicationContext(), ShowBookActivity.class);
        if(!searchResult.isEmpty()){
            Bundle bundle = new Bundle();
            bundle.putParcelable("book", book);
            showBook.putExtras(bundle);
        }
        startActivity(showBook);
        finish();
    }
}
