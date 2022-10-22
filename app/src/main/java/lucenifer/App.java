package lucenifer;

import lucenifer.input.ConsoleInputController;
import lucenifer.input.UserQueryParser;
import lucenifer.input.exception.UserQuerySyntaxException;
import lucenifer.input.model.UserQuery;
import lucenifer.search.SearchController;
import lucenifer.search.model.QueryResult;

import java.util.List;

public class App {
	private static final String INDEX_PATH = "target";
	private static final String DATA_PATH = "./../data";
	private static final String DEFAULT_USER_QUERY_SEPARATOR = "=";

	public static void main(String[] args) {
		SearchController searchController = new SearchController(DATA_PATH, INDEX_PATH);
		searchController.indexDocs();
		UserQueryParser userQueryParser = new UserQueryParser(DEFAULT_USER_QUERY_SEPARATOR);

		System.out.println("Type a query using the syntax <field>=<value> (ex.: title=network):");
		String input = ConsoleInputController.readString();

		UserQuery query;
		try {
			query = userQueryParser.parseString(input);
			List<QueryResult> results = searchController.doSearch(query.getFieldName(), query.getParameter());
			results.forEach(System.out::println);
		} catch (UserQuerySyntaxException e) {
			System.out.println("Invalid syntax: " + e.getMessage());
		}
	}
}
