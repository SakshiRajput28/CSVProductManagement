package Api.Ignitiv_Project.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class CSVUtils {

	// OLD METHOD (keep it if needed elsewhere)
	public static List<String[]> readFullCsv(String filePath) throws Exception {

		List<String[]> rows = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

			String line;

			while ((line = br.readLine()) != null) {
				String[] values = line.split(",", -1);
				rows.add(values);
			}
		}

		return rows;
	}

	// NEW METHOD (this fixes your error)
	public static List<Map<String, String>> readCsv(String filePath) {

		List<Map<String, String>> rows = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

			String headerLine = br.readLine();
			if (headerLine == null)
				return rows;

			String[] headers = headerLine.split(",");

			String line;

			while ((line = br.readLine()) != null) {

				String[] values = line.split(",", -1);

				Map<String, String> row = new HashMap<>();

				for (int i = 0; i < headers.length; i++) {

					String value = "";

					if (i < values.length) {
						value = values[i].trim();
					}

					row.put(headers[i].trim(), value);
				}

				rows.add(row);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return rows;
	}
}