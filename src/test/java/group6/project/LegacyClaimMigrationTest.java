package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LegacyClaimMigrationTest {
    // Upgrade a V10 database containing an approved batch-only claim; migration must preserve a payable fee.
    @Test void batchLinkedClaimKeepsItsFeeDuringUpgrade() throws Exception {
        String url="jdbc:h2:mem:legacy_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER,YEAR";
        var dataSource=new org.springframework.jdbc.datasource.SingleConnectionDataSource(url,"sa","",true);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("10").load().migrate();
        try(Connection connection=dataSource.getConnection();Statement sql=connection.createStatement()) {
            sql.execute("insert into users(user_id,user_name,name,password,role,staff_id) values(10,'legacy','Legacy','test','STAFF','LEGACY')");
            sql.execute("insert into course_detail(course_id,title,course_fee) values(10,'Legacy batch course',345.67)");
            sql.execute("insert into course_batch(batch_id,course_id) values(10,10)");
            sql.execute("insert into course_fee_application(application_id,staff_id,batch_id,application_status,amount) values(10,10,10,'APPROVED',0)");
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
            try(ResultSet row=sql.executeQuery("select amount from course_fee_application where application_id=10")) {
                assertTrue(row.next());assertEquals(new java.math.BigDecimal("345.67"),row.getBigDecimal(1));
            }
        } finally {dataSource.destroy();}
    }
}
