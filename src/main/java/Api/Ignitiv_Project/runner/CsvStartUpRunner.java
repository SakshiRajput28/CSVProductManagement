package Api.Ignitiv_Project.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import Api.Ignitiv_Project.service.CSVProcessorService;

import java.nio.file.*;

@Component
public class CsvStartUpRunner implements CommandLineRunner {

    @Autowired
    private CSVProcessorService csvProcessorService;

    private static final String FILE_PATH =
            "C:/Users/Sakshi S Rajput/Desktop/catalogs/product9.csv";

    @Override
    public void run(String... args) throws Exception {
        Path folderPath = Paths.get(
                "C:/Users/Sakshi S Rajput/Desktop/catalogs"
        );

        WatchService watchService = FileSystems.getDefault().newWatchService();

        folderPath.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
        );

        // ✅ FIX 1 — Process CSV once when application starts
        System.out.println("Processing CSV at startup...");

        try {
            csvProcessorService.processCsv(FILE_PATH);
        } catch (Exception e) {
            System.err.println("Startup CSV processing failed: " + e.getMessage());
        }

        System.out.println("Watching CSV file changes...");

        while (true) {

            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {

                Path changedFile = (Path) event.context();

                if (changedFile.toString().equals("product9.csv")) {

                    System.out.println("CSV Updated! Processing...");

                    try {
                    	    Thread.sleep(500);
                        csvProcessorService.processCsv(FILE_PATH);
                    } catch (Exception e) {
                        System.err.println("CSV processing failed: " + e.getMessage());
                    }
                }
            }

            key.reset();
        }
    }
}