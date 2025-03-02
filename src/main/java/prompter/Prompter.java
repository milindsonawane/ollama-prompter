package prompter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import prompter.FileContextDtos.FileContext;

public class Prompter {
	private final String host;
	private final String model;
	private static final String PROMPT = """
			Given the code below, write junit 5 test case for [%s]. 
			For each test case, write javadocs in given, when, then format explaining the test case. 
			The provided test cases must compile and run. 
			Mock any database connection by returning pre-determined responses. 
			The root and referred classes are defined as: %s
			""";

	private Prompter(String host, String model) {
		this.host = host;
		this.model = model;
	}
	
	public static class Builder {
		private String host;
		private String model;
		
		public Builder onHost(String host) {
			this.host = host;
			return this;
		}
		
		public Builder withModel(String model) {
			this.model = model;
			return this;
		}
		
		public Prompter build() {
			return new Prompter(host, model);
		}
	}

	public static Builder getBuilder() {
		return new Builder();
	}
	
	public String unitTestFor(FileContext fileContext) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("model", model)
				  .put("stream", false)
				  .put("prompt", PROMPT.formatted(fileContext.root().name().getFileName(), fileContext.getMinifiedContents()));
		HttpRequest request = HttpRequest.newBuilder()
										.uri(URI.create(host))
										.POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
										.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
}
