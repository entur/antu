package no.entur.antu.cache;

import java.util.stream.Collectors;
import org.redisson.api.RType;
import org.redisson.api.RedissonClient;

/**
 * Redisson-based implementation of the CacheAdmin interface.
 */
public class RedissonCacheAdmin implements CacheAdmin {

  private final RedissonClient redissonClient;

  public RedissonCacheAdmin(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public void clear() {
    redissonClient.getKeys().flushdbParallelAsync();
  }

  @Override
  public long deleteKeysByPattern(String pattern) {
    return redissonClient.getKeys().deleteByPattern(pattern);
  }

  @Override
  public String dumpKeys() {
    return redissonClient
      .getKeys()
      .getKeysStream()
      .map(key ->
        key +
        " (" +
        formatKeyDetails(key) +
        ", TTL: " +
        formatTtl(redissonClient.getKeys().remainTimeToLive(key)) +
        ")"
      )
      .sorted()
      .collect(Collectors.joining("\n"));
  }

  private String formatKeyDetails(String key) {
    RType type = redissonClient.getKeys().getType(key);
    StringBuilder details = new StringBuilder();
    details.append(type.name());
    if (type == RType.MAP) {
      details.append("[");
      details.append(redissonClient.getMap(key).size());
      details.append(" entries, ");
      details.append(redissonClient.getMap(key).sizeInMemory() / 1000);
      details.append(" kb]");
    }
    if (type == RType.SET) {
      details.append("[");
      details.append(redissonClient.getSet(key).size());
      details.append(" entries, ");
      details.append(redissonClient.getSet(key).sizeInMemory() / 1000);
      details.append(" kb]");
    }
    return details.toString();
  }

  private String formatTtl(long ttl) {
    if (ttl == -2) {
      return "DELETED";
    }
    if (ttl == -1) {
      return "NONE";
    }
    return ttl / 1000 + "s";
  }

  @Override
  public String inspectKey(String key) {
    if (redissonClient.getKeys().countExists(key) == 0) {
      return "Key '" + key + "' does not exist";
    }

    RType type = redissonClient.getKeys().getType(key);
    StringBuilder result = new StringBuilder();
    result.append("Key: ").append(key).append("\n");
    result.append("Type: ").append(type.name()).append("\n");
    result
      .append("TTL: ")
      .append(formatTtl(redissonClient.getKeys().remainTimeToLive(key)))
      .append("\n");

    try {
      if (type == RType.MAP) {
        result.append("Content:\n").append(inspectMapContent(key));
      } else if (type == RType.SET) {
        result.append("Content:\n").append(inspectSetContent(key));
      } else if (type == RType.LIST) {
        result.append("Content:\n").append(inspectListContent(key));
      } else if (type == RType.OBJECT) {
        result.append("Content: ").append(redissonClient.getBucket(key).get());
      } else {
        result
          .append("Content inspection not supported for type: ")
          .append(type);
      }
    } catch (Exception e) {
      result.append("Error reading content: ").append(e.getMessage());
    }

    return result.toString();
  }

  @Override
  public String inspectKeysByPattern(String pattern) {
    return redissonClient
      .getKeys()
      .getKeysStream()
      .filter(key -> key.matches(pattern.replace("*", ".*")))
      .limit(50) // Limit to prevent overwhelming output
      .map(key -> {
        try {
          return "=== " + key + " ===\n" + inspectKey(key) + "\n";
        } catch (Exception e) {
          return (
            "=== " +
            key +
            " ===\nError inspecting key: " +
            e.getMessage() +
            "\n"
          );
        }
      })
      .collect(Collectors.joining("\n"));
  }

  private String inspectMapContent(String key) {
    try {
      var map = redissonClient.getMap(key);
      if (map.size() > 20) {
        return (
          "Map too large (" +
          map.size() +
          " entries). First 20 entries:\n" +
          map
            .entrySet()
            .stream()
            .limit(20)
            .map(entry ->
              "  " + entry.getKey() + " -> " + formatValue(entry.getValue())
            )
            .collect(Collectors.joining("\n"))
        );
      } else {
        return map
          .entrySet()
          .stream()
          .map(entry ->
            "  " + entry.getKey() + " -> " + formatValue(entry.getValue())
          )
          .collect(Collectors.joining("\n"));
      }
    } catch (Exception e) {
      return "Error reading map: " + e.getMessage();
    }
  }

  private String inspectSetContent(String key) {
    try {
      var set = redissonClient.getSet(key);
      if (set.size() > 20) {
        return (
          "Set too large (" +
          set.size() +
          " entries). First 20 entries:\n" +
          set
            .stream()
            .limit(20)
            .map(item -> "  " + formatValue(item))
            .collect(Collectors.joining("\n"))
        );
      } else {
        return set
          .stream()
          .map(item -> "  " + formatValue(item))
          .collect(Collectors.joining("\n"));
      }
    } catch (Exception e) {
      return "Error reading set: " + e.getMessage();
    }
  }

  private String inspectListContent(String key) {
    try {
      var list = redissonClient.getList(key);
      if (list.size() > 20) {
        return (
          "List too large (" +
          list.size() +
          " entries). First 20 entries:\n" +
          list
            .stream()
            .limit(20)
            .map(item -> "  " + formatValue(item))
            .collect(Collectors.joining("\n"))
        );
      } else {
        return list
          .stream()
          .map(item -> "  " + formatValue(item))
          .collect(Collectors.joining("\n"));
      }
    } catch (Exception e) {
      return "Error reading list: " + e.getMessage();
    }
  }

  private String formatValue(Object value) {
    if (value == null) {
      return "null";
    }
    String str = value.toString();
    return str.length() > 200 ? str.substring(0, 200) + "..." : str;
  }
}
