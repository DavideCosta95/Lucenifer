package lucenifer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.tests.analysis.TokenStreamToDot;

@Slf4j
public class SamplesTest {
	public void testIndexStatistics() throws Exception {
		Path path = Paths.get("target/idx0");

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory, new SimpleTextCodec());
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				Collection<String> indexedFields = FieldInfos.getIndexedFields(reader);
				for (String field : indexedFields) {
					log.info("{}", searcher.collectionStatistics(field));
				}
			}
		}
	}

	public void testIndexingAndSearchAll() throws Exception {
		Path path = Paths.get("target/idx3");

		Query query = new MatchAllDocsQuery();

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testIndexingAndSearchTQ() throws Exception {
		Path path = Paths.get("target/idx2");

		Query query = new TermQuery(new Term("titolo", "Ingegneria"));

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testIndexingAndSearchTQOnStringField() throws Exception {
		Path path = Paths.get("target/idx7");

		Query query = new TermQuery(new Term("data", "12 ottobre 2016"));

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testIndexingAndSearchPQ() throws Exception {
		Path path = Paths.get("target/idx4");

		PhraseQuery query = new PhraseQuery.Builder()
				.add(new Term("contenuto", "data"))
				.add(new Term("contenuto", "scientist"))
				.build();

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testIndexingAndSearchBQ() throws Exception {
		Path path = Paths.get("target/idx5");

		PhraseQuery phraseQuery = new PhraseQuery.Builder()
				.add(new Term("contenuto", "data"))
				.add(new Term("contenuto", "scientist"))
				.build();

		TermQuery termQuery = new TermQuery(new Term("titolo", "Ingegneria"));

		BooleanQuery query = new BooleanQuery.Builder()
				.add(new BooleanClause(termQuery, BooleanClause.Occur.SHOULD))
				.add(new BooleanClause(phraseQuery, BooleanClause.Occur.SHOULD))
				.build();

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testIndexingAndSearchQP() throws Exception {
		Path path = Paths.get("target/idx1");

		QueryParser parser = new QueryParser("contenuto", new WhitespaceAnalyzer());
		Query query = parser.parse("+ingegneria dei +dati");

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory);
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	public void testRankingWithDifferentSimilarities() throws Exception {
		Path path = Paths.get(Files.createTempDirectory("target").toUri());
		try (Directory directory = FSDirectory.open(path)) {
			QueryParser parser = new MultiFieldQueryParser(new String[]{"contenuto", "titolo"}, new WhitespaceAnalyzer());
			Query query = parser.parse("ingegneria dati data scientist");

			indexDocs(directory);
			Collection<Similarity> similarities = List.of(new ClassicSimilarity(), new BM25Similarity(2.5f, 0.2f),
					new LMJelinekMercerSimilarity(0.1f));
			for (Similarity similarity : similarities) {
				try (IndexReader reader = DirectoryReader.open(directory)) {
					IndexSearcher searcher = new IndexSearcher(reader);
					searcher.setSimilarity(similarity);
					log.info("Using {}", similarity);
					runQuery(searcher, query, true);
				}
			}
		}
	}

	public void testIndexingAndSearchAllWithCodec() throws Exception {
		Path path = Paths.get("target/idx6");
		Query query = new MatchAllDocsQuery();

		try (Directory directory = FSDirectory.open(path)) {
			indexDocs(directory, new SimpleTextCodec());
			try (IndexReader reader = DirectoryReader.open(directory)) {
				IndexSearcher searcher = new IndexSearcher(reader);
				runQuery(searcher, query);
			}
		}
	}

	private void runQuery(IndexSearcher searcher, Query query) throws IOException {
		runQuery(searcher, query, false);
	}

	private void runQuery(IndexSearcher searcher, Query query, boolean explain) throws IOException {
		TopDocs hits = searcher.search(query, 10);
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = searcher.doc(scoreDoc.doc);
			log.info("doc{}: {} ({})", scoreDoc.doc, doc.get("titolo"), scoreDoc.score);
			if (explain) {
				Explanation explanation = searcher.explain(query, scoreDoc.doc);
				log.info("{}", explanation);
			}
		}
	}

	private void indexDocs(Directory directory) throws IOException {
		indexDocs(directory, null);
	}

	private void indexDocs(Directory directory, Codec codec) throws IOException {
		Analyzer defaultAnalyzer = new StandardAnalyzer();
		CharArraySet stopWords = new CharArraySet(List.of("in", "dei", "di"), true);
		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
		perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer());

		Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if (codec != null) {
			config.setCodec(codec);
		}
		IndexWriter writer = new IndexWriter(directory, config);
		writer.deleteAll();

		Document doc1 = new Document();
		doc1.add(new TextField("titolo", "Come diventare un ingegnere dei dati, Data Engineer?", Field.Store.YES));
		doc1.add(new TextField("contenuto", "Sembra che oggigiorno tutti vogliano diventare un Data Scientist  ...", Field.Store.YES));
		doc1.add(new StringField("data", "12 ottobre 2016", Field.Store.YES));

		Document doc2 = new Document();
		doc2.add(new TextField("titolo", "Curriculum Ingegneria dei Dati - Sezione di Informatica e Automazione", Field.Store.YES));
		doc2.add(new TextField("contenuto", "Curriculum. Ingegneria dei Dati. Laurea Magistrale in Ingegneria Informatica ...", Field.Store.YES));

		writer.addDocument(doc1);
		writer.addDocument(doc2);

		writer.commit();
		writer.close();
	}

	public void testAnalyzer() throws Exception {
		CharArraySet stopWords = new CharArraySet(List.of("di", "a", "da", "dei", "il", "la"), true);
		TokenStream ts;
		try (Analyzer a = new StandardAnalyzer(stopWords)) {
			ts = a.tokenStream(null, "Come diventare un ingegnere dei dati,");
		}
		StringWriter w = new StringWriter();
		new TokenStreamToDot(null, ts, new PrintWriter(w)).toDot();
		log.info("{}", w);
	}

}