package lucenifer.input.model;

import lombok.Data;
import lombok.NonNull;

@Data
public class UserQuery {
	@NonNull
	private String fieldName;

	@NonNull
	private String parameter;
}
