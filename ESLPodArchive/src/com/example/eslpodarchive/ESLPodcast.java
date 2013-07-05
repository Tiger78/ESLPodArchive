package com.example.eslpodarchive;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.regex.Pattern;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AnalogClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import vitaly.myutils.*;

public class ESLPodcast extends Activity implements
		MyMediaController.MediaPlayerControl,
		MediaPlayer.OnBufferingUpdateListener {
	String PATHTOFILES = "/sdcard/ESLPodcast";
	// Arrays
	String[] arrAllPodcastList;// в этом массиве все подкасты
	String[] arrFilteredPodcastList;
	String[] arrAllPodcastLinks;
	String[] arrFilteredPodcastLinks;
	String[] arrPodcastTexts;
	ArrayList<String> listFilteredPodcastList;
	ArrayList<String> listFilteredPodcastLinks; 
	
	// Views
	ListView lw;
	WebView podcasttext;
	TextView podcasttitle;
	// downloading
	GetFileFromInet gffi;
	private static ProgressBar mProgressBar;
	private static ProgressBar mSearchProgressBar;
	private static Handler mHandler;
	private static Handler mSearchHandler;
	// Podcast text
	int textPodcastIndex = 0;
	String textPodcastTitle;// название текущего подкаста в плеере
	static TabHost tabs;
	// Podcast sound
	MyMediaController mController;
	MediaPlayer mPlayer;
	int bufferPercent = 0;
	View.OnClickListener mPlayerPrevListener;
	View.OnClickListener mPlayerNextListener;
	boolean newPodcast;
	boolean haveReadAssets;
	// вспомогательный класс для взаимодействия потоков (downloading)
	private final static class HandlerExtension extends Handler {

		public void handleMessage(Message msg) {
			mProgressBar.setProgress(msg.what);
			if (msg.arg1 == 1) {// файл скачался
				tabs.setCurrentTabByTag("player");
			}
			/*
			 * if (msg.arg1 == 123) { bundle = msg.getData(); String s =
			 * bundle.getString("filename"); tw1.setText("Скачивается файл: " +
			 * s); }
			 */
		}
	}
	
	private final class SearchHandler extends Handler {
		public void handleMessage(Message msg) {
			mSearchProgressBar.setProgress(msg.what);
			if (msg.arg1 == 1) {// файл скачался
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						ESLPodcast.this, android.R.layout.simple_list_item_1,
						arrFilteredPodcastList);
				lw.setAdapter(adapter);
				lw.setVisibility(View.VISIBLE);
				mSearchProgressBar.setVisibility(View.GONE);
			}
		}
	}

	// начать загрузку выбранного подкаста
	void startDownload() {
		// кнопка Download
		// Button downloadbtn = (Button) findViewById(R.id.download);
		// downloadbtn.setOnClickListener(new OnClickListener() {

		// @Override
		// public void onClick(View v) {
		// TODO Auto-generated method stub
		// ищем ссылку
		// 1. Ищем индекс названия подкаста в массиве arrAllPodcastList
		textPodcastIndex = searchPodcastIndexByName(textPodcastTitle,
				arrFilteredPodcastList);
		Log.d("ESLPodDebug", "podcastIndex :" + textPodcastIndex);
		Log.d("ESLPodDebug", arrFilteredPodcastLinks[textPodcastIndex]);
		if (textPodcastIndex != -1) {
			TextView downloadingfile = (TextView) findViewById(R.id.downloadingfile);
			downloadingfile.setText(arrFilteredPodcastList[textPodcastIndex]);
			downloadPodcast(arrFilteredPodcastLinks[textPodcastIndex],
					arrFilteredPodcastList[textPodcastIndex]);// скачаем файл в
															// отдельном
															// потоке
			tabs.setCurrentTabByTag("downloads");
		}
		// }
		// });
	}

	// загрузить подкаст из интернета
	void downloadPodcast(String url, String podcasttitle) {
		// final String downloadurl = url.trim();
		final String downloadurl = url;
		// final String filename = podcasttitle.trim();
		final String filename = podcasttitle;
		File f = new File("/sdcard/ESLPodcast");
		f.mkdirs();
		Thread downloadthread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				gffi = new GetFileFromInet();
				int return_code = gffi.getURLSaveFileInit(downloadurl,
						PATHTOFILES, filename + ".mp3");
				if (return_code < 0) {
					Log.d("ESLPodDebug", "downloadPodcast(), return_code="
							+ return_code);
					return;
				}
				Message m;
				m = mHandler.obtainMessage();
				m.what = 0;
				m.arg1 = 0;
				mHandler.sendMessage(m);
				while ((gffi.getURLSaveFileCycle()) != -1) {
					m = mHandler.obtainMessage();
					m.what = gffi.getPercent();
					mHandler.sendMessage(m);
				}
				m = mHandler.obtainMessage();
				m.what = 100;
				mHandler.sendMessage(m);
				gffi.getURLSaveFileFinish();
				if (gffi.getFileSize() == gffi.getFileReadBytes()) {
					Log.d("ESLPodDebug", "Файл скачался");
					m = mHandler.obtainMessage();
					m.what = 100;
					m.arg1 = 1;
					mHandler.sendMessage(m);
					// return 0;// скачался
				} else {
					Log.d("ESLPodDebug", "Файл не скачался");
					// return -1;// не скачался
				}
			}
		});
		downloadthread.start();
	}

	// вспомогательная функция ковертирования
	String convertStreamToString(InputStream is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i = -1;
		try {
			i = is.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (i != -1) {
			baos.write(i);
			try {
				i = is.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return baos.toString();
	}

	// возвращает Adapter списка подкастов
	private Adapter getPodcastListAdapter() {
		ArrayAdapter<String> adapter = new PodcastListAdapter();
		return adapter;
	}

	class PodcastListAdapter extends ArrayAdapter<String> {
		PodcastListAdapter() {
			super(ESLPodcast.this, android.R.layout.simple_list_item_1,
					arrFilteredPodcastList);
		}
	}

String readFromFileInAsset(String fileName){
	AssetManager am = getAssets();
	InputStream is = null;
	try {
		is = am.open(fileName);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	String s = convertStreamToString(is);
	try {
		is.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return(s);
}
	
	String[] readLinesFromFileInAsset(String fileName) {
		String[] stringsInFile;
		String s=readFromFileInAsset(fileName);
		stringsInFile = s.split("\r\n");
		return stringsInFile;
	}

	// 1. Ищем индекс названия подкаста в массиве
	private int searchPodcastIndexByName(String name, String[] arr) {
		int podcastindex = -1;
		int curindex = 0;
		while (curindex < arr.length) {
			if (arr[curindex].equals(name)) {
				podcastindex = curindex;
				break;
			}
			curindex++;
		}
		return curindex;
	}

	private int getPrevPodcast(int podcastindex, String[] arr) {
		// TODO Auto-generated method stub
		if (podcastindex > 0)
			podcastindex--;
		else
			podcastindex = arr.length - 1;
		return podcastindex;
	}

	private int getNextPodcast(int podcastindex, String[] arr) {
		if (podcastindex < (arr.length - 1))
			podcastindex++;
		else
			podcastindex = 0;
		return podcastindex;
	}

	void outputPodcastText(int podcastindex) {
		String title = arrFilteredPodcastList[podcastindex];
		podcasttext.loadUrl("file:///android_asset/texts/"
				+ arrFilteredPodcastList[podcastindex] + ".html");
		podcasttitle.setText(title);
	}

	void hideSoftKeys(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

void readAllAssetFiles(){
	// прочитаем сначала все файлы в массив строк
	arrPodcastTexts=new String[arrAllPodcastList.length];
	Message m;
	m = mSearchHandler.obtainMessage();
	m.what = 0;
	m.arg1=0;
	mSearchHandler.sendMessage(m);
	for (int i=0;i<arrAllPodcastList.length;i++) {
		m = mSearchHandler.obtainMessage();
		m.what = (int)(((float)i/(float)arrAllPodcastList.length)*(float)100);		
		mSearchHandler.sendMessage(m);
		long start1=System.currentTimeMillis();
		arrPodcastTexts[i]=readFromFileInAsset("texts/"+arrAllPodcastList[i]+".html");
		long stop1=System.currentTimeMillis();
		Log.d("ESLPodDebug","подкаст номер"+i+" "+(double)(stop1-start1)/(double)1000+" c");
	}	
}
	
void findtextinpodcasts(String searchstring){	
	// поиск в названиях подкастов
	listFilteredPodcastList = new ArrayList<String>();
	listFilteredPodcastLinks = new ArrayList<String>();
	Message m;
	m = mSearchHandler.obtainMessage();
	m.what = 0;
	m.arg1=0;
	mSearchHandler.sendMessage(m);
	for (int i=0;i<arrAllPodcastList.length;i++) {
		m = mSearchHandler.obtainMessage();
		m.what = (int)(((float)i/(float)arrAllPodcastList.length)*(float)100);		
		mSearchHandler.sendMessage(m);
		long start1=System.currentTimeMillis();
		String podcast_title=arrAllPodcastList[i];
		String podcast_link=arrAllPodcastLinks[i];
		//String podcast_text=readFromFileInAsset("texts/"+arrAllPodcastList[i]+".html");
		String podcast_text=arrPodcastTexts[i];
		String lc_podcast_text=podcast_text.toLowerCase();
		String lc_podcast_title = podcast_title.toLowerCase();
		if (lc_podcast_title.contains(searchstring) || 
				(lc_podcast_text.contains(searchstring))) {
			listFilteredPodcastList.add(podcast_title);
			listFilteredPodcastLinks.add(podcast_link);				
		}
		long stop1=System.currentTimeMillis();
		Log.d("ESLPodDebug","поиск! подкаст номер"+i+" "+(double)(stop1-start1)/(double)1000+" c");
	}
	Object[] objectarraylist = listFilteredPodcastList.toArray();
	Object[] objectarraylinks = listFilteredPodcastLinks.toArray();
	arrFilteredPodcastList = new String[objectarraylist.length];
	for (int i = 0; i < objectarraylist.length; i++) {
		arrFilteredPodcastList[i] = (String) objectarraylist[i];
		arrFilteredPodcastLinks[i] = (String) objectarraylinks[i];
	}
	m = mSearchHandler.obtainMessage();
	m.what = 0;
	m.arg1=1;//закончили поиск - выводим результат
	mSearchHandler.sendMessage(m);	
}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mHandler = new HandlerExtension();// Handler для взаимодействия потоков
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		mSearchHandler = new SearchHandler();
		mSearchProgressBar = (ProgressBar)findViewById(R.id.searchProgressBar);
		mSearchProgressBar.setVisibility(View.GONE);		
		// чтение списка подкастов из файла
		arrAllPodcastList = readLinesFromFileInAsset("nameslist.txt");
		arrFilteredPodcastList = arrAllPodcastList;// ссылаемся на полный список подкастов
		// чтение списка mp3-ссылок в файл
		arrAllPodcastLinks = readLinesFromFileInAsset("mp3links.txt");
		arrFilteredPodcastLinks=arrAllPodcastLinks;		
		Adapter podcastListAdapter = getPodcastListAdapter();
		lw = (ListView) findViewById(R.id.listView1);
		lw.setAdapter((ListAdapter) podcastListAdapter);
		podcasttext = (WebView) findViewById(R.id.webpodcasttext);
		podcasttitle = (TextView) findViewById(R.id.podcastname);
		outputPodcastText(textPodcastIndex);
		//readAllAssetFiles();
		Button findmore=(Button)findViewById(R.id.findmore);
		findmore.setVisibility(View.GONE);
		haveReadAssets=false;
		hideSoftKeys(lw);
		// кнопка Search для поиска подкастов
		Button searchbtn = (Button) findViewById(R.id.search);
		searchbtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				// TODO Auto-generated method stub
				EditText editsearchstring = (EditText) findViewById(R.id.editText1);
				String searchstring = editsearchstring.getText().toString();
				searchstring = searchstring.toLowerCase();
				final String final_searchstring=searchstring;
				Runnable r=new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (!haveReadAssets) {
							readAllAssetFiles();
							haveReadAssets=true;
						}
						findtextinpodcasts(final_searchstring);		
						// lw.requestFocus();
					}					
				};
				new Thread(r).start();
				lw.setVisibility(View.GONE);
				mSearchProgressBar.setVisibility(View.VISIBLE);				
				hideSoftKeys(v);
			}
		});
		lw.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				tabs.setCurrentTabByTag("player");
				TextView tw = (TextView) arg1.findViewById(android.R.id.text1);
				textPodcastTitle = (String) tw.getText();
				textPodcastIndex = searchPodcastIndexByName(textPodcastTitle,
						arrFilteredPodcastList);
				if (textPodcastIndex < 0)
					textPodcastIndex = 0;// если не нашёлся выводим первый
											// подкаст
				// загрузим текст в WebView и обновим название
				outputPodcastText(textPodcastIndex);
				// prepareMediaPlayer();
				newPodcast = true;
				mPlayer.reset();
				hideSoftKeys(arg1);
			}
		});
		// вкладки
		tabs = (TabHost) findViewById(R.id.tabhost);
		tabs.setup();
		TabHost.TabSpec spec = tabs.newTabSpec("list");
		spec.setContent(R.id.tab1);
		spec.setIndicator("Список подкастов");
		tabs.addTab(spec);

		spec = tabs.newTabSpec("player");
		spec.setContent(R.id.player);
		spec.setIndicator("Плеер");
		tabs.addTab(spec);

		spec = tabs.newTabSpec("downloads");
		spec.setContent(R.id.downloads);
		spec.setIndicator("Загрузки");
		tabs.addTab(spec);
		tabs.setOnTabChangedListener(new OnTabChangeListener() {
			
			@Override
			public void onTabChanged(String tabId) {
				// TODO Auto-generated method stub
				hideSoftKeys(lw);
			}
		});
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Calling super is required
		super.onConfigurationChanged(newConfig);
		// Store important UI state
		// saveState();
		// Reload the view resources
		// loadView();
		/*
		 * LinearLayout container = (LinearLayout) findViewById(R.id.container);
		 * if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		 * container.setOrientation(LinearLayout.HORIZONTAL); } else {
		 * container.setOrientation(LinearLayout.VERTICAL); }
		 */
	}

	private void saveState() {
		// Implement any code to persist the UI state
	}

	private void loadView() {
		setContentView(R.layout.main);
		// Handle any other required UI changes upon a new configuration
		// Including restoring and stored state
	}

	@Override
	public void onResume() {
		super.onResume();

		// mPlayer = new MediaPlayer(); // Set the audio data source try {
		/*
		 * try { mPlayer.setDataSource(this,
		 * Uri.parse("http://www.jingle.org/levysfurnishers.mp3"));
		 * 
		 * mPlayer.prepare(); } catch (Exception e) { e.printStackTrace(); }
		 */
		mPlayer = new MediaPlayer();
		// prepareMediaPlayer();
		newPodcast = true;
		mController = new MyMediaController(this, null);
		mController.setAnchorView(findViewById(R.id.player));
		mController.setMediaPlayer(this);
		mController.setEnabled(true);
		mPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				mController.onComplete();
			}
		});
		mPlayerPrevListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d("ESLPodDebug", "Player: prev");
				textPodcastIndex = getPrevPodcast(textPodcastIndex,
						arrFilteredPodcastList);
				// загрузим текст в WebView
				outputPodcastText(textPodcastIndex);
				textPodcastTitle = arrFilteredPodcastList[textPodcastIndex];
				// prepareMediaPlayer();
				newPodcast = true;
				mPlayer.reset();
			}
		};
		mPlayerNextListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d("ESLPodDebug", "Player: next");
				textPodcastIndex = getNextPodcast(textPodcastIndex,
						arrFilteredPodcastList);
				// загрузим текст в WebView
				outputPodcastText(textPodcastIndex);
				textPodcastTitle = arrFilteredPodcastList[textPodcastIndex];
				// prepareMediaPlayer();
				newPodcast = true;
				mPlayer.reset();
			}
		};
		mController.setPrevNextListeners(mPlayerNextListener,
				mPlayerPrevListener);
	}

	boolean isPodcastExist() {
		String filename = arrFilteredPodcastList[textPodcastIndex] + ".mp3";
		File file = new File(PATHTOFILES, filename);
		if (!file.exists()) {
			return false;
		}
		return true;
	}

	void prepareMediaPlayer() {
		mPlayer.reset();
		try {
			mPlayer.setDataSource(PATHTOFILES + "/"
					+ arrFilteredPodcastList[textPodcastIndex] + ".mp3");
			mPlayer.prepare();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		/*
		 * if (player!=null){ player.stopMediaPlayer(); } player=null;
		 */
	}

	@Override
	public void onPause() {
		super.onPause();
		mPlayer.release();
		mPlayer = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// Mediacontroller & MediaPlayer

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		bufferPercent = percent;
	}

	// Android 2.0+ Target Callbacks
	@Override
	public boolean canPause() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canSeekForward() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return bufferPercent;
	}

	@Override
	public int getCurrentPosition() {
		// TODO Auto-generated method stub
		return mPlayer.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		// TODO Auto-generated method stub
		return mPlayer.getDuration();
	}

	@Override
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return mPlayer.isPlaying();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		mPlayer.pause();
	}

	@Override
	public void seekTo(int pos) {
		// TODO Auto-generated method stub
		mPlayer.seekTo(pos);
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		// проверяем существует ли файл
		if (newPodcast) {
			if (!isPodcastExist()) {
				startDownload();
				return;
			}
			prepareMediaPlayer();
			newPodcast = false;
		}
		mPlayer.start();
	}
}
