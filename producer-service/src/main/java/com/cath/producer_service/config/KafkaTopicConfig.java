package com.cath.producer_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic heartbeatTopic() {
        return TopicBuilder.name("heartbeat-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topologyInitTopic() {
        return TopicBuilder.name("topology-init-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic nodeFailureTopic() {
        return TopicBuilder.name("node-failure-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
