package lucenifer.search;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lucenifer.search.model.QueryResult;
import lucenifer.search.model.RawDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class SearchController {
	private static final String TITLE_FIELD_NAME = "title";
	private static final String CONTENT_FIELD_NAME = "content";

	private boolean indexed;

	private final String indexPath;

	private final String dataPath;

	public SearchController(String dataPath, String indexPath) {
		this.indexed = false;
		this.indexPath = indexPath;
		this.dataPath = dataPath;
	}

	public List<QueryResult> doSearch(String field, String queryString) {
		if (!indexed) {
			throw new IllegalStateException("You must create an index before performing queries");
		}

		QueryParser parser = new QueryParser(field, new WhitespaceAnalyzer());
		try {
			Query query = parser.parse(queryString);
			try (Directory directory = FSDirectory.open(Paths.get(indexPath))) {
				try (IndexReader reader = DirectoryReader.open(directory)) {
					IndexSearcher searcher = new IndexSearcher(reader);
					return runQuery(searcher, query);
				}
			}
		} catch (IOException | ParseException e) {
			log.error("", e);
			return Collections.emptyList();
		}
	}

	private List<QueryResult> runQuery(IndexSearcher searcher, Query query) {
		return runQuery(searcher, query, false);
	}

	private List<QueryResult> runQuery(IndexSearcher searcher, Query query, boolean explain) {
		try {
			List<QueryResult> results = new ArrayList<>();
			TopDocs hits = searcher.search(query, 10);
			for (int i = 0; i < hits.scoreDocs.length; i++) {
				ScoreDoc scoreDoc = hits.scoreDocs[i];
				Document doc = searcher.doc(scoreDoc.doc);
				Explanation explanation = explain ? searcher.explain(query, scoreDoc.doc) : null;
				results.add(new QueryResult(doc, scoreDoc.score, explanation));
			}
			return results;
		} catch (IOException e) {
			log.error("", e);
			return Collections.emptyList();
		}
	}

	public void indexDocs() {
		List<RawDocument> rawDocuments = readAllDocumentsFromDirectory(dataPath);
		List<Document> documents = rawDocuments.stream()
				.map(rd -> {
					Document doc = new Document();
					TextField title = new TextField(TITLE_FIELD_NAME, rd.getTitle(), Field.Store.YES);
					TextField content = new TextField(CONTENT_FIELD_NAME, rd.getContent(), Field.Store.YES);
					doc.add(title);
					doc.add(content);
					return doc;
				})
				.collect(Collectors.toList());

		Path path = Paths.get(indexPath);
		IndexWriter writer;
		try (Directory indexDirectory = FSDirectory.open(path)) {
			Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
			perFieldAnalyzers.put(TITLE_FIELD_NAME, new WhitespaceAnalyzer());
			perFieldAnalyzers.put(CONTENT_FIELD_NAME, new WhitespaceAnalyzer());

			Analyzer analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), perFieldAnalyzers);
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(indexDirectory, config);
			writer.deleteAll();
			documents.stream().parallel().forEach(d -> {
				try {
					writer.addDocument(d);
				} catch (IOException e) {
					log.error("", e);
				}
			});
			writer.commit();
			writer.close();
			this.indexed = true;
		} catch (IOException e) {
			log.error("", e);
		}
	}

	private List<RawDocument> readAllDocumentsFromDirectory(String path) {
		File directory = new File(path);
		File[] files = directory.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(files)
				.map(RawDocument::fromFile)
				.collect(Collectors.toList());
	}
}
