/**
 * SimpleRedis - A simple and optimized Redis wrapper for Bukkit plugins
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Option 1: Auto-load from redis.yml (recommended)
 * SimpleRedis.init(this);
 *
 * // Option 2: With custom config
 * SimpleRedisConfig config = SimpleRedisConfig.of("localhost", 6379);
 * SimpleRedis.init(this, config);
 *
 * // Get instance
 * SimpleRedis redis = SimpleRedis.get();
 *
 * // Use it
 * redis.set("key", "value");
 * String value = redis.get("key");
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <h3>Using redis.yml (Auto-created)</h3>
 * <p>When you call {@code SimpleRedis.init(this)}, a redis.yml file is automatically created:</p>
 * <pre>
 * redis:
 *   host: localhost
 *   port: 6379
 *   password: ""
 *   database: 0
 *   timeout: 2000
 *   pool-size: 8
 *   key-prefix: ""
 * </pre>
 *
 * <h3>Programmatic Configuration</h3>
 * <pre>{@code
 * // Default config (localhost:6379)
 * SimpleRedisConfig config = SimpleRedisConfig.defaults();
 *
 * // Custom host and port
 * SimpleRedisConfig config = SimpleRedisConfig.of("redis.example.com", 6379);
 *
 * // With password
 * SimpleRedisConfig config = SimpleRedisConfig.of("localhost", 6379, "mypassword");
 *
 * // Full builder
 * SimpleRedisConfig config = SimpleRedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .password("secret")
 *     .database(0)
 *     .timeout(3000)
 *     .poolSize(10)
 *     .keyPrefix("myplugin:")
 *     .build();
 * }</pre>
 *
 * <h2>Basic Operations</h2>
 * <pre>{@code
 * SimpleRedis redis = SimpleRedis.get();
 *
 * // Strings
 * redis.set("player:123", "John");
 * String name = redis.get("player:123");
 * redis.set("temp:token", "abc123", 60); // expires in 60 seconds
 *
 * // Objects (auto JSON serialization)
 * Player player = new Player("John", 100);
 * redis.setObject("player:123", player);
 * Player loaded = redis.getObject("player:123", Player.class);
 *
 * // Numbers
 * long count = redis.incr("visits");
 * redis.incrBy("score", 10);
 * redis.decr("lives");
 *
 * // Hashes
 * redis.hset("user:123", "name", "John");
 * redis.hset("user:123", "level", "10");
 * String name = redis.hget("user:123", "name");
 * Map<String, String> user = redis.hgetAll("user:123");
 *
 * // Sets
 * redis.sadd("online", "player1", "player2");
 * boolean isOnline = redis.sismember("online", "player1");
 * Set<String> players = redis.smembers("online");
 *
 * // Expiration
 * redis.expire("key", 300); // 5 minutes
 * long ttl = redis.ttl("key");
 * boolean exists = redis.exists("key");
 * redis.delete("key");
 * }</pre>
 *
 * <h2>Async Operations</h2>
 * <pre>{@code
 * // Any operation can be async
 * redis.executeAsync(jedis -> jedis.set("key", "value"))
 *     .thenAccept(result -> {
 *         // Handle result on main thread
 *     });
 *
 * redis.executeAsync(jedis -> {
 *     jedis.set("key1", "value1");
 *     jedis.set("key2", "value2");
 * }).thenRun(() -> {
 *     System.out.println("Done!");
 * });
 * }</pre>
 *
 * <h2>Pub/Sub Messaging</h2>
 * <pre>{@code
 * SimpleRedis redis = SimpleRedis.get();
 *
 * // Subscribe to a channel
 * redis.pubSub().subscribe("announcements", message -> {
 *     Bukkit.broadcastMessage(message);
 * });
 *
 * // Publish to a channel
 * redis.publish("announcements", "Server restarting soon!");
 *
 * // Object messaging
 * redis.pubSub().subscribeObject("player-events", PlayerEvent.class, event -> {
 *     handlePlayerEvent(event);
 * });
 *
 * redis.publishObject("player-events", new PlayerEvent("join", "player123"));
 *
 * // Unsubscribe
 * redis.pubSub().unsubscribe("announcements");
 * }</pre>
 *
 * <h2>Advanced Usage</h2>
 * <pre>{@code
 * // Direct Jedis access for complex operations
 * redis.execute(jedis -> {
 *     jedis.watch("key");
 *     Transaction t = jedis.multi();
 *     t.set("key", "value");
 *     return t.exec();
 * });
 *
 * // Pipeline for batch operations
 * redis.execute(jedis -> {
 *     Pipeline pipe = jedis.pipelined();
 *     pipe.set("key1", "value1");
 *     pipe.set("key2", "value2");
 *     pipe.set("key3", "value3");
 *     return pipe.syncAndReturnAll();
 * });
 * }</pre>
 *
 * <h2>Shutdown</h2>
 * <pre>{@code
 * // In your plugin's onDisable()
 * SimpleRedis.get().shutdown();
 * }</pre>
 */
package net.exylia.commons.simpleredis;
