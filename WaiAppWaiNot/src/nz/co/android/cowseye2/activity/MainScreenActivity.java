package nz.co.android.cowseye2.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import nz.co.android.cowseye2.R;
import nz.co.android.cowseye2.RiverWatchApplication;
import nz.co.android.cowseye2.common.Constants;
import nz.co.android.cowseye2.event.SubmissionEvent;
import nz.co.android.cowseye2.event.SubmissionEventBuilder;
import nz.co.android.cowseye2.event.SubmissionEventBuilderException;
import nz.co.android.cowseye2.fragments.GridIncidentGalleryFragment;
import nz.co.android.cowseye2.fragments.HomeFragment;
import nz.co.android.cowseye2.fragments.MainViewFragment;
import nz.co.android.cowseye2.fragments.NavigationDrawerFragment;
import nz.co.android.cowseye2.fragments.WaterReadingFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * This is the main screen of the CowsEye application
 *
 * @author Mitchell Lane (modified by Hamish Cundy)
 *
 */
public class MainScreenActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

	private Button buttonSubmit;
	private Button buttonGallery;
	private RiverWatchApplication myApplication;

	private ProgressDialog progressDialog;

	private String[] imageUrls;
	private String[] thumbUrls;
	private String[] descriptions;
	private boolean loadingGridView = false;
	private boolean haveBaseIncidents = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Crashlytics.start(this);
		setContentView(R.layout.activity_main);

		myApplication = (RiverWatchApplication) getApplication();
		setupDrawer();

		setupUI();
		// new GetIncidentsAsyncTask(MainScreenActivity.this, new
		// GetIncidentsEvent(myApplication, 0, 50),myApplication).execute();
		checkForCachedSubmissions();
	}

	private void setupDrawer() {
		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
	}

	/**
	 * Checks the apps photo storage for any cached submissions, and upload them
	 * if internet is available
	 *
	 */
	private void checkForCachedSubmissions() {
		if (myApplication.isOnline()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					File dir = MainScreenActivity.this.getDir("",
							Context.MODE_WORLD_READABLE);
					String pathToDir = dir.getAbsolutePath();
					// Log.d("MainScreenAct", pathToDir);
					File[] fileNames = dir.listFiles();
					Log.d("MainScreenAct", pathToDir + " " + fileNames.length);
					for (File f : fileNames) {
						ExifInterface exif = null;

						try {
							exif = new ExifInterface(f.getPath());
							Log.d("MSA", exif.getAttribute("UserComment") + "");
							if (exif.getAttribute("UserComment") != null) {
								SubmissionEventBuilder build = SubmissionEventBuilder
										.getSubmissionEventBuilder(myApplication);

								Scanner sc = new Scanner(exif
										.getAttribute("UserComment"));

								LatLng coord = new LatLng(sc.nextDouble(), sc
										.nextDouble());
								build.setGeoCoordinates(coord);
								sc.nextLine();

								build.setImageDescription(sc.nextLine());
								List<String> tagList = new ArrayList<String>();
								while (sc.hasNext()) {
									tagList.add(sc.next());
								}

								build.setImageTag(tagList);

								build.setImagePath(Uri.parse(f.getPath()));
								Log.d("MSA2",
										f.getPath()
												+ " 1 "
												+ Uri.parse(f.getPath())
												+ " 2 "
												+ Uri.parse(f.getAbsolutePath()));
								final SubmissionEvent event = build.build();

								final boolean result = RiverWatchApplication
										.processEventResponse(event
												.processRaw());

								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (result == false) {
											Toast.makeText(
													getApplicationContext(),
													"Could not send cached submission. Will try again later",
													Toast.LENGTH_SHORT).show();
										} else {
											myApplication.deleteImage(event);
											Toast.makeText(
													getApplicationContext(),
													"Successfully sent cached submissions",
													Toast.LENGTH_SHORT).show();
										}
									}
								});

							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SubmissionEventBuilderException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}

			}).start();
		} else {
			Toast.makeText(
					getApplicationContext(),
					"Could not submit cached submissions (no internet connection)",
					Toast.LENGTH_SHORT).show();

		}

	}

	/**
	 * This gets called after a successfull submission event as the activity is
	 * already open and this current opened activity is not destroyed
	 */
	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);
	}

	protected boolean networkIsConnected() {
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		Boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.isConnected();
		Boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnected();
		return is3g || isWifi;
	}

	/* Sets up the UI */
	private void setupUI() {
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(getString(R.string.loading_images_title));
		progressDialog.setMessage(getString(R.string.please_wait));
		progressDialog.setCancelable(false);

		/*
		 * buttonGallery = (Button)findViewById(R.id.button_view_gallery);
		 * buttonGallery.setOnClickListener(new View.OnClickListener() {
		 *
		 * @Override public void onClick(View v) { //progressDialog.show();
		 * //only get new list of incidents if we don't already have them
		 * //if(!haveBaseIncidents){ // new
		 * GetIncidentsAsyncTask(MainScreenActivity.this, new
		 * GetIncidentsEvent(myApplication, 0, 50), myApplication).execute(); //
		 * loadingGridView = true; //} //else //loadGridView(); //} Intent
		 * bIntent = new Intent(Intent.ACTION_VIEW,
		 * Uri.parse("http://homepages.ecs.vuw.ac.nz/wainz/maps"));
		 * startActivity(bIntent); } });
		 */

	}

	/**
	 * Ends the web service call to get all incidents and opens the grid view if
	 * the call was succesful
	 */
	public void endGetIncidentsServiceCall(boolean result) {
		progressDialog.dismiss();
		if (!result) {
			if (loadingGridView)
				Toast.makeText(this,
						getString(R.string.failure_load_images_msg),
						Toast.LENGTH_LONG).show();
		} else {
			// REMOVED
			haveBaseIncidents = false;
			if (loadingGridView) {
				loadingGridView = false;
				loadGridView();
			}
		}

	}

	/*

*/
	public void loadGridView() {
		loadingGridView = false;
		if (progressDialog != null)
			progressDialog.dismiss();
		Intent i = new Intent(MainScreenActivity.this,
				GridIncidentGalleryActivity.class);
		i.putExtra(Constants.GALLERY_IMAGES_ARRAY_KEY, imageUrls);
		i.putExtra(Constants.GALLERY_THUMBNAIL_IMAGES_ARRAY_KEY, thumbUrls);
		i.putExtra(Constants.JSON_INCIDENT_IMAGE_DESCRIPTION_KEY, descriptions);
		startActivity(i);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// myApplication.requestStartEventHandling();
		Log.i(toString(), "MainScreen requestStartEventHandling");
	}

	@Override
	protected void onDestroy() {
		Log.i(toString(), "MainScreen stopTimerEventHandling");
		// myApplication.stopTimerEventHandling();
		super.onDestroy();
	}

	/** Starts a submission of a pollution event */
	public void SubmitPollutionEvent(View view) {

		startActivity(new Intent(MainScreenActivity.this,
				SelectImageActivity.class));

	}

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment f = null;
		switch (position) {
		case 0:
			f = HomeFragment.newInstance(position);
			break;
		case 1:

			f = MainViewFragment.newInstance(position);
			break;
		case 2:
			
			f = WaterReadingFragment.newInstance(position);
			break;
		case 3:
			
			f = GridIncidentGalleryFragment.newInstance(position);
			break;
		}

		fragmentManager.beginTransaction().replace(R.id.container, f).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.



			//getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}