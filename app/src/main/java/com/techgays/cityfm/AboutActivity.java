package com.techgays.cityfm;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

public class AboutActivity extends WebviewActivity {

	private ImageView stationImageView,CompanyLogoImageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		stationImageView = (ImageView) findViewById(R.id.imageViewStation);
		CompanyLogoImageView = (ImageView) findViewById(R.id.imageViewCompanyLogo);


		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			stationImageView.setImageDrawable(getApplicationContext().getDrawable(R.drawable.station_1));
			CompanyLogoImageView.setImageDrawable(getApplicationContext().getDrawable(R.drawable.sarasalogo));
		} else {
			stationImageView.setImageDrawable(getResources().getDrawable(R.drawable.station_1));
			CompanyLogoImageView.setImageDrawable(getResources().getDrawable(R.drawable.sarasalogo));
		}


		//loadUrl("file:///android_asset/html/about.html", getResources().getString(R.string.menu_about));
		//loadUrl("http://www.yoursite.com/about.html");


	}
}
