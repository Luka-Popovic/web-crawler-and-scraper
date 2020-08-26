import java.util.*;

public class AlbumBean {
	private String name;
	private String country;	
	private int year;
	private int versions;
	private String alphabet = "L";
	private List<String> genres = new ArrayList<>();
	private List<String> styles = new ArrayList<>();
	private List<String> formats = new ArrayList<>();
	private List<Song> songs = new ArrayList<>();
	
	public AlbumBean() {}
	
	public AlbumBean(String name, String country, int year, int versions, List<String> genres, List<String> styles,
			List<String> formats, List<Song> songs) {
		super();
		this.name = name;
		this.country = country;
		this.year = year;
		this.versions = versions;
		this.genres = genres;
		this.styles = styles;
		this.formats = formats;
		this.songs = songs;
	}
	
	
	
	public String getAlphabet() {
		return alphabet;
	}

	public void setAlphabet(String alphabet) {
		this.alphabet = alphabet;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public int getVersions() {
		return versions;
	}
	public void setVersions(int versions) {
		this.versions = versions;
	}
	public List<String> getGenres() {
		return genres;
	}
	public void setGenres(List<String> genres) {
		this.genres = genres;
	}
	public List<String> getStyles() {
		return styles;
	}
	public void setStyles(List<String> styles) {
		this.styles = styles;
	}
	public List<String> getFormats() {
		return formats;
	}
	public void setFormats(List<String> formats) {
		this.formats = formats;
	}
	public List<Song> getSongs() {
		return songs;
	}
	public void setSongs(List<Song> songs) {
		this.songs = songs;
	}
	
	public void addListString(List<String> list, String str) {
		if (!str.equals("")) {
			list.add(str);
		}		
	}
	
	public void printAlbum() {
		System.out.println("Name " + name);
		System.out.println("Country " + country);
		System.out.println("Year " + year);
		System.out.println("Version " + versions);
		System.out.println("Genres ");
		for(String g:genres) {
			System.out.println(g);
			
		}
		System.out.println("Styles ");
		for(String g:styles) {
			System.out.println(g);
			
		}
		System.out.println("Formats ");
		for(String g:formats) {
			System.out.println(g);
			
		}
		if (alphabet.equals("C")) {
			System.out.println("CIRILICA");
		}
		
	}
	
	
}
