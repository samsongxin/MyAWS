package com.dapai.game;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Duration heartbeatTimeout;
    private final int maxRooms;

    public RoomService(@Value("${dapai.rooms.heartbeat-timeout-seconds:60}") int heartbeatTimeoutSeconds,
                       @Value("${dapai.rooms.max-rooms:100}") int maxRooms) {
        if (heartbeatTimeoutSeconds < 1 || maxRooms < 1) {
            throw new IllegalArgumentException("Room limits and heartbeat timeout must be positive.");
        }
        heartbeatTimeout = Duration.ofSeconds(heartbeatTimeoutSeconds);
        this.maxRooms = maxRooms;
    }

    public synchronized GameRoom.State create(String roomName, String playerName) {
        String name = normalizeName(roomName);
        String key = name.toLowerCase(Locale.ROOT);
        if (rooms.containsKey(key)) {
            throw new RoomException(RoomException.Reason.CONFLICT, "A room with that name already exists.");
        }
        if (rooms.size() >= maxRooms) {
            throw new RoomException(RoomException.Reason.CONFLICT, "The server's room limit has been reached.");
        }
        var room = new GameRoom(name, Clock.systemUTC(), heartbeatTimeout);
        var state = room.join(playerName);
        rooms.put(key, room);
        return state;
    }

    public List<GameRoom.State> list() {
        return rooms.values().stream().map(GameRoom::state)
                .sorted(Comparator.comparing(GameRoom.State::roomName)).toList();
    }

    public GameRoom room(String roomName) {
        GameRoom room = rooms.get(normalizeName(roomName).toLowerCase(Locale.ROOT));
        if (room == null) {
            throw new RoomException(RoomException.Reason.NOT_FOUND, "Room not found.");
        }
        return room;
    }

    private String normalizeName(String roomName) {
        if (roomName == null) {
            throw new IllegalArgumentException("A room name is required.");
        }
        String name = roomName.strip();
        if (!name.matches("[\\p{L}\\p{N} _-]{1,40}")) {
            throw new IllegalArgumentException("A room name must contain 1 to 40 letters, numbers, spaces, hyphens, or underscores.");
        }
        return name;
    }
}