package dataaccess;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PersonalTest {
    @Test
    void canConnect() throws Exception {
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement("SELECT 1+1");
             var rs = ps.executeQuery()) {
            assertTrue(rs.next());
            System.out.println("✅ Connected! 1+1=" + rs.getInt(1));
        }
    }
}
