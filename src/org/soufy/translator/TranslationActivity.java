package org.soufy.translator;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TranslationActivity extends Activity implements OnItemSelectedListener, TextToSpeech.OnInitListener {

	private String TAG = TranslationActivity.class.getSimpleName();
		
	private static final int REQUEST_CODE = 1234;
	private TextToSpeech mTts;
	private Locale srcLanguage = Locale.ENGLISH;
	private Locale dstLanguage = Locale.ENGLISH;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mTts = new TextToSpeech(this, this);
		
		initLanguageSpinner((Spinner) findViewById(R.id.srcLanguage));
		initLanguageSpinner((Spinner) findViewById(R.id.dstLanguage));
		
		Button btnTranslation = (Button) findViewById(R.id.btnTranslation);
		// Disable button if no recognition service is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
        		new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
        	btnTranslation.setEnabled(false);
        	btnTranslation.setText("Recognizer not present");
        }        
	}
	
    /** Handle the results from the voice recognition activity. */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text = matches.get(0);
            String translated = translate(text);
            if(translated != null) {
                mTts.setLanguage(dstLanguage);
                mTts.speak(translated,TextToSpeech.QUEUE_FLUSH, null);            	
                Toast.makeText(this, translated, Toast.LENGTH_LONG).show();
            }else {
            	Toast.makeText(this, "Unable to contact translation service", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }
	
	/** Handle the translation button onClick event to request speech recognition service */
	public void transalationButtonClicked(View v) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, srcLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Transalation Demo...");
        startActivityForResult(intent, REQUEST_CODE);
	}

	/** Handle the selection of a language from a spinner */
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		// An item was selected. You can retrieve the selected item using
		int spinner = parent.getId();
		if(spinner == R.id.srcLanguage) {
			System.out.println("Selected src item is " + parent.getItemAtPosition(pos));
			srcLanguage = getLocale((String) parent.getItemAtPosition(pos));
		}else if (spinner == R.id.dstLanguage) {
			System.out.println("Selected dst item is " + parent.getItemAtPosition(pos));
			dstLanguage = getLocale((String) parent.getItemAtPosition(pos));
			mTts.setLanguage(dstLanguage);
		}else {
			System.err.println("Unknow view id: " + spinner);
		}        
	}

	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
	
	/** initialize the spinner list of items (i.e. languages) */
	public void initLanguageSpinner(Spinner spinner) {
		// set a listener for responding to item selection
		spinner.setOnItemSelectedListener(this);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.laguages, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
	}
	
	/**@return {@link Locale} language corresponding to the provided name*/
	public Locale getLocale(String language) {
		Locale local = Locale.ENGLISH;
		if(language.equals("French"))
			local = Locale.FRENCH;
		else if(language.equals("German"))
			local = Locale.GERMAN;
		else if(language.equals("Italian"))
			local = Locale.ITALIAN;
		else if(language.equals("Japanese"))
			local = Locale.JAPANESE;
		else if(language.equals("Korean"))
			local = Locale.KOREAN;
		return local;
	}

	/** Called when the TTS service is initialized */
	public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {            
            int result = mTts.setLanguage(dstLanguage);            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language data is missing or the language is not supported.");
            } else {                
            	Log.i(TAG, "Ready to use Text to Speech service");}
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
	}
	
	/** Translate a given text between a source and a destination language */
	public String translate(String text) {		
	    String translated = null;
	    try {
	    	String query = URLEncoder.encode(text, "UTF-8");
	    	String langpair = URLEncoder.encode(srcLanguage.getLanguage()+"|"+dstLanguage.getLanguage(), "UTF-8");
	    	String url = "http://mymemory.translated.net/api/get?q="+query+"&langpair="+langpair;
	    	HttpClient hc = new DefaultHttpClient();		         
		    HttpGet hg = new HttpGet(url);
			HttpResponse hr = hc.execute(hg);
			if(hr.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {				
				JSONObject response = new JSONObject(EntityUtils.toString(hr.getEntity()));
				translated = response.getJSONObject("responseData").getString("translatedText");				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	    
	    return translated;		
	}
}
