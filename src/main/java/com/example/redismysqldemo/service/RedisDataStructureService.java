package com.example.redismysqldemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisDataStructureService {

    private static final Logger log = LoggerFactory.getLogger(RedisDataStructureService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDataStructureService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ══════════════════════════════════════════════════════════════
    // 1. STRING — Cache session tokens with TTL
    // Use case: Store user session after login, auto-expire after 1 hour
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> stringDemo() {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // Store a session token with 1 hour TTL
        ops.set("session:user:101", "token-abc-xyz-789", Duration.ofHours(1));
        log.info("[STRING] Session stored for user:101");

        // Store a simple counter (page views)
        ops.set("pageviews:home", "0");
        ops.increment("pageviews:home", 5);   // 5 views
        ops.increment("pageviews:home", 3);   // 3 more

        String session   = ops.get("session:user:101");
        String pageViews = ops.get("pageviews:home");
        Long   ttl       = redisTemplate.getExpire("session:user:101");

        log.info("[STRING] Session: {}, Page views: {}, TTL: {}s", session, pageViews, ttl);

        return Map.of(
                "structure",  "String",
                "useCase",    "Session token storage with auto-expiry",
                "session",    session,
                "pageViews",  pageViews,
                "ttlSeconds", ttl
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 2. HASH — Store user profile as field-value pairs
    // Use case: User profile where individual fields can be updated
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> hashDemo() {
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        String key = "user:profile:202";

        // Store user profile
        ops.put(key, "name",    "John Doe");
        ops.put(key, "email",   "john@example.com");
        ops.put(key, "role",    "ADMIN");
        ops.put(key, "country", "India");
        log.info("[HASH] User profile stored for user:202");

        // Update only one field (no need to reload entire object)
        ops.put(key, "role", "SUPER_ADMIN");
        log.info("[HASH] Role updated to SUPER_ADMIN");

        // Read entire profile
        Map<String, String> profile = ops.entries(key);
        long fieldCount = ops.size(key);

        log.info("[HASH] Profile: {}", profile);

        return Map.of(
                "structure",  "Hash",
                "useCase",    "User profile with partial field updates",
                "profile",    profile,
                "fieldCount", fieldCount
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 3. LIST — Notification queue (FIFO)
    // Use case: Maintain last 5 recent activities for a user
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> listDemo() {
        ListOperations<String, String> ops = redisTemplate.opsForList();
        String key = "activity:user:303";

        // Clear previous data
        redisTemplate.delete(key);

        // Push new activities (latest on left)
        ops.leftPush(key, "Logged in");
        ops.leftPush(key, "Viewed Product #55");
        ops.leftPush(key, "Added to Cart");
        ops.leftPush(key, "Applied Coupon");
        ops.leftPush(key, "Placed Order #789");
        log.info("[LIST] Activity log updated for user:303");

        // Keep only last 5 activities
        ops.trim(key, 0, 4);

        List<String> activities = ops.range(key, 0, -1);
        Long size = ops.size(key);

        log.info("[LIST] Recent activities: {}", activities);

        return Map.of(
                "structure",  "List",
                "useCase",    "Recent activity feed (latest 5 actions)",
                "activities", activities,
                "count",      size
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 4. SET — Track unique visitors / tag management
    // Use case: Count unique users who visited a page today
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> setDemo() {
        SetOperations<String, String> ops = redisTemplate.opsForSet();
        String todayKey     = "visitors:2024-06-17";
        String yesterdayKey = "visitors:2024-06-16";

        // Clear previous data
        redisTemplate.delete(todayKey);
        redisTemplate.delete(yesterdayKey);

        // Today's visitors (duplicates automatically ignored)
        ops.add(todayKey,     "user1", "user2", "user3", "user4", "user1"); // user1 duplicate
        ops.add(yesterdayKey, "user2", "user3", "user5", "user6");

        long uniqueToday = ops.size(todayKey);

        // Users who visited BOTH days
        Set<String> returningUsers = ops.intersect(todayKey, yesterdayKey);

        // Users who visited today but NOT yesterday (new users)
        Set<String> newUsers = ops.difference(todayKey, yesterdayKey);

        // All users across both days
        Set<String> allUsers = ops.union(todayKey, yesterdayKey);

        log.info("[SET] Unique today: {}, Returning: {}, New: {}", uniqueToday, returningUsers, newUsers);

        return Map.of(
                "structure",      "Set",
                "useCase",        "Unique visitor tracking and set operations",
                "uniqueToday",    uniqueToday,
                "returningUsers", returningUsers,
                "newUsers",       newUsers,
                "allUsers",       allUsers
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 5. SORTED SET — Real-time leaderboard
    // Use case: Game leaderboard ranked by score
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> sortedSetDemo() {
        ZSetOperations<String, String> ops = redisTemplate.opsForZSet();
        String key = "leaderboard:game";

        // Clear previous data
        redisTemplate.delete(key);

        // Add players with scores
        ops.add(key, "PlayerAlpha",   5000);
        ops.add(key, "PlayerBeta",    8500);
        ops.add(key, "PlayerGamma",   3200);
        ops.add(key, "PlayerDelta",   9100);
        ops.add(key, "PlayerEpsilon", 7300);

        // PlayerAlpha earns more points
        ops.incrementScore(key, "PlayerAlpha", 2000);

        // Top 3 players (highest score first)
        Set<ZSetOperations.TypedTuple<String>> top3 =
                ops.reverseRangeWithScores(key, 0, 2);

        // Rank of PlayerAlpha (0-based, higher score = lower rank index)
        Long rank = ops.reverseRank(key, "PlayerAlpha");
        Double score = ops.score(key, "PlayerAlpha");

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        if (top3 != null) {
            int position = 1;
            for (ZSetOperations.TypedTuple<String> entry : top3) {
                leaderboard.add(Map.of(
                        "position", position++,
                        "player",   entry.getValue(),
                        "score",    entry.getScore()
                ));
            }
        }

        log.info("[SORTED SET] Top 3: {}", leaderboard);

        return Map.of(
                "structure",       "Sorted Set",
                "useCase",         "Real-time game leaderboard",
                "top3",            leaderboard,
                "playerAlphaRank", rank != null ? rank + 1 : "N/A",
                "playerAlphaScore",score
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 6. STREAM — Order event log
    // Use case: Append order events and read as audit log
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> streamDemo() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        String key = "stream:orders";

        // Clear previous data
        redisTemplate.delete(key);

        // Append order events to stream
        ops.add(key, Map.of("event", "ORDER_PLACED",    "orderId", "ORD-001", "userId", "101", "amount", "1500"));
        ops.add(key, Map.of("event", "PAYMENT_SUCCESS", "orderId", "ORD-001", "userId", "101", "method", "UPI"));
        ops.add(key, Map.of("event", "ORDER_SHIPPED",   "orderId", "ORD-001", "userId", "101", "courier", "FedEx"));
        ops.add(key, Map.of("event", "ORDER_DELIVERED", "orderId", "ORD-001", "userId", "101", "rating", "5"));
        log.info("[STREAM] Order events appended");

        // Read all events from beginning
        List<MapRecord<String, String, String>> records =
                ops.range(key, org.springframework.data.domain.Range.unbounded());

        List<Map<String, Object>> events = new ArrayList<>();
        if (records != null) {
            for (MapRecord<String, String, String> record : records) {
                events.add(Map.of(
                        "id",   record.getId().getValue(),
                        "data", record.getValue()
                ));
            }
        }

        log.info("[STREAM] Events read: {}", events.size());

        return Map.of(
                "structure",  "Stream",
                "useCase",    "Order lifecycle event log / audit trail",
                "totalEvents", events.size(),
                "events",     events
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 7. BITMAP — Daily user login tracking
    // Use case: Track which users logged in today (memory efficient)
    //           1 million users = only ~125 KB in Redis
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> bitmapDemo() {
        String key = "logins:2024-06-17";

        // Clear previous data
        redisTemplate.delete(key);

        // Mark users as logged in (bit offset = userId)
        redisTemplate.opsForValue().setBit(key, 1,  true);   // user 1
        redisTemplate.opsForValue().setBit(key, 5,  true);   // user 5
        redisTemplate.opsForValue().setBit(key, 42, true);   // user 42
        redisTemplate.opsForValue().setBit(key, 99, true);   // user 99
        log.info("[BITMAP] Login flags set for users 1, 5, 42, 99");

        // Check individual users
        boolean user1LoggedIn  = Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, 1));
        boolean user10LoggedIn = Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, 10));

        // Count total logins today
        Long totalLogins = redisTemplate.execute((RedisConnection conn) ->
                conn.stringCommands().bitCount(key.getBytes())
        );

        log.info("[BITMAP] User1 logged in: {}, User10 logged in: {}, Total: {}", user1LoggedIn, user10LoggedIn, totalLogins);

        return Map.of(
                "structure",       "Bitmap",
                "useCase",         "Memory-efficient daily login tracking",
                "user1LoggedIn",   user1LoggedIn,
                "user10LoggedIn",  user10LoggedIn,
                "totalLoginsToday", totalLogins != null ? totalLogins : 0,
                "memoryNote",      "1 million users tracked in only ~125 KB"
        );
    }

    // ══════════════════════════════════════════════════════════════
    // 8. HYPERLOGLOG — Approximate unique page view counter
    // Use case: Count unique visitors across pages without storing all IDs
    //           ~12 KB memory regardless of how many unique items
    // ══════════════════════════════════════════════════════════════

    public Map<String, Object> hyperLogLogDemo() {
        HyperLogLogOperations<String, String> ops = redisTemplate.opsForHyperLogLog();
        String homePage    = "hll:visitors:home";
        String productPage = "hll:visitors:products";
        String mergedKey   = "hll:visitors:all";

        // Clear previous data
        redisTemplate.delete(homePage);
        redisTemplate.delete(productPage);
        redisTemplate.delete(mergedKey);

        // Home page visitors (user2 visits twice — counted only once)
        ops.add(homePage, "user1", "user2", "user3", "user4", "user2");

        // Product page visitors
        ops.add(productPage, "user2", "user3", "user5", "user6", "user7");

        long homeUnique    = ops.size(homePage);
        long productUnique = ops.size(productPage);

        // Merge both pages — total unique across all pages
        ops.union(mergedKey, homePage, productPage);
        long totalUnique = ops.size(mergedKey);

        log.info("[HLL] Home unique: {}, Product unique: {}, Total unique: {}", homeUnique, productUnique, totalUnique);

        return Map.of(
                "structure",          "HyperLogLog",
                "useCase",            "Approximate unique visitor count across pages",
                "homePageUnique",     homeUnique,
                "productPageUnique",  productUnique,
                "totalUniqueVisitors",totalUnique,
                "memoryNote",         "Fixed ~12 KB memory regardless of dataset size",
                "accuracy",           "~99.19% accurate (0.81% error rate)"
        );
    }
}
