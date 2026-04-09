package com.dagdockersim.simulation.infrastructure.cache;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues()
            .entryTtl(Duration.ofSeconds(60));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<String, RedisCacheConfiguration>();
        cacheConfigs.put(SimulationCacheNames.SIMULATION_HEALTH, baseConfig.entryTtl(Duration.ofSeconds(15)));
        cacheConfigs.put(SimulationCacheNames.SIMULATION_TOPOLOGY, baseConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigs.put(SimulationCacheNames.CLOUD_LEDGER, baseConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigs.put(SimulationCacheNames.FUSION_LIST, baseConfig.entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put(SimulationCacheNames.FUSION_LEDGER, baseConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigs.put(SimulationCacheNames.DEVICE_LIST, baseConfig.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(baseConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
