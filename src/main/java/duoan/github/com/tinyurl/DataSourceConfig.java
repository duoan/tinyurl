package duoan.github.com.tinyurl;

import lombok.Getter;
import lombok.Setter;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    @Bean
    @Profile("local")
    @Primary
    DataSource singleDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Configuration
    @Profile("prod")
    @EnableConfigurationProperties(ReplicationDataSourceProperties.class)
    class ReplicationDataSourceConfig {

        @Bean
        @Primary
        @ConfigurationProperties("spring.datasource.primary")
        DataSourceProperties primaryDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        @ConfigurationProperties("spring.datasource.replica")
        DataSourceProperties replicaDataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        @Primary
        DataSource primaryDataSource() {
            return primaryDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }

        @Bean
        DataSource replicaDataSource() {
            return replicaDataSourceProperties()
                    .initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }

        @Bean
        DataSource replicationRoutingDataSource(
                @Qualifier("primaryDataSource") DataSource primaryDataSource,
                @Qualifier("replicaDataSource") DataSource replicaDataSource) {
            ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();

            Map<Object, Object> dataSources = new HashMap<>();
            dataSources.put("primary", primaryDataSource);
            dataSources.put("replica", replicaDataSource);

            routingDataSource.setTargetDataSources(dataSources);
            routingDataSource.setDefaultTargetDataSource(primaryDataSource);

            return routingDataSource;
        }

        @Bean
        public DataSource dataSource(@Qualifier("replicationRoutingDataSource") DataSource postgresClusterRoutingDataSource) {
            return new LazyConnectionDataSourceProxy(postgresClusterRoutingDataSource);
        }
    }
}


@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "spring.datasource")
class ReplicationDataSourceProperties {
    // Getters and Setters
    private DatabaseConfig primary = new DatabaseConfig();
    private DatabaseConfig replica = new DatabaseConfig();

    @Setter
    @Getter
    public static class DatabaseConfig {
        // Getters and Setters
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}


class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? "replica"
                : "primary";
    }
}


