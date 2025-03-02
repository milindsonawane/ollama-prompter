package prompter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import prompter.FileContextDtos.FileContext;

public class PromptCollector {
	private static final Logger log = LoggerFactory.getLogger(PromptCollector.class);
	
	private static final String MODEL = "deepseek-r1:7b";
	private static final String HOST = "http://localhost:11434/api/generate";
	private static final String PROJECT_PATH = "C:\\Users\\milin\\codes\\self\\foodie";

	public static void main(String... args) throws IOException, InterruptedException {

		log.info("Performing validations, checking if project base path: [{}] exists", PROJECT_PATH);
		System.out.println("Performing validations, checking if project base path: [%s] exists".formatted(PROJECT_PATH));
		if (Files.notExists(Paths.get(PROJECT_PATH))) {
			log.error("Given directory does not exist, program will exit now.");
			System.exit(0);
		}
		
		Prompter prompter = Prompter.getBuilder().onHost(HOST).withModel(MODEL).build();
		List<FileContext> list = ContextExtractor.getBuilder()
												.forProject(Paths.get(PROJECT_PATH))
												.forClasses(
													"com.foodie.controllers.v1.CompanyController")
												.build().extract();
		if (list.isEmpty()) {
			log.error("No file found from the given list. Program will now exit.");
			System.exit(0);
		}
		
		log.info("Starting test generation. This may take a while, depending on the machine specs, network speed and model deployed.");
		for (FileContext file : list) {
			String testFileName = file.root().name().getFileName().toString().replace(".java", "Test.java");
			log.info("Generating test class: [{}] for file: [{}].", file.root().name(), testFileName);
			Files.write(Paths.get(testFileName), getContentToWrite(prompter.unitTestFor(file)));
		}
		
		log.info("\n\n\nFin.");
	}
	
	private static byte[] getContentToWrite(String response) {
		String string = new JSONObject(response).getString("response").toString();
		string = string.substring(string.indexOf("```java")).replace("```java", "");
		return string.substring(0, string.lastIndexOf("```")).getBytes();
	}
}
