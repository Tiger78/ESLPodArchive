import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import vitaly.myutils.AnalizeFileContent;
import vitaly.myutils.GetFileFromInet;

public class GetMP3Podcasts {
	static String filename;
	final static String URLPath = "http://media.libsyn.com/media/eslpod/";

	private static int checkPodcastName(String s) {
		if (s.equals("ESL Podcast Special Edition - Video Podcast for 1st Anniversary")) {
			return -1;
		}
		if (s.equals("ESL Podcast's 2nd Anniversary: The Rock Video")) {
			return -1;
		}
		if (s.equals("ESL Podcast's Third Anniversary Video Podcast")) {
			return -1;
		}
		if (s.equals("ESL Podcast's Fifth Anniversary Video Podcast")) {
			return -1;
		}
		return 0;
	}

	// 1. Точка входа:
	// http://www.eslpod.com/website/show_all.php?cat_id=-59456&low_rec=1260
	// 2. Найти название Podcast'а: class="podcast_title">Название</a>
	// 3. Найти:
	// ссылки содержащие строку show_podcast:
	// href="show_podcast.php?issue_id=1770056"
	// 4. Перейти по ссылке www.eslpod/website/show_podcast
	// 5. Найти предпоследний тег <span class="pod_body"> </span>

	static int getPodcastScripts(){
		final String filespath = "c:/tmp/ESL Podcasts";
		final String textspath = "c:/tmp/ESL Podcasts/texts";
		final String url = "http://www.eslpod.com/website/show_all.php?cat_id=-59456&low_rec=";
		final String show_podcast_url = "http://www.eslpod.com/website/";
		final String nameslist_fn = "nameslist.txt";// файл с названиями
													// подкастов
		final String mp3linklist_fn = "mp3links.txt";// файл с mp3 ссылками на
														// подкасты
		final int MAX_NUM_PODCASTS=1318;//общее количество подкастов постоянно меняется..
		//final int MAX_NUM_PODCASTS=20;//общее количество подкастов постоянно меняется..
		int total_podcast_number = 0;
		ArrayList<String> nameslist_arr=null;
		ArrayList<String> mp3links_arr=null;
		ArrayList<String> show_podcast_list_arr=null;
		ArrayList<String> pod_bodies=null;
		File f = new File(filespath, nameslist_fn);
		try {
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PrintStream nameslist_ps=null;
		try {
			nameslist_ps = new PrintStream(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File f2 = new File(filespath, mp3linklist_fn);
		try {
			f2.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PrintStream mp3linklist_ps=null;		
		try {
			mp3linklist_ps = new PrintStream(f2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// чтение данных с ресурса		
		int start_low_rec=MAX_NUM_PODCASTS-18;
		//int start_low_rec=800;
		for (int low_rec = start_low_rec; true ; low_rec -= 20) {			
			if (low_rec<0)
				low_rec=0;			
			GetFileFromInet gffi;
			System.out.println("ESL Podcast script download start");
			System.out.println("from: " + url + low_rec);
			int i=-1;
			try {
				gffi = new GetFileFromInet();				
				i = gffi.getURLSaveFile(url + low_rec, filespath, "list1.html");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					gffi = new GetFileFromInet();					
					i = gffi.getURLSaveFile(url + low_rec, filespath, "list1.html");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
			}
			if (i == 0)
				System.out.println("download ok!");
			else {
				System.out.println("download fail");
				System.exit(0);
			}
			// найдём список названий подкастов
			AnalizeFileContent afc = new AnalizeFileContent();
			nameslist_arr=null;
			try {
				nameslist_arr = afc.findInFile(filespath + "/" + "list1.html",
						"\"podcast_title\">[^<>]*?</a>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}// [.]*?</a>
			Collections.reverse(nameslist_arr);
			System.out.println("writing to file " + nameslist_arr.size()
					+ " titles");
			int k = 0;
			while (k < nameslist_arr.size()) {
				String s = nameslist_arr.get(k);
				s = s.substring(16, s.length() - 4);
				s = s.trim();
				s =s.replace("-", "-");
				s =s.replace("’"," ");
				nameslist_arr.set(k, s);
				if (checkPodcastName(s)==0) nameslist_ps.println(s);
				k++;
			}
			// найдём список ссылок на mp3
			afc = new AnalizeFileContent();
			mp3links_arr=null;
			try {
				mp3links_arr = afc.findInFile(filespath + "/" + "list1.html",
						"http://[^<>]*?\\.mp3");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Collections.reverse(mp3links_arr);
			System.out.println("writing to file " + mp3links_arr.size()
					+ " mp3 links");
			for (String s : mp3links_arr) {
				mp3linklist_ps.println(s);
			}
			/*
			if (nameslist_arr.size() != mp3links_arr.size()) {
				System.out.println("what is podcasts number?");
				System.exit(0);
			}*/
			// найдём список ссылок на текстовую часть
			afc = new AnalizeFileContent();
			try {
				show_podcast_list_arr = afc.findInFile(filespath + "/"
						+ "list1.html", "\"show_podcast[^<>]*?\"");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Collections.reverse(show_podcast_list_arr);
			for (int m = 0; m <show_podcast_list_arr.size(); m++) {
				String podcastlink = show_podcast_list_arr.get(m);
				System.out.println(podcastlink);
				podcastlink = podcastlink
						.substring(1, podcastlink.length() - 1);
				// переходим на страницу подкаста
				System.out.println("script from: " + show_podcast_url
						+ podcastlink);
				try {
					gffi = new GetFileFromInet();					
					i = gffi.getURLSaveFile(show_podcast_url + podcastlink,
							filespath, "list2.html");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {		
						gffi = new GetFileFromInet();						
						i = gffi.getURLSaveFile(show_podcast_url + podcastlink,
								filespath, "list2.html");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (i == 0)
					System.out.println("download ok!");
				else {
					System.out.println("download fail");
					System.exit(0);
				}
				// найдём текст
				afc = new AnalizeFileContent();
				try {
					pod_bodies = afc.findInFile(filespath + "/" + "list2.html",
							"<span class=\"pod_body\">[^~]*?</span>");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("pod_bodies: " + pod_bodies.size());
				String podcastbody = pod_bodies.get(pod_bodies.size() - 2);
				podcastbody = podcastbody.substring(23,
						podcastbody.length() - 7);
				podcastbody = podcastbody.trim();
				if (pod_bodies.size() == 11) {// предполагаем что формат
												// страницы не меняется
					System.out.println(podcastbody);
				}
				// положим текст в файл, если не видеоподкаст
				if (checkPodcastName(nameslist_arr.get(m))==0){
					System.out.println("");
					System.out.println(nameslist_arr.get(m)+".txt");
					//StringBuffer s=new StringBuffer(nameslist_arr.get(m)).
					String s=nameslist_arr.get(m);
					s=s.replace("?", "");
					s=s.replace(":", "");
					s=s.replace("/","");
					File ftext=new File(textspath,s+".html");					
					try {
						ftext.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					PrintStream ps=null;
					try {
						ps = new PrintStream(ftext);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ps.println(podcastbody);
					ps.flush();
					ps.close();
				}				
			}
			//System.exit(0);
			total_podcast_number += nameslist_arr.size();
			if (low_rec==0) break;
		}
		System.out.println("Total number of podcasts: " + total_podcast_number);
		nameslist_ps.flush();
		nameslist_ps.close();
		mp3linklist_ps.flush();
		mp3linklist_ps.close();
		return 0;
	}

	// скачаем один файл MP3 подкаста
	static int getMP3Podcast(int number) throws IOException {
		// ESLPod12n.mp3 - ESLPod57n.mp3
		// ESLPod58.mp3 - ESLPod100.mp3
		// ESLPodEQA.mp3
		// ESLPod101.mp3
		GetFileFromInet gffi = new GetFileFromInet();
		System.out.println("ESL Podcast download start");
		// filename = "ESLPod15n.mp3";
		if (number < 12)
			return -1;
		if (number <= 57)
			filename = "ESLPod" + number + "n.mp3";
		else
			filename = "ESLPod" + number + ".mp3";
		System.out.println("File name: " + filename);
		int i = gffi.getURLSaveFile(URLPath + filename,
				"d:/books/ESL Podcasts", filename);
		System.out.println("return code: " + i);
		if (i != 0) {
			i = gffi.getURLSaveFile(URLPath + filename,
					"d:/books/ESL Podcasts", filename);
		}
		if (i == 0) {
			System.out.println("downloaded ok!");
			return 0;
		}
		System.out.println("downloaded fail");
		return -1;
	}
	
	// скачаем MP3 файлы подкастов 
	static void getPodcastsMP3() throws IOException{
		GetFileFromInet gffi = new GetFileFromInet();
		System.out.println("ESL Podcast download start");
		// filename = "ESLPod15n.mp3";
		for (int j = 12; j < 15; j++) {
			if (j <= 57)
				filename = "ESLPod" + j + "n.mp3";
			else
				filename = "ESLPod" + j + ".mp3";
			System.out.println("File name: " + filename);
			int i = gffi.getURLSaveFile(URLPath + filename,
					"d:/books/ESL Podcasts", filename);
			System.out.println("return code: " + i);
			if (i != 0) {
				i = gffi.getURLSaveFile(URLPath + filename,
						"d:/books/ESL Podcasts", filename);
			}
			if (i == 0) {
				System.out.println("downloaded ok!");
			} else {
				System.out.println("downloaded fail");
				System.exit(1);
			}
		}
	}
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		getPodcastScripts();
		//getPodcastsMP3();
	}

}
