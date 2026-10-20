package com.dapai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.dapai.game.GameRoom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomsApiTest {
    @Autowired
    private TestRestTemplate http;

        @Test
        void listsCreatedRoomsWithAllPlayers() {
        var first = http.postForEntity("/api/rooms", Map.of("roomName", "List Room A", "playerName", "Alice"), GameRoom.State.class).getBody();
        var second = http.postForEntity("/api/rooms", Map.of("roomName", "List Room B", "playerName", "Alice"), GameRoom.State.class).getBody();
        http.postForEntity("/api/rooms/{roomName}/join", Map.of("playerName", "Charlie"),
            GameRoom.State.class, first.roomName());
        var response = http.getForEntity("/api/rooms", GameRoom.State[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(Arrays.stream(response.getBody()).map(GameRoom.State::roomName).toList())
            .isSorted().contains(first.roomName(), second.roomName());
        var listedRoom = Arrays.stream(response.getBody())
            .filter(room -> room.roomName().equals(first.roomName())).findFirst().orElseThrow();
        assertThat(listedRoom.roomName()).isEqualTo("List Room A");
        assertThat(listedRoom.status()).isEqualTo(GameRoom.Status.WAITING);
        assertThat(listedRoom.players()).extracting(GameRoom.PlayerView::playerName)
            .containsExactly("Alice", "Charlie");
        assertThat(listedRoom.viewerSeat()).isZero();
        assertThat(listedRoom.paused()).isFalse();
        var byName = http.getForEntity("/api/rooms/{roomName}", GameRoom.State.class, first.roomName());
        assertThat(byName.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byName.getBody()).isEqualTo(listedRoom);
        var disconnected = http.postForEntity("/api/rooms/{roomName}/disconnect?playerName={playerName}",
            null, GameRoom.State.class, first.roomName(), "Alice").getBody();
        assertThat(disconnected.players().get(0).connected()).isFalse();
        var otherRoom = http.getForEntity("/api/rooms/{roomName}", GameRoom.State.class, second.roomName()).getBody();
        assertThat(otherRoom.players()).hasSize(1);
        assertThat(otherRoom.players().get(0).playerName()).isEqualTo("Alice");
        assertThat(otherRoom.players().get(0).connected()).isTrue();
        }

    @Test
    void friendsCreateJoinStartPlayPassAndReconnectThroughHttp() {
        var created = http.postForEntity("/api/rooms", Map.of("roomName", "AliceRoom", "playerName", "Alice"), GameRoom.State.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getCacheControl()).isEqualTo("no-store");
        var host = created.getBody();
        assertThat(host.roomName()).isEqualTo("AliceRoom");
        String base = "/api/rooms/" + host.roomName();
        var players = new ArrayList<GameRoom.State>();
        players.add(host);
        for (String playerName : List.of("Bob", "Charlie", "Diana")) {
            var joined = http.postForEntity(base + "/join", Map.of("playerName", playerName), GameRoom.State.class);
            assertThat(joined.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            players.add(joined.getBody());
        }
        var started = http.postForEntity(base + "/start", null, GameRoom.State.class);
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.OK);
        var state = started.getBody();
        assertThat(state.status()).isEqualTo(GameRoom.Status.PLAYING);
        var repeatedStart = http.postForEntity(base + "/start", null, JsonNode.class);
        assertThat(repeatedStart.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(http.getForEntity(base, GameRoom.State.class).getBody()).isEqualTo(state);
        int leader = state.turnSeat();
        var leaderPlayer = players.get(leader);
        var leaderState = http.getForEntity(base + "?playerName={playerName}",
            GameRoom.State.class, playerName(leaderPlayer)).getBody();
        assertThat(leaderState.hand()).hasSize(27);
        assertThat(leaderState.viewerSeat()).isEqualTo(leader);
        state = post(base + "/plays", leaderPlayer,
                Map.of("cardIds", List.of(leaderState.hand().get(0).id())));
        assertThat(state.hand()).hasSize(26);
        assertThat(state.turnSeat()).isEqualTo(Math.floorMod(leader - 1, 4));
        for (int pass = 0; pass < 3; pass++) {
            state = post(base + "/pass", players.get(state.turnSeat()), null);
        }
        assertThat(state.turnSeat()).isEqualTo(leader);
        assertThat(state.table()).isNull();
        state = post(base + "/disconnect", host, null);
        assertThat(state.paused()).isTrue();
        var listedRoom = Arrays.stream(http.getForEntity("/api/rooms", GameRoom.State[].class).getBody())
            .filter(room -> room.roomName().equals(host.roomName())).findFirst().orElseThrow();
        assertThat(listedRoom).isEqualTo(state);
        assertThat(http.getForEntity(base, GameRoom.State.class).getBody()).isEqualTo(state);
        state = post(base + "/reconnect", host, null);
        assertThat(state.paused()).isFalse();
    }

        @Test
        void duplicateRoomNameIsRejectedWithoutReplacingTheRoom() {
        var original = http.postForEntity("/api/rooms",
            Map.of("roomName", "DuplicateRoom", "playerName", "Alice"), GameRoom.State.class).getBody();
        var duplicate = http.postForEntity("/api/rooms",
            Map.of("roomName", " duplicateroom ", "playerName", "Bob"), JsonNode.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var state = http.getForEntity("/api/rooms/DuplicateRoom?playerName={playerName}",
                GameRoom.State.class, playerName(original)).getBody();
        assertThat(state.roomName()).isEqualTo("DuplicateRoom");
        assertThat(state.players()).hasSize(1);
        assertThat(state.players().get(0).playerName()).isEqualTo("Alice");
        }

        @Test
        void duplicatePlayerNameIsRejectedWithinTheRoom() {
        var created = http.postForEntity("/api/rooms",
            Map.of("roomName", "PlayerNames", "playerName", "Alice"), GameRoom.State.class).getBody();
        var duplicate = http.postForEntity("/api/rooms/PlayerNames/join",
            Map.of("playerName", " alice "), JsonNode.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var state = http.getForEntity("/api/rooms/PlayerNames?playerName=alice", GameRoom.State.class).getBody();
        assertThat(state.viewerSeat()).isEqualTo(created.viewerSeat());
        assertThat(state.players()).hasSize(1);
        assertThat(state.players().get(0).playerName()).isEqualTo("Alice");
        }

    @Test
    void startingWithFewerThanFourPlayersFailsWithoutChangingTheRoom() {
        http.postForEntity("/api/rooms", Map.of("roomName", "UnderfilledRoom", "playerName", "Alice"), GameRoom.State.class);
        String base = "/api/rooms/UnderfilledRoom";
        for (String playerName : List.of("Bob", "Charlie")) {
            http.postForEntity(base + "/join", Map.of("playerName", playerName), GameRoom.State.class);
        }
        var waiting = http.getForEntity(base, GameRoom.State.class).getBody();
        assertThat(waiting.players()).hasSize(3);
        var rejected = http.postForEntity(base + "/start", null, JsonNode.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rejected.getBody().path("detail").asText()).contains("four players");
        assertThat(http.getForEntity(base, GameRoom.State.class).getBody()).isEqualTo(waiting);
    }

        private GameRoom.State post(String url, GameRoom.State player, Object body) {
        var response = http.postForEntity(url + "?playerName={playerName}", body,
            GameRoom.State.class, playerName(player));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private String playerName(GameRoom.State state) {
        return state.players().get(state.viewerSeat()).playerName();
    }
}