# 🎲 Monopoly Klagenfurt

> *Because regular Monopoly wasn't rage-inducing enough — now it's multiplayer, real-time, and runs on your phone.*

---

## 🏘️ What Is This?

**Monopoly Klagenfurt** is a multiplayer digital board game inspired by the classic Monopoly — built as an Android app for the *Software Engineering II* course (621.250, 25S) at AAU Klagenfurt.

Roll dice. Buy properties. Bankrupt your friends. Destroy relationships. All in real-time over WebSockets. 🎉

---

## 🕹️ How to Play

### 1. 🔗 Connect
Fire up the app, hit **Connect**, and link up to the game server. You're now in the lobby — the calm before the storm.

### 2. 🆕 Create or Join a Game
- **Create Game** — Be the host. Enter your name, create a room, and share the Game ID with your soon-to-be-enemies.
- **Join Game** — Got a Game ID from a friend? Paste it in, pick a name, and hop in.

### 3. 🚀 Start the Game
Once everyone's in, the host hits **Start Game** and the chaos begins.

### 4. 🎲 Roll the Dice
On your turn, smash that **Roll Dice** button. Your piece moves, and fate decides your destiny:
- 🏠 **Land on a Property?** Buy it or let it go to auction.
- 🚂 **Railroad or Utility?** Strategic gold.
- 💸 **Tax Field?** The government always wins.
- 🃏 **Chance / Community Chest?** Could be a blessing… or a disaster.
- 🚔 **Go to Jail?** Do not pass Go. Do not collect $200. Sit there and think about what you did.
- 🅿️ **Free Parking?** Take a breather — you earned it.

### 5. 💰 Get Rich (or Go Broke)
Collect rent from anyone unlucky enough to land on your properties. Build houses, upgrade to hotels, and watch your empire grow. Or watch your money evaporate. It's Monopoly — anything can happen.

### 6. 🏁 Win the Game
Last player standing wins! Everyone else? Bankrupt and full of regret. 😈

---

## 🔧 Tech Behind the Fun

| Tech | What it does |
|---|---|
| **Kotlin + Android** | The entire app |
| **Jetpack Compose & XML Views** | Beautiful (enough) UI |
| **WebSockets + STOMP** | Real-time multiplayer via [Krossbow](https://github.com/joffrey-bion/krossbow) |
| **Coroutines** | Async networking that doesn't freeze your phone |
| **Spring Boot Backend** | The server pulling all the strings |

## 👥 Team

Built with 💛 (and caffeine ☕) by SE2 students at **Alpen-Adria-Universität Klagenfurt**.

---

*May the dice be ever in your favor.* 🎲🎲
