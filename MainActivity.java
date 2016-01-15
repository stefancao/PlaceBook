package eecs40.placebook;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {

    //Defining Placebookentry array
    private ArrayList<PlaceBookEntry> Entry = new ArrayList<PlaceBookEntry>();

    //defining image,text,buttons id's
    private EditText TxtPlaceContent;
    private ImageButton ImageButtonSnapshot;
    private ImageButton ImageButtonSpeak;
    private ImageButton ImageButtonLocation;
    private ImageButton ImageButtonPlacePicker;
    private EditText EditPlaceDesc;
    private ImageView imageView;

    private int Listposition = -1;  //position of the last click of an entry

    GPSTracker gps;

    private GoogleApiClient mGoogleApiClient;

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_SPEECH_INPUT = 1002;
    private static final int REQUEST_PLACE_PICKER = 1003;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGoogleApi();    //initialize google API

        //buttons
        ImageButtonSnapshot = (ImageButton) findViewById(R.id.button_snapshot);
        ImageButtonSpeak = (ImageButton) findViewById(R.id.button_speak);
        ImageButtonLocation = (ImageButton) findViewById(R.id.button_location);
        ImageButtonPlacePicker = (ImageButton) findViewById(R.id.button_place_picker);

        //text
        TxtPlaceContent = (EditText) findViewById(R.id.txtPlaceContent);
        EditPlaceDesc = (EditText) findViewById(R.id.edit_place_desc);

        //image
        imageView = (ImageView) findViewById(R.id.image1);


        registerClickCallback();    //register click of scrollview entry

        //register click for buttons
        //snapshot/camera
        ImageButtonSnapshot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        //speak/voice recognition
        ImageButtonSpeak.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dispatchSpeechInputIntent();
            }
        });

        //place picker
        ImageButtonPlacePicker.setOnClickListener(new View.OnClickListener()    {
            public void onClick(View v){
                launchPlacePicker();
            }
        });

        //gps location
        ImageButtonLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gps = new GPSTracker(MainActivity.this);
                if(gps.canGetLocation()){
                    double latidude = gps.getLatitude();
                    double longitude = gps.getLongitude();

                    String aString = Double.toString(latidude);
                    String bString = Double.toString(longitude);

                    TxtPlaceContent.setText("("+aString+", "+bString+")");
                }
            }
        });

    }

    //INIT GOOGLE API
    private void initGoogleApi () {
        mGoogleApiClient = new GoogleApiClient
                . Builder ( this )
                . addApi(Places.GEO_DATA_API)
                . addApi(Places.PLACE_DETECTION_API)
                . addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                . addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {

                    }
                })
                . build() ;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //Saving Button
        if(id == R.id.action_save){
            if(Entry.isEmpty()){
                String message = "No Entries, add one entry first";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else if(Listposition < 0){
                String message = "Please select an entry before Saving";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else {
                String tmp_place_name = TxtPlaceContent.getText().toString();
                String tmp_description = EditPlaceDesc.getText().toString();
                Bitmap tmp_bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                Entry.get(Listposition).setname(tmp_place_name);    //get whatever text is currently present
                Entry.get(Listposition).setDescription(tmp_description);
                Entry.get(Listposition).setImage(tmp_bitmap);   //get whatever image is currently present

                ListView_tick();
            }

        }

        //adding new place
        if(id == R.id.action_new_place){
            //if anything is empty, cannot add
            if(TxtPlaceContent.getText().toString().matches("") || EditPlaceDesc.getText().toString().matches("") || imageView.getDrawable() == null){
                String message = "Please fill in all entries (+Picture) before adding!";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else {
                String tmp_place_name = TxtPlaceContent.getText().toString();
                String tmp_description = EditPlaceDesc.getText().toString();

                //get a date when added
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = df.format(c.getTime());

                Bitmap tmp_bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                newEntry(Entry.size(), tmp_place_name, tmp_description, formattedDate, tmp_bitmap); //adding to PlaceBookEntry

                ListView_tick();
                Listposition = Entry.size()-1;  //change listposition to one last added (which is the last one in list)
            }
        }

        //edit place button
        if(id == R.id.action_edit_place){
            if(Entry.isEmpty()){
                String message = "No Entries, add one entry first";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else {
                String message = "Please Select an Entry and press Save after editing";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }

        //delete place button
        if(id == R.id.action_delete_place){
            if(Entry.isEmpty()){
                String message = "No Entries have been added!";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else if(Listposition < 0){
                String message = "Please Select an entry to delete then press delete again";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
            else {
                Entry.remove(Listposition);     //removing entry from EntryPlaceBook List

                ListView_tick();
                if(!Entry.isEmpty()) {      //if not empty then Display first entry
                    TxtPlaceContent.setText(Entry.get(0).getname());
                    EditPlaceDesc.setText(Entry.get(0).getDescription());
                    imageView.setImageBitmap(Entry.get(0).getImage());
                    Listposition = 0;
                }
                else{   //else set everything empty
                    TxtPlaceContent.setText("");
                    EditPlaceDesc.setText("");
                    imageView.setImageBitmap(null);
                    Listposition = -1;
                }
            }
        }

        //setting button
        if (id == R.id.action_settings) {
            String message = "There are no Settings :)";
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //if clicking on one of the entry list
    private void registerClickCallback(){
        ListView list = (ListView) findViewById(R.id.entrylistview);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                Listposition = position;

                TxtPlaceContent.setText(Entry.get(position).getname());     //print out text of the currently looking entry
                EditPlaceDesc.setText(Entry.get(position).getDescription());
                imageView.setImageBitmap(Entry.get(position).getImage());   //put image of the currently looking entry
            }
        });
    }

    private void ListView_tick() {
        ArrayAdapter<PlaceBookEntry> adapter = new MyListAdapter();
        ListView list = (ListView) findViewById(R.id.entrylistview);
        list.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<PlaceBookEntry>{
        public MyListAdapter(){
            super(MainActivity.this, R.layout.item_view, Entry);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View itemView = convertView;
            if(itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            PlaceBookEntry currentEntry = Entry.get(position);

            //place the image
            ImageView imageView = (ImageView)itemView.findViewById(R.id.item_icon);
            imageView.setImageBitmap(currentEntry.getImage());

            //place the Place
            TextView placeText = (TextView) itemView.findViewById(R.id.item_txtPlace);
            placeText.setText(currentEntry.getname());

            //place the Date
            TextView dateText = (TextView) itemView.findViewById(R.id.item_txtDate);
            dateText.setText(currentEntry.getDate());

            //place the description
            TextView descriptionText = (TextView) itemView.findViewById(R.id.item_txtDescription);
            descriptionText.setText(currentEntry.getDescription());

            return itemView;

        }
    }


    //PlacePicker
    private void launchPlacePicker () {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder () ;
        Context context = getApplicationContext() ;
        try {
            startActivityForResult ( builder . build ( context ) , REQUEST_PLACE_PICKER );
        } catch ( GooglePlayServicesRepairableException e) {
            String message = "Can't open Place Picker!";
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            return;
        } catch ( GooglePlayServicesNotAvailableException e ) {
            String message = "Can't open Place Picker!";
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            return;
        }
    }


    //PICTURE
    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult ( takePictureIntent , REQUEST_IMAGE_CAPTURE ) ;
        }
    }

    //SPEECH
    private void dispatchSpeechInputIntent(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try{
            startActivityForResult(intent, REQUEST_SPEECH_INPUT);
        } catch (ActivityNotFoundException a){
            String message = "Can't open Speech!";
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }

    //New PlaceBook Entry
    private void newEntry(int id, String name, String description, String date, Bitmap image){
        Entry.add(new PlaceBookEntry(id, name, description, date, image));
    }

    //onActivityResult
    protected void onActivityResult ( int requestCode , int resultCode , Intent data ) {
        if ( resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE && data != null ) {    //ImageCapture
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }

        if ( resultCode == RESULT_OK && requestCode == REQUEST_SPEECH_INPUT && data != null) {      //Speech Recognition
            ArrayList< String > result = data . getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            EditPlaceDesc.append(result.get(0));

        }

        if ( resultCode == RESULT_OK && requestCode == REQUEST_PLACE_PICKER && data != null ) {     //PlacePicker
            Place place = PlacePicker.getPlace(data, this) ;
            String toastMsg = String.format("%s", place.getName());
            TxtPlaceContent.setText(toastMsg);
        }

    }

}
