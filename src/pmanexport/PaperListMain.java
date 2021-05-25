package pmanexport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import org.sqlite.SQLiteConfig;

/**
 * Extract attached files from a PMAN3 database
 * This program reads the FILES table in the DB 
 * and decodes the content stored in a utf-8 string. 
 */
public class PaperListMain {
	

	private static final String[] months = { "", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

	public static class BibEntry implements Comparable<BibEntry> {
	
		private int publicationId;
		private String content;
		private int month;
		private int year;
		private int filecount;
		
		public BibEntry(int pubId, String content, int month, int year, int filecount) {
			this.publicationId = pubId;
			this.content = content;
			this.month = month;
			this.year = year;
			this.filecount = filecount;
		}
		
		@Override
		public int compareTo(BibEntry o) {
			if (this.year == o.year) {
				if (this.month == o.month) {
					return this.content.compareTo(o.content);
				}
				return o.month - this.month;
			}
			return o.year - this.year;
		}
		
	}
	
	
	public static void main(String[] args) {

		String url = "jdbc:sqlite:bibdat.db";
		SQLiteConfig config = new SQLiteConfig(); 
		config.setEncoding(SQLiteConfig.Encoding.UTF8);
		try (Connection conn = DriverManager.getConnection(url, config.toProperties())) {
			
			PreparedStatement pre = conn.prepareStatement("select count(*) as filecount from files where pid = ?");

			// Read entries from the bib table
			Statement s = conn.createStatement();
			ArrayList<BibEntry> entries = new ArrayList<>();
			try (ResultSet rs = s.executeQuery("select * from bib;")) {
				while (rs.next()) {
					int publicationId = rs.getInt("id");
					String authors = rs.getString("author");
					authors = authors.replaceAll(",", ", ");
					authors = authors.replaceAll("  ", " ");
					String title = rs.getString("title");
					String journal = rs.getString("journal");
					String booktitle = rs.getString("booktitle");
					String series = rs.getString("series");
					String chapter = rs.getString("chapter");
					String volume = rs.getString("volume");
					String number = rs.getString("number");
					String pages = rs.getString("pages");
					int year = rs.getInt("year");
					int month = rs.getInt("month");
					
					StringBuilder buf = new StringBuilder();
					buf.append(authors);
					buf.append(": ");
					buf.append(title);
					if (journal != null && !journal.isEmpty()) {
						buf.append(", ");
						buf.append(journal);
					}
					if (booktitle != null && !booktitle.isEmpty()) {
						buf.append(", ");
						buf.append(booktitle);
					}
					if (series != null && !series.isEmpty()) {
						buf.append(", ");
						buf.append(series);
					}
					if (chapter != null && !chapter.isEmpty()) {
						buf.append(", chapter ");
						buf.append(chapter);
					}
					if (volume != null && !volume.isEmpty()) {
						buf.append(", volume ");
						buf.append(volume);
					}
					if (number != null && !number.isEmpty()) {
						buf.append(", number ");
						buf.append(number);
					}
					if (pages != null && !pages.isEmpty()) {
						buf.append(", pages ");
						buf.append(pages);
					}
					buf.append(", ");
					buf.append(months[month]);
					buf.append(" ");
					buf.append(year);
					String content = buf.toString();
					
					
					int filecount = 0;
					pre.setInt(1, publicationId);
					try (ResultSet filesearch = pre.executeQuery()) {
						if (filesearch.next()) {
							filecount = filesearch.getInt("filecount");
						}
					}					
					
					entries.add(new BibEntry(publicationId, content, month, year, filecount));
					
				}
				
				// Sort and print the contents
				Collections.sort(entries);
				for (BibEntry e: entries) {
					if (e.year == 9999) continue;
					
					StringBuilder buf = new StringBuilder();
					buf.append("<li>");
					buf.append(e.content);
					buf.append(" ");
					
					String ext = ".pdf";
					// hard-coded rules for NAIST-SE papers
					if (e.publicationId == 13) ext = ".ps";
					if (e.publicationId == 425 || e.publicationId == 426) ext = ".ppt";
					
					for (int i=0; i<e.filecount; i++) {
						if (i==0) {
							buf.append("<a href=\"publications/naistse-" + e.publicationId + ext + "\">[file]</a> ");
						} else {
							buf.append("<a href=\"publications/naistse-" + e.publicationId + "-" + (i+1) + ext + "\">[file]</a> ");
						}
					}
					buf.append("</li>");
					System.out.println(buf.toString());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
