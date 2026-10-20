# DaPai

DaPai is a private Guandan game for friends. The MVP will use a Spring Boot
backend with REST APIs and a lightweight frontend added afterward. The backend
is authoritative for cards, legal plays, turns, finishing order, and scoring.

## Poker Game
掼蛋需要两副牌，共计有十种牌型，是按照牌型的点数大管小加炸弹，争当头游和从2升级打过A获得胜利的一种纸牌游戏。
相同牌型之间，大小比较看牌点。不同牌型之间的大小比较，从大到小按照如下排列：四大天王（四个猫儿）＞超过五张数的炸弹＞同花顺＞五张数炸弹＞四张数炸弹＞7种牌型单张、对子（如44）、三同张（如666）、三带对（如666+77）、三连对（如334455）、三同连张（如333444）和顺子（如45678，只许五张连顺）。
最先出牌结束者为赢家(头游)。如果赢家获得头游和二游，则直升3级（如头把打2，那么第二把直接打5）、如赢家拿下头游和三游则升2级（如头把打2，那么第二把打4）、头游和末游升1级（如头把打2，那么第二把打3）。讨论
掼蛋按照逆时针方向出牌，不出可过牌。全过则最后出牌的人下一圈领出牌。三人出完手中牌结束本副牌。
所谓“借风出牌”，是当上游或二游打完最后一手牌，余下各家均不压牌时，其搭档承接下一圈领出牌权。
贡牌与还牌，由上副牌的下游向上游进贡全手牌中最大的一张牌（红心级牌除外，如打2，则红心2不可进贡），上游选择一张10及以下的牌还给下游。
抗贡，下游或双下方抓到两个大王，则抗贡。
报牌，在出完一手牌后，如剩余牌张数10张及以下，应报出牌张数。

### 十种牌型示例（由大到小）
炸弹按张数分档比较，所以以下列出12个大小档次，对应10种基本牌型：
1. 四大天王：大王＋大王＋小王＋小王
2. 六张及以上炸弹：666666
3. 同花顺：黑桃45678
4. 五张炸弹：88888
5. 四张炸弹：9999
6. 单张：A
7. 对子：44
8. 三同张：666
9. 三带对：666＋77
10. 三连对：334455
11. 三同连张：333444
12. 顺子：45678

普通牌型之间不能互相压制，只能比较相同牌型的点数；以上普通牌型按 README 中的介绍顺序列出。

## Agreed MVP Scope

- Exactly four human players; opposite seats are fixed partners. No bots.
- Named rooms and room-scoped player names; no accounts or authentication for the MVP.
- Two standard decks, including jokers: 108 cards, 27 cards per player.
- Both teams start at level 2 and track their own levels independently.
- No tribute, return cards, or anti-tribute in the MVP. The original rules above
	remain a reference for a later version.
- Pause play when a player disconnects and allow that player to rejoin.
	No turn timers, automatic plays, or forfeits in the MVP.
- Build and test the backend first, with documented REST APIs. Add the frontend
	afterward; the frontend must not decide whether a move is legal.
- AWS deployment is a later milestone. Prefer a western US region, with
	`us-west-2` as the default. Do not provision resources yet.

## Rule Defaults

These defaults make regional variants explicit. Where the original rules do not
specify behavior, use standard Guandan rules with the choices below.

- Card order outside sequences: big joker > small joker > current level rank >
	A > K > Q > J > 10 down to 2, excluding the elevated level rank.
- The two heart-suit level cards are wildcards. They may substitute for non-joker
	cards in combinations, including bombs and straight flushes. Played alone,
	they are level cards; they cannot substitute for jokers.
- In sequences, level cards retain their natural rank. A can be low or high,
	but sequences cannot wrap: `A2345` and `10JQKA` are legal straights, while
	`JQKA2` is not. Straights and straight flushes contain exactly five cards.
- Consecutive pairs contain exactly three consecutive pairs; consecutive
	triples contain exactly two consecutive triples. A may be low or high.
- Ordinary combinations can beat only the same type. Compare full houses by
	their triple rank, and sequences by sequence rank; suits do not break ties.
- Bombs beat ordinary combinations. More cards beat fewer cards; equal-size
	bombs compare rank. Straight flushes beat four- and five-card bombs but lose
	to bombs of six or more cards. The four actual jokers beat everything.
- Pick the first hand's leader randomly. With tribute omitted, the previous
	hand's first finisher leads the next hand.
- Passing does not exclude a player from later turns in the same trick. A player
	with the lead must play; everyone else may pass even when able to beat a play.
- A trick clears when every other unfinished player passes consecutively after
	the latest play. That player leads again if still holding cards; otherwise
	their partner leads if still playing, or the next unfinished player in turn
	order leads. Play proceeds counterclockwise and skips finished players.
- End a hand when three players finish; the remaining player is fourth. The
	first finisher's team wins and advances 3, 2, or 1 levels when its partner
	finishes second, third, or fourth respectively. The other team's level stays
	unchanged. Advancement is capped at A, which must be played to win the match.
- To pass A, a team must be playing at A, finish first, and have its partner
	finish second or third. First plus fourth does not pass A.
- For the MVP, do not add a repeated-failure penalty at A; the team remains at
	A until it passes. This is an explicit simplification of regional variants.

## Backend Expectations

- Create/join rooms, assign seats, start hands, retrieve player-specific state,
	play cards, pass, reconnect, and start the next hand through REST APIs.
- Identify each physical card separately, including duplicates across decks.
- Return the hand for the supplied player name, omitting other players' hands.
- Use each player's unique name as their ID within a room. No login or credentials;
	players are expected to interact through the UI, which supplies their name.
- Poll state for the initial frontend. Use heartbeat expiry to detect lost
	connections; polling and heartbeats must continue while gameplay is paused.
- Serialize moves within each room and validate each action against its current
	status, turn, and cards. No room version is tracked or supplied by clients.
- Verify combinations, comparison, turn handling, partner lead inheritance,
	finishing/scoring, player-specific responses, and pause/rejoin behavior with tests.
- Persistence across server restarts and the AWS budget/service choice remain
	deployment decisions; they have not been agreed yet.

## Running the Backend

The backend implements the card model, all ten combination types, private
four-player rooms, dealing, named players, play/pass turns, hand scoring,
level progression, and reconnect handling. Games can be driven through REST;
the frontend is still to come. No AWS resources have been created.

Requirements: JDK 17 or newer and Maven, with `JAVA_HOME` pointing to the JDK.
On this development machine, the existing JDK is at
`C:\Users\rk8652\.jdks\ms-17.0.15`.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.15"
mvn test
mvn spring-boot:run
```

The default port is 8080; set the `PORT` environment variable to override it.
The test suite intentionally contains only basic happy-path examples for the
MVP, with no full-coverage target or exhaustive invalid-input matrix.

## Initial REST API

- `GET /api/health`: returns the application name and `UP` status.
- `POST /api/rules/evaluate`: returns all legal interpretations of a selection.
	This is a stateless rules endpoint; it evaluates a selection without making a move.

Example request with `Content-Type: application/json`:

```json
{
	"level": "TWO",
	"cards": [
		{ "deck": 0, "suit": "SPADES", "rank": "KING" },
		{ "deck": 0, "suit": "HEARTS", "rank": "TWO" }
	]
}
```

Example response:

```json
{
	"valid": true,
	"combinations": [
		{ "type": "PAIR", "cardCount": 2, "strength": 13 }
	]
}
```

Deck indexes are 0 and 1 and distinguish otherwise identical physical cards.
Rank names are `TWO` through `TEN`, `JACK`, `QUEEN`, `KING`, `ACE`,
`SMALL_JOKER`, and `BIG_JOKER`. Suit names are `CLUBS`, `DIAMONDS`, `HEARTS`,
`SPADES`, and `JOKER`; joker ranks must use the `JOKER` suit.

Strength is the natural rank value (2 through 14), elevated to 15 for level
cards, with small/big jokers at 16/17. For sequences it is the highest rank,
with A counted as 1 when low. Bomb size and type take precedence over strength.
Only compare descriptors evaluated at the same hand level. Wildcard selections
can have multiple interpretations; the play API validates the player's chosen
interpretation rather than silently choosing one.

A well-formed selection with no legal interpretation returns HTTP 200 with
`valid: false` and an empty `combinations` array. Invalid request data returns
HTTP 400 with a problem-details response.

## Room REST API

The room is the primary game entity and owns its players, hands, turns, and
scores. There is no global player identity or player registry. A player's ID is
their name within a specific room: Alice in RoomA and Alice in RoomB are two
independent players, with separate cards, connection state, and game progress.
Every player operation resolves the room first, then the player in that room.

Create a room with `POST /api/rooms` and a JSON body such as
`{"roomName":"AliceRoom","playerName":"Alice"}`. The chosen room name is
also its ID; there is no generated room code. Share the returned `roomName` with friends.
They join with `POST /api/rooms/AliceRoom/join` and `{"playerName":"Bob"}`.
Room names contain 1 to 40 letters, numbers, spaces, hyphens, or underscores.
Outer spaces are trimmed, the chosen casing is kept in responses, and lookup
and uniqueness are case-insensitive. Creating an existing name returns HTTP 409
without changing the original room. Names remain reserved until server restart.
URL-encode room names containing spaces or non-ASCII characters in endpoint paths.
Player names contain 1 to 24 characters, with outer whitespace trimmed. Names
are case-insensitively unique within a room; joining with an existing name returns
HTTP 409. The same player name may be used in different rooms.

Create and join return HTTP 201 with room state directly, without a wrapper.
Player entries use `playerName`, which is also the player's ID. Reconnect using
the same room and player name. The MVP trusts supplied names and does not
authenticate callers; anyone supplying an existing name can act as that player.
Seat 0 is the host; seats 0/2 and 1/3 are partners. Seats are assigned in join
order and arranged clockwise, so play advances 0 -> 3 -> 2 -> 1 -> 0.

`GET /api/rooms` lists all stored rooms, sorted by room name.
Each item uses the full room-state response, including every player's
information. It uses the host's perspective (`viewerSeat: 0`), so `hand`
contains the host's cards. Example for a waiting room:

```json
[
	{
		"roomName": "AliceRoom",
		"status": "WAITING",
		"paused": false,
		"viewerSeat": 0,
		"level": "TWO",
		"teamLevels": ["TWO", "TWO"],
		"turnSeat": null,
		"players": [
			{"seat":0, "team":0, "playerName":"Alice", "connected":true, "finished":false, "cardCount":0},
			{"seat":1, "team":1, "playerName":"Bob", "connected":true, "finished":false, "cardCount":0}
		],
		"hand": [],
		"table": null,
		"passedSeats": [],
		"finishOrder": [],
		"matchWinner": null
	}
]
```

An empty server returns `[]`. Finished rooms are included until server restart.
Listing does not send a heartbeat or reconnect any player. Connection expiry
is still checked for each room. Use `players.length` for the player count.

`GET /api/rooms/{roomName}` returns the same host-perspective state for one
room without needing a player name or reconnecting anyone:

```powershell
curl.exe "http://localhost:8080/api/rooms"
curl.exe "http://localhost:8080/api/rooms/AliceRoom"
```

On the single-room GET, supplying `?playerName=Alice` selects Alice's
perspective and sends her heartbeat. Start is a room-level command and does
not require player information. The other POST actions below still require
the `playerName` query parameter. URL-encode the parameter for names
containing spaces or non-ASCII characters. No authentication header is needed.
JSON bodies use `Content-Type: application/json`.

| Method | Path | Body | Purpose |
| --- | --- | --- | --- |
| GET | `/api/rooms/{roomName}` | None | Room state; optional playerName selects a player and sends their heartbeat |
| POST | `/api/rooms/{roomName}/start` | None | Start the first or next hand; no playerName required |
| POST | `/api/rooms/{roomName}/plays` | See below | Play owned cards with a legal combination |
| POST | `/api/rooms/{roomName}/pass` | None | Pass when not leading a trick |
| POST | `/api/rooms/{roomName}/heartbeat` | None | Keep a player connected |
| POST | `/api/rooms/{roomName}/disconnect` | None | Mark the player disconnected |
| POST | `/api/rooms/{roomName}/reconnect` | None | Rejoin the same seat using the player name |

Start a room using its name, with no request body:

```powershell
curl.exe -X POST "http://localhost:8080/api/rooms/AliceRoom/start"
```

The backend checks readiness when the request arrives. Starting requires exactly
four connected players and a room in `WAITING` or `HAND_FINISHED` state. Fewer
than four players, disconnected players, an active hand, or a finished match
returns HTTP 409 without dealing cards. A repeated start during play also fails.
Start returns the host-perspective room state and does not send any player's
heartbeat or reconnect anyone.

Example play body (use IDs from your own current `hand`):

```json
{
	"cardIds": ["0-SPADES-KING", "0-HEARTS-TWO"],
	"combination": {"type":"PAIR", "cardCount":2, "strength":13}
}
```

The `combination` field can be omitted when exactly one legal interpretation
beats the current table, or when leading with an unambiguous selection. For
ambiguous selections, send an option returned by `/api/rules/evaluate` using
the room state's current `level`. The room checks ownership, the interpretation,
and whether it beats the table before removing any cards.

Successful player operations return the state for the supplied player name. Important
fields are `roomName`, `status`, `paused`, `viewerSeat`, `turnSeat`, `level`,
`teamLevels`, `players`, `hand`, `table`, `passedSeats`, and `finishOrder`.
Other players' entries expose names and card counts, not their hands.
The table exposes the latest play; it clears when all other active players pass.
After three players finish, `finishOrder` includes the remaining player fourth,
`turnSeat` becomes null, and `status` becomes `HAND_FINISHED` or `MATCH_FINISHED`.
For a match win, `matchWinner` is the winning team index (0 or 1).

Start and pass have no request body; play supplies only card IDs and an optional
combination. Mutations are serialized per room and checked against the room's
current state. There is no stale-request version check or general retry guarantee.
Refresh state and reconsider an action after an error rather than blindly retrying.
Missing rooms or players return 404, and invalid cards/combinations return 400.
A full room, too few players to start, wrong turn, paused game,
or invalid lifecycle action returns 409. Error responses use problem details.

Poll every few seconds even while paused. Connection expiry is checked when
the room is accessed, with a default 60-second heartbeat timeout. Any expired or
explicitly disconnected player pauses an active hand; play resumes once everyone
is connected. A state request with `playerName`, or a heartbeat POST, also
reconnects that player. Room-only GET and room listing do not reconnect players.

## MVP Storage Limits

Rooms and player state live in memory in one backend process and are lost on restart.
There is no database, multi-instance room sharing, room deletion, seat replacement,
or automatic forfeiture yet. Keep one backend instance when deploying this MVP.
The server accepts up to 100 rooms; rooms remain until restart. This bounded
storage is an implementation default, not a production persistence guarantee.

Configuration properties are `dapai.rooms.heartbeat-timeout-seconds` (60 by
default) and `dapai.rooms.max-rooms` (100 by default). Persistent games and an
AWS deployment service/budget remain later decisions.