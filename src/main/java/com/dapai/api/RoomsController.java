package com.dapai.api;

import com.dapai.game.Combination;
import com.dapai.game.GameRoom;
import com.dapai.game.RoomService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomsController {
    private final RoomService rooms;

    public RoomsController(RoomService rooms) {
        this.rooms = rooms;
    }

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping
    public List<GameRoom.State> list() {
        return rooms.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoom.State create(@RequestBody CreateRequest request) {
        return rooms.create(request.roomName(), request.playerName());
    }

    @PostMapping("/{roomName}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoom.State join(@PathVariable String roomName, @RequestBody JoinRequest request) {
        return rooms.room(roomName).join(request.playerName());
    }

    @GetMapping("/{roomName}")
    public GameRoom.State state(@PathVariable String roomName,
                                @RequestParam(required = false) String playerName) {
        GameRoom room = rooms.room(roomName);
        return playerName == null ? room.state() : room.state(playerName);
    }

    @PostMapping({"/{roomName}/heartbeat", "/{roomName}/reconnect"})
    public GameRoom.State heartbeat(@PathVariable String roomName, @RequestParam String playerName) {
        return rooms.room(roomName).state(playerName);
    }

    @PostMapping("/{roomName}/disconnect")
    public GameRoom.State disconnect(@PathVariable String roomName, @RequestParam String playerName) {
        return rooms.room(roomName).disconnect(playerName);
    }

    @PostMapping("/{roomName}/start")
    public GameRoom.State start(@PathVariable String roomName) {
        return rooms.room(roomName).start();
    }

    @PostMapping("/{roomName}/plays")
    public GameRoom.State play(@PathVariable String roomName, @RequestParam String playerName,
                               @RequestBody PlayRequest request) {
        return rooms.room(roomName).play(playerName, request.cardIds(), request.combination());
    }

    @PostMapping("/{roomName}/pass")
    public GameRoom.State pass(@PathVariable String roomName, @RequestParam String playerName) {
        return rooms.room(roomName).pass(playerName);
    }

    public record CreateRequest(String roomName, String playerName) {
    }

    public record JoinRequest(String playerName) {
    }

    public record PlayRequest(List<String> cardIds, Combination combination) {
    }
}