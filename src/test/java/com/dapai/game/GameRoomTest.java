package com.dapai.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameRoomTest {
    @Test
    void fourFriendsJoinAndReceiveTheirOwnHands() {
        var room = newRoom();
        var playerNames = joinFour(room);
        var state = room.start();
        assertThat(state.status()).isEqualTo(GameRoom.Status.PLAYING);
        assertThat(state.players()).hasSize(4);
        assertThat(state.players().stream().map(GameRoom.PlayerView::team)).containsExactly(0, 1, 0, 1);
        assertThat(state.turnSeat()).isBetween(0, 3);
        var dealtCards = new ArrayList<String>();
        for (String playerName : playerNames) {
            var ownState = room.state(playerName);
            assertThat(ownState.hand()).hasSize(27);
            ownState.hand().forEach(card -> dealtCards.add(card.id()));
        }
        assertThat(dealtCards).hasSize(108).doesNotHaveDuplicates();
    }

    @Test
    void playMovesCounterclockwiseAndThreePassesReturnTheLead() {
        var room = newRoom();
        var playerNames = joinFour(room);
        var state = room.start();
        int leader = state.turnSeat();
        var leaderState = room.state(playerNames.get(leader));
        state = room.play(playerNames.get(leader),
                List.of(leaderState.hand().get(0).id()), null);
        assertThat(state.turnSeat()).isEqualTo(Math.floorMod(leader - 1, 4));
        assertThat(state.table().seat()).isEqualTo(leader);
        assertThat(room.state(playerNames.get(leader)).hand()).hasSize(26);
        for (int pass = 0; pass < 3; pass++) {
            state = room.pass(playerNames.get(state.turnSeat()));
        }
        assertThat(state.turnSeat()).isEqualTo(leader);
        assertThat(state.table()).isNull();
    }

    @Test
    void finishingPlayerPassesTheLeadToTheirPartnerAndTheRoomStartsAnotherHand() {
        var hands = List.of(
                List.of(new Card(0, Suit.SPADES, Rank.THREE)),
                List.of(new Card(0, Suit.SPADES, Rank.FOUR)),
                List.of(new Card(0, Suit.SPADES, Rank.FIVE)),
                List.of(new Card(0, Suit.SPADES, Rank.SIX)));
        var room = new GameRoom("ROOM01", Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC),
                Duration.ofSeconds(60), () -> hands);
        var playerNames = joinFour(room);
        var state = room.start();
        int leader = state.turnSeat();
        var leaderState = room.state(playerNames.get(leader));
        state = room.play(playerNames.get(leader),
                List.of(leaderState.hand().get(0).id()), null);
        for (int pass = 0; pass < 3; pass++) {
            state = room.pass(playerNames.get(state.turnSeat()));
        }
        assertThat(state.turnSeat()).isEqualTo((leader + 2) % 4);
        assertThat(state.finishOrder()).containsExactly(leader);
        while (state.status() == GameRoom.Status.PLAYING) {
            int seat = state.turnSeat();
            var ownState = room.state(playerNames.get(seat));
            if (state.table() == null) {
                state = room.play(playerNames.get(seat),
                        List.of(ownState.hand().get(0).id()), null);
            } else {
                state = room.pass(playerNames.get(seat));
            }
        }
        assertThat(state.status()).isEqualTo(GameRoom.Status.HAND_FINISHED);
        assertThat(state.finishOrder()).hasSize(4);
        assertThat(state.teamLevels().get(leader % 2)).isEqualTo(Rank.FIVE);
        state = room.start();
        assertThat(state.turnSeat()).isEqualTo(leader);
        assertThat(state.level()).isEqualTo(Rank.FIVE);
    }

    @Test
    void timedOutFriendsCanRejoinAndResumeTheHand() {
        Instant now = Instant.parse("2026-10-06T12:00:00Z");
        var clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        var room = new GameRoom("ROOM01", clock, Duration.ofSeconds(60));
        var playerNames = joinFour(room);
        room.start();
        when(clock.instant()).thenReturn(now.plusSeconds(61));
        var paused = room.state(playerNames.get(0));
        assertThat(paused.paused()).isTrue();
        room.state(playerNames.get(1));
        room.state(playerNames.get(2));
        var resumed = room.state(playerNames.get(3));
        assertThat(resumed.paused()).isFalse();
        assertThat(resumed.hand()).hasSize(27);
    }

    private GameRoom newRoom() {
        return new GameRoom("ROOM01", Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC),
                Duration.ofSeconds(60));
    }

    private List<String> joinFour(GameRoom room) {
        var playerNames = List.of("Alice", "Bob", "Charlie", "Diana");
        playerNames.forEach(room::join);
        return playerNames;
    }
}