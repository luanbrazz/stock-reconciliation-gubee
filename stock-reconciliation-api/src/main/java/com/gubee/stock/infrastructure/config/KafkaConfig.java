package com.gubee.stock.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.topic.stock-events.name}")
    private String topicName;

    @Value("${spring.kafka.topic.stock-events.partitions}")
    private int partitions;

    @Value("${spring.kafka.topic.stock-events.replicas}")
    private int replicas;

    @Bean
    public NewTopic stockEventsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public CommonErrorHandler errorHandler() {
        return new DefaultErrorHandler((record, exception) ->
                log.error("Failed to process record topic={} partition={} offset={}: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage())
        );
    }
}