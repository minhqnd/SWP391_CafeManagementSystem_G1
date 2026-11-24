package dao;

import context.DBContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Reads schema metadata only; does not add or change application records. */
@Tag("database")
class Bf10Bf11SchemaTest {
    private record Guard(String table, Set<String> columns) { }

    @TestFactory Stream<DynamicTest> requiredUniqueIndexesExistInTheApplicationDatabase() {
        List<Guard> guards = List.of(new Guard("users", Set.of("phone")), new Guard("users", Set.of("email")),
                new Guard("products", Set.of("name")), new Guard("categories", Set.of("name")),
                new Guard("product_ingredients", Set.of("product_id", "ingredient_id")));
        return guards.stream().map(guard -> DynamicTest.dynamicTest(guard.table() + " unique " + guard.columns(), () -> {
            try (Connection conn = DBContext.getConnection()) {
                conn.setReadOnly(true);
                Map<String,Set<String>> indexes = new HashMap<>();
                try (ResultSet rs = conn.getMetaData().getIndexInfo(conn.getCatalog(), null, guard.table(), true, false)) {
                    while (rs.next()) {
                        String index = rs.getString("INDEX_NAME"), column = rs.getString("COLUMN_NAME");
                        if (index != null && column != null) indexes.computeIfAbsent(index, key -> new HashSet<>()).add(column);
                    }
                }
                assertTrue(indexes.containsValue(guard.columns()), "Missing unique constraint for " + guard.table() + " " + guard.columns());
            }
        }));
    }
}
