package prompter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import prompter.FileContextDtos.FileContent;
import prompter.FileContextDtos.FileContext;

public class ContextExtractor {

	private static final String SRC_MAIN_JAVA = "src/main/java";
	private final Path projectPath;
	private final List<String> fullyQualifiedClassNames;
	
	private ContextExtractor(Path projectPath, List<String> fullyQualifiedClassNames) {
		this.projectPath = projectPath;
		this.fullyQualifiedClassNames = fullyQualifiedClassNames;
	}
	
	public static Builder getBuilder() {
		return new Builder();
	}

	public static class Builder {
		Path projectPath;
		Set<String> fullyQualifiedClassNames = new HashSet<>();

		public Builder forProject(Path projectPath) {
			this.projectPath = projectPath;
			return this;
		}

		public Builder forClasses(String... fullyQualifiedClassNames) {
			for (String clazz : fullyQualifiedClassNames) {
				this.fullyQualifiedClassNames.add(clazz);
			}
			return this;
		}

		public ContextExtractor build() {
			return new ContextExtractor(projectPath, new ArrayList<>(fullyQualifiedClassNames));
		}
	}

	public List<FileContext> extract() {
		List<Path> targetFiles = fullyQualifiedClassNames.stream().map(this::absolutePath).toList();
		List<FileContext> retList = new ArrayList<>();
		for (Path target : targetFiles) {
			if (Files.notExists(target))
				continue;
			
			Map<Path, List<String>> itrContentHolder = new HashMap<>();
			fileContents(target, itrContentHolder);
			retList.add(
				new FileContext(
					new FileContent(target, itrContentHolder.get(target)), 
					itrContentHolder.entrySet().stream().filter(e -> !e.getKey().equals(target)).map(e -> new FileContent(e.getKey(), e.getValue())).toList()
					)
				);
		}
		
		System.out.println("From the given classes: [%s], only the following exist: [%s]".formatted(fullyQualifiedClassNames, retList.stream().map(f -> f.root().name()).toList()));
		return retList;
	}

	private Path absolutePath(String fileName) {
		return projectPath.resolve(SRC_MAIN_JAVA).resolve(fileName.replace(".", File.separator).concat(".java"));
	}
	
	private void fileContents(Path file, Map<Path, List<String>> itrContentHolder) {
		if (itrContentHolder.containsKey(file)) return;
		
		try (Stream<String> stream = Files.lines(file)) {
			List<String> lines = stream.toList();
			itrContentHolder.putIfAbsent(file, lines);

			lines.stream()
				.filter(l -> l.startsWith("import"))
				.map(l -> l.replace("import", "").replace(";", "").trim())
				.map(this::absolutePath)
				.filter(Files::exists)
				.filter(p -> !itrContentHolder.containsKey(p))
				.forEach(f -> fileContents(f, itrContentHolder));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
