package com.dapai.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class GameRoom {
    public enum Status {
        WAITING, PLAYING, HAND_FINISHED, MATCH_FINISHED
    }

    public record CardView(String id, int deck, Suit suit, Rank rank) {
        static CardView from(Card card) {
            return new CardView(card.id(), card.deck(), card.suit(), card.rank());
        }
    }

    public record PlayerView(int seat, int team, String playerName, boolean connected,
                             boolean finished, int cardCount) {
    }

    public record PlayView(int seat, List<CardView> cards, Combination combination) {
    }

    public record State(String roomName, Status status, boolean paused,
                        int viewerSeat, Rank level, List<Rank> teamLevels, Integer turnSeat,
                        List<PlayerView> players, List<CardView> hand, PlayView table,
                        List<Integer> passedSeats, List<Integer> finishOrder, Integer matchWinner) {
    }

    private static final class Player {
        private final String playerName;
        private final List<Card> hand = new ArrayList<>();
        private Instant lastSeen;
        private boolean connected = true;

        private Player(String playerName, Instant now) {
            this.playerName = playerName;
            lastSeen = now;
        }
    }

    private final String roomName;
    private final Clock clock;
    private final Duration heartbeatTimeout;
    private final Supplier<List<List<Card>>> dealer;
    private final GuandanRules rules = new GuandanRules();
    private final List<Player> players = new ArrayList<>(4);
    private final Rank[] teamLevels = {Rank.TWO, Rank.TWO};
    private final List<Integer> finishOrder = new ArrayList<>(4);
    private final List<Integer> passedSeats = new ArrayList<>(3);
    private Status status = Status.WAITING;
    private Rank level = Rank.TWO;
    private Integer turnSeat;
    private PlayView table;
    private Integer matchWinner;

    public GameRoom(String roomName, Clock clock, Duration heartbeatTimeout) {
        this(roomName, clock, heartbeatTimeout, Deck::deal);
    }

    GameRoom(String roomName, Clock clock, Duration heartbeatTimeout, Supplier<List<List<Card>>> dealer) {
        this.roomName = roomName;
        this.clock = clock;
        this.heartbeatTimeout = heartbeatTimeout;
        this.dealer = dealer;
    }

    public synchronized State join(String playerName) {
        expireConnections();
        if (status != Status.WAITING || players.size() == 4) {
            throw conflict("This room is not accepting new players.");
        }
        String normalized = normalizePlayerName(playerName);
        if (players.stream().anyMatch(player -> player.playerName.equalsIgnoreCase(normalized))) {
            throw conflict("A player with that name already exists in this room.");
        }
        var player = new Player(normalized, clock.instant());
        players.add(player);
        return snapshot(players.size() - 1);
    }

    public synchronized State state(String playerName) {
        return snapshot(heartbeat(playerName));
    }

    public synchronized State state() {
        expireConnections();
        return snapshot(0);
    }

    public synchronized State disconnect(String playerName) {
        int seat = findPlayer(playerName);
        expireConnections();
        Player player = players.get(seat);
        if (player.connected) {
            player.connected = false;
        }
        return snapshot(seat);
    }

    public synchronized State start() {
        expireConnections();
        if (players.size() != 4) {
            throw conflict("Starting requires exactly four players.");
        }
        if (status != Status.WAITING && status != Status.HAND_FINISHED) {
            throw conflict("Starting requires no hand or match in progress.");
        }
        if (players.stream().anyMatch(player -> !player.connected)) {
            throw conflict("Wait for every player to reconnect before starting.");
        }
        List<List<Card>> hands = dealer.get();
        int leader = status == Status.WAITING ? ThreadLocalRandom.current().nextInt(4) : finishOrder.get(0);
        for (int playerSeat = 0; playerSeat < 4; playerSeat++) {
            players.get(playerSeat).hand.clear();
            players.get(playerSeat).hand.addAll(hands.get(playerSeat));
        }
        level = teamLevels[leader % 2];
        finishOrder.clear();
        passedSeats.clear();
        table = null;
        turnSeat = leader;
        status = Status.PLAYING;
        return snapshot(0);
    }

    public synchronized State play(String playerName, List<String> cardIds,
                                   Combination selection) {
        int seat = requireTurn(playerName);
        if (cardIds == null || cardIds.isEmpty() || cardIds.size() > 27
                || new HashSet<>(cardIds).size() != cardIds.size()) {
            throw new IllegalArgumentException("Select 1 to 27 distinct card IDs from your hand.");
        }
        Player player = players.get(seat);
        List<Card> cards = cardIds.stream().map(id -> player.hand.stream()
                .filter(card -> card.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("A selected card is not in your hand."))).toList();
        List<Combination> options = rules.evaluate(cards, level).stream()
                .filter(option -> table == null || option.beats(table.combination())).toList();
        if (options.isEmpty()) {
            throw new IllegalArgumentException("This selection is not a legal play against the table.");
        }
        Combination chosen = selection;
        if (chosen == null) {
            if (options.size() != 1) {
                throw new IllegalArgumentException("This play is ambiguous. Supply a combination from /api/rules/evaluate.");
            }
            chosen = options.get(0);
        } else if (!options.contains(chosen)) {
            throw new IllegalArgumentException("The chosen combination is not a legal interpretation of these cards.");
        }
        player.hand.removeAll(cards);
        table = new PlayView(seat, cards.stream().map(CardView::from).toList(), chosen);
        passedSeats.clear();
        if (player.hand.isEmpty()) {
            finishOrder.add(seat);
        }
        if (finishOrder.size() == 3) {
            finishOrder.add(nextUnfinished(seat));
            finishHand();
        } else {
            turnSeat = nextUnfinished(seat);
        }
        return snapshot(seat);
    }

    public synchronized State pass(String playerName) {
        int seat = requireTurn(playerName);
        if (table == null) {
            throw conflict("The player leading a trick must play cards.");
        }
        passedSeats.add(seat);
        int requiredPasses = 4 - finishOrder.size() - (players.get(table.seat()).hand.isEmpty() ? 0 : 1);
        if (passedSeats.size() == requiredPasses) {
            int lastPlayer = table.seat();
            int partner = (lastPlayer + 2) % 4;
            turnSeat = !players.get(lastPlayer).hand.isEmpty() ? lastPlayer
                    : !players.get(partner).hand.isEmpty() ? partner : nextUnfinished(lastPlayer);
            table = null;
            passedSeats.clear();
        } else {
            turnSeat = nextUnfinished(seat);
        }
        return snapshot(seat);
    }

    private int requireTurn(String playerName) {
        int seat = heartbeat(playerName);
        if (status != Status.PLAYING) {
            throw conflict("There is no hand in progress.");
        }
        if (players.stream().anyMatch(player -> !player.connected)) {
            throw conflict("Play is paused until every player reconnects.");
        }
        if (turnSeat == null || turnSeat != seat) {
            throw conflict("It is not your turn.");
        }
        return seat;
    }

    private int nextUnfinished(int fromSeat) {
        for (int step = 1; step <= 4; step++) {
            int seat = Math.floorMod(fromSeat - step, 4);
            if (!players.get(seat).hand.isEmpty()) {
                return seat;
            }
        }
        throw new IllegalStateException("No unfinished player remains.");
    }

    private void finishHand() {
        int winner = finishOrder.get(0);
        int team = winner % 2;
        int partnerPosition = finishOrder.indexOf((winner + 2) % 4);
        if (level == Rank.ACE && teamLevels[team] == Rank.ACE && partnerPosition < 3) {
            matchWinner = team;
            status = Status.MATCH_FINISHED;
        } else {
            int advancement = 4 - partnerPosition;
            teamLevels[team] = Rank.values()[Math.min(Rank.ACE.ordinal(), teamLevels[team].ordinal() + advancement)];
            status = Status.HAND_FINISHED;
        }
        turnSeat = null;
    }

    private String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank() || playerName.strip().length() > 24) {
            throw new IllegalArgumentException("A player name must contain 1 to 24 characters.");
        }
        return playerName.strip();
    }

    private int findPlayer(String playerName) {
        String normalized = normalizePlayerName(playerName);
        for (int seat = 0; seat < players.size(); seat++) {
            if (players.get(seat).playerName.equalsIgnoreCase(normalized)) {
                return seat;
            }
        }
        throw new RoomException(RoomException.Reason.NOT_FOUND, "Player not found in this room.");
    }

    private int heartbeat(String playerName) {
        int seat = findPlayer(playerName);
        expireConnections();
        Player player = players.get(seat);
        if (!player.connected) {
            player.connected = true;
        }
        player.lastSeen = clock.instant();
        return seat;
    }

    private void expireConnections() {
        Instant now = clock.instant();
        for (Player player : players) {
            if (player.connected && !now.isBefore(player.lastSeen.plus(heartbeatTimeout))) {
                player.connected = false;
            }
        }
    }

    private RoomException conflict(String message) {
        return new RoomException(RoomException.Reason.CONFLICT, message);
    }

    private State snapshot(int viewerSeat) {
        var visiblePlayers = new ArrayList<PlayerView>(players.size());
        for (int seat = 0; seat < players.size(); seat++) {
            Player player = players.get(seat);
            visiblePlayers.add(new PlayerView(seat, seat % 2, player.playerName,
                    player.connected, finishOrder.contains(seat), player.hand.size()));
        }
        boolean paused = status == Status.PLAYING && players.stream().anyMatch(player -> !player.connected);
        return new State(roomName, status, paused, viewerSeat, level,
                List.of(teamLevels[0], teamLevels[1]), turnSeat, List.copyOf(visiblePlayers),
                players.get(viewerSeat).hand.stream().map(CardView::from).toList(),
                table, List.copyOf(passedSeats), List.copyOf(finishOrder), matchWinner);
    }
}