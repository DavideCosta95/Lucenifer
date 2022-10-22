package lucenifer.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class QueryResult {
	@NonNull
	private Document document;

	private float score;

	private Explanation explanation;

	public boolean hasExplanation() {
		return this.explanation != null;
	}
}
