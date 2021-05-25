package pmanexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.sqlite.SQLiteConfig;

/**
 * Extract attached files from a PMAN3 database
 * This program reads the FILES table in the DB 
 * and decodes the content stored in a utf-8 string. 
 */
public class FileExportMain {

	public static void main(String[] args) {

		String url = "jdbc:sqlite:bibdat.db";
		SQLiteConfig config = new SQLiteConfig(); 
		config.setEncoding(SQLiteConfig.Encoding.UTF8);
		try (Connection conn = DriverManager.getConnection(url, config.toProperties())) {
			
			Statement s = conn.createStatement();
			try (ResultSet rs = s.executeQuery("select * from files;")) {
				while (rs.next()) {
					int publicationId = rs.getInt("pid");
					
					// Select file extension based on the mime type
					String mimeType = rs.getString("mimetype");
					String filename = "naistse-" + Integer.toString(publicationId);
					if (mimeType.equals("application/pdf")) { 
						filename = filename + ".pdf";
					} else if (mimeType.equals("application/postscript")) {
						filename = filename + ".ps";
					} else if (mimeType.equals("application/zip")) {
						filename = filename + ".zip";
					} else if (mimeType.equals("application/vnd.ms-powerpoint")) {
						filename = filename + ".ppt";
					}
					
					try {
						File f = new File(filename);
						while (f.exists()) {
							f = new File(f.getName() + ".2");
						}
						// Decode utf-8 text to binary
						InputStreamReader r = new InputStreamReader(rs.getBinaryStream("file"), Charset.forName("utf-8"));
						try (FileOutputStream w = new FileOutputStream(f)) {
							int x = r.read();
							while (x != -1) {
								w.write(x);
								x = r.read();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
