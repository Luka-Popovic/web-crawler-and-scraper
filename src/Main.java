import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;

public class Main {
	public static DB db = new DB();
	private static String countryProcessed = "Serbia";

	public static void main(String[] args) throws SQLException, IOException {
		String startURL = "https://www.discogs.com/search/?page=1&country_exact=Serbia";
		String URLList[] = new String[7];
		String countries[] = new String[7];
		URLList[0] = "https://www.discogs.com/search/?page=1&country_exact=Serbia";
		URLList[1] = "https://www.discogs.com/search/?page=1&country_exact=Serbia+and+Montenegro";
		URLList[2] = "https://www.discogs.com/search/?page=1&country_exact=Montenegro";
		URLList[3] = "https://www.discogs.com/search/?page=1&country_exact=Croatia";
		URLList[4] = "https://www.discogs.com/search/?page=1&country_exact=Slovenia";
		URLList[5] = "https://www.discogs.com/search/?page=1&country_exact=Macedonia";
		URLList[6] = "https://www.discogs.com/search/?page=1&country_exact=Bosnia+%26+Herzegovina";

		countries[0] = "Serbia";
		countries[1] = "Serbia and Montenegro";
		countries[2] = "Montenegro";
		countries[3] = "Croatia";
		countries[4] = "Slovenia";
		countries[5] = "Macedonia";
		countries[6] = "Bosnia & Herzegovina";

		firstProcessPage(startURL);

		for (int i = 0; i < 7; i++) {
			countryProcessed = countries[i];
			processPage(URLList[i]);
		}

	}

	private static void fillAlbumRelations(List<String> list, String name, int idAlbum) throws SQLException {
		PreparedStatement st;
		ResultSet rs;
		boolean flag = false;
		int idAttribute = 0;
		for (int i = 0; i < list.size(); i++) {
			Statement stmt = db.conn.createStatement();
			rs = stmt.executeQuery("SELECT id" + name + " FROM " + name + " where name='" + list.get(i) + "';");
			if (rs.next()) {
				idAttribute = rs.getInt(1);
				flag = true;
			}
			rs.close();
			stmt.close();
			if (flag) {
				st = db.conn.prepareStatement("INSERT INTO album" + name.toLowerCase() + "(idA" + name.charAt(0)
						+ ", idAlbum, id" + name + ") " + "VALUES(NULL,?, ?)");
				st.setInt(1, idAlbum);
				st.setInt(2, idAttribute);
				st.executeUpdate();
				st.close();
			}
		}
	}

	public static void processAlbum(String url) throws IOException, SQLException {
		Document doc = Jsoup.connect(url).timeout(300000).get();
		Elements div = doc.getElementsByClass("profile");
		AlbumBean ab = new AlbumBean();
		Elements atitle = doc.getElementsByTag("title");
		String albumName = atitle.text();
		albumName = albumName.substring(albumName.indexOf("-") + 1);
		if (albumName.contains("("))
			albumName = albumName.substring(0, albumName.indexOf("(") - 1);
		ab.setName(albumName);
		if (Pattern.matches(".*\\p{InCyrillic}.*", ab.getName())) {
			ab.setAlphabet("C");
		}

		Elements content = div.get(0).getElementsByClass("head");

		for (Element e : content) {
			switch (e.text()) {
			case "Format:": {
				// format
				String[] fs = e.nextElementSibling().text().split(", ");
				for (String f : fs) {
					if (f.contains("File")) {
						int pos = f.indexOf("File");
						f = f.substring(pos);
					} else if (f.contains("CD")) {
						int pos = f.indexOf("CD");
						f = f.substring(pos);
					}
					ab.addListString(ab.getFormats(), f);
				}
				break;
			}
			case "Country:": {
				// country
				ab.setCountry(e.nextElementSibling().text());
				break;
			}
			case "Year:":
			case "Released:": {
				// year
				if (e.nextElementSibling().text().length() > 4) {
					int pos = e.nextElementSibling().text().lastIndexOf(" ");
					String yy = e.nextElementSibling().text().substring(pos + 1);
					ab.setYear(Integer.parseInt(yy));
				} else if (e.nextElementSibling().text().length() > 0) {
					ab.setYear(Integer.parseInt(e.nextElementSibling().text()));
				}
				break;
			}
			case "Genre:": {
				// genre
				Elements gen = e.nextElementSibling().getElementsByTag("a");
				for (Element aref : gen) {
					ab.addListString(ab.getGenres(), aref.text());
				}
				break;
			}
			case "Style:": {
				// style
				String[] fs = e.nextElementSibling().text().split(", ");
				for (String f : fs) {
					ab.addListString(ab.getStyles(), f);
				}
				break;
			}

			}
			// System.out.println(content.get(i).text());
		}
		// System.out.println("--------------------");

		// verzije albuma

		if (url.contains("/master/")) {
			div = doc.getElementsByClass("m_versions").get(0).getElementsByTag("h3");
			String num = div.text().substring(div.text().indexOf("(") + 1, div.text().length() - 1);
			ab.setVersions(Integer.parseInt(num));
		}
		if (ab.getCountry() == null) {
			ab.setCountry(countryProcessed);
		}
		// ab.printAlbum();
		int idAlbum = 0;
		PreparedStatement st;
		try {
			// insert u ALBUM
			st = db.conn.prepareStatement("INSERT INTO album(idAlbum, Name, Country, yearReleased, versions, alphabet) "
					+ "VALUES(NULL,?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			st.setString(1, ab.getName());
			st.setString(2, ab.getCountry());
			st.setInt(3, ab.getYear());
			st.setInt(4, ab.getVersions());
			st.setString(5, ab.getAlphabet());
			st.executeUpdate();
			ResultSet rs = st.getGeneratedKeys();
			idAlbum = rs.next() ? rs.getInt(1) : 0;
			rs.close();
			st.close();
			int idAttribute;

			/*
			 * System.out.println("-------------------------");
			 * System.out.println("-------------------------"); System.out.println("Album "
			 * + ab.getName() + " " + idAlbum);
			 */

			// insert u AlbumGenre, AlbumStyle, AlbumFormat
			fillAlbumRelations(ab.getGenres(), "Genre", idAlbum);
			fillAlbumRelations(ab.getStyles(), "Style", idAlbum);
			fillAlbumRelations(ab.getFormats(), "Format", idAlbum);

		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		div = doc.select("span.tracklist_track_title");
		Elements divTime = doc.getElementsByClass("tracklist_track_duration");

		int i = 0;
		for (Element lista : div) {
			// lista.text() je naziv pesme
			// sec je trajanje pesme
			int sec = 0;
			if (!divTime.get(i).text().equals("")) {
				String[] split = divTime.get(i).text().split(":");
				sec = (Integer.parseInt(split[0])) * 60 + Integer.parseInt(split[1]);
			}
			int idSong = 0;
			try {
				st = db.conn.prepareStatement("INSERT INTO song(idSong, Name, Length) VALUES(NULL,?, ?)",
						Statement.RETURN_GENERATED_KEYS);
				st.setString(1, lista.text());
				st.setInt(2, sec);
				st.executeUpdate();
				ResultSet rs = st.getGeneratedKeys();
				idSong = rs.next() ? rs.getInt(1) : 0;
				rs.close();
				st.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			Elements divAuthor = doc.select("span.tracklist_content_multi_artist_dash");

			if (!divAuthor.isEmpty()) {
				Element auth = divAuthor.get(i).nextElementSibling();

				while (auth != null) {
					String authName = auth.text();
					// System.out.println("VARIOUS AUTOR JE:" + authName);
					processAuthors(auth.attr("abs:href"));
					// OVDE UBACITI U TABELU
					try {
						int idAuthor = 0;
						Statement stmt = db.conn.createStatement();
						ResultSet rs = stmt.executeQuery("SELECT idArtist FROM artist WHERE name='" + authName + "';");
						if (rs.next()) {
							idAuthor = rs.getInt(1);
						}
						rs.close();
						stmt.close();

						st = db.conn.prepareStatement(
								"INSERT INTO songartalbum(idSAA, idSong, idArtist, idAlbum) VALUES(NULL,?, ?, ?)");
						st.setInt(1, idSong);
						st.setInt(2, idAuthor);
						st.setInt(3, idAlbum);
						st.executeUpdate();
						st.close();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}

					auth = auth.nextElementSibling();
					if (!auth.is("a")) {
						break;
					}

				}
			} else {
				// UBACITI SA JEDNIM AUTOROM
				try {
					Elements title = doc.getElementsByClass("profile").get(0)
							.getElementsByAttributeValue("itemprop", "byArtist").get(0).getElementsByTag("a");
					for (Element t : title) {
						String artist = t.text();
						int idAuthor = 0;
						Statement stmt = db.conn.createStatement();
						ResultSet rs = stmt.executeQuery("SELECT idArtist FROM artist WHERE name='" + artist + "';");
						if (rs.next()) {
							idAuthor = rs.getInt(1);
						}
						rs.close();
						stmt.close();

						PreparedStatement st1 = db.conn.prepareStatement(
								"INSERT INTO songartalbum(idSAA, idSong, idArtist, idAlbum) VALUES(NULL,?, ?, ?)");
						st1.setInt(1, idSong);
						st1.setInt(2, idAuthor);
						st1.setInt(3, idAlbum);
						st1.executeUpdate();
						st1.close();
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}

			i++;
		}

	}

	public static void firstProcessPage(String URL) throws SQLException, IOException {
		Document doc = Jsoup.connect(URL).get();
		Element div = doc.getElementById("facets_genre_exact");
		Elements refs = div.select("a");

		for (Element lista : refs) {
			int pos = lista.text().indexOf(" ");
			String genre = lista.text().substring(pos + 1);
			PreparedStatement st = db.conn.prepareStatement("INSERT INTO genre(idGenre, Name) VALUES(NULL,?)");
			st.setString(1, genre);
			st.executeUpdate();
			st.close();
		}

		div = doc.getElementById("facets_style_exact");
		refs = div.select("a");

		for (Element lista : refs) {
			int pos = lista.text().indexOf(" ");
			String style = lista.text().substring(pos + 1);
			PreparedStatement st = db.conn.prepareStatement("INSERT INTO style(idStyle, Name) VALUES(NULL,?)");
			st.setString(1, style);
			st.executeUpdate();
			st.close();
		}

		div = doc.getElementById("facets_format_exact");
		refs = div.select("a");

		for (Element lista : refs) {
			int pos = lista.text().indexOf(" ");
			String format = lista.text().substring(pos + 1);
			PreparedStatement st = db.conn.prepareStatement("INSERT INTO format(idFormat, Name) VALUES(NULL,?)");
			st.setString(1, format);
			st.executeUpdate();
			st.close();
		}
	}

	public static void processPage(String URL) throws SQLException, IOException {
		
		Document doc = null;
		// get useful informationt
		while (doc == null) {
			try {
				doc = Jsoup.connect(URL).timeout(0).get();
			} catch (HttpStatusException e) {
				int i = 0;
				while (i < 15000)
					i++;
			}
		}

		// -----------------------------------------------------------------------
		// za dohvatanje izvodjaca sa stranice

		Elements authordocs = doc.select("a[href*=/artist]");
		// ****lista linkova albuma****
		for (Element lista : authordocs) {

			String authorAbsHref = lista.attr("abs:href");
			
			try {
				processAuthors(authorAbsHref);
			} catch (HttpStatusException e) {
				int i = 0;
				while (i < 15000)
					i++;
			}

		}

		// -----------------------------------------------------------------------
		// za dohvatanje albuma sa stranice

		Element div = doc.getElementById("search_results");
		Elements docs = div.select("h4").select("a[href]");

		// ****lista linkova albuma****
		for (Element lista : docs) {

			String absHref = lista.attr("abs:href");
			try {
				processAlbum(absHref);
			} catch (SQLException e) {

			} catch (HttpStatusException e) {
				int i = 0;
				while (i < 15000)
					i++;
			} catch (Exception e) {

			}
		}
		// ------------------------------------------------------------------------
		// ****link slecede stranice
		Elements nextdiv = doc.getElementsByClass("pagination_next");
		Elements nextdocs = nextdiv.select("a[href]");

		if (nextdocs.isEmpty() == true) {
			// System.out.println("Nema sledece strane");
		} else {
			String absHrefnext = nextdocs.get(0).attr("abs:href");
			System.out.println("Sleceda stranica " + absHrefnext);
			processPage(absHrefnext);
		}
	}

	public static void processAuthors(String URL) throws SQLException, IOException {

		// kolone tabele
		String ArtistName = "";
		int Credits = 0;
		int Vocals = 0;
		int WritingArrangements = 0;

		Document doc = Jsoup.connect(URL).timeout(30000).get();
		Elements div = doc.getElementsByClass("credit_type");

		Element namediv = doc.getElementById("page_content");
		Element autname = namediv.selectFirst("h1");

		ArtistName = autname.text();

		for (Element lista : div) {
			String toProcess = lista.text();

			if (toProcess.contains("Credits")) {
				int pos = toProcess.indexOf(" ");
				String strCredits = toProcess.substring(0, pos);
				Credits = Integer.parseInt(strCredits);
			}

			if (toProcess.contains("Vocals")) {
				int pos = toProcess.indexOf(" ");
				String strVocals = toProcess.substring(0, pos);
				Vocals = Integer.parseInt(strVocals);
			}

			if (toProcess.contains("Writing")) {
				int pos = toProcess.indexOf(" ");
				String strWritingArrangements = toProcess.substring(0, pos);
				WritingArrangements = Integer.parseInt(strWritingArrangements);
			}

		}
		// ****DODATI INSERT U BAZU****
		PreparedStatement st = db.conn.prepareStatement(
				"INSERT IGNORE INTO artist(idArtist, Name, Credits, Vocals, Writing) VALUES(NULL,?, ?, ?, ?)");
		st.setString(1, ArtistName);
		st.setInt(2, Credits);
		st.setInt(3, Vocals);
		st.setInt(4, WritingArrangements);
		st.executeUpdate();
		st.close();
	}
}
