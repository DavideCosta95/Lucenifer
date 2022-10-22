package lucenifer.input;

import lombok.Getter;
import lucenifer.input.exception.UserQuerySyntaxException;
import lucenifer.input.model.UserQuery;

import java.util.Arrays;
import java.util.List;

@Getter
public class UserQueryParser {
	private final String querySeparator;

	public UserQueryParser(String querySeparator) {
		this.querySeparator = querySeparator;
	}

	public UserQuery parseString(String s) throws UserQuerySyntaxException {
		String[] tokens = s.split(querySeparator);

		if (tokens.length < 2) {
			throw new UserQuerySyntaxException("Missing field value in query: " + s);
		}

		// handles queries containing field separator
		List<String> fieldValueTokens = Arrays.asList(tokens).subList(1, tokens.length);
		return new UserQuery(tokens[0], String.join(querySeparator, fieldValueTokens));
	}
}
