package lucenifer.search.model;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Data
@Slf4j
public class RawDocument {
	@NonNull
	private String title;

	@NonNull
	private String content;

	public static RawDocument fromFile(@NonNull File f) {
		return new RawDocument(f.getName(), readFileContent(f));
	}

	private static String readFileContent(File f) {
		StringBuilder sb = new StringBuilder();
		try (FileInputStream fis = new FileInputStream(f);
			 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
			 BufferedReader reader = new BufferedReader(isr)
		) {
			String str;
			while ((str = reader.readLine()) != null) {
				sb.append(str);
			}
		} catch (IOException e) {
			log.error("", e);
		}
		return sb.toString();
	}
}
