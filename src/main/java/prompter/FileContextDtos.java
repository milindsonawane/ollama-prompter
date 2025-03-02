package prompter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileContextDtos {
	public static record FileContent(Path name, List<String> contents) {
		public String getMinifiedContents() {
			return contents.stream().map(String::trim).collect(Collectors.joining("\n"));
		}
	}
	public static record FileContext(FileContent root, List<FileContent> children) {
		public String getMinifiedContents() {
			List<String> list = children.stream().map(FileContent::getMinifiedContents).collect(Collectors.toList());
			list.add(root.getMinifiedContents());
			return list.stream().collect(Collectors.joining("\n\n"));
		}
	}
}
